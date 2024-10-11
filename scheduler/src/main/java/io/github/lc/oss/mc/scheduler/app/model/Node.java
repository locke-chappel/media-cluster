package io.github.lc.oss.mc.scheduler.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.github.lc.oss.mc.api.Status;

@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Node extends io.github.lc.oss.mc.api.Node {
    public Node() {
        super();
    }

    public Node(io.github.lc.oss.mc.scheduler.app.entity.Node node) {
        super(node);

        this.setClusterName(node.getClusterName());
        this.setName(node.getName());
        this.setStatus(node.getStatus());
        this.setUrl(node.getUrl());
        this.setAllowAudio(node.allowAudio());
        this.setAllowVideo(node.allowVideo());
        this.setAllowScan(node.allowScan());
        this.setAllowMerge(node.allowMerge());
        this.setAllowMux(node.allowMux());
    }

    /* Used by ApplicationSearchService via reflection */
    public Node(String id, String clusterName, String name, Status status, String url) {
        super(id);
        this.setClusterName(clusterName);
        this.setName(name);
        this.setStatus(status);
        this.setUrl(url);
    }
}
