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
    private static final Set<String> SEARCHABLE_METADATA_KEYS = Set.of(
            SearchNodeKeys.MOD_ID,
            SearchNodeKeys.ONTOLOGY_CATEGORY,
            SearchNodeKeys.ONTOLOGY_SUBCATEGORY,
            SearchNodeKeys.VARIANT_GROUP,
            SearchNodeKeys.COLOR_BUCKET,
            SearchNodeKeys.MATERIAL_GROUP,
            SearchNodeKeys.ESM_CAPACITY,
            SearchNodeKeys.DPS,
            SearchNodeKeys.DIMENSION,
            SearchNodeKeys.ENTITY_CATEGORY,
            SearchNodeKeys.REQUIRED_TOOL,
            SearchNodeKeys.ACCESS_LEVEL,
            SearchNodeKeys.OBTAINABILITY
    );

    private static final class TrieNode {
        final Char2ObjectOpenHashMap<TrieNode> children = new Char2ObjectOpenHashMap<>();
        final CopyOnWriteArrayList<SearchNode> hits = new CopyOnWriteArrayList<>();
    }

    private final TrieNode root = new TrieNode();
    private final Set<SearchNode> allNodes = ConcurrentHashMap.newKeySet();
    private final Map<SearchNode, String> searchableText = new ConcurrentHashMap<>();

    /**
     * Add a node into the trie. Mutations are synchronized to keep the trie safe
     * during concurrent indexing; reads remain lock-free.
     */
    public void addNode(SearchNode node) {
        synchronized (this) {
            for (String key : searchableKeys(node)) {
                addKey(key, node);
            }
            allNodes.add(node);
            searchableText.put(node, searchableHaystack(node));
        }
    }

    /**
     * Prefix search: walk the trie to the prefix node then BFS collect results.
     */
    public List<SearchNode> prefixSearch(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        String low = normalizeSearchText(prefix);
        if (low.isEmpty()) return Collections.emptyList();
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
        String low = normalizeSearchText(substring);
        if (low.isEmpty()) return Collections.emptyList();
        List<SearchNode> out = new ArrayList<>();
        for (SearchNode n : allNodes) {
            if (searchableText.getOrDefault(n, n.displayName().toLowerCase(Locale.ROOT)).contains(low)) {
                out.add(n);
            }
        }
        return out;
    }

    private void addKey(String key, SearchNode node) {
        if (key == null || key.isBlank()) return;
        String low = normalizeSearchText(key);
        if (low.isBlank()) return;

        TrieNode cur = root;
        for (int i = 0; i < low.length(); i++) {
            char c = low.charAt(i);
            TrieNode next = cur.children.get(c);
            if (next == null) {
                next = new TrieNode();
                cur.children.put(c, next);
            }
            cur = next;
        }
        cur.hits.add(node);
    }

    private static List<String> searchableKeys(SearchNode node) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add(node.displayName());
        keys.add(node.id().toString());
        keys.add(node.id().getNamespace());
        keys.add(node.id().getPath());

        for (var entry : node.metadata().entrySet()) {
            if (!SEARCHABLE_METADATA_KEYS.contains(entry.getKey())) continue;
            addMetadataAliases(keys, entry.getValue());
        }
        return new ArrayList<>(keys);
    }

    private static String searchableHaystack(SearchNode node) {
        return normalizeSearchText(String.join(" ", searchableKeys(node)));
    }

    private static void addMetadataAliases(Set<String> keys, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return;
        keys.add(rawValue);

        for (String part : rawValue.split("[,:/\\s]+")) {
            if (!part.isBlank()) keys.add(part);
        }

        String path = rawValue;
        int namespaceSep = path.indexOf(':');
        if (namespaceSep >= 0 && namespaceSep + 1 < path.length()) {
            path = path.substring(namespaceSep + 1);
            keys.add(path);
        }
        keys.add(path.replace('_', ' '));
    }

    private static String normalizeSearchText(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .trim();
    }
}
