package com.sanhiruzu.ami.client;

import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

final class EntityIconSlowKeys {
    static final long SLOW_ENTITY_THRESHOLD_NANOS =
            Math.max(0L, Long.getLong("ami.entityIconSlowThresholdMs", 6L)) * 1_000_000L;

    private static final Set<Identifier> slowKeys = new HashSet<>();

    private EntityIconSlowKeys() {
    }

    static boolean isKnownSlow(Identifier id) {
        return slowKeys.contains(id);
    }

    static void recordBakeElapsed(Identifier id, long nanos) {
        if (nanos > SLOW_ENTITY_THRESHOLD_NANOS) {
            slowKeys.add(id);
        }
    }

    static void clear() {
        slowKeys.clear();
    }
}
