package io.github.lc.oss.mc.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class Job extends Entity {
    private String id;
    private String clusterName;
    private String source;
    private Status status;
    private String statusMessage;
    private JobTypes type;
    private Integer index;
    private Integer batchIndex;
    private String profile;

    public Job() {
    }

    /* Used by search services via reflection */
    public Job(String id, JobTypes type, Status status, String source) {
        this.id = id;
        this.type = type;
        this.source = source;

        if (this.type == JobTypes.Scan && status == Status.Finished) {
            this.status = Status.InProgress;
        } else {
            this.status = status;
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public JobTypes getType() {
        return this.type;
    }

    public void setType(JobTypes type) {
        this.type = type;
    }

    public Integer getIndex() {
        return this.index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Integer getBatchIndex() {
        return this.batchIndex;
    }

    public void setBatchIndex(Integer batchIndex) {
        this.batchIndex = batchIndex;
    }

    public String getProfile() {
        return this.profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
