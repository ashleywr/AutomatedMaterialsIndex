package com.sanhiruzu.ami.index;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassificationOverrides {
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    private static volatile Map<String, ClassificationOverride> itemOverrides = Map.of();
    private static volatile Map<String, List<ModPatternRule>> modPatternRules = Map.of();
    private static final Set<Integer> WARNED_VERSIONS = ConcurrentHashMap.newKeySet();

    private ClassificationOverrides() {
    }

    public static void install(Map<String, ClassificationOverride> items,
                               Map<String, List<ModPatternRule>> patterns) {
        itemOverrides = Map.copyOf(items);
        modPatternRules = Map.copyOf(patterns);
    }

    public static void clear() {
        itemOverrides = Map.of();
        modPatternRules = Map.of();
        WARNED_VERSIONS.clear();
    }

    private static void warnIfFutureSchemaVersion(JsonObject root) {
        if (!root.has("schemaVersion") || !root.get("schemaVersion").isJsonPrimitive()) {
            return;
        }
        int version;
        try {
            version = root.get("schemaVersion").getAsInt();
        } catch (RuntimeException ignored) {
            return;
        }
        if (version > SUPPORTED_SCHEMA_VERSION && WARNED_VERSIONS.add(version)) {
            org.slf4j.LoggerFactory.getLogger("ami").warn(
                    "Classification override file declares schemaVersion={} but this AMI build supports up to {}; unknown fields will be ignored. Update AMI to pick up newer override features.",
                    version, SUPPORTED_SCHEMA_VERSION);
        }
    }

    public static Optional<ClassificationOverride> forItem(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemOverrides.get(id.toString().toLowerCase(Locale.ROOT)));
    }

    public static Optional<ModPatternRule> patternFor(String modId, String path) {
        return patternFor(modId, path, null);
    }

    public static Optional<ModPatternRule> patternFor(String modId, String path, String itemClass) {
        if (modId == null || path == null) {
            return Optional.empty();
        }
        List<ModPatternRule> rules = modPatternRules.get(modId.toLowerCase(Locale.ROOT));
        if (rules == null) {
            return Optional.empty();
        }
        String[] tokens = path.toLowerCase(Locale.ROOT).split("[_/]");
        String lowerClass = itemClass == null ? "" : itemClass.toLowerCase(Locale.ROOT);

        // Rules with actual criteria (pathTokens/classTokens) always take priority over an
        // unconditional mod-wide wildcard ("match: all" with no criteria), regardless of
        // declaration order — otherwise a blanket rule listed before a narrower exception for
        // the same mod would permanently shadow it.
        for (ModPatternRule rule : rules) {
            if (!isWildcard(rule) && matches(rule, tokens, lowerClass)) {
                return Optional.of(rule);
            }
        }
        for (ModPatternRule rule : rules) {
            if (isWildcard(rule) && matches(rule, tokens, lowerClass)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    private static boolean isWildcard(ModPatternRule rule) {
        return rule.pathTokens().isEmpty() && rule.classTokens().isEmpty();
    }

    private static boolean matches(ModPatternRule rule, String[] pathTokens, String lowerClass) {
        if (rule.pathTokens().isEmpty() && rule.classTokens().isEmpty()) {
            return rule.requiresAllCriteria();
        }
        if (rule.requiresAllCriteria()) {
            return matchesAllCriteria(rule, pathTokens, lowerClass);
        }
        return matchesAnyCriteria(rule, pathTokens, lowerClass);
    }

    private static boolean matchesAllCriteria(ModPatternRule rule, String[] pathTokens, String lowerClass) {
        if (!rule.pathTokens().isEmpty() && !matchesPathToken(rule, pathTokens, true)) {
            return false;
        }
        return rule.classTokens().isEmpty() || matchesClassToken(rule, lowerClass);
    }

    private static boolean matchesAnyCriteria(ModPatternRule rule, String[] pathTokens, String lowerClass) {
        return (!rule.pathTokens().isEmpty() && matchesPathToken(rule, pathTokens, false))
                || (!rule.classTokens().isEmpty() && matchesClassToken(rule, lowerClass));
    }

    private static boolean matchesPathToken(ModPatternRule rule, String[] pathTokens, boolean allowCompoundPathToken) {
        for (String token : pathTokens) {
            for (String ruleToken : rule.pathTokens()) {
                if (token.equals(ruleToken) || (allowCompoundPathToken && token.contains(ruleToken))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesClassToken(ModPatternRule rule, String lowerClass) {
        if (lowerClass.isEmpty()) {
            return false;
        }
        for (String classToken : rule.classTokens()) {
            if (lowerClass.contains(classToken)) {
                return true;
            }
        }
        return false;
    }

    public static void loadBundledDefaults() {
        try (var stream = ClassificationOverrides.class.getClassLoader()
                .getResourceAsStream("assets/ami/classification_overrides.json")) {
            if (stream == null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
            }
            parseAndInstall(sb.toString());
        } catch (RuntimeException | java.io.IOException ignored) {
            // Missing or unreadable defaults must not break indexing.
        }
    }

    public static void mergeAndInstall(String json) {
        Map<String, ClassificationOverride> items = new LinkedHashMap<>(itemOverrides);
        Map<String, List<ModPatternRule>> patterns = new LinkedHashMap<>();
        for (Map.Entry<String, List<ModPatternRule>> e : modPatternRules.entrySet()) {
            patterns.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                warnIfFutureSchemaVersion(root);
                Map<String, ClassificationOverride> newItems = new LinkedHashMap<>();
                Map<String, List<ModPatternRule>> newPatterns = new LinkedHashMap<>();
                parseItems(root, newItems);
                parsePatterns(root, newPatterns);
                items.putAll(newItems);
                for (Map.Entry<String, List<ModPatternRule>> e : newPatterns.entrySet()) {
                    List<ModPatternRule> existing = patterns.computeIfAbsent(e.getKey(), k -> new ArrayList<>());
                    existing.addAll(0, e.getValue());  // prepend: pack patterns win
                }
            }
        } catch (RuntimeException ignored) {
            // Malformed pack override JSON must never break indexing; bundled state is preserved.
        }
        install(items, patterns);
    }

    public static void parseAndInstall(String json) {
        Map<String, ClassificationOverride> items = new LinkedHashMap<>();
        Map<String, List<ModPatternRule>> patterns = new LinkedHashMap<>();
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                warnIfFutureSchemaVersion(root);
                parseItems(root, items);
                parsePatterns(root, patterns);
            }
        } catch (RuntimeException ignored) {
            // Malformed override data must never break indexing; fall back to empty.
        }
        install(items, patterns);
    }

    private static void parseItems(JsonObject root, Map<String, ClassificationOverride> out) {
        if (!root.has("items") || !root.get("items").isJsonObject()) {
            return;
        }
        JsonObject items = root.getAsJsonObject("items");
        for (String id : items.keySet()) {
            if (!items.get(id).isJsonObject()) {
                continue;
            }
            JsonObject entry = items.getAsJsonObject(id);
            String category = optString(entry, "category");
            String subcategory = optString(entry, "subcategory");
            out.put(id.toLowerCase(Locale.ROOT), new ClassificationOverride(
                    parseFacets(entry, "addFacets"),
                    parseFacets(entry, "removeFacets"),
                    parseVerbs(entry, "addVerbs"),
                    parseVerbs(entry, "removeVerbs"),
                    category,
                    subcategory,
                    parseAccessLevel(entry),
                    parseVisibility(entry),
                    parseStringList(entry, "tooltipLines")));
        }
    }

    private static void parsePatterns(JsonObject root, Map<String, List<ModPatternRule>> out) {
        if (!root.has("modPatterns") || !root.get("modPatterns").isJsonArray()) {
            return;
        }
        JsonArray rules = root.getAsJsonArray("modPatterns");
        for (JsonElement element : rules) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String mod = optString(entry, "mod");
            if (mod == null || mod.isBlank()) {
                continue;
            }
            Set<String> tokens = new LinkedHashSet<>();
            if (entry.has("pathTokens") && entry.get("pathTokens").isJsonArray()) {
                for (JsonElement t : entry.getAsJsonArray("pathTokens")) {
                    tokens.add(t.getAsString().toLowerCase(Locale.ROOT));
                }
            }
            Set<String> classTokens = new LinkedHashSet<>();
            if (entry.has("classTokens") && entry.get("classTokens").isJsonArray()) {
                for (JsonElement t : entry.getAsJsonArray("classTokens")) {
                    classTokens.add(t.getAsString().toLowerCase(Locale.ROOT));
                }
            }
            out.computeIfAbsent(mod.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>())
                    .add(new ModPatternRule(mod.toLowerCase(Locale.ROOT), tokens, classTokens,
                            parseFacets(entry, "addFacets"), parseFacets(entry, "removeFacets"),
                            parseVerbs(entry, "addVerbs"), parseVerbs(entry, "removeVerbs"),
                            optString(entry, "category"), optString(entry, "subcategory"),
                            optString(entry, "collapseFamily"), optString(entry, "collapseLabel"),
                            optString(entry, "collapseMode"), parseAccessLevel(entry),
                            parseVisibility(entry), optString(entry, "match")));
        }
    }

    private static java.util.List<String> parseStringList(JsonObject entry, String key) {
        if (!entry.has(key) || !entry.get(key).isJsonArray()) {
            return java.util.List.of();
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (JsonElement el : entry.getAsJsonArray(key)) {
            if (el.isJsonPrimitive()) {
                out.add(el.getAsString());
            }
        }
        return java.util.List.copyOf(out);
    }

    private static EnumSet<ItemFacet> parseFacets(JsonObject entry, String key) {
        EnumSet<ItemFacet> result = EnumSet.noneOf(ItemFacet.class);
        if (entry.has(key) && entry.get(key).isJsonArray()) {
            for (JsonElement element : entry.getAsJsonArray(key)) {
                ItemFacet facet = ItemFacet.byId(element.getAsString().trim().toLowerCase(Locale.ROOT));
                if (facet != null) {
                    result.add(facet);
                }
            }
        }
        return result;
    }

    private static EnumSet<SemanticVerb> parseVerbs(JsonObject entry, String key) {
        EnumSet<SemanticVerb> result = EnumSet.noneOf(SemanticVerb.class);
        if (entry.has(key) && entry.get(key).isJsonArray()) {
            for (JsonElement element : entry.getAsJsonArray(key)) {
                SemanticVerb verb = SemanticVerb.byId(element.getAsString());
                if (verb != null) {
                    result.add(verb);
                }
            }
        }
        return result;
    }

    private static String optString(JsonObject entry, String key) {
        return entry.has(key) && entry.get(key).isJsonPrimitive() ? entry.get(key).getAsString() : null;
    }

    private static String parseAccessLevel(JsonObject entry) {
        String raw = optString(entry, "accessLevel");
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case ItemFilter.ACCESS_SURVIVAL, ItemFilter.ACCESS_CREATIVE, ItemFilter.ACCESS_CHEAT, ItemFilter.ACCESS_DEV -> value;
            default -> null;
        };
    }

    private static String parseVisibility(JsonObject entry) {
        String raw = optString(entry, "visibility");
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "hidden", "visible" -> value;
            default -> null;
        };
    }
}
