package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DoggyTalentsOverrideMigrationTest {

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

    private static CategoryAssignment resolveBare(String id, Map<String, String> meta) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta);
    }

    private static boolean hasFacet(CategoryAssignment a, ItemFacet facet) {
        return a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(facet.id());
    }

    @Test
    void treatItemsRouteToFoodPrepared() {
        CategoryAssignment a = resolveBare("doggytalents:training_treat",
                meta("doggytalents", "doggytalents.common.item.TreatItem"));
        assertEquals("food", a.categoryId());
        assertEquals("prepared", a.subcategoryId());
    }

    @Test
    void trackerItemsGainNavigationFacet() {
        CategoryAssignment a = resolveBare("doggytalents:canine_tracker",
                meta("doggytalents", "doggytalents.common.item.CanineTrackerItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_NAVIGATION));
    }

    @Test
    void toyItemsGainUtilityToolFacet() {
        CategoryAssignment a = resolveBare("doggytalents:frisbee",
                meta("doggytalents", "doggytalents.common.item.FrisbeeItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_TOOL));
    }

    @Test
    void decorItemsRouteToFurniture() {
        CategoryAssignment a = resolveBare("doggytalents:grand_piano_white_item",
                meta("doggytalents", "doggytalents.common.item.PianoItem"));
        assertEquals("decoration", a.categoryId());
        assertEquals("furniture", a.subcategoryId());
    }

    @Test
    void accessoryItemsGainCurioFacet() {
        // class contains the accessory package path token
        CategoryAssignment a = resolveBare("doggytalents:flatcap",
                meta("doggytalents", "doggytalents.common.entity.accessory.FlatCap$FlatCapItem"));
        assertTrue(hasFacet(a, ItemFacet.CURIO));
    }

    @Test
    void unmatchedItemsGainNoFacet() {
        CategoryAssignment a = resolveBare("doggytalents:generic_item",
                meta("doggytalents", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.CURIO));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_NAVIGATION));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_TOOL));
    }
}
