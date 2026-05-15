package com.sanhiruzu.ami.index;

import net.minecraft.network.chat.Component;

public enum NodeType {
    ITEM("ami.gui.items"),
    BIOME("ami.gui.biomes"),
    STRUCTURE("ami.gui.structures"),
    ENTITY("ami.gui.entities"),
    DIMENSION("ami.gui.dimensions"),
    PLAYER("ami.gui.players");

    private final String translationKey;

    NodeType(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    /**
     * Cycles through BIOME → STRUCTURE → ENTITY → DIMENSION → BIOME (never includes ITEM).
     * Matches the former AtlasType.next() contract exactly.
     * Throws IllegalStateException if called on ITEM.
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

    /**
     * Atlas-only values (excludes ITEM). Use for tab cycling in the overlay.
     */
    public static NodeType[] atlasValues() {
        return new NodeType[]{BIOME, STRUCTURE, ENTITY, DIMENSION};
    }

    private int atlasOrdinal() {
        NodeType[] atlas = atlasValues();
        for (int i = 0; i < atlas.length; i++) {
            if (atlas[i] == this) return i;
        }
        throw new IllegalStateException("NodeType.next()/prev() called on non-atlas type: " + this);
    }
}
