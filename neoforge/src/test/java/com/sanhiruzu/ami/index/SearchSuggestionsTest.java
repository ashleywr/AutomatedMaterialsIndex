package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.compat.CompatDisplayNames;
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
        index.addNode(item("minecraft", "spider_eye", "Spider Eye", Map.of(
                SearchNodeKeys.FACETS, "edible,magic_reagent"
        )));

        assertSuggests(index, "?", "?kind:");
        assertSuggests(index, "?", "?color:");
        assertSuggests(index, "?fact:stores", "?fact:stores_fe");
        assertSuggests(index, "?stores", "?stores_fe");
        assertSuggests(index, "?e", "?edible");
        assertSuggests(index, "?edi", "?edible");
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
    void suggestsGuidebookFieldForGuidePrefixes() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("patchouli", "guide_book", "Guide Book", Map.of(
                SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true",
                SearchNodeKeys.GUIDE_BOOK_SYSTEM, "patchouli",
                SearchNodeKeys.FACETS, "guide_book"
        )));

        assertSuggests(index, "?gui", "?guidebook:");
        assertSuggests(index, "?guidebook:patch", "?guidebook:patchouli");
        assertTrue(helpExamples(index).contains("?guidebook:"));
    }

    @Test
    void guidebookAliasesDoNotRepeatAsSimplePropertySuggestions() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("patchouli", "guide_book", "Guide Book", Map.of(
                SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true",
                SearchNodeKeys.FACETS, "guide_book",
                SearchNodeKeys.SEARCH_TOKENS, "guide guide_book guidebook guidebooks"
        )));
        index.addNode(item("example", "manual", "Manual", Map.of(
                SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true",
                SearchNodeKeys.FACETS, "guide_book",
                SearchNodeKeys.SEARCH_TOKENS, "guidebook"
        )));

        List<String> displays = suggestionDisplays(index, "?guide");

        assertEquals(List.of("?guidebook:"), displays.stream()
                .filter(display -> display.startsWith("?guide"))
                .toList());
        assertFalse(displays.contains("?guide_book"));
        assertFalse(displays.contains("?guidebook"));
        assertFalse(displays.contains("?guidebooks"));
        assertFalse(displays.contains("?guide"));
    }

    @Test
    void propertyFieldAliasesDoNotRepeatAsSimplePropertySuggestions() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("powah", "starter_cell", "Starter Cell", Map.of(
                "powahFacts", "energy,power,rf,portable_power",
                SearchNodeKeys.ENERGY_CAPACITY, "10000"
        )));
        index.addNode(item("silentgear", "material", "Material", Map.of(
                SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, "trait,traits,modifier,modifiers,malleable"
        )));

        List<String> energyDisplays = suggestionDisplays(index, "?energy");
        assertTrue(energyDisplays.contains("?energy:"));
        assertFalse(energyDisplays.contains("?energy"));
        assertFalse(energyDisplays.contains("?power"));
        assertFalse(energyDisplays.contains("?rf"));

        List<String> powerDisplays = suggestionDisplays(index, "?power");
        assertEquals(List.of("?energy:"), powerDisplays);

        List<String> traitDisplays = suggestionDisplays(index, "?trait");
        assertTrue(traitDisplays.contains("?trait:"));
        assertFalse(traitDisplays.contains("?traits"));
        assertFalse(traitDisplays.contains("?modifier"));
        assertFalse(traitDisplays.contains("?modifiers"));

        assertSuggests(index, "?portable", "?portable_power");
    }

    @Test
    void suggestsGregTechPropertyFieldsAndValuesFromGregTechMetadataOnly() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("powah", "starter_cell", "Starter Cell", Map.of(
                "powahTier", "starter",
                "powahItemKind", "energy_cell"
        )));
        index.addNode(item("gtceu", "good_electronic_circuit", "Good Electronic Circuit", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "gregtech",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "gregtech",
                SearchNodeKeys.GREGTECH_ITEM_KIND, "circuits",
                SearchNodeKeys.GREGTECH_FACTS, "circuit",
                SearchNodeKeys.GREGTECH_TIER, "mv",
                SearchNodeKeys.GREGTECH_CIRCUIT_GRADE, "good"
        )));
        index.addNode(item("gtceu", "ev_energy_input_hatch_4a", "EV Energy Input Hatch (4A)", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "gregtech",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "gregtech",
                SearchNodeKeys.GREGTECH_ITEM_KIND, "machines",
                SearchNodeKeys.GREGTECH_FACTS, "machine,power,inputs_eu",
                SearchNodeKeys.GREGTECH_TIER, "ev",
                SearchNodeKeys.GREGTECH_ENERGY_ROLE, "inputs_eu",
                SearchNodeKeys.GREGTECH_EU_INPUT, "8192",
                SearchNodeKeys.GREGTECH_AMPERAGE, "4"
        )));

        assertSuggests(index, "?", "?gregtechTier:");
        assertSuggests(index, "?", "?gregtechCircuit:");
        assertSuggests(index, "?", "?gregtechEnergy:");
        assertSuggests(index, "?gregtechT", "?gregtechTier:");
        assertSuggests(index, "?gregtechTier:m", "?gregtechTier:mv");
        assertDoesNotSuggest(index, "?gregtechTier:sta", "?gregtechTier:starter");
        assertSuggests(index, "?gregtechCircuit:g", "?gregtechCircuit:good");
        assertSuggests(index, "?gregtechKind:cir", "?gregtechKind:circuits");
        assertSuggests(index, "?gregtechFact:pow", "?gregtechFact:power");
        assertSuggests(index, "?gregtechEnergyRole:inputs", "?gregtechEnergyRole:inputs_eu");
        assertSuggests(index, "?gregtechEnergy:4", "?gregtechEnergy:4a");
    }

    @Test
    void gregTechModSuggestionUsesFriendlyCompatAlias() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("gtceu", "lv_macerator", "LV Macerator", Map.of(
                SearchNodeKeys.MOD_ID, "gtceu",
                SearchNodeKeys.COMPAT_FAMILIES, "gregtech",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "gregtech"
        )));

        assertSuggests(index, "@greg", "@GregTech");
        assertSuggests(index, "@gt", "@GregTech");
        assertSuggestion(index, "@gt", "@GregTech", "@gregtech ");
        assertDoesNotSuggest(index, "@gt", "@gtceu");
    }

    @Test
    void modPropertyFieldIsSupportedButNotDiscoverable() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("create", "shaft", "Shaft", Map.of(
                SearchNodeKeys.MOD_ID, "create"
        )));

        assertDoesNotSuggest(index, "?", "?mod:");
        assertDoesNotSuggest(index, "?m", "?mod:");
        assertSuggests(index, "@cr", "@create");
        assertFalse(helpExamples(index).contains("?mod:"));
    }

    @Test
    void helpOnlyListsPropertyFieldsBackedByIndexedValues() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("powah", "starter_cell", "Starter Cell", Map.of(
                "powahItemKind", "energy_cell",
                SearchNodeKeys.ENERGY_CAPACITY, "10000"
        )));

        List<String> examples = helpExamples(index);

        assertTrue(examples.contains("?energy:"));
        assertTrue(examples.contains("?kind:"));
        assertFalse(examples.contains("?guidebook:"));
        assertFalse(examples.contains("?gregtechCircuit:"));
    }

    @Test
    void amiSemanticBucketFieldIsSupportedButNotDiscoverable() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("minecraft", "coal", "Coal", Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, "ingredients",
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "fuel",
                SearchNodeKeys.RECIPE_USE_CATEGORIES, "ami:fuel,fuel",
                SearchNodeKeys.TAGS, "ami:fuel"
        )));

        assertDoesNotSuggest(index, "?a", "?ami:");
        assertSuggests(index, "?ami:f", "?ami:fuel");
        assertFalse(helpExamples(index).contains("?ami:"));
    }

    @Test
    void sameModTechnicalAliasesCollapseToCanonicalFriendlySuggestions() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("appliedenergistics2", "crafting_unit", "Crafting Unit", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "ae2",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "ae2"
        )));
        index.addNode(item("tconstruct", "part_builder", "Part Builder", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "tinkers",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "tinkers"
        )));
        index.addNode(item("silentgear", "pickaxe_blueprint", "Pickaxe Blueprint", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "silent_gear,modular_gear",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "silent_gear"
        )));

        assertSuggestion(index, "@applied", "@Applied Energistics 2", "@ae2 ");
        assertSuggestion(index, "@tc", "@Tinkers' Construct", "@tinkers ");
        assertSuggestion(index, "@silentg", "@Silent Gear", "@silent_gear ");
        assertDoesNotSuggest(index, "@applied", "@appliedenergistics2");
        assertDoesNotSuggest(index, "@tc", "@tconstruct");
        assertDoesNotSuggest(index, "@silentg", "@silentgear");
    }

    @Test
    void friendlyModSuggestionAliasesMatchWhenIndexAlreadyUsesCanonicalNamespace() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("ae2", "crafting_unit", "Crafting Unit", Map.of(
                SearchNodeKeys.MOD_ID, "ae2",
                SearchNodeKeys.COMPAT_FAMILY, "ae2",
                SearchNodeKeys.COMPAT_FAMILIES, "ae2",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "ae2"
        )));

        assertSuggestion(index, "@app", "@Applied Energistics 2", "@ae2 ");
    }

    @Test
    void modSuggestionCountDeduplicatesRepeatedModIdentityPerNode() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.addNode(item("ae2", "facade", "Cable Facade", Map.of(
                SearchNodeKeys.MOD_ID, "ae2",
                SearchNodeKeys.COMPAT_FAMILY, "ae2",
                SearchNodeKeys.COMPAT_FAMILIES, "ae2",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "ae2",
                SearchNodeKeys.VARIANT_SUPPRESSED_COUNT, "1439"
        )));
        index.addNode(item("ae2", "controller", "ME Controller", Map.of(
                SearchNodeKeys.MOD_ID, "ae2",
                SearchNodeKeys.COMPAT_FAMILY, "ae2",
                SearchNodeKeys.COMPAT_FAMILIES, "ae2",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "ae2"
        )));

        SearchSuggestions.Suggestion suggestion = SearchSuggestions.suggest(index, "@app", 4, 16).stream()
                .filter(s -> "@Applied Energistics 2".equals(s.display()))
                .findFirst()
                .orElseThrow();

        assertEquals("2", suggestion.detail());
    }

    @Test
    void modSuggestionAliasesIncludeGenericDisplayNameForAnyModToken() {
        assertTrue(CompatDisplayNames.modSuggestionAliases("thermal_expansion").contains("Thermal Expansion"));
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
        List<SearchSuggestions.Suggestion> suggestions = SearchSuggestions.suggest(index, query, query.length(), 16);
        assertTrue(suggestions.stream().anyMatch(s -> expectedDisplay.equals(s.display())),
                () -> "Expected " + expectedDisplay + " in " + suggestions);
    }

    private static void assertSuggestion(GlobalIndex index, String query, String expectedDisplay, String expectedReplacement) {
        List<SearchSuggestions.Suggestion> suggestions = SearchSuggestions.suggest(index, query, query.length(), 16);
        assertTrue(suggestions.stream().anyMatch(s -> expectedDisplay.equals(s.display())
                        && expectedReplacement.equals(s.replacement())),
                () -> "Expected " + expectedDisplay + " -> " + expectedReplacement + " in " + suggestions);
    }

    private static void assertDoesNotSuggest(GlobalIndex index, String query, String rejectedDisplay) {
        List<SearchSuggestions.Suggestion> suggestions = SearchSuggestions.suggest(index, query, query.length(), 16);
        assertFalse(suggestions.stream().anyMatch(s -> rejectedDisplay.equals(s.display())),
                () -> "Did not expect " + rejectedDisplay + " in " + suggestions);
    }

    private static List<String> suggestionDisplays(GlobalIndex index, String query) {
        return SearchSuggestions.suggest(index, query, query.length(), 16).stream()
                .map(SearchSuggestions.Suggestion::display)
                .toList();
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
