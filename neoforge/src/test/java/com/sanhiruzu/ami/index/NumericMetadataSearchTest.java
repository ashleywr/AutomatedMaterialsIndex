package com.sanhiruzu.ami.index;

import net.minecraft.resources.Identifier;
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
                new Identifier("minecraft:diamond_sword"),
                NodeType.ITEM,
                "Diamond Sword",
                0,
                0,
                Map.of(SearchNodeKeys.DPS, "11.2")
        );
        SearchNode chest = new SearchNode(
                new Identifier("minecraft:chest"),
                NodeType.ITEM,
                "Chest",
                0,
                0,
                Map.of(SearchNodeKeys.ESM_CAPACITY, "1728")
        );
        SearchNode barrel = new SearchNode(
                new Identifier("minecraft:barrel"),
                NodeType.ITEM,
                "Barrel",
                0,
                0,
                Map.of(SearchNodeKeys.ESM_CAPACITY, "1728")
        );
        SearchNode energyCell = new SearchNode(
                new Identifier("example:energy_cell"),
                NodeType.ITEM,
                "Energy Cell",
                0,
                0,
                Map.of(SearchNodeKeys.ENERGY_CAPACITY, "100000")
        );
        SearchNode generator = new SearchNode(
                new Identifier("example:generator"),
                NodeType.ITEM,
                "Generator",
                0,
                0,
                Map.of(SearchNodeKeys.ENERGY_GENERATION, "80")
        );
        SearchNode fluidTank = new SearchNode(
                new Identifier("example:fluid_tank"),
                NodeType.ITEM,
                "Fluid Tank",
                0,
                0,
                Map.of(SearchNodeKeys.FLUID_CAPACITY, "16")
        );
        SearchNode pickaxe = new SearchNode(
                new Identifier("example:pickaxe"),
                NodeType.ITEM,
                "Pickaxe",
                0,
                0,
                Map.of(SearchNodeKeys.TOOL_SPEED, "8")
        );
        SearchNode cardboardSword = new SearchNode(
                new Identifier("create:cardboard_sword"),
                NodeType.ITEM,
                "Cardboard Sword",
                0,
                0,
                Map.of(SearchNodeKeys.MAX_DURABILITY, "13")
        );
        SearchNode gtGenerator = new SearchNode(
                new Identifier("gtceu:hv_combustion"),
                NodeType.ITEM,
                "HV Combustion Generator",
                0,
                0,
                Map.of(SearchNodeKeys.GREGTECH_EU_GENERATION, "512")
        );
        SearchNode gtMachine = new SearchNode(
                new Identifier("gtceu:lv_macerator"),
                NodeType.ITEM,
                "LV Macerator",
                0,
                0,
                Map.of(SearchNodeKeys.GREGTECH_EU_CONSUMPTION, "32")
        );
        SearchNode gtInputHatch = new SearchNode(
                new Identifier("gtceu:ev_energy_input_hatch_4a"),
                NodeType.ITEM,
                "EV Energy Input Hatch (4A)",
                0,
                0,
                Map.of(
                        SearchNodeKeys.GREGTECH_EU_INPUT, "8192",
                        SearchNodeKeys.GREGTECH_AMPERAGE, "4"
                )
        );
        SearchNode gtOutputHatch = new SearchNode(
                new Identifier("gtceu:ev_energy_output_hatch_16a"),
                NodeType.ITEM,
                "EV Energy Output Hatch (16A)",
                0,
                0,
                Map.of(
                        SearchNodeKeys.GREGTECH_EU_OUTPUT, "32768",
                        SearchNodeKeys.GREGTECH_AMPERAGE, "16"
                )
        );

        index.addNode(sword);
        index.addNode(chest);
        index.addNode(barrel);
        index.addNode(energyCell);
        index.addNode(generator);
        index.addNode(fluidTank);
        index.addNode(pickaxe);
        index.addNode(cardboardSword);
        index.addNode(gtGenerator);
        index.addNode(gtMachine);
        index.addNode(gtInputHatch);
        index.addNode(gtOutputHatch);
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

        List<SearchNode> gtHighEuGeneration = service.query(">eugen:500").get(NodeType.ITEM);
        assertTrue(gtHighEuGeneration.contains(gtGenerator));
        assertFalse(gtHighEuGeneration.contains(gtMachine));

        List<SearchNode> gtEuConsumption = service.query("=euconsume:32").get(NodeType.ITEM);
        assertTrue(gtEuConsumption.contains(gtMachine));
        assertFalse(gtEuConsumption.contains(gtGenerator));

        List<SearchNode> gtEuInput = service.query(">euinput:8000").get(NodeType.ITEM);
        assertTrue(gtEuInput.contains(gtInputHatch));
        assertFalse(gtEuInput.contains(gtOutputHatch));

        List<SearchNode> gtEuOutput = service.query(">euoutput:30000").get(NodeType.ITEM);
        assertTrue(gtEuOutput.contains(gtOutputHatch));
        assertFalse(gtEuOutput.contains(gtInputHatch));

        List<SearchNode> gtFourAmp = service.query("=amps:4").get(NodeType.ITEM);
        assertTrue(gtFourAmp.contains(gtInputHatch));
        assertFalse(gtFourAmp.contains(gtOutputHatch));
    }
}
