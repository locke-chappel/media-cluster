package io.github.lc.oss.mc.worker.security;

import io.github.lc.oss.mc.api.NodeConfig;

public class Configuration {
    private final String id;
    private String clusterName;
    private final String name;
    private final String schedulerUrl;
    private final String schedulerPublicKey;
    private final String privateKey;

    public Configuration(NodeConfig config) {
        this.id = config.getId();
        this.clusterName = config.getClusterName();
        this.name = config.getName();
        this.schedulerUrl = config.getSchedulerUrl();
        this.schedulerPublicKey = config.getSchedulerPublicKey();
        this.privateKey = config.getPrivateKey();
    }

    public String getId() {
        return this.id;
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public String getName() {
        return this.name;
    }

    public String getSchedulerUrl() {
        return this.schedulerUrl;
    }

    public String getSchedulerPublicKey() {
        return this.schedulerPublicKey;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }
}
