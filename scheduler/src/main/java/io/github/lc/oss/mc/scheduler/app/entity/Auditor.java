package io.github.lc.oss.mc.scheduler.app.entity;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import io.github.lc.oss.mc.scheduler.security.WebUser;

@Component("auditorAware")
public class Auditor implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        String userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof WebUser) {
            userId = ((WebUser) authentication.getPrincipal()).getId();
        }

        if (StringUtils.isBlank(userId)) {
            return Optional.empty();
        }

        return Optional.of(userId);
    }
}
