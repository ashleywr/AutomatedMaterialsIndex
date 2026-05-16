package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.index.query.QueryParser;
import com.sanhiruzu.ami.index.resolvers.EnvironmentResolver;
import com.sanhiruzu.ami.index.resolvers.LiteralResolver;
import com.sanhiruzu.ami.index.resolvers.NumericMetadataResolver;
import com.sanhiruzu.ami.index.resolvers.PlayerResolver;
import com.sanhiruzu.ami.index.resolvers.PropertyResolver;
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
    private final PropertyResolver propertyResolver;

    private SearchService(List<IQueryResolver> resolvers, TagResolver tagResolver, EnvironmentResolver envResolver,
                          NumericMetadataResolver numericResolver, PropertyResolver propertyResolver) {
        this.resolvers = List.copyOf(resolvers);
        this.tagResolver = tagResolver;
        this.envResolver = envResolver;
        this.numericResolver = numericResolver;
        this.propertyResolver = propertyResolver;
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
        PropertyResolver propertyResolver = new PropertyResolver();

        // Pre-load all resolvers with indexed nodes
        for (NodeType type : NodeType.values()) {
            for (SearchNode node : index.getNodes(type)) {
                literal.addNode(node);
                tagResolver.addNode(node);
                envResolver.addNode(node);
                numericResolver.addNode(node);
                propertyResolver.addNode(node);
            }
        }

        List<IQueryResolver> resolvers = new ArrayList<>();
        resolvers.add(literal);
        if (includePlayers) {
            resolvers.add(new PlayerResolver());
        }

        return new SearchService(resolvers, tagResolver, envResolver, numericResolver, propertyResolver);
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
        boolean hasActiveResultSet = false;

        if (parsed.tokens().isEmpty()) {
            return new LinkedHashMap<>();
        }

        List<String> includeParts = new ArrayList<>();
        List<String> excludeParts = new ArrayList<>();
        List<String> tagParts = new ArrayList<>();
        List<String> envParts = new ArrayList<>();
        List<String> propertyParts = new ArrayList<>();
        List<String> numericParts = new ArrayList<>();

        for (QueryParser.QueryToken token : parsed.tokens()) {
            String value = token.value();

            switch (token.type()) {
                case INCLUDE -> includeParts.add(value);
                case TAG -> tagParts.add(value);
                case ENV -> envParts.add(value);
                case PROP -> propertyParts.add(value);
                case EXCLUDE -> excludeParts.add(value);
                case ESM -> numericParts.add(value);
                default -> {} // ESSENTIAL is reserved for curated result sets.
            }
        }

        // If we have INCLUDE parts, query literal resolver for them
        if (!includeParts.isEmpty()) {
            String combinedInclude = String.join(" ", includeParts);
            for (IQueryResolver resolver : resolvers) {
                Map<NodeType, List<SearchNode>> partial = resolver.resolve(combinedInclude);
                mergeResults(results, partial);
            }
            hasActiveResultSet = true;
        }

        for (String tagPart : tagParts) {
            hasActiveResultSet = applyPositiveFilter(results, tagResolver.resolve(tagPart), hasActiveResultSet);
        }
        for (String envPart : envParts) {
            hasActiveResultSet = applyPositiveFilter(results, envResolver.resolve(envPart), hasActiveResultSet);
        }
        for (String propertyPart : propertyParts) {
            hasActiveResultSet = applyPositiveFilter(results, propertyResolver.resolve(propertyPart), hasActiveResultSet);
        }
        for (String numericPart : numericParts) {
            hasActiveResultSet = applyPositiveFilter(results, numericResolver.resolve(numericPart), hasActiveResultSet);
        }

        for (String excludePart : excludeParts) {
            Map<NodeType, List<SearchNode>> partial = resolveExclude(excludePart);
            for (var entry : partial.entrySet()) {
                excluded.addAll(entry.getValue());
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

    private Map<NodeType, List<SearchNode>> resolveExclude(String excludePart) {
        if (excludePart.startsWith("#")) {
            return tagResolver.resolve(excludePart.substring(1));
        }
        if (excludePart.startsWith("&")) {
            return envResolver.resolve(excludePart.substring(1));
        }
        if (excludePart.startsWith("?")) {
            return propertyResolver.resolve(excludePart.substring(1));
        }

        Map<NodeType, List<SearchNode>> results = new LinkedHashMap<>();
        for (IQueryResolver resolver : resolvers) {
            Map<NodeType, List<SearchNode>> partial = resolver.resolve(excludePart);
            mergeResults(results, partial);
        }
        return results;
    }

    private boolean applyPositiveFilter(Map<NodeType, List<SearchNode>> results, Map<NodeType, List<SearchNode>> filter,
                                        boolean hasActiveResultSet) {
        if (!hasActiveResultSet) {
            mergeResults(results, filter);
        } else {
            intersectResults(results, filter);
        }
        return true;
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
