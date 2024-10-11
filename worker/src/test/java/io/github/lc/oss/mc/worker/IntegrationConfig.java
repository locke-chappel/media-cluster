package io.github.lc.oss.mc.worker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

import io.github.lc.oss.commons.signing.KeyGenerator;
import io.github.lc.oss.commons.testing.web.DefaultIntegrationConfig;
import io.github.lc.oss.commons.util.TimeIntervalParser;
import io.github.lc.oss.commons.web.services.HttpService;
import io.github.lc.oss.commons.web.services.JsonService;
import io.github.lc.oss.mc.api.NodeConfig;
import io.github.lc.oss.mc.service.FileService;
import io.github.lc.oss.mc.validation.CommandValidator;

/* Note: same annotations as app config (mostly) */
@Configuration
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = { "io.github.lc.oss.mc.worker.app" })
public class IntegrationConfig extends DefaultIntegrationConfig {
    @Bean
    public io.github.lc.oss.mc.worker.security.Configuration configuration(@Autowired Environment env) {
        String prefix = "application.config.";

        NodeConfig config = new NodeConfig();
        config.setId(env.getProperty(prefix + "id"));
        config.setName(env.getProperty(prefix + "name"));
        config.setPrivateKey(env.getProperty(prefix + "privatekey"));
        config.setSchedulerPublicKey(env.getProperty(prefix + "scheduler.publickey"));
        config.setSchedulerUrl(env.getProperty(prefix + "scheduler.url"));

        return new io.github.lc.oss.mc.worker.security.Configuration(config);
    }

    @Bean
    public Factory factory() {
        return new Factory();
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
}
