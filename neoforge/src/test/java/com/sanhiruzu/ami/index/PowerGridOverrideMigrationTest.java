package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PowerGridOverrideMigrationTest {

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
    void componentItemsGainTechComponentFacet() {
        // "resistor" path token
        CategoryAssignment a = resolveBare("powergrid:resistor",
                meta("powergrid", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.TECH_COMPONENT));
    }

    @Test
    void toolsGainUtilityToolFacet() {
        CategoryAssignment a = resolveBare("powergrid:multimeter",
                meta("powergrid", "org.patryk3211.powergrid.equipment.multimeter.MultimeterItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_TOOL));
    }

    @Test
    void punchCardGainsBothFacets() {
        CategoryAssignment a = resolveBare("powergrid:punch_card",
                meta("powergrid", "org.patryk3211.powergrid.kinetics.punchcard.PunchCardItem"));
        assertTrue(hasFacet(a, ItemFacet.TECH_COMPONENT));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_MISC));
    }

    @Test
    void unmatchedItemsGainNoFacet() {
        CategoryAssignment a = resolveBare("powergrid:motor",
                meta("powergrid", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.TECH_COMPONENT));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_TOOL));
    }
}
