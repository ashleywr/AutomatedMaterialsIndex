package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Final presentation cleanup for result trees, independent of how the tree was built.
 */
public final class ResultsTreeNormalizer {
    private ResultsTreeNormalizer() {
    }

    public static List<TreeNode> normalize(List<TreeNode> nodes) {
        List<TreeNode> result = new ArrayList<>();
        for (TreeNode node : nodes) {
            result.add(normalizeNode(node));
        }
        return result;
    }

    public static void normalizeChildren(TreeNode parent) {
        if (parent == null || parent.isLeaf()) {
            return;
        }

        List<TreeNode> normalized = normalize(parent.getChildren());
        parent.getChildren().clear();
        parent.getChildren().addAll(normalized);
        flattenMatchingChildGroups(parent);
        flattenOnlyChildIfRedundant(parent);
    }

    private static TreeNode normalizeNode(TreeNode node) {
        if (node.isLeaf()) {
            return node;
        }

        TreeNode copy = copyGroupNode(node);
        copy.getChildren().addAll(normalize(node.getChildren()));
        flattenMatchingChildGroups(copy);
        flattenOnlyChildIfRedundant(copy);
        return copy;
    }

    private static void flattenMatchingChildGroups(TreeNode parent) {
        List<TreeNode> flattened = new ArrayList<>();
        boolean changed = false;
        for (TreeNode child : parent.getChildren()) {
            if (shouldFlattenMatchingChildGroup(parent, child)) {
                flattened.addAll(child.getChildren());
                changed = true;
            } else {
                flattened.add(child);
            }
        }

        if (changed) {
            parent.getChildren().clear();
            parent.getChildren().addAll(flattened);
        }
    }

    private static void flattenOnlyChildIfRedundant(TreeNode parent) {
        if (parent.getChildren().size() != 1) {
            return;
        }

        TreeNode onlyChild = parent.getChildren().get(0);
        if (!shouldFlattenOnlyChildGroup(parent, onlyChild)) {
            return;
        }

        parent.getChildren().clear();
        parent.getChildren().addAll(onlyChild.getChildren());
    }

    private static boolean shouldFlattenMatchingChildGroup(TreeNode parent, TreeNode child) {
        return !child.isLeaf()
                && child.isExpanded()
                && !isRepresentativeVariantGroup(child)
                && (normalizedLabel(parent).equals(normalizedLabel(child))
                || shouldFlattenCoveredSemanticGroup(parent, child));
    }

    private static boolean shouldFlattenOnlyChildGroup(TreeNode parent, TreeNode child) {
        if (child.isLeaf() || !child.isExpanded()) {
            return false;
        }
        if (isRepresentativeVariantGroup(child)) {
            return false;
        }
        if (child.isHighCardinality()) {
            return "cardinality:minecraft:dye".equals(child.getKey())
                    || normalizedLabel(parent).equals(normalizedLabel(child))
                    || shouldFlattenCoveredSemanticGroup(parent, child);
        }
        return normalizedLabel(parent).equals(normalizedLabel(child));
    }

    private static boolean isRepresentativeVariantGroup(TreeNode node) {
        if (node == null || !node.isHighCardinality()) {
            return false;
        }
        for (TreeNode child : node.getChildren()) {
            if (child.isLeaf()) {
                String mode = child.getEntry().meta(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "");
                if ("auto".equals(mode) || "default_collapsed".equals(mode)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldFlattenCoveredSemanticGroup(TreeNode parent, TreeNode child) {
        return ("nature/fungi".equals(parent.getKey())
                && "cardinality:minecraft:mushroom".equals(child.getKey()))
                || isWoodAndLogsBranch(parent);
    }

    private static boolean isWoodAndLogsBranch(TreeNode parent) {
        String key = parent.getKey();
        return "nature/wood".equals(key) || (key != null && key.startsWith("nature/wood/"));
    }

    private static String normalizedLabel(TreeNode node) {
        return node.getLabel().getString().trim().toLowerCase(Locale.ROOT);
    }

    private static TreeNode copyGroupNode(TreeNode node) {
        TreeNode copy = new TreeNode(node.getKey(), node.getLabel());
        copy.setExpanded(node.isExpanded());
        copy.setModGroup(node.isModGroup());
        copy.setHighCardinality(node.isHighCardinality());
        copy.setItemCountOverride(node.getItemCountOverride());
        return copy;
    }
}
