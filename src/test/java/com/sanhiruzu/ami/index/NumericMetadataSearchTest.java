package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumericMetadataSearchTest {

    @AfterEach
    public void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    public void numericFiltersCanSeedOrRefineSearchResults() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode sword = new SearchNode(
                ResourceLocation.parse("minecraft:diamond_sword"),
                NodeType.ITEM,
                "Diamond Sword",
                0,
                0,
                Map.of(SearchNodeKeys.DPS, "11.2")
        );
        SearchNode chest = new SearchNode(
                ResourceLocation.parse("minecraft:chest"),
                NodeType.ITEM,
                "Chest",
                0,
                0,
                Map.of(SearchNodeKeys.ESM_CAPACITY, "1728")
        );
        SearchNode barrel = new SearchNode(
                ResourceLocation.parse("minecraft:barrel"),
                NodeType.ITEM,
                "Barrel",
                0,
                0,
                Map.of(SearchNodeKeys.ESM_CAPACITY, "1728")
        );

        index.addNode(sword);
        index.addNode(chest);
        index.addNode(barrel);
        SearchService service = SearchService.buildFrom(index, false);

        List<SearchNode> highDps = service.query(">dps:10").get(NodeType.ITEM);
        assertTrue(highDps.contains(sword));
        assertFalse(highDps.contains(chest));

        List<SearchNode> refined = service.query("chest >storage:1000").get(NodeType.ITEM);
        assertTrue(refined.contains(chest));
        assertFalse(refined.contains(barrel));

        List<SearchNode> shorthandStorage = service.query(">1000").get(NodeType.ITEM);
        assertTrue(shorthandStorage.contains(chest));
        assertTrue(shorthandStorage.contains(barrel));
        assertFalse(shorthandStorage.contains(sword));
    }
}
