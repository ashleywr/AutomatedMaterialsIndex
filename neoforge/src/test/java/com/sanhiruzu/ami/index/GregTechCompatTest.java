package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;
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

        CompatFamilyDetector.detect(new ResourceLocation("gtceu", "copper_ingot"), meta);
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
        CompatFamilyDetector.detect(new ResourceLocation("gtceu", "lv_macerator"), machine);

        Map<String, String> circuit = meta("gtceu");
        CompatFamilyDetector.detect(new ResourceLocation("gtceu", "good_electronic_circuit"), circuit);

        assertEquals("machines", resolve("gtceu:lv_macerator", machine,
                ItemFacet.PLACEABLE, ItemFacet.MACHINE).subcategoryId());
        assertEquals("circuits", resolve("gtceu:good_electronic_circuit", circuit).subcategoryId());
    }

    @Test
    void gregTechRuntimeClassMetadataRoutesDumpedMachineFamilies() {
        Map<String, String> metaMachine = meta("gtceu");
        metaMachine.put(SearchNodeKeys.ITEM_CLASS, "com.gregtechceu.gtceu.api.item.MetaMachineItem");
        metaMachine.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY)));
        CompatFamilyDetector.detect(new ResourceLocation("gtceu", "lv_alloy_smelter"), metaMachine);

        Map<String, String> metaMachineBlock = meta("gtceu");
        metaMachineBlock.put(SearchNodeKeys.BLOCK_CLASS, "com.gregtechceu.gtceu.api.block.MetaMachineBlock");
        metaMachineBlock.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY)));
        CompatFamilyDetector.detect(new ResourceLocation("gtceu", "mv_chemical_reactor"), metaMachineBlock);

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
        CompatFamilyDetector.detect(new ResourceLocation("gtceu", "aluminium_bucket"), bucket);

        Map<String, String> surfaceRock = meta("gtceu");
        surfaceRock.put(SearchNodeKeys.ITEM_CLASS, "com.gregtechceu.gtceu.api.item.SurfaceRockBlockItem");
        surfaceRock.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE)));
        CompatFamilyDetector.detect(new ResourceLocation("gtceu", "aluminium_indicator"), surfaceRock);

        assertEquals("materials", resolve("gtceu:aluminium_bucket", bucket,
                ItemFacet.FLUID_CONTAINER, ItemFacet.UTILITY_MISC).subcategoryId());
        assertEquals("materials", resolve("gtceu:aluminium_indicator", surfaceRock,
                ItemFacet.PLACEABLE).subcategoryId());
    }

    @Test
    void semanticGregTechPolicyCanStillOptOut() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.gregtechCategoryPolicy;
        try {
            AmiConfig.gregtechCategoryPolicy = AmiConfig.CompatCategoryPolicy.SEMANTIC;
            Map<String, String> meta = meta("gtceu");
            meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.INGOT)));
            CompatFamilyDetector.detect(new ResourceLocation("gtceu", "copper_ingot"), meta);

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
                new ResourceLocation(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
