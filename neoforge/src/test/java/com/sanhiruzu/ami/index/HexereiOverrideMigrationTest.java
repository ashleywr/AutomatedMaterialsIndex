package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HexereiOverrideMigrationTest {

    @BeforeEach
    void installBundled() { ClassificationOverrides.loadBundledDefaults(); }

    @AfterEach
    void reset() { ClassificationOverrides.clear(); }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> m = new HashMap<>();
        m.put(SearchNodeKeys.MOD_ID, modId);
        m.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return m;
    }

    private static CategoryAssignment resolveBare(String id, Map<String, String> meta) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta);
    }

    private static boolean hasFacet(CategoryAssignment a, ItemFacet facet) {
        return a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(facet.id());
    }

    @Test
    void broomItemsGainTransportFacet() {
        // anonymous inner class for broom — matched by path token "broom"
        CategoryAssignment a = resolveBare("hexerei:mahogany_broom",
                meta("hexerei", "net.joefoxe.hexerei.item.ModItems$4"));
        assertTrue(hasFacet(a, ItemFacet.TRANSPORT));
    }

    @Test
    void herbItemsGainOrganicFacet() {
        // "sage" in path — organic reagent
        CategoryAssignment a = resolveBare("hexerei:sage",
                meta("hexerei", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void sigilItemsGainArtifactFacet() {
        // "sigil" in path — magic artifact
        CategoryAssignment a = resolveBare("hexerei:blood_sigil",
                meta("hexerei", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void dowsingRodGainsNavigationFacet() {
        CategoryAssignment a = resolveBare("hexerei:dowsing_rod",
                meta("hexerei", "net.joefoxe.hexerei.item.custom.DowsingRodItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_NAVIGATION));
    }

    @Test
    void unmatchedItemsGainNoFacet() {
        CategoryAssignment a = resolveBare("hexerei:candle",
                meta("hexerei", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.TRANSPORT));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
    }
}
