package com.sanhiruzu.ami.client.results;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ResultsExpansionDefaults {
    private ResultsExpansionDefaults() {
    }

    public static void apply(List<TreeNode> roots, boolean expandedByDefault) {
        if (roots == null) return;
        for (TreeNode root : roots) {
            apply(root, expandedByDefault);
        }
    }

    private static void apply(TreeNode node, boolean expandedByDefault) {
        if (node == null || node.isLeaf()) return;

        node.setExpanded(expandedByDefault && !node.isHighCardinality());
        for (TreeNode child : node.getChildren()) {
            apply(child, expandedByDefault);
        }
    }

    public static void transferExpansionState(List<TreeNode> currentRoots, List<TreeNode> refreshedRoots) {
        if (currentRoots == null || refreshedRoots == null) return;
        transferSiblings(currentRoots, refreshedRoots);
    }

    private static void transferSiblings(List<TreeNode> current, List<TreeNode> refreshed) {
        Map<String, TreeNode> currentByIdentity = new HashMap<>();
        for (TreeNode node : current) {
            if (node == null || node.isLeaf()) continue;
            currentByIdentity.putIfAbsent(groupIdentity(node), node);
        }

        for (TreeNode node : refreshed) {
            if (node == null || node.isLeaf()) continue;
            TreeNode previous = currentByIdentity.get(groupIdentity(node));
            if (previous == null) continue;
            node.setExpanded(previous.isExpanded());
            transferSiblings(previous.getChildren(), node.getChildren());
        }
    }

    private static String groupIdentity(TreeNode node) {
        String key = node.getKey();
        if (key != null && !key.isBlank()) {
            return "key:" + key;
        }
        return "label:" + (node.getLabel() == null ? "" : node.getLabel().getString());
    }
}
