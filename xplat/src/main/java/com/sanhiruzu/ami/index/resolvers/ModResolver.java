package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;

import java.util.*;

public class ModResolver implements IQueryResolver {
    private final Map<String, List<SearchNode>> modIndex = new HashMap<>();

    public void addNode(SearchNode node) {
        String namespace = node.id().getNamespace().toLowerCase();
        if (!namespace.isEmpty()) {
            modIndex.computeIfAbsent(namespace, k -> new ArrayList<>()).add(node);
        }
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();

        // Find all mods that contain the query string (case-insensitive substring match)
        String lowerQuery = query.toLowerCase();
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
