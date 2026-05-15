package com.sanhiruzu.ami.index;

import java.util.*;

/**
 * Trie-based search service for querying SearchNodes across all NodeTypes.
 * Implements prefix matching via Trie with substring fallback for broad results.
 */
public final class SearchService {
    private static final class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        final List<SearchNode> hits = new ArrayList<>();
    }

    private final TrieNode root = new TrieNode();
    private final List<SearchNode> allNodes = new ArrayList<>();

    private SearchService() {}

    /**
     * Build a SearchService from the current GlobalIndex.
     */
    public static SearchService buildFrom(GlobalIndex index) {
        SearchService service = new SearchService();

        for (NodeType type : NodeType.values()) {
            for (SearchNode node : index.getNodes(type)) {
                service.allNodes.add(node);
                service.insert(node.displayName().toLowerCase(), node);
            }
        }

        return service;
    }

    private void insert(String key, SearchNode node) {
        TrieNode current = root;
        for (char c : key.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
        }
        current.hits.add(node);
    }

    /**
     * Query for nodes matching the text (prefix or substring).
     * Returns results grouped by NodeType, prefix matches before substring matches.
     */
    public Map<NodeType, List<SearchNode>> query(String text) {
        if (text == null || text.isEmpty()) {
            return new LinkedHashMap<>();
        }

        String lower = text.toLowerCase();
        Set<SearchNode> prefixHits = new LinkedHashSet<>();

        // Step 1: Walk Trie for prefix matches
        TrieNode current = root;
        for (char c : lower.toCharArray()) {
            current = current.children.get(c);
            if (current == null) break;
        }

        if (current != null) {
            bfsCollectHits(current, prefixHits);
        }

        // Step 2: Linear scan for substring matches not already found
        List<SearchNode> substringHits = new ArrayList<>();
        for (SearchNode node : allNodes) {
            if (!prefixHits.contains(node) && node.displayName().toLowerCase().contains(lower)) {
                substringHits.add(node);
            }
        }

        // Step 3: Merge into result map grouped by type
        Map<NodeType, List<SearchNode>> result = new LinkedHashMap<>();
        for (NodeType type : NodeType.values()) {
            result.put(type, new ArrayList<>());
        }

        // Add prefix hits first (higher relevance)
        for (SearchNode node : prefixHits) {
            result.get(node.type()).add(node);
        }

        // Then add substring hits
        for (SearchNode node : substringHits) {
            result.get(node.type()).add(node);
        }

        // Remove empty types and cap results per type
        result.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        for (List<SearchNode> list : result.values()) {
            if (list.size() > 200) {
                list.subList(200, list.size()).clear();
            }
        }

        return result;
    }

    private void bfsCollectHits(TrieNode startNode, Set<SearchNode> results) {
        Queue<TrieNode> queue = new LinkedList<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();
            results.addAll(current.hits);
            queue.addAll(current.children.values());
        }
    }
}
