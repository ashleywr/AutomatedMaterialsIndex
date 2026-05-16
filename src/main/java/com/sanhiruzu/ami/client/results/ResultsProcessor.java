package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

public class ResultsProcessor {
    public enum SortField {
        ALPHABETICAL("ami.sort.alphabetical"),
        COLOR("ami.sort.color"),
        MOD("ami.sort.mod"),
        STORAGE_CAPACITY("ami.sort.storage"),
        DPS("ami.sort.dps");

        public final Component displayName;
        SortField(String key) { this.displayName = Component.translatable(key); }
    }

    public enum GroupBy {
        DIMENSION("ami.group.dimension"),
        MOD("ami.group.mod"),
        CATEGORY("ami.group.category"),
        MATERIAL("ami.group.material"),
        SHAPE("ami.group.shape");

        public final Component displayName;
        GroupBy(String key) { this.displayName = Component.translatable(key); }
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
            case STORAGE_CAPACITY -> compareNumericMeta(a, b, SearchNodeKeys.ESM_CAPACITY);
            case DPS -> compareNumericMeta(a, b, SearchNodeKeys.DPS);
        };
    }

    private int compareNumericMeta(SearchNode a, SearchNode b, String metadataKey) {
        return Double.compare(parseNumericMeta(a, metadataKey), parseNumericMeta(b, metadataKey));
    }

    private double parseNumericMeta(SearchNode node, String metadataKey) {
        String value = node.meta(metadataKey, "");
        if (value.isBlank()) return Double.NEGATIVE_INFINITY;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    private List<TreeNode> buildTree(List<SearchNode> sorted) {
        return switch (groupBy) {
            case DIMENSION -> groupByDimension(sorted);
            case MOD -> groupByMod(sorted);
            case CATEGORY -> groupByCategory(sorted);
            case MATERIAL -> groupByMetadata(sorted, SearchNodeKeys.MATERIAL_GROUP, "Unknown Material", true);
            case SHAPE -> groupByMetadata(sorted, SearchNodeKeys.VARIANT_GROUP, "Unknown Shape", false);
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
            modNode.setModGroup(true);
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
                n.setModGroup(true);
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

        boolean blocksMaterial = AMIConfig.BLOCK_SUBGROUP.get() == AMIConfig.BlockSubgroup.MATERIAL;

        for (SearchNode entry : entries) {
            AmiOntology.Category cat = AmiOntology.classifyNode(entry);
            TreeNode catNode = catGroups.get(cat.id);

            String subId = entry.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");

            // For building-type block shapes, swap to material grouping when config says so.
            // Structural subcategories (functional/redstone/decorative) are never swapped.
            if (cat == AmiOntology.BLOCKS && blocksMaterial && isBuildingShape(subId)) {
                String matId = entry.meta(SearchNodeKeys.BLOCKS_MATERIAL, "");
                if (!matId.isEmpty()) subId = matId;
            }

            if (!subId.isEmpty()) {
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
                modNode.setModGroup(true);
                modNode.addChild(new TreeNode(entry.displayName(), entry));
            }
        }

        // Only include categories that actually have children
        return catGroups.values().stream()
                .filter(n -> !n.getChildren().isEmpty())
                .collect(Collectors.toList());
    }

    private List<TreeNode> groupByMetadata(List<SearchNode> entries, String metadataKey, String fallback, boolean compactResourceIds) {
        Map<String, TreeNode> groups = new LinkedHashMap<>();

        for (SearchNode entry : entries) {
            String groupValue = entry.meta(metadataKey, "");
            String label = formatGroupLabel(groupValue, fallback, compactResourceIds);
            TreeNode groupNode = groups.computeIfAbsent(label, k -> {
                TreeNode n = new TreeNode(k);
                n.setExpanded(true);
                return n;
            });
            groupNode.addChild(new TreeNode(entry.displayName(), entry));
        }

        return new ArrayList<>(groups.values());
    }

    private String formatGroupLabel(String value, String fallback, boolean compactResourceIds) {
        if (value == null || value.isBlank()) return fallback;

        String label = value;
        if (compactResourceIds) {
            int namespaceSep = label.indexOf(':');
            if (namespaceSep >= 0 && namespaceSep + 1 < label.length()) {
                label = label.substring(namespaceSep + 1);
            }
        }

        label = label.replace('_', ' ').trim();
        if (label.isEmpty()) return fallback;

        String[] words = label.split("\\s+");
        StringBuilder out = new StringBuilder(label.length());
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
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

    private static final Set<String> BUILDING_SHAPES =
            Set.of("full_block", "stairs", "slab", "wall", "fence", "pane", "building");

    /** True for subcategory IDs that represent a building block shape (not functional/redstone/decorative). */
    private static boolean isBuildingShape(String subId) {
        return BUILDING_SHAPES.contains(subId);
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
