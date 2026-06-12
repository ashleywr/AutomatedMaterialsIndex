package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.discovery.AmiDiscoveryState;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryChecklistProjectionTest {
    private static final ResourceLocation PLAINS = new ResourceLocation("minecraft:plains");
    private static final ResourceLocation CHERRY_GROVE = new ResourceLocation("minecraft:cherry_grove");
    private static final ResourceLocation VILLAGE = new ResourceLocation("minecraft:village_plains");
    private static final ResourceLocation APPLE = new ResourceLocation("minecraft:apple");
    private static final ResourceLocation CARROT = new ResourceLocation("minecraft:carrot");

    @BeforeEach
    void setUp() {
        AmiConfig.resetToDefaults();
        AmiDiscoveryState.disablePersistenceForTests();
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @AfterEach
    void tearDown() {
        AmiConfig.resetToDefaults();
        AmiDiscoveryState.disablePersistenceForTests();
        GlobalIndex.getInstance().clear();
    }

    @Test
    void featureGateLeavesWorldNodesUndecorated() {
        AmiConfig.enableDiscoveryChecklist = false;
        SearchNode plains = biome(PLAINS, "Plains");

        SearchNode projected = ResultsViewProjector.applyRuntimeMetadataForLens(List.of(plains)).get(0);

        assertEquals("", projected.meta(SearchNodeKeys.DISCOVERY_STATE, ""));
        assertFalse(ListLens.availableFor(List.of(projected)).contains(ListLens.DISCOVERED));
        assertFalse(ListLens.availableFor(List.of(projected)).contains(ListLens.UNDISCOVERED));
    }

    @Test
    void projectedBiomesCarryDiscoveredOrUndiscoveredState() {
        AmiConfig.enableDiscoveryChecklist = true;
        AmiDiscoveryState.markBiomeDiscoveredForTests(PLAINS);
        AmiDiscoveryState.markStructureDiscoveredForTests(VILLAGE);
        AmiDiscoveryState.markFoodTastedForTests(APPLE);

        List<SearchNode> projected = ResultsViewProjector.applyRuntimeMetadataForLens(List.of(
                biome(PLAINS, "Plains"),
                biome(CHERRY_GROVE, "Cherry Grove"),
                structure(VILLAGE, "Plains Village"),
                food(APPLE, "Apple"),
                food(CARROT, "Carrot")
        ));

        assertEquals("discovered", projected.get(0).meta(SearchNodeKeys.DISCOVERY_STATE, ""));
        assertEquals("undiscovered", projected.get(1).meta(SearchNodeKeys.DISCOVERY_STATE, ""));
        assertEquals("discovered", projected.get(2).meta(SearchNodeKeys.DISCOVERY_STATE, ""));
        assertEquals("discovered", projected.get(3).meta(SearchNodeKeys.DISCOVERY_STATE, ""));
        assertEquals("undiscovered", projected.get(4).meta(SearchNodeKeys.DISCOVERY_STATE, ""));
    }

    @Test
    void listLensesFilterDiscoveryState() {
        AmiConfig.enableDiscoveryChecklist = true;
        AmiDiscoveryState.markBiomeDiscoveredForTests(PLAINS);
        List<SearchNode> projected = ResultsViewProjector.applyRuntimeMetadataForLens(List.of(
                biome(PLAINS, "Plains"),
                biome(CHERRY_GROVE, "Cherry Grove")
        ));

        assertTrue(ListLens.availableFor(projected).contains(ListLens.DISCOVERED));
        assertTrue(ListLens.availableFor(projected).contains(ListLens.UNDISCOVERED));
        assertEquals(List.of(PLAINS), ListLens.DISCOVERED.filter(projected).stream().map(SearchNode::id).toList());
        assertEquals(List.of(CHERRY_GROVE), ListLens.UNDISCOVERED.filter(projected).stream().map(SearchNode::id).toList());
    }

    @Test
    void foodDiscoveryTermsAreSearchableWithRegularItemQueries() {
        AmiConfig.enableDiscoveryChecklist = true;
        AmiDiscoveryState.markFoodTastedForTests(APPLE);
        SearchNode apple = food(APPLE, "Apple");
        SearchNode carrot = food(CARROT, "Carrot");
        GlobalIndex.getInstance().addNode(apple);
        GlobalIndex.getInstance().addNode(carrot);
        SearchService searchService = SearchService.buildFrom(GlobalIndex.getInstance(), false);

        SearchState tastedApple = new SearchState();
        tastedApple.setQuery("tasted apple");
        SearchState untastedApple = new SearchState();
        untastedApple.setQuery("untasted apple");
        SearchState untastedCarrot = new SearchState();
        untastedCarrot.setQuery("untasted carrot");

        assertEquals(1, ResultsViewProjector.project(List.of(apple, carrot), tastedApple, searchService, false, false).displayedItemCount());
        assertEquals(0, ResultsViewProjector.project(List.of(apple, carrot), untastedApple, searchService, false, false).displayedItemCount());
        assertEquals(1, ResultsViewProjector.project(List.of(apple, carrot), untastedCarrot, searchService, false, false).displayedItemCount());
    }

    @Test
    void visitedTermsAreSearchableForWorldResults() {
        AmiConfig.enableDiscoveryChecklist = true;
        AmiDiscoveryState.markBiomeDiscoveredForTests(PLAINS);
        SearchNode plains = biome(PLAINS, "Plains");
        SearchNode cherry = biome(CHERRY_GROVE, "Cherry Grove");
        GlobalIndex.getInstance().addNode(plains);
        GlobalIndex.getInstance().addNode(cherry);
        SearchService searchService = SearchService.buildFrom(GlobalIndex.getInstance(), false);

        SearchState visitedPlains = new SearchState();
        visitedPlains.setQuery("visited plains");
        SearchState unvisitedPlains = new SearchState();
        unvisitedPlains.setQuery("unvisited plains");

        assertEquals(1, ResultsViewProjector.project(List.of(plains, cherry), visitedPlains, searchService, false, false).displayedItemCount());
        assertEquals(0, ResultsViewProjector.project(List.of(plains, cherry), unvisitedPlains, searchService, false, false).displayedItemCount());
    }

    private static SearchNode biome(ResourceLocation id, String name) {
        return new SearchNode(id, NodeType.BIOME, name, 0, 0, Map.of(SearchNodeKeys.DIMENSION, "overworld"));
    }

    private static SearchNode structure(ResourceLocation id, String name) {
        return new SearchNode(id, NodeType.STRUCTURE, name, 0, 0, Map.of(SearchNodeKeys.DIMENSION, "overworld"));
    }

    private static SearchNode food(ResourceLocation id, String name) {
        return new SearchNode(id, NodeType.ITEM, name, 0, 0, Map.of(SearchNodeKeys.FOOD_NUTRITION, "4"));
    }
}
