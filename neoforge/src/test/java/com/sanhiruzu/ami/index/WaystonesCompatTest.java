package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WaystonesCompatTest {
    @Test
    void teleportTargetBlocksRouteToMagicArtifacts() {
        Map<String, String> meta = meta();
        meta.put(SearchNodeKeys.BLOCK_CLASS, "net.blay09.mods.waystones.block.WaystoneBlock");
        meta.put(SearchNodeKeys.BLOCK_TAGS, "minecraft:mineable/pickaxe,waystones:is_teleport_target,waystones:waystones");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY)));

        CategoryAssignment assignment = resolve("waystones:waystone", meta,
                ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY);

        assertEquals("magic", assignment.categoryId());
        assertEquals("artifacts", assignment.subcategoryId());
    }

    @Test
    void warpStoneRoutesToMagicArtifacts() {
        Map<String, String> meta = meta();
        meta.put(SearchNodeKeys.ITEM_CLASS, "net.blay09.mods.waystones.item.WarpStoneItem");

        CategoryAssignment assignment = resolve("waystones:warp_stone", meta);

        assertEquals("magic", assignment.categoryId());
        assertEquals("artifacts", assignment.subcategoryId());
    }

    @Test
    void warpDustRoutesToMagicReagents() {
        Map<String, String> meta = meta();
        meta.put(SearchNodeKeys.ITEM_CLASS, "net.blay09.mods.waystones.item.WarpDustItem");

        CategoryAssignment assignment = resolve("waystones:warp_dust", meta);

        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void partialWordPathsDoNotRouteAsWaystonesArtifactsOrReagents() {
        Map<String, String> sharded = meta();
        Map<String, String> scrollwork = meta();

        CategoryAssignment shardedAssignment = resolve("waystones:sharded_tablet", sharded);
        CategoryAssignment scrollworkAssignment = resolve("waystones:scrollwork_banner", scrollwork);

        assertNotEquals("magic", shardedAssignment.categoryId());
        assertNotEquals("magic", scrollworkAssignment.categoryId());
    }

    private static Map<String, String> meta() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "waystones");
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
