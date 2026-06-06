package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.AlexsCavesCompat;
import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlexsCavesCompatTest {
    @Test
    void alexsCavesNamespaceGetsFamilyPolicy() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");

        CompatFamilyDetector.detect(new ResourceLocation("alexscaves", "telecore"), meta);

        assertEquals("alexscaves", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void caveInfoItemsRouteToUtilityBooksAndCollapseTogether() {
        Map<String, String> meta = meta("com.github.alexmodguy.alexscaves.server.item.CaveInfoItem");

        AlexsCavesCompat.enrichItem(
                new ResourceLocation("alexscaves", "cave_tablet/variant/cave_tablet_1da4b27ad913"),
                meta);
        CategoryAssignment assignment = resolve("alexscaves:cave_tablet/variant/cave_tablet_1da4b27ad913", meta);

        assertEquals("guide_items", meta.get(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.GUIDE_BOOK.id()));
        assertEquals("alexscaves:cave_info", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Cave Info", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("utility", assignment.categoryId());
        assertEquals("books", assignment.subcategoryId());
    }

    @Test
    void neodymiumMaterialsRouteToTechIngots() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");

        AlexsCavesCompat.enrichItem(new ResourceLocation("alexscaves", "scarlet_neodymium_ingot"), meta);
        CategoryAssignment assignment = resolve("alexscaves:scarlet_neodymium_ingot", meta);

        assertEquals("materials", meta.get(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.INGOT.id()));
        assertEquals("tech", assignment.categoryId());
        assertEquals("ingots", assignment.subcategoryId());
    }

    @Test
    void caveGadgetsRouteToTechOrUtilityBuckets() {
        Map<String, String> part = meta("net.minecraft.world.item.Item");
        AlexsCavesCompat.enrichItem(new ResourceLocation("alexscaves", "telecore"), part);
        CategoryAssignment partAssignment = resolve("alexscaves:telecore", part);

        Map<String, String> tool = meta("com.github.alexmodguy.alexscaves.server.item.QuarrySmasherItem");
        AlexsCavesCompat.enrichItem(new ResourceLocation("alexscaves", "quarry_smasher"), tool);
        CategoryAssignment toolAssignment = resolve("alexscaves:quarry_smasher", tool);

        assertEquals("tech_parts", part.get(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND));
        assertEquals("tech", partAssignment.categoryId());
        assertEquals("parts", partAssignment.subcategoryId());
        assertEquals("harvest_tools", tool.get(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND));
        assertEquals("tools", toolAssignment.categoryId());
        assertEquals("harvest", toolAssignment.subcategoryId());
    }

    @Test
    void foodFamiliesRouteToNatureBuckets() {
        Map<String, String> protein = meta("net.minecraft.world.item.Item");
        protein.put(SearchNodeKeys.TAGS, "minecraft:fishes");
        AlexsCavesCompat.enrichItem(new ResourceLocation("alexscaves", "cooked_radgill"), protein);
        CategoryAssignment proteinAssignment = resolve("alexscaves:cooked_radgill", protein);

        Map<String, String> snack = meta("net.minecraft.world.item.Item");
        snack.put(SearchNodeKeys.TAGS, "alexscaves:gelatins,alexscaves:gummy_items");
        AlexsCavesCompat.enrichItem(new ResourceLocation("alexscaves", "gelatin_red"), snack);
        CategoryAssignment snackAssignment = resolve("alexscaves:gelatin_red", snack);

        assertEquals("protein_foods", protein.get(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND));
        assertEquals("nature", proteinAssignment.categoryId());
        assertEquals("proteins", proteinAssignment.subcategoryId());
        assertEquals("snacks", snack.get(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND));
        assertEquals("nature", snackAssignment.categoryId());
        assertEquals("snacks", snackAssignment.subcategoryId());
    }

    @Test
    void hiddenWeaponInventoryVariantsKeepAccessAndRouteAsWeapons() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");
        meta.put(SearchNodeKeys.ACCESS_LEVEL, "dev");
        meta.put(SearchNodeKeys.VISIBILITY, "hidden");

        AlexsCavesCompat.enrichItem(new ResourceLocation("alexscaves", "dreadbow_pulling_0_inventory"), meta);
        CategoryAssignment assignment = resolve("alexscaves:dreadbow_pulling_0_inventory", meta);

        assertEquals("ranged_weapons", meta.get(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND));
        assertEquals("dev", meta.get(SearchNodeKeys.ACCESS_LEVEL));
        assertEquals("hidden", meta.get(SearchNodeKeys.VISIBILITY));
        assertEquals("tools", assignment.categoryId());
        assertEquals("ranged", assignment.subcategoryId());
    }

    @Test
    void magicAndOrganicCaveDropsRouteSemantically() {
        Map<String, String> magic = meta("com.github.alexmodguy.alexscaves.server.item.OccultGemItem");
        AlexsCavesCompat.enrichItem(new ResourceLocation("alexscaves", "occult_gem"), magic);
        CategoryAssignment magicAssignment = resolve("alexscaves:occult_gem", magic);

        Map<String, String> organic = meta("net.minecraft.world.item.Item");
        AlexsCavesCompat.enrichItem(new ResourceLocation("alexscaves", "vesper_wing"), organic);
        CategoryAssignment organicAssignment = resolve("alexscaves:vesper_wing", organic);

        assertEquals("magic_reagents", magic.get(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND));
        assertEquals("magic", magicAssignment.categoryId());
        assertEquals("reagents", magicAssignment.subcategoryId());
        assertEquals("organic_drops", organic.get(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND));
        assertEquals("ingredients", organicAssignment.categoryId());
        assertEquals("organic", organicAssignment.subcategoryId());
    }

    private static Map<String, String> meta(String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "alexscaves");
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Alex's Caves");
        meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id),
                facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets),
                meta
        );
    }
}
