package io.github.lc.oss.mc.scheduler.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.web.annotations.SystemIdentity;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.Job;
import io.github.lc.oss.mc.scheduler.app.repository.JobRepository;

@Service
public class JobStatusService extends AbstractService {
    @Autowired
    private JobRepository jobRepo;

    @SystemIdentity
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ServiceResponse<io.github.lc.oss.mc.api.Job> updateJobStatusSystem(String jobId, Status newStatus) {
        return this.updateJobStatus(jobId, newStatus);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ServiceResponse<io.github.lc.oss.mc.api.Job> updateJobStatus(String jobId, Status newStatus) {
        ServiceResponse<io.github.lc.oss.mc.api.Job> response = new ServiceResponse<>();
        Job job = this.jobRepo.findById(jobId).orElse(null);
        if (job == null) {
            this.rollback();
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        job.setStatus(newStatus);

        return response;
    }
}
