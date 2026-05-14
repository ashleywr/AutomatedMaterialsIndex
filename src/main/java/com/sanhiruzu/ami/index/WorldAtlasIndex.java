package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldAtlasIndex {
    private static final WorldAtlasIndex INSTANCE = new WorldAtlasIndex();
    
    public enum AtlasType {
        BIOME("Biomes"),
        STRUCTURE("Structures"),
        ENTITY("Entities");

        private final String displayName;

        AtlasType(String displayName) { this.displayName = displayName; }

        public String displayName() { return displayName; }

        public AtlasType next() {
            AtlasType[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private final Map<AtlasType, List<AtlasEntry>> registryData = new HashMap<>();
    private long lastUpdateTime = 0;

    private WorldAtlasIndex() {
        for (AtlasType type : AtlasType.values()) {
            registryData.put(type, new ArrayList<>());
        }
    }

    public static WorldAtlasIndex getInstance() {
        return INSTANCE;
    }

    public void clear() {
        for (List<AtlasEntry> list : registryData.values()) {
            list.clear();
        }
    }

    public void addEntry(AtlasType type, AtlasEntry entry) {
        registryData.get(type).add(entry);
    }

    public List<AtlasEntry> getEntries(AtlasType type) {
        return registryData.get(type);
    }

    public void setLastUpdateTime(long time) {
        this.lastUpdateTime = time;
    }

    public record AtlasEntry(ResourceLocation id, String name, AtlasType type) {}
}
