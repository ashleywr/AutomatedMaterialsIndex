package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.RechiseledCompat;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RechiseledCompatTest {
    @Test
    void generatedBlockVariantsCollapseByRechiseledMaterialTag() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.TAGS, "rechiseled:acacia_planks,rechiseled:acacia_planks_stairs");
        meta.put(SearchNodeKeys.FACETS, "placeable");
        meta.put(SearchNodeKeys.ITEM_CLASS, "com.supermartijn642.rechiseled.registration.RechiseledCommonBlockBuilderImpl$2");
        meta.put(SearchNodeKeys.BLOCK_CLASS, "com.supermartijn642.rechiseled.blocks.RechiseledStairBlock");

        RechiseledCompat.enrichItem(new Identifier("rechiseled:acacia_planks_bricks_stairs_connecting"), meta);

        assertEquals("rechiseled:acacia_planks", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Acacia Planks", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void materialTagDoesNotNeedToBePathPrefix() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.TAGS, "rechiseled:cobblestone");
        meta.put(SearchNodeKeys.FACETS, "placeable");
        meta.put(SearchNodeKeys.ITEM_CLASS, "com.supermartijn642.core.item.BaseBlockItem");
        meta.put(SearchNodeKeys.BLOCK_CLASS, "com.supermartijn642.rechiseled.blocks.RechiseledBlock");

        RechiseledCompat.enrichItem(new Identifier("rechiseled:mossy_cobblestone_stripes"), meta);

        assertEquals("rechiseled:cobblestone", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Cobblestone", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void shapeOnlyTagsAreIgnored() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.TAGS, "rechiseled:cobblestone_stairs");
        meta.put(SearchNodeKeys.FACETS, "placeable");
        meta.put(SearchNodeKeys.ITEM_CLASS, "com.supermartijn642.core.item.BaseBlockItem");
        meta.put(SearchNodeKeys.BLOCK_CLASS, "com.supermartijn642.rechiseled.blocks.RechiseledStairBlock");

        RechiseledCompat.enrichItem(new Identifier("rechiseled:cobblestone_bricks_stairs"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.COLLAPSE_FAMILY));
    }

    @Test
    void rechiseledCreateWindowVariantsCollapseByPathRoot() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.FACETS, "placeable");
        meta.put(SearchNodeKeys.ITEM_CLASS, "com.supermartijn642.rechiseled.registration.RechiseledCommonBlockBuilderImpl$2");
        meta.put(SearchNodeKeys.BLOCK_CLASS, "com.supermartijn642.rechiseled.blocks.RechiseledGlassPillarBlock");

        RechiseledCompat.enrichItem(new Identifier("rechiseledcreate:acacia_window_covered_connecting"), meta);

        assertEquals("rechiseledcreate:acacia_window", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Acacia Windows", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void rechiseledCreateConnectingShapeSuffixesSharePathRoot() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.FACETS, "placeable");
        meta.put(SearchNodeKeys.ITEM_CLASS, "com.supermartijn642.rechiseled.registration.RechiseledCommonBlockBuilderImpl$2");
        meta.put(SearchNodeKeys.BLOCK_CLASS, "com.supermartijn642.rechiseled.blocks.RechiseledStairBlock");

        RechiseledCompat.enrichItem(new Identifier("rechiseledcreate:andesite_cut_polished_stairs_connecting"), meta);

        assertEquals("rechiseledcreate:andesite_cut_polished", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Andesite Cut Polished", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void chiselGetsUtilityToolCategory() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.ITEM_CLASS, "com.supermartijn642.rechiseled.ChiselItem");

        RechiseledCompat.enrichItem(new Identifier("rechiseled:chisel"), meta);

        assertEquals("tools", meta.get(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("utility", meta.get(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }
}
