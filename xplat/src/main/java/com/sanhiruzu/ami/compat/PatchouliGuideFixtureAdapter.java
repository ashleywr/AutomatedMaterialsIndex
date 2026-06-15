package com.sanhiruzu.ami.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Converts Patchouli-shaped JSON fixtures into AMI guide documents.
 * <p>
 * This intentionally has no Patchouli dependency. Runtime compat can use the
 * same mapping once it has gathered book/category/entry JSON from resources.
 */
public final class PatchouliGuideFixtureAdapter {
    public static final String SOURCE_TYPE = "patchouli";

    private PatchouliGuideFixtureAdapter() {
    }

    public static List<AmiGuideDocument> parse(ResourceLocation bookId,
                                               Map<String, String> categoryJsonById,
                                               Map<String, String> entryJsonById) {
        return parse(bookId, "", categoryJsonById, entryJsonById);
    }

    public static List<AmiGuideDocument> parse(ResourceLocation bookId,
                                               String bookJson,
                                               Map<String, String> categoryJsonById,
                                               Map<String, String> entryJsonById) {
        return parse(bookId, bookJson, categoryJsonById, entryJsonById, Map.of());
    }

    public static List<AmiGuideDocument> parse(ResourceLocation bookId,
                                               String bookJson,
                                               Map<String, String> categoryJsonById,
                                               Map<String, String> entryJsonById,
                                               Map<String, String> translations) {
        if (bookId == null || entryJsonById == null || entryJsonById.isEmpty()) {
            return List.of();
        }

        Map<String, String> safeTranslations = translations == null ? Map.of() : translations;
        Map<String, String> categoryLabels = parseCategoryLabels(bookId, categoryJsonById, safeTranslations);
        Optional<JsonObject> bookObject = parseObject(bookJson);
        String bookTitle = bookObject
                .flatMap(object -> firstString(object, safeTranslations, "name", "title"))
                .orElse(bookId.toString());
        // Fall-back icon for the whole book (used when entries have no per-entry icon).
        ResourceLocation bookFallbackIcon = bookObject
                .flatMap(object -> parseIconItemId(object, "index_icon"))
                .orElse(null);

        List<AmiGuideDocument> documents = new ArrayList<>();
        for (Map.Entry<String, String> entry : entryJsonById.entrySet()) {
            Optional<JsonObject> maybeObject = parseObject(entry.getValue());
            if (maybeObject.isEmpty()) {
                continue;
            }

            JsonObject object = maybeObject.get();
            String pageId = normalizeEntryKey(entry.getKey());
            String title = firstString(object, safeTranslations, "name", "title").orElse(humanize(pageId));
            String categoryKey = firstString(object, safeTranslations, "category")
                    .map(value -> normalizeCategoryReference(bookId, value))
                    .orElse("");
            String chapter = categoryLabels.getOrDefault(categoryKey, categoryKey);
            Set<ResourceLocation> referencedItems = new LinkedHashSet<>();
            List<String> summaryParts = new ArrayList<>();

            add(summaryParts, bookTitle);
            add(summaryParts, chapter);
            add(summaryParts, title);
            collectEntrySummary(object, safeTranslations, summaryParts);
            collectReferencedItems(object, referencedItems);

            ResourceLocation entryIcon = parseIconItemId(object, "icon").orElse(bookFallbackIcon);

            AmiGuideDocument document = AmiGuideDocument.builder(documentId(bookId, pageId),
                            SOURCE_TYPE,
                            bookId.getNamespace(),
                            title)
                    .bookId(bookId)
                    .iconItemId(entryIcon)
                    .pageId(pageId)
                    .chapter(chapter)
                    .referencedItems(new ArrayList<>(referencedItems))
                    .tags(tags(categoryKey))
                    .summaryText(String.join(" ", summaryParts).trim())
                    .build();
            documents.add(document);
        }
        return List.copyOf(documents);
    }

