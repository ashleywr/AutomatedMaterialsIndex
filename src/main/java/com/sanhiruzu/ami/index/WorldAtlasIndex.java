package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldAtlasIndex {
    private static final WorldAtlasIndex INSTANCE = new WorldAtlasIndex();
    
    public enum AtlasType {
        BIOME, STRUCTURE, ENTITY
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
