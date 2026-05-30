package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.AmiOntologyKinds;
import com.sanhiruzu.ami.index.GroupingEngine;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.providers.RegistryUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class ResultsTreeBuilder {
    private static final String FALLBACK_GROUP_KEY = "__fallback__";
    private static final int MIN_CATEGORY_KIND_GROUP_SIZE = 8;

    private final ResultsPresentationOptions options;
    private final ResultsSorter sorter;

    ResultsTreeBuilder(ResultsPresentationOptions options, ResultsSorter sorter) {
        this.options = options;
        this.sorter = sorter;
    }

    List<TreeNode> build(List<SearchNode> sorted) {
        return switch (options.groupBy()) {
            case NONE -> sorted.stream()
                    .map(node -> new TreeNode(Component.literal(node.displayName()), node))
                    .collect(Collectors.toList());
            case DIMENSION -> groupByDimension(sorted);
            case MOD -> groupByMod(sorted);
            case CATEGORY -> groupByCategory(sorted);
            case CREATIVE -> groupByCreative(sorted);
            case MATERIAL -> groupByClassifier(sorted,
                    n -> n.meta(SearchNodeKeys.MATERIAL_GROUP, ""),
                    Component.translatable("ami.group.unknown_material"),
                    true,
                    List.of(),
                    true);
            case FAMILY -> groupByClassifier(sorted,
                    this::classifyFamilyRoot,
                    Component.translatable("ami.group.unknown_family"),
                    true,
                    List.of(),
                    true);
            case SHAPE -> groupByClassifier(sorted,
                    n -> n.meta(SearchNodeKeys.VARIANT_GROUP, ""),
                    Component.translatable("ami.group.unknown_shape"),
                    false,
                    GroupingEngine.SHAPE_ORDER,
                    false);
            case TOPOLOGY -> groupByClassifier(sorted,
                    n -> {
                        ItemStack stack = ItemIconRenderer.resolveStack(n.id());
                        return GroupingEngine.classifyTopologyRoot(stack);
                    },
                    Component.translatable("ami.group.unknown_topology"),
                    true,
                    GroupingEngine.TOPOLOGY_ORDER,
                    true);
            case SIMILARITY -> groupByClassifier(sorted,
                    n -> {
                        ItemStack stack = ItemIconRenderer.resolveStack(n.id());
                        return GroupingEngine.classifySimilarityRoot(stack);
                    },
                    Component.translatable("ami.group.unknown_similarity"),
                    true,
                    List.of(),
                    true);
            case PROPERTIES -> groupByClassifier(sorted,
                    GroupingEngine::classifyPropertyRoot,
                    Component.translatable("ami.group.unknown_properties"),
                    true,
                    List.of(),
                    true);
        };
    }

    private List<TreeNode> groupByDimension(List<SearchNode> entries) {
        Map<String, List<SearchNode>> dimGroups = new LinkedHashMap<>();
        for (SearchNode entry : entries) {
            String dim = entry.meta(SearchNodeKeys.DIMENSION, "overworld");
            dimGroups.computeIfAbsent(dim, k -> new ArrayList<>()).add(entry);
        }

        Map<String, List<SearchNode>> sortedGroups = sorter.sortGroups(dimGroups, List.of("overworld", "nether", "end"));
        List<TreeNode> result = new ArrayList<>();

        for (var entry : sortedGroups.entrySet()) {
            String dimKey = entry.getKey();
            Component dimLabel = switch (dimKey) {
                case "nether" -> Component.translatable("ami.dimension.nether");
                case "end" -> Component.translatable("ami.dimension.end");
                default -> Component.translatable("ami.dimension.overworld");
            };
            TreeNode dimNode = new TreeNode(dimKey, dimLabel);
            dimNode.setExpanded(true);

            Map<String, List<SearchNode>> modGroups = new LinkedHashMap<>();
            for (SearchNode node : entry.getValue()) {
                modGroups.computeIfAbsent(node.id().getNamespace(), k -> new ArrayList<>()).add(node);
            }

            for (var modEntry : modGroups.entrySet()) {
                String namespace = modEntry.getKey();
                TreeNode modNode = new TreeNode(namespace, Component.literal(RegistryUtils.modDisplayName(namespace)));
                modNode.setModGroup(true);
                modNode.setExpanded(true);
                addLeaves(modNode, modEntry.getValue());
                dimNode.addChild(modNode);
            }
            result.add(dimNode);
        }
        return result;
    }

    private List<TreeNode> groupByMod(List<SearchNode> entries) {
        Map<String, List<SearchNode>> modGroups = new LinkedHashMap<>();
        for (SearchNode entry : entries) {
            modGroups.computeIfAbsent(entry.id().getNamespace(), k -> new ArrayList<>()).add(entry);
        }

        Map<String, List<SearchNode>> sortedGroups = sorter.sortGroups(modGroups, List.of());
        List<TreeNode> result = new ArrayList<>();

        for (var entry : sortedGroups.entrySet()) {
            String namespace = entry.getKey();
            TreeNode modNode = new TreeNode(namespace, Component.literal(RegistryUtils.modDisplayName(namespace)));
            modNode.setExpanded(true);
            modNode.setModGroup(true);

            Map<NodeType, List<SearchNode>> typeGroups = new LinkedHashMap<>();
            for (SearchNode node : entry.getValue()) {
                typeGroups.computeIfAbsent(node.type(), k -> new ArrayList<>()).add(node);
            }

            for (var typeEntry : typeGroups.entrySet()) {
                NodeType type = typeEntry.getKey();
                TreeNode typeNode = new TreeNode(type.name(), type.displayName());
                typeNode.setExpanded(true);
                addLeaves(typeNode, typeEntry.getValue());
                modNode.addChild(typeNode);
            }
            result.add(modNode);
        }
        return result;
    }

    private List<TreeNode> groupByCategory(List<SearchNode> entries) {
        Map<String, List<SearchNode>> catMap = new LinkedHashMap<>();
        for (SearchNode entry : entries) {
            catMap.computeIfAbsent(AmiOntology.classifyNode(entry).id, k -> new ArrayList<>()).add(entry);
        }

        List<TreeNode> result = new ArrayList<>();
        boolean blocksMaterial = false;

        List<AmiOntology.Category> categoriesToDisplay = new ArrayList<>(AmiOntology.CATEGORIES);
        categoriesToDisplay.sort((a, b) -> a.displayName().getString().compareToIgnoreCase(b.displayName().getString()));
        if (!options.ascending()) {
            Collections.reverse(categoriesToDisplay);
        }

        for (AmiOntology.Category cat : categoriesToDisplay) {
            List<SearchNode> catEntries = catMap.get(cat.id);
            if (catEntries == null || catEntries.isEmpty()) continue;

            TreeNode catNode = new TreeNode(cat.id, cat.displayName());
            catNode.setExpanded(true);

            Map<String, List<SearchNode>> subMap = new LinkedHashMap<>();
            for (SearchNode entry : catEntries) {
                String rawSubId = entry.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
                final String subId;
                if (cat == AmiOntology.MASONRY && blocksMaterial && isBuildingShape(rawSubId)) {
                    String matId = entry.meta(SearchNodeKeys.BLOCKS_MATERIAL, "");
                    subId = matId.isEmpty() ? rawSubId : matId;
                } else {
                    subId = rawSubId;
                }
                subMap.computeIfAbsent(subId, k -> new ArrayList<>()).add(entry);
            }

            Map<String, List<SearchNode>> sortedSubs = sorter.sortGroups(subMap, List.of());
            for (var subEntry : sortedSubs.entrySet()) {
                String subId = subEntry.getKey();
                String subKey = cat.id + "/" + (subId.isEmpty() ? "none" : subId);

                Component subLabel;
                if (subId.isEmpty()) {
                    subLabel = Component.translatable("ami.group.misc");
                } else {
                    subLabel = cat.subCategories.stream()
                            .filter(s -> s.id().equals(subId))
                            .map(AmiOntology.SubCategory::displayName)
                            .findFirst()
                            .orElse(Component.literal(ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(subId, true))));
                }

                TreeNode subNode = new TreeNode(subKey, subLabel);
                subNode.setExpanded(true);
                addCategoryLeaves(cat.id, subId, subNode, subEntry.getValue());
                catNode.addChild(subNode);
            }
            result.add(catNode);
        }
        return result;
    }

    private List<TreeNode> groupByCreative(List<SearchNode> entries) {
        Map<String, List<SearchNode>> creativeGroups = new LinkedHashMap<>();
        Map<String, String> labelsByGroup = new LinkedHashMap<>();
        for (SearchNode entry : entries) {
            String groupId = entry.meta(SearchNodeKeys.CREATIVE_TAB_ID, "");
            if (groupId.isBlank()) {
                groupId = FALLBACK_GROUP_KEY;
            } else {
                labelsByGroup.putIfAbsent(groupId, entry.meta(SearchNodeKeys.CREATIVE_TAB_LABEL, groupId));
            }
            creativeGroups.computeIfAbsent(groupId, k -> new ArrayList<>()).add(entry);
        }

        Map<String, List<SearchNode>> sortedGroups = sorter.sortGroups(creativeGroups, List.of());
        List<TreeNode> result = new ArrayList<>();
        for (var entry : sortedGroups.entrySet()) {
            String groupId = entry.getKey();
            Component label = FALLBACK_GROUP_KEY.equals(groupId)
                    ? Component.translatable("ami.group.unregistered")
                    : Component.literal(labelsByGroup.getOrDefault(groupId, ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(groupId, true))));
            TreeNode groupNode = new TreeNode(groupId, label);
            groupNode.setExpanded(true);
            addLeaves(groupNode, entry.getValue());
            result.add(groupNode);
        }
        return result;
    }

    private List<TreeNode> groupByClassifier(List<SearchNode> entries, Function<SearchNode, String> classifier,
                                             Component fallback, boolean compactResourceIds, List<String> order, boolean mergeLonely) {
        Map<String, List<SearchNode>> rawGroups = new LinkedHashMap<>();
        for (SearchNode entry : entries) {
            rawGroups.computeIfAbsent(classifier.apply(entry), k -> new ArrayList<>()).add(entry);
        }

        Map<String, List<SearchNode>> sortedGroups = sorter.sortGroups(rawGroups, order);
        List<TreeNode> result = new ArrayList<>();
        List<SearchNode> lonelyItems = new ArrayList<>();

        for (var entry : sortedGroups.entrySet()) {
            String val = entry.getKey();
            List<SearchNode> members = entry.getValue();

            if (mergeLonely && members.size() == 1 && !order.contains(val) && !GroupingEngine.isUnknownGroup(val)) {
                lonelyItems.add(members.get(0));
                continue;
            }

            boolean isFallback = GroupingEngine.isUnknownGroup(val);
            String mapKey = isFallback ? FALLBACK_GROUP_KEY : ResultsGroupLabels.formatGroupKey(val, compactResourceIds);

            TreeNode groupNode = new TreeNode(mapKey, isFallback ? fallback : Component.literal(ResultsGroupLabels.formatGroupLabel(mapKey)));
            groupNode.setExpanded(true);
            addLeaves(groupNode, members);
            result.add(groupNode);
        }

        if (!lonelyItems.isEmpty()) {
            TreeNode miscNode = new TreeNode("ami.group.misc", Component.translatable("ami.group.misc"));
            miscNode.setExpanded(true);

            if (lonelyItems.size() > 20) {
                addHierarchicalMiscGroups(miscNode, lonelyItems);
            } else {
                addLeaves(miscNode, lonelyItems);
            }

            int fallbackIdx = -1;
            for (int i = 0; i < result.size(); i++) {
                if (FALLBACK_GROUP_KEY.equals(result.get(i).getKey())) {
                    fallbackIdx = i;
                    break;
                }
            }
            if (fallbackIdx >= 0) {
                result.add(fallbackIdx, miscNode);
            } else {
                result.add(miscNode);
            }
        }

        return result;
    }

    private void addHierarchicalMiscGroups(TreeNode miscNode, List<SearchNode> lonelyItems) {
        Map<String, List<SearchNode>> byCategory = new LinkedHashMap<>();
        for (SearchNode node : lonelyItems) {
            byCategory.computeIfAbsent(AmiOntology.classifyNode(node).id, k -> new ArrayList<>()).add(node);
        }

        for (var catEntry : byCategory.entrySet()) {
            String catId = catEntry.getKey();
            Component label = AmiOntology.CATEGORIES.stream()
                    .filter(c -> c.id.equals(catId))
                    .map(AmiOntology.Category::displayName)
                    .findFirst()
                    .orElse(Component.literal(ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(catId, true))));

            TreeNode catNode = new TreeNode("misc:" + catId, Component.literal("Misc: ").append(label));
            catNode.setExpanded(false);
            addLeaves(catNode, catEntry.getValue());
            miscNode.addChild(catNode);
        }
    }

    private static void addLeaves(TreeNode parent, List<SearchNode> nodes) {
        for (SearchNode node : nodes) {
            parent.addChild(new TreeNode(Component.literal(node.displayName()), node));
        }
    }

    private static void addCategoryLeaves(String categoryId, String subId, TreeNode parent, List<SearchNode> nodes) {
        List<AmiOntologyKinds.Kind> knownKinds = AmiOntologyKinds.kindsFor(categoryId, subId);
        if (knownKinds.isEmpty()) {
            addLeaves(parent, nodes);
            return;
        }

        Map<String, List<SearchNode>> grouped = new LinkedHashMap<>();
        Map<String, AmiOntologyKinds.Kind> kindsById = new LinkedHashMap<>();
        for (AmiOntologyKinds.Kind kind : knownKinds) {
            grouped.put(kind.id(), new ArrayList<>());
            kindsById.put(kind.id(), kind);
        }

        List<SearchNode> directLeaves = new ArrayList<>();
        for (SearchNode node : nodes) {
            var kind = AmiOntologyKinds.classify(node, categoryId, subId);
            if (kind.isPresent()) {
                grouped.computeIfAbsent(kind.get().id(), ignored -> new ArrayList<>()).add(node);
                kindsById.putIfAbsent(kind.get().id(), kind.get());
            } else {
                directLeaves.add(node);
            }
        }

        for (var entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            if (entry.getValue().size() < MIN_CATEGORY_KIND_GROUP_SIZE) {
                directLeaves.addAll(entry.getValue());
                continue;
            }
            AmiOntologyKinds.Kind kind = kindsById.get(entry.getKey());
            TreeNode groupNode = new TreeNode(parent.getKey() + "/" + kind.id(), Component.literal(kind.label()));
            groupNode.setExpanded(true);
            addLeaves(groupNode, entry.getValue());
            parent.addChild(groupNode);
        }

        if (!directLeaves.isEmpty()) {
            directLeaves.sort(Comparator.comparing(SearchNode::displayName, String.CASE_INSENSITIVE_ORDER));
            addLeaves(parent, directLeaves);
        }
    }

    private static boolean isBuildingShape(String subId) {
        return Set.of("full_block", "stairs", "slab", "wall", "fence", "pane", "building").contains(subId);
    }

    private String classifyFamilyRoot(SearchNode node) {
        try {
            ItemStack stack = ItemIconRenderer.resolveStack(node.id());
            return GroupingEngine.classifyFamilyRoot(stack);
        } catch (LinkageError | RuntimeException ignored) {
            String explicitFamily = node.meta(SearchNodeKeys.COLLAPSE_FAMILY, "");
            if (!explicitFamily.isEmpty()) {
                return node.id().getNamespace() + ":" + explicitFamily;
            }

            String material = node.meta(SearchNodeKeys.MATERIAL_GROUP, "");
            if (!material.isEmpty() && !GroupingEngine.isUnknownGroup(material)) {
                return material;
            }

            String subtype = node.meta(SearchNodeKeys.SUBTYPE_OF, "");
            if (!subtype.isEmpty()) {
                return subtype;
            }

            return node.id().toString();
        }
    }
}
