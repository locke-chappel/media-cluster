package io.github.lc.oss.mc.scheduler;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.github.lc.oss.commons.jwt.DefaultUserCache;
import io.github.lc.oss.commons.jwt.UserCache;
import io.github.lc.oss.commons.signing.KeyGenerator;
import io.github.lc.oss.commons.testing.web.DefaultIntegrationConfig;
import io.github.lc.oss.commons.util.TimeIntervalParser;
import io.github.lc.oss.commons.web.services.HttpService;
import io.github.lc.oss.commons.web.services.JsonService;
import io.github.lc.oss.commons.web.tokens.CsrfTokenManager;
import io.github.lc.oss.commons.web.tokens.StatelessCsrfTokenManager;
import io.github.lc.oss.commons.web.util.PropertiesConfigUtil;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.scheduler.app.thread.JobCreator;
import io.github.lc.oss.mc.scheduler.security.JwtManager;
import io.github.lc.oss.mc.scheduler.security.SecureConfig;
import io.github.lc.oss.mc.scheduler.security.WebUser;
import io.github.lc.oss.mc.service.FileService;
import io.github.lc.oss.mc.validation.CommandValidator;

/* Note: same annotations as app config (mostly) */
@Configuration
@EnableAutoConfiguration(exclude = { ErrorMvcAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class })
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@ComponentScan(basePackages = { "io.github.lc.oss.mc.scheduler.app" }, excludeFilters = {
        @Filter(type = FilterType.REGEX, pattern = "io.github.lc.oss.mc.scheduler.app.service.FirstRunService"), //
        @Filter(type = FilterType.REGEX, pattern = "io.github.lc.oss.mc.scheduler.app.thread.IntakeThread"), //
        @Filter(type = FilterType.REGEX, pattern = "io.github.lc.oss.mc.scheduler.app.thread.JobCreator") //
})
public class IntegrationConfig extends DefaultIntegrationConfig {
    @Bean
    public DataSource dataSource(@Autowired SecureConfig config) {
        return DataSourceBuilder.create(). //
                username(config.getDatabaseUser().getUsername()). //
                password(config.getDatabaseUser().getPassword()). //
                url(config.getDatabaseUrl()). //
                build();
    }

    @Bean
    public Factory factory() {
        return new Factory();
    }

    @Bean
    public SqlHelper sqlHelper() {
        return new SqlHelper();
    }

    @Bean
    public CsrfTokenManager csrfTokenManager() {
        return new StatelessCsrfTokenManager();
    }

    @Bean
    public JwtManager jwtManager() {
        return new JwtManager();
    }

    @Bean
    public UserCache<WebUser> userCache() {
        return new DefaultUserCache<>();
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

    @Bean
    public TimeIntervalParser timeIntervalParser() {
        return new TimeIntervalParser();
    }

    @Bean
    public CommandValidator commandValidator() {
        return new CommandValidator();
    }

    @Override
    @Bean
    public io.github.lc.oss.mc.service.PathNormalizer pathNormalizer() {
        return new io.github.lc.oss.mc.service.PathNormalizer();
    }

    @Bean
    public JobCreator jobCreator() {
        return new JobCreator() {
            @Override
            public boolean offer(List<Job> jobs) {
                /* No-op */
                return false;
            }
        };
    }

    @Bean
    public SecureConfig secureConfigIt(@Autowired Environment env) {
        SecureConfig config = new SecureConfig();
        PropertiesConfigUtil.loadFromEnv(config, env, "application.secure-config.", SecureConfig.Keys.values());
        return config;
    }
}
