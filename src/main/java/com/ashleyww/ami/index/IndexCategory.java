package com.ashleyww.ami.index;

public enum IndexCategory {
    BY_COLOR("Color"),
    BY_MOD("Mod"),
    BY_TIER("Tier"),
    BY_VARIANT_GROUP("Material");

    private final String displayName;

    IndexCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
