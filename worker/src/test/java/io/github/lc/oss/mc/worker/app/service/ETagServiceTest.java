package io.github.lc.oss.mc.worker.app.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.info.BuildProperties;

import io.github.lc.oss.mc.worker.AbstractMockTest;

public class ETagServiceTest extends AbstractMockTest {
    @Mock
    private BuildProperties buildProperties;

    @InjectMocks
    private ETagService service;

    @Test
    public void test_methods() {
        Mockito.when(this.buildProperties.getVersion()).thenReturn("1.1.1");

        Assertions.assertEquals("1.1.1", this.service.getAppVersion());
    }
}
