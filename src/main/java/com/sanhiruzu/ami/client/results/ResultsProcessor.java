package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.AmiIndexerService;
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
    private static final String FALLBACK_GROUP_KEY = "__fallback__";

    public enum SortField {
        ALPHABETICAL("ami.sort.alphabetical"),
        COLOR("ami.sort.color"),
        MOD("ami.sort.mod"),
        STORAGE_CAPACITY("ami.sort.storage"),
        DPS("ami.sort.dps"),
        COUNT("ami.sort.count");

        public final Component displayName;
        SortField(String key) { this.displayName = Component.translatable(key); }
    }

    public enum GroupBy {
        DIMENSION("ami.group.dimension"),
        MOD("ami.group.mod"),
        CATEGORY("ami.group.category"),
        MATERIAL("ami.group.material"),
        FAMILY("ami.group.family"),
        SHAPE("ami.group.shape");

        public final Component displayName;
        GroupBy(String key) { this.displayName = Component.translatable(key); }
    }


    private final SortField sortField;
    private final boolean ascending;
    private final GroupBy groupBy;
    private final Set<String> selectedMods;
    private final Set<String> activeFacets;

    public ResultsProcessor(SortField sortField, boolean ascending, GroupBy groupBy,
                            Set<String> selectedMods, Set<String> activeFacets) {
        this.sortField    = sortField;
        this.ascending    = ascending;
        this.groupBy      = groupBy;
        this.selectedMods = selectedMods  != null ? selectedMods  : new HashSet<>();
        this.activeFacets = activeFacets != null ? activeFacets : new HashSet<>();
    }

    public List<TreeNode> process(List<SearchNode> results) {
        // Handle background indexing state
        if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
            return List.of(new TreeNode("indexing", Component.translatable("ami.gui.background_indexing")
                    .withStyle(s -> s.withColor(0xFFAA00))));
        }

        // Filter
        List<SearchNode> filtered = results.stream()
                .filter(n -> selectedMods.isEmpty() || selectedMods.contains(n.id().getNamespace()))
                .filter(this::matchesFacets)
                .filter(this::matchesAccessLevel)
                .collect(Collectors.toList());

        // Sort
        filtered.sort(this::compareNodes);
        if (!ascending) {
            Collections.reverse(filtered);
        }

        // Group
        List<TreeNode> tree = buildTree(filtered);
        
        return applyHighCardinalityGrouping(tree);
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
            case DIMENSION -> groupByDimension(sorted);
            case MOD -> groupByMod(sorted);
            case CATEGORY -> groupByCategory(sorted);
            case MATERIAL -> groupByClassifier(sorted, n -> n.meta(SearchNodeKeys.MATERIAL_GROUP, ""), Component.translatable("ami.group.unknown_material"), true, List.of());
            case FAMILY -> groupByClassifier(sorted, n -> {
                ItemStack stack = ItemIconRenderer.resolveStack(n.id());
                return GroupingEngine.classifyFamilyRoot(stack);
            }, Component.translatable("ami.group.unknown_family"), true, List.of());
            case SHAPE -> groupByClassifier(sorted, n -> n.meta(SearchNodeKeys.VARIANT_GROUP, ""), Component.translatable("ami.group.unknown_shape"), false, GroupingEngine.SHAPE_ORDER);
        };
    }

    private Map<String, List<SearchNode>> sortGroupsLocally(Map<String, List<SearchNode>> groups, List<String> order) {
        if (sortField != SortField.COUNT) {
            return GroupingEngine.sortGroups(groups, order);
        }

        List<Map.Entry<String, List<SearchNode>>> entries = new ArrayList<>(groups.entrySet());
        entries.sort((a, b) -> {
            String k1 = a.getKey(); String k2 = b.getKey();
            
            // Unknowns/Fallbacks always to the bottom
            boolean u1 = k1.isBlank() || k1.equals("item") || k1.equals("minecraft:item") || k1.equals("block") || k1.toLowerCase().contains("unknown") || k1.equals(FALLBACK_GROUP_KEY);
            boolean u2 = k2.isBlank() || k2.equals("item") || k2.equals("minecraft:item") || k2.equals("block") || k2.toLowerCase().contains("unknown") || k2.equals(FALLBACK_GROUP_KEY);
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

        for (AmiOntology.Category cat : AmiOntology.CATEGORIES) {
            List<SearchNode> catEntries = catMap.get(cat.id);
            if (catEntries == null || catEntries.isEmpty()) continue;

            TreeNode catNode = new TreeNode(cat.id, cat.displayName());
            catNode.setExpanded(true);

            Map<String, List<SearchNode>> subMap = new LinkedHashMap<>();
            for (SearchNode entry : catEntries) {
                String rawSubId = entry.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
                final String subId;
                if (cat == AmiOntology.BLOCKS && blocksMaterial && isBuildingShape(rawSubId)) {
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
                if (subId.isEmpty()) {
                    TreeNode miscNode = new TreeNode("misc", Component.translatable("ami.group.misc"));
                    for (SearchNode node : subEntry.getValue()) miscNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
                    catNode.addChild(miscNode);
                    continue;
                }

                Component subLabel = cat.subCategories.stream()
                        .filter(s -> s.id().equals(subId))
                        .map(AmiOntology.SubCategory::displayName)
                        .findFirst()
                        .orElse(Component.literal(formatGroupLabel(formatGroupKey(subId, true))));
                
                TreeNode subNode = new TreeNode(subId, subLabel);
                subNode.setExpanded(true);

                Map<String, List<SearchNode>> pMap = new LinkedHashMap<>();
                for (SearchNode node : subEntry.getValue()) {
                    pMap.computeIfAbsent(node.meta(SearchNodeKeys.POTION_EFFECT, ""), k -> new ArrayList<>()).add(node);
                }
                
                for (var pEntry : pMap.entrySet()) {
                    if (pEntry.getKey().isEmpty()) {
                        for (SearchNode node : pEntry.getValue()) subNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
                    } else {
                        TreeNode pNode = new TreeNode(pEntry.getKey(), potionEffectLabel(pEntry.getKey()));
                        for (SearchNode node : pEntry.getValue()) pNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
                        subNode.addChild(pNode);
                    }
                }
                catNode.addChild(subNode);
            }
            result.add(catNode);
        }
        return result;
    }

    private List<TreeNode> groupByClassifier(List<SearchNode> entries, java.util.function.Function<SearchNode, String> classifier, 
                                            Component fallback, boolean compactResourceIds, List<String> order) {
        Map<String, List<SearchNode>> rawGroups = new LinkedHashMap<>();
        for (SearchNode entry : entries) {
            rawGroups.computeIfAbsent(classifier.apply(entry), k -> new ArrayList<>()).add(entry);
        }

        Map<String, List<SearchNode>> sortedGroups = sortGroupsLocally(rawGroups, order);
        List<TreeNode> result = new ArrayList<>();

        for (var entry : sortedGroups.entrySet()) {
            String val = entry.getKey();
            boolean isFallback = val.isBlank() || val.equals("item") || val.equals("block") || val.toLowerCase().contains("unknown");
            String mapKey = isFallback ? FALLBACK_GROUP_KEY : formatGroupKey(val, compactResourceIds);
            
            TreeNode groupNode = new TreeNode(mapKey, isFallback ? fallback : Component.literal(formatGroupLabel(mapKey)));
            groupNode.setExpanded(true);
            for (SearchNode node : entry.getValue()) {
                groupNode.addChild(new TreeNode(Component.literal(node.displayName()), node));
            }
            result.add(groupNode);
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
                TreeNode processedGroup = new TreeNode(node.getKey(), node.getLabel());
                processedGroup.setExpanded(node.isExpanded());
                processedGroup.setModGroup(node.isModGroup());
                processedGroup.getChildren().addAll(applyHighCardinalityGrouping(node.getChildren()));
                result.add(processedGroup);
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

    public SortField getSortField() { return sortField; }
    public boolean isAscending() { return ascending; }
    public GroupBy getGroupBy() { return groupBy; }
    public Set<String> getSelectedMods() { return selectedMods; }
    public Set<String> getActiveFacets() { return activeFacets; }
}
