package io.github.lc.oss.mc.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockTest;

public class JobResultTest extends AbstractMockTest {
    @Test
    public void test_getset() {
        JobResult jr = new JobResult();

        Assertions.assertNull(jr.getId());
        Assertions.assertNull(jr.getResult());

        jr.setId("id");
        jr.setResult(1l);

        Assertions.assertEquals("id", jr.getId());
        Assertions.assertEquals(1l, jr.getResult());
    }
}
