package io.github.lc.oss.mc.scheduler.app.service;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.web.annotations.SystemIdentity;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.Job;
import io.github.lc.oss.mc.scheduler.app.entity.JobHistory;
import io.github.lc.oss.mc.scheduler.app.repository.JobHistoryRepository;
import io.github.lc.oss.mc.scheduler.app.repository.JobRepository;

@Service
public class JobHistoryService extends AbstractService {
    @Autowired
    private JobRepository jobRepo;
    @Autowired
    private JobHistoryRepository jobHistoryRepo;

    @SystemIdentity
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ServiceResponse<io.github.lc.oss.mc.api.Job> moveToHistory(String jobId) {
        ServiceResponse<io.github.lc.oss.mc.api.Job> response = new ServiceResponse<>();
        Job job = this.jobRepo.findById(jobId).orElse(null);
        if (job == null) {
            this.rollback();
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        List<Job> jobs = this.jobRepo.findBySourceIgnoreCase(job.getSource());
        for (Job j : jobs) {
            if (j.getStatus() != Status.Complete) {
                this.rollback();
                this.addMessage(response, Messages.Application.UnfinishedJob);
                return response;
            }

            this.jobRepo.delete(j);
        }

        JobHistory history = new JobHistory();
        history.setClusterName(job.getClusterName());
        history.setSource(job.getSource());
        history.setProfile(job.getProfile());
        this.jobHistoryRepo.save(history);

        this.jobRepo.delete(job);

        return response;
    }

    /*
     * This service is called exclusively via background thread which will never
     * have a user context, therefore we must provide a sane default here to resolve
     * messages with.
     */
    @Override
    protected Locale getUserLocale() {
        return Locale.ENGLISH;
    }
}
