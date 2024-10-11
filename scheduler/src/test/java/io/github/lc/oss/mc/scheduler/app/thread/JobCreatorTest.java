package io.github.lc.oss.mc.scheduler.app.thread;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.scheduler.AbstractMockTest;

public class JobCreatorTest extends AbstractMockTest {
    @Test
    public void test_defaultFunction_offer() {
        final Job j = new Job();

        JobCreator creator = new JobCreator() {
            @Override
            public boolean offer(List<Job> jobs) {
                Assertions.assertEquals(1, jobs.size());
                Assertions.assertSame(j, jobs.iterator().next());
                return true;
            }
        };

        creator.offer(j);
    }
}
