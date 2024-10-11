package io.github.lc.oss.mc.scheduler.app.entity;

import io.github.lc.oss.mc.entity.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.Size;

@Entity
public class Profile extends AbstractEntity {
    @Column(length = Constants.Lengths.NAME, nullable = false)
    @Size(min = 1, max = Constants.Lengths.NAME)
    private String name;
    @Column(length = Constants.Lengths.JSON)
    @Size(max = Constants.Lengths.JSON)
    @Lob
    private String json;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJson() {
        return this.json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
