package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnigmaticLegacyPlusOverrideMigrationTest {

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
    void artifactItemsGainMagicArtifactFacet() {
        CategoryAssignment a = resolveBare("enigmaticlegacyplus:extradimensional_eye",
                meta("enigmaticlegacyplus",
                        "auviotre.enigmatic.legacy.contents.item.misc.ExtradimensionalEye"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void bagItemsGainStorageFacet() {
        CategoryAssignment a = resolveBare("enigmaticlegacyplus:antique_bag",
                meta("enigmaticlegacyplus",
                        "auviotre.enigmatic.legacy.contents.item.legacy.AntiqueBag"));
        assertTrue(hasFacet(a, ItemFacet.STORAGE));
    }

    @Test
    void classTokenWinsOverPathForMixture() {
        // MendingMixture → artifact via classToken; "mixture" pathToken fires reagent
        // artifact rule is first → artifact wins
        CategoryAssignment a = resolveBare("enigmaticlegacyplus:mending_mixture",
                meta("enigmaticlegacyplus",
                        "auviotre.enigmatic.legacy.contents.item.potions.MendingMixture"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void ringItemsGainCurioAndArtifactFacets() {
        CategoryAssignment a = resolveBare("enigmaticlegacyplus:extra_ring",
                meta("enigmaticlegacyplus",
                        "auviotre.enigmatic.legacy.contents.item.BaseItem$1"));
        assertTrue(hasFacet(a, ItemFacet.CURIO));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void unmatchedItemsGainNoFacet() {
        CategoryAssignment a = resolveBare("enigmaticlegacyplus:unknown",
                meta("enigmaticlegacyplus", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.STORAGE));
    }
}
