package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.*;

public class ModResolver implements IQueryResolver {
    private final Map<String, List<SearchNode>> modIndex = new HashMap<>();

    public void addNode(SearchNode node) {
        String namespace = node.id().getNamespace().toLowerCase(Locale.ROOT);
        if (!namespace.isEmpty()) {
            modIndex.computeIfAbsent(namespace, k -> new ArrayList<>()).add(node);
        }
        addAlias(node.meta(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, ""), node);
        addAlias(node.meta(SearchNodeKeys.COMPAT_FAMILY, ""), node);
        for (String family : node.meta(SearchNodeKeys.COMPAT_FAMILIES, "").split(",")) {
            addAlias(family, node);
        }
    }

    private void addAlias(String alias, SearchNode node) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        String normalized = alias.trim().toLowerCase(Locale.ROOT);
        modIndex.computeIfAbsent(normalized, k -> new ArrayList<>()).add(node);
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();

        // Find all mods that contain the query string (case-insensitive substring match)
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        Set<SearchNode> matchedNodes = new LinkedHashSet<>();

        for (var entry : modIndex.entrySet()) {
            if (entry.getKey().contains(lowerQuery)) {
                matchedNodes.addAll(entry.getValue());
            }
        }

        if (!matchedNodes.isEmpty()) {
            for (SearchNode node : matchedNodes) {
                NodeType type = node.type();
                result.computeIfAbsent(type, k -> new ArrayList<>()).add(node);
            }
        }

        return result;
    }
}
