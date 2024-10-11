package io.github.lc.oss.mc.scheduler.app.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.web.annotations.SystemIdentity;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.scheduler.app.entity.Job;
import io.github.lc.oss.mc.scheduler.app.repository.JobRepository;

@Service
public class JobCreatorService extends AbstractService {
    @Autowired
    private JobRepository jobRepo;
    @Autowired
    private JobEntityService jobEntityService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @SystemIdentity
    public ServiceResponse<io.github.lc.oss.mc.api.Job> createJobs(List<io.github.lc.oss.mc.api.Job> jobs) {
        ServiceResponse<io.github.lc.oss.mc.api.Job> response = new ServiceResponse<>();

        final String clusterName = jobs.iterator().next().getClusterName();
        int index = this.jobEntityService.findNextJobIndexForCluster(clusterName);
        for (io.github.lc.oss.mc.api.Job j : jobs) {
            if (!StringUtils.equals(clusterName, j.getClusterName())) {
                this.rollback();
                this.addMessage(response, Messages.Application.MixedClusterNames);
                return response;
            }

            Job job = new Job();
            job.setClusterName(j.getClusterName());
            job.setType(j.getType());
            job.setProfile(j.getProfile());
            job.setSource(j.getSource());
            job.setStatus(j.getStatus());
            job.setIndex(index);
            job.setBatchIndex(j.getBatchIndex());
            this.jobRepo.save(job);

            index++;
        }

        return response;
    }
}
