package io.github.lc.oss.mc.scheduler.app.entity;

import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.entity.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.Size;

@Entity
public class Job extends AbstractEntity {
    @Column(length = Constants.Lengths.NAME)
    @Size(min = 1, max = Constants.Lengths.NAME)
    private String clusterName;
    @Column(length = Constants.Lengths.FILE_PATH, nullable = false)
    @Size(min = 1, max = Constants.Lengths.FILE_PATH)
    private String source;
    @Column(nullable = false, length = Constants.Lengths.ENUM)
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(nullable = false, length = Constants.Lengths.ENUM)
    @Enumerated(EnumType.STRING)
    private JobTypes type;
    @Column(nullable = true)
    private Integer index;
    @Column(nullable = true)
    private Integer batchIndex;
    @Column(length = Constants.Lengths.JSON)
    @Size(max = Constants.Lengths.JSON)
    @Lob
    private String profile;

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
