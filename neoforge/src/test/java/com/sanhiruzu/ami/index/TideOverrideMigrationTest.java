package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TideOverrideMigrationTest {

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
    void fishingHookGainsUtilityToolFacet() {
        CategoryAssignment a = resolveBare("tide:fishing_hook",
                meta("tide", "com.li64.tide.registries.items.FishingHookItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_TOOL));
    }

    @Test
    void navigationGadgetsGainNavigationFacet() {
        CategoryAssignment a = resolveBare("tide:depth_meter",
                meta("tide", "com.li64.tide.registries.items.DepthMeterItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_NAVIGATION));
    }

    @Test
    void journalGainsBookFacets() {
        CategoryAssignment a = resolveBare("tide:fishing_journal",
                meta("tide", "com.li64.tide.registries.items.FishingJournalItem"));
        assertTrue(hasFacet(a, ItemFacet.BOOK));
        assertTrue(hasFacet(a, ItemFacet.GUIDE_BOOK));
    }

    @Test
    void unmatchedItemsGainNoFacet() {
        CategoryAssignment a = resolveBare("tide:cooked_fish",
                meta("tide", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_TOOL));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_NAVIGATION));
    }
}
