package io.github.lc.oss.mc.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockTest;

public class NodeStatusTest extends AbstractMockTest {
    @Test
    public void test_currentJob() {
        NodeStatus ns = new NodeStatus();

        Assertions.assertNull(ns.getCurrentJob());

        final Job job = new Job();
        ns.setCurrentJob(job);

        Assertions.assertSame(job, ns.getCurrentJob());
    }
}
