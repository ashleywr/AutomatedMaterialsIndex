package com.sanhiruzu.ami.client.sources;

public enum ItemSourceType {
    MOB_DROP("Mob Drop"),
    RECIPE("Recipes"),
    PROCESSING("Processing"),
    INDIRECT_SOURCE("Indirect Source"),
    SALVAGE("Salvage"),
    STRUCTURE_LOOT("Structure Loot"),
    TRADE("Trade");

    private final String label;

    ItemSourceType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
