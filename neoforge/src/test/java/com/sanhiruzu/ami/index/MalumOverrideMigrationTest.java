package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MalumOverrideMigrationTest {

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

    private static CategoryAssignment resolve(String id, Map<String, String> meta) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta);
    }

    private static boolean hasFacet(Map<String, String> attrs, ItemFacet facet) {
        String facets = attrs.getOrDefault(SearchNodeKeys.FACETS, "");
        if (facets.isBlank()) return false;
        for (String part : facets.split(",")) {
            if (part.trim().equals(facet.id())) return true;
        }
        return false;
    }

    @Test
    void geasClassRoutesToGeasaAndCollapses() {
        var meta = meta("malum", "com.sammy.malum.common.item.GeasItem");
        var a = resolve("malum:geas/variant/geas_b28b0afe76f1", meta);
        assertEquals("malum", a.categoryId());
        assertEquals("geasa", a.subcategoryId());
        assertTrue(hasFacet(a.attributes(), ItemFacet.MAGIC_ARTIFACT));
        assertEquals("malum:geas", a.attributes().get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Geas", a.attributes().get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", a.attributes().get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void geasPathAlsoCollapsesAndRoutesToGeasa() {
        var meta = meta("malum", "net.minecraft.world.item.Item");
        var a = resolve("malum:geas_of_something", meta);
        assertEquals("geasa", a.subcategoryId());
        assertNotNull(a.attributes().get(SearchNodeKeys.COLLAPSE_FAMILY));
    }

    @Test
    void encyclopediaGainsBookFacets() {
        var meta = meta("malum", "com.sammy.malum.common.item.misc.SoulsticeEncyclopedia");
        var a = resolve("malum:soulstice_encyclopedia", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.BOOK));
        assertTrue(hasFacet(a.attributes(), ItemFacet.GUIDE_BOOK));
        assertEquals("malum", a.categoryId());
        assertEquals("misc", a.subcategoryId());
    }

    @Test
    void impetusClassRoutesToImpetus() {
        var meta = meta("malum", "com.sammy.malum.common.item.impetus.ImpetusItem");
        var a = resolve("malum:the_impetus", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.MAGIC_ARTIFACT));
        assertTrue(hasFacet(a.attributes(), ItemFacet.UPGRADE));
        assertEquals("malum", a.categoryId());
        assertEquals("impetus", a.subcategoryId());
    }

    @Test
    void augmentClassRoutesToAugments() {
        var meta = meta("malum", "com.sammy.malum.common.item.augment.SpiritAugmentItem");
        var a = resolve("malum:spirit_augment", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.MAGIC_ARTIFACT));
        assertTrue(hasFacet(a.attributes(), ItemFacet.UPGRADE));
        assertEquals("malum", a.categoryId());
        assertEquals("augments", a.subcategoryId());
    }

    @Test
    void tinkeringToolRoutesToEquipment() {
        var meta = meta("malum", "com.sammy.malum.common.item.misc.TinkeringToolItem");
        var a = resolve("malum:tinkering_tool", meta);
        assertEquals("malum", a.categoryId());
        assertEquals("equipment", a.subcategoryId());
    }

    @Test
    void windNucleusRoutesToArtifacts() {
        var meta = meta("malum", "com.sammy.malum.common.item.misc.WindNucleusItem");
        var a = resolve("malum:wind_nucleus", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.MAGIC_ARTIFACT));
        assertEquals("malum", a.categoryId());
        assertEquals("artifacts", a.subcategoryId());
    }

    @Test
    void weavePathRoutesToWeaves() {
        var meta = meta("malum", "net.minecraft.world.item.Item");
        var a = resolve("malum:brilliance_weave", meta);
        assertEquals("malum", a.categoryId());
        assertEquals("weaves", a.subcategoryId());
    }

    @Test
    void sapballPathRoutesToArtifacts() {
        var meta = meta("malum", "net.minecraft.world.item.Item");
        var a = resolve("malum:sacred_sapball", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.MAGIC_ARTIFACT));
        assertEquals("artifacts", a.subcategoryId());
    }

    @Test
    void spiritShardClassRoutesToSpirits() {
        var meta = meta("malum", "com.sammy.malum.common.item.spirits.SpiritShardItem");
        var a = resolve("malum:sacred_spirit", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.MAGIC_REAGENT));
        assertEquals("malum", a.categoryId());
        assertEquals("spirits", a.subcategoryId());
    }

    @Test
    void runePathRoutesToSpirits() {
        var meta = meta("malum", "net.minecraft.world.item.Item");
        var a = resolve("malum:rune_of_warding", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.MAGIC_REAGENT));
        assertEquals("spirits", a.subcategoryId());
    }

    @Test
    void nuggetPathGainsNuggetAndMineralFacets() {
        var meta = meta("malum", "net.minecraft.world.item.Item");
        var a = resolve("malum:blazing_quartz_nugget", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.NUGGET));
        assertTrue(hasFacet(a.attributes(), ItemFacet.INGREDIENT_MINERAL));
    }

    @Test
    void ingotPathGainsIngotAndMineralFacets() {
        var meta = meta("malum", "net.minecraft.world.item.Item");
        var a = resolve("malum:malum_ingot", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.INGOT));
        assertTrue(hasFacet(a.attributes(), ItemFacet.INGREDIENT_MINERAL));
    }

    @Test
    void quartzPathGainsMineralFacet() {
        var meta = meta("malum", "net.minecraft.world.item.Item");
        var a = resolve("malum:blazing_quartz", meta);
        assertTrue(hasFacet(a.attributes(), ItemFacet.INGREDIENT_MINERAL));
    }
}
