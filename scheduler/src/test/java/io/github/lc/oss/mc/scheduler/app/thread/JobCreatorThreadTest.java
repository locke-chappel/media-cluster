package io.github.lc.oss.mc.scheduler.app.thread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.scheduler.AbstractMockTest;
import io.github.lc.oss.mc.scheduler.app.service.JobCreatorService;
import io.github.lc.oss.mc.scheduler.app.service.ScheduledJobService;

public class JobCreatorThreadTest extends AbstractMockTest {
    private static class CallHelper {
        public boolean wasCalled = false;
    }

    private static class ThreadStopper implements Runnable {
        private JobCreatorThread other;

        public ThreadStopper(JobCreatorThread other) {
            this.other = other;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Assertions.fail("Unexpected exception");
            }
            this.other.stop();
        }
    }

    @Mock
    private JobCreatorService jobCreatorService;
    @Mock
    private ScheduledJobService scheduledJobService;

    @InjectMocks
    private JobCreatorThread thread;

    @BeforeEach
    public void init() {
        this.setField("maxQueueSize", 10000, this.thread);
        this.thread.start();
    }

    @AfterEach
    public void cleanup() {
        BlockingQueue<List<Job>> toCreate = this.getField("toCreate", this.thread);
        toCreate.clear();
    }

    @Test
    public void test_offer() {
        /*
         * This test requires a stopped thread
         */
        this.thread.stop();
        this.waitUntil(() -> !this.thread.isRunning(), 5000);
        BlockingQueue<List<Job>> toCreate = this.getField("toCreate", this.thread);
        toCreate.clear();

        final List<Job> jobs = Arrays.asList(new Job());

        Assertions.assertTrue(toCreate.isEmpty());
        boolean result = this.thread.offer(jobs);
        Assertions.assertTrue(result);
        Assertions.assertEquals(1, toCreate.size());

        try {
            List<Job> actual = toCreate.poll(1, TimeUnit.SECONDS);
            Assertions.assertSame(jobs, actual);
        } catch (InterruptedException ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_offer_empty() {
        /*
         * This test requires a stopped thread
         */
        this.thread.stop();
        this.waitUntil(() -> !this.thread.isRunning(), 5000);
        BlockingQueue<List<Job>> toCreate = this.getField("toCreate", this.thread);
        toCreate.clear();

        final List<Job> jobs = new ArrayList<>();

        Assertions.assertTrue(toCreate.isEmpty());
        boolean result = this.thread.offer(jobs);
        Assertions.assertTrue(toCreate.isEmpty());
        Assertions.assertFalse(result);
    }

    @Test
    public void test_stop() {
        BlockingQueue<List<Job>> toCreate = new LinkedBlockingQueue<>(10);
        JobCreatorThread thread = new JobCreatorThread();
        this.setField("toCreate", toCreate, thread);

        Assertions.assertTrue(toCreate.isEmpty());
        Assertions.assertFalse(thread.isRunning());
        thread.stop();
        Assertions.assertEquals(1, toCreate.size());
        Assertions.assertFalse(thread.isRunning());
        try {
            List<Job> actual = toCreate.poll(1, TimeUnit.SECONDS);
            Assertions.assertNotNull(actual);
            Assertions.assertTrue(actual.isEmpty());
        } catch (InterruptedException ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_run_shouldntRun() {
        this.thread.stop();
        Assertions.assertFalse((boolean) this.getField("shouldRun", this.thread));

        this.thread.run();

        Assertions.assertFalse(this.thread.isRunning());
    }

    @Test
    public void test_run_interrupted() {
        final CallHelper loggerHelper = new CallHelper();

        JobCreatorThread thread = new JobCreatorThread() {
            @Override
            protected Logger getLogger() {
                Assertions.assertFalse(loggerHelper.wasCalled);
                loggerHelper.wasCalled = true;
                return super.getLogger();
            }
        };

        @SuppressWarnings("unchecked")
        final BlockingQueue<List<Job>> toCreate = Mockito.mock(BlockingQueue.class);

        this.setField("toCreate", toCreate, thread);

        try {
            Mockito.when(toCreate.take()).thenThrow(new InterruptedException("WAKE UP!"));
        } catch (InterruptedException ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertFalse(loggerHelper.wasCalled);
        thread.run();
        Assertions.assertTrue(loggerHelper.wasCalled);
        Assertions.assertFalse(thread.isRunning());
    }

    @Test
    public void test_run_createJob_error() {
        final CallHelper loggerHelper = new CallHelper();
        final CallHelper informHelper = new CallHelper();
        final BlockingQueue<List<Job>> toCreate = new LinkedBlockingQueue<>(10000);

        JobCreatorThread thread = new JobCreatorThread() {
            @Override
            protected Logger getLogger() {
                Assertions.assertFalse(loggerHelper.wasCalled);
                loggerHelper.wasCalled = true;
                return super.getLogger();
            }
        };
        this.setField("maxQueueSize", 10000, thread);
        this.setField("toCreate", toCreate, thread);
        this.setField("jobCreatorService", this.jobCreatorService, thread);
        this.setField("scheduledJobService", this.scheduledJobService, thread);

        Job job = new Job();
        List<Job> jobs = Arrays.asList(job);

        ServiceResponse<Job> response = new ServiceResponse<>();
        response.addMessages(Messages.Application.UnhandledError);

        Mockito.when(this.jobCreatorService.createJobs(jobs)).thenReturn(response);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Assertions.assertFalse(informHelper.wasCalled);
                informHelper.wasCalled = true;
                return null;
            }
        }).when(this.scheduledJobService).informNodesOfNewJobs();

        thread.offer(jobs);

        Thread t = new Thread(new ThreadStopper(thread));
        t.start();

        Assertions.assertFalse(loggerHelper.wasCalled);
        Assertions.assertFalse(informHelper.wasCalled);

        thread.run();

        this.waitUntil(() -> !thread.isRunning());
        Assertions.assertTrue(loggerHelper.wasCalled);
        Assertions.assertTrue(informHelper.wasCalled);
    }

    @Test
    public void test_run_createJob() {
        /*
         * This test requires a stopped thread
         */
        this.thread.stop();
        this.waitUntil(() -> !this.thread.isRunning(), 5000);
        BlockingQueue<List<Job>> toCreate = this.getField("toCreate", this.thread);
        toCreate.clear();
        this.thread.setShouldRun(true);

        final CallHelper informHelper = new CallHelper();

        Job job = new Job();
        List<Job> jobs = Arrays.asList(job);

        ServiceResponse<Job> response = new ServiceResponse<>();

        Mockito.when(this.jobCreatorService.createJobs(jobs)).thenReturn(response);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Assertions.assertFalse(informHelper.wasCalled);
                informHelper.wasCalled = true;
                return null;
            }
        }).when(this.scheduledJobService).informNodesOfNewJobs();

        this.thread.offer(jobs);

        Thread t = new Thread(new ThreadStopper(this.thread));
        t.start();

        Assertions.assertFalse(informHelper.wasCalled);

        this.thread.run();

        this.waitUntil(() -> !this.thread.isRunning());
        Assertions.assertTrue(informHelper.wasCalled);
    }
}
