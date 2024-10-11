package io.github.lc.oss.mc.scheduler.app.entity;

import io.github.lc.oss.mc.entity.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.Size;

@Entity
public class JobHistory extends AbstractBaseEntity {
    @Column(length = Constants.Lengths.NAME, nullable = false, updatable = false)
    @Size(min = 1, max = Constants.Lengths.NAME)
    private String clusterName;
    @Column(length = Constants.Lengths.FILE_PATH, nullable = false, updatable = false)
    @Size(min = 1, max = Constants.Lengths.FILE_PATH)
    private String source;
    @Column(length = Constants.Lengths.JSON, nullable = false, updatable = false)
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

    public String getProfile() {
        return this.profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
