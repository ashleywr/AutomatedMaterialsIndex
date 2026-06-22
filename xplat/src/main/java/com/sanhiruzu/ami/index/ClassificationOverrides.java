package com.sanhiruzu.ami.index;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ClassificationOverrides {
    private static volatile Map<String, ClassificationOverride> itemOverrides = Map.of();
    private static volatile Map<String, List<ModPatternRule>> modPatternRules = Map.of();

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
    }

    public static Optional<ClassificationOverride> forItem(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemOverrides.get(id.toString().toLowerCase(Locale.ROOT)));
    }

    public static Optional<ModPatternRule> patternFor(String modId, String path) {
        if (modId == null || path == null) {
            return Optional.empty();
        }
        List<ModPatternRule> rules = modPatternRules.get(modId.toLowerCase(Locale.ROOT));
        if (rules == null) {
            return Optional.empty();
        }
        String[] tokens = path.toLowerCase(Locale.ROOT).split("[_/]");
        for (ModPatternRule rule : rules) {
            for (String token : tokens) {
                if (rule.pathTokens().contains(token)) {
                    return Optional.of(rule);
                }
            }
        }
        return Optional.empty();
    }

    public static void parseAndInstall(String json) {
        Map<String, ClassificationOverride> items = new LinkedHashMap<>();
        Map<String, List<ModPatternRule>> patterns = new LinkedHashMap<>();
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
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
                    category,
                    subcategory));
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
            out.computeIfAbsent(mod.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>())
                    .add(new ModPatternRule(mod.toLowerCase(Locale.ROOT), tokens,
                            optString(entry, "category"), optString(entry, "subcategory")));
        }
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

    private static String optString(JsonObject entry, String key) {
        return entry.has(key) && entry.get(key).isJsonPrimitive() ? entry.get(key).getAsString() : null;
    }
}
