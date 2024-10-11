package io.github.lc.oss.mc.scheduler.app.jobs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.mc.scheduler.app.entity.JobHistory;
import io.github.lc.oss.mc.scheduler.app.repository.JobHistoryRepository;

@DisallowConcurrentExecution
public class JobHistoryCleanupJob implements Job {
    @Autowired
    private JobHistoryRepository jobHistoryRepo;

    @Value("${application.history.retention:30}")
    private int keepHistoryForDays;

    @Transactional
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Date cutoff = Date
                .from(Instant.now().truncatedTo(ChronoUnit.DAYS).minus(this.keepHistoryForDays, ChronoUnit.DAYS));
        List<JobHistory> toDelete = this.jobHistoryRepo.findByCreatedLessThan(cutoff);
        for (JobHistory hist : toDelete) {
            this.jobHistoryRepo.delete(hist);
        }
    }
}
