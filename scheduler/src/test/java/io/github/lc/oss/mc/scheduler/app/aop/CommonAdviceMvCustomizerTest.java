package io.github.lc.oss.mc.scheduler.app.aop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.mc.scheduler.AbstractMockTest;

public class CommonAdviceMvCustomizerTest extends AbstractMockTest {
    @InjectMocks
    private CommonAdviceMvCustomizer customizer;

    @Test
    public void test_customize_null() {
        ModelAndView result = this.customizer.customize(null);
        Assertions.assertNull(result);
    }
}
