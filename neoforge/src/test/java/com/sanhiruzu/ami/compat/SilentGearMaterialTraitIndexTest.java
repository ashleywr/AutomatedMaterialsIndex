package com.sanhiruzu.ami.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentGearMaterialTraitIndexTest {
    @AfterEach
    void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void materialTraitsAnnotateMatchingIngredientItems() {
        GlobalIndex index = GlobalIndex.getInstance();
        SearchNode crimsonSteelIngot = item("silentgear", "crimson_steel_ingot", "Crimson Steel Ingot", Map.of(
                SearchNodeKeys.TAGS, "c:ingots/crimson_steel,c:ingots",
                SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, "tools"
        ));
        SearchNode finishedRod = item("silentgear", "fishing_rod", "Crimson Steel Fishing Rod", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "silent_gear,modular_gear",
                SearchNodeKeys.MODULAR_GEAR_FAMILY, "silent_gear",
                SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, "tools",
                SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, "malleable_v,hard_iii",
                SearchNodeKeys.SEARCH_TOKENS, "gear_trait_malleable_v"
        ));
        index.addNode(crimsonSteelIngot);
        index.addNode(finishedRod);

        SilentGearMaterialTraitIndex.MaterialRecord record = SilentGearMaterialTraitIndex.materialRecord(
                new Identifier("silentgear", "silentgear_materials/crimson_steel.json"),
                materialJson());
        assertEquals(1, SilentGearMaterialTraitIndex.applyMaterialRecords(index, List.of(record)));

        SearchNode updatedIngot = index.getNode(crimsonSteelIngot.id(), NodeType.ITEM).orElseThrow();
        assertEquals("materials", updatedIngot.meta(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
        assertEquals("malleable,malleable_v,hard,hard_iii,flexible,flexible_iv",
                updatedIngot.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, ""));
        assertTrue(updatedIngot.meta(SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAIT_DETAILS, "")
                .contains("crimson_steel:main:malleable_v"));

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> traitResults = service.query("?trait:malleable").getOrDefault(NodeType.ITEM, List.of());
        assertTrue(traitResults.contains(updatedIngot));
        assertFalse(traitResults.contains(finishedRod));
        assertTrue(service.query("?runtime_trait:malleable_v").getOrDefault(NodeType.ITEM, List.of())
                .contains(finishedRod));
        assertTrue(service.query("?token:gear_trait_malleable_v").getOrDefault(NodeType.ITEM, List.of())
                .contains(finishedRod));
    }

    private static JsonObject materialJson() {
        return JsonParser.parseString("""
                {
                  "crafting": {
                    "ingredient": { "tag": "c:ingots/crimson_steel" },
                    "part_substitutes": {
                      "silentgear:rod": { "tag": "c:rods/crimson_steel" }
                    }
                  },
                  "properties": {
                    "silentgear:main": {
                      "traits": [
                        { "trait": "silentgear:malleable", "level": 5 },
                        { "trait": "silentgear:hard", "level": 3 }
                      ]
                    },
                    "silentgear:rod": {
                      "traits": [
                        { "trait": "silentgear:flexible", "level": 4 }
                      ]
                    }
                  }
                }
                """).getAsJsonObject();
    }

    private static SearchNode item(String namespace, String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(new Identifier(namespace, path), NodeType.ITEM, displayName, 0, 0, metadata);
    }
}
