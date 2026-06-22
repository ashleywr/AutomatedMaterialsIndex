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
 * Proves bundled ntgl override data reproduces NtglCompat's class-based facet
 * tagging WITHOUT referencing NtglCompat -- survives the plugin's deletion.
 */
class NtglOverrideMigrationTest {

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
    void weaponItemGainsRangedWeaponFacet() {
        CategoryAssignment a = resolveBare("ntgl:pistol10mm",
                meta("ntgl", "com.nukateam.ntgl.common.foundation.item.WeaponItem"));
        assertTrue(hasFacet(a, ItemFacet.RANGED_WEAPON));
    }

    @Test
    void attachmentItemsGainTechComponentAndUpgradeFacets() {
        CategoryAssignment a = resolveBare("ntgl:holographic_sight",
                meta("ntgl", "com.nukateam.ntgl.common.foundation.item.attachment.ScopeItem"));
        assertTrue(hasFacet(a, ItemFacet.TECH_COMPONENT));
        assertTrue(hasFacet(a, ItemFacet.UPGRADE));
    }

    @Test
    void chassisArmorRoutesToPowerArmorCategory() {
        CategoryAssignment a = resolveBare("ntgl:t45_body",
                meta("ntgl", "com.nukateam.chassis_core.common.foundation.item.ChassisArmor"));
        assertEquals("ntgl", a.categoryId());
        assertEquals("power_armor", a.subcategoryId());
    }

    @Test
    void unmatchedNtglItemsGainNoOverrideFacets() {
        CategoryAssignment a = resolveBare("ntgl:steel_ingot",
                meta("ntgl", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.RANGED_WEAPON));
        assertFalse(hasFacet(a, ItemFacet.TECH_COMPONENT));
    }
}
