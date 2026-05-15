package com.sanhiruzu.ami.index;

/**
 * String constants for SearchNode.metadata() keys.
 */
public final class SearchNodeKeys {
    private SearchNodeKeys() {}

    // Shared
    public static final String MOD_ID          = "modId";

    // Item-specific
    public static final String TIER            = "tier";
    public static final String VARIANT_GROUP   = "variantGroup";
    public static final String COLOR_BUCKET    = "colorBucket";
    public static final String TAGS            = "tags";  // Comma-separated tag paths
    public static final String ESM_CAPACITY    = "emsCapacity";  // Equivalent Stack Metric

    // Atlas-specific
    public static final String DIMENSION       = "dimension";
    public static final String ENTITY_CATEGORY = "entityCategory";
    public static final String FIRE_IMMUNE     = "fireImmune";

    // Player-specific (transient, never persisted)
    public static final String PLAYER_UUID     = "playerUuid";
}
