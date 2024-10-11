package io.github.lc.oss.mc.api;

import java.util.Set;

import io.github.lc.oss.commons.util.TypedEnumCache;

public enum Status {
    /**
     * Requires User Input
     */
    Pending,
    /**
     * Available for Processing
     */
    Available,
    /**
     * Actively Processing
     */
    InProgress,
    /**
     * Processing is finished but follow-on tasks are still incomplete
     */
    Finished,
    /**
     * Processing has been completed
     */
    Complete;

    private static final TypedEnumCache<Status, Status> CACHE = new TypedEnumCache<>(Status.class);

    public static Set<Status> all() {
        return Status.CACHE.values();
    }

    public static Status byName(String name) {
        return Status.CACHE.byName(name);
    }

    public static boolean hasName(String name) {
        return Status.CACHE.hasName(name);
    }

    public static Status tryParse(String name) {
        return Status.CACHE.tryParse(name);
    }
}
