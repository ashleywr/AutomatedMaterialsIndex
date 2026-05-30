package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.index.query.QueryParser;
import com.sanhiruzu.ami.index.resolvers.*;

import java.util.*;

/**
 * Search service orchestrating multiple resolvers (Literal trie, Player list, etc).
 * Each resolver contributes results; all are merged by NodeType.
 */
public final class SearchService {
    private final List<IQueryResolver> resolvers;
    private final List<IQueryResolver> broadResolvers;
    private final TagResolver tagResolver;
    private final ModResolver modResolver;
    private final EnvironmentResolver envResolver;
    private final NumericMetadataResolver numericResolver;
    private final PropertyResolver propertyResolver;
    private final CategoryResolver categoryResolver;

    private SearchService(List<IQueryResolver> resolvers, List<IQueryResolver> broadResolvers,
                          TagResolver tagResolver, ModResolver modResolver,
                          EnvironmentResolver envResolver, NumericMetadataResolver numericResolver,
                          PropertyResolver propertyResolver, CategoryResolver categoryResolver) {
        this.resolvers = List.copyOf(resolvers);
        this.broadResolvers = List.copyOf(broadResolvers);
        this.tagResolver = tagResolver;
        this.modResolver = modResolver;
        this.envResolver = envResolver;
        this.numericResolver = numericResolver;
        this.propertyResolver = propertyResolver;
        this.categoryResolver = categoryResolver;
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
        LiteralResolver literal = new LiteralResolver(false);
        LiteralResolver broadLiteral = new LiteralResolver(true);
        TagResolver tagResolver = new TagResolver();
        ModResolver modResolver = new ModResolver();
        EnvironmentResolver envResolver = new EnvironmentResolver();
        NumericMetadataResolver numericResolver = new NumericMetadataResolver();
        PropertyResolver propertyResolver = new PropertyResolver();
        CategoryResolver categoryResolver = new CategoryResolver();

        // Pre-load all resolvers with indexed nodes
        for (NodeType type : NodeType.values()) {
            for (SearchNode node : index.getNodes(type)) {
                literal.addNode(node);
                broadLiteral.addNode(node);
                tagResolver.addNode(node);
                modResolver.addNode(node);
                envResolver.addNode(node);
                numericResolver.addNode(node);
                propertyResolver.addNode(node);
                categoryResolver.addNode(node);
            }
        }

        List<IQueryResolver> resolvers = new ArrayList<>();
        resolvers.add(literal);
        List<IQueryResolver> broadResolvers = new ArrayList<>();
        broadResolvers.add(broadLiteral);
        if (includePlayers) {
            resolvers.add(new PlayerResolver());
            broadResolvers.add(new PlayerResolver());
        }

        return new SearchService(resolvers, broadResolvers, tagResolver, modResolver, envResolver, numericResolver, propertyResolver, categoryResolver);
    }

    private static void record(QueryTrace trace, String operation, String value, Map<NodeType, List<SearchNode>> results) {
        if (trace == null) return;
        trace.steps.add(new QueryStep(operation, value, countsByType(results)));
    }

