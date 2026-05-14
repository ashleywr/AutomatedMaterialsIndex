package com.ashleyww.ami.index;

import net.minecraft.world.item.Item;

public record MaterialEntry(
        Item item,
        String modId,
        int dominantColor,
        String colorBucket,
        MaterialTier materialTier,
        String variantGroup
) {
    public enum MaterialTier {
        WOOD("Wood", 0),
        STONE("Stone", 1),
        IRON("Iron", 2),
        GOLD("Gold", 3),
        DIAMOND("Diamond", 4),
        NETHERITE("Netherite", 5),
        MODDED("Modded", 6);

        private final String displayName;
        private final int sortOrder;

        MaterialTier(String displayName, int sortOrder) {
            this.displayName = displayName;
            this.sortOrder = sortOrder;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getSortOrder() {
            return sortOrder;
        }
    }
}
