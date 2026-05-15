package com.sanhiruzu.ami.index;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tokenized Trie implementation using fastutil's primitive-char map to avoid boxing.
 * Fast for prefix searches and has a simple substring fallback.
 */
public final class SearchIndex {

    private static final class TrieNode {
        final Char2ObjectOpenHashMap<TrieNode> children = new Char2ObjectOpenHashMap<>();
        final CopyOnWriteArrayList<SearchNode> hits = new CopyOnWriteArrayList<>();
    }

    private final TrieNode root = new TrieNode();
    private final Set<SearchNode> allNodes = ConcurrentHashMap.newKeySet();

    /**
     * Add a node into the trie. Mutations are synchronized to keep the trie safe
     * during concurrent indexing; reads remain lock-free.
     */
    public void addNode(SearchNode node) {
        String key = node.displayName().toLowerCase(Locale.ROOT);
        synchronized (this) {
            TrieNode cur = root;
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                TrieNode next = cur.children.get(c);
                if (next == null) {
                    next = new TrieNode();
                    cur.children.put(c, next);
                }
                cur = next;
            }
            cur.hits.add(node);
            allNodes.add(node);
        }
    }

    /**
     * Prefix search: walk the trie to the prefix node then BFS collect results.
     */
    public List<SearchNode> prefixSearch(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        String low = prefix.toLowerCase(Locale.ROOT);
        TrieNode cur = root;
        for (int i = 0; i < low.length(); i++) {
            TrieNode next = cur.children.get(low.charAt(i));
            if (next == null) return Collections.emptyList();
            cur = next;
        }

        LinkedHashSet<SearchNode> out = new LinkedHashSet<>();
        Deque<TrieNode> dq = new ArrayDeque<>();
        dq.add(cur);
        while (!dq.isEmpty()) {
            TrieNode t = dq.poll();
            out.addAll(t.hits);
            for (TrieNode child : t.children.values()) {
                dq.add(child);
            }
        }
        return new ArrayList<>(out);
    }

    /**
     * Substring fallback scan.
     */
    public List<SearchNode> substringSearch(String substring) {
        if (substring == null || substring.isEmpty()) return Collections.emptyList();
        String low = substring.toLowerCase(Locale.ROOT);
        List<SearchNode> out = new ArrayList<>();
        for (SearchNode n : allNodes) {
            if (n.displayName().toLowerCase(Locale.ROOT).contains(low)) out.add(n);
        }
        return out;
    }
}
