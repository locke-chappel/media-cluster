package io.github.lc.oss.mc.worker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.lc.oss.mc.worker.security.Configuration;

@ExtendWith(SpringExtension.class)
@Tag("mockTest")
@ActiveProfiles("test")
public abstract class AbstractMockTest extends io.github.lc.oss.commons.testing.web.AbstractLocaleMockTest {
    private Factory factory = new Factory();

    @Mock
    private Configuration config;

    @BeforeEach
    public void init() {
        this.factory.setConfig(this.config);
    }

    protected void expectPublicKey() {
        Mockito.when(this.config.getSchedulerPublicKey())
                .thenReturn("MCowBQYDK2VwAyEA/nhJdAGmoLecvJlhjv6KpWGeS5EBtD3jE873wopqEAA=");
    }

    protected void expectPrivateKey() {
        Mockito.when(this.config.getPrivateKey())
                .thenReturn("MC4CAQAwBQYDK2VwBCIEIJJw6OfOvokC4xO+t247uiR/3ffxrz0i8EBFRH0NWFHs");
    }

    protected Configuration getConfig() {
        return this.config;
    }

    protected Factory fac() {
        return this.factory;
    }
}
