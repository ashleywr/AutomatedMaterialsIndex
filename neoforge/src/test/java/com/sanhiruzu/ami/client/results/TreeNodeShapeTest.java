package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeNodeShapeTest {
    @Test
    void equivalentQuestRefreshIgnoresUserExpansionState() {
        TreeNode current = group("ftbquests:chapter/start", "Getting Started", leaf("minecraft:redstone", "x4 Redstone Dust"));
        current.setExpanded(false);
        TreeNode refreshed = group("ftbquests:chapter/start", "Getting Started", leaf("minecraft:redstone", "x4 Redstone Dust"));
        refreshed.setExpanded(true);

        assertTrue(TreeNodeShape.sameVisibleContent(List.of(current), List.of(refreshed)));
    }

    @Test
    void questCountOrItemChangesInvalidateShape() {
        TreeNode current = group("ftbquests:chapter/start", "Getting Started", leaf("minecraft:redstone", "x4 Redstone Dust"));
        TreeNode differentCount = group("ftbquests:chapter/start", "Getting Started", leaf("minecraft:redstone", "x8 Redstone Dust"));
        TreeNode differentItem = group("ftbquests:chapter/start", "Getting Started", leaf("minecraft:diamond", "x4 Diamond"));

        assertFalse(TreeNodeShape.sameVisibleContent(List.of(current), List.of(differentCount)));
        assertFalse(TreeNodeShape.sameVisibleContent(List.of(current), List.of(differentItem)));
    }

    @Test
    void waypointDeletionIsDetectedAsDifferentShape() {
        TreeNode waypoint = leaf("waystones:waystone_oak", "Oak Waystone");
        TreeNode item = leaf("minecraft:stone", "Stone");

        List<TreeNode> withWaypoint = List.of(waypoint, item);
        List<TreeNode> withoutWaypoint = List.of(item);

        assertFalse(TreeNodeShape.sameVisibleContent(withWaypoint, withoutWaypoint));
        assertFalse(TreeNodeShape.sameVisibleContent(withoutWaypoint, withWaypoint));
    }

    @Test
    void sameNodesAfterIncrementalUpdateAreStillEquivalent() {
        TreeNode item = leaf("minecraft:stone", "Stone");
        List<TreeNode> before = List.of(item);
        List<TreeNode> after = List.of(leaf("minecraft:stone", "Stone"));

        assertTrue(TreeNodeShape.sameVisibleContent(before, after));
    }

    @Test
    void resolvedNodeMetadataChangesInvalidateShape() {
        TreeNode fallback = leaf("example:missing_machine", "example:missing_machine", Map.of(
                SearchNodeKeys.MOD_ID, "example",
                "questFallback", "true"
        ));
        TreeNode resolved = leaf("example:missing_machine", "Missing Machine", Map.of(SearchNodeKeys.MOD_ID, "example"));

        assertFalse(TreeNodeShape.sameVisibleContent(List.of(fallback), List.of(resolved)));
    }

    private static TreeNode group(String key, String label, TreeNode... children) {
        TreeNode node = new TreeNode(key, Component.literal(label));
        node.setExpanded(true);
        for (TreeNode child : children) {
            node.addChild(child);
        }
        return node;
    }

    private static TreeNode leaf(String id, String label) {
        return leaf(id, label, Map.of(SearchNodeKeys.MOD_ID, Identifier.parse(id).getNamespace()));
    }

    private static TreeNode leaf(String id, String label, Map<String, String> metadata) {
        Identifier itemId = Identifier.parse(id);
        SearchNode node = new SearchNode(itemId, NodeType.ITEM, label, 0, 0, metadata);
        return new TreeNode(Component.literal(label), node);
    }
}
