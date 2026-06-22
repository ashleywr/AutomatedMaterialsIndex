package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the bundled cnc override data reproduces CncCompat's facet tagging WITHOUT referencing
 * CncCompat -- so it survives the plugin's deletion. While CncCompat still exists,
 * SynesthesiaCompatTest pins plugin -> the same literal categories; two green tests sharing the
 * literals = equivalence locked.
 */
class CncOverrideMigrationTest {

    @BeforeEach
    void installBundled() {
        ClassificationOverrides.loadBundledDefaults();
    }

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return meta;
    }

    private static CategoryAssignment resolveBare(String id, Map<String, String> meta) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta);
    }

    private static boolean hasFacet(CategoryAssignment a, ItemFacet facet) {
        return a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(facet.id());
    }

    @Test
    void organicSingleTokenItemsGainOrganicFacetAndCategory() {
        CategoryAssignment buckskin = resolveBare("cnc:buckskin",
                meta("cnc", "net.imasillylittleguy.cnc.item.DeerLeatherItem"));
        assertTrue(hasFacet(buckskin, ItemFacet.INGREDIENT_ORGANIC));
        assertEquals("ingredients", buckskin.categoryId());
        assertEquals("organic", buckskin.subcategoryId());

        CategoryAssignment wishbone = resolveBare("cnc:wishbone",
                meta("cnc", "net.imasillylittleguy.cnc.item.WishboneItem"));
        assertTrue(hasFacet(wishbone, ItemFacet.INGREDIENT_ORGANIC));
        assertEquals("ingredients", wishbone.categoryId());
        assertEquals("organic", wishbone.subcategoryId());
    }

    @Test
    void organicMultiComponentItemsMatchByToken() {
        // elk_antler -> tokens [elk, antler]; token "antler" must match
        CategoryAssignment antler = resolveBare("cnc:elk_antler",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(antler, ItemFacet.INGREDIENT_ORGANIC));

        // tusk_club -> tokens [tusk, club]; token "tusk" must match
        CategoryAssignment tusk = resolveBare("cnc:tusk_club",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(tusk, ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void rawTurkeyGainsOrganicViaPerItemEntry() {
        CategoryAssignment turkey = resolveBare("cnc:raw_turkey",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(turkey, ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void artifactItemsGainArtifactFacet() {
        CategoryAssignment pot = resolveBare("cnc:potofmouse",
                meta("cnc", "net.imasillylittleguy.cnc.item.PotofmouseItem"));
        assertTrue(hasFacet(pot, ItemFacet.MAGIC_ARTIFACT));
        assertEquals("magic", pot.categoryId());
        assertEquals("artifacts", pot.subcategoryId());

        // kill_stick -> tokens [kill, stick]; token "kill" must match
        CategoryAssignment killStick = resolveBare("cnc:kill_stick",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(killStick, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void unmatchedCncItemsGainNeitherFacet() {
        CategoryAssignment effigy = resolveBare("cnc:effigy",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(effigy, ItemFacet.INGREDIENT_ORGANIC));
        assertFalse(hasFacet(effigy, ItemFacet.MAGIC_ARTIFACT));

        // cooked_turkey must NOT be caught (raw_turkey was per-item; token "turkey" deliberately unused)
        CategoryAssignment cookedTurkey = resolveBare("cnc:cooked_turkey",
                meta("cnc", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(cookedTurkey, ItemFacet.INGREDIENT_ORGANIC));
    }
}
