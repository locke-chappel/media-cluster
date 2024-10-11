package io.github.lc.oss.mc.worker.app.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.worker.AbstractMockTest;

public class FFMPEGServiceThreadingTest extends AbstractMockTest {
    private static class ThreadHelper implements Runnable {
        private Thread parent;

        public ThreadHelper(Thread parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Test was interrupted unexpectedly", ex);
            }
            this.parent.interrupt();
        }
    }

    @Test
    public void test_waitForResult_interrupt() {
        Thread t = new Thread(new ThreadHelper(Thread.currentThread()));
        t.start();

        FFMPEGService service = new FFMPEGService();
        this.setField("postProcessingTimeout", 250, service);

        try {
            Runnable runnable = service.waitForResult();
            runnable.run();
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Interrupted while waiting for condition.", ex.getMessage());
        }
    }

    @Test
    public void test_waitForResult_timeout() {
        FFMPEGService service = new FFMPEGService() {
            @Override
            void onExit() {
            }
        };
        this.setField("postProcessingTimeout", 250, service);

        final long start = System.currentTimeMillis();
        Runnable runnable = service.waitForResult();
        runnable.run();
        final long end = System.currentTimeMillis();
        Assertions.assertTrue(end - start >= 250);
    }
}
