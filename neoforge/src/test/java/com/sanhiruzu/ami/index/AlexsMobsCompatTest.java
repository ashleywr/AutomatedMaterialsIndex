package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.AlexsMobsCompat;
import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlexsMobsCompatTest {
    @Test
    void alexsMobsNamespaceGetsFamilyPolicy() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");

        CompatFamilyDetector.detect(new ResourceLocation("alexsmobs", "bear_fur"), meta);

        assertEquals("alexsmobs", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void animalDictionaryIngredientsRouteToOrganicIngredients() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");
        meta.put(SearchNodeKeys.TAGS, "alexsmobs:animal_dictionary_ingredient");

        AlexsMobsCompat.enrichItem(new ResourceLocation("alexsmobs", "bear_fur"), meta);
        CategoryAssignment assignment = resolve("alexsmobs:bear_fur", meta);

        assertEquals("organic_drops", meta.get(SearchNodeKeys.ALEXS_MOBS_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.ALEXS_MOBS_FACTS, "").contains("organic_drop"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.INGREDIENT_ORGANIC.id()));
        assertEquals("ingredients", assignment.categoryId());
        assertEquals("organic", assignment.subcategoryId());
    }

    @Test
    void alexsMobsFoodTagsRouteToProteins() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");
        meta.put(SearchNodeKeys.TAGS, "alexsmobs:platypus_foodstuffs,alexsmobs:seal_breedables");

        AlexsMobsCompat.enrichItem(new ResourceLocation("alexsmobs", "lobster_tail"), meta);
        CategoryAssignment assignment = resolve("alexsmobs:lobster_tail", meta);

        assertEquals("protein_foods", meta.get(SearchNodeKeys.ALEXS_MOBS_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.FOOD_PROTEIN.id()));
        assertEquals("nature", assignment.categoryId());
        assertEquals("proteins", assignment.subcategoryId());
    }

    @Test
    void customUtilityClassesRouteToExpectedToolBuckets() {
        Map<String, String> locator = meta("com.github.alexthe666.alexsmobs.item.ItemEcholocator");
        AlexsMobsCompat.enrichItem(new ResourceLocation("alexsmobs", "pupfish_locator"), locator);
        CategoryAssignment locatorAssignment = resolve("alexsmobs:pupfish_locator", locator);

        Map<String, String> blaster = meta("com.github.alexthe666.alexsmobs.item.ItemHemolymphBlaster");
        AlexsMobsCompat.enrichItem(new ResourceLocation("alexsmobs", "hemolymph_blaster"), blaster);
        CategoryAssignment blasterAssignment = resolve("alexsmobs:hemolymph_blaster", blaster);

        assertEquals("navigation_tools", locator.get(SearchNodeKeys.ALEXS_MOBS_ITEM_KIND));
        assertEquals("utility", locatorAssignment.categoryId());
        assertEquals("navigation", locatorAssignment.subcategoryId());
        assertEquals("ranged_weapons", blaster.get(SearchNodeKeys.ALEXS_MOBS_ITEM_KIND));
        assertEquals("tools", blasterAssignment.categoryId());
        assertEquals("ranged", blasterAssignment.subcategoryId());
    }

    @Test
    void transportAndArmorClassesRouteSemantically() {
        Map<String, String> board = meta("com.github.alexthe666.alexsmobs.item.ItemStraddleboard");
        AlexsMobsCompat.enrichItem(new ResourceLocation("alexsmobs", "straddleboard"), board);
        CategoryAssignment boardAssignment = resolve("alexsmobs:straddleboard", board);

        Map<String, String> shoes = meta("com.github.alexthe666.alexsmobs.item.ItemPigshoes");
        AlexsMobsCompat.enrichItem(new ResourceLocation("alexsmobs", "pigshoes"), shoes);
        CategoryAssignment shoesAssignment = resolve("alexsmobs:pigshoes", shoes);

        assertEquals("transport", board.get(SearchNodeKeys.ALEXS_MOBS_ITEM_KIND));
        assertEquals("tech", boardAssignment.categoryId());
        assertEquals("transport", boardAssignment.subcategoryId());
        assertEquals("feet_armor", shoes.get(SearchNodeKeys.ALEXS_MOBS_ITEM_KIND));
        assertEquals("armor", shoesAssignment.categoryId());
        assertEquals("feet", shoesAssignment.subcategoryId());
    }

    @Test
    void hiddenInventoryOnlyVariantsRouteToUtilityMiscWithoutChangingAccess() {
        Map<String, String> meta = meta("com.github.alexthe666.alexsmobs.item.ItemInventoryOnly");
        meta.put(SearchNodeKeys.ACCESS_LEVEL, "dev");
        meta.put(SearchNodeKeys.VISIBILITY, "hidden");

        AlexsMobsCompat.enrichItem(new ResourceLocation("alexsmobs", "falconry_glove_inventory"), meta);
        CategoryAssignment assignment = resolve("alexsmobs:falconry_glove_inventory", meta);

        assertEquals("internal_items", meta.get(SearchNodeKeys.ALEXS_MOBS_ITEM_KIND));
        assertEquals("dev", meta.get(SearchNodeKeys.ACCESS_LEVEL));
        assertEquals("hidden", meta.get(SearchNodeKeys.VISIBILITY));
        assertEquals("utility", assignment.categoryId());
        assertEquals("misc", assignment.subcategoryId());
    }

    private static Map<String, String> meta(String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "alexsmobs");
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Alex's Mobs");
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
