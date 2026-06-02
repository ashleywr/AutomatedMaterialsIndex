package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.query.SearchSuggestions;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchSuggestionsTest {
    @AfterEach
    void cleanup() {
        GlobalIndex.getInstance().clear();
        AmiConfig.resetToDefaults();
    }

    @Test
    void suggestsConventionMetadataValuesFromCurrentIndex() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("powah", "starter_cell", "Starter Cell", Map.of(
                "powahItemKind", "energy_cell",
                "powahFacts", "stores_fe,portable_power",
                "powahTier", "starter",
                SearchNodeKeys.ENERGY_CAPACITY, "10000"
        )));
        index.addNode(item("examplecompat", "washer", "Washer", Map.of(
                "exampleRoles", "washing_input,washing_output",
                "exampleFacts", "fluid_processing",
                SearchNodeKeys.COLOR_BUCKET, "red",
                SearchNodeKeys.FLUID_CAPACITY, "4"
        )));

        assertSuggests(index, "?", "?kind:");
        assertSuggests(index, "?", "?color:");
        assertSuggests(index, "?fact:stores", "?fact:stores_fe");
        assertSuggests(index, "?stores", "?stores_fe");
        assertSuggests(index, "?kind:energy", "?kind:energy_cell");
        assertSuggests(index, "?energy", "?energy:");
        assertSuggests(index, "?color:r", "?color:red");
        assertSuggests(index, "?tier:sta", "?tier:starter");
        assertSuggests(index, "?role:washing", "?role:washing_input");
        assertSuggests(index, "?capability:ene", "?capability:energy");
        assertSuggests(index, "~ene", "~energy");
        assertSuggests(index, ">en", ">energy:");
    }

    @Test
    void suggestsModsFamiliesAndTagsThatResolversCanUse() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("addonmod", "copper_drawer", "Copper Drawer", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "storagedrawers",
                SearchNodeKeys.TAGS, "c:storage_blocks,ami:drawer",
                SearchNodeKeys.BLOCK_TAGS, "minecraft:mineable/axe"
        )));

        assertSuggests(index, "@storage", "@storagedrawers");
        assertSuggests(index, "#minecraft:mine", "#minecraft:mineable/axe");
    }

    @Test
    void suggestsCategoriesEnvironmentsAndPrefixShortcuts() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("example", "battery", "Battery", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "tech",
                SearchNodeKeys.ENERGY_CAPACITY, "10000"
        )));
        index.addNode(node("minecraft", "the_nether", NodeType.DIMENSION, "The Nether", Map.of()));
        index.addNode(item("cobblemon", "field_guide", "Field Guide", Map.of(
                SearchNodeKeys.POKEMON_EGG_GROUPS, "monster,field"
        )));

        assertSuggests(index, "$te", "$tech");
        assertSuggests(index, "&net", "&the_nether");
        assertSuggests(index, "%", "%egg:");
        assertSuggests(index, "%egg:mon", "%egg:monster");
    }

    @Test
    void gatesPokemonShortcutsAndHelpByIndexedMetadata() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("minecraft", "stick", "Stick", Map.of()));

        assertDoesNotSuggest(index, "%", "%egg:");
        assertFalse(helpExamples(index).stream().anyMatch(example -> example.startsWith("@type:")
                || example.startsWith("#move:")
                || example.startsWith("%egg:")));
        assertFalse(helpExamples(index).contains("@create"));
        assertTrue(helpExamples(index).contains("@minecraft"));

        index.addNode(item("cobblemon", "pikachu", "Pikachu", Map.of(
                SearchNodeKeys.POKEMON_TYPE, "electric",
                SearchNodeKeys.POKEMON_MOVE, "thunderbolt",
                SearchNodeKeys.POKEMON_EGG_GROUPS, "field"
        )));

        assertSuggests(index, "%", "%egg:");
        List<String> examples = helpExamples(index);
        assertTrue(examples.contains("@type:electric"));
        assertTrue(examples.contains("#move:thunderbolt"));
        assertTrue(examples.contains("%egg:field"));
    }

    @Test
    void appliesSuggestionAcrossActiveTokenOnly() {
        SearchSuggestions.Suggestion suggestion = new SearchSuggestions.Suggestion(
                "?fact:stores_fe ",
                "?fact:stores_fe",
                "1",
                6,
                18
        );

        assertEquals("chest ?fact:stores_fe diamond", SearchSuggestions.apply("chest ?fact:stores diamond", suggestion));
    }

    @Test
    void traitSuggestionsAreTwoStageAndUseVisibleMaterialTraits() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("silentgear", "crimson_steel_ingot", "Crimson Steel Ingot", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL,
                SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, "malleable,malleable_v,hard_iii",
                SearchNodeKeys.SEARCH_TOKENS, "gear_trait_malleable_v"
        )));

        List<SearchSuggestions.Suggestion> fieldSuggestions = SearchSuggestions.suggest(index, "?tr", 3, 8);
        assertTrue(fieldSuggestions.stream().anyMatch(s -> "?trait:".equals(s.display())),
                () -> "Expected ?trait: in " + fieldSuggestions);
        assertFalse(fieldSuggestions.stream().anyMatch(s -> s.display().startsWith("?trait:malleable")),
                () -> "Trait values should wait until the field prefix is accepted: " + fieldSuggestions);

        assertSuggests(index, "?trait:", "?trait:malleable");
        assertSuggests(index, "?trait:malleable_", "?trait:malleable_v");
    }

    @Test
    void suggestionsRespectCheatAndDevVisibility() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("silentgear", "fishing_rod/variant/crimson_steel", "Crimson Steel Fishing Rod", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_CHEAT,
                SearchNodeKeys.SEARCH_TOKENS, "gear_trait_hidden_cheat"
        )));
        index.addNode(item("example", "debug_probe", "Debug Probe", Map.of(
                SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_DEV,
                "exampleFacts", "debug_probe_fact"
        )));

        assertDoesNotSuggest(index, "~gear_trait_hidden", "~gear_trait_hidden_cheat");
        assertDoesNotSuggest(index, "?fact:debug", "?fact:debug_probe_fact");

        AmiConfig.cheatMode = true;
        assertSuggests(index, "~gear_trait_hidden", "~gear_trait_hidden_cheat");
        assertDoesNotSuggest(index, "?fact:debug", "?fact:debug_probe_fact");

        AmiConfig.cheatMode = false;
        AmiConfig.devMode = true;
        assertSuggests(index, "~gear_trait_hidden", "~gear_trait_hidden_cheat");
        assertSuggests(index, "?fact:debug", "?fact:debug_probe_fact");
    }

    @Test
    void emptyQuerySuggestionsAreIndexedExamplesNotHistory() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("powah", "starter_cell", "Starter Cell", Map.of(
                "powahFacts", "stores_fe,energy",
                "powahItemKind", "energy_cell",
                SearchNodeKeys.ENERGY_CAPACITY, "10000"
        )));
        index.addNode(item("storagedrawers", "oak_drawer", "Oak Drawer", Map.of(
                SearchNodeKeys.STORAGE_ITEM_KIND, "drawer",
                SearchNodeKeys.STORAGE_FACTS, "storage,bulk_storage"
        )));

        List<SearchSuggestions.Suggestion> suggestions = SearchSuggestions.suggest(index, "", 0, 12);

        assertTrue(suggestions.size() > 2);
        assertTrue(suggestions.stream().allMatch(SearchSuggestions.Suggestion::example));
        assertTrue(suggestions.stream().anyMatch(s -> s.display().equals("?kind:")));
        assertTrue(suggestions.stream().anyMatch(s -> s.display().equals("?capability:")));
        assertTrue(suggestions.stream().anyMatch(s -> s.display().startsWith("@")));
        assertTrue(suggestions.stream().noneMatch(s -> s.display().startsWith("~")));
        assertTrue(suggestions.stream().noneMatch(s -> s.display().startsWith("#")));
        assertTrue(suggestions.stream().noneMatch(s ->
                s.display().startsWith("?capability:") && !s.display().equals("?capability:")));
        assertTrue(suggestions.stream().noneMatch(s -> s.display().contains("typed_before")));
    }

    private static void assertSuggests(GlobalIndex index, String query, String expectedDisplay) {
        List<SearchSuggestions.Suggestion> suggestions = SearchSuggestions.suggest(index, query, query.length(), 8);
        assertTrue(suggestions.stream().anyMatch(s -> expectedDisplay.equals(s.display())),
                () -> "Expected " + expectedDisplay + " in " + suggestions);
    }

    private static void assertDoesNotSuggest(GlobalIndex index, String query, String rejectedDisplay) {
        List<SearchSuggestions.Suggestion> suggestions = SearchSuggestions.suggest(index, query, query.length(), 8);
        assertFalse(suggestions.stream().anyMatch(s -> rejectedDisplay.equals(s.display())),
                () -> "Did not expect " + rejectedDisplay + " in " + suggestions);
    }

    private static List<String> helpExamples(GlobalIndex index) {
        return java.util.stream.Stream.concat(
                        SearchSuggestions.helpLayout(index).leftSections().stream(),
                        SearchSuggestions.helpLayout(index).rightSections().stream())
                .flatMap(section -> section.examples().stream())
                .map(com.sanhiruzu.ami.index.query.SearchSyntax.Example::text)
                .toList();
    }

    private static SearchNode item(String namespace, String path, String displayName, Map<String, String> metadata) {
        return node(namespace, path, NodeType.ITEM, displayName, metadata);
    }

    private static SearchNode node(String namespace, String path, NodeType type, String displayName, Map<String, String> metadata) {
        return new SearchNode(new ResourceLocation(namespace, path), type, displayName, 0, 0, metadata);
    }
}
