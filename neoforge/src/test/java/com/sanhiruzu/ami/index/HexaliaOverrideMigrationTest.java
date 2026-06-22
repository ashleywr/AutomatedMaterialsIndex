package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HexaliaOverrideMigrationTest {

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
    void focusItemsGainArtifactFacet() {
        // HexFocusItem via classToken; "focus" path token also fires — same rule
        CategoryAssignment a = resolveBare("hexalia:hex_focus",
                meta("hexalia", "net.astralya.hexalia.item.custom.HexFocusItem"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void sacItemsGainProjectileFacet() {
        CategoryAssignment a = resolveBare("hexalia:foul_sac",
                meta("hexalia", "net.astralya.hexalia.item.custom.ThrownSacItem"));
        assertTrue(hasFacet(a, ItemFacet.PROJECTILE));
    }

    @Test
    void athameGainsMeleeWeaponFacet() {
        CategoryAssignment a = resolveBare("hexalia:athame",
                meta("hexalia", "net.astralya.hexalia.item.custom.AthameItem"));
        assertTrue(hasFacet(a, ItemFacet.MELEE_WEAPON));
    }

    @Test
    void powderItemsGainMagicReagentFacet() {
        // "powder" path token — covers crushed_herbs items whose tag is not migrated
        CategoryAssignment a = resolveBare("hexalia:spirit_powder",
                meta("hexalia", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void unmatchedItemsGainNoFacet() {
        CategoryAssignment a = resolveBare("hexalia:book_of_hexalia",
                meta("hexalia", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }
}
