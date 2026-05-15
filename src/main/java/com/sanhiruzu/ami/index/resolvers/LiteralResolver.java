package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;

import java.util.*;

/**
 * Wraps the trie + substring fallback logic for indexed nodes.
 * This is the primary resolver — it covers all cached NodeTypes.
 */
public final class LiteralResolver implements IQueryResolver {

    private static final class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        final List<SearchNode> hits = new ArrayList<>();
    }

    private final TrieNode root = new TrieNode();
    private final List<SearchNode> allNodes = new ArrayList<>();

    /** Called during SearchService.buildFrom() to pre-load the trie. */
    public void addNode(SearchNode node) {
        allNodes.add(node);
        insert(node.displayName().toLowerCase(), node);
    }

    private void insert(String key, SearchNode node) {
        TrieNode current = root;
        for (char c : key.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
        }
        current.hits.add(node);
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        String lower = query.toLowerCase();
        Set<SearchNode> prefixHits = new LinkedHashSet<>();

        // Step 1: Walk trie for prefix matches
        TrieNode current = root;
        for (char c : lower.toCharArray()) {
            current = current.children.get(c);
            if (current == null) break;
        }
        if (current != null) bfsCollect(current, prefixHits);

        // Step 2: Linear scan for substring fallback (not already found)
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

        for (SearchNode node : prefixHits) {
            result.get(node.type()).add(node);
        }
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

    private void bfsCollect(TrieNode start, Set<SearchNode> out) {
        Queue<TrieNode> q = new LinkedList<>();
        q.add(start);
        while (!q.isEmpty()) {
            TrieNode t = q.poll();
            out.addAll(t.hits);
            q.addAll(t.children.values());
        }
    }
}
