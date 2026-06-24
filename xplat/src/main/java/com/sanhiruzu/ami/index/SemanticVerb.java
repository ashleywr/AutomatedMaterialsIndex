package com.sanhiruzu.ami.index;

import java.util.Locale;

public enum SemanticVerb {
    SLEEP_REST("sleep_rest"),
    STORES_ITEMS("stores_items"),
    SETTLEMENT_WORKSITE("settlement_worksite");

    private final String id;

    SemanticVerb(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static SemanticVerb byId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (SemanticVerb verb : values()) {
            if (verb.id.equals(normalized)) {
                return verb;
            }
        }
        return null;
    }
}
