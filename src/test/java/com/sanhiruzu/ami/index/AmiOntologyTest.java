package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class AmiOntologyTest {

    @Test
    void testClassifyItemByTag() {
        // Mock a node with "sword" tag
        SearchNode sword = new SearchNode(
            ResourceLocation.parse("minecraft:iron_sword"),
            NodeType.ITEM,
            "Iron Sword",
            0, 0,
            Map.of(SearchNodeKeys.TAGS, "minecraft:swords,minecraft:tools")
        );

        AmiOntology.Category cat = AmiOntology.classifyNode(sword);
        assertEquals(AmiOntology.WEAPONS, cat);
    }

    @Test
    void testClassifyItemByPath() {
        // Mock a node with "apple" in path
        SearchNode apple = new SearchNode(
            ResourceLocation.parse("minecraft:apple"),
            NodeType.ITEM,
            "Apple",
            0, 0,
            Map.of()
        );

        AmiOntology.Category cat = AmiOntology.classifyNode(apple);
        assertEquals(AmiOntology.FOOD, cat);
    }

    @Test
    void testClassifyBlockDefault() {
        // Mock a random block
        SearchNode bricks = new SearchNode(
            ResourceLocation.parse("minecraft:bricks"),
            NodeType.ITEM,
            "Bricks",
            0, 0,
            Map.of()
        );

        AmiOntology.Category cat = AmiOntology.classifyNode(bricks);
        assertEquals(AmiOntology.BLOCKS, cat);
    }

    @Test
    void testClassifyEnvironmentTypes() {
        SearchNode biome = new SearchNode(
            ResourceLocation.parse("minecraft:plains"),
            NodeType.BIOME,
            "Plains",
            0, 0, Map.of()
        );
        assertEquals(AmiOntology.ENVIRONMENT, AmiOntology.classifyNode(biome));

        SearchNode structure = new SearchNode(
            ResourceLocation.parse("minecraft:village"),
            NodeType.STRUCTURE,
            "Village",
            0, 0, Map.of()
        );
        assertEquals(AmiOntology.ENVIRONMENT, AmiOntology.classifyNode(structure));
    }
}
