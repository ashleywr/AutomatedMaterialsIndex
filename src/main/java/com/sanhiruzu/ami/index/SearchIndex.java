package com.sanhiruzu.ami.index;

import java.util.*;
import java.util.concurrent.*;

/**
 * Lightweight, concurrent-friendly search index.
 * Uses a ConcurrentSkipListMap for prefix searches (fast, memory-efficient) and
 * falls back to a concurrent set for substring scans.
 *
 * This class is intentionally pluggable: later it can be swapped to a shaded
 * ConcurrentRadixTree implementation without changing callers.
 */
public final class SearchIndex {
    // Keys are lowercase display names
    private final ConcurrentSkipListMap<String, CopyOnWriteArrayList<SearchNode>> map = new ConcurrentSkipListMap<>();
    private final Set<SearchNode> allNodes = ConcurrentHashMap.newKeySet();

    public void addNode(SearchNode node) {
        String key = node.displayName().toLowerCase();
        map.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(node);
        allNodes.add(node);
    }

    /**
     * Prefix search using subMap (log(n) to find start, then linear across the prefix range).
     */
    public List<SearchNode> prefixSearch(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        String low = prefix.toLowerCase();
        String high = low + Character.MAX_VALUE;
        NavigableMap<String, CopyOnWriteArrayList<SearchNode>> sub = map.subMap(low, true, high, true);
        LinkedHashSet<SearchNode> out = new LinkedHashSet<>();
        for (List<SearchNode> bucket : sub.values()) {
            out.addAll(bucket);
        }
        return new ArrayList<>(out);
    }

    /**
     * Substring fallback (only used when prefix search misses). This is linear in node count.
     */
    public List<SearchNode> substringSearch(String substring) {
        if (substring == null || substring.isEmpty()) return Collections.emptyList();
        String low = substring.toLowerCase();
        List<SearchNode> out = new ArrayList<>();
        for (SearchNode n : allNodes) {
            if (n.displayName().toLowerCase().contains(low)) out.add(n);
        }
        return out;
    }
}
