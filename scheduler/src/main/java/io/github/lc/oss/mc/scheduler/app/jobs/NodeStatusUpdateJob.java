package io.github.lc.oss.mc.scheduler.app.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.web.services.HttpService;
import io.github.lc.oss.mc.api.NodeStatus;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.Node;
import io.github.lc.oss.mc.scheduler.app.model.NodeStatusResponse;
import io.github.lc.oss.mc.scheduler.app.model.NodeTypes;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;
import io.github.lc.oss.mc.scheduler.app.service.NodeService;

@DisallowConcurrentExecution
public class NodeStatusUpdateJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(NodeStatusUpdateJob.class);

    @Autowired
    private NodeRepository nodeRepo;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private HttpService httpService;

    @Transactional
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<Node> workingNodes = this.nodeRepo.findByTypeAndStatusIn(NodeTypes.Worker, Status.InProgress);
        for (Node node : workingNodes) {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            ResponseEntity<NodeStatusResponse> response = this.httpService.call(HttpMethod.GET, node.getUrl(), headers,
                    NodeStatusResponse.class, null);
            if (response.getStatusCode() != HttpStatus.OK) {
                NodeStatusUpdateJob.logger.error(String.format("Error getting status for node '%s' (%s) at %s",
                        node.getName(), node.getId(), node.getUrl()));
                continue;
            }

            NodeStatus nodeState = response.getBody().getBody();
            if (nodeState.getStatus() != Status.InProgress) {
                Status newStatus = Status.Available;

                ServiceResponse<io.github.lc.oss.mc.scheduler.app.model.Node> result = this.nodeService
                        .updateNodeStatus(node.getId(), newStatus);
                if (result.hasMessages()) {
                    NodeStatusUpdateJob.logger.error(String.format("Error setting node '%s' (%s) to %s", node.getName(),
                            node.getId(), newStatus));
                }
            }
        }
    }
}
