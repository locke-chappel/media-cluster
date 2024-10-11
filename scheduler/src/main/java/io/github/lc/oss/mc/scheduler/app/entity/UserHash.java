package io.github.lc.oss.mc.scheduler.app.entity;

import io.github.lc.oss.mc.entity.Constants;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Size;

@Entity
public class UserHash extends AbstractEntity {
    @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "\"User\"", nullable = false, columnDefinition = "CHAR(" + Constants.Lengths.ID + ")")
    private User user;
    @Column(length = 256, nullable = false)
    @Size(min = 1, max = 256)
    private String hash;

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getHash() {
        return this.hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
