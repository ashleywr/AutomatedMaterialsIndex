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

    private final SearchIndex index;

    public LiteralResolver() {
        this(true);
    }

    public LiteralResolver(boolean includeMetadata) {
        this.index = new SearchIndex(includeMetadata);
    }

    /** Called during SearchService.buildFrom() to pre-load the trie. */
    public void addNode(SearchNode node) {
        index.addNode(node);
    }

    @Override
    public Map<NodeType, List<SearchNode>> resolve(String query) {
        String lower = query.toLowerCase();

        Set<SearchNode> prefixHits = new LinkedHashSet<>(index.prefixSearch(lower));
        List<SearchNode> substringHits = new ArrayList<>();
        for (SearchNode node : index.substringSearch(lower)) {
            if (!prefixHits.contains(node)) substringHits.add(node);
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
}
