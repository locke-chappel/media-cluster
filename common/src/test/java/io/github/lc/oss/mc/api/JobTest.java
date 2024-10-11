package io.github.lc.oss.mc.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockTest;

public class JobTest extends AbstractMockTest {
    @Test
    public void test_getset() {
        Job j = new Job();

        Assertions.assertNull(j.getBatchIndex());
        Assertions.assertNull(j.getClusterName());
        Assertions.assertNull(j.getId());
        Assertions.assertNull(j.getIndex());
        Assertions.assertNull(j.getProfile());
        Assertions.assertNull(j.getSource());
        Assertions.assertNull(j.getStatus());
        Assertions.assertNull(j.getType());
        Assertions.assertNull(j.getStatusMessage());

        j.setBatchIndex(1);
        j.setClusterName("cluster");
        j.setId("id");
        j.setIndex(999);
        j.setProfile("profile");
        j.setSource("src");
        j.setStatus(Status.Available);
        j.setType(JobTypes.Audio);
        j.setStatusMessage("status message");

        Assertions.assertEquals(Integer.valueOf(1), j.getBatchIndex());
        Assertions.assertEquals("cluster", j.getClusterName());
        Assertions.assertEquals("id", j.getId());
        Assertions.assertEquals(Integer.valueOf(999), j.getIndex());
        Assertions.assertEquals("profile", j.getProfile());
        Assertions.assertEquals("src", j.getSource());
        Assertions.assertEquals(Status.Available, j.getStatus());
        Assertions.assertEquals(JobTypes.Audio, j.getType());
        Assertions.assertEquals("status message", j.getStatusMessage());
    }

    @Test
    public void test_altConstructor() {
        Job j = new Job("id", JobTypes.Audio, Status.Available, "src");

        Assertions.assertNull(j.getBatchIndex());
        Assertions.assertNull(j.getClusterName());
        Assertions.assertEquals("id", j.getId());
        Assertions.assertNull(j.getIndex());
        Assertions.assertNull(j.getProfile());
        Assertions.assertEquals("src", j.getSource());
        Assertions.assertEquals(Status.Available, j.getStatus());
        Assertions.assertEquals(JobTypes.Audio, j.getType());
    }

    @Test
    public void test_altConstructor_scanJobStatus() {
        Job j = new Job("id", JobTypes.Scan, Status.Available, "src");
        Assertions.assertEquals(Status.Available, j.getStatus());
        Assertions.assertEquals(JobTypes.Scan, j.getType());

        j = new Job("id", JobTypes.Scan, Status.Finished, "src");
        Assertions.assertEquals(Status.InProgress, j.getStatus());
        Assertions.assertEquals(JobTypes.Scan, j.getType());

        j = new Job("id", JobTypes.Merge, Status.Finished, "src");
        Assertions.assertEquals(Status.Finished, j.getStatus());
        Assertions.assertEquals(JobTypes.Merge, j.getType());
    }
}
