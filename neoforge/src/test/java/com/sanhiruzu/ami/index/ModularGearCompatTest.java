package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.ModularGearCompat;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularGearCompatTest {
    @Test
    void knownNamespacesGetSpecificCompatFamilies() {
        Map<String, String> tinkers = meta("tconstruct", "Tinkers' Construct", "", "");
        Map<String, String> silentGear = meta("silentgear", "Silent Gear", "", "");

        CompatFamilyDetector.detect(new ResourceLocation("tconstruct", "part_builder"), tinkers);
        CompatFamilyDetector.detect(new ResourceLocation("silentgear", "pickaxe_blueprint"), silentGear);

        assertEquals("tinkers", tinkers.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        assertEquals("silent_gear", silentGear.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void silentGearBlueprintsRouteToModularGear() {
        Map<String, String> meta = meta("silentgear", "Silent Gear", "net.silentchaos512.gear.item.BlueprintItem", "");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.TEMPLATE, ItemFacet.INGREDIENT_ORGANIC)));

        ModularGearCompat.enrichItem(new ResourceLocation("silentgear", "pickaxe_blueprint"), meta);
        CategoryAssignment assignment = resolve("silentgear:pickaxe_blueprint", meta,
                ItemFacet.TEMPLATE, ItemFacet.INGREDIENT_ORGANIC);

        assertEquals("silent_gear", meta.get(SearchNodeKeys.MODULAR_GEAR_FAMILY));
        assertEquals("blueprints", meta.get(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND));
        assertEquals("pickaxe", meta.get(SearchNodeKeys.MODULAR_GEAR_PART));
        assertTrue(meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").contains("modular_gear"));
        assertEquals("modular_gear", assignment.categoryId());
        assertEquals("blueprints", assignment.subcategoryId());
    }

    @Test
    void tinkersStationsRouteToModularGear() {
        Map<String, String> meta = meta("tconstruct", "Tinkers' Construct", "",
                "slimeknights.tconstruct.tables.block.table.PartBuilderBlock");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.WORKSTATION)));

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "part_builder"), meta);
        CategoryAssignment assignment = resolve("tconstruct:part_builder", meta,
                ItemFacet.PLACEABLE, ItemFacet.WORKSTATION);

        assertEquals("tinkers", meta.get(SearchNodeKeys.MODULAR_GEAR_FAMILY));
        assertEquals("stations", meta.get(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_FACTS, "").contains("station"));
        assertEquals("modular_gear", assignment.categoryId());
        assertEquals("stations", assignment.subcategoryId());
    }

    @Test
    void tinkersCastsArePartsNotStations() {
        Map<String, String> meta = meta("tconstruct", "Tinkers' Construct", "", "");

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "pickaxe_head_cast"), meta);

        assertEquals("parts", meta.get(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND));
        assertEquals("head", meta.get(SearchNodeKeys.MODULAR_GEAR_PART));
    }

    @Test
    void tinkersSmelteryBuildingBlocksDoNotRouteToModularGearStations() {
        Map<String, String> meta = meta("tconstruct", "Tinkers' Smeltery",
                "slimeknights.mantle.item.BlockTooltipItem",
                "slimeknights.tconstruct.smeltery.block.SearedBlock");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE)));

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "seared_stone"), meta);
        CategoryAssignment assignment = resolve("tconstruct:seared_stone", meta, ItemFacet.PLACEABLE);

        assertEquals("", meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
        assertNotEquals("modular_gear", assignment.categoryId());
    }

    @Test
    void tinkersFunctionalSmelteryBlocksStillRouteToStations() {
        Map<String, String> meta = meta("tconstruct", "Tinkers' Smeltery",
                "slimeknights.mantle.item.BlockTooltipItem",
                "slimeknights.tconstruct.smeltery.block.SearedBlock");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE)));

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "seared_table"), meta);
        CategoryAssignment assignment = resolve("tconstruct:seared_table", meta, ItemFacet.PLACEABLE);

        assertEquals("stations", meta.get(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND));
        assertEquals("modular_gear", assignment.categoryId());
        assertEquals("stations", assignment.subcategoryId());
    }

    @Test
    void broadPlateAndHeadWordsDoNotBecomeTinkersParts() {
        Map<String, String> pressurePlate = meta("tconstruct", "Tinkers' World", "", "");
        Map<String, String> mobHead = meta("tconstruct", "Tinkers' World",
                "net.minecraft.world.item.StandingAndWallBlockItem", "");

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "greenheart_pressure_plate"), pressurePlate);
        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "blaze_head"), mobHead);

        assertEquals("", pressurePlate.getOrDefault(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
        assertEquals("", pressurePlate.getOrDefault(SearchNodeKeys.MODULAR_GEAR_PART, ""));
        assertEquals("", mobHead.getOrDefault(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
        assertEquals("", mobHead.getOrDefault(SearchNodeKeys.MODULAR_GEAR_PART, ""));
    }

    @Test
    void tinkersReinforcementsRouteToModifiers() {
        Map<String, String> meta = meta("tconstruct", "Tinkers' General Items", "net.minecraft.world.item.Item", "");

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "emerald_reinforcement"), meta);
        CategoryAssignment assignment = resolve("tconstruct:emerald_reinforcement", meta);

        assertEquals("modifiers", meta.get(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND));
        assertEquals("modular_gear", assignment.categoryId());
        assertEquals("modifiers", assignment.subcategoryId());
    }

    @Test
    void tinkersCreativeSlotsRouteToModifiers() {
        Map<String, String> meta = meta("tconstruct", "Tinkers' General Items",
                "slimeknights.tconstruct.tools.item.CreativeSlotItem", "");

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "creative_slot"), meta);
        CategoryAssignment assignment = resolve("tconstruct:creative_slot", meta);

        assertEquals("modifiers", meta.get(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND));
        assertEquals("modular_gear", assignment.categoryId());
        assertEquals("modifiers", assignment.subcategoryId());
    }

    @Test
    void tinkersCopperCanDoesNotBecomeFocusedModularGear() {
        Map<String, String> meta = meta("tconstruct", "Tinkers' Smeltery",
                "slimeknights.tconstruct.smeltery.item.CopperCanItem", "");

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "copper_can"), meta);

        assertEquals("", meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
    }

    @Test
    void modularGearPathFactsUseExactTokens() {
        Map<String, String> meta = meta("tconstruct", "Tinkers' Construct", "", "");

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "gearbox"), meta);

        assertEquals("", meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
        assertEquals("", meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_PART, ""));
    }

    @Test
    void toolsStaySemanticUnderHybridPolicy() {
        Map<String, String> meta = meta("tconstruct", "Tinkers' Construct",
                "slimeknights.tconstruct.library.tools.item.ModifiablePickaxeItem", "");

        ModularGearCompat.enrichItem(new ResourceLocation("tconstruct", "manyullyn_pickaxe"), meta);
        CategoryAssignment assignment = resolve("tconstruct:manyullyn_pickaxe", meta, ItemFacet.HARVEST_TOOL);

        assertEquals("tools", meta.get(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND));
        assertEquals("manyullyn", meta.get(SearchNodeKeys.MODULAR_GEAR_MATERIAL));
        assertNotEquals("modular_gear", assignment.categoryId());
        assertEquals("tools", assignment.categoryId());
    }

    @Test
    void focusedPolicyKeepsGearToolsTogether() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.modularGearCategoryPolicy;
        try {
            AmiConfig.modularGearCategoryPolicy = AmiConfig.CompatCategoryPolicy.FOCUSED;
            Map<String, String> meta = meta("silentgear", "Silent Gear",
                    "net.silentchaos512.gear.item.GearPickaxeItem", "");

            ModularGearCompat.enrichItem(new ResourceLocation("silentgear", "crimson_iron_pickaxe"), meta);
            CategoryAssignment assignment = resolve("silentgear:crimson_iron_pickaxe", meta, ItemFacet.HARVEST_TOOL);

            assertEquals("modular_gear", assignment.categoryId());
            assertEquals("tools", assignment.subcategoryId());
        } finally {
            AmiConfig.modularGearCategoryPolicy = oldPolicy;
        }
    }

    @Test
    void unrelatedGearWordsDoNotGetFamilyMetadata() {
        Map<String, String> meta = meta("minecraft", "Combat", "", "");

        ModularGearCompat.enrichItem(new ResourceLocation("minecraft", "iron_pickaxe"), meta);

        assertEquals("", meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_FAMILY, ""));
        assertEquals("", meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, ""));
    }

    @Test
    void runtimeTooltipFactsKeepPlanningFieldsCompact() {
        ModularGearCompat.RuntimeFacts facts = ModularGearCompat.extractRuntimeFacts(List.of(
                "Crimson Iron Pickaxe",
                "Material: Crimson Iron",
                "Traits: Magnetic, Brittle",
                "Durability: 820",
                "Mining Speed: 8.5",
                "Flavor text that should not be indexed"
        ));

        assertEquals(List.of("crimson_iron"), facts.materials());
        assertEquals(List.of("magnetic", "brittle"), facts.traits());
        assertEquals(List.of("Durability: 820", "Mining Speed: 8.5"), facts.stats());
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
