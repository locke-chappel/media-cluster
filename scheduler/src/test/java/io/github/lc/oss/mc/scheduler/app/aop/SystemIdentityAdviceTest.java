package io.github.lc.oss.mc.scheduler.app.aop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.security.core.Authentication;

import io.github.lc.oss.mc.scheduler.AbstractMockTest;
import io.github.lc.oss.mc.scheduler.security.WebUser;

public class SystemIdentityAdviceTest extends AbstractMockTest {
    @InjectMocks
    private SystemIdentityAdvice advice;

    @Test
    public void test_getSystemAuthentication() {
        Authentication result = this.advice.getSystemAuthentication();
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getCredentials());
        Assertions.assertNotNull(result.getAuthorities());
        Assertions.assertTrue(result.getAuthorities().isEmpty());
        Object principal = result.getPrincipal();
        Assertions.assertNotNull(principal);
        Assertions.assertTrue(principal instanceof WebUser);
        WebUser system = (WebUser) principal;
        Assertions.assertEquals("DEADBEEF-DEAD-BEEF-DEAD-BEEFDEADBEEF", system.getId());
        Assertions.assertNull(system.getJwtId());
        Assertions.assertNotNull(system.getAuthorities());
        Assertions.assertTrue(system.getAuthorities().isEmpty());

        Assertions.assertSame(result, this.advice.getSystemAuthentication());
    }
}
