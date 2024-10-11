package io.github.lc.oss.mc.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockTest;

public class ConstantsTest extends AbstractMockTest {
    /**
     * Coverage tools don't reliably detect access to this value despite it's use.
     */
    @Test
    public void test_coverageFiller() {
        Assertions.assertNotNull(Constants.FILE_SEPARATOR);
    }
}
