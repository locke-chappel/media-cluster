package io.github.lc.oss.mc.scheduler.app.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

public abstract class AbstractThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractThread.class);

    private boolean isRunning = false;
    private boolean shouldRun = true;

    public boolean isRunning() {
        return this.isRunning;
    }

    protected void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public void stop() {
        this.shouldRun = false;
    }

    /*
     * Exposed for testing
     */
    void setShouldRun(boolean shouldRun) {
        this.shouldRun = shouldRun;
    }

    protected boolean shouldRun() {
        return this.shouldRun;
    }

    @PostConstruct
    public void start() {
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    protected Logger getLogger() {
        return AbstractThread.logger;
    }
}
