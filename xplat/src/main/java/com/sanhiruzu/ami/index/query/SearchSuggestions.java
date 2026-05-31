package com.sanhiruzu.ami.index.query;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.resolvers.NumericMetadataResolver;
import com.sanhiruzu.ami.index.resolvers.PropertyResolver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds search completions from the active AMI index instead of from a fixed
 * mod list. The only static pieces here are query-language fields; values come
 * from currently indexed node metadata and therefore follow datapack/modpack
 * changes automatically.
 */
public final class SearchSuggestions {
    private static final int DEFAULT_LIMIT = 24;
    private static volatile Cache cache = new Cache(-1L, Vocabulary.empty());

    private SearchSuggestions() {
    }

    public static List<Suggestion> suggest(GlobalIndex index, String query, int cursorPosition) {
        return suggest(index, query, cursorPosition, DEFAULT_LIMIT);
    }

    public static List<Suggestion> suggest(GlobalIndex index, String query, int cursorPosition, int limit) {
        if (index == null || limit <= 0) {
            return List.of();
        }
        String text = query == null ? "" : query;
        int cursor = Math.max(0, Math.min(cursorPosition, text.length()));
        ActiveToken token = activeToken(text, cursor);
        Vocabulary vocabulary = vocabulary(index);

        if (token.raw().isBlank()) {
            return defaultSuggestions(vocabulary, token, limit);
        }

        String body = token.body();
        if (body.isBlank()) {
            return defaultSuggestions(vocabulary, token, limit);
        }

        char prefix = body.charAt(0);
        return switch (prefix) {
            case '?' -> propertySuggestions(vocabulary, text, token, limit);
            case '@' -> valueSuggestions(vocabulary.mods, text, token, "@", body.substring(1), limit, Kind.MOD);
            case '#' -> valueSuggestions(vocabulary.tags, text, token, "#", body.substring(1), limit, Kind.TAG);
            case '~' -> valueSuggestions(vocabulary.meta, text, token, "~", body.substring(1), limit, Kind.META);
            case '>', '<', '=' -> numericSuggestions(vocabulary, text, token, prefix, limit);
            default -> List.of();
        };
    }

    public static String apply(String query, Suggestion suggestion) {
        if (suggestion == null) {
            return query == null ? "" : query;
        }
        String text = query == null ? "" : query;
        int start = Math.max(0, Math.min(suggestion.replaceStart(), text.length()));
        int end = Math.max(start, Math.min(suggestion.replaceEnd(), text.length()));
        if (suggestion.replacement().endsWith(" ") && end < text.length() && Character.isWhitespace(text.charAt(end))) {
            end++;
        }
        return text.substring(0, start) + suggestion.replacement() + text.substring(end);
    }

    private static Vocabulary vocabulary(GlobalIndex index) {
        long revision = index.revision();
        Cache cached = cache;
        if (cached.revision == revision) {
            return cached.vocabulary;
        }
        Vocabulary built = Vocabulary.build(index);
        cache = new Cache(revision, built);
        return built;
    }

    private static List<Suggestion> defaultSuggestions(Vocabulary vocabulary, ActiveToken token, int limit) {
        List<Suggestion> suggestions = new ArrayList<>();
        addTopValueSuggestions(suggestions, vocabulary.capabilities, token, "?capability:", limit, Kind.PROPERTY, 2);
        addTopValueSuggestions(suggestions, vocabulary.kinds, token, "?kind:", limit, Kind.PROPERTY, 3);
        addTopValueSuggestions(suggestions, vocabulary.facts, token, "?fact:", limit, Kind.PROPERTY, 4);
        addTopValueSuggestions(suggestions, vocabulary.tiers, token, "?tier:", limit, Kind.PROPERTY, 2);
        addTopValueSuggestions(suggestions, vocabulary.roles, token, "?role:", limit, Kind.PROPERTY, 3);
        addTopValueSuggestions(suggestions, vocabulary.mods, token, "@", limit, Kind.MOD, 3);
        addTopValueSuggestions(suggestions, vocabulary.tags, token, "#", limit, Kind.TAG, 2);
        return suggestions;
    }

    private static void addTopValueSuggestions(List<Suggestion> out, CountedValues values, ActiveToken token,
                                               String prefix, int limit, Kind kind, int perCategoryLimit) {
        if (out.size() >= limit) return;
        List<ValueCount> top = values.match("", Math.max(1, perCategoryLimit));
        if (top.isEmpty()) {
            return;
        }
        for (ValueCount value : top) {
            if (out.size() >= limit) return;
            String replacement = prefix + value.value() + " ";
            out.add(new Suggestion(replacement, prefix + value.value(), "example", token.start(), token.end(), kind, true));
        }
    }

