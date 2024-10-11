package io.github.lc.oss.mc.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockTest;

public class SignedRequestTest extends AbstractMockTest {
    @Test
    public void test_getset() {
        SignedRequest sr = new SignedRequest();

        Assertions.assertNull(sr.getBody());
        Assertions.assertNotNull(sr.getCreated());
        Assertions.assertTrue(sr.getCreated() <= System.currentTimeMillis());
        Assertions.assertNull(sr.getNodeId());
        Assertions.assertNull(sr.getSignature());
        Assertions.assertEquals(Long.toString(sr.getCreated()) + "nullnull", sr.getSignatureData());

        sr.setBody("body");
        sr.setCreated(0);
        sr.setNodeId("node-id");
        sr.setSignature("sig");

        Assertions.assertEquals("body", sr.getBody());
        Assertions.assertEquals(0, sr.getCreated());
        Assertions.assertEquals("node-id", sr.getNodeId());
        Assertions.assertEquals("sig", sr.getSignature());
        Assertions.assertEquals("0node-idbody", sr.getSignatureData());
    }
}
