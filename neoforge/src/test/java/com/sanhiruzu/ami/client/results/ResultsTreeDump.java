package com.sanhiruzu.ami.client.results;

import java.util.List;

final class ResultsTreeDump {
    private ResultsTreeDump() {
    }

    static String dump(List<TreeNode> nodes) {
        StringBuilder out = new StringBuilder();
        for (TreeNode node : nodes) {
            append(out, node, 0);
        }
        return out.toString();
    }

    private static void append(StringBuilder out, TreeNode node, int depth) {
        out.append("  ".repeat(depth));
        out.append(ResultsDumpLabels.label(node));
        if (!node.isLeaf()) {
            out.append(node.isExpanded() ? " [expanded]" : " [collapsed]");
            if (node.isHighCardinality()) {
                out.append(" [cardinality]");
            }
        }
        out.append('\n');

        for (TreeNode child : node.getChildren()) {
            append(out, child, depth + 1);
        }
    }
}
