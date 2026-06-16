package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsExpansionDefaultsTest {
    @Test
    void expandedDefaultExpandsNormalGroupsButKeepsCardinalityGroupsCollapsed() {
        TreeNode root = group("building", "Building");
        TreeNode cardinality = group("cardinality:minecraft:white_wool", "Wool");
        cardinality.setHighCardinality(true);
        root.addChild(cardinality);

        ResultsExpansionDefaults.apply(List.of(root), true);

        assertTrue(root.isExpanded());
        assertFalse(cardinality.isExpanded());
    }

    @Test
    void collapsedDefaultCollapsesAllGroups() {
        TreeNode root = group("building", "Building");
        TreeNode child = group("building/doors", "Doors");
        root.addChild(child);
        child.addChild(leaf("minecraft:oak_door", "Oak Door"));
        root.setExpanded(true);
        child.setExpanded(true);

        ResultsExpansionDefaults.apply(List.of(root), false);

        assertFalse(root.isExpanded());
        assertFalse(child.isExpanded());
    }

    @Test
    void transferExpansionStateKeepsUserCollapsedGroupsDuringIncrementalRefresh() {
        TreeNode currentRoot = group("ingredients", "Ingredients");
        TreeNode currentFood = group("ingredients/food", "Food");
        TreeNode currentMineral = group("ingredients/mineral", "Mineral");
        currentRoot.setExpanded(true);
        currentFood.setExpanded(false);
        currentMineral.setExpanded(true);
        currentRoot.addChild(currentFood);
        currentRoot.addChild(currentMineral);

        TreeNode refreshedRoot = group("ingredients", "Ingredients");
        TreeNode refreshedFood = group("ingredients/food", "Food");
        TreeNode refreshedMineral = group("ingredients/mineral", "Mineral");
        refreshedRoot.setExpanded(false);
        refreshedFood.setExpanded(true);
        refreshedMineral.setExpanded(false);
        refreshedFood.addChild(leaf("minecraft:apple", "Apple"));
        refreshedMineral.addChild(leaf("minecraft:iron_ingot", "Iron Ingot"));
        refreshedRoot.addChild(refreshedFood);
        refreshedRoot.addChild(refreshedMineral);

        ResultsExpansionDefaults.transferExpansionState(List.of(currentRoot), List.of(refreshedRoot));

        assertTrue(refreshedRoot.isExpanded());
        assertFalse(refreshedFood.isExpanded());
        assertTrue(refreshedMineral.isExpanded());
    }

    private static TreeNode group(String key, String label) {
        return new TreeNode(key, Component.literal(label));
    }

    private static TreeNode leaf(String id, String label) {
        SearchNode node = new SearchNode(Identifier.parse(id), NodeType.ITEM, label, 0, 0, Map.of());
        return new TreeNode(Component.literal(label), node);
    }
}
