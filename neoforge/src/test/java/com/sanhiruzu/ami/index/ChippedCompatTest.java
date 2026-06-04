package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.ChippedCompat;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChippedCompatTest {
    @Test
    void generatedBlockVariantsCollapseByChippedMaterialTag() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.TAGS, "minecraft:stone_type/andesite,chipped:andesite");
        meta.put(SearchNodeKeys.BLOCK_TAGS, "chipped:andesite,minecraft:mineable/pickaxe");
        meta.put(SearchNodeKeys.FACETS, "placeable");
        meta.put(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.BlockItem");
        meta.put(SearchNodeKeys.BLOCK_CLASS, "net.minecraft.world.level.block.Block");

        ChippedCompat.enrichItem(new ResourceLocation("chipped:brick_bordered_andesite"), meta);

        assertEquals("chipped:andesite", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Andesite", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void shapeOnlyTagsAreIgnored() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.TAGS, "chipped:oak_planks_stairs");
        meta.put(SearchNodeKeys.FACETS, "placeable");
        meta.put(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.BlockItem");

        ChippedCompat.enrichItem(new ResourceLocation("chipped:ornate_oak_planks_stairs"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.COLLAPSE_FAMILY));
    }

    @Test
    void workbenchItemsGetUtilityCategory() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.ITEM_CLASS, "earth.terrarium.chipped.common.items.WorkbenchItem");

        ChippedCompat.enrichItem(new ResourceLocation("chipped:saw"), meta);

        assertEquals("utility", meta.get(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("misc", meta.get(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void unrelatedNamespacesAreIgnored() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.TAGS, "chipped:andesite");
        meta.put(SearchNodeKeys.FACETS, "placeable");

        ChippedCompat.enrichItem(new ResourceLocation("other:brick_bordered_andesite"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.COLLAPSE_FAMILY));
    }
}
