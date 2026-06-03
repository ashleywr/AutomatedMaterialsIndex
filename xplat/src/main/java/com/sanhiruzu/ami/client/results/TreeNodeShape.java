package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.SearchNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TreeNodeShape {
    private TreeNodeShape() {
    }

    public static boolean sameVisibleContent(List<TreeNode> left, List<TreeNode> right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            if (!sameVisibleContent(left.get(i), right.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameVisibleContent(TreeNode left, TreeNode right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (left.isLeaf() != right.isLeaf()) return false;
        if (!Objects.equals(label(left), label(right))) return false;

        if (left.isLeaf()) {
            return sameEntry(left.getEntry(), right.getEntry());
        }

        if (!Objects.equals(left.getKey(), right.getKey())) return false;
        if (left.isModGroup() != right.isModGroup()) return false;
        if (left.isHighCardinality() != right.isHighCardinality()) return false;
        if (left.getItemCountOverride() != right.getItemCountOverride()) return false;
        return sameVisibleContent(left.getChildren(), right.getChildren());
    }

    private static String label(TreeNode node) {
        return node.getLabel() == null ? "" : node.getLabel().getString();
    }

    private static boolean sameEntry(SearchNode left, SearchNode right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return Objects.equals(left.id(), right.id())
                && left.type() == right.type()
                && Objects.equals(left.displayName(), right.displayName())
                && left.color() == right.color()
                && left.searchWeight() == right.searchWeight()
                && sameMetadata(left.metadata(), right.metadata());
    }

    private static boolean sameMetadata(Map<String, String> left, Map<String, String> right) {
        return Objects.equals(left, right);
    }
}
