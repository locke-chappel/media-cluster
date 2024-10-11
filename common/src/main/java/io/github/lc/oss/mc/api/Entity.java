package io.github.lc.oss.mc.api;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.lc.oss.commons.serialization.DateSerializer;

@JsonInclude(Include.NON_EMPTY)
public class Entity implements ApiObject {
    private String id;
    @JsonSerialize(using = DateSerializer.class)
    private Date modified;

    public Entity() {
    }

    public Entity(AbstractEntity entity) {
        this.id = entity.getId();
        this.modified = entity.getModified();
    }

    protected Entity(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getModified() {
        return this.modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }
}
