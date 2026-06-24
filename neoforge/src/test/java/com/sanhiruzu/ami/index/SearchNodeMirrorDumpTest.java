package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static SearchNode item(String id, Map<String, String> metadata) {
        return new SearchNode(ResourceLocation.parse(id), NodeType.ITEM, id, 0xFFFFFF, 0, metadata);
    }
}
