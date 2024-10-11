package io.github.lc.oss.mc.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockTest;

public class NodeConfigTest extends AbstractMockTest {
    @Test
    public void test_getset() {
        NodeConfig nc = new NodeConfig();

        Assertions.assertNull(nc.getClusterName());
        Assertions.assertNull(nc.getId());
        Assertions.assertNull(nc.getName());
        Assertions.assertNull(nc.getPrivateKey());
        Assertions.assertNull(nc.getSchedulerPublicKey());
        Assertions.assertNull(nc.getSchedulerUrl());

        nc.setClusterName("cluster");
        nc.setId("id");
        nc.setName("name");
        nc.setPrivateKey("pk");
        nc.setSchedulerPublicKey("pub");
        nc.setSchedulerUrl("url");

        Assertions.assertEquals("cluster", nc.getClusterName());
        Assertions.assertEquals("id", nc.getId());
        Assertions.assertEquals("name", nc.getName());
        Assertions.assertEquals("pk", nc.getPrivateKey());
        Assertions.assertEquals("pub", nc.getSchedulerPublicKey());
        Assertions.assertEquals("url", nc.getSchedulerUrl());
    }
}
