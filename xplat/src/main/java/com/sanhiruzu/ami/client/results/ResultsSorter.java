package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GroupingEngine;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.*;

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
            case GREGTECH_EU -> Double.compare(gregTechEuValue(a), gregTechEuValue(b));
            case GREGTECH_EU_GENERATION -> compareNumericMeta(a, b, SearchNodeKeys.GREGTECH_EU_GENERATION);
            case GREGTECH_EU_CONSUMPTION -> compareNumericMeta(a, b, SearchNodeKeys.GREGTECH_EU_CONSUMPTION);
            case GREGTECH_EU_INPUT -> compareNumericMeta(a, b, SearchNodeKeys.GREGTECH_EU_INPUT);
            case GREGTECH_EU_OUTPUT -> compareNumericMeta(a, b, SearchNodeKeys.GREGTECH_EU_OUTPUT);
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
        int bucketA = registrySortBucket(a);
        int bucketB = registrySortBucket(b);
        if (bucketA != bucketB) {
            return Integer.compare(bucketA, bucketB);
        }

        int cmp;
        if (bucketA == 0) {
            cmp = Integer.compare(registryId(a), registryId(b));
        } else if (bucketA == 1) {
            cmp = compareNumericMeta(a, b, SearchNodeKeys.POKEMON_DEX_NUMBER);
        } else {
            cmp = compareDisplayName(a, b);
        }
        return cmp != 0 ? cmp : compareStableIdentity(a, b);
    }

    private int registrySortBucket(SearchNode node) {
        if (registryId(node) >= 0) {
            return 0;
        }
        if (isPokemonSpecies(node)) {
            return 1;
        }
        return 2;
    }

    private int registryId(SearchNode node) {
        if (node == null || node.id() == null) return -1;
        Identifier loc = node.id();
        if (loc.getPath().contains("/")) return -1;
        var item = BuiltInRegistries.ITEM.getValue(loc);
        if ((item == null || item == net.minecraft.world.item.Items.AIR) && !loc.getPath().equals("air")) return -1;
        return BuiltInRegistries.ITEM.getId(item);
    }

    private boolean isPokemonSpecies(SearchNode node) {
        return node != null && "pokemon_species".equals(node.meta(SearchNodeKeys.ENTITY_CATEGORY, ""));
    }

    private int compareDisplayName(SearchNode a, SearchNode b) {
        String nameA = a == null || a.displayName() == null ? "" : a.displayName();
        String nameB = b == null || b.displayName() == null ? "" : b.displayName();
        return nameA.compareTo(nameB);
    }

    private int compareStableIdentity(SearchNode a, SearchNode b) {
        int cmp = compareDisplayName(a, b);
        if (cmp != 0) {
            return cmp;
        }
        Identifier idA = a == null ? null : a.id();
        Identifier idB = b == null ? null : b.id();
        cmp = String.valueOf(idA).compareTo(String.valueOf(idB));
        if (cmp != 0) {
            return cmp;
        }
        String typeA = a == null || a.type() == null ? "" : a.type().name();
        String typeB = b == null || b.type() == null ? "" : b.type().name();
        return typeA.compareTo(typeB);
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

    private double gregTechEuValue(SearchNode node) {
        double generation = parseNumericMeta(node, SearchNodeKeys.GREGTECH_EU_GENERATION);
        double output = parseNumericMeta(node, SearchNodeKeys.GREGTECH_EU_OUTPUT);
        double consumption = parseNumericMeta(node, SearchNodeKeys.GREGTECH_EU_CONSUMPTION);
        double input = parseNumericMeta(node, SearchNodeKeys.GREGTECH_EU_INPUT);
        return Math.max(Math.max(generation, output), Math.max(consumption, input));
    }
}
