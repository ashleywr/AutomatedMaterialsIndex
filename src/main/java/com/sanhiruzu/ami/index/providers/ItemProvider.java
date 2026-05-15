package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
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
            String variantGroup = getVariantGroup(item);
            String colorBucket  = "gray";
            int color           = 0xFFFFFF;
            String tags         = collectTags(item);
            String requiredTool = determineRequiredTool(item);

            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, modId);
            meta.put(SearchNodeKeys.VARIANT_GROUP, variantGroup);
            meta.put(SearchNodeKeys.COLOR_BUCKET, colorBucket);
            if (!tags.isEmpty()) {
                meta.put(SearchNodeKeys.TAGS, tags);
            }
            if (requiredTool != null) {
                meta.put(SearchNodeKeys.REQUIRED_TOOL, requiredTool);
            }

            index.addNode(new SearchNode(id, NodeType.ITEM, displayName, color, 0, meta));
        }
    }

    @Nullable
    private String determineRequiredTool(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }
        BlockState state = blockItem.getBlock().defaultBlockState();

        String req = null;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) req = "minecraft:diamond_pickaxe";
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) req = "minecraft:iron_pickaxe";
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) req = "minecraft:stone_pickaxe";
            else req = "minecraft:wooden_pickaxe";
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) req = "minecraft:diamond_axe";
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) req = "minecraft:iron_axe";
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) req = "minecraft:stone_axe";
            else req = "minecraft:wooden_axe";
        } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) req = "minecraft:diamond_shovel";
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) req = "minecraft:iron_shovel";
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) req = "minecraft:stone_shovel";
            else req = "minecraft:wooden_shovel";
        } else if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) req = "minecraft:diamond_hoe";
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) req = "minecraft:iron_hoe";
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) req = "minecraft:stone_hoe";
            else req = "minecraft:wooden_hoe";
        }

        if (req != null && BuiltInRegistries.ITEM.getKey(item).getPath().equals("stone")) {
            com.sanhiruzu.ami.AMI.LOGGER.info("AMI DEBUG: Stone required tool is " + req);
        }

        return req;
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
}
