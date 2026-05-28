package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmiOntologyTest {

    @Test
    void testClassifyItemByTag() {
        // Mock a node with "sword" tag
        SearchNode sword = new SearchNode(
                new ResourceLocation("minecraft:iron_sword"),
                NodeType.ITEM,
                "Iron Sword",
                0, 0,
                Map.of(SearchNodeKeys.TAGS, "minecraft:swords,minecraft:tools")
        );

        AmiOntology.Category cat = AmiOntology.classifyNode(sword);
        assertEquals(AmiOntology.TOOLS, cat);
    }

    @Test
    void testClassifyItemByPath() {
        // Mock a node with "apple" in path
        SearchNode apple = new SearchNode(
                new ResourceLocation("minecraft:apple"),
                NodeType.ITEM,
                "Apple",
                0, 0,
                Map.of()
        );

        AmiOntology.Category cat = AmiOntology.classifyNode(apple);
        assertEquals(AmiOntology.NATURE, cat);
    }

    @Test
    void testClassifyEggAsIngredient() {
        // Mock an egg
        SearchNode egg = new SearchNode(
                new ResourceLocation("minecraft:egg"),
                NodeType.ITEM,
                "Egg",
                0, 0,
                Map.of(SearchNodeKeys.TAGS, "c:eggs")
        );

        AmiOntology.Category cat = AmiOntology.classifyNode(egg);
        assertEquals(AmiOntology.INGREDIENTS, cat, "Eggs should be classified as Ingredients, but was: " + cat.id);
    }

    @Test
    void testClassifyBlockDefault() {
        // MASONRY items are always pre-computed at index time by OntologyClassifier,
        // so the runtime classifier needs pre-computed metadata to route here.
        SearchNode bricks = new SearchNode(
                new ResourceLocation("minecraft:bricks"),
                NodeType.ITEM,
                "Bricks",
                0, 0,
                Map.of(SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block")
        );

        AmiOntology.Category cat = AmiOntology.classifyNode(bricks);
        assertEquals(AmiOntology.MASONRY, cat);
    }

    @Test
    void testClassifyEnvironmentTypes() {
        SearchNode biome = new SearchNode(
                new ResourceLocation("minecraft:plains"),
                NodeType.BIOME,
                "Plains",
                0, 0, Map.of()
        );
        assertEquals(AmiOntology.ENVIRONMENT, AmiOntology.classifyNode(biome));

        SearchNode structure = new SearchNode(
                new ResourceLocation("minecraft:village"),
                NodeType.STRUCTURE,
                "Village",
                0, 0, Map.of()
        );
        assertEquals(AmiOntology.ENVIRONMENT, AmiOntology.classifyNode(structure));
    }
}
