package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SwemOverrideMigrationTest {

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
    void horseArmorGainsArmorAnimalFacet() {
        CategoryAssignment a = resolveBare("swem:horse_armor_cloth",
                meta("swem", "net.minecraft.world.item.ArmorItem"));
        assertTrue(hasFacet(a, ItemFacet.ARMOR_ANIMAL));
        assertEquals("swem", a.categoryId());
        assertEquals("horse_armor", a.subcategoryId());
    }

    @Test
    void tackItemClassRoutesToSwemTack() {
        CategoryAssignment a = resolveBare("swem:english_saddle",
                meta("swem", "com.alaharranhonor.swem.item.tack.TackItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_MISC));
        assertEquals("swem", a.categoryId());
        assertEquals("tack", a.subcategoryId());
    }

    @Test
    void ridingHelmetGainsHeadArmorFacets() {
        CategoryAssignment a = resolveBare("swem:helmet_riding",
                meta("swem", "com.alaharranhonor.swem.item.armor.RidingHelmet"));
        assertTrue(hasFacet(a, ItemFacet.EQUIPPABLE));
        assertTrue(hasFacet(a, ItemFacet.ARMOR_HEAD));
        assertEquals("swem", a.categoryId());
        assertEquals("riding_gear", a.subcategoryId());
    }

    @Test
    void ridingBootsGainsFeetArmorFacets() {
        CategoryAssignment a = resolveBare("swem:boots_riding",
                meta("swem", "com.alaharranhonor.swem.item.armor.RidingBoots"));
        assertTrue(hasFacet(a, ItemFacet.EQUIPPABLE));
        assertTrue(hasFacet(a, ItemFacet.ARMOR_FEET));
        assertEquals("swem", a.categoryId());
        assertEquals("riding_gear", a.subcategoryId());
    }

    @Test
    void coinItemGainsCurrencyFacet() {
        CategoryAssignment a = resolveBare("swem:coin_gold",
                meta("swem", "com.alaharranhonor.swem.item.CoinItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_CURRENCY));
        assertEquals("swem", a.categoryId());
        assertEquals("care", a.subcategoryId());
    }

    @Test
    void horseFeedItemClassRoutesToFeed() {
        CategoryAssignment a = resolveBare("swem:apple_feed",
                meta("swem", "com.alaharranhonor.swem.item.FeedItem"));
        assertTrue(hasFacet(a, ItemFacet.EDIBLE));
        assertTrue(hasFacet(a, ItemFacet.FOOD_PROTEIN));
        assertEquals("swem", a.categoryId());
        assertEquals("feed", a.subcategoryId());
    }

    @Test
    void binGrainPathRoutesToFeed() {
        CategoryAssignment a = resolveBare("swem:bin_grain_white",
                meta("swem", "net.minecraft.world.item.BlockItem"));
        assertTrue(hasFacet(a, ItemFacet.EDIBLE));
        assertEquals("feed", a.subcategoryId());
    }

    @Test
    void stableEquipmentClassRoutesToStable() {
        CategoryAssignment a = resolveBare("swem:jump_xc_bronze",
                meta("swem", "com.alaharranhonor.swem.item.EggJumpItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_MISC));
        assertEquals("swem", a.categoryId());
        assertEquals("stable", a.subcategoryId());
    }

    @Test
    void metalComponentGainsTechAndMineralFacets() {
        CategoryAssignment a = resolveBare("swem:rivet_copper",
                meta("swem", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.TECH_COMPONENT));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_MINERAL));
    }

    @Test
    void horseCareClassRoutesToCare() {
        CategoryAssignment a = resolveBare("swem:fly_spray",
                meta("swem", "com.alaharranhonor.swem.item.FlySprayItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_MISC));
        assertEquals("swem", a.categoryId());
        assertEquals("care", a.subcategoryId());
    }

    @Test
    void magicReagentPathGainsMagicReagentFacet() {
        CategoryAssignment a = resolveBare("swem:star_worm",
                meta("swem", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void organicDropPathRoutesToCare() {
        CategoryAssignment a = resolveBare("swem:manure_pile",
                meta("swem", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
        assertEquals("swem", a.categoryId());
        assertEquals("care", a.subcategoryId());
    }

    @Test
    void unmatchedSwemItemGainsNoOverrideFacets() {
        CategoryAssignment a = resolveBare("swem:generic_block",
                meta("swem", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.ARMOR_ANIMAL));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_MISC));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }
}
