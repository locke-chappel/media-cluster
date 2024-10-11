package io.github.lc.oss.mc.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class NodeConfig implements ApiObject {
    private String id;
    private String clusterName;
    private String name;
    private String schedulerUrl;
    private String schedulerPublicKey;
    private String privateKey;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getSchedulerUrl() {
        return this.schedulerUrl;
    }

    public void setSchedulerUrl(String schedulerUrl) {
        this.schedulerUrl = schedulerUrl;
    }

    public String getSchedulerPublicKey() {
        return this.schedulerPublicKey;
    }

    public void setSchedulerPublicKey(String schedulerPublicKey) {
        this.schedulerPublicKey = schedulerPublicKey;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
