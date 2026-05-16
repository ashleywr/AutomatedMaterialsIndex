package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.index.query.QueryParser;
import com.sanhiruzu.ami.index.resolvers.EnvironmentResolver;
import com.sanhiruzu.ami.index.resolvers.LiteralResolver;
import com.sanhiruzu.ami.index.resolvers.NumericMetadataResolver;
import com.sanhiruzu.ami.index.resolvers.PlayerResolver;
import com.sanhiruzu.ami.index.resolvers.TagResolver;

import java.util.*;

/**
 * Search service orchestrating multiple resolvers (Literal trie, Player list, etc).
 * Each resolver contributes results; all are merged by NodeType.
 */
public final class SearchService {
    private final List<IQueryResolver> resolvers;
    private final TagResolver tagResolver;
    private final EnvironmentResolver envResolver;
    private final NumericMetadataResolver numericResolver;

    private SearchService(List<IQueryResolver> resolvers, TagResolver tagResolver, EnvironmentResolver envResolver,
                          NumericMetadataResolver numericResolver) {
        this.resolvers = List.copyOf(resolvers);
        this.tagResolver = tagResolver;
        this.envResolver = envResolver;
        this.numericResolver = numericResolver;
    }

    /**
     * Build a SearchService from the current GlobalIndex.
     * Constructs LiteralResolver, TagResolver, EnvironmentResolver pre-loaded with indexed nodes,
     * plus a PlayerResolver for live multiplayer lookups.
     */
    public static SearchService buildFrom(GlobalIndex index) {
        return buildFrom(index, true);
    }

    public static SearchService buildFrom(GlobalIndex index, boolean includePlayers) {
        LiteralResolver literal = new LiteralResolver();
        TagResolver tagResolver = new TagResolver();
        EnvironmentResolver envResolver = new EnvironmentResolver();
        NumericMetadataResolver numericResolver = new NumericMetadataResolver();

        // Pre-load all resolvers with indexed nodes
        for (NodeType type : NodeType.values()) {
            for (SearchNode node : index.getNodes(type)) {
                literal.addNode(node);
                tagResolver.addNode(node);
                envResolver.addNode(node);
                numericResolver.addNode(node);
            }
        }

        List<IQueryResolver> resolvers = new ArrayList<>();
        resolvers.add(literal);
        if (includePlayers) {
            resolvers.add(new PlayerResolver());
        }

        return new SearchService(resolvers, tagResolver, envResolver, numericResolver);
    }

    /**
     * Query resolvers using UQL tokens if prefixes are present, else fallback to literal search.
     * Handles INCLUDE, TAG, ENV, EXCLUDE tokens.
     * Results are deduplicated and exclusions are applied.
     */
    public Map<NodeType, List<SearchNode>> query(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashMap<>();
        }

        String trimmed = text.trim();
        QueryParser.ParsedQuery parsed = QueryParser.parse(trimmed);
        Map<NodeType, List<SearchNode>> results = new LinkedHashMap<>();
        Set<SearchNode> excluded = new HashSet<>();

        if (parsed.tokens().isEmpty()) {
            return new LinkedHashMap<>();
        }

        // Separate tokens by type
        List<String> includeParts = new ArrayList<>();
        List<String> excludeParts = new ArrayList<>();
        List<String> numericParts = new ArrayList<>();

        for (QueryParser.QueryToken token : parsed.tokens()) {
            String value = token.value();

            switch (token.type()) {
                case INCLUDE -> includeParts.add(value);
                case TAG -> {
                    Map<NodeType, List<SearchNode>> tagResults = tagResolver.resolve(value);
                    mergeResults(results, tagResults);
                }
                case ENV -> {
                    Map<NodeType, List<SearchNode>> envResults = envResolver.resolve(value);
                    mergeResults(results, envResults);
                }
                case EXCLUDE -> excludeParts.add(value);
                case ESM -> numericParts.add(value);
                default -> {} // PROP, ESSENTIAL handled in Phase 3
            }
        }

        // If we have INCLUDE parts, query literal resolver for them
        if (!includeParts.isEmpty()) {
            String combinedInclude = String.join(" ", includeParts);
            for (IQueryResolver resolver : resolvers) {
                Map<NodeType, List<SearchNode>> partial = resolver.resolve(combinedInclude);
                mergeResults(results, partial);
            }
        }

        // If we have EXCLUDE parts, resolve them and collect to exclude
        if (!excludeParts.isEmpty()) {
            String combinedExclude = String.join(" ", excludeParts);
            for (IQueryResolver resolver : resolvers) {
                Map<NodeType, List<SearchNode>> partial = resolver.resolve(combinedExclude);
                for (var entry : partial.entrySet()) {
                    excluded.addAll(entry.getValue());
                }
            }
        }

        if (!numericParts.isEmpty()) {
            boolean hadPriorResults = !results.isEmpty();
            for (String numericPart : numericParts) {
                Map<NodeType, List<SearchNode>> numericResults = numericResolver.resolve(numericPart);
                if (hadPriorResults || !results.isEmpty()) {
                    intersectResults(results, numericResults);
                } else {
                    mergeResults(results, numericResults);
                }
            }
        }

        // Apply exclusions
        if (!excluded.isEmpty()) {
            for (var entry : results.entrySet()) {
                entry.getValue().removeAll(excluded);
            }
            // Remove empty type buckets
            results.entrySet().removeIf(e -> e.getValue().isEmpty());
        }

        return results;
    }

    private void mergeResults(Map<NodeType, List<SearchNode>> dest, Map<NodeType, List<SearchNode>> src) {
        Set<SearchNode> seen = new HashSet<>();
        for (var entry : dest.values()) {
            seen.addAll(entry);
        }

        for (var entry : src.entrySet()) {
            List<SearchNode> bucket = dest.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
            for (SearchNode node : entry.getValue()) {
                if (seen.add(node)) {
                    bucket.add(node);
                }
            }
        }
    }

    private void intersectResults(Map<NodeType, List<SearchNode>> dest, Map<NodeType, List<SearchNode>> filter) {
        Set<SearchNode> allowed = new HashSet<>();
        for (List<SearchNode> nodes : filter.values()) {
            allowed.addAll(nodes);
        }

        for (List<SearchNode> nodes : dest.values()) {
            nodes.removeIf(node -> !allowed.contains(node));
        }
        dest.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
