package com.sanhiruzu.ami.client.results;

import java.util.List;

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
}
