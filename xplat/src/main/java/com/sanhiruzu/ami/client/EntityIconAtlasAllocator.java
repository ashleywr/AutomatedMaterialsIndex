package com.sanhiruzu.ami.client;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

final class EntityIconAtlasAllocator {
    private final int atlasSize;
    private final int iconSize;
    private final int columns;
    private final int maxSlots;
    private final Map<ResourceLocation, AtlasEntry> entries = new LinkedHashMap<>();

    EntityIconAtlasAllocator(int atlasSize, int iconSize) {
        if (atlasSize <= 0 || iconSize <= 0) {
            throw new IllegalArgumentException("Atlas and icon sizes must be positive");
        }
        this.atlasSize = atlasSize;
        this.iconSize = iconSize;
        this.columns = atlasSize / iconSize;
        this.maxSlots = columns * columns;
    }

    AtlasEntry entry(ResourceLocation id) {
        return entries.get(id);
    }

    AtlasEntry allocate(ResourceLocation id) {
        AtlasEntry existing = entries.get(id);
        if (existing != null) {
            return existing;
        }
        int slot = entries.size();
        if (columns <= 0 || slot >= maxSlots) {
            return null;
        }
        AtlasEntry entry = new AtlasEntry((slot % columns) * iconSize, (slot / columns) * iconSize);
        entries.put(id, entry);
        return entry;
    }

    int entryCount() {
        return entries.size();
    }

    int maxSlots() {
        return maxSlots;
    }

    Map<ResourceLocation, AtlasEntry> entries() {
        return Map.copyOf(entries);
    }

    record AtlasEntry(int x, int y) {
    }
}
