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
 * Proves bundled cgs override data reproduces CgsCompat's class-based facet
 * tagging WITHOUT referencing CgsCompat -- survives the plugin's deletion.
 */
class CgsOverrideMigrationTest {

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
    void gatlingItemGainsRangedWeaponFacet() {
        CategoryAssignment a = resolveBare("cgs:gatling_pistol",
                meta("cgs", "com.nukateam.cgs.common.foundation.item.GatlingItem"));
        assertTrue(hasFacet(a, ItemFacet.RANGED_WEAPON));
    }

    @Test
    void attachmentItemsGainTechComponentAndUpgradeFacets() {
        CategoryAssignment a = resolveBare("cgs:red_dot_scope",
                meta("cgs", "com.nukateam.cgs.common.foundation.item.attachment.ScopeItem"));
        assertTrue(hasFacet(a, ItemFacet.TECH_COMPONENT));
        assertTrue(hasFacet(a, ItemFacet.UPGRADE));
    }

    @Test
    void unmatchedCgsItemsGainNoOverrideFacets() {
        CategoryAssignment a = resolveBare("cgs:steel_ingot",
                meta("cgs", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.RANGED_WEAPON));
        assertFalse(hasFacet(a, ItemFacet.TECH_COMPONENT));
    }
}
