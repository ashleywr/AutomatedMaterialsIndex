package com.sanhiruzu.ami.index;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldAtlasIndex {
    private static final WorldAtlasIndex INSTANCE = new WorldAtlasIndex();
    
    public enum AtlasType {
        BIOME("ami.gui.biomes"),
        STRUCTURE("ami.gui.structures"),
        ENTITY("ami.gui.entities"),
        DIMENSION("ami.gui.dimensions");

        private final String translationKey;

        AtlasType(String translationKey) { this.translationKey = translationKey; }

        public String translationKey() { return translationKey; }
        public Component displayName() { return Component.translatable(translationKey); }

        public AtlasType next() {
            AtlasType[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private final Map<AtlasType, List<AtlasEntry>> registryData = new HashMap<>();
    private final java.util.Set<AtlasType> loadingTypes = new java.util.HashSet<>();
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

    public void setLoading(AtlasType type, boolean loading) {
        if (loading) {
            loadingTypes.add(type);
        } else {
            loadingTypes.remove(type);
        }
    }

    public boolean isLoading(AtlasType type) {
        return loadingTypes.contains(type);
    }

    public enum Dimension {
        OVERWORLD("ami.dimension.overworld"),
        NETHER("ami.dimension.nether"),
        END("ami.dimension.end");

        private final String translationKey;

        Dimension(String translationKey) { this.translationKey = translationKey; }

        public String translationKey() { return translationKey; }
        public Component displayName() { return Component.translatable(translationKey); }
    }

    public record AtlasEntry(ResourceLocation id, String name, AtlasType type, int color, Dimension dimension) {}
}
