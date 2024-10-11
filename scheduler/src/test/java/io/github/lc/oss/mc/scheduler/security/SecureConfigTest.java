package io.github.lc.oss.mc.scheduler.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.scheduler.AbstractMockTest;

public class SecureConfigTest extends AbstractMockTest {
    /*
     * Coverage filler, a properly running system should never log errors here, when
     * it does it's just to help fix the deployment issue.
     */
    @Test
    public void test_logError() {
        SecureConfig config = new SecureConfig();

        config.logError("message");
    }

    /*
     * These fields are only used when connecting to an external identity service
     */
    @Test
    public void test_externalIdentity_filler() {
        SecureConfig config = new SecureConfig();

        Assertions.assertNull(config.getPrivateKey());
        Assertions.assertNull(config.getJwtIssuers());
    }
}
