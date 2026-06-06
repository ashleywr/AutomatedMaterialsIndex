package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.GregTechCompat;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredCompatSearchTest {
    @AfterEach
    void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void structuredCompatQueriesResolveFactsKindsTiersRolesAndCapabilities() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode mixer = item("create", "mechanical_mixer", "Mechanical Mixer", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "create",
                SearchNodeKeys.CREATE_ITEM_KIND, "machines",
                SearchNodeKeys.CREATE_FACTS, "uses_su,kinetic,create_processing",
                SearchNodeKeys.CREATE_RECIPE_ROLES, "mixing_input,mixing_output",
                SearchNodeKeys.CREATE_STRESS_ROLE, "consumes_su"
        ));
        SearchNode tank = item("mekanism", "basic_fluid_tank", "Basic Fluid Tank", Map.of(
                SearchNodeKeys.MEKANISM_ITEM_KIND, "machines",
                SearchNodeKeys.MEKANISM_TIER, "basic",
                SearchNodeKeys.MEKANISM_FACTS, "machine,fluid_or_heat",
                SearchNodeKeys.FLUID_CAPACITY, "14"
        ));
        SearchNode upgrade = item("sophisticatedbackpacks", "stack_upgrade_tier_1", "Stack Upgrade", Map.of(
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "sophisticated",
                SearchNodeKeys.SOPHISTICATED_ITEM_KIND, "upgrades",
                SearchNodeKeys.SOPHISTICATED_FACTS, "upgrade",
                SearchNodeKeys.SOPHISTICATED_TIER, "basic"
        ));
        SearchNode terminal = item("ae2", "wireless_terminal", "Wireless Terminal", Map.of(
                SearchNodeKeys.AE2_ITEM_KIND, "terminals",
                SearchNodeKeys.AE2_FACTS, "terminal,network"
        ));
        SearchNode gregTechMacerator = item("gtceu", "lv_macerator", "LV Macerator", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "gregtech",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "gregtech",
                SearchNodeKeys.GREGTECH_ITEM_KIND, "machines",
                SearchNodeKeys.GREGTECH_FACTS, "machine,consumes_eu",
                SearchNodeKeys.GREGTECH_TIER, "lv",
                SearchNodeKeys.GREGTECH_ENERGY_ROLE, "consumes_eu",
                SearchNodeKeys.GREGTECH_EU_CONSUMPTION, "32"
        ));
        SearchNode gregTechCircuit = item("gtceu", "basic_electronic_circuit", "Basic Electronic Circuit", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "gregtech",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "gregtech",
                SearchNodeKeys.GREGTECH_ITEM_KIND, "circuits",
                SearchNodeKeys.GREGTECH_FACTS, "circuit",
                SearchNodeKeys.GREGTECH_TIER, "lv",
                SearchNodeKeys.GREGTECH_CIRCUIT_GRADE, "basic"
        ));
        SearchNode gregTechInputHatch = item("gtceu", "ev_energy_input_hatch_4a", "EV Energy Input Hatch (4A)", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "gregtech",
                SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "gregtech",
                SearchNodeKeys.GREGTECH_ITEM_KIND, "machines",
                SearchNodeKeys.GREGTECH_FACTS, "machine,power,inputs_eu",
                SearchNodeKeys.GREGTECH_TIER, "ev",
                SearchNodeKeys.GREGTECH_ENERGY_ROLE, "inputs_eu",
                SearchNodeKeys.GREGTECH_EU_INPUT, "8192",
                SearchNodeKeys.GREGTECH_AMPERAGE, "4"
        ));
        SearchNode externalBattery = item("powah", "starter_cell", "Starter Cell", Map.of(
                "powahItemKind", "energy",
                "powahFacts", "stores_fe,energy",
                "powahTier", "starter",
                SearchNodeKeys.COLOR_BUCKET, "red",
                SearchNodeKeys.ENERGY_CAPACITY, "10000"
        ));
        SearchNode pickaxeBlueprint = item("silentgear", "pickaxe_blueprint", "Pickaxe Blueprint", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "silent_gear,modular_gear",
                SearchNodeKeys.MODULAR_GEAR_FAMILY, "silent_gear",
                SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, "blueprints",
                SearchNodeKeys.MODULAR_GEAR_PART, "pickaxe",
                SearchNodeKeys.MODULAR_GEAR_FACTS, "blueprint,tool",
                SearchNodeKeys.MODULAR_GEAR_RUNTIME_MATERIALS, "crimson_iron",
                SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, "magnetic,brittle",
                SearchNodeKeys.SEARCH_TOKENS, "gear_trait_magnetic gear_trait_brittle"
        ));

        index.addNode(mixer);
        index.addNode(tank);
        index.addNode(upgrade);
        index.addNode(terminal);
        index.addNode(gregTechMacerator);
        index.addNode(gregTechCircuit);
        index.addNode(gregTechInputHatch);
        index.addNode(externalBattery);
        index.addNode(pickaxeBlueprint);
        SearchService service = SearchService.buildFrom(index, false);

        assertOnlyContains(service.query("?fact:uses_su").get(NodeType.ITEM), mixer, tank);
        assertOnlyContains(service.query("?kind:terminals").get(NodeType.ITEM), terminal, mixer);
        assertOnlyContains(service.query("?tier:basic").get(NodeType.ITEM), tank, mixer);
        assertTrue(service.query("?tier:basic").get(NodeType.ITEM).contains(upgrade));
        assertOnlyContains(service.query("?role:mixing_input").get(NodeType.ITEM), mixer, tank);
        assertOnlyContains(service.query("?capability:fluid").get(NodeType.ITEM), tank, mixer);
        assertOnlyContains(service.query("~fluid").get(NodeType.ITEM), tank, mixer);
        assertOnlyContains(service.query("?upgrade").get(NodeType.ITEM), upgrade, terminal);
        assertOnlyContains(service.query("?machine").get(NodeType.ITEM), tank, terminal);
        assertOnlyContains(service.query("?gregtech").get(NodeType.ITEM), gregTechMacerator, terminal);
        assertOnlyContains(service.query("?gregtechTier:lv").get(NodeType.ITEM), gregTechMacerator, tank);
        assertTrue(service.query("?gregtechTier:lv").get(NodeType.ITEM).contains(gregTechCircuit));
        assertOnlyContains(service.query("?gregtechKind:machines").get(NodeType.ITEM), gregTechMacerator, terminal);
        assertOnlyContains(service.query("?gregtechFact:machine").get(NodeType.ITEM), gregTechMacerator, terminal);
        assertOnlyContains(service.query("?gregtechCircuit:basic").get(NodeType.ITEM), gregTechCircuit, gregTechMacerator);
        assertOnlyContains(service.query("?gregtechGrade:basic").get(NodeType.ITEM), gregTechCircuit, gregTechMacerator);
        assertOnlyContains(service.query("?gregtechEnergy").get(NodeType.ITEM), gregTechMacerator, terminal);
        assertTrue(service.query("?gregtechEnergy").get(NodeType.ITEM).contains(gregTechInputHatch));
        assertOnlyContains(service.query("?gregtechEnergyRole:inputs_eu").get(NodeType.ITEM), gregTechInputHatch, gregTechMacerator);
        assertOnlyContains(service.query("?gregtechEnergy:4a").get(NodeType.ITEM), gregTechInputHatch, gregTechMacerator);
        assertOnlyContains(service.query("~inputs_eu").get(NodeType.ITEM), gregTechInputHatch, gregTechMacerator);
        assertOnlyContains(service.query("?tier:starter").get(NodeType.ITEM), externalBattery, tank);
        assertOnlyContains(service.query("?fact:stores_fe").get(NodeType.ITEM), externalBattery, terminal);
        assertOnlyContains(service.query("?capability:energy").get(NodeType.ITEM), externalBattery, tank);
        assertTrue(service.query("?capability:energy").get(NodeType.ITEM).contains(gregTechMacerator));
        assertTrue(service.query("?capability:energy").get(NodeType.ITEM).contains(gregTechInputHatch));
        assertOnlyContains(service.query("?energy").get(NodeType.ITEM), externalBattery, tank);
        assertTrue(service.query("?energy").get(NodeType.ITEM).contains(gregTechMacerator));
        assertTrue(service.query("?energy").get(NodeType.ITEM).contains(gregTechInputHatch));
        assertOnlyContains(service.query("?color:red").get(NodeType.ITEM), externalBattery, tank);
        assertOnlyContains(service.query("?gear").get(NodeType.ITEM), pickaxeBlueprint, terminal);
        assertOnlyContains(service.query("?part:pickaxe").get(NodeType.ITEM), pickaxeBlueprint, terminal);
        assertOnlyContains(service.query("?trait:blueprint").get(NodeType.ITEM), pickaxeBlueprint, terminal);
        assertFalse(service.query("?trait:magnetic").getOrDefault(NodeType.ITEM, List.of()).contains(pickaxeBlueprint));
        assertOnlyContains(service.query("?runtime_trait:magnetic").get(NodeType.ITEM), pickaxeBlueprint, terminal);
        assertFalse(service.query("?gear_trait_magnetic").getOrDefault(NodeType.ITEM, List.of()).contains(pickaxeBlueprint));
        assertOnlyContains(service.query("?token:gear_trait_magnetic").get(NodeType.ITEM), pickaxeBlueprint, terminal);
        assertOnlyContains(service.query("~gear_trait_magnetic").get(NodeType.ITEM), pickaxeBlueprint, terminal);
        assertOnlyContains(service.query("?material:crimson_iron").get(NodeType.ITEM), pickaxeBlueprint, terminal);
        assertOnlyContains(service.query("~magnetic").get(NodeType.ITEM), pickaxeBlueprint, terminal);
        assertOnlyContains(service.query("~energy").get(NodeType.ITEM), externalBattery, tank);
        assertOnlyContains(service.query("~stores_fe").get(NodeType.ITEM), externalBattery, terminal);
    }

    @Test
    void modPrefixAlsoMatchesCompatFamiliesForAddonEcosystems() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode addonTrain = item("railways", "track_coupler", "Track Coupler", Map.of(
                SearchNodeKeys.COMPAT_FAMILIES, "create",
                SearchNodeKeys.CREATE_FACTS, "kinetic"
        ));
        SearchNode vanillaRail = item("minecraft", "rail", "Rail", Map.of());

        index.addNode(addonTrain);
        index.addNode(vanillaRail);
        SearchService service = SearchService.buildFrom(index, false);

        List<SearchNode> createFamily = service.query("@create").get(NodeType.ITEM);
        assertTrue(createFamily.contains(addonTrain));
        assertFalse(createFamily.contains(vanillaRail));
    }

    @Test
    void guidebookPropertyQueriesMatchOnlyGuideBookCandidates() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode fieldGuide = item("example", "field_guide", "Field Guide", Map.of(
                SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true",
                SearchNodeKeys.FACETS, "book,guide_book"
        ));
        SearchNode patchouliManual = item("example", "manual", "Manual", Map.of(
                SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true",
                SearchNodeKeys.GUIDE_BOOK_SYSTEM, "patchouli"
        ));
        SearchNode plainBook = item("minecraft", "book", "Book", Map.of(
                SearchNodeKeys.FACETS, "book"
        ));

        index.addNode(fieldGuide);
        index.addNode(patchouliManual);
        index.addNode(plainBook);
        SearchService service = SearchService.buildFrom(index, false);

        assertTrue(service.query("?guidebook").get(NodeType.ITEM).contains(fieldGuide));
        assertTrue(service.query("?type:guidebook").get(NodeType.ITEM).contains(fieldGuide));
        assertTrue(service.query("?type:guidebook").get(NodeType.ITEM).contains(patchouliManual));
        assertTrue(service.query("?guidebook:patchouli").get(NodeType.ITEM).contains(patchouliManual));
        assertFalse(service.query("?type:guidebook").get(NodeType.ITEM).contains(plainBook));
    }

    @Test
    void gregTechEnrichedMetadataIsIndexedForPlainStructuredAndNumericSearch() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode macerator = enrichedGregTechItem("lv_macerator", "LV Macerator", Map.of(
                SearchNodeKeys.ITEM_CLASS, "com.gregtechceu.gtceu.api.item.MetaMachineItem"
        ));
        SearchNode circuit = enrichedGregTechItem("basic_electronic_circuit", "Basic Electronic Circuit", Map.of(
                SearchNodeKeys.TAGS, "gtceu:circuits,gtceu:circuits/lv"
        ));
        SearchNode inputHatch = enrichedGregTechItem("ev_energy_input_hatch_4a", "EV Energy Input Hatch (4A)", Map.of(
                SearchNodeKeys.ITEM_CLASS, "com.gregtechceu.gtceu.api.item.MetaMachineItem"
        ));

        index.addNode(macerator);
        index.addNode(circuit);
        index.addNode(inputHatch);
        SearchService service = SearchService.buildFrom(index, false);

        assertTrue(service.query("~lv_tier").get(NodeType.ITEM).contains(macerator));
        assertTrue(service.query("~basic_circuit").get(NodeType.ITEM).contains(circuit));
        assertTrue(service.query("~inputs_eu").get(NodeType.ITEM).contains(inputHatch));
        assertTrue(service.query("?gregtechTier:lv").get(NodeType.ITEM).contains(macerator));
        assertTrue(service.query("?gregtechCircuit:basic").get(NodeType.ITEM).contains(circuit));
        assertTrue(service.query("?gregtechEnergyRole:inputs_eu").get(NodeType.ITEM).contains(inputHatch));
        assertTrue(service.query("=euconsume:32").get(NodeType.ITEM).contains(macerator));
        assertTrue(service.query("=euinput:8192").get(NodeType.ITEM).contains(inputHatch));
        assertFalse(service.query(">eugen:0").getOrDefault(NodeType.ITEM, List.of()).contains(macerator));
    }

    private static SearchNode item(String namespace, String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(new ResourceLocation(namespace, path), NodeType.ITEM, displayName, 0, 0, metadata);
    }

    private static SearchNode enrichedGregTechItem(String path, String displayName, Map<String, String> metadata) {
        Map<String, String> mutable = new HashMap<>(metadata);
        mutable.putIfAbsent(SearchNodeKeys.MOD_ID, "gtceu");
        ResourceLocation id = new ResourceLocation("gtceu", path);
        GregTechCompat.enrichItem(id, mutable);
        return new SearchNode(id, NodeType.ITEM, displayName, 0, 0, Map.copyOf(mutable));
    }

    private static void assertOnlyContains(List<SearchNode> results, SearchNode expected, SearchNode unexpected) {
        assertTrue(results != null && results.contains(expected));
        if (unexpected != null) {
            assertFalse(results.contains(unexpected));
        }
    }
}
