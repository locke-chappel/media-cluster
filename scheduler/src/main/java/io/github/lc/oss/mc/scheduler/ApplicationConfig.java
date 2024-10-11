package io.github.lc.oss.mc.scheduler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.h2.tools.Server;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.github.lc.oss.commons.jwt.DefaultUserCache;
import io.github.lc.oss.commons.jwt.UserCache;
import io.github.lc.oss.commons.signing.KeyGenerator;
import io.github.lc.oss.commons.util.TimeIntervalParser;
import io.github.lc.oss.commons.web.config.DefaultAppConfiguration;
import io.github.lc.oss.commons.web.filters.UserLocaleFilter;
import io.github.lc.oss.commons.web.services.HttpService;
import io.github.lc.oss.commons.web.services.JsonService;
import io.github.lc.oss.commons.web.services.ThemeService;
import io.github.lc.oss.commons.web.util.ContextUtil;
import io.github.lc.oss.mc.scheduler.app.filters.CsrfFilter;
import io.github.lc.oss.mc.scheduler.app.filters.JwtFilter;
import io.github.lc.oss.mc.scheduler.app.jobs.CsrfTokenSaltJob;
import io.github.lc.oss.mc.scheduler.app.jobs.JobHistoryCleanupJob;
import io.github.lc.oss.mc.scheduler.app.jobs.NodeInformJob;
import io.github.lc.oss.mc.scheduler.app.jobs.NodeStatusUpdateJob;
import io.github.lc.oss.mc.scheduler.app.jobs.TokenCleanupJob;
import io.github.lc.oss.mc.scheduler.security.JwtManager;
import io.github.lc.oss.mc.scheduler.security.Permissions;
import io.github.lc.oss.mc.scheduler.security.SecureConfig;
import io.github.lc.oss.mc.scheduler.security.WebUser;
import io.github.lc.oss.mc.service.FileService;
import io.github.lc.oss.mc.validation.CommandValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableAutoConfiguration(exclude = { ErrorMvcAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class })
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ComponentScan(basePackages = { "io.github.lc.oss.mc.scheduler.app" })
public class ApplicationConfig extends DefaultAppConfiguration {
    @Value("${application.database-server-mode.enabled:false}")
    private boolean enableDatabaseServer;
    @Value("${application.database-server-mode.port:47890}")
    private int databaseServerPort;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sameSiteCustomizer() {
        return new SameSiteCustomizer();
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> customErrorPageCustomizer() {
        return new ErrorPageCustomizer();
    }

    @Bean
    public FilterRegistrationBean<UserLocaleFilter> registerUserLocaleFilter() {
        FilterRegistrationBean<UserLocaleFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(this.userLocaleFilter());
        bean.setUrlPatterns(Arrays.asList("/*"));
        bean.setName("User Locale Filter");
        return bean;
    }

    @Bean
    @Profile({ "ConsoleListener" })
    public ConsoleListener consoleListener() {
        return new ConsoleListener();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server inMemoryDatabase() throws SQLException {
        if (this.enableDatabaseServer) {
            return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort",
                    Integer.toString(this.databaseServerPort));
        }
        return null;
    }

    @Bean
    public DataSource dataSource(@Autowired SecureConfig config) {
        return DataSourceBuilder.create(). //
                username(config.getDatabaseUser().getUsername()). //
                password(config.getDatabaseUser().getPassword()). //
                url(config.getDatabaseUrl()). //
                build();
    }

    @Bean
    public HttpService httpService() {
        return new HttpService();
    }

    @Bean
    public JsonService jsonService() {
        return new JsonService();
    }

    @Bean
    public FileService fileService() {
        return new FileService();
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return new KeyGenerator();
    }

    @Override
    @Bean
    public io.github.lc.oss.mc.service.PathNormalizer pathNormalizer() {
        return new io.github.lc.oss.mc.service.PathNormalizer();
    }

    @Bean
    public SecureConfig secureConfig(@Autowired Environment env) {
        return this.loadEncryptedConfig(env, SecureConfig.Keys.values(), SecureConfig.class);
    }

    @Bean
    public TimeIntervalParser timeIntervalParser() {
        return new TimeIntervalParser();
    }

    @Bean
    public CommandValidator commandValidator() {
        return new CommandValidator();
    }

    @Bean
    public Scheduler scheduler(@Autowired ApplicationContext context) {
        try {
            SchedulerFactoryBean factory = new SchedulerFactoryBean();

            Properties settings = new Properties();
            settings.put("org.quartz.scheduler.instanceName", "Default");
            settings.put("org.quartz.scheduler.threadCount", "5");
            settings.put("org.quartz.scheduler.jobStore.class", "org.quartz.simpl.RAMJobStore");
            factory.setQuartzProperties(settings);

            factory.setApplicationContext(context);
            factory.setJobFactory(new SpringBeanJobFactory());
            factory.afterPropertiesSet();

            Scheduler scheduler = factory.getScheduler();

            Environment env = context.getEnvironment();
            if (!this.isIntegrationtest(env)) {
                boolean enableNodeInformJob = env.getProperty("application.jobs.NodeInformJob.enabled", Boolean.class,
                        Boolean.FALSE);

                scheduler.start();
                this.scheduleJob(env, scheduler, CsrfTokenSaltJob.class);
                this.scheduleJob(env, scheduler, JobHistoryCleanupJob.class);
                this.scheduleJob(env, scheduler, NodeStatusUpdateJob.class);
                this.scheduleJob(env, scheduler, TokenCleanupJob.class);

                this.registerJob(env, scheduler, NodeInformJob.class, enableNodeInformJob);
            }

            return scheduler;
        } catch (Exception ex) {
            throw new RuntimeException("Error initalizaing Quarts Scheduler", ex);
        }
    }

    private void scheduleJob(Environment env, Scheduler scheduler, Class<? extends Job> jobClass) {
        this.registerJob(env, scheduler, jobClass, true);
    }

    private void registerJob(Environment env, Scheduler scheduler, Class<? extends Job> jobClass, boolean scheduleJob) {
        try {
            String key = JobUtil.getJobKey(jobClass);
            JobDetail job = JobBuilder.newJob(jobClass). //
                    storeDurably(). //
                    withIdentity(key). //
                    build();
            scheduler.addJob(job, true);

            if (scheduleJob) {
                String schedule = env.getRequiredProperty(key);
                Trigger trigger = TriggerBuilder.newTrigger(). //
                        withIdentity(key). //
                        withSchedule(CronScheduleBuilder.cronSchedule(schedule). //
                                withMisfireHandlingInstructionDoNothing())
                        .forJob(job). //
                        build();

                scheduler.scheduleJob(trigger);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error scheduling job", ex);
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        this.configureDefaultPublicAccessUrls(http);

        /* Public Access */
        http.cors((cors) -> org.springframework.security.config.Customizer.withDefaults());
        http.authorizeHttpRequests((ahr) -> ahr //
                .requestMatchers(this.matchers(HttpMethod.GET, //
                        /* Pages */
                        "^/login(?:\\?logout=true)?$", //

                        /* Resources */
                        "^/img/.+$", //
                        "^/l10n/[a-z]{2}(?:-[A-Z]{2})?/messages.Application.Error.1$", //
                        "^/l10n/[a-z]{2}(?:-[A-Z]{2})?/messages.Application.Error.2$",
                        "^/l10n/[a-z]{2}(?:-[A-Z]{2})?/messages.Authentication.Error.2001$",
                        "^/l10n/[a-z]{2}(?:-[A-Z]{2})?/messages.Authentication.Info.2001$",

                        /* Error Page */
                        "^/error$"))
                .permitAll(). //
                requestMatchers(this.matchers(HttpMethod.POST, //
                        /* Public APIs */
                        "^/api/v1/login$", //

                        /* Signed APIs */
                        "^/api/v1/jobs/complete$"))
                .permitAll());

        /* Secured */
        http.authorizeHttpRequests((ahr) -> ahr //
                .anyRequest(). //
                authenticated());
        http.exceptionHandling((eh) -> eh //
                .defaultAuthenticationEntryPointFor(this.authenticationEntryPoint(), //
                        new RegexRequestMatcher(".*", null))
                .accessDeniedHandler(this.accessDeniedHandler()));

        http.addFilterBefore(this.jwtFilter(), UsernamePasswordAuthenticationFilter.class);

        this.configureDefaultHeaders(http);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new AuthenticationEntryPoint() {
            @Autowired
            private JwtManager jwtManager;
            @Autowired
            private ThemeService themeService;

            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException authException) throws IOException, ServletException {
                SecurityContextHolder.clearContext();
                this.jwtManager.invalidate(request, response);
                if (request.getCookies() != null) {
                    Arrays.stream(request.getCookies()). //
                            filter(c -> !StringUtils.equals(c.getName(), this.themeService.getCookieId())). //
                            forEach(c -> {
                                c.setMaxAge(0);
                                response.addCookie(c);
                            });
                }

                response.sendRedirect(ContextUtil.getAbsoluteUrl("/login", request.getServletContext()));
            }
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response,
                    AccessDeniedException accessDeniedException) throws IOException, ServletException {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (ApplicationConfig.hasAnyPermission(auth, Permissions.User)) {
                    response.sendRedirect(ContextUtil.getAbsoluteUrl("/", request.getServletContext()));
                } else {
                    response.sendRedirect(ContextUtil.getAbsoluteUrl("/login", request.getServletContext()));
                }
            }
        };
    }

    @Override
    @Bean
    public CsrfFilter csrfFilter() {
        return new CsrfFilter();
    }

    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter();
    }

    @Bean
    public JwtManager jwtManager() {
        return new JwtManager();
    }

    @Bean
    public UserCache<WebUser> userCache() {
        return new DefaultUserCache<>();
    }

    protected static boolean hasAnyPermission(Authentication user, Permissions... permissions) {
        if (user == null) {
            return false;
        }

        if (!(user.getPrincipal() instanceof WebUser)) {
            return false;
        }

        HashSet<Permissions> toFind = new HashSet<>(Arrays.asList(permissions));
        return user.getAuthorities().stream(). //
                filter(a -> Permissions.hasPermission(a.getAuthority())). //
                map(a -> Permissions.byPermission(a.getAuthority())). //
                anyMatch(p -> toFind.contains(p));
    }
}
