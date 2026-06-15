package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.FacetProfile;
import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemProviderClassificationTest {
    @Test
    void primaryCategoryUsesCompatFacetsAddedAfterFacetIndexing() {
        Map<String, String> metalComponent = new HashMap<>();
        metalComponent.put(SearchNodeKeys.FACETS, ItemFacet.TECH_COMPONENT.id() + "," + ItemFacet.INGREDIENT_MINERAL.id());

        ItemProvider.applyPrimaryCategoryMeta(
                new ResourceLocation("swem", "plate_copper"),
                null,
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of()),
                metalComponent);

        assertEquals("ingredients", metalComponent.get(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("mineral", metalComponent.get(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("primary_rule", metalComponent.get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
        assertEquals("clear ingredients before incidental equipment or tech",
                metalComponent.get(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE));

        Map<String, String> utility = new HashMap<>();
        utility.put(SearchNodeKeys.FACETS, ItemFacet.UTILITY_MISC.id());

        ItemProvider.applyPrimaryCategoryMeta(
                new ResourceLocation("swem", "horse_whistle"),
                null,
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of()),
                utility);

        assertEquals("utility", utility.get(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("misc", utility.get(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }
}
