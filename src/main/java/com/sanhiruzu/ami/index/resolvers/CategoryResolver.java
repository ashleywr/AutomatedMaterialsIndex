package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;

import java.util.*;

/**
 * Resolves $categoryId tokens against the AMI ontology.
 * "$magic" returns all nodes classified as Magic & Alchemy, etc.
 * Partial prefix matching is supported ("$mag" → magic).
 */
public class CategoryResolver implements IQueryResolver {

    private final Map<String, Map<NodeType, List<SearchNode>>> byCategory = new HashMap<>();

    public void addNode(SearchNode node) {
        String catId = AmiOntology.classifyNode(node).id;
        byCategory
                .computeIfAbsent(catId, k -> new LinkedHashMap<>())
                .computeIfAbsent(node.type(), k -> new ArrayList<>())
                .add(node);
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        String id = query.toLowerCase(java.util.Locale.ROOT);

        // Exact match first
        if (byCategory.containsKey(id)) return copy(byCategory.get(id));

        // Prefix match (e.g. "mag" → "magic", "mob" → "mobs" if that were a category id)
        for (var entry : byCategory.entrySet()) {
            if (entry.getKey().startsWith(id)) return copy(entry.getValue());
        }

        return Map.of();
    }

    private static Map<NodeType, List<SearchNode>> copy(Map<NodeType, List<SearchNode>> src) {
        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();
        for (var entry : src.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
}
