package io.github.lc.oss.mc.scheduler.app.entity;

import java.util.Date;

import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import io.github.lc.oss.mc.entity.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;

@MappedSuperclass
public abstract class AbstractEntity extends AbstractBaseEntity implements io.github.lc.oss.mc.api.AbstractEntity {
    @LastModifiedDate
    @Column(name = "modified", columnDefinition = "TIMESTAMP")
    private Date modified;
    @LastModifiedBy
    @Column(name = "modifiedBy", columnDefinition = "CHAR(" + Constants.Lengths.ID + ")")
    @Size(min = Constants.Lengths.ID, max = Constants.Lengths.ID)
    private String modifiedBy;

    @Override
    public Date getModified() {
        return this.modified;
    }

    @Override
    public String getModifiedBy() {
        return this.modifiedBy;
    }
}
