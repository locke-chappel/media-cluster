package io.github.lc.oss.mc.scheduler.app.aop;

import java.util.ArrayList;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.web.advice.AbstractSystemIdentityAdvice;
import io.github.lc.oss.mc.scheduler.security.WebUser;

@Component
public class SystemIdentityAdvice extends AbstractSystemIdentityAdvice {
    private final WebUser systemUser = new WebUser("System", "System", "DEADBEEF-DEAD-BEEF-DEAD-BEEFDEADBEEF",
            new ArrayList<>());
    private final Authentication systemAuth = new UsernamePasswordAuthenticationToken(this.systemUser, null,
            this.systemUser.getAuthorities());

    @Override
    protected Authentication getSystemAuthentication() {
        return this.systemAuth;
    }
}
