package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;

import java.util.*;

import com.sanhiruzu.ami.AmiCore;
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

        // Exact match check
        if (byCategory.containsKey(id)) {
            return byCategory.get(id);
        }

        // Prefix match check
        for (var entry : byCategory.entrySet()) {
            if (entry.getKey().startsWith(id)) {
                return entry.getValue();
            }
        }

        return Collections.emptyMap();
    }
}
