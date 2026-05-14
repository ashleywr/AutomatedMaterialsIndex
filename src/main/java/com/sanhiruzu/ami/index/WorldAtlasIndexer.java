package com.sanhiruzu.ami.index;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.fml.ModList;

public class WorldAtlasIndexer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Comparator<WorldAtlasIndex.AtlasEntry> ENTRY_ORDER =
            Comparator.comparing((WorldAtlasIndex.AtlasEntry e) -> e.id().getNamespace().equals("minecraft") ? 0 : 1)
                      .thenComparing(e -> e.id().getNamespace())
                      .thenComparing(WorldAtlasIndex.AtlasEntry::name);

    public static void index(ClientLevel level) {
        try {
            LOGGER.info("Starting World Atlas indexing...");
            WorldAtlasIndex index = WorldAtlasIndex.getInstance();
            index.clear();

            // Biomes — use holders() to access tags for dimension detection
            level.registryAccess().registry(Registries.BIOME).ifPresent(biomeRegistry ->
                biomeRegistry.holders().forEach(holder -> {
                    ResourceLocation id = holder.key().location();
                    int waterColor = holder.value().getSpecialEffects().getWaterColor();

                    WorldAtlasIndex.Dimension dimension = WorldAtlasIndex.Dimension.OVERWORLD;
                    if (holder.is(BiomeTags.IS_NETHER)) dimension = WorldAtlasIndex.Dimension.NETHER;
                    else if (holder.is(BiomeTags.IS_END)) dimension = WorldAtlasIndex.Dimension.END;

                    index.addEntry(WorldAtlasIndex.AtlasType.BIOME, new WorldAtlasIndex.AtlasEntry(
                            id, formatPath(id.getPath()), WorldAtlasIndex.AtlasType.BIOME,
                            0xFF000000 | waterColor, dimension));
                })
            );

            // Structures
            var structureOpt = level.registryAccess().registry(Registries.STRUCTURE);
            LOGGER.debug("Structure registry present: {}, size: {}",
                    structureOpt.isPresent(), structureOpt.map(r -> r.size()).orElse(0));
            structureOpt.ifPresent(structureRegistry ->
                structureRegistry.entrySet().forEach(entry -> {
                    ResourceLocation id = entry.getKey().location();
                    index.addEntry(WorldAtlasIndex.AtlasType.STRUCTURE, new WorldAtlasIndex.AtlasEntry(
                            id, formatPath(id.getPath()), WorldAtlasIndex.AtlasType.STRUCTURE,
                            namespaceColor(id.getNamespace()), WorldAtlasIndex.Dimension.OVERWORLD));
                })
            );

            // Entities
            BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(entry -> {
                ResourceLocation id = entry.getKey().location();
                MobCategory category = entry.getValue().getCategory();
                index.addEntry(WorldAtlasIndex.AtlasType.ENTITY, new WorldAtlasIndex.AtlasEntry(
                        id, formatPath(id.getPath()), WorldAtlasIndex.AtlasType.ENTITY,
                        categoryColor(category), WorldAtlasIndex.Dimension.OVERWORLD));
            });

            for (WorldAtlasIndex.AtlasType type : WorldAtlasIndex.AtlasType.values()) {
                index.getEntries(type).sort(ENTRY_ORDER);
            }

            index.setLastUpdateTime(System.currentTimeMillis());
            LOGGER.info("World Atlas indexed: {} biomes, {} structures, {} entities",
                    index.getEntries(WorldAtlasIndex.AtlasType.BIOME).size(),
                    index.getEntries(WorldAtlasIndex.AtlasType.STRUCTURE).size(),
                    index.getEntries(WorldAtlasIndex.AtlasType.ENTITY).size());
        } catch (Exception e) {
            LOGGER.error("Failed to index World Atlas", e);
        }
    }

    /** Resolves a namespace to a human-readable mod name, falling back to title-cased namespace. */
    public static String modDisplayName(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(mc -> mc.getModInfo().getDisplayName())
                .orElse(formatPath(namespace));
    }

    /** "dark_forest" → "Dark Forest" */
    public static String formatPath(String path) {
        return Arrays.stream(path.split("_"))
                .map(word -> word.isEmpty() ? word
                        : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static int namespaceColor(String namespace) {
        int hash = namespace.hashCode();
        int r = 128 + ((hash >> 16) & 0x7F);
        int g = 128 + ((hash >> 8) & 0x7F);
        int b = 128 + (hash & 0x7F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int categoryColor(MobCategory category) {
        return switch (category) {
            case MONSTER              -> 0xFFCC4444;
            case CREATURE             -> 0xFF44AA44;
            case AMBIENT              -> 0xFFAAAA44;
            case WATER_CREATURE,
                 WATER_AMBIENT,
                 UNDERGROUND_WATER_CREATURE -> 0xFF4488CC;
            default                   -> 0xFF888888;
        };
    }
}
