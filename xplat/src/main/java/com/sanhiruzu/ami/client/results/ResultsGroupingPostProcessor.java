package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GroupingEngine;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Applies cross-cutting grouping passes after a result tree has been built.
 */
final class ResultsGroupingPostProcessor {
    private static final int CARDINALITY_THRESHOLD = 4;
    private static final int EXPLICIT_FAMILY_THRESHOLD = 4;
    private static final int COLOR_GROUP_MIN = 3;
    private static final int CATEGORY_SHAPE_MATERIAL_GROUP_MIN = 2;
    private static final int DUPLICATE_LABEL_THRESHOLD = 4;
    private static final Map<String, String> CATEGORY_SHAPE_MATERIAL_LABELS = Map.of(
            "slab", "Slabs",
            "stairs", "Stairs",
            "wall", "Walls"
    );
    private static final Set<String> CATEGORY_CARDINALITY_BASE_PATHS = Set.of(
            "candle",
            "mushroom",
            "pottery_sherd"
    );
    private static final Set<String> CATEGORY_CARDINALITY_FAMILY_KEYS = Set.of(
            "banner_patterns",
            "banners",
            "goat_horns",
            "music_discs"
    );

    private ResultsGroupingPostProcessor() {
    }

    static List<TreeNode> applyToTree(List<TreeNode> tree, ResultsProcessor.GroupBy groupBy) {
        return switch (groupBy) {
            case NONE -> tree;
            case CATEGORY -> applyDefaultCollapsedFamilyGrouping(
                    applyCategoryShapeMaterialGrouping(applyCategoryHighCardinalityGrouping(tree)),
                    true
            );
            case MATERIAL -> applyDefaultCollapsedFamilyGrouping(applyMaterialGroupingPasses(tree), true);
            case FAMILY -> applyFamilyGroupingPasses(tree);
            case SHAPE -> applyDefaultCollapsedFamilyGrouping(applyHighCardinalityGrouping(tree), true);
            case MOD, CREATIVE, DIMENSION, BEHAVIOR, TOPOLOGY, SIMILARITY, PROPERTIES ->
                    applyDefaultCollapsedFamilyGrouping(applyHighCardinalityGrouping(tree), true);
        };
    }

    static List<TreeNode> applyToFlatCards(List<TreeNode> flat) {
        return applyDuplicateLabelGrouping(applyExplicitFamilyGrouping(applyHighCardinalityGrouping(flat), true), true);
    }

    private static List<TreeNode> applyMaterialGroupingPasses(List<TreeNode> tree) {
        List<TreeNode> processed = applyHighCardinalityGrouping(tree);
        if (AmiConfig.enableMaterialRootUI) {
            processed = applyColorGrouping(processed);
        }
        return processed;
    }

    private static List<TreeNode> applyFamilyGroupingPasses(List<TreeNode> tree) {
        return applyDuplicateLabelGrouping(applyExplicitFamilyGrouping(applyHighCardinalityGrouping(tree), false), false);
    }

    private static List<TreeNode> applyDefaultCollapsedFamilyGrouping(List<TreeNode> nodes, boolean compactCards) {
        return applyExplicitFamilyGrouping(nodes, compactCards, true);
    }

