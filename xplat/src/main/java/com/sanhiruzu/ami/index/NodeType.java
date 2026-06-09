package com.sanhiruzu.ami.index;

import net.minecraft.network.chat.Component;

public enum NodeType {
    ITEM("ami.gui.items"),
    FLUID("ami.gui.fluids"),
    INGREDIENT("ami.gui.ingredients"),
    BIOME("ami.gui.biomes"),
    STRUCTURE("ami.gui.structures"),
    ENTITY("ami.gui.entities"),
    DIMENSION("ami.gui.dimensions"),
    PLAYER("ami.gui.players"),
    WAYPOINT("ami.gui.waypoints");

    private final String translationKey;

    NodeType(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * All browseable types in Tab-cycle order:
     * Items → Fluids → Ingredients → Biomes → Structures → Entities → Dimensions.
     */
    public static NodeType[] atlasValues() {
        return new NodeType[]{ITEM, FLUID, INGREDIENT, BIOME, STRUCTURE, ENTITY, DIMENSION};
    }

    public String translationKey() {
        return translationKey;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    public String tooltipKey() {
        return switch (this) {
            case BIOME -> "ami.tooltip.biome";
            case STRUCTURE -> "ami.tooltip.structure";
            case DIMENSION -> "ami.tooltip.dimension";
            case ENTITY -> "ami.tooltip.entity";
            case PLAYER -> "ami.tooltip.player_subtitle";
            default -> "ami.tooltip.generic";
        };
    }

    /**
     * Cycles through atlasValues().
     */
    public NodeType next() {
        NodeType[] atlas = atlasValues();
        int idx = atlasOrdinal();
        return atlas[(idx + 1) % atlas.length];
    }

    public NodeType prev() {
        NodeType[] atlas = atlasValues();
        int idx = atlasOrdinal();
        return atlas[(idx - 1 + atlas.length) % atlas.length];
    }

    private int atlasOrdinal() {
        NodeType[] atlas = atlasValues();
        for (int i = 0; i < atlas.length; i++) {
            if (atlas[i] == this) return i;
        }
        throw new IllegalStateException("NodeType.next()/prev() called on non-cycleable type: " + this);
    }
}
