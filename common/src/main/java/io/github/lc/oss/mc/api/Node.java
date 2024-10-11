package io.github.lc.oss.mc.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Node extends Entity {
    private String clusterName;
    private String name;
    private Status status;
    private String url;
    private String config;
    @JsonProperty("allowAudio")
    private Boolean allowAudio;
    @JsonProperty("allowVideo")
    private Boolean allowVideo;
    @JsonProperty("allowScan")
    private Boolean allowScan;
    @JsonProperty("allowMerge")
    private Boolean allowMerge;
    @JsonProperty("allowMux")
    private Boolean allowMux;

    public Node() {
        super();
    }

    protected Node(String id) {
        super(id);
    }

    protected Node(AbstractEntity entity) {
        super(entity);
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getConfig() {
        return this.config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Boolean allowAudio() {
        return this.allowAudio;
    }

    public void setAllowAudio(Boolean allowAudio) {
        this.allowAudio = allowAudio;
    }

    public Boolean allowVideo() {
        return this.allowVideo;
    }

    public void setAllowVideo(Boolean allowVideo) {
        this.allowVideo = allowVideo;
    }

    public Boolean allowScan() {
        return this.allowScan;
    }

    public void setAllowScan(Boolean allowScan) {
        this.allowScan = allowScan;
    }

    public Boolean allowMerge() {
        return this.allowMerge;
    }

    public void setAllowMerge(Boolean allowMerge) {
        this.allowMerge = allowMerge;
    }

    public Boolean allowMux() {
        return this.allowMux;
    }

    public void setAllowMux(Boolean allowMux) {
        this.allowMux = allowMux;
    }

}
