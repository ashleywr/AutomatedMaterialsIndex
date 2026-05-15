package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.index.resolvers.LiteralResolver;
import com.sanhiruzu.ami.index.resolvers.PlayerResolver;

import java.util.*;

/**
 * Search service orchestrating multiple resolvers (Literal trie, Player list, etc).
 * Each resolver contributes results; all are merged by NodeType.
 */
public final class SearchService {
    private final List<IQueryResolver> resolvers;

    private SearchService(List<IQueryResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    /**
     * Build a SearchService from the current GlobalIndex.
     * Constructs a LiteralResolver pre-loaded with all indexed nodes,
     * plus a PlayerResolver for live multiplayer lookups.
     */
    public static SearchService buildFrom(GlobalIndex index) {
        LiteralResolver literal = new LiteralResolver();

        // Pre-load literal resolver with indexed nodes
        for (NodeType type : NodeType.values()) {
            for (SearchNode node : index.getNodes(type)) {
                literal.addNode(node);
            }
        }

        return new SearchService(List.of(literal, new PlayerResolver()));
    }

    /**
     * Query all resolvers, merge results.
     * Resolver order determines priority: earlier resolver's nodes appear first
     * within each NodeType bucket. Duplicate nodes are deduplicated.
     */
    public Map<NodeType, List<SearchNode>> query(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashMap<>();
        }

        String trimmed = text.trim();
        Map<NodeType, List<SearchNode>> merged = new LinkedHashMap<>();
        Set<SearchNode> seen = new HashSet<>();

        for (IQueryResolver resolver : resolvers) {
            Map<NodeType, List<SearchNode>> partial = resolver.resolve(trimmed);
            for (var entry : partial.entrySet()) {
                List<SearchNode> bucket = merged.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                for (SearchNode node : entry.getValue()) {
                    if (seen.add(node)) {
                        bucket.add(node);
                    }
                }
            }
        }

        return merged;
    }
}
