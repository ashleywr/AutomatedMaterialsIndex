package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.MekanismCompat;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MekanismCompatTest {
    @Test
    void mekanismNamespaceGetsFamilyPolicy() {
        Map<String, String> meta = meta("mekanism", "Mekanism", "net.minecraft.world.item.Item", "");

        CompatFamilyDetector.detect(new ResourceLocation("mekanism", "energy_tablet"), meta);

        assertEquals("mekanism", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void energyTabletRoutesToMekanismEnergy() {
        Map<String, String> meta = meta("mekanism", "Mekanism",
                "mekanism.common.item.ItemEnergized", "");
        meta.put(SearchNodeKeys.ENERGY_CAPACITY, "400000");

        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "energy_tablet"), meta);
        CategoryAssignment assignment = resolve("mekanism:energy_tablet", meta, ItemFacet.HAS_ENERGY);

        assertEquals("energy", meta.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.MEKANISM_FACTS, "").contains("energy"));
        assertEquals("mekanism", assignment.categoryId());
        assertEquals("energy", assignment.subcategoryId());
    }

    @Test
    void mekanismMachinesRouteToMekanismMachines() {
        Map<String, String> meta = meta("mekanism", "Mekanism",
                "mekanism.common.item.block.ItemBlockMachine",
                "mekanism.common.block.prefab.BlockTile");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(
                ItemFacet.PLACEABLE,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.MACHINE
        )));

        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "metallurgic_infuser"), meta);
        CategoryAssignment assignment = resolve("mekanism:metallurgic_infuser", meta,
                ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.MACHINE);

        assertEquals("machines", meta.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertEquals("mekanism", assignment.categoryId());
        assertEquals("machines", assignment.subcategoryId());
    }

    @Test
    void upgradesRouteToMekanismUpgradesAndExposeTier() {
        Map<String, String> meta = meta("mekanism", "Mekanism",
                "mekanism.common.item.ItemTierInstaller", "");

        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "elite_tier_installer"), meta);
        CategoryAssignment assignment = resolve("mekanism:elite_tier_installer", meta);

        assertEquals("upgrades", meta.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertEquals("elite", meta.get(SearchNodeKeys.MEKANISM_TIER));
        assertEquals("mekanism", assignment.categoryId());
        assertEquals("upgrades", assignment.subcategoryId());
    }

    @Test
    void qioDriveRoutesToMekanismLogistics() {
        Map<String, String> meta = meta("mekanism", "Mekanism",
                "mekanism.common.item.ItemQIODrive", "");

        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "qio_drive_hyper_dense"), meta);
        CategoryAssignment assignment = resolve("mekanism:qio_drive_hyper_dense", meta);

        assertEquals("logistics", meta.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertEquals("mekanism", assignment.categoryId());
        assertEquals("logistics", assignment.subcategoryId());
    }

    @Test
    void energyToolsStayToolsUnderHybridPolicy() {
        Map<String, String> meta = meta("mekanism", "Mekanism",
                "mekanism.common.item.gear.ItemMekaTool", "");
        meta.put(SearchNodeKeys.ENERGY_CAPACITY, "6400000");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.HAS_ENERGY, ItemFacet.UTILITY_TOOL)));

        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "meka_tool"), meta);
        CategoryAssignment assignment = resolve("mekanism:meka_tool", meta, ItemFacet.HAS_ENERGY, ItemFacet.UTILITY_TOOL);

        assertEquals("tools", meta.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertNotEquals("mekanism", assignment.categoryId());
        assertEquals("tools", assignment.categoryId());
    }

    @Test
    void facetlessWorkflowToolsRouteToMekanismToolsUnderHybridPolicy() {
        Map<String, String> dictionary = meta("mekanism", "Mekanism",
                "mekanism.common.item.ItemDictionary", "");
        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "dictionary"), dictionary);
        CategoryAssignment dictionaryAssignment = resolve("mekanism:dictionary", dictionary);

        assertEquals("tools", dictionary.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertEquals("mekanism", dictionaryAssignment.categoryId());
        assertEquals("tools", dictionaryAssignment.subcategoryId());

        Map<String, String> configurationCard = meta("mekanism", "Mekanism",
                "mekanism.common.item.ItemConfigurationCard", "");
        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "configuration_card"), configurationCard);
        CategoryAssignment configurationCardAssignment = resolve("mekanism:configuration_card", configurationCard);

        assertEquals("tools", configurationCard.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertEquals("mekanism", configurationCardAssignment.categoryId());
        assertEquals("tools", configurationCardAssignment.subcategoryId());
    }

    @Test
    void materialsStaySemanticUnderHybridPolicy() {
        Map<String, String> meta = meta("mekanism", "Mekanism",
                "net.minecraft.world.item.Item", "");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.DUST)));
        meta.put(SearchNodeKeys.TAGS, "c:dusts,c:dusts/bronze");

        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "dust_bronze"), meta);
        CategoryAssignment assignment = resolve("mekanism:dust_bronze", meta, ItemFacet.DUST);

        assertEquals("materials", meta.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertNotEquals("mekanism", assignment.categoryId());
    }

    @Test
    void facetlessMekanismMaterialsFallBackToMekanismMaterials() {
        Map<String, String> meta = meta("mekanism", "Mekanism",
                "net.minecraft.world.item.Item", "");

        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "alloy_atomic"), meta);
        CategoryAssignment assignment = resolve("mekanism:alloy_atomic", meta);

        assertEquals("materials", meta.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertEquals("mekanism", assignment.categoryId());
        assertEquals("materials", assignment.subcategoryId());
        assertEquals("compat_fallback", assignment.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void facetlessMekanismWeaponToolsFallBackToMekanismTools() {
        Map<String, String> meta = meta("mekanism", "Mekanism",
                "mekanism.common.item.gear.ItemFlamethrower", "");

        MekanismCompat.enrichItem(new ResourceLocation("mekanism", "flamethrower"), meta);
        CategoryAssignment assignment = resolve("mekanism:flamethrower", meta);

        assertEquals("tools", meta.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
        assertEquals("mekanism", assignment.categoryId());
        assertEquals("tools", assignment.subcategoryId());
    }

    @Test
    void semanticMekanismPolicyKeepsMachineInNormalOntology() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.mekanismCategoryPolicy;
        try {
            AmiConfig.mekanismCategoryPolicy = AmiConfig.CompatCategoryPolicy.SEMANTIC;
            Map<String, String> meta = meta("mekanism", "Mekanism",
                    "mekanism.common.item.block.ItemBlockMachine",
                    "mekanism.common.block.prefab.BlockTile");

            MekanismCompat.enrichItem(new ResourceLocation("mekanism", "crusher"), meta);
            CategoryAssignment assignment = resolve("mekanism:crusher", meta,
                    ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.MACHINE);

            assertEquals("machines", meta.get(SearchNodeKeys.MEKANISM_ITEM_KIND));
            assertNotEquals("mekanism", assignment.categoryId());
            assertEquals("semantic", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        } finally {
            AmiConfig.mekanismCategoryPolicy = oldPolicy;
        }
    }

    private static Map<String, String> meta(String modId, String creativeTab, String itemClass, String blockClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, creativeTab);
        if (!itemClass.isBlank()) {
            meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        }
        if (!blockClass.isBlank()) {
            meta.put(SearchNodeKeys.BLOCK_CLASS, blockClass);
        }
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
