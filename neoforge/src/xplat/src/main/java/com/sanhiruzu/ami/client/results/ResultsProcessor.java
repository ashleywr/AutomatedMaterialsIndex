package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.GroupingEngine;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class ResultsProcessor {
    private static final int CARDINALITY_THRESHOLD = 10;
    private static final int EXPLICIT_FAMILY_THRESHOLD = 4;
    private static final String FALLBACK_GROUP_KEY = "__fallback__";

    public enum SortField {
        ALPHABETICAL("ami.sort.alphabetical"),
        COLOR("ami.sort.color"),
        MOD("ami.sort.mod"),
        STORAGE_CAPACITY("ami.sort.storage"),
        DPS("ami.sort.dps"),
        COUNT("ami.sort.count");

        public final Component displayName;

        SortField(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum GroupBy {
        NONE("ami.group.none"),
        DIMENSION("ami.group.dimension"),
        MOD("ami.group.mod"),
        CATEGORY("ami.group.category"),
        CREATIVE("ami.group.creative"),
        MATERIAL("ami.group.material"),
        FAMILY("ami.group.family"),
        SHAPE("ami.group.shape"),
        TOPOLOGY("ami.group.topology"),
        SIMILARITY("ami.group.similarity"),
        PROPERTIES("ami.group.properties");

        public final Component displayName;

        GroupBy(String key) {
            this.displayName = Component.translatable(key);
        }
    }


    private final SortField sortField;
    private final boolean ascending;
    private final GroupBy groupBy;
    private final Set<String> selectedMods;
    private final Set<String> activeFacets;

    public ResultsProcessor(SortField sortField, boolean ascending, GroupBy groupBy,
                            Set<String> selectedMods, Set<String> activeFacets) {
        this.sortField = sortField;
        this.ascending = ascending;
        this.groupBy = groupBy;
        this.selectedMods = selectedMods != null ? selectedMods : new HashSet<>();
        this.activeFacets = activeFacets != null ? activeFacets : new HashSet<>();
    }

    public List<TreeNode> process(List<SearchNode> results) {
        // Handle background indexing state
        if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
            return List.of(createIndexingNode());
        }

        List<SearchNode> filtered = filterAndSort(results);

        if (groupBy == GroupBy.NONE) {
            List<TreeNode> leaves = filtered.stream()
                    .map(node -> new TreeNode(Component.literal(node.displayName()), node))
                    .collect(Collectors.toList());
            return applyExplicitFamilyGrouping(applyHighCardinalityGrouping(leaves));
        }

        // Group
        List<TreeNode> tree = buildTree(filtered);

        return applyExplicitFamilyGrouping(applyHighCardinalityGrouping(tree));
    }

    public List<TreeNode> processFlat(List<SearchNode> results) {
        if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
            return List.of(createIndexingNode());
        }

        return filterAndSort(results).stream()
                .map(node -> new TreeNode(Component.literal(node.displayName()), node))
                .collect(Collectors.toList());
    }

    private TreeNode createIndexingNode() {
        return new TreeNode("indexing", Component.translatable("ami.gui.background_indexing")
                .withStyle(s -> s.withColor(com.sanhiruzu.ami.client.AMITheme.CHEAT_INDICATOR)));
    }

    private List<SearchNode> filterAndSort(List<SearchNode> results) {
        List<SearchNode> filtered = results.stream()
                .filter(n -> selectedMods.isEmpty() || selectedMods.contains(n.id().getNamespace()))
                .filter(this::matchesFacets)
                .filter(this::matchesAccessLevel)
                .filter(this::matchesVisibility)
                .collect(Collectors.toList());

        filtered.sort(this::compareNodes);
        if (!ascending) {
            Collections.reverse(filtered);
        }
        return filtered;
    }

    private boolean matchesVisibility(SearchNode node) {
        if (com.sanhiruzu.ami.config.AmiConfig.devMode) return true;
        return !"hidden".equals(node.meta(SearchNodeKeys.VISIBILITY, ""));
    }

    private int compareNodes(SearchNode a, SearchNode b) {
        return switch (sortField) {
            case ALPHABETICAL, COUNT -> a.displayName().compareTo(b.displayName());
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
            case NONE -> sorted.stream().map(node -> new TreeNode(Component.literal(node.displayName()), node)).collect(Collectors.toList());
            case DIMENSION -> groupByDimension(sorted);
            case MOD -> groupByMod(sorted);
            case CATEGORY -> groupByCategory(sorted);
            case CREATIVE -> groupByCreative(sorted);
            case MATERIAL ->
                    groupByClassifier(sorted, n -> n.meta(SearchNodeKeys.MATERIAL_GROUP, ""), Component.translatable("ami.group.unknown_material"), true, List.of(), true);
            case FAMILY -> groupByClassifier(sorted, n -> {
                ItemStack stack = ItemIconRenderer.resolveStack(n.id());
                return GroupingEngine.classifyFamilyRoot(stack);
            }, Component.translatable("ami.group.unknown_family"), true, List.of(), true);
            case SHAPE ->
                    groupByClassifier(sorted, n -> n.meta(SearchNodeKeys.VARIANT_GROUP, ""), Component.translatable("ami.group.unknown_shape"), false, GroupingEngine.SHAPE_ORDER, false);
            case TOPOLOGY -> groupByClassifier(sorted, n -> {
                ItemStack stack = ItemIconRenderer.resolveStack(n.id());
                return GroupingEngine.classifyTopologyRoot(stack);
            }, Component.translatable("ami.group.unknown_topology"), true, GroupingEngine.TOPOLOGY_ORDER, true);
            case SIMILARITY -> groupByClassifier(sorted, n -> {
                ItemStack stack = ItemIconRenderer.resolveStack(n.id());
                return GroupingEngine.classifySimilarityRoot(stack);
            }, Component.translatable("ami.group.unknown_similarity"), true, List.of(), true);
            case PROPERTIES -> groupByClassifier(sorted, n -> {
                return GroupingEngine.classifyPropertyRoot(n);
            }, Component.translatable("ami.group.unknown_properties"), true, List.of(), true);
        };
    }

    private Map<String, List<SearchNode>> sortGroupsLocally(Map<String, List<SearchNode>> groups, List<String> order) {
        if (sortField != SortField.COUNT) {
            Map<String, Integer> orderMap = new HashMap<>();
            for (int i = 0; i < order.size(); i++) {
                orderMap.put(order.get(i), i);
            }

            List<Map.Entry<String, List<SearchNode>>> entries = new ArrayList<>(groups.entrySet());
            entries.sort((a, b) -> {
                String k1 = a.getKey();
                String k2 = b.getKey();
                Integer i1 = orderMap.get(k1);
                Integer i2 = orderMap.get(k2);
                int cmp;
                if (i1 != null && i2 != null) cmp = Integer.compare(i1, i2);
                else if (i1 != null) cmp = -1;
                else if (i2 != null) cmp = 1;
                else {
                    boolean u1 = GroupingEngine.isUnknownGroup(k1);
                    boolean u2 = GroupingEngine.isUnknownGroup(k2);
                    if (u1 && !u2) cmp = 1;
                    else if (!u1 && u2) cmp = -1;
                    else cmp = k1.compareTo(k2);
                }
                return ascending ? cmp : -cmp;
            });

            Map<String, List<SearchNode>> result = new LinkedHashMap<>();
            for (var entry : entries) result.put(entry.getKey(), entry.getValue());
            return result;
        }

        List<Map.Entry<String, List<SearchNode>>> entries = new ArrayList<>(groups.entrySet());
        entries.sort((a, b) -> {
            String k1 = a.getKey();
            String k2 = b.getKey();

            // Unknowns/Fallbacks always to the bottom
            boolean u1 = GroupingEngine.isUnknownGroup(k1);
            boolean u2 = GroupingEngine.isUnknownGroup(k2);
            if (u1 && !u2) return 1;
            if (!u1 && u2) return -1;

            // Sort by count
            int cmp = Integer.compare(a.getValue().size(), b.getValue().size());
            if (!ascending) {
                cmp = -cmp; // Reverse size comparison if descending is selected
            }
            if (cmp == 0) {
                return k1.compareTo(k2); // Fallback to alphabetical if counts are identical
            }
            return cmp;
        });

        Map<String, List<SearchNode>> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private List<TreeNode> groupByDimension(List<SearchNode> entries) {
        Map<String, List<SearchNode>> dimGroups = new LinkedHashMap<>();
        for (SearchNode entry : entries) {
            String dim = entry.meta(SearchNodeKeys.DIMENSION, "overworld");
            dimGroups.computeIfAbsent(dim, k -> new ArrayList<>()).add(entry);
        }

        Map<String, List<SearchNode>> sortedGroups = sortGroupsLocally(dimGroups, List.of("overworld", "nether", "end"));
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
                TreeNode modNode = new TreeNode(namespace, Component.literal(com.sanhiruzu.ami.index.providers.RegistryUtils.modDisplayName(namespace)));
                modNode.setModGroup(true);
                modNode.setExpanded(true);
                for (SearchNode node : modEntry.getValue()) {
                    modNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
                }
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

        Map<String, List<SearchNode>> sortedGroups = sortGroupsLocally(modGroups, List.of());
        List<TreeNode> result = new ArrayList<>();

        for (var entry : sortedGroups.entrySet()) {
            String namespace = entry.getKey();
            TreeNode modNode = new TreeNode(namespace, Component.literal(com.sanhiruzu.ami.index.providers.RegistryUtils.modDisplayName(namespace)));
            modNode.setExpanded(true);
            modNode.setModGroup(true);

            Map<com.sanhiruzu.ami.index.NodeType, List<SearchNode>> typeGroups = new LinkedHashMap<>();
            for (SearchNode node : entry.getValue()) {
                typeGroups.computeIfAbsent(node.type(), k -> new ArrayList<>()).add(node);
            }

            for (var typeEntry : typeGroups.entrySet()) {
                com.sanhiruzu.ami.index.NodeType type = typeEntry.getKey();
                TreeNode typeNode = new TreeNode(type.name(), type.displayName());
                typeNode.setExpanded(true);
                for (SearchNode node : typeEntry.getValue()) {
                    typeNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
                }
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
        // Fallback for missing setting in AmiConfig or using UI themes
        boolean blocksMaterial = false; // TODO: Map ui settings to AmiConfig

        // Sort categories alphabetically by display name if in ALPHABETICAL sort mode, otherwise use ontology order
        List<AmiOntology.Category> categoriesToDisplay = new ArrayList<>(AmiOntology.CATEGORIES);
        if (sortField == SortField.ALPHABETICAL) {
            categoriesToDisplay.sort((a, b) -> a.displayName().getString().compareToIgnoreCase(b.displayName().getString()));
        }
        if (!ascending) {
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

            Map<String, List<SearchNode>> sortedSubs = sortGroupsLocally(subMap, List.of());
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
                            .orElse(Component.literal(formatGroupLabel(formatGroupKey(subId, true))));
                }

                TreeNode subNode = new TreeNode(subKey, subLabel);
                subNode.setExpanded(true);

                Map<String, List<SearchNode>> pMap = new LinkedHashMap<>();
                for (SearchNode node : subEntry.getValue()) {
                    pMap.computeIfAbsent(node.meta(SearchNodeKeys.POTION_EFFECT, ""), k -> new ArrayList<>()).add(node);
                }

                for (var pEntry : pMap.entrySet()) {
                    if (pEntry.getKey().isEmpty()) {
                        for (SearchNode node : pEntry.getValue())
                            subNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
                    } else {
                        TreeNode pNode = new TreeNode(pEntry.getKey(), potionEffectLabel(pEntry.getKey()));
                        pNode.setExpanded(false); // Keep specific potion effect clusters collapsed by default
                        for (SearchNode node : pEntry.getValue())
                            pNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
                        subNode.addChild(pNode);
                    }
                }
                catNode.addChild(subNode);
            }
            result.add(catNode);
        }
        return result;
    }

    private List<TreeNode> groupByCreative(List<SearchNode> entries) {
        Map<String, List<SearchNode>> creativeGroups = new LinkedHashMap<>();
        Map<String, String> labelsByGroup = new HashMap<>();
        for (SearchNode entry : entries) {
            String groupId = entry.meta(SearchNodeKeys.CREATIVE_TAB_ID, "");
            if (groupId.isBlank()) {
                groupId = FALLBACK_GROUP_KEY;
            } else {
                labelsByGroup.putIfAbsent(groupId, entry.meta(SearchNodeKeys.CREATIVE_TAB_LABEL, groupId));
            }
            creativeGroups.computeIfAbsent(groupId, k -> new ArrayList<>()).add(entry);
        }

        Map<String, List<SearchNode>> sortedGroups = sortGroupsLocally(creativeGroups, List.of());
        List<TreeNode> result = new ArrayList<>();
        for (var entry : sortedGroups.entrySet()) {
            String groupId = entry.getKey();
            Component label = FALLBACK_GROUP_KEY.equals(groupId)
                    ? Component.translatable("ami.group.unregistered")
                    : Component.literal(labelsByGroup.getOrDefault(groupId, formatGroupLabel(formatGroupKey(groupId, true))));
            TreeNode groupNode = new TreeNode(groupId, label);
            groupNode.setExpanded(true);
            for (SearchNode node : entry.getValue()) {
                groupNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
            }
            result.add(groupNode);
        }
        return result;
    }

    private List<TreeNode> groupByClassifier(List<SearchNode> entries, java.util.function.Function<SearchNode, String> classifier,
                                             Component fallback, boolean compactResourceIds, List<String> order, boolean mergeLonely) {
        Map<String, List<SearchNode>> rawGroups = new LinkedHashMap<>();
        for (SearchNode entry : entries) {
            rawGroups.computeIfAbsent(classifier.apply(entry), k -> new ArrayList<>()).add(entry);
        }

        Map<String, List<SearchNode>> sortedGroups = sortGroupsLocally(rawGroups, order);
        List<TreeNode> result = new ArrayList<>();
        List<SearchNode> lonelyItems = new ArrayList<>();

        for (var entry : sortedGroups.entrySet()) {
            String val = entry.getKey();
            List<SearchNode> members = entry.getValue();

            if (mergeLonely && members.size() == 1 && !order.contains(val) && !isUnknownGroup(val)) {
                lonelyItems.add(members.get(0));
                continue;
            }

            boolean isFallback = isUnknownGroup(val);
            String mapKey = isFallback ? FALLBACK_GROUP_KEY : formatGroupKey(val, compactResourceIds);

            TreeNode groupNode = new TreeNode(mapKey, isFallback ? fallback : Component.literal(formatGroupLabel(mapKey)));
            groupNode.setExpanded(true);
            for (SearchNode node : members) {
                groupNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
            }
            result.add(groupNode);
        }

        if (!lonelyItems.isEmpty()) {
            TreeNode miscNode = new TreeNode("ami.group.misc", Component.translatable("ami.group.misc"));
            miscNode.setExpanded(true);

            if (lonelyItems.size() > 20) {
                // Hierarchical Misc: group by category
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
                            .orElse(Component.literal(formatGroupLabel(formatGroupKey(catId, true))));

                    TreeNode catNode = new TreeNode("misc:" + catId, Component.literal("Misc: ").append(label));
                    catNode.setExpanded(false);
                    for (SearchNode node : catEntry.getValue()) {
                        catNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
                    }
                    miscNode.addChild(catNode);
                }
            } else {
                // Flat Misc
                for (SearchNode node : lonelyItems) {
                    miscNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
                }
            }

            // Insert before fallback if exists, otherwise at the end
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

    private Component potionEffectLabel(String effectId) {
        ResourceLocation loc = ResourceLocation.tryParse(effectId);
        if (loc != null) return Component.translatable("effect." + loc.getNamespace() + "." + loc.getPath());
        return Component.literal(formatGroupLabel(effectId.replace(':', '_').replace('/', '_')));
    }

    private static boolean isBuildingShape(String subId) {
        return Set.of("full_block", "stairs", "slab", "wall", "fence", "pane", "building").contains(subId);
    }

    private boolean matchesFacets(SearchNode node) {
        if (activeFacets.isEmpty()) return true;
        return activeFacets.contains(AmiOntology.classifyNode(node).id);
    }

    private boolean matchesAccessLevel(SearchNode node) {
        String level = node.meta(SearchNodeKeys.ACCESS_LEVEL, "");
        if ("dev".equals(level)) return AmiConfig.devMode;

        // Hide creative-only items in survival mode
        if ("creative".equals(level)) {
            net.minecraft.client.multiplayer.MultiPlayerGameMode gameMode = net.minecraft.client.Minecraft.getInstance().gameMode;
            if (gameMode != null && gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SURVIVAL) {
                return false;
            }
        }

        return true;
    }

    private List<TreeNode> applyHighCardinalityGrouping(List<TreeNode> nodes) {
        List<TreeNode> result = new ArrayList<>();
        List<TreeNode> buffer = new ArrayList<>();
        String bufferBaseId = null;

        for (TreeNode node : nodes) {
            if (!node.isLeaf()) {
                flushCardinalityBuffer(buffer, result);
                bufferBaseId = null;
                List<TreeNode> children = applyHighCardinalityGrouping(node.getChildren());
                if (!children.isEmpty()) {
                    TreeNode processedGroup = new TreeNode(node.getKey(), node.getLabel());
                    processedGroup.setExpanded(node.isExpanded());
                    processedGroup.setModGroup(node.isModGroup());
                    processedGroup.getChildren().addAll(children);
                    result.add(processedGroup);
                }
                continue;
            }

            String baseId = node.getEntry().meta(SearchNodeKeys.SUBTYPE_OF, "");
            if (baseId.isEmpty()) {
                flushCardinalityBuffer(buffer, result);
                result.add(node);
                bufferBaseId = null;
            } else if (baseId.equals(bufferBaseId)) {
                buffer.add(node);
            } else {
                flushCardinalityBuffer(buffer, result);
                buffer.add(node);
                bufferBaseId = baseId;
            }
        }
        flushCardinalityBuffer(buffer, result);
        return result;
    }

    private boolean isUnknownGroup(String key) {
        return GroupingEngine.isUnknownGroup(key);
    }

    private List<TreeNode> applyExplicitFamilyGrouping(List<TreeNode> nodes) {
        List<TreeNode> recursive = new ArrayList<>();
        for (TreeNode node : nodes) {
            if (node.isLeaf()) {
                recursive.add(node);
                continue;
            }

            TreeNode processedGroup = new TreeNode(node.getKey(), node.getLabel());
            processedGroup.setExpanded(node.isExpanded());
            processedGroup.setModGroup(node.isModGroup());
            processedGroup.setHighCardinality(node.isHighCardinality());
            processedGroup.setItemCountOverride(node.getItemCountOverride());
            processedGroup.getChildren().addAll(applyExplicitFamilyGrouping(node.getChildren()));
            recursive.add(processedGroup);
        }

        Map<String, List<TreeNode>> familyMembers = new LinkedHashMap<>();
        Map<String, String> familyLabels = new HashMap<>();
        for (TreeNode node : recursive) {
            if (!node.isLeaf()) continue;
            String familyKey = node.getEntry().meta(SearchNodeKeys.COLLAPSE_FAMILY, "");
            if (familyKey.isEmpty()) continue;
            familyMembers.computeIfAbsent(familyKey, ignored -> new ArrayList<>()).add(node);
            familyLabels.putIfAbsent(familyKey, node.getEntry().meta(SearchNodeKeys.COLLAPSE_LABEL, ""));
        }

        Map<TreeNode, TreeNode> replacementGroups = new IdentityHashMap<>();
        for (var entry : familyMembers.entrySet()) {
            if (entry.getValue().size() < EXPLICIT_FAMILY_THRESHOLD) continue;
            String familyKey = entry.getKey();
            String label = familyLabels.getOrDefault(familyKey, formatGroupLabel(formatGroupKey(familyKey, false)));
            TreeNode group = new TreeNode("cardinality:family:" + familyKey, Component.literal(label));
            group.setHighCardinality(true);
            group.setExpanded(false);
            group.getChildren().addAll(entry.getValue());
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

    private void flushCardinalityBuffer(List<TreeNode> buffer, List<TreeNode> result) {
        if (buffer.isEmpty()) return;
        if (buffer.size() >= CARDINALITY_THRESHOLD) {
            SearchNode representative = buffer.get(0).getEntry();
            String baseId = representative.meta(SearchNodeKeys.SUBTYPE_OF);
            String label = "";

            ResourceLocation loc = ResourceLocation.tryParse(baseId);
            if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                // Derive label from registry path to avoid "Uncraftable Potion" from plain ItemStack
                label = formatGroupLabel(formatGroupKey(loc.getPath(), false));
                if (!label.endsWith("s")) label += "s";
            } else {
                label = buffer.get(0).getLabel().getString();
                if (label.contains("(")) label = label.substring(0, label.indexOf('(')).trim();
                else if (label.contains(" - ")) label = label.substring(0, label.indexOf(" - ")).trim();
            }

            TreeNode group = new TreeNode("cardinality:" + baseId, Component.literal(label));
            group.setHighCardinality(true);
            group.setExpanded(false);
            group.getChildren().addAll(buffer);
            result.add(group);
        } else {
            result.addAll(buffer);
        }
        buffer.clear();
    }

    public Set<String> getAllMods(List<SearchNode> results) {
        return results.stream().map(n -> n.id().getNamespace()).collect(Collectors.toSet());
    }

    public SortField getSortField() {
        return sortField;
    }

    public boolean isAscending() {
        return ascending;
    }

    public GroupBy getGroupBy() {
        return groupBy;
    }

    public Set<String> getSelectedMods() {
        return selectedMods;
    }

    public Set<String> getActiveFacets() {
        return activeFacets;
    }
}
