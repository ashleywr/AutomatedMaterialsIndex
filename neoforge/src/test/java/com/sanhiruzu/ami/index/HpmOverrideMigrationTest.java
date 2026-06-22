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
 * Proves bundled hpm override data reproduces HpmCompat's path-token-based facet
 * tagging WITHOUT referencing HpmCompat -- survives the plugin's deletion.
 */
class HpmOverrideMigrationTest {

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
    void cannonballGainsProjectileFacet() {
        CategoryAssignment a = resolveBare("hpm:cannonball",
                meta("hpm", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.PROJECTILE));
    }

    @Test
    void mortarGainsRangedWeaponFacet() {
        CategoryAssignment a = resolveBare("hpm:mortar",
                meta("hpm", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.RANGED_WEAPON));
    }

    @Test
    void hullGainsTransportFacet() {
        CategoryAssignment a = resolveBare("hpm:wooden_hull",
                meta("hpm", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.TRANSPORT));
    }

    @Test
    void pirateTokenRoutesToShipsCategory() {
        CategoryAssignment a = resolveBare("hpm:pirate_flag",
                meta("hpm", "net.minecraft.world.item.Item"));
        assertEquals("hpm", a.categoryId());
        assertEquals("ships", a.subcategoryId());
    }

    @Test
    void unmatchedHpmItemsGainNoOverrideFacets() {
        CategoryAssignment a = resolveBare("hpm:compass",
                meta("hpm", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.PROJECTILE));
        assertFalse(hasFacet(a, ItemFacet.RANGED_WEAPON));
        assertFalse(hasFacet(a, ItemFacet.TRANSPORT));
    }
}
