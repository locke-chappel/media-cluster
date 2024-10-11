package io.github.lc.oss.mc.scheduler.security;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.scheduler.AbstractMockTest;

public class WebUserTest extends AbstractMockTest {
    @Test
    public void test_jwt() {
        WebUser wu = new WebUser("user", "display", "id", new ArrayList<>());
        Assertions.assertNull(wu.getJwtId());

        wu.setJwtId("jid");
        Assertions.assertEquals("jid", wu.getJwtId());
        wu.setJwtId("new");
        Assertions.assertEquals("jid", wu.getJwtId());
    }
}
