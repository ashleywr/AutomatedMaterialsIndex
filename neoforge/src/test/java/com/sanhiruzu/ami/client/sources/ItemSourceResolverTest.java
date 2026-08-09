package com.sanhiruzu.ami.client.sources;

import com.sanhiruzu.ami.index.EdgeType;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSourceResolverTest {
    @org.junit.jupiter.api.BeforeEach
    void clearGlobalIndex() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void directMobDropIncludesSpawnBiomes() {
        SearchNode leather = item("leather", "Leather");
        SearchNode cow = entity("cow", "Cow");
        SearchNode plains = biome("plains", "Plains");
        SearchNode meadow = biome("meadow", "Meadow");
        cow.addResolvedEdge(EdgeType.DROPS, leather);
        cow.addResolvedEdge(EdgeType.SPAWNS_IN, plains);
        cow.addResolvedEdge(EdgeType.SPAWNS_IN, meadow);

        ItemSourceReport report = resolver(leather, cow, plains, meadow).resolve(leather);

        assertEquals("ami.sources.title.named", report.title().getString());
        assertEquals(List.of(ItemSourceType.MOB_DROP), report.groupOrder());
        assertEquals(
                List.of("Cow -> drops Leather -> spawns in Plains, Meadow"),
                report.rows(ItemSourceType.MOB_DROP).stream().map(ItemSourceRow::text).toList()
        );
        ItemSourceRow row = report.rows(ItemSourceType.MOB_DROP).get(0);
        assertEquals("Cow", row.primaryLink().label());
        assertEquals(cow, row.primaryLink().node());
        assertEquals("drops Leather", row.routeSummary());
        assertEquals("drop", row.routeActionLabel());
        assertEquals("Leather", row.routeOutputLink().label());
        assertEquals(leather, row.routeOutputLink().node());
        assertEquals(
                List.of("Plains", "Meadow"),
                row.biomeLinks().stream().map(ItemSourceLink::label).toList()
        );
    }

    @Test
    void recipeIngredientWithMobDropCreatesOneHopIndirectRoute() {
        SearchNode leather = item("leather", "Leather");
        SearchNode rabbitHide = item("rabbit_hide", "Rabbit Hide");
        SearchNode craftingTable = item("crafting_table", "Crafting Table");
        SearchNode rabbit = entity("rabbit", "Rabbit");
        SearchNode desert = biome("desert", "Desert");
        SearchNode recipe = recipe("rabbit_hide_to_leather", "crafting");
        recipe.addResolvedEdge(EdgeType.PRODUCES, leather);
        recipe.addResolvedEdge(EdgeType.REQUIRES, rabbitHide);
        leather.addResolvedEdge(EdgeType.OUTPUT_OF, recipe);
        rabbit.addResolvedEdge(EdgeType.DROPS, rabbitHide);
        rabbit.addResolvedEdge(EdgeType.SPAWNS_IN, desert);

        ItemSourceReport report = resolver(leather, rabbitHide, craftingTable, rabbit, desert, recipe).resolve(leather);

        assertEquals(
                List.of("Crafting Table -> Leather"),
                report.rows(ItemSourceType.RECIPE).stream().map(ItemSourceRow::text).toList()
        );
        assertEquals(
                List.of("Rabbit -> drops Rabbit Hide -> spawns in Desert -> Crafting Table -> Leather"),
                report.rows(ItemSourceType.INDIRECT_SOURCE).stream().map(ItemSourceRow::text).toList()
        );
        ItemSourceRow recipeRow = report.rows(ItemSourceType.RECIPE).get(0);
        assertEquals("Crafting Table", recipeRow.primaryLink().label());
        assertEquals(craftingTable, recipeRow.primaryLink().node());
        assertEquals("makes Leather", recipeRow.routeSummary());
        assertEquals("recipe", recipeRow.routeActionLabel());
        assertEquals(leather, recipeRow.routeOutputLink().node());
        assertTrue(recipeRow.biomeLinks().isEmpty());

        ItemSourceRow indirectRow = report.rows(ItemSourceType.INDIRECT_SOURCE).get(0);
        assertEquals("Rabbit", indirectRow.primaryLink().label());
        assertEquals("drops Rabbit Hide -> Crafting Table -> Leather", indirectRow.routeSummary());
        assertEquals(List.of("Desert"), indirectRow.biomeLinks().stream().map(ItemSourceLink::label).toList());
    }

    @Test
    void resolveUsesCanonicalIndexedNodeWhenClickedNodeHasNoEdges() {
        SearchNode indexedLeather = item("leather", "Leather");
        SearchNode clickedLeather = item("leather", "Leather");
        SearchNode rabbitHide = item("rabbit_hide", "Rabbit Hide");
        SearchNode craftingTable = item("crafting_table", "Crafting Table");
        SearchNode recipe = recipe("rabbit_hide_to_leather", "crafting");
        recipe.addResolvedEdge(EdgeType.REQUIRES, rabbitHide);
        indexedLeather.addResolvedEdge(EdgeType.OUTPUT_OF, recipe);

        ItemSourceReport report = resolver(indexedLeather, clickedLeather, rabbitHide, craftingTable, recipe).resolve(clickedLeather);

        assertEquals(
                List.of("Crafting Table -> Leather"),
                report.rows(ItemSourceType.RECIPE).stream().map(ItemSourceRow::text).toList()
        );
    }

    @Test
    void recipeSourcesResolveUnresolvedRecipeGraphEdgesFromIndexedNodes() {
        SearchNode leather = item("leather", "Leather");
        SearchNode rabbitHide = item("rabbit_hide", "Rabbit Hide");
        SearchNode craftingTable = item("crafting_table", "Crafting Table");
        SearchNode recipe = recipe("rabbit_hide_to_leather", "crafting");
        recipe.addResolvedEdge(EdgeType.REQUIRES, rabbitHide);
        leather.addUnresolvedEdge(EdgeType.OUTPUT_OF, recipe.id());

        ItemSourceReport report = resolver(leather, rabbitHide, craftingTable, recipe).resolve(leather);

        assertEquals(
                List.of("Crafting Table -> Leather"),
                report.rows(ItemSourceType.RECIPE).stream().map(ItemSourceRow::text).toList()
        );
    }

    @Test
    void recipeSourcesFallbackToRecipeProducesEdgesWhenOutputReverseEdgeIsMissing() {
        SearchNode leather = item("leather", "Leather");
        SearchNode rabbitHide = item("rabbit_hide", "Rabbit Hide");
        SearchNode craftingTable = item("crafting_table", "Crafting Table");
        SearchNode recipe = recipe("rabbit_hide_to_leather", "crafting");
        recipe.addUnresolvedEdge(EdgeType.PRODUCES, leather.id());
        recipe.addResolvedEdge(EdgeType.REQUIRES, rabbitHide);

        ItemSourceReport report = resolver(leather, rabbitHide, craftingTable, recipe).resolve(leather);

        assertEquals(
                List.of("Crafting Table -> Leather"),
                report.rows(ItemSourceType.RECIPE).stream().map(ItemSourceRow::text).toList()
        );
    }

    @Test
    void sourceBiomeLabelsDropRedundantBiomeSuffix() {
        SearchNode leather = item("leather", "Leather");
        SearchNode cow = entity("cow", "Cow");
        SearchNode savanna = biome("savanna", "Savanna Biome");
        cow.addResolvedEdge(EdgeType.DROPS, leather);
        cow.addResolvedEdge(EdgeType.SPAWNS_IN, savanna);

        ItemSourceReport report = resolver(leather, cow, savanna).resolve(leather);

        assertEquals(
                List.of("Savanna"),
                report.rows(ItemSourceType.MOB_DROP).get(0).biomeLinks().stream().map(ItemSourceLink::label).toList()
        );
    }

    @Test
    void processingRecipesUseRecipeGroupWithMethodIconSource() {
        SearchNode leather = item("leather", "Leather");
        SearchNode rottenFlesh = item("rotten_flesh", "Rotten Flesh");
        SearchNode dryingRack = node(new ResourceLocation("hexerei:drying_rack"), NodeType.ITEM, "Drying Rack");
        SearchNode drying = recipe("drying_rotten_flesh", "hexerei:drying_rack");
        drying.addResolvedEdge(EdgeType.PRODUCES, leather);
        drying.addResolvedEdge(EdgeType.REQUIRES, rottenFlesh);
        leather.addResolvedEdge(EdgeType.OUTPUT_OF, drying);

        ItemSourceReport report = resolver(leather, rottenFlesh, dryingRack, drying).resolve(leather);

        assertEquals(
                List.of("Drying Rack -> Leather"),
                report.rows(ItemSourceType.RECIPE).stream().map(ItemSourceRow::text).toList()
        );
        ItemSourceRow row = report.rows(ItemSourceType.RECIPE).get(0);
        assertEquals("Drying Rack", row.primaryLink().label());
        assertEquals(dryingRack, row.primaryLink().node());
        assertTrue(report.rows(ItemSourceType.PROCESSING).isEmpty());
    }

    @Test
    void recipeMethodIconMetadataWinsForModdedCategoryIds() {
        SearchNode leather = item("leather", "Leather");
        SearchNode strangeInput = item("strange_input", "Strange Input");
        SearchNode tannery = node(new ResourceLocation("example:tannery"), NodeType.ITEM, "Tannery");
        SearchNode recipe = recipe(
                "modded_leather",
                Map.of(
                        SearchNodeKeys.RECIPE_TYPE_ID, "example:hide_work",
                        SearchNodeKeys.RECIPE_METHOD_ICON_ITEM_ID, "example:tannery",
                        SearchNodeKeys.RECIPE_METHOD_LABEL, "Tannery"
                )
        );
        recipe.addResolvedEdge(EdgeType.PRODUCES, leather);
        recipe.addResolvedEdge(EdgeType.REQUIRES, strangeInput);
        leather.addResolvedEdge(EdgeType.OUTPUT_OF, recipe);

        ItemSourceReport report = resolver(leather, strangeInput, tannery, recipe).resolve(leather);

        ItemSourceRow row = report.rows(ItemSourceType.RECIPE).get(0);
        assertEquals("Tannery -> Leather", row.text());
        assertEquals("Tannery", row.primaryLink().label());
        assertEquals(tannery, row.primaryLink().node());
    }

    @Test
    void cycleProtectionSkipsRecipesThatRequireTheTargetItem() {
        SearchNode leather = item("leather", "Leather");
        SearchNode recipe = recipe("leather_loop", "crafting");
        recipe.addResolvedEdge(EdgeType.PRODUCES, leather);
        recipe.addResolvedEdge(EdgeType.REQUIRES, leather);
        leather.addResolvedEdge(EdgeType.OUTPUT_OF, recipe);

        ItemSourceReport report = resolver(leather, recipe).resolve(leather);

        assertTrue(report.rows(ItemSourceType.RECIPE).isEmpty());
        assertTrue(report.groupOrder().isEmpty());
    }

    @Test
    void usefulRoutesAreRankedBeforeProcessingAndSalvage() {
        SearchNode leather = item("leather", "Leather");
        SearchNode cow = entity("cow", "Cow");
        SearchNode rabbitHide = item("rabbit_hide", "Rabbit Hide");
        SearchNode rabbit = entity("rabbit", "Rabbit");
        SearchNode rottenFlesh = item("rotten_flesh", "Rotten Flesh");
        SearchNode saddle = item("saddle", "Saddle");
        SearchNode craftingTable = item("crafting_table", "Crafting Table");
        SearchNode smoker = item("smoker", "Smoker");
        SearchNode crafting = recipe("rabbit_hide_to_leather", "crafting");
        SearchNode drying = recipe("smoke_rotten_flesh", "smoking");
        SearchNode salvage = recipe("salvage_saddle", "cutting");

        cow.addResolvedEdge(EdgeType.DROPS, leather);
        rabbit.addResolvedEdge(EdgeType.DROPS, rabbitHide);
        crafting.addResolvedEdge(EdgeType.PRODUCES, leather);
        crafting.addResolvedEdge(EdgeType.REQUIRES, rabbitHide);
        drying.addResolvedEdge(EdgeType.PRODUCES, leather);
        drying.addResolvedEdge(EdgeType.REQUIRES, rottenFlesh);
        salvage.addResolvedEdge(EdgeType.PRODUCES, leather);
        salvage.addResolvedEdge(EdgeType.REQUIRES, saddle);
        leather.addResolvedEdge(EdgeType.OUTPUT_OF, crafting);
        leather.addResolvedEdge(EdgeType.OUTPUT_OF, drying);
        leather.addResolvedEdge(EdgeType.OUTPUT_OF, salvage);

        ItemSourceReport report = resolver(leather, cow, rabbitHide, rabbit, rottenFlesh, saddle, craftingTable, smoker, crafting, drying, salvage)
                .resolve(leather);

        assertEquals(
                List.of(
                        ItemSourceType.MOB_DROP,
                        ItemSourceType.RECIPE,
                        ItemSourceType.INDIRECT_SOURCE
                ),
                report.groupOrder()
        );
        assertEquals(
                List.of("Crafting Table -> Leather", "Smoker -> Leather", "Cutting -> Leather"),
                report.rows(ItemSourceType.RECIPE).stream().map(ItemSourceRow::text).toList()
        );
    }

    private static ItemSourceResolver resolver(SearchNode... nodes) {
        return new ItemSourceResolver(List.of(nodes));
    }

    private static SearchNode item(String path, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.ITEM, name, 0, 0, Map.of());
    }

    private static SearchNode node(ResourceLocation id, NodeType type, String name) {
        return new SearchNode(id, type, name, 0, 0, Map.of());
    }

    private static SearchNode entity(String path, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.ENTITY, name, 0, 0, Map.of());
    }

    private static SearchNode biome(String path, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.BIOME, name, 0, 0, Map.of());
    }

    private static SearchNode recipe(String path, String recipeType) {
        return recipe(path, Map.of(SearchNodeKeys.RECIPE_TYPE_ID, recipeType));
    }

    private static SearchNode recipe(String path, Map<String, String> metadata) {
        return new SearchNode(
                new ResourceLocation("minecraft:" + path),
                NodeType.RECIPE,
                path,
                0,
                0,
                metadata
        );
    }
}
