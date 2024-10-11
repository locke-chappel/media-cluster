package io.github.lc.oss.mc.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NodeTest extends AbstractApiEntityTest {
    @Test
    public void test_getset() {
        Node n = new Node();

        Assertions.assertNull(n.allowAudio());
        Assertions.assertNull(n.allowMerge());
        Assertions.assertNull(n.allowMux());
        Assertions.assertNull(n.allowScan());
        Assertions.assertNull(n.allowVideo());
        Assertions.assertNull(n.getClusterName());
        Assertions.assertNull(n.getConfig());
        Assertions.assertNull(n.getName());
        Assertions.assertNull(n.getId());
        Assertions.assertNull(n.getStatus());
        Assertions.assertNull(n.getUrl());

        n.setAllowAudio(true);
        n.setAllowMerge(true);
        n.setAllowMux(true);
        n.setAllowScan(true);
        n.setAllowVideo(true);
        n.setClusterName("junit");
        n.setConfig("c");
        n.setId("id");
        n.setName("name");
        n.setStatus(Status.Available);
        n.setUrl("url");

        Assertions.assertEquals(Boolean.TRUE, n.allowAudio());
        Assertions.assertEquals(Boolean.TRUE, n.allowMerge());
        Assertions.assertEquals(Boolean.TRUE, n.allowMux());
        Assertions.assertEquals(Boolean.TRUE, n.allowScan());
        Assertions.assertEquals(Boolean.TRUE, n.allowVideo());
        Assertions.assertEquals("junit", n.getClusterName());
        Assertions.assertEquals("c", n.getConfig());
        Assertions.assertEquals("id", n.getId());
        Assertions.assertEquals("name", n.getName());
        Assertions.assertEquals(Status.Available, n.getStatus());
        Assertions.assertEquals("url", n.getUrl());
    }

    @Test
    public void test_idConstructor() {
        Node n = new Node("id");

        Assertions.assertNull(n.allowAudio());
        Assertions.assertNull(n.allowMerge());
        Assertions.assertNull(n.allowMux());
        Assertions.assertNull(n.allowScan());
        Assertions.assertNull(n.allowVideo());
        Assertions.assertNull(n.getClusterName());
        Assertions.assertNull(n.getConfig());
        Assertions.assertEquals("id", n.getId());
        Assertions.assertNull(n.getName());
        Assertions.assertNull(n.getStatus());
        Assertions.assertNull(n.getUrl());
    }

    @Test
    public void test_entityConstructor() {
        AbstractEntity base = new TestEntity();

        Node n = new Node(base);

        Assertions.assertNull(n.allowAudio());
        Assertions.assertNull(n.allowMerge());
        Assertions.assertNull(n.allowMux());
        Assertions.assertNull(n.allowScan());
        Assertions.assertNull(n.allowVideo());
        Assertions.assertNull(n.getClusterName());
        Assertions.assertNull(n.getConfig());
        Assertions.assertEquals(base.getId(), n.getId());
        Assertions.assertNull(n.getName());
        Assertions.assertNull(n.getStatus());
        Assertions.assertNull(n.getUrl());

        Assertions.assertSame(base.getModified(), n.getModified());
    }
}
