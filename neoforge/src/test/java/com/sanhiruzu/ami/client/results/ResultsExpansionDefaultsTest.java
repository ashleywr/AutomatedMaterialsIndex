package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    private static TreeNode group(String key, String label) {
        return new TreeNode(key, Component.literal(label));
    }

    private static TreeNode leaf(String id, String label) {
        SearchNode node = new SearchNode(ResourceLocation.parse(id), NodeType.ITEM, label, 0, 0, Map.of());
        return new TreeNode(Component.literal(label), node);
    }
}
