package com.sanhiruzu.ami.api;

import net.minecraft.resources.ResourceLocation;

/**
 * A single item requirement within a quest group.
 *
 * @param itemId        the registry ID of the required item
 * @param requiredCount how many of this item are needed
 */
public record AmiQuestEntry(ResourceLocation itemId, int requiredCount) {
    public AmiQuestEntry {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId must not be null");
        }
        if (requiredCount <= 0) {
            throw new IllegalArgumentException("requiredCount must be positive, got " + requiredCount);
        }
    }
}
