package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import io.github.lc.oss.commons.identity.model.ApplicationInfo;
import io.github.lc.oss.mc.security.Authorities;
import io.github.lc.oss.mc.scheduler.app.service.IdentityService;
import io.github.lc.oss.mc.scheduler.security.Permissions;

@Controller
@PreAuthorize(Authorities.PUBLIC)
public class ResourceController extends io.github.lc.oss.commons.web.controllers.ResourceController {
    private static final Set<Permissions> NO_PERMISSIONS = new HashSet<>();
    private static final Pattern FILTERED_RESOURCE = Pattern.compile(".+/(?:css|js)/(?:views/(.+)/).+");

    private static final Map<Permissions, Set<String>> PAGES;
    static {
        Map<Permissions, Set<String>> map = new HashMap<>();
        map.put(Permissions.User, Collections.unmodifiableSet(new HashSet<>(Arrays.asList( //
                "jobs", //
                "nodes", //
                "settings"))));
        map.put(null, Collections.unmodifiableSet(new HashSet<>(Arrays.asList( //
                "login"))));
        PAGES = Collections.unmodifiableMap(map);
    }

    @Autowired
    private IdentityService identityService;

    private int sessionTimeout = -1;
    private Map<String, String> extraReplacementValues;

    @Override
    protected Set<String> getAllowedPages() {
        Set<String> allowed = new HashSet<>();
        allowed.addAll(ResourceController.PAGES.get(null)); // public pages
        Set<Permissions> userPermissions = this.getUserPermissions();
        for (Permissions p : userPermissions) {
            allowed.addAll(ResourceController.PAGES.get(p));
        }
        return allowed;
    }

    @Override
    protected String getCacheKeyPrefix() {
        return this.getUserPermissions().stream(). //
                map(p -> p.getPermission()). //
                collect(Collectors.joining("+"));
    }

    @Override
    protected int getSessionTimeout() {
        if (this.sessionTimeout < 0) {
            ApplicationInfo applicationInfo = this.identityService.getApplicationInfo();
            this.sessionTimeout = (int) applicationInfo.getSessionTimeout();
        }
        return this.sessionTimeout;
    }

    @Override
    protected boolean isPageAllowed(Path path) {
        String filePath = StringUtils.replace(path.toString(), "\\", "/");
        Matcher matcher = ResourceController.FILTERED_RESOURCE.matcher(filePath);
        if (!matcher.matches()) {
            /* Unfiltered resources are always allowed */
            return true;
        }

        String permission = matcher.group(1);
        if (!Permissions.hasPermission(permission)) {
            /* Not a permission filtered resource */
            return true;
        }

        return this.getUserPermissions().contains(Permissions.byPermission(permission));
    }

    @Override
    protected Map<String, String> getExtraReplacementValues() {
        if (this.extraReplacementValues == null) {
            Map<String, String> map = new HashMap<>();
            map.put("login.auth.appId", this.identityService.getApplicationId());
            map.put("login.auth.url", this.identityService.getIdentityUrl());
            this.extraReplacementValues = Collections.unmodifiableMap(map);
        }
        return this.extraReplacementValues;
    }

    protected Set<Permissions> getUserPermissions() {
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        if (user == null) {
            return ResourceController.NO_PERMISSIONS;
        }

        return user.getAuthorities().stream(). //
                filter(a -> Permissions.hasPermission(a.getAuthority())). //
                map(a -> Permissions.byPermission(a.getAuthority())). //
                collect(Collectors.toSet());
    }
}
