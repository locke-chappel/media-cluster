package io.github.lc.oss.mc.scheduler.app.service;

import java.security.KeyPair;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.commons.signing.KeyGenerator;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.NodeConfig;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.model.Node;
import io.github.lc.oss.mc.scheduler.app.model.NodeTypes;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;

@Service
public class NodeService extends AbstractService {
    private static final String SCHEDULER_ID = "scheduler";

    @Autowired
    private JsonService jsonService;
    @Autowired
    private KeyGenerator keyGenerator;
    @Autowired
    private NodeRepository nodeRepo;
    @Autowired
    private SignatureService signatureService;

    private String urlPrefix;

    @Transactional(readOnly = true)
    public boolean canClusterProcess(String clusterName, boolean requireAudio, boolean requiredVideo) {
        List<io.github.lc.oss.mc.scheduler.app.entity.Node> nodes = this.nodeRepo
                .findByClusterNameIgnoreCase(clusterName);
        // Scan and Mux jobs are always required
        if (!nodes.stream().anyMatch(n -> n.allowScan())) {
            // cluster can't scan
            return false;
        }

        if (!nodes.stream().anyMatch(n -> n.allowMux())) {
            // cluster can't mux
            return false;
        }

        if (requireAudio && !nodes.stream().anyMatch(n -> n.allowAudio())) {
            // job requires audio but cluster can't process audio
            return false;
        }

        if (requiredVideo && !nodes.stream().anyMatch(n -> n.allowVideo())) {
            // job requires video but cluster can't process video
            return false;
        }

        return true;
    }

    @Transactional(readOnly = true)
    public String getUrlPrefix() {
        if (this.urlPrefix == null) {
            String schedulerUrl = this.getSchedulerNode().getUrl();
            this.urlPrefix = StringUtils.substringBefore(schedulerUrl, "://") + "://";
        }
        return this.urlPrefix;
    }

    public void clearUrlPrefix() {
        this.urlPrefix = null;
    }

    @Transactional(readOnly = true)
    public io.github.lc.oss.mc.scheduler.app.entity.Node getSchedulerNode() {
        List<io.github.lc.oss.mc.scheduler.app.entity.Node> schedulers = this.nodeRepo
                .findByTypeIn(NodeTypes.Scheduler);
        if (schedulers.size() != 1) {
            throw new RuntimeException(
                    String.format("Expected exactly 1 scheduler type node but found %s", schedulers.size()));
        }
        return schedulers.iterator().next();
    }

    @Transactional(readOnly = true)
    public ServiceResponse<Node> getNode(String id) {
        ServiceResponse<Node> response = new ServiceResponse<>();

        io.github.lc.oss.mc.scheduler.app.entity.Node existing = null;
        if (StringUtils.equals(NodeService.SCHEDULER_ID, id)) {
            existing = this.getSchedulerNode();
        } else {
            existing = this.nodeRepo.findById(id).orElse(null);
        }

        if (existing == null) {
            response.setMessages(this.toMessages(Messages.Application.NotFound));
            return response;
        }

        response.setEntity(new Node(existing));
        return response;
    }

    @Transactional
    public ServiceResponse<Node> newConfig(String nodeId) {
        ServiceResponse<Node> response = new ServiceResponse<>();

        io.github.lc.oss.mc.scheduler.app.entity.Node node = this.nodeRepo.findById(nodeId).orElse(null);
        if (node == null) {
            response.setMessages(this.toMessages(Messages.Application.NotFound));
            return response;
        }

        String schedulerUrl = this.getSchedulerNode().getUrl();
        if (StringUtils.isEmpty(schedulerUrl)) {
            response.setMessages(this.toMessages(Messages.Application.NoSchedulerUrl));
            return response;
        }

        KeyPair keyPair = this.keyGenerator.generate(Algorithms.ED25519);
        node.setPublicKey(Encodings.Base64.encode(keyPair.getPublic().getEncoded()));

        NodeConfig config = new NodeConfig();
        config.setId(node.getId());
        config.setClusterName(node.getClusterName());
        config.setName(node.getName());
        config.setSchedulerUrl(schedulerUrl);
        config.setSchedulerPublicKey(this.signatureService.getPublicKey());
        config.setPrivateKey(Encodings.Base64.encode(keyPair.getPrivate().getEncoded()));

        response.setEntity(new Node(node));
        response.getEntity().setConfig(Encodings.Base64.encode(this.jsonService.to(config)));
        return response;
    }

