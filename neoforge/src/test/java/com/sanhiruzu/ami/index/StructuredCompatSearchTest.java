package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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

        index.addNode(mixer);
        index.addNode(tank);
        index.addNode(upgrade);
        index.addNode(terminal);
        SearchService service = SearchService.buildFrom(index, false);

        assertOnlyContains(service.query("?fact:uses_su").get(NodeType.ITEM), mixer, tank);
        assertOnlyContains(service.query("?kind:terminals").get(NodeType.ITEM), terminal, mixer);
        assertOnlyContains(service.query("?tier:basic").get(NodeType.ITEM), tank, mixer);
        assertTrue(service.query("?tier:basic").get(NodeType.ITEM).contains(upgrade));
        assertOnlyContains(service.query("?role:mixing_input").get(NodeType.ITEM), mixer, tank);
        assertOnlyContains(service.query("?capability:fluid").get(NodeType.ITEM), tank, mixer);
        assertOnlyContains(service.query("?upgrade").get(NodeType.ITEM), upgrade, terminal);
        assertOnlyContains(service.query("?machine").get(NodeType.ITEM), tank, terminal);
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

    private static SearchNode item(String namespace, String path, String displayName, Map<String, String> metadata) {
        return new SearchNode(new ResourceLocation(namespace, path), NodeType.ITEM, displayName, 0, 0, metadata);
    }

    private static void assertOnlyContains(List<SearchNode> results, SearchNode expected, SearchNode unexpected) {
        assertTrue(results != null && results.contains(expected));
        if (unexpected != null) {
            assertFalse(results.contains(unexpected));
        }
    }
}
