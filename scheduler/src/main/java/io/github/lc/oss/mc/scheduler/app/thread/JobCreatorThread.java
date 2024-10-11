package io.github.lc.oss.mc.scheduler.app.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.scheduler.app.service.JobCreatorService;
import io.github.lc.oss.mc.scheduler.app.service.ScheduledJobService;
import jakarta.annotation.PostConstruct;

@Component
public class JobCreatorThread extends AbstractThread implements JobCreator {
    @Autowired
    private JobCreatorService jobCreatorService;
    @Autowired
    private ScheduledJobService scheduledJobService;

    @Value("${application.job-queue-creation-size:10000}")
    private int maxQueueSize;

    private final Object lock = new Object();

    private BlockingQueue<List<Job>> toCreate;

    @Override
    @PostConstruct
    public void start() {
        this.toCreate = new LinkedBlockingQueue<>(this.maxQueueSize);
        super.start();
    }

    @Override
    public boolean offer(List<Job> jobs) {
        if (jobs.isEmpty()) {
            return false;
        }

        synchronized (this.lock) {
            boolean offer = this.toCreate.offer(jobs);

            return offer;
        }
    }

    @Override
    public void stop() {
        super.stop();
        synchronized (this.lock) {
            this.toCreate.offer(new ArrayList<>());
        }
    }

    @Override
    public void run() {
        try {
            this.setRunning(true);

            while (this.shouldRun()) {
                List<Job> jobs = this.toCreate.take();
                if (jobs.isEmpty()) {
                    /* Special shutdown condition to break the infinite wait */
                    continue;
                }

                ServiceResponse<Job> response = this.jobCreatorService.createJobs(jobs);
                if (response.hasMessages()) {
                    this.getLogger().error("Error creating jobs");
                }

                this.scheduledJobService.informNodesOfNewJobs();
            }

        } catch (InterruptedException ex) {
            this.getLogger().error("Error waiting for next job to create", ex);
        } finally {
            this.setRunning(false);
        }
    }
}
