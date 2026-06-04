package com.sanhiruzu.ami.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Rule-based taxonomy overrides for pack authors and players.
 *
 * Pack profiles live under {@code <gameDir>/ami/taxonomy/*.json}. Player-level
 * overrides live at {@code <configDir>/ami/taxonomy.json}. Pack profiles are
 * loaded first, then the player profile can override category definitions and
 * append later rules.
 */
public final class AmiCustomTaxonomy {
    private static final Object LOCK = new Object();
    private static final String DEFAULT_ICON_ITEM = "minecraft:paper";
    private static final int DEFAULT_COLOR = 0xFF888888;
    private static final String DEFAULT_UNCATEGORIZED_CATEGORY = "uncategorized";
    private static final String DEFAULT_UNCATEGORIZED_SUBCATEGORY = "uncategorized";

    private static volatile State state = State.empty();

    private AmiCustomTaxonomy() {
    }

    public static Path userFile() {
        return Services.PLATFORM.getConfigDir().resolve("ami").resolve("taxonomy.json");
    }

    public static Path packDirectory() {
        return Services.PLATFORM.getGameDir().resolve("ami").resolve("taxonomy");
    }

    public static void reload() {
        synchronized (LOCK) {
            ParsedProfile merged = ParsedProfile.empty();
            for (Path path : packFiles(packDirectory())) {
                merged = merge(merged, read(path, "pack:" + path.getFileName()));
            }
            merged = merge(merged, read(userFile(), "user"));
            state = State.from(merged);
        }
    }

    public static void applyToIndex(GlobalIndex index) {
        if (index == null || state.isEmpty()) {
            return;
        }

        for (NodeType type : NodeType.values()) {
            List<SearchNode> nodes = new ArrayList<>(index.getNodes(type));
            for (SearchNode node : nodes) {
                Map<String, String> updated = apply(node);
                if (!updated.equals(node.metadata())) {
                    index.replaceNode(node.id(), node.type(), node.withMetadata(updated));
                }
            }
        }
    }

    public static Map<String, String> apply(SearchNode node) {
        if (node == null) {
            return Map.of();
        }
        return state.apply(node);
    }

    public static Optional<AmiOntology.Category> definedCategory(String categoryId) {
        return state.definedCategory(categoryId);
    }

    public static List<AmiOntology.Category> definedCategories() {
        return state.definedCategories();
    }

    static ParsedProfile read(Path path, String source) {
        if (path == null || !Files.isRegularFile(path)) {
            return ParsedProfile.empty();
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                return ParsedProfile.empty();
            }

            JsonObject root = parsed.getAsJsonObject();
            Boolean replaceDefaults = booleanValue(root.get("replaceDefaults")).orElse(null);

            Map<String, CategoryDefinition> categories = new LinkedHashMap<>();
            object(root, "categories").ifPresent(rawCategories -> {
                for (String categoryId : rawCategories.keySet()) {
                    parseCategoryDefinition(categoryId, rawCategories.get(categoryId)).ifPresent(definition ->
                            categories.put(definition.id(), definition));
                }
            });

            List<Rule> rules = new ArrayList<>();
            array(root, "rules").ifPresent(rawRules -> {
                for (JsonElement element : rawRules) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    parseRule(element.getAsJsonObject(), source).ifPresent(rules::add);
                }
            });

            return new ParsedProfile(replaceDefaults, Map.copyOf(categories), List.copyOf(rules));
        } catch (IOException | RuntimeException e) {
            AmiCore.LOGGER.warn("AMI: Failed to load custom taxonomy profile from {}", path, e);
            return ParsedProfile.empty();
        }
    }

    private static ParsedProfile merge(ParsedProfile current, ParsedProfile next) {
        if (next.isEmpty()) {
            return current;
        }

        Map<String, CategoryDefinition> categories = new LinkedHashMap<>(current.categories());
        categories.putAll(next.categories());

        List<Rule> rules = new ArrayList<>(current.rules());
        rules.addAll(next.rules());

        Boolean replaceDefaults = next.replaceDefaults() != null
                ? next.replaceDefaults()
                : current.replaceDefaults();

        return new ParsedProfile(replaceDefaults, Map.copyOf(categories), List.copyOf(rules));
    }

    private static Optional<CategoryDefinition> parseCategoryDefinition(String categoryId, JsonElement raw) {
        String normalizedId = normalizeId(categoryId, "custom");
        if (normalizedId.isBlank()) {
            return Optional.empty();
        }

        if (raw != null && raw.isJsonPrimitive()) {
            String label = raw.getAsString().trim();
            return Optional.of(new CategoryDefinition(
                    normalizedId,
                    label.isBlank() ? titleCase(normalizedId) : label,
                    DEFAULT_ICON_ITEM,
                    DEFAULT_COLOR,
                    Map.of()
            ));
        }

        if (raw == null || !raw.isJsonObject()) {
            return Optional.empty();
        }

        JsonObject object = raw.getAsJsonObject();
        String label = stringValue(object.get("label")).orElse(titleCase(normalizedId));
        String iconItemId = stringValue(object.get("iconItem"))
                .or(() -> stringValue(object.get("icon")))
                .orElse(DEFAULT_ICON_ITEM);
        int color = parseColor(object.get("color")).orElse(DEFAULT_COLOR);

        Map<String, SubcategoryDefinition> subcategories = new LinkedHashMap<>();
        object(object, "subcategories").ifPresent(rawSubcategories -> {
            for (String subcategoryId : rawSubcategories.keySet()) {
                parseSubcategoryDefinition(subcategoryId, rawSubcategories.get(subcategoryId)).ifPresent(definition ->
                        subcategories.put(definition.id(), definition));
            }
        });

        return Optional.of(new CategoryDefinition(
                normalizedId,
                label.isBlank() ? titleCase(normalizedId) : label,
                iconItemId,
                color,
                Map.copyOf(subcategories)
        ));
    }

    private static Optional<SubcategoryDefinition> parseSubcategoryDefinition(String subcategoryId, JsonElement raw) {
        String normalizedId = normalizeId(subcategoryId, "custom");
        if (normalizedId.isBlank()) {
            return Optional.empty();
        }

        if (raw != null && raw.isJsonPrimitive()) {
            String label = raw.getAsString().trim();
            return Optional.of(new SubcategoryDefinition(
                    normalizedId,
                    label.isBlank() ? titleCase(normalizedId) : label
            ));
        }

        if (raw == null || !raw.isJsonObject()) {
            return Optional.of(new SubcategoryDefinition(normalizedId, titleCase(normalizedId)));
        }

        JsonObject object = raw.getAsJsonObject();
        String label = stringValue(object.get("label")).orElse(titleCase(normalizedId));
        return Optional.of(new SubcategoryDefinition(normalizedId, label.isBlank() ? titleCase(normalizedId) : label));
    }

    private static Optional<Rule> parseRule(JsonObject rawRule, String source) {
        RuleMatcher matcher = object(rawRule, "match")
                .map(AmiCustomTaxonomy::parseMatcher)
                .orElse(RuleMatcher.matchAll());

        Map<String, String> metadata = readMetadata(rawRule);
        stringValue(rawRule.get("category")).ifPresent(value ->
                metadata.put(SearchNodeKeys.ONTOLOGY_CATEGORY, normalizeId(value, "custom")));
        stringValue(rawRule.get("subcategory")).ifPresent(value ->
                metadata.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, normalizeId(value, "")));

        Set<String> removeMetadata = readStringSet(rawRule.get("removeMetadata"));
        if (metadata.isEmpty() && removeMetadata.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Rule(matcher, Map.copyOf(cleanMetadata(metadata)), Set.copyOf(removeMetadata), source));
    }

    private static RuleMatcher parseMatcher(JsonObject rawMatcher) {
        Optional<NodeType> type = stringValue(rawMatcher.get("type")).flatMap(AmiCustomTaxonomy::parseType);
        return new RuleMatcher(
                type.orElse(null),
                readStringSet(rawMatcher.get("ids")),
                readStringSet(rawMatcher.get("idPrefixes")),
                readStringSet(rawMatcher.get("mods")),
                readStringSet(rawMatcher.get("modIds")),
                readStringSet(rawMatcher.get("paths")),
                readStringSet(rawMatcher.get("pathPrefixes")),
                readStringSet(rawMatcher.get("pathContains")),
                readStringSet(rawMatcher.get("displayNameContains")),
                readStringSet(rawMatcher.get("tags")),
                readStringSet(rawMatcher.get("facets")),
                readMetadataMatcher(rawMatcher)
        );
    }

    private static Map<String, Set<String>> readMetadataMatcher(JsonObject rawMatcher) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        object(rawMatcher, "metadata").ifPresent(metadata -> {
            for (String key : metadata.keySet()) {
                Set<String> values = readStringSet(metadata.get(key));
                if (!values.isEmpty()) {
                    result.put(key.trim(), values);
                }
            }
        });
        return Map.copyOf(result);
    }

    private static Map<String, String> readMetadata(JsonObject rawRule) {
        Map<String, String> metadata = new LinkedHashMap<>();
        object(rawRule, "metadata").ifPresent(rawMetadata -> {
            for (String key : rawMetadata.keySet()) {
                JsonElement value = rawMetadata.get(key);
                if (value != null && value.isJsonPrimitive()) {
                    metadata.put(key, value.getAsString());
                }
            }
        });
        return metadata;
    }

    private static List<Path> packFiles(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return List.of();
        }
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            AmiCore.LOGGER.warn("AMI: Failed to list custom taxonomy profiles in {}", directory, e);
            return List.of();
        }
    }

    private static Optional<JsonObject> object(JsonObject root, String key) {
        JsonElement element = root.get(key);
        return element != null && element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
    }

    private static Optional<JsonArray> array(JsonObject root, String key) {
        JsonElement element = root.get(key);
        return element != null && element.isJsonArray() ? Optional.of(element.getAsJsonArray()) : Optional.empty();
    }

    private static Optional<Boolean> booleanValue(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return Optional.empty();
        }
        try {
            return Optional.of(element.getAsBoolean());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<String> stringValue(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(element.getAsString()).map(String::trim).filter(value -> !value.isBlank());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseColor(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return Optional.empty();
        }
        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return Optional.of(element.getAsInt());
            }
            String raw = element.getAsString().trim();
            if (raw.isBlank()) {
                return Optional.empty();
            }
            String normalized = raw.startsWith("#") ? raw.substring(1) : raw;
            return Optional.of((int) Long.parseLong(normalized, 16));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<NodeType> parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(NodeType.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static Set<String> readStringSet(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        if (element.isJsonPrimitive()) {
            addNormalizedString(result, element.getAsString());
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child.isJsonPrimitive()) {
                    addNormalizedString(result, child.getAsString());
                }
            }
        }
        return Set.copyOf(result);
    }

    private static void addNormalizedString(Set<String> out, String raw) {
        if (raw == null) {
            return;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            out.add(normalized);
        }
    }

    private static Map<String, String> cleanMetadata(Map<String, String> metadata) {
        Map<String, String> cleaned = new LinkedHashMap<>();
        for (var entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank() || value == null) {
                continue;
            }
            cleaned.put(key.trim(), value.trim());
        }
        return cleaned;
    }

    private static String normalizeId(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return normalized.isBlank() ? fallback : normalized;
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Custom";
        }
        String normalized = raw.replace(':', ' ').replace('/', ' ').replace('_', ' ').trim();
        String[] words = normalized.split("\\s+");
        StringBuilder out = new StringBuilder(normalized.length());
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1));
            }
        }
        return out.isEmpty() ? "Custom" : out.toString();
    }

    record ParsedProfile(Boolean replaceDefaults, Map<String, CategoryDefinition> categories, List<Rule> rules) {
        static ParsedProfile empty() {
            return new ParsedProfile(null, Map.of(), List.of());
        }

        boolean isEmpty() {
            return replaceDefaults == null && categories.isEmpty() && rules.isEmpty();
        }
    }

    private record State(boolean replaceDefaults, Map<String, CategoryDefinition> categories, List<Rule> rules) {
        static State empty() {
            return new State(false, Map.of(), List.of());
        }

        static State from(ParsedProfile profile) {
            boolean replaceDefaults = profile.replaceDefaults() != null && profile.replaceDefaults();
            Map<String, CategoryDefinition> categories = profile.categories().isEmpty()
                    ? Map.of()
                    : Map.copyOf(profile.categories());
            List<Rule> rules = profile.rules().isEmpty()
                    ? List.of()
                    : List.copyOf(profile.rules());
            return new State(replaceDefaults, categories, rules);
        }

        boolean isEmpty() {
            return !replaceDefaults && categories.isEmpty() && rules.isEmpty();
        }

        Map<String, String> apply(SearchNode node) {
            Map<String, String> working = node.metadata();
            boolean mutated = false;
            boolean matched = false;

            for (Rule rule : rules) {
                if (!rule.matcher().matches(node, working)) {
                    continue;
                }
                if (!mutated) {
                    working = new LinkedHashMap<>(working);
                    mutated = true;
                }
                matched = true;
                rule.applyTo(working);
            }

            if (!matched && replaceDefaults && node.type() == NodeType.ITEM) {
                if (!mutated) {
                    working = new LinkedHashMap<>(working);
                    mutated = true;
                }
                working.put(SearchNodeKeys.ONTOLOGY_CATEGORY, DEFAULT_UNCATEGORIZED_CATEGORY);
                working.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, DEFAULT_UNCATEGORIZED_SUBCATEGORY);
                working.put(SearchNodeKeys.CUSTOM_TAXONOMY_SOURCE, "replace_defaults");
            }

            return mutated ? Map.copyOf(working) : node.metadata();
        }

        Optional<AmiOntology.Category> definedCategory(String categoryId) {
            if (categoryId == null || categoryId.isBlank()) {
                return Optional.empty();
            }
            CategoryDefinition definition = categories.get(categoryId.trim().toLowerCase(Locale.ROOT));
            return definition == null ? Optional.empty() : Optional.of(definition.toOntologyCategory());
        }

        List<AmiOntology.Category> definedCategories() {
            return categories.values().stream()
                    .map(CategoryDefinition::toOntologyCategory)
                    .toList();
        }
    }

    record CategoryDefinition(String id, String label, String iconItemId, int color,
                              Map<String, SubcategoryDefinition> subcategories) {
        AmiOntology.Category toOntologyCategory() {
            List<AmiOntology.SubCategory> subcategoryList = subcategories.values().stream()
                    .map(SubcategoryDefinition::toOntologySubcategory)
                    .toList();
            return new AmiOntology.Category(id, "", label, iconItemId, color, subcategoryList, List.of());
        }
    }

    record SubcategoryDefinition(String id, String label) {
        AmiOntology.SubCategory toOntologySubcategory() {
            return new AmiOntology.SubCategory(id, label);
        }
    }

    record Rule(RuleMatcher matcher, Map<String, String> metadata, Set<String> removeMetadata, String source) {
        void applyTo(Map<String, String> target) {
            for (String key : removeMetadata) {
                target.remove(key);
            }
            target.putAll(metadata);
            target.put(SearchNodeKeys.CUSTOM_TAXONOMY_SOURCE, source);
        }
    }

    record RuleMatcher(
            NodeType type,
            Set<String> ids,
            Set<String> idPrefixes,
            Set<String> mods,
            Set<String> modIds,
            Set<String> paths,
            Set<String> pathPrefixes,
            Set<String> pathContains,
            Set<String> displayNameContains,
            Set<String> tags,
            Set<String> facets,
            Map<String, Set<String>> metadata
    ) {
        static RuleMatcher matchAll() {
            return new RuleMatcher(null, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                    Set.of(), Set.of(), Set.of(), Map.of());
        }

        boolean matches(SearchNode node, Map<String, String> currentMetadata) {
            if (node == null) {
                return false;
            }
            if (type != null && node.type() != type) {
                return false;
            }

            String id = node.id().toString().toLowerCase(Locale.ROOT);
            String namespace = node.id().getNamespace().toLowerCase(Locale.ROOT);
            String path = node.id().getPath().toLowerCase(Locale.ROOT);
            String displayName = node.displayName().toLowerCase(Locale.ROOT);
            String modId = currentMetadata.getOrDefault(SearchNodeKeys.MOD_ID, namespace).toLowerCase(Locale.ROOT);

            if (!ids.isEmpty() && !ids.contains(id)) {
                return false;
            }
            if (!idPrefixes.isEmpty() && idPrefixes.stream().noneMatch(id::startsWith)) {
                return false;
            }
            if (!mods.isEmpty() && !mods.contains(namespace) && !mods.contains(modId)) {
                return false;
            }
            if (!modIds.isEmpty() && !modIds.contains(namespace) && !modIds.contains(modId)) {
                return false;
            }
            if (!paths.isEmpty() && !paths.contains(path)) {
                return false;
            }
            if (!pathPrefixes.isEmpty() && pathPrefixes.stream().noneMatch(path::startsWith)) {
                return false;
            }
            if (!pathContains.isEmpty() && pathContains.stream().noneMatch(path::contains)) {
                return false;
            }
            if (!displayNameContains.isEmpty() && displayNameContains.stream().noneMatch(displayName::contains)) {
                return false;
            }
            if (!tags.isEmpty() && tags.stream().noneMatch(token ->
                    containsMetadataToken(currentMetadata.get(SearchNodeKeys.TAGS), token)
                            || containsMetadataToken(currentMetadata.get(SearchNodeKeys.BLOCK_TAGS), token))) {
                return false;
            }
            if (!facets.isEmpty() && facets.stream().noneMatch(token ->
                    containsMetadataToken(currentMetadata.get(SearchNodeKeys.FACETS), token))) {
                return false;
            }

            for (var entry : metadata.entrySet()) {
                String raw = currentMetadata.get(entry.getKey());
                if (raw == null || entry.getValue().stream().noneMatch(value -> metadataValueMatches(raw, value))) {
                    return false;
                }
            }

            return true;
        }

        private static boolean metadataValueMatches(String raw, String expected) {
            String normalizedRaw = raw.trim().toLowerCase(Locale.ROOT);
            String normalizedExpected = expected.trim().toLowerCase(Locale.ROOT);
            return normalizedRaw.equals(normalizedExpected) || containsMetadataToken(normalizedRaw, normalizedExpected);
        }

        private static boolean containsMetadataToken(String raw, String expected) {
            if (raw == null || raw.isBlank() || expected == null || expected.isBlank()) {
                return false;
            }
            String normalizedExpected = expected.trim().toLowerCase(Locale.ROOT);
            for (String token : raw.toLowerCase(Locale.ROOT).split("[,\\s]+")) {
                if (token.equals(normalizedExpected)) {
                    return true;
                }
            }
            return false;
        }
    }
}
