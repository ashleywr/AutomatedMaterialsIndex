package com.sanhiruzu.ami.index;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Client-side indexer for AMI.
 * Runs once on client load to categorize items by color, mod origin, and material tier.
 */
public class Indexer {
    private static final org.slf4j.Logger LOGGER = com.sanhiruzu.ami.AMI.LOGGER;

    public static void index() {
        LOGGER.info("Starting AMI indexing pipeline (client-side)...");
        long startTime = System.currentTimeMillis();
        AMIIndex index = AMIIndex.getInstance();
        index.clear();

        int itemCount = 0;
        Set<String> mods = new HashSet<>();

        LOGGER.debug("Scanning item registry...");
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null) continue;

            ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
            if (registryName == null || registryName.getNamespace().equals("air")) continue;

            itemCount++;
            String modId = registryName.getNamespace();
            mods.add(modId);

            String colorBucket = computeColorBucket(item);
            MaterialEntry.MaterialTier tier = determineTier(item);
            String variantGroup = getVariantGroup(item);
            int dominantColor = 0xFFFFFF;

            MaterialEntry entry = new MaterialEntry(
                    item,
                    modId,
                    dominantColor,
                    colorBucket,
                    tier,
                    variantGroup
            );

            addToIndex(entry, index);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        index.setTotalItemsIndexed(itemCount);
        index.setIndexBuildTime(duration);

        LOGGER.info("✓ AMI indexing complete: {} items from {} mods in {}ms", itemCount, mods.size(), duration);
        LOGGER.debug("Indexed mods: {}", mods);
    }

    private static void addToIndex(MaterialEntry entry, AMIIndex index) {
        index.getCategoryIndex(IndexCategory.BY_COLOR)
                .computeIfAbsent(entry.colorBucket(), k -> new ArrayList<>())
                .add(entry);

        index.getCategoryIndex(IndexCategory.BY_MOD)
                .computeIfAbsent(entry.modId(), k -> new ArrayList<>())
                .add(entry);

        index.getCategoryIndex(IndexCategory.BY_TIER)
                .computeIfAbsent(entry.materialTier().getDisplayName(), k -> new ArrayList<>())
                .add(entry);

        index.getCategoryIndex(IndexCategory.BY_VARIANT_GROUP)
                .computeIfAbsent(entry.variantGroup(), k -> new ArrayList<>())
                .add(entry);
    }

    private static String computeColorBucket(Item item) {
        return "gray";
    }

    private static MaterialEntry.MaterialTier determineTier(Item item) {
        if (hasTag(item, "c:ingots") || hasTag(item, "c:gems") || hasTag(item, "c:dusts")) {
            if (hasTag(item, "c:netherite")) return MaterialEntry.MaterialTier.NETHERITE;
            if (hasTag(item, "c:diamonds")) return MaterialEntry.MaterialTier.DIAMOND;
            if (hasTag(item, "c:gold_ores") || hasTag(item, "c:gold_ingots")) return MaterialEntry.MaterialTier.GOLD;
            if (hasTag(item, "c:iron_ores") || hasTag(item, "c:iron_ingots")) return MaterialEntry.MaterialTier.IRON;
            if (hasTag(item, "c:stone")) return MaterialEntry.MaterialTier.STONE;
            if (hasTag(item, "c:wood_logs")) return MaterialEntry.MaterialTier.WOOD;
            return MaterialEntry.MaterialTier.MODDED;
        }

        if (hasTag(item, "c:ores")) return MaterialEntry.MaterialTier.STONE;
        if (hasTag(item, "c:wood_logs")) return MaterialEntry.MaterialTier.WOOD;

        return MaterialEntry.MaterialTier.MODDED;
    }

    private static boolean hasTag(Item item, String tagNamespace) {
        ResourceLocation tagId = ResourceLocation.parse(tagNamespace);
        TagKey<Item> tagKey = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        if (itemKey == null) return false;
        Holder<Item> holder = BuiltInRegistries.ITEM.getHolder(itemKey).orElse(null);
        if (holder == null) return false;
        return holder.is(tagKey);
    }

    private static String getVariantGroup(Item item) {
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
        if (registryName == null) return "unknown";

        String name = registryName.getPath();
        if (name.contains("_stair")) return "stair";
        if (name.contains("_slab")) return "slab";
        if (name.contains("_pillar")) return "pillar";
        if (name.contains("_wall")) return "wall";
        if (name.contains("_fence")) return "fence";
        if (name.contains("_door")) return "door";
        if (name.contains("_trapdoor")) return "trapdoor";
        if (name.contains("_button")) return "button";
        if (name.contains("_pressure_plate")) return "pressure_plate";

        return "block";
    }
}
