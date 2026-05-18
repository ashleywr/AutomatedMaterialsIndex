package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuerySearchModeTest {

    @AfterEach
    public void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    public void plainTextSearchDoesNotMatchMetadataOnlyAliases() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode ingot = new SearchNode(
                ResourceLocation.parse("minecraft:iron_ingot"),
                NodeType.ITEM,
                "Iron Ingot",
                0,
                0,
                Map.of()
        );
        SearchNode copycat = new SearchNode(
                ResourceLocation.parse("copycats:copycat_light_weighted_pressure_plate"),
                NodeType.ITEM,
                "Copycat Light Weighted Pressure Plate",
                0,
                0,
                Map.of(SearchNodeKeys.MATERIAL_GROUP, "create:zinc_ingot")
        );
        SearchNode template = new SearchNode(
                ResourceLocation.parse("minecraft:netherite_upgrade_smithing_template"),
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
    public void broadMetadataSearchUsesTildePrefix() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode copycat = new SearchNode(
                ResourceLocation.parse("copycats:copycat_light_weighted_pressure_plate"),
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
                ResourceLocation.parse("minecraft:iron_ingot"),
                NodeType.ITEM,
                "Iron Ingot",
                0,
                0,
                Map.of(SearchNodeKeys.MOD_ID, "minecraft")
        );
        SearchNode zincIngot = new SearchNode(
                ResourceLocation.parse("create:zinc_ingot"),
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
}