    /**
     * Collapses leaves that share the same material root and each carry a color bucket.
     */
    private static List<TreeNode> applyColorGrouping(List<TreeNode> nodes) {
        List<TreeNode> processed = new ArrayList<>();
        for (TreeNode node : nodes) {
            if (!node.isLeaf()) {
                if (isIntentionalLeafFamily(node)) {
                    processed.add(node);
                    continue;
                }
                TreeNode copy = copyGroupNode(node);
                copy.getChildren().addAll(applyColorGrouping(node.getChildren()));
                processed.add(copy);
            } else {
                processed.add(node);
            }
        }

        Map<String, List<TreeNode>> byMaterial = new LinkedHashMap<>();
        for (TreeNode node : processed) {
            if (!node.isLeaf()) continue;
            String color = node.getEntry().meta(SearchNodeKeys.COLOR_BUCKET, "");
            if (color.isEmpty()) continue;
            String material = node.getEntry().meta(SearchNodeKeys.MATERIAL_GROUP, "");
            if (material.isEmpty() || GroupingEngine.isUnknownGroup(material)) continue;
            byMaterial.computeIfAbsent(material, k -> new ArrayList<>()).add(node);
        }

        Map<TreeNode, TreeNode> replacements = new IdentityHashMap<>();
        for (var entry : byMaterial.entrySet()) {
            List<TreeNode> members = entry.getValue();
            if (members.size() < COLOR_GROUP_MIN) continue;
            String material = entry.getKey();
            TreeNode group = new TreeNode("color_group:" + material, Component.literal(ResultsGroupLabels.colorGroupLabel(material)));
            group.setHighCardinality(true);
            group.setExpanded(true);
            group.getChildren().addAll(members);
            for (TreeNode member : members) {
                replacements.put(member, group);
            }
        }

        if (replacements.isEmpty()) return processed;

        List<TreeNode> result = new ArrayList<>();
        Set<TreeNode> emitted = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TreeNode node : processed) {
            TreeNode replacement = replacements.get(node);
            if (replacement == null) {
                result.add(node);
            } else if (emitted.add(replacement)) {
                result.add(replacement);
            }
        }
        return result;
    }

    private static List<TreeNode> applyHighCardinalityGrouping(List<TreeNode> nodes) {
        return applyHighCardinalityGrouping(nodes, (node, baseId) -> true);
    }

    private static List<TreeNode> applyCategoryHighCardinalityGrouping(List<TreeNode> nodes) {
        return applyHighCardinalityGrouping(nodes, ResultsGroupingPostProcessor::isCategoryCardinalityNode);
    }

    private static List<TreeNode> applyCategoryShapeMaterialGrouping(List<TreeNode> nodes) {
        List<TreeNode> result = new ArrayList<>();
        for (TreeNode node : nodes) {
            if (node.isLeaf() || isIntentionalLeafFamily(node)) {
                result.add(node);
                continue;
            }

            TreeNode copy = copyGroupNode(node);
            List<TreeNode> children = applyCategoryShapeMaterialGrouping(node.getChildren());
            String shapeId = categoryShapeMaterialGroupId(node);
            copy.getChildren().addAll(shapeId.isEmpty()
                    ? children
                    : groupCategoryShapeMaterialLeaves(children, shapeId));
            result.add(copy);
        }
        return result;
    }

    private static String categoryShapeMaterialGroupId(TreeNode node) {
        String key = node.getKey();
        if (!key.startsWith("masonry/")) {
            return "";
        }
        String subcategory = key.substring("masonry/".length());
        return CATEGORY_SHAPE_MATERIAL_LABELS.containsKey(subcategory) ? subcategory : "";
    }

    private static List<TreeNode> groupCategoryShapeMaterialLeaves(List<TreeNode> nodes, String shapeId) {
        Map<String, List<TreeNode>> membersByMaterial = new LinkedHashMap<>();
        for (TreeNode node : nodes) {
            if (!node.isLeaf()) continue;
            String material = categoryShapeMaterialKey(node);
            if (!material.isEmpty()) {
                membersByMaterial.computeIfAbsent(material, ignored -> new ArrayList<>()).add(node);
            }
        }

        Map<TreeNode, TreeNode> replacements = new IdentityHashMap<>();
        for (var entry : membersByMaterial.entrySet()) {
            List<TreeNode> members = entry.getValue();
            if (members.size() < CATEGORY_SHAPE_MATERIAL_GROUP_MIN) continue;
            TreeNode group = categoryShapeMaterialGroup(shapeId, entry.getKey(), members);
            for (TreeNode member : members) {
                replacements.put(member, group);
            }
        }

        if (replacements.isEmpty()) {
            return nodes;
        }

        List<TreeNode> result = new ArrayList<>();
        Set<TreeNode> emittedGroups = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TreeNode node : nodes) {
            TreeNode replacement = replacements.get(node);
            if (replacement == null) {
                result.add(node);
                continue;
            }
            if (emittedGroups.add(replacement)) {
                result.add(replacement);
            }
        }
        return result;
    }

    private static String categoryShapeMaterialKey(TreeNode node) {
        if (!node.getEntry().meta(SearchNodeKeys.COLLAPSE_FAMILY, "").isEmpty()) {
            return "";
        }
        String material = node.getEntry().meta(SearchNodeKeys.SUBTYPE_OF, "");
        if (material.isEmpty()) {
            material = node.getEntry().meta(SearchNodeKeys.MATERIAL_GROUP, "");
        }
        return material.isEmpty() || GroupingEngine.isUnknownGroup(material) ? "" : material;
    }

    private static TreeNode categoryShapeMaterialGroup(String shapeId, String material, List<TreeNode> members) {
        TreeNode group = new TreeNode(
                "category_shape_material:" + shapeId + ":" + material,
                Component.literal(categoryShapeMaterialLabel(shapeId, material))
        );
        group.setHighCardinality(true);
        group.setExpanded(true);
        group.getChildren().addAll(members);
        return group;
    }

    private static String categoryShapeMaterialLabel(String shapeId, String material) {
        ResourceLocation loc = ResourceLocation.tryParse(material);
        String path = loc == null ? material : loc.getPath();
        String base = stripShapeSuffix(path, shapeId).replace('_', ' ').replace('/', ' ');
        String label = ResultsGroupLabels.formatGroupLabel(base);
        String shapeLabel = CATEGORY_SHAPE_MATERIAL_LABELS.getOrDefault(shapeId, ResultsGroupLabels.formatGroupLabel(shapeId));
        if (label.isBlank()) {
            return shapeLabel;
        }
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.endsWith(" " + shapeLabel.toLowerCase(Locale.ROOT))) {
            return label;
        }
        return label + " " + shapeLabel;
    }

    private static String stripShapeSuffix(String path, String shapeId) {
        return switch (shapeId) {
            case "slab" -> stripAnySuffix(path, "_slabs", "_slab", "/slabs", "/slab");
            case "stairs" -> stripAnySuffix(path, "_stairs", "_stair", "/stairs", "/stair");
            case "wall" -> stripAnySuffix(path, "_walls", "_wall", "/walls", "/wall");
            default -> path;
        };
    }

    private static String stripAnySuffix(String value, String... suffixes) {
        for (String suffix : suffixes) {
            if (value.endsWith(suffix)) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return value;
    }

    private static List<TreeNode> applyHighCardinalityGrouping(List<TreeNode> nodes, BiPredicate<TreeNode, String> baseIdFilter) {
        List<TreeNode> recursive = new ArrayList<>();
        for (TreeNode node : nodes) {
            if (node.isLeaf()) {
                recursive.add(node);
                continue;
            }

            List<TreeNode> children = applyHighCardinalityGrouping(node.getChildren(), baseIdFilter);
            if (!children.isEmpty()) {
                TreeNode processedGroup = copyGroupNode(node);
                processedGroup.getChildren().addAll(children);
                recursive.add(processedGroup);
            }
        }

        Set<String> variantBaseIds = variantBaseIds(nodes);
        Map<String, List<TreeNode>> membersByBaseId = new LinkedHashMap<>();
        for (TreeNode node : recursive) {
            if (!node.isLeaf()) continue;

            String baseId = highCardinalityBaseId(node, variantBaseIds);
            boolean hasExplicitFamily = !node.getEntry().meta(SearchNodeKeys.COLLAPSE_FAMILY, "").isEmpty();
            if (!baseId.isEmpty() && !hasExplicitFamily && baseIdFilter.test(node, baseId)) {
                membersByBaseId.computeIfAbsent(baseId, ignored -> new ArrayList<>()).add(node);
            }
        }

        Map<TreeNode, TreeNode> replacementGroups = new IdentityHashMap<>();
        for (var entry : membersByBaseId.entrySet()) {
            List<TreeNode> members = entry.getValue();
            if (members.size() < CARDINALITY_THRESHOLD) continue;
            TreeNode group = cardinalityGroup(entry.getKey(), members);
            for (TreeNode member : members) {
                replacementGroups.put(member, group);
            }
        }

        if (replacementGroups.isEmpty()) {
            return recursive;
        }

        List<TreeNode> result = new ArrayList<>();
        Set<TreeNode> emittedGroups = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TreeNode node : recursive) {
            TreeNode replacement = replacementGroups.get(node);
            if (replacement == null) {
                result.add(node);
                continue;
            }
            if (emittedGroups.add(replacement)) {
                result.add(replacement);
            }
        }
        return result;
    }

    private static Set<String> variantBaseIds(List<TreeNode> nodes) {
        Set<String> result = new HashSet<>();
        for (TreeNode node : nodes) {
            if (node.isLeaf()) {
                String baseId = node.getEntry().meta(SearchNodeKeys.SUBTYPE_OF, "");
                if (!baseId.isEmpty()) {
                    result.add(baseId);
                }
            }
        }
        return result;
    }

    private static String highCardinalityBaseId(TreeNode node, Set<String> variantBaseIds) {
        String baseId = node.getEntry().meta(SearchNodeKeys.SUBTYPE_OF, "");
        if (!baseId.isEmpty()) {
            return baseId;
        }
        String nodeId = node.getEntry().id().toString();
        return variantBaseIds.contains(nodeId) ? nodeId : "";
    }

    private static boolean isCategoryCardinalityBaseId(String baseId) {
        ResourceLocation loc = ResourceLocation.tryParse(baseId);
        String path = loc == null ? baseId : loc.getPath();
        return CATEGORY_CARDINALITY_BASE_PATHS.contains(path);
    }

    private static boolean isCategoryCardinalityNode(TreeNode node, String baseId) {
        if (isCategoryCardinalityBaseId(baseId)) {
            return true;
        }
        if (!node.isLeaf()) {
            return false;
        }
        String mode = node.getEntry().meta(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "");
        return "auto".equals(mode) || "default_collapsed".equals(mode);
    }

    private static List<TreeNode> applyExplicitFamilyGrouping(List<TreeNode> nodes, boolean compactCards) {
        return applyExplicitFamilyGrouping(nodes, compactCards, false);
    }

    private static List<TreeNode> applyExplicitFamilyGrouping(List<TreeNode> nodes, boolean compactCards,
                                                             boolean requireDefaultCollapsed) {
        List<TreeNode> recursive = new ArrayList<>();
        for (TreeNode node : nodes) {
            if (node.isLeaf()) {
                recursive.add(node);
                continue;
            }

            TreeNode processedGroup = copyGroupNode(node);
            processedGroup.getChildren().addAll(applyExplicitFamilyGrouping(
                    node.getChildren(),
                    compactCards,
                    requireDefaultCollapsed
            ));
            recursive.add(processedGroup);
        }

        Map<String, List<TreeNode>> familyMembers = new LinkedHashMap<>();
        Map<String, String> familyLabels = new HashMap<>();
        Set<String> categoryCollapsedFamilyKeys = new HashSet<>();
        for (TreeNode node : recursive) {
            if (!node.isLeaf()) continue;
            String familyKey = explicitFamilyKey(node);
            if (familyKey.isEmpty()) continue;
            if (requireDefaultCollapsed && !isDefaultCollapsedFamilyMember(node)) continue;
            familyMembers.computeIfAbsent(familyKey, ignored -> new ArrayList<>()).add(node);
            familyLabels.putIfAbsent(familyKey, explicitFamilyLabel(node, familyKey));
            categoryCollapsedFamilyKeys.add(familyKey);
        }

        if (requireDefaultCollapsed && !categoryCollapsedFamilyKeys.isEmpty()) {
            for (TreeNode node : recursive) {
                if (!node.isLeaf()) continue;
                String nodeId = node.getEntry().id().toString();
                if (!categoryCollapsedFamilyKeys.contains(nodeId)) continue;
                if (!explicitFamilyKey(node).isEmpty()) continue;
                familyMembers.computeIfAbsent(nodeId, ignored -> new ArrayList<>()).add(node);
            }
        }

        Map<TreeNode, TreeNode> replacementGroups = new IdentityHashMap<>();
        for (var entry : familyMembers.entrySet()) {
            if (entry.getValue().size() < explicitFamilyThreshold(entry.getValue())) continue;
            String familyKey = entry.getKey();
            String label = familyLabels.getOrDefault(familyKey,
                    ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(familyKey, false)));
            TreeNode group = new TreeNode("cardinality:family:" + familyKey, Component.literal(label));
            group.setHighCardinality(compactCards);
            group.setExpanded(true);
            group.getChildren().addAll(sortedFamilyMembers(familyKey, entry.getValue()));
            for (TreeNode member : entry.getValue()) {
                replacementGroups.put(member, group);
            }
        }

        if (replacementGroups.isEmpty()) {
            return recursive;
        }

        List<TreeNode> result = new ArrayList<>();
        Set<TreeNode> emittedGroups = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TreeNode node : recursive) {
            TreeNode replacement = replacementGroups.get(node);
            if (replacement == null) {
                result.add(node);
                continue;
            }
            if (emittedGroups.add(replacement)) {
                result.add(replacement);
            }
        }
        return result;
    }

    private static List<TreeNode> sortedFamilyMembers(String familyKey, List<TreeNode> members) {
        List<TreeNode> sorted = new ArrayList<>(members);
        sorted.sort(Comparator
                .comparing((TreeNode node) -> !node.getEntry().id().toString().equals(familyKey))
                .thenComparing(node -> node.getLabel().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(node -> node.getEntry().id().toString()));
        return sorted;
    }

    private static int explicitFamilyThreshold(List<TreeNode> members) {
        if (members.size() >= 2 && members.stream().allMatch(ResultsGroupingPostProcessor::hasExplicitDefaultCollapsedMode)) {
            return 2;
        }
        return EXPLICIT_FAMILY_THRESHOLD;
    }

    private static boolean hasExplicitDefaultCollapsedMode(TreeNode node) {
        return "default_collapsed".equals(node.getEntry().meta(SearchNodeKeys.VARIANT_COLLAPSE_MODE, ""));
    }

    private static boolean isDefaultCollapsedFamilyMember(TreeNode node) {
        String mode = node.getEntry().meta(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "");
        if ("default_collapsed".equals(mode)) {
            return true;
        }
        return CATEGORY_CARDINALITY_FAMILY_KEYS.contains(explicitFamilyKey(node));
    }

    private static String explicitFamilyKey(TreeNode node) {
        if (!node.isLeaf()) {
            return "";
        }
        String familyKey = node.getEntry().meta(SearchNodeKeys.COLLAPSE_FAMILY, "");
        if (!familyKey.isEmpty()) {
            return familyKey;
        }
        String path = node.getEntry().id().getPath();
        if (path.endsWith("_banner_pattern")) {
            return "banner_patterns";
        }
        if (path.startsWith("music_disc_")) {
            return "music_discs";
        }
        return "";
    }

    private static String explicitFamilyLabel(TreeNode node, String familyKey) {
        String label = node.getEntry().meta(SearchNodeKeys.COLLAPSE_LABEL, "");
        if (!label.isEmpty()) {
            return label;
        }
        return switch (familyKey) {
            case "banner_patterns" -> "Banner Patterns";
            case "music_discs" -> "Music Discs";
            default -> "";
        };
    }

    private static List<TreeNode> applyDuplicateLabelGrouping(List<TreeNode> nodes, boolean compactCards) {
        List<TreeNode> recursive = new ArrayList<>();
        for (TreeNode node : nodes) {
            if (node.isLeaf() || isIntentionalLeafFamily(node)) {
                recursive.add(node);
                continue;
            }

            TreeNode processedGroup = copyGroupNode(node);
            processedGroup.getChildren().addAll(applyDuplicateLabelGrouping(node.getChildren(), compactCards));
            recursive.add(processedGroup);
        }

        Map<String, List<TreeNode>> membersByLabel = new LinkedHashMap<>();
        for (TreeNode node : recursive) {
            if (!node.isLeaf()) continue;
            String label = node.getLabel().getString().trim();
            if (label.isEmpty()) continue;
            membersByLabel.computeIfAbsent(label, ignored -> new ArrayList<>()).add(node);
        }

        Map<TreeNode, TreeNode> replacementGroups = new IdentityHashMap<>();
        for (var entry : membersByLabel.entrySet()) {
            List<TreeNode> members = entry.getValue();
            if (members.size() < DUPLICATE_LABEL_THRESHOLD) continue;
            TreeNode group = duplicateLabelGroup(entry.getKey(), members, compactCards);
            for (TreeNode member : members) {
                replacementGroups.put(member, group);
            }
        }

        if (replacementGroups.isEmpty()) {
            return recursive;
        }

        List<TreeNode> result = new ArrayList<>();
        Set<TreeNode> emittedGroups = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TreeNode node : recursive) {
            TreeNode replacement = replacementGroups.get(node);
            if (replacement == null) {
                result.add(node);
                continue;
            }
            if (emittedGroups.add(replacement)) {
                result.add(replacement);
            }
        }
        return result;
    }

    private static TreeNode cardinalityGroup(String baseId, List<TreeNode> members) {
        List<TreeNode> children = new ArrayList<>(members);
        children.sort((a, b) -> Boolean.compare(!a.getEntry().id().toString().equals(baseId), !b.getEntry().id().toString().equals(baseId)));

        String label;
        ResourceLocation loc = ResourceLocation.tryParse(baseId);
        if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
            label = ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(loc.getPath(), false));
            if (!label.endsWith("s")) label += "s";
        } else if (loc != null) {
            label = ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(loc.getPath(), false));
        } else {
            label = members.get(0).getLabel().getString();
            if (label.contains("(")) label = label.substring(0, label.indexOf('(')).trim();
            else if (label.contains(" - ")) label = label.substring(0, label.indexOf(" - ")).trim();
        }

        TreeNode group = new TreeNode("cardinality:" + baseId, Component.literal(label));
        group.setHighCardinality(true);
        group.setExpanded(true);
        group.getChildren().addAll(children);
        return group;
    }

    private static TreeNode duplicateLabelGroup(String label, List<TreeNode> members, boolean compactCards) {
        TreeNode group = new TreeNode("duplicate_label:" + label.toLowerCase(Locale.ROOT).replace(' ', '_'),
                Component.literal(pluralize(label)));
        group.setHighCardinality(compactCards);
        group.setExpanded(true);
        group.getChildren().addAll(members);
        return group;
    }

    private static String pluralize(String label) {
        if (label.endsWith("s")) return label;
        if (label.endsWith("y") && label.length() > 1) {
            char beforeY = Character.toLowerCase(label.charAt(label.length() - 2));
            if ("aeiou".indexOf(beforeY) < 0) {
                return label.substring(0, label.length() - 1) + "ies";
            }
        }
        return label + "s";
    }

    private static boolean isIntentionalLeafFamily(TreeNode node) {
        return !node.isLeaf()
                && (node.getKey().startsWith("cardinality:family:")
                || node.isHighCardinality());
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
