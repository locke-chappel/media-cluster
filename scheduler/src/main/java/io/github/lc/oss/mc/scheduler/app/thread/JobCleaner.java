package io.github.lc.oss.mc.scheduler.app.thread;

import io.github.lc.oss.mc.api.Job;

public interface JobCleaner {
    boolean offer(Job job);
}
