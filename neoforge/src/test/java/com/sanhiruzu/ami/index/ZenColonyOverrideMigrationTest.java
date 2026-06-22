package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ZenColonyOverrideMigrationTest {

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
    void supplyPacksRouteToMinecolonies() {
        // "pack" path token replaces the zen_colony:supply_packs tag
        CategoryAssignment a = resolveBare("zen_colony:raw_lumber_pack",
                meta("zen_colony", "net.minecraft.world.item.Item"));
        assertEquals("minecolonies", a.categoryId());
        assertEquals("supply_packs", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.STORAGE));
    }

    @Test
    void focusGainsMagicArtifactFacet() {
        CategoryAssignment a = resolveBare("zen_colony:astral_focus",
                meta("zen_colony", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void unmatchedItemsGainNoFacet() {
        CategoryAssignment a = resolveBare("zen_colony:colony_key",
                meta("zen_colony", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.STORAGE));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }
}
