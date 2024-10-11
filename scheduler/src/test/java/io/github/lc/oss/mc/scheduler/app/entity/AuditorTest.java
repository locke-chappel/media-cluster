package io.github.lc.oss.mc.scheduler.app.entity;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.lc.oss.mc.scheduler.AbstractMockTest;

public class AuditorTest extends AbstractMockTest {
    @InjectMocks
    private Auditor auditor;

    @Test
    public void test_getCurrentAuditor_wrongType() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

        Optional<String> result = this.auditor.getCurrentAuditor();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }
}
