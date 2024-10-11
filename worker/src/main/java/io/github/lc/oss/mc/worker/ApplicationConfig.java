package io.github.lc.oss.mc.worker;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.util.TimeIntervalParser;
import io.github.lc.oss.commons.web.config.DefaultAppConfiguration;
import io.github.lc.oss.commons.web.filters.UserLocaleFilter;
import io.github.lc.oss.commons.web.services.HttpService;
import io.github.lc.oss.commons.web.services.JsonService;
import io.github.lc.oss.mc.api.NodeConfig;
import io.github.lc.oss.mc.service.FileService;
import io.github.lc.oss.mc.validation.CommandValidator;

@Configuration
@EnableAutoConfiguration(exclude = { ErrorMvcAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class })
@EnableAspectJAutoProxy
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ComponentScan(basePackages = { "io.github.lc.oss.mc.worker.app" })
public class ApplicationConfig extends DefaultAppConfiguration {
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

    @Bean
    public io.github.lc.oss.mc.worker.security.Configuration configuration(@Autowired Environment env,
            @Autowired JsonService jsonService) {
        if (this.isIntegrationtest(env)) {
            String prefix = "application.config.";

            NodeConfig config = new NodeConfig();
            config.setId(env.getProperty(prefix + "id"));
            config.setClusterName(env.getProperty(prefix + "clusterName"));
            config.setName(env.getProperty(prefix + "name"));
            config.setPrivateKey(env.getProperty(prefix + "privatekey"));
            config.setSchedulerPublicKey(env.getProperty(prefix + "scheduler.publickey"));
            config.setSchedulerUrl(env.getProperty(prefix + "scheduler.url"));

            return new io.github.lc.oss.mc.worker.security.Configuration(config);
        } else {
            String json = env.getProperty("WORKER_CONFIG");
            if (StringUtils.isBlank(json)) {
                throw new RuntimeException(
                        "Unable to load WORKER_CONFIG, check environment and/or application properties");
            }

            json = Encodings.Base64.decodeString(json);
            NodeConfig config = jsonService.from(json, NodeConfig.class);

            return new io.github.lc.oss.mc.worker.security.Configuration(config);
        }
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

    @Override
    @Bean
    public io.github.lc.oss.mc.service.PathNormalizer pathNormalizer() {
        return new io.github.lc.oss.mc.service.PathNormalizer();
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        /* Public Access */
        http.cors((cors) -> org.springframework.security.config.Customizer.withDefaults());
        http.authorizeHttpRequests((ahr) -> ahr //
                .requestMatchers(this.matchers(HttpMethod.GET, //
                        /* Public Pages */
                        "^/$", //

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
                        /* Signed Pages */
                        "^/api/v1/jobs$"))
                .permitAll(). //
                requestMatchers(this.matchers(HttpMethod.DELETE, //
                        /* Signed Pages */
                        "^/api/v1/jobs$"))
                .permitAll());

        this.configureDefaults(http);

        return http.build();
    }
}
