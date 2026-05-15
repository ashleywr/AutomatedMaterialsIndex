package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/**
 * Unified data node replacing both MaterialEntry and AtlasEntry.
 * Metadata map uses String keys from SearchNodeKeys.
 */
public record SearchNode(
    ResourceLocation id,
    NodeType type,
    String displayName,
    int color,
    int searchWeight,
    Map<String, String> metadata
) {
    /**
     * Convenience: read a metadata key with a default.
     */
    public String meta(String key, String defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }

    public String meta(String key) {
        return meta(key, "");
    }
}
