package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CreateCompat;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateCompatTest {
    @Test
    void kineticClassEvidenceRoutesStrongCreateMachineryToCreateCategory() {
        Map<String, String> meta = meta("create", "Create: Kinetics",
                "net.minecraft.world.item.BlockItem",
                "com.simibubi.create.content.kinetics.press.MechanicalPressBlock");
        meta.put(SearchNodeKeys.RECIPE_CATEGORIES, "pressing");
        meta.put(SearchNodeKeys.RECIPE_USE_CATEGORIES, "pressing");

        CreateCompat.enrichItem(new Identifier("create", "mechanical_press"), meta);
        CategoryAssignment assignment = resolve("create:mechanical_press", meta,
                ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.MACHINE);

        assertEquals("kinetics", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_FACTS, "").contains("uses_su"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_FACTS, "").contains("kinetic"));
        assertEquals("uses_su", meta.get(SearchNodeKeys.CREATE_STRESS_ROLE));
        assertEquals("appliance", meta.get(SearchNodeKeys.CREATE_KINETIC_ROLE));
        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_RECIPE_ROLES, "").contains("pressing_output"));
        assertEquals("create", assignment.categoryId());
        assertEquals("kinetics", assignment.subcategoryId());
    }

    @Test
    void createMaterialsStaySemanticUnderHybridPolicy() {
        Map<String, String> meta = meta("create", "Create: Materials",
                "net.minecraft.world.item.Item", "");
        meta.put(SearchNodeKeys.TAGS, "c:ingots,c:ingots/zinc");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.INGOT)));

        CreateCompat.enrichItem(new Identifier("create", "zinc_ingot"), meta);
        CategoryAssignment assignment = resolve("create:zinc_ingot", meta, ItemFacet.INGOT);

        assertEquals("materials", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertNotEquals("create", assignment.categoryId());
    }

    @Test
    void createProcessingRecipeAloneDoesNotMakeAMachine() {
        Map<String, String> meta = meta("createdeco", "Create Deco Props",
                "net.minecraft.world.item.Item", "");
        meta.put(SearchNodeKeys.RECIPE_CATEGORIES, "pressing");
        meta.put(SearchNodeKeys.RECIPE_USE_CATEGORIES, "crafting");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.TECH_COMPONENT)));

        CreateCompat.enrichItem(new Identifier("createdeco", "zinc_sheet"), meta);
        CategoryAssignment assignment = resolve("createdeco:zinc_sheet", meta, ItemFacet.TECH_COMPONENT);

        assertEquals("materials", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_FACTS, "").contains("create_processing"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_RECIPE_ROLES, "").contains("pressing_output"));
        assertNotEquals("create", assignment.categoryId());
    }

    @Test
    void createSpecificFacetlessComponentsFallBackToCreateMaterials() {
        Map<String, String> meta = meta("create", "Create: Materials",
                "net.minecraft.world.item.Item", "");
        meta.put(SearchNodeKeys.RECIPE_CATEGORIES, "sequenced_assembly");
        meta.put(SearchNodeKeys.RECIPE_USE_CATEGORIES, "mechanical_crafting,crafting");

        CreateCompat.enrichItem(new Identifier("create", "precision_mechanism"), meta);
        CategoryAssignment assignment = resolve("create:precision_mechanism", meta);

        assertEquals("materials", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_FACTS, "").contains("create_component"));
        assertEquals("create", assignment.categoryId());
        assertEquals("materials", assignment.subcategoryId());
        assertEquals("compat_fallback", assignment.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void createAddonFacetlessToolsFallBackToCreateTools() {
        Map<String, String> meta = meta("createdieselgenerators", "Create Diesel Generators",
                "com.jesz.createdieselgenerators.content.tools.OilScannerItem", "");

        CreateCompat.enrichItem(new Identifier("createdieselgenerators", "oil_scanner"), meta);
        CategoryAssignment assignment = resolve("createdieselgenerators:oil_scanner", meta);

        assertEquals("tools", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertEquals("create", assignment.categoryId());
        assertEquals("tools", assignment.subcategoryId());
    }

    @Test
    void createOreExcavationDrillsFallBackToCreateMachines() {
        Map<String, String> meta = meta("createoreexcavation", "Create Ore Excavation",
                "net.minecraft.world.item.Item", "");

        CreateCompat.enrichItem(new Identifier("createoreexcavation", "diamond_drill"), meta);
        CategoryAssignment assignment = resolve("createoreexcavation:diamond_drill", meta);

        assertEquals("machines", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertEquals("create", assignment.categoryId());
        assertEquals("machines", assignment.subcategoryId());
    }

    @Test
    void addonSpecificPartsFromRuntimeDumpFallBackToCreateBuckets() {
        Map<String, String> coupling = meta("simulated", "Create: Simulated",
                "net.minecraft.world.item.Item", "");
        coupling.put(SearchNodeKeys.COMPAT_FAMILIES, "create");
        coupling.put(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "create");

        CreateCompat.enrichItem(new Identifier("simulated", "rope_coupling"), coupling);
        CategoryAssignment couplingAssignment = resolve("simulated:rope_coupling", coupling);

        assertEquals("materials", coupling.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertTrue(coupling.getOrDefault(SearchNodeKeys.CREATE_FACTS, "").contains("create_component"));
        assertEquals("create", couplingAssignment.categoryId());
        assertEquals("materials", couplingAssignment.subcategoryId());

        Map<String, String> amulet = meta("createaddition", "Create Crafts & Additions",
                "net.minecraft.world.item.Item", "");

        CreateCompat.enrichItem(new Identifier("createaddition", "electrum_amulet"), amulet);
        CategoryAssignment amuletAssignment = resolve("createaddition:electrum_amulet", amulet);

        assertEquals("tools", amulet.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertEquals("create", amuletAssignment.categoryId());
        assertEquals("tools", amuletAssignment.subcategoryId());
    }

    @Test
    void addonIconPlaceholdersFallBackToCreateMisc() {
        Map<String, String> meta = meta("create_confectionery", "Create Confectionery",
                "net.minecraft.world.item.Item", "");

        CreateCompat.enrichItem(new Identifier("create_confectionery", "create_confectionery_icon"), meta);
        CategoryAssignment assignment = resolve("create_confectionery:create_confectionery_icon", meta);

        assertEquals("misc", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertEquals("create", assignment.categoryId());
        assertEquals("misc", assignment.subcategoryId());
    }

    @Test
    void createProcessingDecorativeBlockStaysSemanticUnderHybridPolicy() {
        Map<String, String> meta = meta("createdeco", "Create Deco Props",
                "net.minecraft.world.item.BlockItem", "net.minecraft.world.level.block.Block");
        meta.put(SearchNodeKeys.RECIPE_CATEGORIES, "cutting");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(
                ItemFacet.PLACEABLE,
                ItemFacet.DECORATIVE_BLOCK
        )));

        CreateCompat.enrichItem(new Identifier("createdeco", "verdant_bricks"), meta);
        CategoryAssignment assignment = resolve("createdeco:verdant_bricks", meta,
                ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK);

        assertEquals("building", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_FACTS, "").contains("create_processing"));
        assertNotEquals("create", assignment.categoryId());
    }

    @Test
    void storedEnergyItemAloneDoesNotBecomeCreateMachine() {
        Map<String, String> meta = meta("create_jetpack", "Create Jetpack",
                "net.minecraft.world.item.Item", "");
        meta.put(SearchNodeKeys.ENERGY_CAPACITY, "10000");

        CreateCompat.enrichItem(new Identifier("create_jetpack", "jetpack"), meta);
        CategoryAssignment assignment = resolve("create_jetpack:jetpack", meta, ItemFacet.EQUIPPABLE);

        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_FACTS, "").contains("stores_fe"));
        assertEquals(null, meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertNotEquals("create", assignment.categoryId());
    }

    @Test
    void packageTagsAndClassesRouteToCreateLogistics() {
        Map<String, String> meta = meta("create", "Create: Logistics",
                "com.simibubi.create.content.logistics.box.PackageItem", "");
        meta.put(SearchNodeKeys.TAGS, "create:packages");

        CreateCompat.enrichItem(new Identifier("create", "cardboard_package"), meta);
        CategoryAssignment assignment = resolve("create:cardboard_package", meta);

        assertEquals("logistics", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_FACTS, "").contains("package"));
        assertEquals("create", assignment.categoryId());
        assertEquals("logistics", assignment.subcategoryId());
    }

    @Test
    void createAddonEnergyMetricsBecomeMachineFacts() {
        Map<String, String> meta = meta("createaddition", "Create Crafts & Additions",
                "net.minecraft.world.item.BlockItem",
                "com.mrh0.createaddition.blocks.alternator.AlternatorBlock");
        meta.put(SearchNodeKeys.ENERGY_GENERATION, "80");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(
                ItemFacet.PLACEABLE,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.HAS_ENERGY,
                ItemFacet.MACHINE
        )));

        CreateCompat.enrichItem(new Identifier("createaddition", "alternator"), meta);
        CategoryAssignment assignment = resolve("createaddition:alternator", meta,
                ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.HAS_ENERGY);

        assertEquals("machines", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.CREATE_FACTS, "").contains("generates_fe"));
        assertEquals("create", assignment.categoryId());
        assertEquals("machines", assignment.subcategoryId());
    }

    @Test
    void semanticCreatePolicyKeepsKineticMachinesInNormalOntology() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.createCategoryPolicy;
        try {
            AmiConfig.createCategoryPolicy = AmiConfig.CompatCategoryPolicy.SEMANTIC;
            Map<String, String> meta = meta("create", "Create: Kinetics",
                    "net.minecraft.world.item.BlockItem",
                    "com.simibubi.create.content.kinetics.press.MechanicalPressBlock");

            CreateCompat.enrichItem(new Identifier("create", "mechanical_press"), meta);
            CategoryAssignment assignment = resolve("create:mechanical_press", meta,
                    ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.MACHINE);

            assertEquals("kinetics", meta.get(SearchNodeKeys.CREATE_ITEM_KIND));
            assertNotEquals("create", assignment.categoryId());
            assertEquals("semantic", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        } finally {
            AmiConfig.createCategoryPolicy = oldPolicy;
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
                new Identifier(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
