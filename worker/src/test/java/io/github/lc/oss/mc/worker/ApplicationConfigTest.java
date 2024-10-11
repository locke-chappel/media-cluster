package io.github.lc.oss.mc.worker;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import io.github.lc.oss.commons.web.services.JsonService;
import io.github.lc.oss.mc.api.NodeConfig;
import io.github.lc.oss.mc.worker.security.Configuration;

public class ApplicationConfigTest extends AbstractMockTest {
    @Mock
    private Environment env;
    @Mock
    private JsonService jsonService;

    @InjectMocks
    private ApplicationConfig config;

    @Test
    public void test_configuration_blank() {
        Mockito.when(this.env.getProperty("integrationtest", Boolean.class, Boolean.FALSE)).thenReturn(false);
        Mockito.when(this.env.getProperty("WORKER_CONFIG")).thenReturn(StringUtils.EMPTY);

        try {
            this.config.configuration(this.env, this.jsonService);
            Assertions.fail("Expected Exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Unable to load WORKER_CONFIG, check environment and/or application properties",
                    ex.getMessage());
        }
    }

    @Test
    public void test_configuration() {
        NodeConfig nodeConfig = new NodeConfig();

        Mockito.when(this.env.getProperty("integrationtest", Boolean.class, Boolean.FALSE)).thenReturn(false);
        Mockito.when(this.env.getProperty("WORKER_CONFIG")).thenReturn("e30");
        Mockito.when(this.jsonService.from("{}", NodeConfig.class)).thenReturn(nodeConfig);

        Configuration result = this.config.configuration(this.env, this.jsonService);
        Assertions.assertNotNull(result);
    }
}