    private static Map<String, String> parseCategoryLabels(ResourceLocation bookId,
                                                           Map<String, String> categoryJsonById,
                                                           Map<String, String> translations) {
        if (categoryJsonById == null || categoryJsonById.isEmpty()) {
            return Map.of();
        }
        Map<String, String> labels = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : categoryJsonById.entrySet()) {
            String key = normalizeCategoryReference(bookId, entry.getKey());
            parseObject(entry.getValue())
                    .flatMap(object -> firstString(object, translations, "name", "title"))
                    .ifPresent(label -> labels.put(key, label));
        }
        return Map.copyOf(labels);
    }

    private static void collectEntrySummary(JsonObject object, Map<String, String> translations, List<String> summaryParts) {
        firstString(object, translations, "description", "landing_text").ifPresent(value -> add(summaryParts, value));
        JsonElement pages = object.get("pages");
        if (pages instanceof JsonArray array) {
            for (JsonElement page : array) {
                if (!page.isJsonObject()) {
                    continue;
                }
                JsonObject pageObject = page.getAsJsonObject();
                firstString(pageObject, translations, "title").ifPresent(value -> add(summaryParts, value));
                firstString(pageObject, translations, "text").ifPresent(value -> add(summaryParts, value));
                firstString(pageObject, translations, "body").ifPresent(value -> add(summaryParts, value));
                firstString(pageObject, translations, "description").ifPresent(value -> add(summaryParts, value));
            }
        }
    }

    private static void collectReferencedItems(JsonObject object, Set<ResourceLocation> referencedItems) {
        collectReferencedItems(object.get("icon"), referencedItems);
        JsonElement pages = object.get("pages");
        if (pages instanceof JsonArray array) {
            for (JsonElement page : array) {
                collectPageItems(page, referencedItems);
            }
        }
    }

    private static void collectPageItems(JsonElement element, Set<ResourceLocation> referencedItems) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            collectReferencedItems(object.get("item"), referencedItems);
            collectReferencedItems(object.get("items"), referencedItems);
            collectReferencedItems(object.get("output"), referencedItems);
            collectReferencedItems(object.get("outputs"), referencedItems);
            collectReferencedItems(object.get("ingredient"), referencedItems);
            collectReferencedItems(object.get("ingredients"), referencedItems);
        }
    }

    private static void collectReferencedItems(JsonElement element, Set<ResourceLocation> referencedItems) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive()) {
            itemId(element.getAsString()).ifPresent(referencedItems::add);
            return;
        }
        if (element instanceof JsonArray array) {
            for (JsonElement child : array) {
                collectReferencedItems(child, referencedItems);
            }
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            collectReferencedItems(object.get("item"), referencedItems);
            collectReferencedItems(object.get("id"), referencedItems);
        }
    }

    private static Optional<ResourceLocation> parseIconItemId(JsonObject object, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonElement el = object.get(fieldName);
            if (el != null && el.isJsonPrimitive()) {
                Optional<ResourceLocation> id = itemId(el.getAsString());
                if (id.isPresent()) {
                    return id;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<ResourceLocation> itemId(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.isEmpty() || value.startsWith("#")) {
            return Optional.empty();
        }
        int cut = firstIndexOf(value, '{', ' ', '\t', '\n', '\r');
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        return parsed == null ? Optional.empty() : Optional.of(parsed);
    }

    private static Optional<JsonObject> parseObject(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        JsonElement parsed = JsonParser.parseString(json);
        return parsed.isJsonObject() ? Optional.of(parsed.getAsJsonObject()) : Optional.empty();
    }

    private static Optional<String> firstString(JsonObject object, String... keys) {
        return firstString(object, Map.of(), keys);
    }

    private static Optional<String> firstString(JsonObject object, Map<String, String> translations, String... keys) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element != null && element.isJsonPrimitive()) {
                String raw = element.getAsString();
                String value = cleanText(resolveText(raw, translations));
                if (!value.isEmpty()) {
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }

    private static String resolveText(String raw, Map<String, String> translations) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String translated = translations.get(raw);
        if (translated != null && !translated.isBlank()) {
            return translated;
        }
        if (!looksLikeTranslationKey(raw)) {
            return raw;
        }
        return readableTranslationFallback(raw);
    }

    private static boolean looksLikeTranslationKey(String raw) {
        String value = raw.trim();
        return value.startsWith("item.")
                || value.startsWith("block.")
                || value.startsWith("book.")
                || value.startsWith("entity.")
                || value.startsWith("advancements.")
                || (value.indexOf(':') < 0
                && value.contains(".")
                && value.matches("[a-z0-9_.-]+"));
    }

    private static String readableTranslationFallback(String raw) {
        String[] parts = raw.split("\\.");
        if (parts.length == 0) {
            return humanize(raw);
        }
        int index = parts.length - 1;
        String last = parts[index];
        if (isGenericTranslationLeaf(last) && index > 0) {
            index--;
        }
        if (("item".equals(parts[0]) || "block".equals(parts[0]) || "entity".equals(parts[0])) && parts.length > 2) {
            index = parts.length - 1;
        }
        while (index > 0 && isGenericTranslationLeaf(parts[index])) {
            index--;
        }
        return humanize(parts[Math.max(0, index)]);
    }

    private static boolean isGenericTranslationLeaf(String value) {
        return switch (value) {
            case "name", "title", "text", "description", "landing", "landing_text", "entries", "categories", "pages" -> true;
            default -> false;
        };
    }

    private static List<String> tags(String categoryKey) {
        List<String> tags = new ArrayList<>();
        tags.add(SOURCE_TYPE);
        add(tags, categoryKey);
        return List.copyOf(tags);
    }

    private static ResourceLocation documentId(ResourceLocation bookId, String pageId) {
        return ResourceLocation.fromNamespaceAndPath("ami",
                "guide/patchouli/" + bookId.getNamespace() + "/" + safePath(bookId.getPath()) + "/" + safePath(pageId));
    }

    private static String normalizeEntryKey(String key) {
        String value = normalizePathKey(key);
        int entries = value.indexOf("entries/");
        if (entries >= 0) {
            value = value.substring(entries + "entries/".length());
        }
        return value.isEmpty() ? "entry" : value;
    }

    private static String normalizeCategoryReference(ResourceLocation bookId, String raw) {
        String value = normalizePathKey(raw);
        if (value.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(value);
            if (parsed != null && !parsed.getNamespace().equals(bookId.getNamespace())) {
                return parsed.toString();
            }
            if (parsed != null) {
                value = parsed.getPath();
            }
        }
        int categories = value.indexOf("categories/");
        if (categories >= 0) {
            value = value.substring(categories + "categories/".length());
        }
        return value;
    }

    private static String normalizePathKey(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().replace('\\', '/');
        if (value.endsWith(".json")) {
            value = value.substring(0, value.length() - ".json".length());
        }
        return value;
    }

    private static String cleanText(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\$\\([^)]*\\)", " ")
                .replace("$(br)", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String humanize(String raw) {
        String value = normalizePathKey(raw);
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        StringBuilder out = new StringBuilder();
        for (String part : value.split("[_\\-]+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                out.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.isEmpty() ? "Entry" : out.toString();
    }

    private static String safePath(String raw) {
        String cleaned = normalizePathKey(raw).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9/._-]", "_")
                .replaceAll("/+", "/");
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isEmpty() ? "entry" : cleaned;
    }

    private static void add(List<String> values, String value) {
        String cleaned = cleanText(value);
        if (!cleaned.isEmpty() && !values.contains(cleaned)) {
            values.add(cleaned);
        }
    }

    private static int firstIndexOf(String value, char... candidates) {
        int result = -1;
        for (char candidate : candidates) {
            int index = value.indexOf(candidate);
            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }
        return result;
    }
}
