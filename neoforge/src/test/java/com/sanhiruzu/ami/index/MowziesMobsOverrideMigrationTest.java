package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves bundled mowziesmobs override data reproduces MowziesMobsCompat's class-based
 * facet tagging WITHOUT referencing MowziesMobsCompat -- survives the plugin's deletion.
 */
class MowziesMobsOverrideMigrationTest {

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
    void dartGainsProjectileFacet() {
        CategoryAssignment a = resolveBare("mowziesmobs:dart",
                meta("mowziesmobs", "com.bobmowzie.mowziesmobs.server.item.ItemDart"));
        assertTrue(hasFacet(a, ItemFacet.PROJECTILE));
    }

    @Test
    void toolItemsGainUtilityToolFacet() {
        CategoryAssignment a = resolveBare("mowziesmobs:bluff_rod",
                meta("mowziesmobs", "com.bobmowzie.mowziesmobs.server.item.ItemBluffRod"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_TOOL));
    }

    @Test
    void artifactItemsGainMagicArtifactFacet() {
        CategoryAssignment a = resolveBare("mowziesmobs:elokosa_paw_full",
                meta("mowziesmobs", "com.bobmowzie.mowziesmobs.server.item.ItemElokosaPaw"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void nagaFangGainsOrganicFacet() {
        CategoryAssignment a = resolveBare("mowziesmobs:naga_fang",
                meta("mowziesmobs", "com.bobmowzie.mowziesmobs.server.item.ItemNagaFang"));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void unmatchedMowziesItemsGainNoOverrideFacets() {
        CategoryAssignment a = resolveBare("mowziesmobs:raw_meat",
                meta("mowziesmobs", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.PROJECTILE));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_TOOL));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
    }
}