    @Transactional
    public ServiceResponse<Node> saveNode(Node request) {
        ServiceResponse<Node> response = new ServiceResponse<>();

        io.github.lc.oss.mc.scheduler.app.entity.Node node;
        if (StringUtils.isBlank(request.getId())) {
            node = new io.github.lc.oss.mc.scheduler.app.entity.Node();

            KeyPair keyPair = this.keyGenerator.generate(Algorithms.ED448);
            node.setPublicKey(Encodings.Base64.encode(keyPair.getPublic().getEncoded()));
            node.setName(request.getName());
            node.setStatus(Status.Available);
            node.setType(NodeTypes.Worker);
            node.setClusterName(request.getClusterName());
            this.nodeRepo.save(node);

            String schedulerUrl = this.getSchedulerNode().getUrl();
            if (StringUtils.isEmpty(schedulerUrl)) {
                this.rollback();
                response.setMessages(this.toMessages(Messages.Application.NoSchedulerUrl));
                return response;
            }

            NodeConfig config = new NodeConfig();
            config.setId(node.getId());
            config.setName(node.getName());
            config.setSchedulerUrl(schedulerUrl);
            config.setSchedulerPublicKey(this.signatureService.getPublicKey());
            config.setPrivateKey(Encodings.Base64.encode(keyPair.getPrivate().getEncoded()));

            response.setEntity(new Node(node));
            response.getEntity().setConfig(Encodings.Base64.encode(this.jsonService.to(config)));
        } else {
            node = this.nodeRepo.findById(request.getId()).orElse(null);
            if (node == null) {
                this.rollback();
                response.setMessages(this.toMessages(Messages.Application.NotFound));
                return response;
            }
            response.setEntity(new Node(node));
        }

        node.setClusterName(request.getClusterName());
        response.getEntity().setClusterName(request.getClusterName());

        node.setName(request.getName());
        response.getEntity().setName(request.getName());

        node.setUrl(request.getUrl());
        response.getEntity().setUrl(request.getUrl());

        node.setAllowAudio(this.allow(request.allowAudio()));
        response.getEntity().setAllowAudio(request.allowAudio());

        node.setAllowVideo(this.allow(request.allowVideo()));
        response.getEntity().setAllowVideo(request.allowVideo());

        node.setAllowScan(this.allow(request.allowScan()));
        response.getEntity().setAllowScan(request.allowScan());

        node.setAllowMerge(this.allow(request.allowMerge()));
        response.getEntity().setAllowMerge(request.allowMerge());

        node.setAllowMux(this.allow(request.allowMux()));
        response.getEntity().setAllowMux(request.allowMux());

        if (node.getType() == NodeTypes.Scheduler) {
            this.urlPrefix = null;
        }

        return response;
    }

    private boolean allow(Boolean allowed) {
        return allowed != null && allowed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ServiceResponse<Node> updateNodeStatus(String nodeId, Status status) {
        ServiceResponse<Node> response = new ServiceResponse<>();

        io.github.lc.oss.mc.scheduler.app.entity.Node node = this.nodeRepo.findById(nodeId).orElse(null);
        if (node == null) {
            this.rollback();
            response.setMessages(this.toMessages(Messages.Application.NotFound));
            return response;
        }

        node.setStatus(status);

        return response;
    }
}
