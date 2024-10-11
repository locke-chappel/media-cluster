package io.github.lc.oss.mc.scheduler.app.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.model.Profile;
import io.github.lc.oss.mc.scheduler.app.repository.JobRepository;
import io.github.lc.oss.mc.scheduler.app.service.JobHistoryService;
import io.github.lc.oss.mc.scheduler.app.service.JobStatusService;
import io.github.lc.oss.mc.scheduler.app.service.ProfileService;
import io.github.lc.oss.mc.service.FileService;
import jakarta.annotation.PostConstruct;

@Component
public class JobCleanerThread extends AbstractThread implements JobCleaner {
    @Autowired
    private FileService fileService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private JobHistoryService jobHistoryService;
    @Autowired
    private JobRepository jobRepo;
    @Autowired
    private JobStatusService jobStatusService;

    @Value("${application.job-queue-clean-size:1000}")
    private int maxQueueSize;

    private final Object lock = new Object();

    private BlockingQueue<Job> toClean;

    @Override
    @PostConstruct
    public void start() {
        this.toClean = new LinkedBlockingQueue<>(this.maxQueueSize);
        super.start();
    }

    @Override
    public boolean offer(Job job) {
        if (job == null) {
            return false;
        }

        synchronized (this.lock) {
            return this.toClean.offer(job);
        }
    }

    @Override
    public void stop() {
        super.stop();
        synchronized (this.lock) {
            this.toClean.offer(null);
        }
    }

    @Override
    public void run() {
        try {
            this.setRunning(true);

            while (this.shouldRun()) {
                Job job = this.toClean.take();
                if (job == null) {
                    /* Special shutdown condition to break the infinite wait */
                    continue;
                }

                Profile profile = this.profileService.validateJson(job.getProfile()).getEntity();

                this.fileService.moveToComplete(job.getSource(), profile.getExt(), job.getClusterName());
                this.fileService.cleanProcessing(job.getSource(), job.getClusterName());
                this.fileService.cleanTemp(job.getSource(), job.getClusterName());

                String finishedScanJobId = this.jobRepo.findJobId( //
                        job.getSource(), //
                        JobTypes.Scan, //
                        Status.Finished);
                if (finishedScanJobId != null) {
                    this.jobStatusService.updateJobStatusSystem(finishedScanJobId, Status.Complete);
                    this.jobHistoryService.moveToHistory(finishedScanJobId);
                }
            }

        } catch (InterruptedException ex) {
            this.getLogger().error("Error waiting for next job to clean", ex);
        } finally {
            this.setRunning(false);
        }
    }
}
