package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchNodeMirrorDumpTest {

    @Test
    void replayReconstructsSemanticVerbsFromStableStorageTerminalMetadata() {
        SearchNode node = item("toms_storage:storage_terminal", Map.of(
                SearchNodeKeys.FACETS, "placeable,has_block_entity,light_source",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_CLASS, "com.tom.storagemod.block.StorageTerminalBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(SemanticVerbCodec.has(reclassified.metadata(), SemanticVerb.STORES_ITEMS));
        assertEquals("storage", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("misc", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("semantic_verb", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayStorageTerminalPhraseDoesNotMatchPartialSegmentTokens() {
        SearchNode node = item("example:storage_terminal_frame", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building",
                SearchNodeKeys.SUBTYPE_OF, "example:storage_terminal_frame",
                SearchNodeKeys.MATERIAL_GROUP, "example:storage_terminal_frame"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertFalse(SemanticVerbCodec.has(reclassified.metadata(), SemanticVerb.STORES_ITEMS));
        assertNotEquals("semantic_verb", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayStorageTerminalPhraseMatchesSuffixTokenSequence() {
        SearchNode node = item("example:wireless_storage_terminal", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(SemanticVerbCodec.has(reclassified.metadata(), SemanticVerb.STORES_ITEMS));
        assertEquals("storage", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("misc", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("semantic_verb", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayReconstructsSemanticVerbsFromStableBedMetadata() {
        SearchNode node = item("doggytalents:dog_bed/variant/dog_bed_271e6c3a5908", Map.of(
                SearchNodeKeys.FACETS, "placeable,decorative_block",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_CLASS, "doggytalents.common.block.DogBedBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(SemanticVerbCodec.has(reclassified.metadata(), SemanticVerb.SLEEP_REST));
        assertEquals("decoration", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("furniture", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("semantic_verb", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayReconstructsSemanticVerbsFromStableHutWorksiteMetadata() {
        SearchNode node = item("minecolonies:blockhutbuilder", Map.of(
                SearchNodeKeys.FACETS, "placeable,has_block_entity",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_CLASS, "com.minecolonies.core.blocks.huts.BlockHutBuilder",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(SemanticVerbCodec.has(reclassified.metadata(), SemanticVerb.SETTLEMENT_WORKSITE));
        assertEquals("utility", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("workstations", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("semantic_verb", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayReconstructsClimbAccessVerbFromStableClimbableMetadata() {
        SearchNode node = item("minecraft:ladder", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_TAGS, "minecraft:climbable,minecraft:fall_damage_resetting",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(SemanticVerbCodec.has(reclassified.metadata(), SemanticVerb.CLIMB_ACCESS));
        assertEquals("decoration", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("access", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("semantic_verb", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayReconstructsBarrierGrateVerbFromStableBarsMetadata() {
        SearchNode node = item("minecraft:iron_bars", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_TAGS, "minecraft:bars,minecraft:mineable/pickaxe",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(SemanticVerbCodec.has(reclassified.metadata(), SemanticVerb.BARRIER_GRATE));
        assertEquals("decoration", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("barriers", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("semantic_verb", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayRoutesStableDecalMetadataToDecorationSignage() {
        SearchNode node = item("createdeco:decal_warning", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_TAGS, "createdeco:decals,createdeco:weightless",
                SearchNodeKeys.BLOCK_CLASS, "com.github.talrey.createdeco.blocks.DecalBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertEquals("decoration", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("signage", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("hard_identity", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayRoutesStableSupplementariesTextileMetadataToDecorationTextiles() {
        SearchNode node = item("supplementaries:awning_white", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.TAGS, "c:dyed,supplementaries:awnings,c:dyed/white",
                SearchNodeKeys.BLOCK_TAGS, "c:dyed,c:dyed/white,supplementaries:awnings",
                SearchNodeKeys.BLOCK_CLASS, "net.mehvahdjukaar.supplementaries.common.block.blocks.AwningBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertEquals("decoration", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("textiles", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("hard_identity", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayRoutesStableStructuralDecorAndDisplayBoardMetadata() {
        SearchNode support = item("createdeco:iron_support", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_TAGS, "createdeco:supports,minecraft:mineable/pickaxe",
                SearchNodeKeys.BLOCK_CLASS, "com.github.talrey.createdeco.blocks.SupportBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));
        SearchNode displayBoard = item("dndecor:white_display_board", Map.of(
                SearchNodeKeys.FACETS, "placeable,has_block_entity",
                "blockShape", "partial",
                SearchNodeKeys.TAGS, "c:create/display_boards,c:create/dyed_display_boards",
                SearchNodeKeys.BLOCK_CLASS, "dev.lopyluna.dndecor.content.blocks.FlapDisplayTypeBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        List<SearchNode> reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(support, displayBoard));

        assertEquals("masonry", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("decorative", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("hard_identity", reclassified.get(0).meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
        assertEquals("decoration", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("signage", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("hard_identity", reclassified.get(1).meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayRoutesStableTextileNatureLightingAndAnimalEggMetadata() {
        SearchNode ribbon = item("swem:ribbon_champion", Map.of(
                SearchNodeKeys.FACETS, "placeable,has_block_entity",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_CLASS, "com.alaharranhonor.swem.block.RibbonBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));
        SearchNode sparkler = item("caverns_and_chasms:sparkler", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.TAGS, "caverns_and_chasms:sparklers",
                SearchNodeKeys.BLOCK_CLASS, "com.teamabnormals.caverns_and_chasms.common.block.SparklerBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));
        SearchNode sporeBlossom = item("pastel:black_spore_blossom", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_TAGS, "pastel:colored_spore_blossoms",
                SearchNodeKeys.BLOCK_CLASS, "earth.terrarium.pastel.blocks.conditional.colored_tree.ColoredSporeBlossomBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));
        SearchNode animalEgg = item("minecraft:turtle_egg", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_TAGS, "primal:animal_egg",
                SearchNodeKeys.BLOCK_CLASS, "net.minecraft.world.level.block.TurtleEggBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        List<SearchNode> reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(ribbon, sparkler, sporeBlossom, animalEgg));

        assertEquals("textiles", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("lighting", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("flora", reclassified.get(2).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("passive", reclassified.get(3).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void replayRoutesStableFurnitureAndStructuralTrimClassMetadata() {
        SearchNode pouffe = item("furniture:pouffe_white", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_CLASS, "com.berksire.furniture.core.block.PouffeBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));
        SearchNode pillar = item("hearth_and_timber:oak_pillar", Map.of(
                SearchNodeKeys.FACETS, "placeable",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_CLASS, "net.satisfy.hearth_and_timber.core.block.PillarBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        List<SearchNode> reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(pouffe, pillar));

        assertEquals("furniture", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("decorative", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void replayReconstructsMagicStructureFacetFromStableBlockClassMetadata() {
        SearchNode node = item("minecraft:enchanting_table", Map.of(
                SearchNodeKeys.FACETS, "placeable,has_block_entity,machine,workstation,light_source",
                "blockShape", "partial",
                SearchNodeKeys.BLOCK_CLASS, "net.minecraft.world.level.block.EnchantingTableBlock",
                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(reclassified.meta(SearchNodeKeys.FACETS).contains(ItemFacet.MAGIC_ARTIFACT.id()));
        assertEquals("magic", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("artifacts", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("hard_identity", reclassified.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void replayReconstructsUpgradeFacetFromStableAddonItemClassMetadata() {
        SearchNode node = item("industrialforegoing:range_addon_tier_5", Map.of(
                SearchNodeKeys.ITEM_CLASS, "com.buuz135.industrial.item.addon.RangeAddonItem"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(reclassified.meta(SearchNodeKeys.FACETS).contains(ItemFacet.UPGRADE.id()));
        assertEquals("tech", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("upgrades", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void replayReconstructsCommonIngredientFacetsFromStableTags() {
        SearchNode fertilizer = item("industrialforegoing:fertilizer", Map.of(
                SearchNodeKeys.TAGS, "c:fertilizers"
        ));
        SearchNode plastic = item("industrialforegoing:plastic", Map.of(
                SearchNodeKeys.TAGS, "c:plastics"
        ));

        List<SearchNode> reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(fertilizer, plastic));

        assertTrue(reclassified.get(0).meta(SearchNodeKeys.FACETS).contains(ItemFacet.INGREDIENT_ORGANIC.id()));
        assertEquals("ingredients", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("organic", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertTrue(reclassified.get(1).meta(SearchNodeKeys.FACETS).contains(ItemFacet.INGREDIENT_MINERAL.id()));
        assertEquals("ingredients", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("mineral", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void replayReconstructsTechComponentFacetFromStableLaserLensItemClassMetadata() {
        SearchNode node = item("industrialforegoing:white_laser_lens", Map.of(
                SearchNodeKeys.ITEM_CLASS, "com.buuz135.industrial.item.LaserLensItem"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(reclassified.meta(SearchNodeKeys.FACETS).contains(ItemFacet.TECH_COMPONENT.id()));
        assertEquals("tech", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("parts", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void replayReconstructsTransportFacetFromStableTransporterTypeItemClassMetadata() {
        SearchNode node = item("industrialforegoing:item_transporter_type", Map.of(
                SearchNodeKeys.ITEM_CLASS, "com.buuz135.industrial.item.ItemTransporterType"
        ));

        SearchNode reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(node)).get(0);

        assertTrue(reclassified.meta(SearchNodeKeys.FACETS).contains(ItemFacet.TRANSPORT.id()));
        assertEquals("tech", reclassified.meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("transport", reclassified.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void replayReconstructsInfinityToolAndWeaponFacetsFromStableItemClassMetadata() {
        SearchNode drill = item("industrialforegoing:infinity_drill/variant/infinity_drill_1234", Map.of(
                SearchNodeKeys.FACETS, "fluid_container",
                SearchNodeKeys.ITEM_CLASS, "com.buuz135.industrial.item.infinity.item.ItemInfinityDrill"
        ));
        SearchNode launcher = item("industrialforegoing:infinity_launcher/variant/infinity_launcher_1234", Map.of(
                SearchNodeKeys.FACETS, "fluid_container",
                SearchNodeKeys.ITEM_CLASS, "com.buuz135.industrial.item.infinity.item.ItemInfinityLauncher"
        ));

        List<SearchNode> reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(drill, launcher));

        assertTrue(reclassified.get(0).meta(SearchNodeKeys.FACETS).contains(ItemFacet.HARVEST_TOOL.id()));
        assertEquals("tools", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("harvest", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertTrue(reclassified.get(1).meta(SearchNodeKeys.FACETS).contains(ItemFacet.RANGED_WEAPON.id()));
        assertEquals("tools", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("ranged", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void replayRoutesStructurizeBuildWorkflowToolsToMinecoloniesBuildings() {
        SearchNode buildTool = item("structurize:sceptergold", Map.of(
                SearchNodeKeys.ITEM_CLASS, "com.ldtteam.structurize.items.ItemBuildTool"
        ));
        SearchNode caliper = item("structurize:caliper", Map.of(
                SearchNodeKeys.ITEM_CLASS, "com.ldtteam.structurize.items.ItemCaliper"
        ));

        List<SearchNode> reclassified = SearchNodeMirrorDump.reclassifyItemOntology(List.of(buildTool, caliper));

        assertEquals("minecolonies", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("buildings", reclassified.get(0).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("minecolonies", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("buildings", reclassified.get(1).meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    private static SearchNode item(String id, Map<String, String> metadata) {
        return new SearchNode(ResourceLocation.parse(id), NodeType.ITEM, id, 0xFFFFFF, 0, metadata);
    }
}
