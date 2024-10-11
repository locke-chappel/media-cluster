package io.github.lc.oss.mc.scheduler.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.lc.oss.commons.util.TypedEnumCache;
import io.github.lc.oss.mc.security.Authorities;

public enum Permissions {
    User(Authorities.USER_PERMISSION);

    private final String permission;

    private static final TypedEnumCache<Permissions, Permissions> CACHE = new TypedEnumCache<>(Permissions.class);
    private static final Map<String, Permissions> BY_PERMISSION = Arrays.stream(Permissions.values()). //
            collect(Collectors.collectingAndThen( //
                    Collectors.toMap( //
                            Permissions::getPermission, //
                            Function.identity()), //
                    Collections::unmodifiableMap));

    public static Set<Permissions> all() {
        return Permissions.CACHE.values();
    }

    public static Permissions byName(String name) {
        return Permissions.CACHE.byName(name);
    }

    public static boolean hasName(String name) {
        return Permissions.CACHE.hasName(name);
    }

    public static Permissions tryParse(String name) {
        return Permissions.CACHE.tryParse(name);
    }

    public static Permissions byPermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("No enum constant Permissions.null");
        }

        Permissions result = Permissions.BY_PERMISSION.get(permission);
        if (result == null) {
            throw new IllegalArgumentException(String.format("No enum constant Permissions.%s", permission));
        }
        return result;
    }

    public static boolean hasPermission(String permission) {
        return Permissions.BY_PERMISSION.containsKey(permission);
    }

    private Permissions(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return this.permission;
    }
}
