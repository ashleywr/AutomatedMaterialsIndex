package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchIndexTest {

    @AfterEach
    public void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    public void prefixAndSubstringSearchWork() {
        SearchIndex idx = new SearchIndex();

        var nodeA = new SearchNode(new ResourceLocation("ami:pack"), NodeType.ITEM, "Pack", 0, 0, new HashMap<>());
        var nodeB = new SearchNode(new ResourceLocation("ami:packable_box"), NodeType.ITEM, "Packable Box", 0, 0, new HashMap<>());
        var nodeC = new SearchNode(new ResourceLocation("ami:soph_backpack"), NodeType.ITEM, "Sophisticated Backpack", 0, 0, new HashMap<>());

        idx.addNode(nodeA);
        idx.addNode(nodeB);
        idx.addNode(nodeC);

        List<SearchNode> prefix = idx.prefixSearch("pack");
        // prefix should find Pack and Packable Box (exact-start matches)
        assertTrue(prefix.stream().anyMatch(n -> n.displayName().equals("Pack")));
        assertTrue(prefix.stream().anyMatch(n -> n.displayName().equals("Packable Box")));

        List<SearchNode> substring = idx.substringSearch("backpack");
        assertTrue(substring.stream().anyMatch(n -> n.displayName().equals("Sophisticated Backpack")));
    }

    @Test
    public void metadataAliasesAreSearchable() {
        SearchIndex idx = new SearchIndex();

        var oakStairs = new SearchNode(
                new ResourceLocation("minecraft:oak_stairs"),
                NodeType.ITEM,
                "Oak Stairs",
                0,
                0,
                Map.of(
                        SearchNodeKeys.FACETS, "placeable,stairs,wood_block",
                        SearchNodeKeys.MATERIAL_GROUP, "minecraft:oak",
                        SearchNodeKeys.VARIANT_GROUP, "stairs",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "building_blocks"
                )
        );

        idx.addNode(oakStairs);

        assertTrue(idx.prefixSearch("stairs").contains(oakStairs));
        assertTrue(idx.substringSearch("building blocks").contains(oakStairs));
        assertTrue(idx.substringSearch("building_blocks").contains(oakStairs));
        assertTrue(idx.substringSearch("minecraft:oak").contains(oakStairs));
        assertTrue(idx.substringSearch("wood block").contains(oakStairs));
    }

    @Test
    public void colonDelimitedSearchWorks() {
        SearchIndex idx = new SearchIndex();

        var cricket = new SearchNode(
                new ResourceLocation("zen_amphibia:cricket"),
                NodeType.ENTITY,
                "Cricket",
                0,
                0,
                new HashMap<>()
        );

        idx.addNode(cricket);

        assertTrue(idx.prefixSearch("zen_amphibia:cricket").contains(cricket));
        assertTrue(idx.prefixSearch("cricket").contains(cricket));
        assertTrue(idx.substringSearch("amphibia").contains(cricket));
    }

    @Test
    public void globalIndexGetNodesReturnsStableSnapshot() {
        GlobalIndex index = GlobalIndex.getInstance();
        var codBucket = new SearchNode(
                new ResourceLocation("minecraft:cod_bucket"),
                NodeType.ITEM,
                "Bucket of Cod",
                0,
                0,
                Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, "food")
        );
        index.addNode(codBucket);

        List<SearchNode> snapshot = index.getNodes(NodeType.ITEM);
        index.replaceNode(
                codBucket.id(),
                codBucket.type(),
                codBucket.withMetadata(Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry"))
        );

        assertEquals(1, snapshot.size());
        assertEquals("food", snapshot.get(0).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("masonry", index.getNodes(NodeType.ITEM).get(0).meta(SearchNodeKeys.ONTOLOGY_CATEGORY));
    }
}
