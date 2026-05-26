package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.*;

/**
 * Resolves simple metadata property filters such as ?tamable, ?mountable,
 * ?category:creature, and ?health:20.
 */
public final class PropertyResolver implements IQueryResolver {
    private final List<SearchNode> nodes = new ArrayList<>();

    public void addNode(SearchNode node) {
        nodes.add(node);
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return new LinkedHashMap<>();
        }

        int separator = normalized.indexOf(':');
        String key = separator >= 0 ? normalized.substring(0, separator) : normalized;
        String value = separator >= 0 ? normalized.substring(separator + 1) : "";

        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();
        for (SearchNode node : nodes) {
            if (matches(node, key, value)) {
                result.computeIfAbsent(node.type(), ignored -> new ArrayList<>()).add(node);
            }
        }
        return result;
    }

    private static boolean matches(SearchNode node, String key, String value) {
        return switch (key) {
            case "tamable", "tameable", "mountable", "trustsplayer", "pet" ->
                    containsToken(node, SearchNodeKeys.ENTITY_TRAITS, key)
                            || containsToken(node, SearchNodeKeys.SEARCH_TOKENS, key)
                            || containsToken(node, SearchNodeKeys.TAGS, "ami:" + key);
            case "category", "entitycategory" -> containsValue(node, SearchNodeKeys.ENTITY_CATEGORY, value);
            case "mod", "modid" -> containsValue(node, SearchNodeKeys.MOD_ID, value);
            case "health", "hp" -> containsValue(node, SearchNodeKeys.ENTITY_HEALTH, value);
            case "attack", "attackdamage", "damage" -> containsValue(node, SearchNodeKeys.ENTITY_ATTACK_DAMAGE, value);
            case "fireimmune" -> value.isEmpty()
                    ? "true".equalsIgnoreCase(node.meta(SearchNodeKeys.FIRE_IMMUNE, ""))
                    : containsValue(node, SearchNodeKeys.FIRE_IMMUNE, value);
            default -> containsAnyMetadata(node, key, value);
        };
    }

    private static boolean containsToken(SearchNode node, String metadataKey, String token) {
        String normalizedToken = normalize(token);
        if (normalizedToken.isEmpty()) {
            return false;
        }
        for (String part : normalize(node.meta(metadataKey, "")).split("[,\\s]+")) {
            if (part.equals(normalizedToken)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsValue(SearchNode node, String metadataKey, String value) {
        String metadata = normalize(node.meta(metadataKey, ""));
        String normalizedValue = normalize(value);
        if (normalizedValue.isEmpty()) {
            return !metadata.isEmpty();
        }
        return metadata.contains(normalizedValue);
    }

    private static boolean containsAnyMetadata(SearchNode node, String key, String value) {
        String normalizedValue = normalize(value.isEmpty() ? key : value);
        if (normalizedValue.isEmpty()) {
            return false;
        }
        for (var entry : node.metadata().entrySet()) {
            String metadataKey = normalize(entry.getKey());
            String metadataValue = normalize(entry.getValue());
            if ((metadataKey.equals(key) || value.isEmpty()) && metadataValue.contains(normalizedValue)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .trim();
    }
}
