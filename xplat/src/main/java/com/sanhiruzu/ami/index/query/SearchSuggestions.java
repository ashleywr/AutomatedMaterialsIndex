package com.sanhiruzu.ami.index.query;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.ItemFilter;
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
    private static final List<String> DEFAULT_PROPERTY_FIELDS = List.of(
            "trait", "kind", "material", "role", "capability", "energy", "storage", "fluid", "color"
    );
    private static volatile Cache cache = new Cache(-1L, false, false, false, Vocabulary.empty());

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
            case '$' -> valueSuggestions(vocabulary.categories, text, token, "$", body.substring(1), limit, Kind.CATEGORY);
            case '&' -> valueSuggestions(vocabulary.environments, text, token, "&", body.substring(1), limit, Kind.ENVIRONMENT);
            case '%' -> prefixShortcutSuggestions(vocabulary, text, token, limit);
            case '>', '<', '=' -> numericSuggestions(vocabulary, text, token, prefix, limit);
            default -> List.of();
        };
    }

    public static SearchSyntax.HelpLayout helpLayout(GlobalIndex index) {
        Vocabulary vocabulary = index == null ? Vocabulary.empty() : vocabulary(index);
        List<SearchSyntax.Example> commonExamples = new ArrayList<>(SearchSyntax.HELP_LEFT_SECTIONS.get(0).examples());
        vocabulary.mods.firstValue().ifPresent(mod ->
                commonExamples.add(new SearchSyntax.Example("@" + mod,
                        "ami.gui.search.help.mod", Kind.MOD)));
        vocabulary.tags.firstValue().ifPresent(tag ->
                commonExamples.add(new SearchSyntax.Example("#" + tag,
                        "ami.gui.search.help.tag", Kind.TAG)));
        List<SearchSyntax.HelpSection> leftSections = List.of(
                new SearchSyntax.HelpSection(SearchSyntax.COMMON_SECTION_TITLE_KEY, List.copyOf(commonExamples)));

        List<SearchSyntax.HelpSection> rightSections = new ArrayList<>(SearchSyntax.HELP_RIGHT_SECTIONS);
        List<SearchSyntax.Example> compatExamples = new ArrayList<>();

        vocabulary.pokemonTypes.firstValue().ifPresent(type ->
                compatExamples.add(new SearchSyntax.Example("@type:" + type,
                        "ami.gui.search.help.pokemon_type", Kind.MOD)));
        vocabulary.pokemonMoves.firstValue().ifPresent(move ->
                compatExamples.add(new SearchSyntax.Example("#move:" + move,
                        "ami.gui.search.help.pokemon_move", Kind.TAG)));
        vocabulary.pokemonEggGroups.firstValue().ifPresent(group ->
                compatExamples.add(new SearchSyntax.Example("%egg:" + group,
                        "ami.gui.search.help.pokemon_egg", Kind.PROPERTY)));

        if (!compatExamples.isEmpty()) {
            rightSections.add(new SearchSyntax.HelpSection(SearchSyntax.COMPAT_SECTION_TITLE_KEY,
                    List.copyOf(compatExamples)));
        }

        return new SearchSyntax.HelpLayout(leftSections, List.copyOf(rightSections));
    }

    public static void warm(GlobalIndex index) {
        if (index != null) {
            vocabulary(index);
        }
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
        boolean cheatMode = AmiConfig.cheatMode;
        boolean devMode = AmiConfig.devMode;
        boolean showHidden = AmiConfig.showHiddenModItems;
        Cache cached = cache;
        if (cached.revision == revision
                && cached.cheatMode == cheatMode
                && cached.devMode == devMode
                && cached.showHidden == showHidden) {
            return cached.vocabulary;
        }
        Vocabulary built = Vocabulary.build(index, cheatMode, devMode, showHidden);
        cache = new Cache(revision, cheatMode, devMode, showHidden, built);
        return built;
    }

    private static List<Suggestion> defaultSuggestions(Vocabulary vocabulary, ActiveToken token, int limit) {
        List<Suggestion> suggestions = new ArrayList<>();
        addDefaultPropertyFields(suggestions, vocabulary, token, limit);
        addTopValueSuggestions(suggestions, vocabulary.categories, token, "$", limit, Kind.CATEGORY, 3);
        addTopValueSuggestions(suggestions, vocabulary.mods, token, "@", limit, Kind.MOD, 4);
        return suggestions;
    }

    private static void addDefaultPropertyFields(List<Suggestion> out, Vocabulary vocabulary, ActiveToken token, int limit) {
        for (String field : DEFAULT_PROPERTY_FIELDS) {
            if (out.size() >= limit) {
                return;
            }
            if (valuesForPropertyField(vocabulary, field).isEmpty()) {
                continue;
            }
            String replacement = (token.negated() ? "-?" : "?") + field + ":";
            boolean duplicate = out.stream().anyMatch(existing -> existing.display().equals(replacement));
            if (!duplicate) {
                out.add(new Suggestion(replacement, replacement, "field", token.start(), token.end(),
                        Kind.PROPERTY, true));
            }
        }
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
            List<Suggestion> suggestions = new ArrayList<>();
            addSuggestions(suggestions, fieldSuggestions(vocabulary, query, token, body, limit), limit);
            if (suggestions.isEmpty()) {
                addSuggestions(suggestions, simplePropertySuggestions(vocabulary, query, token, body, limit), limit);
            }
            return suggestions;
        }

        String rawField = body.substring(0, separator);
        String field = SearchSyntax.propertyField(rawField)
                .map(SearchSyntax.PropertyField::id)
                .orElseGet(() -> normalizeField(rawField));
        String prefix = body.substring(separator + 1);
        CountedValues values = valuesForPropertyField(vocabulary, field);
        String propertyPrefix = token.negated() ? "-?" + rawField + ":" : "?" + rawField + ":";
        Kind kind = SearchSyntax.propertyField(rawField).map(SearchSyntax.PropertyField::kind).orElse(Kind.PROPERTY);
        return valueSuggestions(values, query, token, propertyPrefix, prefix, limit, kind);
    }

    private static List<Suggestion> fieldSuggestions(Vocabulary vocabulary, String query, ActiveToken token,
                                                     String prefix, int limit) {
        CountedValues fields = new CountedValues();
        for (SearchSyntax.PropertyField field : SearchSyntax.PROPERTY_FIELDS) {
            if (!valuesForPropertyField(vocabulary, field.id()).isEmpty()) {
                fields.add(field.id());
            }
        }
        return valueSuggestions(fields, query, token, token.negated() ? "-?" : "?", prefix, limit, ":", false, Kind.PROPERTY);
    }

    private static List<Suggestion> simplePropertySuggestions(Vocabulary vocabulary, String query, ActiveToken token,
                                                              String prefix, int limit) {
        CountedValues values = new CountedValues();
        values.addAll(vocabulary.capabilities);
        values.addAll(vocabulary.kinds);
        values.addAll(vocabulary.facts);
        values.addAll(vocabulary.tiers);
        values.addAll(vocabulary.roles);
        return valueSuggestions(values, query, token, token.negated() ? "-?" : "?", prefix, limit, Kind.PROPERTY);
    }

    private static CountedValues valuesForPropertyField(Vocabulary vocabulary, String field) {
        return switch (field) {
            case "energy" -> vocabulary.energy;
            case "fluid" -> vocabulary.fluid;
            case "storage" -> vocabulary.storage;
            case "capability" -> vocabulary.capabilities;
            case "kind" -> vocabulary.kinds;
            case "tier" -> vocabulary.tiers;
            case "role" -> vocabulary.roles;
            case "fact" -> vocabulary.facts;
            case "trait" -> vocabulary.traits;
            case "material" -> vocabulary.materials;
            case "mod" -> vocabulary.mods;
            case "color" -> vocabulary.colors;
            default -> CountedValues.empty();
        };
    }

    private static void addSuggestions(List<Suggestion> out, List<Suggestion> additions, int limit) {
        for (Suggestion suggestion : additions) {
            if (out.size() >= limit) {
                return;
            }
            boolean duplicate = out.stream().anyMatch(existing -> existing.display().equals(suggestion.display()));
            if (!duplicate) {
                out.add(suggestion);
            }
        }
    }

    private static List<Suggestion> numericSuggestions(Vocabulary vocabulary, String query, ActiveToken token,
                                                       char operator, int limit) {
        String body = token.body().substring(1);
        if (body.contains(":")) {
            return List.of();
        }
        return valueSuggestions(vocabulary.numericFields, query, token, Character.toString(operator), body, limit, ":", false, Kind.NUMERIC);
    }

    private static List<Suggestion> prefixShortcutSuggestions(Vocabulary vocabulary, String query, ActiveToken token, int limit) {
        String body = token.body().substring(1);
        if (body.startsWith("egg:")) {
            return valueSuggestions(vocabulary.pokemonEggGroups, query, token, "%egg:", body.substring("egg:".length()), limit, Kind.PROPERTY);
        }
        List<Suggestion> suggestions = new ArrayList<>();
        String normalizedPrefix = normalizeValuePrefix(body);
        for (SearchSyntax.PrefixShortcut shortcut : SearchSyntax.PREFIX_SHORTCUTS) {
            if (suggestions.size() >= limit) {
                break;
            }
            if (!shortcutAvailable(vocabulary, shortcut)) {
                continue;
            }
            if (normalizedPrefix.isEmpty()
                    || normalizeValuePrefix(shortcut.id()).startsWith(normalizedPrefix)
                    || normalizeValuePrefix(shortcut.display()).contains(normalizedPrefix)) {
                suggestions.add(new Suggestion(shortcut.replacement(), shortcut.display(), "",
                        token.start(), token.end(), shortcut.kind(), false));
            }
        }
        return suggestions;
    }

    private static boolean shortcutAvailable(Vocabulary vocabulary, SearchSyntax.PrefixShortcut shortcut) {
        return switch (shortcut.feature()) {
            case POKEMON_TYPES -> !vocabulary.pokemonTypes.isEmpty();
            case POKEMON_MOVES -> !vocabulary.pokemonMoves.isEmpty();
            case POKEMON_EGG_GROUPS -> !vocabulary.pokemonEggGroups.isEmpty();
        };
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
            String display = replacementPrefix + value.value() + (includeCount ? "" : suffix);
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

    private record Cache(long revision, boolean cheatMode, boolean devMode, boolean showHidden, Vocabulary vocabulary) {
    }

    private record ActiveToken(int start, int end, String raw, String body, boolean negated) {
    }

    public enum Kind {
        PLAIN,
        PROPERTY,
        MOD,
        TAG,
        META,
        NUMERIC,
        CATEGORY,
        ENVIRONMENT
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
        final CountedValues traits = new CountedValues();
        final CountedValues materials = new CountedValues();
        final CountedValues capabilities = new CountedValues();
        final CountedValues energy = new CountedValues();
        final CountedValues fluid = new CountedValues();
        final CountedValues storage = new CountedValues();
        final CountedValues colors = new CountedValues();
        final CountedValues categories = new CountedValues();
        final CountedValues environments = new CountedValues();
        final CountedValues pokemonTypes = new CountedValues();
        final CountedValues pokemonMoves = new CountedValues();
        final CountedValues pokemonEggGroups = new CountedValues();
        final CountedValues meta = new CountedValues();
        final CountedValues numericFields = new CountedValues();

        static Vocabulary empty() {
            return new Vocabulary();
        }

        static Vocabulary build(GlobalIndex index, boolean cheatMode, boolean devMode, boolean showHidden) {
            Vocabulary vocabulary = new Vocabulary();
            for (NodeType type : NodeType.values()) {
                for (SearchNode node : index.getNodes(type)) {
                    if (isSuggestionVisible(node, cheatMode, devMode, showHidden)) {
                        vocabulary.add(node, cheatMode || devMode);
                    }
                }
            }
            return vocabulary;
        }

        private static boolean isSuggestionVisible(SearchNode node, boolean cheatMode, boolean devMode, boolean showHidden) {
            if (!devMode && !showHidden && "hidden".equals(node.meta(SearchNodeKeys.VISIBILITY, ""))) {
                return false;
            }
            String level = node.meta(SearchNodeKeys.ACCESS_LEVEL, "");
            if (ItemFilter.ACCESS_DEV.equals(level)) {
                return devMode;
            }
            if (ItemFilter.ACCESS_CHEAT.equals(level)) {
                return cheatMode || devMode;
            }
            return true;
        }

        private void add(SearchNode node, boolean includeDebugTokens) {
            categories.add(AmiOntology.classifyNode(node).id);
            if (node.type() == NodeType.DIMENSION || node.type() == NodeType.BIOME) {
                environments.add(node.id().getPath());
                environments.add(node.displayName().replace(' ', '_'));
            }

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
                if (SearchNodeKeys.COLOR_BUCKET.equals(key)) {
                    addTokens(colors, value);
                    addTokens(meta, value);
                }
                if (SearchNodeKeys.POKEMON_TYPE.equals(key)) {
                    addTokens(pokemonTypes, value);
                    addTokens(meta, value);
                }
                if (SearchNodeKeys.POKEMON_MOVE.equals(key)
                        || SearchNodeKeys.POKEMON_TM_MOVE.equals(key)
                        || SearchNodeKeys.POKEMON_EGG_MOVE.equals(key)
                        || SearchNodeKeys.POKEMON_TUTOR_MOVE.equals(key)
                        || SearchNodeKeys.POKEMON_LEVEL_UP_MOVE.equals(key)) {
                    addTokens(pokemonMoves, value);
                    addTokens(meta, value);
                }
                if (SearchNodeKeys.POKEMON_EGG_GROUPS.equals(key)) {
                    addTokens(pokemonEggGroups, value);
                    addTokens(meta, value);
                }
                if (SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS.equals(key)) {
                    addTokens(traits, value);
                    addTokens(meta, value);
                }
                if (SearchNodeKeys.MODULAR_GEAR_MATERIAL.equals(key)
                        || SearchNodeKeys.MATERIAL_GROUP.equals(key)
                        || SearchNodeKeys.BLOCKS_MATERIAL.equals(key)) {
                    addTokens(materials, value);
                    addTokens(meta, value);
                }
                if (matchesConvention(key, FieldConvention.FACTS)
                        || SearchNodeKeys.FACETS.equals(key)
                        || SearchNodeKeys.COMPONENT_FACTS.equals(key)
                        || (includeDebugTokens && SearchNodeKeys.SEARCH_TOKENS.equals(key))) {
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
            for (String capability : PropertyResolver.indexedCapabilities(node)) {
                capabilities.add(capability);
                switch (capability) {
                    case "energy" -> energy.add(capability);
                    case "fluid" -> fluid.add(capability);
                    case "storage" -> storage.add(capability);
                    default -> {
                    }
                }
                meta.add(capability);
            }
            if (!node.meta(SearchNodeKeys.ENERGY_CAPACITY, "").isBlank()
                    || !node.meta(SearchNodeKeys.ENERGY_GENERATION, "").isBlank()
                    || !node.meta(SearchNodeKeys.ENERGY_CONSUMPTION, "").isBlank()) {
                energy.add("energy");
            }
            if (!node.meta(SearchNodeKeys.FLUID_CAPACITY, "").isBlank()) {
                fluid.add("fluid");
            }
            if (!node.meta(SearchNodeKeys.ESM_CAPACITY, "").isBlank()
                    || !node.meta(SearchNodeKeys.STORAGE_ITEM_KIND, "").isBlank()
                    || !node.meta(SearchNodeKeys.STORAGE_FACTS, "").isBlank()) {
                storage.add("storage");
            }
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

        void addAll(CountedValues values) {
            for (var entry : values.counts.entrySet()) {
                counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        boolean isEmpty() {
            return counts.isEmpty();
        }

        java.util.Optional<String> firstValue() {
            List<ValueCount> values = match("", 1);
            return values.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(values.get(0).value());
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
