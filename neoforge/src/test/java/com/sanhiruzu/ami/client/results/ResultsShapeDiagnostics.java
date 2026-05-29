package com.sanhiruzu.ami.client.results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ResultsShapeDiagnostics {
    private ResultsShapeDiagnostics() {
    }

    static List<String> warnings(List<TreeNode> roots) {
        List<String> warnings = new ArrayList<>();
        collectWarnings(roots, warnings, "");
        return warnings;
    }

    private static void collectWarnings(List<TreeNode> nodes, List<String> warnings, String path) {
        for (TreeNode node : nodes) {
            String nodePath = path.isEmpty() ? ResultsDumpLabels.label(node) : path + " > " + ResultsDumpLabels.label(node);
            if (!node.isLeaf()) {
                if (!node.isExpanded()) {
                    warnings.add("collapsed group: " + nodePath);
                }
                if (node.getChildren().isEmpty()) {
                    warnings.add("empty group: " + nodePath);
                }
                if (node.getChildren().size() == 1) {
                    TreeNode onlyChild = node.getChildren().get(0);
                    if (!onlyChild.isLeaf() && node.getLabel().getString().equalsIgnoreCase(onlyChild.getLabel().getString())) {
                        warnings.add("duplicate only-child group: " + nodePath);
                    }
                }
                warnForDuplicateLeafLabels(node, warnings, nodePath);
                collectWarnings(node.getChildren(), warnings, nodePath);
            }
        }
    }

    private static void warnForDuplicateLeafLabels(TreeNode node, List<String> warnings, String path) {
        if (isIntentionalLeafFamily(node)) {
            return;
        }
        Map<String, Integer> counts = new HashMap<>();
        Map<String, List<String>> ids = new HashMap<>();
        for (TreeNode child : node.getChildren()) {
            if (child.isLeaf()) {
                String label = child.getLabel().getString();
                counts.merge(label, 1, Integer::sum);
                ids.computeIfAbsent(label, ignored -> new ArrayList<>()).add(child.getEntry().id().toString());
            }
        }
        for (var entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                warnings.add("duplicate leaf label under " + path + ": "
                        + entry.getKey() + " x" + entry.getValue()
                        + " (" + String.join(", ", ids.getOrDefault(entry.getKey(), List.of())) + ")");
            }
        }
    }

    private static boolean isIntentionalLeafFamily(TreeNode node) {
        return node.getKey().startsWith("cardinality:family:")
                || node.getKey().startsWith("duplicate_label:")
                || node.isHighCardinality();
    }
}
