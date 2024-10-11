package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.lc.oss.mc.scheduler.AbstractMockTest;
import io.github.lc.oss.mc.scheduler.app.service.ETagService;
import io.github.lc.oss.mc.scheduler.security.Permissions;

public class ResourceControllerTest extends AbstractMockTest {
    @Mock
    private ETagService etagService;

    @InjectMocks
    private ResourceController controller;

    @Test
    public void test_isPageAllowed_notAPermission() {
        boolean result = this.controller.isPageAllowed(Path.of("junk/css/views/junk/view.css"));
        Assertions.assertTrue(result);
    }

    @Test
    public void test_isPageAllowed_noUser() {
        SecurityContextHolder.clearContext();

        boolean result = this.controller
                .isPageAllowed(Path.of("junk/css/views/" + Permissions.User.getPermission() + "/view.css"));
        Assertions.assertFalse(result);
    }
}
