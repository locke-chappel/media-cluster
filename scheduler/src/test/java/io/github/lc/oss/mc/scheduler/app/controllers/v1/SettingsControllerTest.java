package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.mc.scheduler.AbstractMockTest;
import io.github.lc.oss.mc.scheduler.app.service.IdentityService;

public class SettingsControllerTest extends AbstractMockTest {
    @Mock
    private IdentityService identityService;

    @InjectMocks
    private SettingsController controller;

    @Test
    public void test_updateUser_notSelfIssuing() {
        Mockito.when(this.identityService.isSelfIssuing()).thenReturn(false);

        ResponseEntity<Response<?>> result = this.controller.updateUser(null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }
}