    private static List<Suggestion> propertySuggestions(Vocabulary vocabulary, String query, ActiveToken token, int limit) {
        String body = token.body().substring(1);
        int separator = body.indexOf(':');
        if (separator < 0) {
            return fieldSuggestions(vocabulary, query, token, body, limit);
        }

        String field = normalizeField(body.substring(0, separator));
        String prefix = body.substring(separator + 1);
        CountedValues values = switch (field) {
            case "capability", "cap", "resource" -> vocabulary.capabilities;
            case "kind", "itemkind" -> vocabulary.kinds;
            case "tier" -> vocabulary.tiers;
            case "role", "recipe", "processing", "process" -> vocabulary.roles;
            case "fact", "facts", "behavior", "behaviour" -> vocabulary.facts;
            case "mod", "modid", "compat", "family", "ecosystem", "compatfamily", "compatfamilies" -> vocabulary.mods;
            default -> CountedValues.empty();
        };
        String propertyPrefix = token.negated() ? "-?" + body.substring(0, separator) + ":" : "?" + body.substring(0, separator) + ":";
        Kind kind = field.equals("mod") || field.equals("modid") || field.equals("compat") || field.equals("family")
                || field.equals("ecosystem") || field.equals("compatfamily") || field.equals("compatfamilies")
                ? Kind.MOD
                : Kind.PROPERTY;
        return valueSuggestions(values, query, token, propertyPrefix, prefix, limit, kind);
    }

    private static List<Suggestion> fieldSuggestions(Vocabulary vocabulary, String query, ActiveToken token,
                                                     String prefix, int limit) {
        CountedValues fields = new CountedValues();
        if (!vocabulary.capabilities.isEmpty()) fields.add("capability");
        if (!vocabulary.kinds.isEmpty()) fields.add("kind");
        if (!vocabulary.facts.isEmpty()) fields.add("fact");
        if (!vocabulary.tiers.isEmpty()) fields.add("tier");
        if (!vocabulary.roles.isEmpty()) fields.add("role");
        if (!vocabulary.mods.isEmpty()) fields.add("mod");
        return valueSuggestions(fields, query, token, token.negated() ? "-?" : "?", prefix, limit, ":", false, Kind.PROPERTY);
    }

    private static List<Suggestion> numericSuggestions(Vocabulary vocabulary, String query, ActiveToken token,
                                                       char operator, int limit) {
        String body = token.body().substring(1);
        if (body.contains(":")) {
            return List.of();
        }
        return valueSuggestions(vocabulary.numericFields, query, token, Character.toString(operator), body, limit, ":", false, Kind.NUMERIC);
    }

    private static List<Suggestion> valueSuggestions(CountedValues values, String query, ActiveToken token,
                                                     String replacementPrefix, String typedValue, int limit, Kind kind) {
        return valueSuggestions(values, query, token, replacementPrefix, typedValue, limit, " ", true, kind);
    }

    private static List<Suggestion> valueSuggestions(CountedValues values, String query, ActiveToken token,
                                                     String replacementPrefix, String typedValue, int limit,
                                                     String suffix, boolean includeCount, Kind kind) {
        String normalizedPrefix = normalizeValuePrefix(typedValue);
        List<ValueCount> matches = values.match(normalizedPrefix, limit);
        if (matches.isEmpty()) {
            return List.of();
        }
        List<Suggestion> suggestions = new ArrayList<>(matches.size());
        for (ValueCount value : matches) {
            String replacement = replacementPrefix + value.value() + suffix;
            String display = replacementPrefix + value.value();
            suggestions.add(new Suggestion(replacement, display, includeCount ? countDetail(value.count()) : "",
                    token.start(), token.end(), kind, false));
        }
        return suggestions;
    }

    private static ActiveToken activeToken(String query, int cursor) {
        int start = cursor;
        while (start > 0 && !Character.isWhitespace(query.charAt(start - 1))) {
            start--;
        }
        int end = cursor;
        while (end < query.length() && !Character.isWhitespace(query.charAt(end))) {
            end++;
        }
        String raw = query.substring(start, end);
        boolean negated = raw.startsWith("-");
        String body = negated ? raw.substring(1) : raw;
        return new ActiveToken(start, end, raw, body, negated);
    }

    private static String normalizeField(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").trim();
    }

    private static String normalizeValuePrefix(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(' ', '_').trim();
    }

    private static String countDetail(int count) {
        return Integer.toString(count);
    }

    private record Cache(long revision, Vocabulary vocabulary) {
    }

    private record ActiveToken(int start, int end, String raw, String body, boolean negated) {
    }

    public enum Kind {
        PLAIN,
        PROPERTY,
        MOD,
        TAG,
        META,
        NUMERIC
    }

    public record Suggestion(String replacement, String display, String detail, int replaceStart, int replaceEnd,
                             Kind kind, boolean example) {
        public Suggestion(String replacement, String display, String detail, int replaceStart, int replaceEnd) {
            this(replacement, display, detail, replaceStart, replaceEnd, Kind.PLAIN, false);
        }

        public int cursorAfterApply() {
            return replaceStart + replacement.length();
        }
    }

    private record ValueCount(String value, int count) {
    }

