package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EternalStarlightOverrideMigrationTest {

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

    private static boolean hasFacet(CategoryAssignment assignment, ItemFacet facet) {
        return assignment.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(facet.id());
    }

    @Test
    void paintingClassRoutesToPaintingsAndCollapses() {
        var meta = meta("eternal_starlight",
                "cn.leolezury.eternalstarlight.common.item.misc.ESPaintingItem");
        var a = resolve("eternal_starlight:starlit_painting/variant/starlit_painting_27b7fa141554", meta);
        assertEquals("eternal_starlight", a.categoryId());
        assertEquals("paintings", a.subcategoryId());
        assertEquals("eternal_starlight:starlit_painting", a.attributes().get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Starlit Paintings", a.attributes().get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", a.attributes().get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void paintingPathAlsoRoutesToPaintings() {
        var meta = meta("eternal_starlight", "net.minecraft.world.item.Item");
        var a = resolve("eternal_starlight:starlit_painting/variant/painting_xyz", meta);
        assertEquals("eternal_starlight", a.categoryId());
        assertEquals("paintings", a.subcategoryId());
        assertNotNull(a.attributes().get(SearchNodeKeys.COLLAPSE_FAMILY));
    }

    @Test
    void pendantPathRoutesToAccessories() {
        var meta = meta("eternal_starlight", "net.minecraft.world.item.Item");
        var a = resolve("eternal_starlight:battleaxe_pendant", meta);
        assertEquals("eternal_starlight", a.categoryId());
        assertEquals("accessories", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.CURIO));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void accessoryClassRoutesToAccessories() {
        var meta = meta("eternal_starlight",
                "cn.leolezury.eternalstarlight.common.item.misc.GalacticQuiverItem");
        var a = resolve("eternal_starlight:galactic_quiver", meta);
        assertEquals("eternal_starlight", a.categoryId());
        assertEquals("accessories", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.CURIO));
    }

    @Test
    void starfirePathGainsProjectileAndMagicArtifact() {
        var meta = meta("eternal_starlight", "net.minecraft.world.item.Item");
        var a = resolve("eternal_starlight:starfire_ball", meta);
        assertEquals("eternal_starlight", a.categoryId());
        assertEquals("artifacts", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.PROJECTILE));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void tentaclePathGainsProjectileAndMagicArtifact() {
        var meta = meta("eternal_starlight", "net.minecraft.world.item.Item");
        var a = resolve("eternal_starlight:tentacle_spike", meta);
        assertTrue(hasFacet(a, ItemFacet.PROJECTILE));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void artifactClassRouteToArtifacts() {
        var meta = meta("eternal_starlight",
                "cn.leolezury.eternalstarlight.common.item.misc.EthericEyeItem");
        var a = resolve("eternal_starlight:etheric_eye", meta);
        assertEquals("eternal_starlight", a.categoryId());
        assertEquals("artifacts", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.PROJECTILE));
    }

    @Test
    void dewPathRoutesToReagents() {
        var meta = meta("eternal_starlight", "net.minecraft.world.item.Item");
        var a = resolve("eternal_starlight:soul_dew", meta);
        assertEquals("eternal_starlight", a.categoryId());
        assertEquals("reagents", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void brickPathRoutesToMaterials() {
        var meta = meta("eternal_starlight", "net.minecraft.world.item.Item");
        var a = resolve("eternal_starlight:cinder_brick", meta);
        assertEquals("eternal_starlight", a.categoryId());
        assertEquals("materials", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_MINERAL));
    }

    @Test
    void lootBagClassRoutesToUtility() {
        var meta = meta("eternal_starlight",
                "cn.leolezury.eternalstarlight.common.item.misc.LootBagItem");
        var a = resolve("eternal_starlight:loot_bag", meta);
        assertEquals("eternal_starlight", a.categoryId());
        assertEquals("utility", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.UTILITY_MISC));
    }
}
