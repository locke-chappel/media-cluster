package io.github.lc.oss.mc.scheduler.app.entity;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import io.github.lc.oss.mc.entity.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "CHAR(" + Constants.Lengths.ID + ")")
    @Size(min = Constants.Lengths.ID, max = Constants.Lengths.ID)
    private String id;
    @CreatedDate
    @Column(name = "created", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    private Date created = new Date();
    @CreatedBy
    @Column(name = "createdBy", updatable = false, nullable = false, columnDefinition = "CHAR(" + Constants.Lengths.ID
            + ")")
    @Size(min = Constants.Lengths.ID, max = Constants.Lengths.ID)
    private String createdBy;

    public String getId() {
        return this.id;
    }

    public Date getCreated() {
        return this.created;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public boolean isSame(AbstractBaseEntity entity) {
        if (entity == null) {
            return false;
        }
        return StringUtils.equals(this.id, entity.getId());
    }
}
