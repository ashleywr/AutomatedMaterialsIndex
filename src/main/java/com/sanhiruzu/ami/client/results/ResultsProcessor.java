package com.sanhiruzu.ami.client.results;

import java.util.*;
import java.util.stream.Collectors;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNodeKeys;

public class ResultsProcessor {
    public enum SortField {
        ALPHABETICAL("Alphabetical"),
        COLOR("Color"),
        MOD("Mod");

        public final String displayName;
        SortField(String displayName) { this.displayName = displayName; }
    }

    public enum GroupBy {
        DIMENSION("Dimension"),
        MOD("Mod"),
        CATEGORY("Category");

        public final String displayName;
        GroupBy(String displayName) { this.displayName = displayName; }
    }

    private final SortField sortField;
    private final boolean ascending;
    private final GroupBy groupBy;
    private final Set<String> selectedMods; // null = all mods selected

    public ResultsProcessor(SortField sortField, boolean ascending, GroupBy groupBy, Set<String> selectedMods) {
        this.sortField = sortField;
        this.ascending = ascending;
        this.groupBy = groupBy;
        this.selectedMods = selectedMods != null ? selectedMods : new HashSet<>();
    }

    public List<TreeNode> process(List<SearchNode> results) {
        // Filter by selected mods
        List<SearchNode> filtered = results.stream()
                .filter(n -> selectedMods.isEmpty() || selectedMods.contains(n.id().getNamespace()))
                .collect(Collectors.toList());

        // Sort
        filtered.sort((a, b) -> compareNodes(a, b));
        if (!ascending) {
            Collections.reverse(filtered);
        }

        // Group
        return buildTree(filtered);
    }

    private int compareNodes(SearchNode a, SearchNode b) {
        return switch (sortField) {
            case ALPHABETICAL -> a.displayName().compareTo(b.displayName());
            case COLOR -> Integer.compare(a.color(), b.color());
            case MOD -> a.id().getNamespace().compareTo(b.id().getNamespace());
        };
    }

    private List<TreeNode> buildTree(List<SearchNode> sorted) {
        return switch (groupBy) {
            case DIMENSION -> groupByDimension(sorted);
            case MOD -> groupByMod(sorted);
            case CATEGORY -> groupByCategory(sorted);
        };
    }

    private List<TreeNode> groupByDimension(List<SearchNode> entries) {
        Map<String, TreeNode> dimGroups = new LinkedHashMap<>();

        for (SearchNode entry : entries) {
            String dim = entry.meta(SearchNodeKeys.DIMENSION, "overworld");
            String dimDisplay = switch (dim) {
                case "nether" -> "Nether";
                case "end" -> "End";
                default -> "Overworld";
            };

            TreeNode dimNode = dimGroups.computeIfAbsent(dimDisplay, k -> {
                TreeNode n = new TreeNode(k);
                n.setExpanded(true);
                return n;
            });
            TreeNode modNode = findOrCreateChild(dimNode, entry.id().getNamespace());
            modNode.addChild(new TreeNode(entry.displayName(), entry));
        }

        return new ArrayList<>(dimGroups.values());
    }

    private List<TreeNode> groupByMod(List<SearchNode> entries) {
        Map<String, TreeNode> modGroups = new LinkedHashMap<>();

        for (SearchNode entry : entries) {
            String namespace = entry.id().getNamespace();
            TreeNode modNode = modGroups.computeIfAbsent(namespace, k -> {
                TreeNode n = new TreeNode(k);
                n.setExpanded(true);
                return n;
            });
            TreeNode typeNode = findOrCreateChild(modNode, entry.type().displayName().getString());
            typeNode.addChild(new TreeNode(entry.displayName(), entry));
        }

        return new ArrayList<>(modGroups.values());
    }

    private List<TreeNode> groupByCategory(List<SearchNode> entries) {
        Map<NodeType, TreeNode> typeGroups = new LinkedHashMap<>();

        for (SearchNode entry : entries) {
            NodeType type = entry.type();
            TreeNode typeNode = typeGroups.computeIfAbsent(type, t -> {
                TreeNode n = new TreeNode(t.displayName().getString());
                n.setExpanded(true);
                return n;
            });
            TreeNode modNode = findOrCreateChild(typeNode, entry.id().getNamespace());
            modNode.addChild(new TreeNode(entry.displayName(), entry));
        }

        return typeGroups.values().stream().collect(Collectors.toList());
    }

    private TreeNode findOrCreateChild(TreeNode parent, String label) {
        for (TreeNode child : parent.getChildren()) {
            if (!child.isLeaf() && child.getLabel().equals(label)) {
                return child;
            }
        }
        TreeNode newChild = new TreeNode(label);
        parent.addChild(newChild);
        return newChild;
    }

    public Set<String> getAllMods(List<SearchNode> results) {
        return results.stream()
                .map(n -> n.id().getNamespace())
                .collect(Collectors.toSet());
    }

    // Getters for toolbar state
    public SortField getSortField() { return sortField; }
    public boolean isAscending() { return ascending; }
    public GroupBy getGroupBy() { return groupBy; }
    public Set<String> getSelectedMods() { return selectedMods; }
}