    private static Map<NodeType, Integer> countsByType(Map<NodeType, List<SearchNode>> results) {
        Map<NodeType, Integer> counts = new LinkedHashMap<>();
        for (var entry : results.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }

    /**
     * Query resolvers using UQL tokens if prefixes are present, else fallback to literal search.
     * Handles INCLUDE, TAG, ENV, EXCLUDE tokens.
     * Results are deduplicated and exclusions are applied.
     */
    public Map<NodeType, List<SearchNode>> query(String text) {
        if (text != null && text.contains("|")) {
            Map<NodeType, List<SearchNode>> combined = new LinkedHashMap<>();
            for (String branch : text.split("\\|", -1)) {
                String trimmed = branch.trim();
                if (!trimmed.isEmpty()) {
                    mergeResults(combined, queryInternal(trimmed, null));
                }
            }
            return combined;
        }
        return queryInternal(text, null);
    }

    public QueryExplanation explain(String text) {
        QueryTrace trace = new QueryTrace(text);
        Map<NodeType, List<SearchNode>> results = queryInternal(text, trace);
        return new QueryExplanation(text, List.copyOf(trace.tokens), List.copyOf(trace.steps), countsByType(results));
    }

    private Map<NodeType, List<SearchNode>> queryInternal(String text, QueryTrace trace) {
        if (text == null || text.isBlank()) {
            return new LinkedHashMap<>();
        }

        String trimmed = text.trim();
        QueryParser.ParsedQuery parsed = QueryParser.parse(trimmed);
        if (trace != null) {
            for (QueryParser.QueryToken token : parsed.tokens()) {
                trace.tokens.add(token.type() + ":" + token.value());
            }
        }

        Map<NodeType, List<SearchNode>> results = new LinkedHashMap<>();
        Set<SearchNode> excluded = new HashSet<>();
        boolean hasActiveResultSet = false;

        if (parsed.tokens().isEmpty()) {
            return new LinkedHashMap<>();
        }

        List<String> includeParts = new ArrayList<>();
        List<String> metaParts = new ArrayList<>();
        List<String> excludeParts = new ArrayList<>();
        List<String> tagParts = new ArrayList<>();
        List<String> modParts = new ArrayList<>();
        List<String> envParts = new ArrayList<>();
        List<String> propertyParts = new ArrayList<>();
        List<String> numericParts = new ArrayList<>();
        List<String> categoryParts = new ArrayList<>();

        for (QueryParser.QueryToken token : parsed.tokens()) {
            String value = token.value();

            switch (token.type()) {
                case INCLUDE -> includeParts.add(value);
                case META -> metaParts.add(value);
                case TAG -> tagParts.add(value);
                case MOD -> modParts.add(value);
                case ENV -> envParts.add(value);
                case PROP -> propertyParts.add(value);
                case EXCLUDE -> excludeParts.add(value);
                case ESM -> numericParts.add(value);
                case CATEGORY -> categoryParts.add(value);
                default -> {
                } // ESSENTIAL is reserved for curated result sets.
            }
        }

        // Plain INCLUDE parts: resolve each word independently then intersect,
        // so "potion heal" matches "Potion of Healing" even without adjacency.
        if (!includeParts.isEmpty()) {
            Map<NodeType, List<SearchNode>> includeResults = null;
            for (String part : includeParts) {
                Map<NodeType, List<SearchNode>> wordResults = new LinkedHashMap<>();
                for (IQueryResolver resolver : resolvers) {
                    Map<NodeType, List<SearchNode>> partial = resolver.resolve(part);
                    record(trace, "include:" + resolver.getClass().getSimpleName(), part, partial);
                    mergeResults(wordResults, partial);
                }
                if (includeResults == null) {
                    includeResults = wordResults;
                } else {
                    intersectResults(includeResults, wordResults);
                }
            }
            if (includeResults != null) {
                mergeResults(results, includeResults);
            }
            hasActiveResultSet = true;
            record(trace, "after-include", String.join(" ", includeParts), results);
        }

        // META parts opt into the broader metadata-inclusive surface.
        if (!metaParts.isEmpty()) {
            Map<NodeType, List<SearchNode>> metaResults = new LinkedHashMap<>();
            String combinedMeta = String.join(" ", metaParts);
            for (IQueryResolver resolver : broadResolvers) {
                Map<NodeType, List<SearchNode>> partial = resolver.resolve(combinedMeta);
                record(trace, "meta:" + resolver.getClass().getSimpleName(), combinedMeta, partial);
                mergeResults(metaResults, partial);
            }
            hasActiveResultSet = applyPositiveFilter(results, metaResults, hasActiveResultSet);
            record(trace, "after-meta", combinedMeta, results);
        }

        for (String tagPart : tagParts) {
            Map<NodeType, List<SearchNode>> filter = tagResolver.resolve(tagPart);
            record(trace, "tag", tagPart, filter);
            hasActiveResultSet = applyPositiveFilter(results, filter, hasActiveResultSet);
            record(trace, "after-tag", tagPart, results);
        }
        for (String modPart : modParts) {
            Map<NodeType, List<SearchNode>> filter = modResolver.resolve(modPart);
            record(trace, "mod", modPart, filter);
            hasActiveResultSet = applyPositiveFilter(results, filter, hasActiveResultSet);
            record(trace, "after-mod", modPart, results);
        }
        for (String envPart : envParts) {
            Map<NodeType, List<SearchNode>> filter = envResolver.resolve(envPart);
            record(trace, "env", envPart, filter);
            hasActiveResultSet = applyPositiveFilter(results, filter, hasActiveResultSet);
            record(trace, "after-env", envPart, results);
        }
        for (String propertyPart : propertyParts) {
            Map<NodeType, List<SearchNode>> filter = propertyResolver.resolve(propertyPart);
            record(trace, "property", propertyPart, filter);
            hasActiveResultSet = applyPositiveFilter(results, filter, hasActiveResultSet);
            record(trace, "after-property", propertyPart, results);
        }
        for (String numericPart : numericParts) {
            Map<NodeType, List<SearchNode>> filter = numericResolver.resolve(numericPart);
            record(trace, "numeric", numericPart, filter);
            hasActiveResultSet = applyPositiveFilter(results, filter, hasActiveResultSet);
            record(trace, "after-numeric", numericPart, results);
        }
        for (String categoryPart : categoryParts) {
            Map<NodeType, List<SearchNode>> filter = categoryResolver.resolve(categoryPart);
            record(trace, "category", categoryPart, filter);
            hasActiveResultSet = applyPositiveFilter(results, filter, hasActiveResultSet);
            record(trace, "after-category", categoryPart, results);
        }

        for (String excludePart : excludeParts) {
            Map<NodeType, List<SearchNode>> partial = resolveExclude(excludePart);
            record(trace, "exclude", excludePart, partial);
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
            record(trace, "after-exclude", String.join(" ", excludeParts), results);
        }

        return results;
    }

    private Map<NodeType, List<SearchNode>> resolveExclude(String excludePart) {
        if (excludePart.startsWith("$")) {
            return categoryResolver.resolve(excludePart.substring(1));
        }
        if (excludePart.startsWith("~")) {
            return resolveBroadLiteral(excludePart.substring(1));
        }
        if (excludePart.startsWith("#")) {
            return tagResolver.resolve(excludePart.substring(1));
        }
        if (excludePart.startsWith("@")) {
            return modResolver.resolve(excludePart.substring(1));
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

    private Map<NodeType, List<SearchNode>> resolveBroadLiteral(String query) {
        Map<NodeType, List<SearchNode>> results = new LinkedHashMap<>();
        for (IQueryResolver resolver : broadResolvers) {
            Map<NodeType, List<SearchNode>> partial = resolver.resolve(query);
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

    public record QueryStep(String operation, String value, Map<NodeType, Integer> counts) {
    }

    public record QueryExplanation(String input, List<String> tokens, List<QueryStep> steps,
                                   Map<NodeType, Integer> finalCounts) {
    }

    private static final class QueryTrace {
        final String input;
        final List<String> tokens = new ArrayList<>();
        final List<QueryStep> steps = new ArrayList<>();

        QueryTrace(String input) {
            this.input = input;
        }
    }
}
