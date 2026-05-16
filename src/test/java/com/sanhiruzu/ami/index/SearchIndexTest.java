package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchIndexTest {

    @AfterEach
    public void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    public void prefixAndSubstringSearchWork() {
        SearchIndex idx = new SearchIndex();

        var nodeA = new SearchNode(ResourceLocation.parse("ami:pack"), NodeType.ITEM, "Pack", 0, 0, new HashMap<>());
        var nodeB = new SearchNode(ResourceLocation.parse("ami:packable_box"), NodeType.ITEM, "Packable Box", 0, 0, new HashMap<>());
        var nodeC = new SearchNode(ResourceLocation.parse("ami:soph_backpack"), NodeType.ITEM, "Sophisticated Backpack", 0, 0, new HashMap<>());

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
                ResourceLocation.parse("minecraft:oak_stairs"),
                NodeType.ITEM,
                "Oak Stairs",
                0,
                0,
                Map.of(
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
    }
}
