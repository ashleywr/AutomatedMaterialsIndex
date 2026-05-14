package com.sanhiruzu.ami.index;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class WorldAtlasIndexer {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void index() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            LOGGER.info("Starting World Atlas indexing...");
            WorldAtlasIndex index = WorldAtlasIndex.getInstance();
            index.clear();

            // Index Biomes
            var biomeRegistryOpt = mc.level.registryAccess().registry(Registries.BIOME);
            biomeRegistryOpt.ifPresent(biomeRegistry -> {
                biomeRegistry.entrySet().forEach(entry -> {
                    ResourceLocation id = entry.getKey().location();
                    index.addEntry(WorldAtlasIndex.AtlasType.BIOME, new WorldAtlasIndex.AtlasEntry(id, id.getPath(), WorldAtlasIndex.AtlasType.BIOME));
                });
            });

            // Index Structures
            var structureRegistryOpt = mc.level.registryAccess().registry(Registries.STRUCTURE);
            structureRegistryOpt.ifPresent(structureRegistry -> {
                structureRegistry.entrySet().forEach(entry -> {
                    ResourceLocation id = entry.getKey().location();
                    index.addEntry(WorldAtlasIndex.AtlasType.STRUCTURE, new WorldAtlasIndex.AtlasEntry(id, id.getPath(), WorldAtlasIndex.AtlasType.STRUCTURE));
                });
            });

            // Index Entities
            BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(entry -> {
                ResourceLocation id = entry.getKey().location();
                index.addEntry(WorldAtlasIndex.AtlasType.ENTITY, new WorldAtlasIndex.AtlasEntry(id, id.getPath(), WorldAtlasIndex.AtlasType.ENTITY));
            });

            index.setLastUpdateTime(System.currentTimeMillis());
            LOGGER.info("World Atlas indexing complete: {} biomes, {} structures, {} entities", 
                index.getEntries(WorldAtlasIndex.AtlasType.BIOME).size(),
                index.getEntries(WorldAtlasIndex.AtlasType.STRUCTURE).size(),
                index.getEntries(WorldAtlasIndex.AtlasType.ENTITY).size());
        } catch (Exception e) {
            LOGGER.error("Failed to index World Atlas registries", e);
        }
    }
}
