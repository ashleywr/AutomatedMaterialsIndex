package com.sanhiruzu.ami.index.resolvers;

import com.sanhiruzu.ami.index.IQueryResolver;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchIndex;
import com.sanhiruzu.ami.index.SearchNode;

import java.util.*;

/**
 * Wraps the trie + substring fallback logic for indexed nodes.
 * Delegates to SearchIndex which is concurrency-friendly and pluggable.
 */
public final class LiteralResolver implements IQueryResolver {
    private static final int SHORT_QUERY_MAX_RESULTS_PER_TYPE = 200;

    private final SearchIndex index;

    public LiteralResolver() {
        this(true);
    }

    public LiteralResolver(boolean includeMetadata) {
        this.index = new SearchIndex(includeMetadata);
    }

    private static boolean shouldUseSubstringFallback(String query) {
        return query.trim().length() >= 3;
    }

    private static boolean shouldCapShortQuery(String query) {
        return query.trim().length() < 3;
    }

    /**
     * Called during SearchService.buildFrom() to pre-load the trie.
     */
    public void addNode(SearchNode node) {
        index.addNode(node);
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        String lower = query.toLowerCase(Locale.ROOT);

        List<SearchNode> prefixHits = index.prefixSearch(lower);
        List<SearchNode> substringHits = Collections.emptyList();
        if (shouldUseSubstringFallback(lower)) {
            // IdentityHashMap: SearchNode instances are canonical within a session — identity
            // comparison is correct and faster than equals()/hashCode().
            Set<SearchNode> prefixSet = Collections.newSetFromMap(new IdentityHashMap<>(prefixHits.size() * 2 + 1));
            prefixSet.addAll(prefixHits);
            substringHits = new ArrayList<>();
            for (SearchNode node : index.substringSearch(lower)) {
                if (!prefixSet.contains(node)) substringHits.add(node);
            }
        }

        // EnumMap + lazy computeIfAbsent: no pre-allocated empty lists per type.
        Map<NodeType, List<SearchNode>> result = new EnumMap<>(NodeType.class);
        for (SearchNode node : prefixHits) {
            result.computeIfAbsent(node.type(), k -> new ArrayList<>()).add(node);
        }
        for (SearchNode node : substringHits) {
            result.computeIfAbsent(node.type(), k -> new ArrayList<>()).add(node);
        }

        // Keep the old cap only for one/two-character probes; longer queries must not
        // drop valid tooltip/token matches.
        if (shouldCapShortQuery(lower)) {
            for (List<SearchNode> list : result.values()) {
                if (list.size() > SHORT_QUERY_MAX_RESULTS_PER_TYPE) {
                    list.subList(SHORT_QUERY_MAX_RESULTS_PER_TYPE, list.size()).clear();
                }
            }
        }

        return result;
    }
}
