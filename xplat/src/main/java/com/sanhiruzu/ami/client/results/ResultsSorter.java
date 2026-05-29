package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GroupingEngine;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ResultsSorter {
    private final ResultsPresentationOptions options;

    ResultsSorter(ResultsPresentationOptions options) {
        this.options = options;
    }

    List<SearchNode> sort(List<SearchNode> nodes) {
        List<SearchNode> sorted = new ArrayList<>(nodes);
        sorted.sort(this::compareNodes);
        if (!options.ascending()) {
            Collections.reverse(sorted);
        }
        return sorted;
    }

    Map<String, List<SearchNode>> sortGroups(Map<String, List<SearchNode>> groups, List<String> order) {
        if (options.sortField() != ResultsProcessor.SortField.COUNT) {
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
                return options.ascending() ? cmp : -cmp;
            });

            Map<String, List<SearchNode>> result = new LinkedHashMap<>();
            for (var entry : entries) result.put(entry.getKey(), entry.getValue());
            return result;
        }

        List<Map.Entry<String, List<SearchNode>>> entries = new ArrayList<>(groups.entrySet());
        entries.sort((a, b) -> {
            String k1 = a.getKey();
            String k2 = b.getKey();

            boolean u1 = GroupingEngine.isUnknownGroup(k1);
            boolean u2 = GroupingEngine.isUnknownGroup(k2);
            if (u1 && !u2) return 1;
            if (!u1 && u2) return -1;

            int cmp = Integer.compare(a.getValue().size(), b.getValue().size());
            if (!options.ascending()) {
                cmp = -cmp;
            }
            if (cmp == 0) {
                return k1.compareTo(k2);
            }
            return cmp;
        });

        Map<String, List<SearchNode>> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private int compareNodes(SearchNode a, SearchNode b) {
        return switch (options.sortField()) {
            case REGISTRY -> compareByRegistryOrder(a, b);
            case ALPHABETICAL, COUNT -> a.displayName().compareTo(b.displayName());
            case COLOR -> Integer.compare(a.color(), b.color());
            case MOD -> a.id().getNamespace().compareTo(b.id().getNamespace());
            case STORAGE_CAPACITY -> compareNumericMeta(a, b, SearchNodeKeys.ESM_CAPACITY);
            case ENERGY_CAPACITY -> compareNumericMeta(a, b, SearchNodeKeys.ENERGY_CAPACITY);
            case ENERGY_GENERATION -> compareNumericMeta(a, b, SearchNodeKeys.ENERGY_GENERATION);
            case FLUID_CAPACITY -> compareNumericMeta(a, b, SearchNodeKeys.FLUID_CAPACITY);
            case TOOL_SPEED -> compareNumericMeta(a, b, SearchNodeKeys.TOOL_SPEED);
            case TOOL_USES -> compareNumericMeta(a, b, SearchNodeKeys.TOOL_USES);
            case ARMOR_DEFENSE -> compareNumericMeta(a, b, SearchNodeKeys.ARMOR_DEFENSE);
            case ARMOR_TOUGHNESS -> compareNumericMeta(a, b, SearchNodeKeys.ARMOR_TOUGHNESS);
            case FOOD_NUTRITION -> compareNumericMeta(a, b, SearchNodeKeys.FOOD_NUTRITION);
            case FOOD_SATURATION -> compareNumericMeta(a, b, SearchNodeKeys.FOOD_SATURATION);
            case DAMAGE -> Double.compare(damageValue(a), damageValue(b));
            case HEALTH -> compareNumericMeta(a, b, SearchNodeKeys.ENTITY_HEALTH);
            case DPS -> compareNumericMeta(a, b, SearchNodeKeys.DPS);
        };
    }

    private int compareByRegistryOrder(SearchNode a, SearchNode b) {
        int idA = registryId(a);
        int idB = registryId(b);
        if (idA >= 0 && idB >= 0) return Integer.compare(idA, idB);
        if (idA >= 0) return -1;
        if (idB >= 0) return 1;
        return a.displayName().compareTo(b.displayName());
    }

    private int registryId(SearchNode node) {
        ResourceLocation loc = node.id();
        if (loc.getPath().contains("/")) return -1;
        var item = BuiltInRegistries.ITEM.get(loc);
        if (item == net.minecraft.world.item.Items.AIR && !loc.getPath().equals("air")) return -1;
        return BuiltInRegistries.ITEM.getId(item);
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

    private double damageValue(SearchNode node) {
        double itemDamage = parseNumericMeta(node, SearchNodeKeys.ATTACK_DAMAGE);
        if (itemDamage != Double.NEGATIVE_INFINITY) {
            return itemDamage;
        }
        return parseNumericMeta(node, SearchNodeKeys.ENTITY_ATTACK_DAMAGE);
    }
}
