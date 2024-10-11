package io.github.lc.oss.mc.scheduler.app.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.identity.AbstractIdentityService;
import io.github.lc.oss.commons.identity.model.ApplicationInfo;
import io.github.lc.oss.mc.scheduler.app.entity.User;
import io.github.lc.oss.mc.scheduler.app.entity.UserHash;
import io.github.lc.oss.mc.scheduler.app.repository.UserRepository;
import io.github.lc.oss.mc.scheduler.security.SecureConfig;
import io.github.lc.oss.mc.scheduler.security.WebUser;
import jakarta.transaction.Transactional;

@Service
public class IdentityService extends AbstractIdentityService {
    @Autowired
    private PasswordHasher passwordHasher;
    @Autowired(required = false)
    private SecureConfig config;
    @Autowired
    private UserRepository userRepo;

    @Value("${application.applicationId}")
    private String applicationId;
    @Value("${application.authenticationUrl:}")
    private String authenticationUrl;
    @Value("#{timeIntervalParser.parse('${application.sessionTimeout:365d}')}")
    private long sessionTimeout;
    @Value("#{timeIntervalParser.parse('${server.servlet.session.cookie.max-age:365d}')}")
    private long sessionMaxAge;

    private Set<String> jwtIssuers;

    public IdentityService(@Autowired JsonService jsonService) {
        this.init(jsonService);
    }

    public boolean isSelfIssuing() {
        return StringUtils.isBlank(this.authenticationUrl);
    }

    public String getPublicKey() {
        return null;
    }

    @Override
    public String getApplicationId() {
        return this.applicationId;
    }

    @Override
    protected String getApplicationPrivateKey() {
        if (this.config != null) {
            return this.config.getPrivateKey();
        }
        return null;
    }

    public Set<String> getJwtIssuers() {
        if (this.jwtIssuers == null) {
            if (this.isSelfIssuing()) {
                this.jwtIssuers = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(this.getApplicationId())));
            } else {
                this.jwtIssuers = Collections.unmodifiableSet(new HashSet<>(this.config.getJwtIssuers()));
            }
        }
        return this.jwtIssuers;
    }

    @Override
    protected String getIdentityId() {
        if (this.isSelfIssuing()) {
            return this.getApplicationId();
        }
        return this.config.getJwtIssuers().iterator().next();
    }

    @Override
    public String getIdentityUrl() {
        return this.authenticationUrl;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        if (this.isSelfIssuing()) {
            ApplicationInfo appInfo = new ApplicationInfo();
            appInfo.setSessionMax(this.sessionMaxAge);
            appInfo.setSessionTimeout(this.sessionTimeout);
            return appInfo;
        }
        return super.getApplicationInfo();
    }

    public User getCurrentUser() {
        String id = this.getCurrentId();
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return this.userRepo.findById(id).orElse(null);
    }

    public String getCurrentId() {
        WebUser user = this.getCurrentIdentity();
        return user == null ? null : user.getId();
    }

    public String getCurrentName() {
        WebUser user = this.getCurrentIdentity();
        return user == null ? null : user.getDisplayName();
    }

    public WebUser getCurrentIdentity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof WebUser) {
            return (WebUser) auth.getPrincipal();
        }
        return null;
    }

    @Transactional
    public void updateUser(io.github.lc.oss.mc.scheduler.app.model.User user) {
        User existing = this.userRepo.findByExternalId(this.getCurrentId());

        UserHash hash = new UserHash();
        hash.setHash(this.passwordHasher.hash(user.getPassword()));

        existing.setUsername(user.getUsername());
        existing.addHash(hash);
    }
}
