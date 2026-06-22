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
 * Proves bundled cataclysm override data reproduces CataclysmCompat's
 * class+path-based facet tagging WITHOUT referencing CataclysmCompat --
 * survives the plugin's deletion.
 */
class CataclysmOverrideMigrationTest {

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
    void dungeonEyeClassRoutesToCataclysmDungeonEyes() {
        CategoryAssignment a = resolveBare("cataclysm:mech_eye",
                meta("cataclysm", "com.github.L_Ender.cataclysm.items.DungeonEyeItem"));
        assertEquals("cataclysm", a.categoryId());
        assertEquals("dungeon_eyes", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void eyePathRoutesToCataclysmDungeonEyes() {
        CategoryAssignment a = resolveBare("cataclysm:mech_eye",
                meta("cataclysm", "net.minecraft.world.item.Item"));
        assertEquals("cataclysm", a.categoryId());
        assertEquals("dungeon_eyes", a.subcategoryId());
        assertTrue(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void ingotGainsIngotAndMineralFacets() {
        CategoryAssignment a = resolveBare("cataclysm:witherite_ingot",
                meta("cataclysm", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_MINERAL));
        assertTrue(hasFacet(a, ItemFacet.INGOT));
    }

    @Test
    void swordClassGainsMeleeWeaponFacet() {
        CategoryAssignment a = resolveBare("cataclysm:katana",
                meta("cataclysm", "com.github.L_Ender.cataclysm.items.CataclysmSword"));
        assertTrue(hasFacet(a, ItemFacet.MELEE_WEAPON));
        assertEquals("cataclysm", a.categoryId());
        assertEquals("weapons", a.subcategoryId());
    }

    @Test
    void rangedWeaponHasPriorityOverMelee() {
        CategoryAssignment a = resolveBare("cataclysm:laser_cannon",
                meta("cataclysm", "com.github.L_Ender.cataclysm.items.LaserCannon"));
        assertTrue(hasFacet(a, ItemFacet.RANGED_WEAPON));
        assertFalse(hasFacet(a, ItemFacet.MELEE_WEAPON));
    }

    @Test
    void lacrimaReagentGainsMagicReagentFacet() {
        CategoryAssignment a = resolveBare("cataclysm:lacrima",
                meta("cataclysm", "net.minecraft.world.item.Item"));
        assertTrue(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void unmatchedCataclysmItemsGainNoOverrideFacets() {
        CategoryAssignment a = resolveBare("cataclysm:stone_block",
                meta("cataclysm", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.RANGED_WEAPON));
        assertFalse(hasFacet(a, ItemFacet.MELEE_WEAPON));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_ARTIFACT));
        assertFalse(hasFacet(a, ItemFacet.MAGIC_REAGENT));
    }
}
