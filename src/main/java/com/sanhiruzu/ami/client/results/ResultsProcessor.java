package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
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

    // Tag-substring patterns that qualify a node for each facet.
    // Tags are stored as comma-joined resource locations (e.g. "minecraft:foods,c:chests").
    private static final Map<String, List<String>> FACET_TAG_PATTERNS = Map.of(
        "storage", List.of("chest", "shulker", "barrel", "storage", "bundle"),
        "weapons", List.of("sword", "bow", "weapon", "trident"),
        "food",    List.of("food"),
        "tools",   List.of("pickaxe", "shovel", ":hoes", ":axes", ":tools"),
        "magic",   List.of("potion", "magic", "enchant")
    );

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
        newChild.setExpanded(true);
        parent.addChild(newChild);
        return newChild;
    }

    /**
     * Returns true when the node should be shown given the current active facets.
     * Non-ITEM nodes are excluded whenever any facet is active, since facets are
     * tag-based and only item tags are populated.
     */
    private boolean matchesFacets(SearchNode node) {
        if (activeFacets.isEmpty()) return true;
        if (node.type() != NodeType.ITEM) return false;

        String tags = node.meta(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        for (String facetId : activeFacets) {
            List<String> patterns = FACET_TAG_PATTERNS.getOrDefault(facetId, List.of());
            for (String pattern : patterns) {
                if (tags.contains(pattern)) return true;
            }
        }
        return false;
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
