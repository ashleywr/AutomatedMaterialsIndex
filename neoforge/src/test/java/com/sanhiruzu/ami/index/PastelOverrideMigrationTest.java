package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PastelOverrideMigrationTest {

    @BeforeEach
    void installBundled() {
        ClassificationOverrides.loadBundledDefaults();
    }

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> m = new HashMap<>();
        m.put(SearchNodeKeys.MOD_ID, modId);
        m.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return m;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta);
    }

    @Test
    void structurePlacerClassCollapses() {
        var meta = meta("pastel", "me.iris.pastel.item.StructurePlacerItem");
        var a = resolve("pastel:structure_placer_oak_house", meta);
        assertEquals("pastel", a.categoryId());
        assertEquals("structure_placers", a.subcategoryId());
        assertEquals("pastel:structure_placers", a.attributes().get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Structure Placers", a.attributes().get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", a.attributes().get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void structurePlacerPathAlsoCollapses() {
        var meta = meta("pastel", "net.minecraft.world.item.Item");
        var a = resolve("pastel:structure_placer_stone_tower", meta);
        assertEquals("structure_placers", a.subcategoryId());
        assertNotNull(a.attributes().get(SearchNodeKeys.COLLAPSE_FAMILY));
    }

    @Test
    void brushClassRoutesToTools() {
        var meta = meta("pastel", "me.iris.pastel.item.PastelBrushItem");
        var a = resolve("pastel:pastel_brush", meta);
        assertEquals("pastel", a.categoryId());
        assertEquals("tools", a.subcategoryId());
    }

    @Test
    void brushPathRoutesToTools() {
        var meta = meta("pastel", "net.minecraft.world.item.Item");
        var a = resolve("pastel:paint_brush", meta);
        assertEquals("tools", a.subcategoryId());
    }

    @Test
    void dyeClassRoutesToDyes() {
        var meta = meta("pastel", "me.iris.pastel.item.PastelDyeItem");
        var a = resolve("pastel:pastel_red_dye", meta);
        assertEquals("pastel", a.categoryId());
        assertEquals("dyes", a.subcategoryId());
    }

    @Test
    void dyePathRoutesToDyes() {
        var meta = meta("pastel", "net.minecraft.world.item.Item");
        var a = resolve("pastel:blue_dye", meta);
        assertEquals("dyes", a.subcategoryId());
    }

    @Test
    void resourceFamilyRoutesToPastelResources() {
        var meta = meta("pastel", "earth.terrarium.pastel.items.ItemWithLoomPattern");
        var a = resolve("pastel:raw_azurite", meta);
        assertEquals("pastel", a.categoryId());
        assertEquals("resources", a.subcategoryId());
    }

    @Test
    void catalogueClassRoutesToMisc() {
        var meta = meta("pastel", "me.iris.pastel.item.CatalogueItem");
        var a = resolve("pastel:pastel_catalogue", meta);
        assertEquals("pastel", a.categoryId());
        assertEquals("misc", a.subcategoryId());
    }

    @Test
    void slabPathRoutesToBuilding() {
        var meta = meta("pastel", "net.minecraft.world.item.BlockItem");
        var a = resolve("pastel:oak_plank_slab", meta);
        assertEquals("pastel", a.categoryId());
        assertEquals("building", a.subcategoryId());
    }

    @Test
    void blockPathRoutesToBuilding() {
        var meta = meta("pastel", "net.minecraft.world.item.BlockItem");
        var a = resolve("pastel:marble_block", meta);
        assertEquals("building", a.subcategoryId());
    }

    @Test
    void tilePathRoutesToBuilding() {
        var meta = meta("pastel", "net.minecraft.world.item.BlockItem");
        var a = resolve("pastel:terracotta_tile", meta);
        assertEquals("building", a.subcategoryId());
    }

    @Test
    void stairsPathRoutesToBuilding() {
        var meta = meta("pastel", "net.minecraft.world.item.BlockItem");
        var a = resolve("pastel:oak_plank_stairs", meta);
        assertEquals("building", a.subcategoryId());
    }

    @Test
    void lampPathRoutesToBuilding() {
        var meta = meta("pastel", "net.minecraft.world.item.BlockItem");
        var a = resolve("pastel:pastel_lamp", meta);
        assertEquals("building", a.subcategoryId());
    }
}