    private static final class Vocabulary {
        final CountedValues mods = new CountedValues();
        final CountedValues tags = new CountedValues();
        final CountedValues facts = new CountedValues();
        final CountedValues kinds = new CountedValues();
        final CountedValues tiers = new CountedValues();
        final CountedValues roles = new CountedValues();
        final CountedValues capabilities = new CountedValues();
        final CountedValues meta = new CountedValues();
        final CountedValues numericFields = new CountedValues();

        static Vocabulary empty() {
            return new Vocabulary();
        }

        static Vocabulary build(GlobalIndex index) {
            Vocabulary vocabulary = new Vocabulary();
            for (NodeType type : NodeType.values()) {
                for (SearchNode node : index.getNodes(type)) {
                    vocabulary.add(node);
                }
            }
            return vocabulary;
        }

        private void add(SearchNode node) {
            mods.add(node.id().getNamespace());
            addTokens(mods, node.meta(SearchNodeKeys.MOD_ID, ""));
            addTokens(mods, node.meta(SearchNodeKeys.COMPAT_FAMILY, ""));
            addTokens(mods, node.meta(SearchNodeKeys.COMPAT_FAMILIES, ""));
            addTokens(mods, node.meta(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, ""));

            addTokens(tags, node.meta(SearchNodeKeys.TAGS, ""));
            addTokens(tags, node.meta(SearchNodeKeys.BLOCK_TAGS, ""));

            for (var entry : node.metadata().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (matchesConvention(key, FieldConvention.FACTS)
                        || SearchNodeKeys.FACETS.equals(key)
                        || SearchNodeKeys.COMPONENT_FACTS.equals(key)
                        || SearchNodeKeys.SEARCH_TOKENS.equals(key)) {
                    addTokens(facts, value);
                    addTokens(meta, value);
                }
                if (matchesConvention(key, FieldConvention.KIND)
                        || SearchNodeKeys.ONTOLOGY_SUBCATEGORY.equals(key)) {
                    addTokens(kinds, value);
                    addTokens(meta, value);
                }
                if (matchesConvention(key, FieldConvention.TIER)) {
                    addTokens(tiers, value);
                    addTokens(meta, value);
                }
                if (matchesConvention(key, FieldConvention.ROLE)
                        || SearchNodeKeys.RECIPE_CATEGORIES.equals(key)
                        || SearchNodeKeys.RECIPE_USE_CATEGORIES.equals(key)) {
                    addTokens(roles, value);
                    addTokens(meta, value);
                }
                NumericMetadataResolver.aliasesForMetadataKey(key).forEach(numericFields::add);
            }
            PropertyResolver.indexedCapabilities(node).forEach(capabilities::add);
        }
    }

    private static void addTokens(CountedValues values, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }
        for (String part : rawValue.split("[,\\s]+")) {
            String token = cleanToken(part);
            if (!token.isBlank()) {
                values.add(token);
            }
        }
    }

    private static String cleanToken(String raw) {
        String token = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        while (token.startsWith("#")) {
            token = token.substring(1);
        }
        return token;
    }

    private static boolean matchesConvention(String key, FieldConvention convention) {
        String normalized = normalizeField(key);
        return switch (convention) {
            case FACTS -> normalized.endsWith("facts");
            case KIND -> normalized.endsWith("itemkind");
            case TIER -> normalized.endsWith("tier");
            case ROLE -> normalized.endsWith("role") || normalized.endsWith("roles");
        };
    }

    private enum FieldConvention {
        FACTS,
        KIND,
        TIER,
        ROLE
    }

    private static final class CountedValues {
        private final Map<String, Integer> counts = new LinkedHashMap<>();

        static CountedValues empty() {
            return new CountedValues();
        }

        void add(String value) {
            String cleaned = cleanToken(value);
            if (cleaned.isBlank()) {
                return;
            }
            counts.merge(cleaned, 1, Integer::sum);
        }

        boolean isEmpty() {
            return counts.isEmpty();
        }

        List<ValueCount> match(String prefix, int limit) {
            String normalizedPrefix = normalizeValuePrefix(prefix);
            return counts.entrySet().stream()
                    .filter(entry -> normalizedPrefix.isEmpty()
                            || normalizeValuePrefix(entry.getKey()).startsWith(normalizedPrefix)
                            || normalizeValuePrefix(entry.getKey()).contains(normalizedPrefix))
                    .sorted(Comparator
                            .<Map.Entry<String, Integer>>comparingInt(entry -> matchRank(entry.getKey(), normalizedPrefix))
                            .thenComparing(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed())
                            .thenComparing(Map.Entry::getKey))
                    .limit(limit)
                    .map(entry -> new ValueCount(entry.getKey(), entry.getValue()))
                    .toList();
        }

        private static int matchRank(String value, String prefix) {
            if (prefix.isEmpty()) {
                return 0;
            }
            String normalized = normalizeValuePrefix(value);
            return normalized.startsWith(prefix) ? 0 : 1;
        }
    }
}
