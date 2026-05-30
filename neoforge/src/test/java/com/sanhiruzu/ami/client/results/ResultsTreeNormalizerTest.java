package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResultsTreeNormalizerTest {
    private static TreeNode group(String key, String label) {
        TreeNode node = new TreeNode(key, Component.literal(label));
        node.setExpanded(true);
        return node;
    }

    private static TreeNode leaf(String path, String displayName) {
        return new TreeNode(Component.literal(displayName), new SearchNode(
                new ResourceLocation("minecraft:" + path),
                NodeType.ITEM,
                displayName,
                0,
                0,
                Map.of()
        ));
    }

    @Test
    void normalizeChildrenFlattensDuplicateLazySubgroup() {
        TreeNode parent = group("banners", "Banners");
        TreeNode child = group("cardinality:family:banners", "Banners");
        child.addChild(leaf("white_banner", "White Banner"));
        child.addChild(leaf("black_banner", "Black Banner"));
        parent.addChild(child);

        ResultsTreeNormalizer.normalizeChildren(parent);

        assertEquals(2, parent.getChildren().size());
        assertEquals("White Banner", parent.getChildren().get(0).getLabel().getString());
        assertEquals("Black Banner", parent.getChildren().get(1).getLabel().getString());
    }

    @Test
    void normalizeChildrenFlattensDuplicateSubgroupEvenWithSiblings() {
        TreeNode parent = group("banners", "Banners");
        TreeNode child = group("cardinality:family:banners", "Banners");
        child.addChild(leaf("white_banner", "White Banner"));
        child.addChild(leaf("black_banner", "Black Banner"));
        parent.addChild(child);
        parent.addChild(leaf("flower_banner_pattern", "Flower Banner Pattern"));

        ResultsTreeNormalizer.normalizeChildren(parent);

        assertEquals(3, parent.getChildren().size());
        assertEquals("White Banner", parent.getChildren().get(0).getLabel().getString());
        assertEquals("Black Banner", parent.getChildren().get(1).getLabel().getString());
        assertEquals("Flower Banner Pattern", parent.getChildren().get(2).getLabel().getString());
    }

    @Test
    void normalizeChildrenDoesNotFlattenDyeSubgroupWhenSiblingExists() {
        TreeNode parent = group("dyes", "Dyes & Pigments");
        TreeNode child = group("cardinality:minecraft:dye", "Dye");
        child.setHighCardinality(true);
        child.addChild(leaf("white_dye", "White Dye"));
        child.addChild(leaf("black_dye", "Black Dye"));
        parent.addChild(child);
        parent.addChild(leaf("black_pigment", "Black Pigment"));

        ResultsTreeNormalizer.normalizeChildren(parent);

        assertEquals(2, parent.getChildren().size());
        assertEquals("Dye", parent.getChildren().get(0).getLabel().getString());
        assertEquals("Black Pigment", parent.getChildren().get(1).getLabel().getString());
    }

    @Test
    void normalizeChildrenPreservesUsefulLazySubgroup() {
        TreeNode parent = group("flora", "Flora");
        TreeNode child = group("cardinality:minecraft:mushroom", "Mushrooms");
        child.setHighCardinality(true);
        child.addChild(leaf("red_mushroom", "Red Mushroom"));
        child.addChild(leaf("brown_mushroom", "Brown Mushroom"));
        parent.addChild(child);

        ResultsTreeNormalizer.normalizeChildren(parent);

        assertEquals(1, parent.getChildren().size());
        assertEquals("Mushrooms", parent.getChildren().get(0).getLabel().getString());
    }

    @Test
    void normalizeChildrenFlattensMushroomSubgroupInsideFungiSubcategory() {
        TreeNode parent = group("nature/fungi", "Fungi & Forage");
        parent.addChild(leaf("crimson_nylium", "Crimson Nylium"));
        parent.addChild(leaf("warped_nylium", "Warped Nylium"));
        TreeNode child = group("cardinality:minecraft:mushroom", "Mushroom");
        child.setHighCardinality(true);
        child.addChild(leaf("brown_mushroom", "Brown Mushroom"));
        child.addChild(leaf("red_mushroom", "Red Mushroom"));
        child.addChild(leaf("crimson_fungus", "Crimson Fungus"));
        child.addChild(leaf("warped_fungus", "Warped Fungus"));
        parent.addChild(child);

        ResultsTreeNormalizer.normalizeChildren(parent);

        assertEquals(6, parent.getChildren().size());
        assertEquals("Crimson Nylium", parent.getChildren().get(0).getLabel().getString());
        assertEquals("Warped Nylium", parent.getChildren().get(1).getLabel().getString());
        assertEquals("Brown Mushroom", parent.getChildren().get(2).getLabel().getString());
        assertEquals("Red Mushroom", parent.getChildren().get(3).getLabel().getString());
        assertEquals("Crimson Fungus", parent.getChildren().get(4).getLabel().getString());
        assertEquals("Warped Fungus", parent.getChildren().get(5).getLabel().getString());
    }

    @Test
    void normalizeChildrenFlattensDuplicateHighCardinalitySubgroup() {
        TreeNode parent = group("minecraft:candle", "Candle");
        TreeNode child = group("cardinality:minecraft:candle", "Candle");
        child.setHighCardinality(true);
        child.addChild(leaf("candle", "Candle"));
        child.addChild(leaf("white_candle", "White Candle"));
        parent.addChild(child);

        ResultsTreeNormalizer.normalizeChildren(parent);

        assertEquals(2, parent.getChildren().size());
        assertEquals("Candle", parent.getChildren().get(0).getLabel().getString());
        assertEquals("White Candle", parent.getChildren().get(1).getLabel().getString());
    }

    @Test
    void normalizeChildrenFlattensWoodKindAndMaterialLayers() {
        TreeNode parent = group("nature/wood", "Wood & Logs");
        TreeNode logs = group("nature/wood/logs", "Logs");
        TreeNode pine = group("cardinality:biomesoplenty:pine", "Pine");
        pine.setHighCardinality(true);
        pine.addChild(leaf("pine_log", "Pine Log"));
        pine.addChild(leaf("stripped_pine_log", "Stripped Pine Log"));
        logs.addChild(pine);
        parent.addChild(logs);

        ResultsTreeNormalizer.normalizeChildren(parent);

        assertEquals(2, parent.getChildren().size());
        assertEquals("Pine Log", parent.getChildren().get(0).getLabel().getString());
        assertEquals("Stripped Pine Log", parent.getChildren().get(1).getLabel().getString());
    }
}
