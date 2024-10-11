package io.github.lc.oss.mc.scheduler.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

import io.github.lc.oss.commons.jwt.User;

public class WebUser extends org.springframework.security.core.userdetails.User implements User {
    private static final long serialVersionUID = -7653796974378821256L;

    private final String userId;
    private String jwtId;
    private String displayName;

    public WebUser(String username, String displayName, String userId,
            Collection<? extends GrantedAuthority> permissions) {
        super(username, "", permissions);
        this.userId = userId;
        this.displayName = displayName;
    }

    @Override
    public String getId() {
        return this.userId;
    }

    public String getJwtId() {
        return this.jwtId;
    }

    public void setJwtId(String jwtId) {
        if (this.jwtId == null) {
            this.jwtId = jwtId;
        }
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
