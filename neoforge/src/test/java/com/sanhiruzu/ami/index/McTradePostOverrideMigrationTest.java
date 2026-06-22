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
 * Proves bundled mctradepost override data reproduces McTradePostCompat's
 * class+path-based facet tagging WITHOUT referencing McTradePostCompat --
 * survives the plugin's deletion.
 */
class McTradePostOverrideMigrationTest {

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
    void clipboardItemGainsUtilityToolFacet() {
        CategoryAssignment a = resolveBare("mctradepost:advanced_clipboard",
                meta("mctradepost", "com.deathfrog.mctradepost.item.AdvancedClipboardItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_TOOL));
    }

    @Test
    void souvenirGainsUtilityMiscFacet() {
        CategoryAssignment a = resolveBare("mctradepost:souvenir_map",
                meta("mctradepost", "com.deathfrog.mctradepost.item.SouvenirItem"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_MISC));
    }

    @Test
    void wishItemGainsMagicArtifactFacet() {
        CategoryAssignment a = resolveBare("mctradepost:wish_scroll",
                meta("mctradepost", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void nuggetGainsUtilityCurrencyFacet() {
        CategoryAssignment a = resolveBare("mctradepost:copper_nugget",
                meta("mctradepost", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_CURRENCY));
    }

    @Test
    void unmatchedItemsGainNoOverrideFacets() {
        CategoryAssignment a = resolveBare("mctradepost:table",
                meta("mctradepost", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_TOOL));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_CURRENCY));
    }
}
