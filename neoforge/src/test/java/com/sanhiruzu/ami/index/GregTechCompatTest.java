package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.GregTechCompat;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GregTechCompatTest {
    @Test
    void gregTechMaterialsRouteToGregTechByDefault() {
        Map<String, String> meta = meta("gtceu");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.INGOT)));

        CompatFamilyDetector.detect(new Identifier("gtceu", "copper_ingot"), meta);
        CategoryAssignment assignment = resolve("gtceu:copper_ingot", meta, ItemFacet.INGOT);

        assertEquals("gregtech", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        assertEquals("gregtech", assignment.categoryId());
        assertEquals("materials", assignment.subcategoryId());
        assertEquals("focused", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
    }

    @Test
    void gregTechMachinesAndCircuitsStayInsideGregTech() {
        Map<String, String> machine = meta("gtceu");
        machine.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.MACHINE)));
        CompatFamilyDetector.detect(new Identifier("gtceu", "lv_macerator"), machine);
        GregTechCompat.enrichItem(new Identifier("gtceu", "lv_macerator"), machine);

        Map<String, String> circuit = meta("gtceu");
        CompatFamilyDetector.detect(new Identifier("gtceu", "good_electronic_circuit"), circuit);
        GregTechCompat.enrichItem(new Identifier("gtceu", "good_electronic_circuit"), circuit);

        assertEquals("machines", machine.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("lv", machine.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("circuits", circuit.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("machines", resolve("gtceu:lv_macerator", machine,
                ItemFacet.PLACEABLE, ItemFacet.MACHINE).subcategoryId());
        assertEquals("circuits", resolve("gtceu:good_electronic_circuit", circuit).subcategoryId());
    }

    @Test
    void gregTechCircuitTiersComeFromCircuitTagsAndGradesFromRegistryIds() {
        Map<String, String> basic = meta("gtceu");
        basic.put(SearchNodeKeys.TAGS, "gtceu:circuits,gtceu:circuits/lv");
        GregTechCompat.enrichItem(new Identifier("gtceu", "basic_electronic_circuit"), basic);

        Map<String, String> good = meta("gtceu");
        good.put(SearchNodeKeys.TAGS, "gtceu:circuits,gtceu:circuits/mv");
        GregTechCompat.enrichItem(new Identifier("gtceu", "good_electronic_circuit"), good);

        assertEquals("circuits", basic.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("lv", basic.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("basic", basic.get(SearchNodeKeys.GREGTECH_CIRCUIT_GRADE));
        assertEquals("circuits", good.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("mv", good.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("good", good.get(SearchNodeKeys.GREGTECH_CIRCUIT_GRADE));
        assertContains(basic.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, ""), "basic_circuit");
        assertContains(good.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, ""), "gregtech_circuit_good");
    }

    @Test
    void gregTechKnownCircuitIdsFillTierAndGradeWhenTagsAreMissing() {
        Map<String, String> good = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "good_electronic_circuit"), good);

        Map<String, String> wetware = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "wetware_processor_mainframe"), wetware);

        assertEquals("circuits", good.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("mv", good.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("good", good.get(SearchNodeKeys.GREGTECH_CIRCUIT_GRADE));
        assertContains(good.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, ""), "mv_tier");
        assertContains(good.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, ""), "good_circuit");

        assertEquals("circuits", wetware.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("uhv", wetware.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("wetware", wetware.get(SearchNodeKeys.GREGTECH_CIRCUIT_GRADE));
    }

    @Test
    void gregTechEnrichmentFindsVoltageTiersAndUsefulFacts() {
        Map<String, String> battery = meta("gtceu");
        battery.put(SearchNodeKeys.TAGS, "gtceu:batteries,gtceu:batteries/hv");
        battery.put(SearchNodeKeys.ENERGY_CAPACITY, "1600000");

        GregTechCompat.enrichItem(new Identifier("gtceu", "hv_lapotron_crystal"), battery);

        assertEquals("power", battery.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("hv", battery.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("power", battery.get(SearchNodeKeys.GREGTECH_FACTS));
        String tokens = battery.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        assertContains(tokens, "gregtech_hv");
        assertContains(tokens, "hv_tier");
        assertContains(tokens, "gregtech_power");
    }

    @Test
    void gregTechEnergyFactsUseEuVoltageTiersRolesAndAmperage() {
        Map<String, String> macerator = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "lv_macerator"), macerator);

        Map<String, String> combustion = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "hv_combustion"), combustion);

        Map<String, String> inputHatch = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "ev_energy_input_hatch_4a"), inputHatch);

        Map<String, String> outputHatch = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "ev_energy_output_hatch_16a"), outputHatch);

        assertEquals("consumes_eu", macerator.get(SearchNodeKeys.GREGTECH_ENERGY_ROLE));
        assertEquals("32", macerator.get(SearchNodeKeys.GREGTECH_EU_CONSUMPTION));
        assertContains(macerator.getOrDefault(SearchNodeKeys.GREGTECH_FACTS, ""), "consumes_eu");

        assertEquals("generates_eu", combustion.get(SearchNodeKeys.GREGTECH_ENERGY_ROLE));
        assertEquals("512", combustion.get(SearchNodeKeys.GREGTECH_EU_GENERATION));
        assertContains(combustion.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, ""), "eu_produced");

        assertEquals("inputs_eu", inputHatch.get(SearchNodeKeys.GREGTECH_ENERGY_ROLE));
        assertEquals("8192", inputHatch.get(SearchNodeKeys.GREGTECH_EU_INPUT));
        assertEquals("4", inputHatch.get(SearchNodeKeys.GREGTECH_AMPERAGE));

        assertEquals("outputs_eu", outputHatch.get(SearchNodeKeys.GREGTECH_ENERGY_ROLE));
        assertEquals("32768", outputHatch.get(SearchNodeKeys.GREGTECH_EU_OUTPUT));
        assertEquals("16", outputHatch.get(SearchNodeKeys.GREGTECH_AMPERAGE));
    }

    @Test
    void gregTechEnrichmentHandlesSteamAndHighTiers() {
        Map<String, String> bronze = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "bronze_steam_macerator"), bronze);

        Map<String, String> max = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "max_transformer"), max);

        assertEquals("steam", bronze.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("machines", bronze.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("max", max.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("power", max.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
    }

    @Test
    void gregTechRuntimeClassMetadataRoutesDumpedMachineFamilies() {
        Map<String, String> metaMachine = meta("gtceu");
        metaMachine.put(SearchNodeKeys.ITEM_CLASS, "com.gregtechceu.gtceu.api.item.MetaMachineItem");
        metaMachine.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY)));
        CompatFamilyDetector.detect(new Identifier("gtceu", "lv_alloy_smelter"), metaMachine);
        GregTechCompat.enrichItem(new Identifier("gtceu", "lv_alloy_smelter"), metaMachine);

        Map<String, String> metaMachineBlock = meta("gtceu");
        metaMachineBlock.put(SearchNodeKeys.BLOCK_CLASS, "com.gregtechceu.gtceu.api.block.MetaMachineBlock");
        metaMachineBlock.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY)));
        CompatFamilyDetector.detect(new Identifier("gtceu", "mv_chemical_reactor"), metaMachineBlock);
        GregTechCompat.enrichItem(new Identifier("gtceu", "mv_chemical_reactor"), metaMachineBlock);

        assertEquals("machines", metaMachine.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("lv", metaMachine.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("machines", metaMachineBlock.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("mv", metaMachineBlock.get(SearchNodeKeys.GREGTECH_TIER));
        assertEquals("machines", resolve("gtceu:lv_alloy_smelter", metaMachine,
                ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY).subcategoryId());
        assertEquals("machines", resolve("gtceu:mv_chemical_reactor", metaMachineBlock,
                ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY).subcategoryId());
    }

    @Test
    void gregTechRuntimeClassMetadataRoutesDumpedMaterialFamilies() {
        Map<String, String> bucket = meta("gtceu");
        bucket.put(SearchNodeKeys.ITEM_CLASS, "com.gregtechceu.gtceu.api.item.GTBucketItem");
        bucket.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.FLUID_CONTAINER, ItemFacet.UTILITY_MISC)));
        CompatFamilyDetector.detect(new Identifier("gtceu", "aluminium_bucket"), bucket);
        GregTechCompat.enrichItem(new Identifier("gtceu", "aluminium_bucket"), bucket);

        Map<String, String> surfaceRock = meta("gtceu");
        surfaceRock.put(SearchNodeKeys.ITEM_CLASS, "com.gregtechceu.gtceu.api.item.SurfaceRockBlockItem");
        surfaceRock.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE)));
        CompatFamilyDetector.detect(new Identifier("gtceu", "aluminium_indicator"), surfaceRock);
        GregTechCompat.enrichItem(new Identifier("gtceu", "aluminium_indicator"), surfaceRock);

        assertEquals("materials", bucket.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("materials", surfaceRock.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("materials", resolve("gtceu:aluminium_bucket", bucket,
                ItemFacet.FLUID_CONTAINER, ItemFacet.UTILITY_MISC).subcategoryId());
        assertEquals("materials", resolve("gtceu:aluminium_indicator", surfaceRock,
                ItemFacet.PLACEABLE).subcategoryId());
    }

    @Test
    void gregTechFoodArmorWeaponsAndHarvestToolsUseSemanticCategories() {
        Map<String, String> food = meta("gtceu");
        CompatFamilyDetector.detect(new Identifier("gtceu", "chocolate_bar"), food);

        Map<String, String> armor = meta("gtceu");
        CompatFamilyDetector.detect(new Identifier("gtceu", "nano_chestplate"), armor);

        Map<String, String> weapon = meta("gtceu");
        CompatFamilyDetector.detect(new Identifier("gtceu", "nano_saber"), weapon);
        GregTechCompat.enrichItem(new Identifier("gtceu", "nano_saber"), weapon);

        Map<String, String> tool = meta("gtceu");
        CompatFamilyDetector.detect(new Identifier("gtceu", "lv_drill"), tool);
        GregTechCompat.enrichItem(new Identifier("gtceu", "lv_drill"), tool);

        assertEquals("nature", resolve("gtceu:chocolate_bar", food, ItemFacet.EDIBLE).categoryId());
        assertEquals("armor", resolve("gtceu:nano_chestplate", armor, ItemFacet.ARMOR_CHEST).categoryId());
        assertEquals("tools", resolve("gtceu:nano_saber", weapon, ItemFacet.MELEE_WEAPON).categoryId());
        assertEquals("melee", resolve("gtceu:nano_saber", weapon, ItemFacet.MELEE_WEAPON).subcategoryId());
        assertEquals("tools", resolve("gtceu:lv_drill", tool, ItemFacet.HARVEST_TOOL).categoryId());
        assertEquals("harvest", resolve("gtceu:lv_drill", tool, ItemFacet.HARVEST_TOOL).subcategoryId());
        assertEquals("tools", tool.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("lv", tool.get(SearchNodeKeys.GREGTECH_TIER));
    }

    @Test
    void gregTechWorkUtilityToolsRouteToGregTechToolsFromConcreteFamilyFacts() {
        Map<String, String> screwdriver = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "screwdriver"), screwdriver);

        Map<String, String> wireCutters = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "wire_cutters"), wireCutters);

        Map<String, String> mortar = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "flint_mortar"), mortar);

        Map<String, String> saw = meta("gtceu");
        GregTechCompat.enrichItem(new Identifier("gtceu", "steel_saw"), saw);

        assertEquals("tools", screwdriver.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("tools", wireCutters.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("tools", mortar.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("tools", saw.get(SearchNodeKeys.GREGTECH_ITEM_KIND));
        assertEquals("gregtech", resolve("gtceu:screwdriver", screwdriver, ItemFacet.UTILITY_TOOL).categoryId());
        assertEquals("tools", resolve("gtceu:screwdriver", screwdriver, ItemFacet.UTILITY_TOOL).subcategoryId());
        assertEquals("gregtech", resolve("gtceu:wire_cutters", wireCutters, ItemFacet.UTILITY_TOOL).categoryId());
        assertEquals("tools", resolve("gtceu:wire_cutters", wireCutters, ItemFacet.UTILITY_TOOL).subcategoryId());
        assertEquals("gregtech", resolve("gtceu:flint_mortar", mortar).categoryId());
        assertEquals("tools", resolve("gtceu:flint_mortar", mortar).subcategoryId());
        assertEquals("tools", resolve("gtceu:steel_saw", saw, ItemFacet.HARVEST_TOOL).categoryId());
        assertEquals("harvest", resolve("gtceu:steel_saw", saw, ItemFacet.HARVEST_TOOL).subcategoryId());
    }

    @Test
    void semanticGregTechPolicyCanStillOptOut() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.gregtechCategoryPolicy;
        try {
            AmiConfig.gregtechCategoryPolicy = AmiConfig.CompatCategoryPolicy.SEMANTIC;
            Map<String, String> meta = meta("gtceu");
            meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.INGOT)));
            CompatFamilyDetector.detect(new Identifier("gtceu", "copper_ingot"), meta);

            CategoryAssignment assignment = resolve("gtceu:copper_ingot", meta, ItemFacet.INGOT);

            assertNotEquals("gregtech", assignment.categoryId());
            assertEquals("semantic", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        } finally {
            AmiConfig.gregtechCategoryPolicy = oldPolicy;
        }
    }

    private static Map<String, String> meta(String modId) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "GregTech");
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new Identifier(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }

    private static void assertContains(String haystack, String needle) {
        org.junit.jupiter.api.Assertions.assertTrue(haystack.contains(needle), haystack);
    }
}
