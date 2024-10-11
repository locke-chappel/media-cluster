package io.github.lc.oss.mc.scheduler.app.model;

import java.util.Set;

import io.github.lc.oss.commons.util.TypedEnumCache;

public enum NodeTypes {
    Scheduler,
    Worker;

    private static final TypedEnumCache<NodeTypes, NodeTypes> CACHE = new TypedEnumCache<>(NodeTypes.class);

    public static Set<NodeTypes> all() {
        return NodeTypes.CACHE.values();
    }

    public static NodeTypes byName(String name) {
        return NodeTypes.CACHE.byName(name);
    }

    public static boolean hasName(String name) {
        return NodeTypes.CACHE.hasName(name);
    }

    public static NodeTypes tryParse(String name) {
        return NodeTypes.CACHE.tryParse(name);
    }
}
