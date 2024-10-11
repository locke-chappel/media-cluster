package io.github.lc.oss.mc.scheduler.app.entity;

import io.github.lc.oss.mc.entity.Constants;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Size;

@Entity(name = "USERS")
public class User extends AbstractEntity {
    @Column(length = Constants.Lengths.ID, nullable = false, unique = true)
    @Size(min = 1, max = Constants.Lengths.ID)
    private String externalId;
    @Column(length = Constants.Lengths.NAME, nullable = false)
    @Size(min = 1, max = Constants.Lengths.NAME)
    private String username;

    @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "user")
    private UserHash hash;

    public String getExternalId() {
        return this.externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserHash getHash() {
        return this.hash;
    }

    public void addHash(UserHash hash) {
        hash.setUser(this);
        this.hash = hash;
    }
}
