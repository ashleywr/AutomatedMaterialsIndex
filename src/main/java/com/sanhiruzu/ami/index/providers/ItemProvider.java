package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Populates the GlobalIndex with all items from BuiltInRegistries.ITEM.
 * Port of Indexer.java logic.
 */
public class ItemProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable ClientLevel level) {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || id.getNamespace().equals("air")) continue;

            String modId        = id.getNamespace();
            String displayName  = item.getName(new ItemStack(item)).getString();
            String tier         = determineTier(item).name();
            String variantGroup = getVariantGroup(item);
            String colorBucket  = "gray";
            int color           = 0xFFFFFF;
            String tags         = collectTags(item);

            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, modId);
            meta.put(SearchNodeKeys.TIER, tier);
            meta.put(SearchNodeKeys.VARIANT_GROUP, variantGroup);
            meta.put(SearchNodeKeys.COLOR_BUCKET, colorBucket);
            if (!tags.isEmpty()) {
                meta.put(SearchNodeKeys.TAGS, tags);
            }

            index.addNode(new SearchNode(id, NodeType.ITEM, displayName, color, 0, meta));
        }
    }

    private MaterialTier determineTier(Item item) {
        if (item.builtInRegistryHolder().is(ItemTags.create(ResourceLocation.parse("c:netherite"))))
            return MaterialTier.NETHERITE;
        if (item.builtInRegistryHolder().is(ItemTags.create(ResourceLocation.parse("c:diamonds"))))
            return MaterialTier.DIAMOND;
        if (item.builtInRegistryHolder().is(ItemTags.create(ResourceLocation.parse("c:ingots"))))
            return MaterialTier.IRON;
        if (item.builtInRegistryHolder().is(ItemTags.create(ResourceLocation.parse("c:raw_materials"))))
            return MaterialTier.STONE;
        if (item.builtInRegistryHolder().is(ItemTags.create(ResourceLocation.parse("c:gems"))))
            return MaterialTier.DIAMOND;
        return MaterialTier.MODDED;
    }

    private String getVariantGroup(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (path.contains("_stair")) return "stair";
        if (path.contains("_slab")) return "slab";
        if (path.contains("_wall")) return "wall";
        if (path.contains("_door")) return "door";
        if (path.contains("_trapdoor")) return "trapdoor";
        if (path.contains("_fence")) return "fence";
        if (path.contains("_fence_gate")) return "fence_gate";
        if (path.contains("_button")) return "button";
        if (path.contains("_pressure_plate")) return "pressure_plate";
        return "block";
    }

    private String collectTags(Item item) {
        return item.builtInRegistryHolder().tags()
            .map(tag -> tag.location().toString().toLowerCase())
            .collect(Collectors.joining(","));
    }

    enum MaterialTier {
        WOOD, STONE, IRON, GOLD, DIAMOND, NETHERITE, MODDED
    }
}
