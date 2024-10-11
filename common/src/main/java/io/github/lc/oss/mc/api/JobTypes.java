package io.github.lc.oss.mc.api;

import java.util.Set;

import io.github.lc.oss.commons.util.TypedEnumCache;

public enum JobTypes {
    Video,
    Audio,
    Merge,
    Mux,
    Scan;

    private static final TypedEnumCache<JobTypes, JobTypes> CACHE = new TypedEnumCache<>(JobTypes.class);

    public static Set<JobTypes> all() {
        return JobTypes.CACHE.values();
    }

    public static JobTypes byName(String name) {
        return JobTypes.CACHE.byName(name);
    }

    public static boolean hasName(String name) {
        return JobTypes.CACHE.hasName(name);
    }

    public static JobTypes tryParse(String name) {
        return JobTypes.CACHE.tryParse(name);
    }
}
