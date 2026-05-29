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
        SearchNode generator = new SearchNode(
                new ResourceLocation("example:generator"),
                NodeType.ITEM,
                "Generator",
                0,
                0,
                Map.of(SearchNodeKeys.ENERGY_GENERATION, "80")
        );
        SearchNode fluidTank = new SearchNode(
                new ResourceLocation("example:fluid_tank"),
                NodeType.ITEM,
                "Fluid Tank",
                0,
                0,
                Map.of(SearchNodeKeys.FLUID_CAPACITY, "16")
        );
        SearchNode pickaxe = new SearchNode(
                new ResourceLocation("example:pickaxe"),
                NodeType.ITEM,
                "Pickaxe",
                0,
                0,
                Map.of(SearchNodeKeys.TOOL_SPEED, "8")
        );
        SearchNode cardboardSword = new SearchNode(
                new ResourceLocation("create:cardboard_sword"),
                NodeType.ITEM,
                "Cardboard Sword",
                0,
                0,
                Map.of(SearchNodeKeys.MAX_DURABILITY, "13")
        );

        index.addNode(sword);
        index.addNode(chest);
        index.addNode(barrel);
        index.addNode(energyCell);
        index.addNode(generator);
        index.addNode(fluidTank);
        index.addNode(pickaxe);
        index.addNode(cardboardSword);
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

        List<SearchNode> highGeneration = service.query(">gen:40").get(NodeType.ITEM);
        assertTrue(highGeneration.contains(generator));
        assertFalse(highGeneration.contains(energyCell));

        List<SearchNode> largeTank = service.query(">fluid:8").get(NodeType.ITEM);
        assertTrue(largeTank.contains(fluidTank));
        assertFalse(largeTank.contains(chest));

        List<SearchNode> fastTools = service.query(">toolspeed:6").get(NodeType.ITEM);
        assertTrue(fastTools.contains(pickaxe));
        assertFalse(fastTools.contains(chest));

        List<SearchNode> lowDurability = service.query("<durability:20").get(NodeType.ITEM);
        assertTrue(lowDurability.contains(cardboardSword));
        assertFalse(lowDurability.contains(sword));
    }
}
