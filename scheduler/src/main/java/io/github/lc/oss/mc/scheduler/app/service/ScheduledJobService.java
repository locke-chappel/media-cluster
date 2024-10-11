package io.github.lc.oss.mc.scheduler.app.service;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.lc.oss.mc.scheduler.JobUtil;
import io.github.lc.oss.mc.scheduler.app.jobs.NodeInformJob;
import io.github.lc.oss.mc.scheduler.app.jobs.NodeStatusUpdateJob;

@Service
public class ScheduledJobService extends AbstractService {
    @Autowired
    private Scheduler scheduler;

    public void informNodesOfNewJobs() {
        this.triggerJob(NodeInformJob.class);
    }

    public void updateNodeStatuses() {
        this.triggerJob(NodeStatusUpdateJob.class);
    }

    public void triggerJob(Class<? extends Job> jobClass) {
        try {
            Set<JobKey> jobKeys = this.scheduler.getJobKeys(GroupMatcher.anyGroup());
            JobKey key = jobKeys.stream() //
                    .filter(k -> StringUtils.equals( //
                            k.getName(), //
                            JobUtil.getJobKey(jobClass))) //
                    .findAny() //
                    .orElse(null);
            this.scheduler.triggerJob(key);
        } catch (SchedulerException ex) {
            this.getLogger().error(String.format("Error triggering scheduled job '%s'", jobClass.getSimpleName()), ex);
        }
    }
}
