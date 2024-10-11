package io.github.lc.oss.mc.scheduler.app.thread;

import java.util.Arrays;
import java.util.List;

import io.github.lc.oss.mc.api.Job;

public interface JobCreator {
    default boolean offer(Job job) {
        return this.offer(Arrays.asList(job));
    }

    boolean offer(List<Job> jobs);
}
