package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.*;

public class TagResolver implements IQueryResolver {
    private final Map<String, List<SearchNode>> tagIndex = new HashMap<>();

    public void addNode(SearchNode node) {
        addTags(node.metadata().get(SearchNodeKeys.TAGS), node);
        addTags(node.metadata().get(SearchNodeKeys.BLOCK_TAGS), node);
    }

    private void addTags(String tagsStr, SearchNode node) {
        if (tagsStr == null || tagsStr.isBlank()) return;
        for (String tag : tagsStr.split(",")) {
            tag = tag.trim().toLowerCase();
            if (!tag.isEmpty()) {
                tagIndex.computeIfAbsent(tag, k -> new ArrayList<>()).add(node);
            }
        }
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();

        // Find all tags that contain the query string (case-insensitive substring match)
        String lowerQuery = query.toLowerCase();
        Set<SearchNode> matchedNodes = new LinkedHashSet<>();

        for (var entry : tagIndex.entrySet()) {
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
