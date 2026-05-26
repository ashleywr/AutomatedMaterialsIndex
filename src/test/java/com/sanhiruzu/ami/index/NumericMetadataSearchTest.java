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
                new ResourceLocation("minecraft:diamond_sword"),
                NodeType.ITEM,
                "Diamond Sword",
                0,
                0,
                Map.of(SearchNodeKeys.DPS, "11.2")
        );
        SearchNode chest = new SearchNode(
                new ResourceLocation("minecraft:chest"),
                NodeType.ITEM,
                "Chest",
                0,
                0,
                Map.of(SearchNodeKeys.ESM_CAPACITY, "1728")
        );
        SearchNode barrel = new SearchNode(
                new ResourceLocation("minecraft:barrel"),
                NodeType.ITEM,
                "Barrel",
                0,
                0,
                Map.of(SearchNodeKeys.ESM_CAPACITY, "1728")
        );
        SearchNode energyCell = new SearchNode(
                new ResourceLocation("example:energy_cell"),
                NodeType.ITEM,
                "Energy Cell",
                0,
                0,
                Map.of(SearchNodeKeys.ENERGY_CAPACITY, "100000")
        );

        index.addNode(sword);
        index.addNode(chest);
        index.addNode(barrel);
        index.addNode(energyCell);
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

        List<SearchNode> highEnergy = service.query(">energy:50000").get(NodeType.ITEM);
        assertTrue(highEnergy.contains(energyCell));
        assertFalse(highEnergy.contains(chest));
    }
}
