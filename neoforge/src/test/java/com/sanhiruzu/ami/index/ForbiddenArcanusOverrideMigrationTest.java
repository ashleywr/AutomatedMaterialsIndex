package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ForbiddenArcanusOverrideMigrationTest {

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
    void catcherItemsGainUtilityToolFacet() {
        // "catcher" path token covers blue_quantum_catcher, boss_catcher, etc.
        CategoryAssignment a = resolveBare("forbidden_arcanus:blue_quantum_catcher",
                meta("forbidden_arcanus", "com.stal111.forbidden_arcanus.common.item.QuantumCatcherItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_TOOL));
    }

    @Test
    void prismItemsGainArtifactFacet() {
        CategoryAssignment a = resolveBare("forbidden_arcanus:whirlwind_prism",
                meta("forbidden_arcanus", "com.stal111.forbidden_arcanus.common.item.WhirlwindPrismItem"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void soulItemsGainMagicReagentFacet() {
        // "soul" path token covers tag-based soul items without needing the tag
        CategoryAssignment a = resolveBare("forbidden_arcanus:enchanted_soul",
                meta("forbidden_arcanus", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void tankGainsUtilityMiscFacet() {
        CategoryAssignment a = resolveBare("forbidden_arcanus:aureal_tank",
                meta("forbidden_arcanus", "com.stal111.forbidden_arcanus.common.item.AurealTankItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_MISC));
    }

    @Test
    void unmatchedItemsGainNoFacet() {
        CategoryAssignment a = resolveBare("forbidden_arcanus:unknown_block",
                meta("forbidden_arcanus", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }
}
