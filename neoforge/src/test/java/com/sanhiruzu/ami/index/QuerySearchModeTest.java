package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.index.query.QueryParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class QuerySearchModeTest {

    @AfterEach
    public void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    public void plainTextSearchDoesNotMatchMetadataOnlyAliases() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode ingot = new SearchNode(
                new ResourceLocation("minecraft:iron_ingot"),
                NodeType.ITEM,
                "Iron Ingot",
                0,
                0,
                Map.of()
        );
        SearchNode copycat = new SearchNode(
                new ResourceLocation("copycats:copycat_light_weighted_pressure_plate"),
                NodeType.ITEM,
                "Copycat Light Weighted Pressure Plate",
                0,
                0,
                Map.of(SearchNodeKeys.MATERIAL_GROUP, "create:zinc_ingot")
        );
        SearchNode template = new SearchNode(
                new ResourceLocation("minecraft:netherite_upgrade_smithing_template"),
                NodeType.ITEM,
                "Netherite Upgrade Smithing Template",
                0,
                0,
                Map.of(SearchNodeKeys.SEARCH_TOKENS, "ingot upgrade")
        );

        index.addNode(ingot);
        index.addNode(copycat);
        index.addNode(template);

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> hits = service.query("ingot").get(NodeType.ITEM);

        assertTrue(hits.contains(ingot));
        assertFalse(hits.contains(copycat));
        assertFalse(hits.contains(template));
    }

    @Test
    public void plainTextSearchMatchesCuratedPlainSearchTokens() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode medicine = new SearchNode(
                new ResourceLocation("cobblemon:super_potion"),
                NodeType.ITEM,
                "Super Potion",
                0,
                0,
                Map.of(SearchNodeKeys.PLAIN_SEARCH_TOKENS, "pokemon cobblemon")
        );
        SearchNode metadataOnly = new SearchNode(
                new ResourceLocation("example:metadata_capsule"),
                NodeType.ITEM,
                "Metadata Capsule",
                0,
                0,
                Map.of(SearchNodeKeys.SEARCH_TOKENS, "pokemon")
        );

        index.addNode(medicine);
        index.addNode(metadataOnly);

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> hits = service.query("poke").get(NodeType.ITEM);

        assertTrue(hits.contains(medicine));
        assertFalse(hits.contains(metadataOnly));
    }

    @Test
    public void plainTextSearchMatchesTooltipTokensButNotOtherMetadata() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode tooltipMatch = new SearchNode(
                new ResourceLocation("example:medicine_capsule"),
                NodeType.ITEM,
                "Medicine Capsule",
                0,
                0,
                Map.of(SearchNodeKeys.TOOLTIP_SEARCH_TOKENS, "pokemon battle")
        );
        SearchNode metadataOnly = new SearchNode(
                new ResourceLocation("example:metadata_capsule"),
                NodeType.ITEM,
                "Metadata Capsule",
                0,
                0,
                Map.of(SearchNodeKeys.MATERIAL_GROUP, "example:pokemon")
        );

        index.addNode(tooltipMatch);
        index.addNode(metadataOnly);

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> hits = service.query("poke").get(NodeType.ITEM);

        assertTrue(hits.contains(tooltipMatch));
        assertFalse(hits.contains(metadataOnly));
    }

    @Test
    public void plainTextSearchDoesNotCapLongTooltipTokenMatches() {
        GlobalIndex index = GlobalIndex.getInstance();

        for (int i = 0; i < 225; i++) {
            index.addNode(new SearchNode(
                    new ResourceLocation("example", "pokemon_tooltip_" + i),
                    NodeType.ITEM,
                    "Tooltip Match " + i,
                    0,
                    0,
                    Map.of(SearchNodeKeys.TOOLTIP_SEARCH_TOKENS, "pokemon")
            ));
        }
        SearchNode lateMatch = new SearchNode(
                new ResourceLocation("mega_showdown:rotom_mow"),
                NodeType.ITEM,
                "Mow Unit",
                0,
                0,
                Map.of(SearchNodeKeys.TOOLTIP_SEARCH_TOKENS, "special mower specific pokemon enter")
        );
        index.addNode(lateMatch);

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> hits = service.query("poke").get(NodeType.ITEM);

        assertTrue(hits.size() > 200);
        assertTrue(hits.contains(lateMatch));
    }

    @Test
    public void broadMetadataSearchUsesTildePrefix() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode copycat = new SearchNode(
                new ResourceLocation("copycats:copycat_light_weighted_pressure_plate"),
                NodeType.ITEM,
                "Copycat Light Weighted Pressure Plate",
                0,
                0,
                Map.of(SearchNodeKeys.MATERIAL_GROUP, "create:zinc_ingot")
        );

        index.addNode(copycat);

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> hits = service.query("~ingot").get(NodeType.ITEM);

        assertTrue(hits.contains(copycat));
    }

    @Test
    public void broadMetadataSearchCanRefinePlainTextResults() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode ironIngot = new SearchNode(
                new ResourceLocation("minecraft:iron_ingot"),
                NodeType.ITEM,
                "Iron Ingot",
                0,
                0,
                Map.of(SearchNodeKeys.MOD_ID, "minecraft")
        );
        SearchNode zincIngot = new SearchNode(
                new ResourceLocation("create:zinc_ingot"),
                NodeType.ITEM,
                "Zinc Ingot",
                0,
                0,
                Map.of(SearchNodeKeys.MOD_ID, "create")
        );

        index.addNode(ironIngot);
        index.addNode(zincIngot);

        SearchService service = SearchService.buildFrom(index, false);
        List<SearchNode> hits = service.query("ingot ~create").get(NodeType.ITEM);

        assertFalse(hits.contains(ironIngot));
        assertTrue(hits.contains(zincIngot));
    }

    @Test
    public void queryExplanationReportsResolverAndFilterCounts() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode ironIngot = new SearchNode(
                new ResourceLocation("minecraft:iron_ingot"),
                NodeType.ITEM,
                "Iron Ingot",
                0,
                0,
                Map.of(SearchNodeKeys.MOD_ID, "minecraft")
        );
        SearchNode zincIngot = new SearchNode(
                new ResourceLocation("create:zinc_ingot"),
                NodeType.ITEM,
                "Zinc Ingot",
                0,
                0,
                Map.of(SearchNodeKeys.MOD_ID, "create")
        );

        index.addNode(ironIngot);
        index.addNode(zincIngot);

        SearchService service = SearchService.buildFrom(index, false);
        SearchService.QueryExplanation explanation = service.explain("ingot @create");

        assertEquals(List.of("INCLUDE:ingot", "MOD:create"), explanation.tokens());
        assertEquals(1, explanation.finalCounts().get(NodeType.ITEM));
        assertTrue(explanation.steps().stream().anyMatch(step -> step.operation().equals("include:LiteralResolver")));
        assertTrue(explanation.steps().stream().anyMatch(step -> step.operation().equals("after-mod")));
    }

    @Test
    public void parserUsesSharedSearchSyntaxForPrefixesAndShortcuts() {
        assertTrue(QueryParser.parse("?").tokens().isEmpty());
        assertTrue(QueryParser.parse("%egg:").tokens().isEmpty());

        assertEquals(List.of(new QueryParser.QueryToken(QueryParser.TokenType.PROP, "pokemonEggGroup:field")),
                QueryParser.parse("%egg:field").tokens());
        assertEquals(List.of(new QueryParser.QueryToken(QueryParser.TokenType.EXCLUDE, "@type:grass")),
                QueryParser.parse("-@type:grass").tokens());
    }
}
