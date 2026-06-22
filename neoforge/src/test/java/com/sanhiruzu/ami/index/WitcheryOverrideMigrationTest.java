package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WitcheryOverrideMigrationTest {

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
    void broomItemGainsTransportFacet() {
        // path "broom/variant/broom_..." splits to include token "broom"
        CategoryAssignment a = resolveBare("witchery:broom/variant/broom_a22400b68762",
                meta("witchery", "dev.sterner.witchery.content.item.BroomItem"));
        assertTrue(hasFacet(a, ItemFacet.TRANSPORT));
    }

    @Test
    void magicReagentGainsMagicReagentFacet() {
        CategoryAssignment a = resolveBare("witchery:mutandis",
                meta("witchery", "dev.sterner.witchery.content.item.MutandisItem"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void seerStoneGainsMagicArtifactFacet() {
        // "seer" path token + SeerStoneItem classToken
        CategoryAssignment a = resolveBare("witchery:seer_stone",
                meta("witchery", "dev.sterner.witchery.content.item.SeerStoneItem"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void stakeGainsMeleeWeaponFacet() {
        CategoryAssignment a = resolveBare("witchery:wooden_oak_stake",
                meta("witchery", "dev.sterner.witchery.content.item.WoodenStakeItem"));
        assertTrue(hasFacet(a, ItemFacet.MELEE_WEAPON));
    }

    @Test
    void unmatchedItemsGainNoFacet() {
        CategoryAssignment a = resolveBare("witchery:candle",
                meta("witchery", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.TRANSPORT));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }
}
