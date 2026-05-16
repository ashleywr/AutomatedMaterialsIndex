package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.*;
import java.util.stream.Collectors;

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
    private final Set<String> selectedMods; // empty = all mods
    private final Set<String> activeFacets; // empty = no facet filter

    public ResultsProcessor(SortField sortField, boolean ascending, GroupBy groupBy,
                            Set<String> selectedMods, Set<String> activeFacets) {
        this.sortField    = sortField;
        this.ascending    = ascending;
        this.groupBy      = groupBy;
        this.selectedMods = selectedMods  != null ? selectedMods  : new HashSet<>();
        this.activeFacets = activeFacets != null ? activeFacets : new HashSet<>();
    }

    public List<TreeNode> process(List<SearchNode> results) {
        // Filter by selected mods, then by active facets
        List<SearchNode> filtered = results.stream()
                .filter(n -> selectedMods.isEmpty() || selectedMods.contains(n.id().getNamespace()))
                .filter(this::matchesFacets)
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
        Map<String, TreeNode> catGroups = new LinkedHashMap<>();

        // Pre-insert all category nodes to preserve CATEGORIES order
        for (AmiOntology.Category cat : AmiOntology.CATEGORIES) {
            TreeNode n = new TreeNode(cat.displayName);
            n.setExpanded(true);
            catGroups.put(cat.id, n);
        }

        for (SearchNode entry : entries) {
            AmiOntology.Category cat = AmiOntology.classifyNode(entry);
            TreeNode catNode = catGroups.get(cat.id);

            String subId = entry.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
            if (!subId.isEmpty()) {
                // Use named sub-category as the second level when available
                String subName = cat.subCategories.stream()
                        .filter(s -> s.id().equals(subId))
                        .map(AmiOntology.SubCategory::displayName)
                        .findFirst()
                        .orElse(subId);
                TreeNode subNode = findOrCreateChild(catNode, subName);
                subNode.addChild(new TreeNode(entry.displayName(), entry));
            } else {
                // Fall back to mod namespace grouping for items without sub-category data
                TreeNode modNode = findOrCreateChild(catNode, entry.id().getNamespace());
                modNode.addChild(new TreeNode(entry.displayName(), entry));
            }
        }

        // Only include categories that actually have children
        return catGroups.values().stream()
                .filter(n -> !n.getChildren().isEmpty())
                .collect(Collectors.toList());
    }

    private TreeNode findOrCreateChild(TreeNode parent, String label) {
        for (TreeNode child : parent.getChildren()) {
            if (!child.isLeaf() && child.getLabel().equals(label)) {
                return child;
            }
        }
        TreeNode newChild = new TreeNode(label);
        newChild.setExpanded(true);
        parent.addChild(newChild);
        return newChild;
    }

    /**
     * Returns true when the node's ontology category is among the active facets,
     * or when no facets are active (show everything).
     */
    private boolean matchesFacets(SearchNode node) {
        if (activeFacets.isEmpty()) return true;
        return activeFacets.contains(AmiOntology.classifyNode(node).id);
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
    public Set<String> getActiveFacets() { return activeFacets; }
}
