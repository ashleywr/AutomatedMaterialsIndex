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

    // Atlas-specific
    public static final String DIMENSION       = "dimension";
    public static final String ENTITY_CATEGORY = "entityCategory";
    public static final String FIRE_IMMUNE     = "fireImmune";
}
