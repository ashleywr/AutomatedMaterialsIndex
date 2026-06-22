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
 * Proves bundled born_in_chaos override data reproduces BornInChaosCompat's path-based facet
 * tagging WITHOUT referencing BornInChaosCompat -- survives the plugin's deletion.
 *
 * Class-based rules (Sword/ArmorItem/Elixir → MELEE_WEAPON/EQUIPPABLE/EDIBLE) are not
 * expressed in JSON; those items get those facets from core evidence in production.
 */
class BornInChaosOverrideMigrationTest {

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
    void mineralIngotGainsIngotAndMineralFacets() {
        // dark_metal_ingot -> tokens [dark, metal, ingot]; "ingot" pattern fires -> INGREDIENT_MINERAL + INGOT
        CategoryAssignment a = resolveBare("born_in_chaos_v1:dark_metal_ingot",
                meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.DarkMetalIngotItem"));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_MINERAL));
        assertTrue(hasFacet(a, ItemFacet.INGOT));
        assertEquals("ingredients", a.categoryId());
        assertEquals("mineral", a.subcategoryId());
    }

    @Test
    void charmItemGainsMagicArtifactFacet() {
        // charmof_power -> tokens [charmof, power]; "charmof" pattern fires -> MAGIC_ARTIFACT
        CategoryAssignment a = resolveBare("born_in_chaos_v1:charmof_power",
                meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.CharmofPowerItem"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertEquals("magic", a.categoryId());
        assertEquals("artifacts", a.subcategoryId());
    }

    @Test
    void utilityToolItemsGainUtilityToolFacet() {
        // krampuss_bag -> tokens [krampuss, bag]; "bag" pattern fires -> UTILITY_TOOL
        CategoryAssignment bag = resolveBare("born_in_chaos_v1:krampuss_bag",
                meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.KrampussBagItem"));
        assertTrue(hasFacet(bag, ItemFacet.UTILITY_TOOL));
        assertEquals("tools", bag.categoryId());
        assertEquals("utility", bag.subcategoryId());

        // evilometer -> single token; "evilometer" pattern fires -> UTILITY_TOOL
        CategoryAssignment meter = resolveBare("born_in_chaos_v1:evilometer",
                meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.EvilometerItem"));
        assertTrue(hasFacet(meter, ItemFacet.UTILITY_TOOL));
        assertEquals("tools", meter.categoryId());
        assertEquals("utility", meter.subcategoryId());
    }

    @Test
    void organicDropItemsGainOrganicFacet() {
        // zombie_claw -> tokens [zombie, claw]; "claw" pattern fires -> INGREDIENT_ORGANIC
        CategoryAssignment a = resolveBare("born_in_chaos_v1:zombie_claw",
                meta("born_in_chaos_v1", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void reagentItemsGainMagicReagentFacet() {
        // chaos_dust -> tokens [chaos, dust]; "dust" pattern fires -> MAGIC_REAGENT
        CategoryAssignment a = resolveBare("born_in_chaos_v1:chaos_dust",
                meta("born_in_chaos_v1", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void unmatchedItemsGainNoOverrideFacets() {
        // unknown_gem -> tokens [unknown, gem]; no born_in_chaos pattern matches
        CategoryAssignment a = resolveBare("born_in_chaos_v1:unknown_gem",
                meta("born_in_chaos_v1", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_REAGENT));
        assertFalse(hasFacet(a, ItemFacet.INGREDIENT_MINERAL));
    }
}
