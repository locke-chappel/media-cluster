package io.github.lc.oss.mc.scheduler.app.entity;

import java.util.HashSet;
import java.util.Set;

import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.entity.Constants;
import io.github.lc.oss.mc.scheduler.app.model.NodeTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Size;

@Entity
public class Node extends AbstractEntity {
    @Column(length = Constants.Lengths.NAME, nullable = true)
    @Size(min = 1, max = Constants.Lengths.NAME)
    private String clusterName;
    @Column(length = Constants.Lengths.NAME, nullable = false, unique = true)
    @Size(min = 1, max = Constants.Lengths.NAME)
    private String name;
    @Column(nullable = false, length = Constants.Lengths.ENUM)
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(nullable = false, length = Constants.Lengths.ENUM)
    @Enumerated(EnumType.STRING)
    private NodeTypes type;
    @Column(length = Constants.Lengths.URL, nullable = true, unique = true)
    @Size(max = Constants.Lengths.URL)
    private String url;
    @Column(length = Constants.Lengths.PUBLIC_KEY, nullable = false, unique = true)
    @Size(max = Constants.Lengths.PUBLIC_KEY)
    private String publicKey;
    @Column(nullable = false)
    private boolean allowAudio;
    @Column(nullable = false)
    private boolean allowVideo;
    @Column(nullable = false)
    private boolean allowScan;
    @Column(nullable = false)
    private boolean allowMerge;
    @Column(nullable = false)
    private boolean allowMux;

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

    public NodeTypes getType() {
        return this.type;
    }

    public void setType(NodeTypes type) {
        this.type = type;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public boolean allowAudio() {
        return this.allowAudio;
    }

    public void setAllowAudio(boolean allowAudio) {
        this.allowAudio = allowAudio;
    }

    public boolean allowVideo() {
        return this.allowVideo;
    }

    public void setAllowVideo(boolean allowVideo) {
        this.allowVideo = allowVideo;
    }

    public boolean allowScan() {
        return this.allowScan;
    }

    public void setAllowScan(boolean allowScan) {
        this.allowScan = allowScan;
    }

    public boolean allowMerge() {
        return this.allowMerge;
    }

    public void setAllowMerge(boolean allowMerge) {
        this.allowMerge = allowMerge;
    }

    public boolean allowMux() {
        return this.allowMux;
    }

    public void setAllowMux(boolean allowMux) {
        this.allowMux = allowMux;
    }

    public Set<JobTypes> getAllowedJobTypes() {
        Set<JobTypes> types = new HashSet<>();
        if (this.allowAudio) {
            types.add(JobTypes.Audio);
        }
        if (this.allowVideo) {
            types.add(JobTypes.Video);
        }
        if (this.allowScan) {
            types.add(JobTypes.Scan);
        }
        if (this.allowMerge) {
            types.add(JobTypes.Merge);
        }
        if (this.allowMux) {
            types.add(JobTypes.Mux);
        }
        return types;
    }
}
