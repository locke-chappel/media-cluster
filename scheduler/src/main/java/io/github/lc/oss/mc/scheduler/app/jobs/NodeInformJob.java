package io.github.lc.oss.mc.scheduler.app.jobs;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.Node;
import io.github.lc.oss.mc.scheduler.app.model.NodeTypes;
import io.github.lc.oss.mc.scheduler.app.repository.JobRepository;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;
import io.github.lc.oss.mc.scheduler.app.service.JobService;

/**
 * The common use case is to manually trigger this job any time a new job
 * becomes available and any time a worker completes a job. A secondary use of
 * this job is periodically perform a sanity check and make sure we don't have
 * idle workers when there is suitable work to be done. <br />
 * <br/>
 * The scheduled invocation of this job is mostly a catch-all job that checks to
 * see if we have any idle workers and jobs that have not been started. Under
 * normal operation the scheduled invocation shouldn't find anything to do but
 * if somehow a the scheduler failed to hand out the next job when a worker
 * completed it's task this scheduled task would restart that flow.
 */
@DisallowConcurrentExecution
public class NodeInformJob implements org.quartz.Job {
    private static final Logger logger = LoggerFactory.getLogger(NodeInformJob.class);

    @Autowired
    private JobRepository jobRepo;
    @Autowired
    private JobService jobService;
    @Autowired
    private NodeRepository nodeRepo;

    @Transactional(readOnly = true)
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!this.jobRepo.areAnyJobsAvailable()) {
            /*
             * Fast sanity check - no available jobs = nothing to do even if we have idle
             * workers. Short circuit here saves DB query for nodes, node iteration, and job
             * query per node.
             *
             * Note: don't try to be too smart here - keep this check simple otherwise the
             * compute overhead will begin to look like the full processing overhead. :)
             */
            return;
        }

        List<Node> nodes = this.nodeRepo.findByTypeAndStatusIn(NodeTypes.Worker, Status.Available);
        for (Node node : nodes) {
            try {
                ServiceResponse<Job> response = this.jobService.dispatchJob(node.getId());
                if (response.hasMessages(Messages.Application.NoJobsAvailable)
                        || response.hasMessages(Messages.Application.NodeNotAvailable)) {
                    // No-op - normal flow, not an error
                } else if (response.hasMessages()) {
                    this.getLogger().error(
                            String.format("Error dispatching jobs to node %s (%s)", node.getName(), node.getUrl()));
                }
            } catch (Throwable ex) {
                /*
                 * Special case where worker is temporarily offline. It will get picked up next
                 * scheduled run.
                 */
                if (!StringUtils.containsIgnoreCase(ex.getMessage(), "Connection refused")) {
                    throw ex;
                }
            }
        }
    }

    protected Logger getLogger() {
        return NodeInformJob.logger;
    }
}
