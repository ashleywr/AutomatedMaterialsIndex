package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

public class ResultsProcessor {
    private static final int CARDINALITY_THRESHOLD = 10;

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
        List<TreeNode> tree = buildTree(filtered);
        
        // Final pass: Collapse high-cardinality leaf clusters (e.g. enchanted books)
        return applyHighCardinalityGrouping(tree);
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
            case MATERIAL -> groupByMetadata(sorted, SearchNodeKeys.MATERIAL_GROUP, Component.translatable("ami.group.unknown_material"), true);
            case SHAPE -> groupByMetadata(sorted, SearchNodeKeys.VARIANT_GROUP, Component.translatable("ami.group.unknown_shape"), false);
        };
    }

    private List<TreeNode> groupByDimension(List<SearchNode> entries) {
        Map<String, TreeNode> dimGroups = new LinkedHashMap<>();

        for (SearchNode entry : entries) {
            String dim = entry.meta(SearchNodeKeys.DIMENSION, "overworld");
            String dimKey = switch (dim) {
                case "nether" -> "nether";
                case "end" -> "end";
                default -> "overworld";
            };
            Component dimLabel = switch (dimKey) {
                case "nether" -> Component.translatable("ami.dimension.nether");
                case "end" -> Component.translatable("ami.dimension.end");
                default -> Component.translatable("ami.dimension.overworld");
            };

            TreeNode dimNode = dimGroups.computeIfAbsent(dimKey, k -> {
                TreeNode n = new TreeNode(k, dimLabel);
                n.setExpanded(true);
                return n;
            });
            String ns = entry.id().getNamespace();
            TreeNode modNode = findOrCreateChild(dimNode, ns, Component.literal(ns));
            modNode.setModGroup(true);
            modNode.addChild(new TreeNode(Component.literal(entry.displayName()), entry));
        }

        return new ArrayList<>(dimGroups.values());
    }

    private List<TreeNode> groupByMod(List<SearchNode> entries) {
        Map<String, TreeNode> modGroups = new LinkedHashMap<>();

        for (SearchNode entry : entries) {
            String namespace = entry.id().getNamespace();
            TreeNode modNode = modGroups.computeIfAbsent(namespace, k -> {
                TreeNode n = new TreeNode(k, Component.literal(k));
                n.setExpanded(true);
                n.setModGroup(true);
                return n;
            });
            TreeNode typeNode = findOrCreateChild(modNode, entry.type().displayName().getString(), entry.type().displayName());
            typeNode.addChild(new TreeNode(Component.literal(entry.displayName()), entry));
        }

        return new ArrayList<>(modGroups.values());
    }

    private List<TreeNode> groupByCategory(List<SearchNode> entries) {
        Map<String, TreeNode> catGroups = new LinkedHashMap<>();

        // Pre-insert all category nodes to preserve CATEGORIES order
        for (AmiOntology.Category cat : AmiOntology.CATEGORIES) {
            TreeNode n = new TreeNode(cat.id, cat.displayName);
            n.setExpanded(true);
            catGroups.put(cat.id, n);
        }

        boolean blocksMaterial = AMIConfig.BLOCK_SUBGROUP.get() == AMIConfig.BlockSubgroup.MATERIAL;

        for (SearchNode entry : entries) {
            AmiOntology.Category cat = AmiOntology.classifyNode(entry);
            TreeNode catNode = catGroups.get(cat.id);

            String rawSubId = entry.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");

            // For building-type block shapes, swap to material grouping when config says so.
            // Structural subcategories (functional/redstone/decorative) are never swapped.
            final String subId;
            if (cat == AmiOntology.BLOCKS && blocksMaterial && isBuildingShape(rawSubId)) {
                String matId = entry.meta(SearchNodeKeys.BLOCKS_MATERIAL, "");
                subId = matId.isEmpty() ? rawSubId : matId;
            } else {
                subId = rawSubId;
            }

            if (!subId.isEmpty()) {
                Component subLabel = cat.subCategories.stream()
                        .filter(s -> s.id().equals(subId))
                        .map(AmiOntology.SubCategory::displayName)
                        .findFirst()
                        .orElse(Component.literal(subId));
                TreeNode subNode = findOrCreateChild(catNode, subId, subLabel);
                subNode.addChild(new TreeNode(Component.literal(entry.displayName()), entry));
            } else {
                TreeNode miscNode = findOrCreateChild(catNode, "misc", Component.translatable("ami.group.misc"));
                miscNode.addChild(new TreeNode(Component.literal(entry.displayName()), entry));
            }
        }

        // Only include categories that actually have children
        return catGroups.values().stream()
                .filter(n -> !n.getChildren().isEmpty())
                .collect(Collectors.toList());
    }

    private List<TreeNode> groupByMetadata(List<SearchNode> entries, String metadataKey, Component fallback, boolean compactResourceIds) {
        Map<String, TreeNode> groups = new LinkedHashMap<>();

        for (SearchNode entry : entries) {
            String groupValue = entry.meta(metadataKey, "");
            String key = groupValue.isBlank() ? "" : formatGroupKey(groupValue, compactResourceIds);
            Component label = key.isEmpty() ? fallback : Component.literal(formatGroupLabel(key));
            TreeNode groupNode = groups.computeIfAbsent(key.isEmpty() ? "__fallback__" : key, k -> {
                TreeNode n = new TreeNode(k, label);
                n.setExpanded(true);
                return n;
            });
            groupNode.addChild(new TreeNode(Component.literal(entry.displayName()), entry));
        }

        return new ArrayList<>(groups.values());
    }

    private String formatGroupKey(String value, boolean compactResourceIds) {
        String key = value;
        if (compactResourceIds) {
            int sep = key.indexOf(':');
            if (sep >= 0 && sep + 1 < key.length()) key = key.substring(sep + 1);
        }
        return key.replace('_', ' ').trim();
    }

    private String formatGroupLabel(String key) {
        String[] words = key.split("\\s+");
        StringBuilder out = new StringBuilder(key.length());
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
    }

    private TreeNode findOrCreateChild(TreeNode parent, String key, Component label) {
        for (TreeNode child : parent.getChildren()) {
            if (!child.isLeaf() && child.getKey().equals(key)) {
                return child;
            }
        }
        TreeNode newChild = new TreeNode(key, label);
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

    /**
     * Identifies adjacent leaf nodes that share the same base item (e.g. enchanted books)
     * and collapses them into a special high-cardinality group if they exceed the threshold.
     */
    private List<TreeNode> applyHighCardinalityGrouping(List<TreeNode> nodes) {
        List<TreeNode> result = new ArrayList<>();
        
        List<TreeNode> buffer = new ArrayList<>();
        String bufferBaseId = null;

        for (TreeNode node : nodes) {
            if (!node.isLeaf()) {
                // Recursively apply to children of existing groups
                flushCardinalityBuffer(buffer, result);
                bufferBaseId = null;
                
                TreeNode processedGroup = new TreeNode(node.getKey(), node.getLabel());
                processedGroup.setExpanded(node.isExpanded());
                processedGroup.setModGroup(node.isModGroup());
                processedGroup.getChildren().addAll(applyHighCardinalityGrouping(node.getChildren()));
                result.add(processedGroup);
                continue;
            }

            // Check if this leaf belongs to the current buffer group
            String baseId = node.getEntry().meta(SearchNodeKeys.SUBTYPE_OF, "");
            if (baseId.isEmpty()) {
                // Not a subtype node, treat as unique
                flushCardinalityBuffer(buffer, result);
                result.add(node);
                bufferBaseId = null;
            } else if (baseId.equals(bufferBaseId)) {
                // Same base item, add to buffer
                buffer.add(node);
            } else {
                // New subtype group
                flushCardinalityBuffer(buffer, result);
                buffer.add(node);
                bufferBaseId = baseId;
            }
        }
        flushCardinalityBuffer(buffer, result);
        
        return result;
    }

    private void flushCardinalityBuffer(List<TreeNode> buffer, List<TreeNode> result) {
        if (buffer.isEmpty()) return;

        if (buffer.size() >= CARDINALITY_THRESHOLD) {
            // Collapse into a high-cardinality group
            SearchNode representative = buffer.get(0).getEntry();
            String baseId = representative.meta(SearchNodeKeys.SUBTYPE_OF);
            
            // Try to find a nice name: "Enchanted Books" instead of "minecraft:enchanted_book"
            String label = buffer.get(0).getLabel().getString();
            if (label.contains("(")) {
                label = label.substring(0, label.indexOf('(')).trim();
            } else if (label.contains(" - ")) {
                label = label.substring(0, label.indexOf(" - ")).trim();
            } else if (label.contains(":")) {
                // Fallback to registry-like name if it's too technical
            }

            TreeNode group = new TreeNode("cardinality:" + baseId, Component.literal(label));
            group.setHighCardinality(true);
            group.setExpanded(false); // Closed by default as requested
            group.getChildren().addAll(buffer);
            result.add(group);
        } else {
            // Just add them as normal leaves
            result.addAll(buffer);
        }
        buffer.clear();
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
