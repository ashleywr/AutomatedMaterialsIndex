package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.NaturesAuraCompat;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturesAuraCompatTest {
    @Test
    void naturesAuraNamespaceGetsFamilyPolicy() {
        Map<String, String> meta = meta("de.ellpeck.naturesaura.items.ItemEffectPowder");

        CompatFamilyDetector.detect(new Identifier("naturesaura", "effect_powder/variant/powder_of_fertility"), meta);

        assertEquals("naturesaura", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void effectPowderVariantsRouteToMagicReagentsAndCollapseTogether() {
        Map<String, String> meta = meta("de.ellpeck.naturesaura.items.ItemEffectPowder");

        NaturesAuraCompat.enrichItem(new Identifier("naturesaura", "effect_powder/variant/powder_of_fertility"), meta);
        CategoryAssignment assignment = resolve("naturesaura:effect_powder/variant/powder_of_fertility", meta);

        assertEquals("effect_powders", meta.get(SearchNodeKeys.NATURES_AURA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.NATURES_AURA_FACTS, "").contains("effect_powder"));
        assertEquals("naturesaura:effect_powders", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Effect Powders", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void structureFinderItemsRouteToUtilityNavigation() {
        Map<String, String> meta = meta("de.ellpeck.naturesaura.items.ItemStructureFinder");

        NaturesAuraCompat.enrichItem(new Identifier("naturesaura", "fortress_finder"), meta);
        CategoryAssignment assignment = resolve("naturesaura:fortress_finder", meta);

        assertEquals("structure_finders", meta.get(SearchNodeKeys.NATURES_AURA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.UTILITY_NAVIGATION.id()));
        assertEquals("utility", assignment.categoryId());
        assertEquals("navigation", assignment.subcategoryId());
    }

    @Test
    void tokenRitualItemsRouteToMagicReagents() {
        Map<String, String> meta = meta("de.ellpeck.naturesaura.items.ItemImpl");
        meta.put(SearchNodeKeys.RECIPE_CATEGORIES, "tree_ritual");

        NaturesAuraCompat.enrichItem(new Identifier("naturesaura", "token_joy"), meta);
        CategoryAssignment assignment = resolve("naturesaura:token_joy", meta);

        assertEquals("tokens", meta.get(SearchNodeKeys.NATURES_AURA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.NATURES_AURA_FACTS, "").contains("aura_ritual_item"));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void taintedGoldRoutesToMineralIngredient() {
        Map<String, String> meta = meta("de.ellpeck.naturesaura.items.ItemImpl");

        NaturesAuraCompat.enrichItem(new Identifier("naturesaura", "tainted_gold"), meta);
        CategoryAssignment assignment = resolve("naturesaura:tainted_gold", meta);

        assertEquals("materials", meta.get(SearchNodeKeys.NATURES_AURA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.INGREDIENT_MINERAL.id()));
        assertEquals("ingredients", assignment.categoryId());
        assertEquals("mineral", assignment.subcategoryId());
    }

    @Test
    void auraCartRoutesToTechTransport() {
        Map<String, String> meta = meta("de.ellpeck.naturesaura.items.ItemMoverMinecart");

        NaturesAuraCompat.enrichItem(new Identifier("naturesaura", "mover_cart"), meta);
        CategoryAssignment assignment = resolve("naturesaura:mover_cart", meta);

        assertEquals("transport", meta.get(SearchNodeKeys.NATURES_AURA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.TRANSPORT.id()));
        assertEquals("tech", assignment.categoryId());
        assertEquals("transport", assignment.subcategoryId());
    }

    @Test
    void utilityItemsRouteToUtilityMiscWithoutCrashingOnSpecificMetadata() {
        Map<String, String> meta = meta("de.ellpeck.naturesaura.items.ItemMultiblockMaker");

        NaturesAuraCompat.enrichItem(new Identifier("naturesaura", "multiblock_maker"), meta);
        CategoryAssignment assignment = resolve("naturesaura:multiblock_maker", meta);

        assertEquals("utility", meta.get(SearchNodeKeys.NATURES_AURA_ITEM_KIND));
        assertEquals("utility", assignment.categoryId());
        assertEquals("misc", assignment.subcategoryId());
    }

    private static Map<String, String> meta(String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "naturesaura");
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Nature's Aura");
        meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new Identifier(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
