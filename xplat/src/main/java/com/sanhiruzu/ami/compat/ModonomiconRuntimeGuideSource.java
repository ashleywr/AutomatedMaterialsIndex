package com.sanhiruzu.ami.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiGuideOpeners;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Deferred runtime reader for Modonomicon books such as Spectrum's guidebook.
 */
public final class ModonomiconRuntimeGuideSource {
    static final String SOURCE_TYPE = "modonomicon";
    private static final String ROOT = "modonomicon/books";
    private static final String LANG_ROOT = "lang";
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final int SUMMARY_CAP = 4096;

    private ModonomiconRuntimeGuideSource() {
    }

    public static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
        if (documents == null) {
            return;
        }
        ResourceManager resourceManager = resourceManager();
        if (resourceManager == null) {
            return;
        }
        for (AmiGuideDocument document : documentsFromResources(
                readJsonResources(resourceManager),
                readLangResources(resourceManager),
                GlobalIndexCache.currentClientLanguageCacheKey())) {
            documents.accept(document);
        }
    }

    static List<AmiGuideDocument> documentsFromResources(Map<ResourceLocation, String> jsonById,
                                                         Map<ResourceLocation, String> langJsonById,
                                                         String selectedLanguage) {
        if (jsonById == null || jsonById.isEmpty()) {
            return List.of();
        }
        String language = normalizeLanguage(selectedLanguage);
        Map<String, String> translations = translations(langJsonById, language);
        Map<ResourceLocation, BookResources> books = new LinkedHashMap<>();

        jsonById.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> parseResource(entry.getKey()).ifPresent(resource -> {
                    BookResources book = books.computeIfAbsent(resource.bookId(), ignored -> new BookResources());
                    book.put(resource, entry.getValue());
                }));

        List<AmiGuideDocument> documents = new ArrayList<>();
        for (Map.Entry<ResourceLocation, BookResources> entry : books.entrySet()) {
            ResourceLocation bookId = entry.getKey();
            BookResources book = entry.getValue();
            String bookTitle = parseObject(book.bookJson())
                    .flatMap(object -> firstString(object, translations, "name", "title"))
                    .orElse(bookId.toString());
            Map<String, String> categoryLabels = categoryLabels(bookId, book.categoryJsonById(), translations);

            for (Map.Entry<String, String> rawEntry : book.entryJsonById().entrySet()) {
                try {
                    documentFromEntry(bookId, bookTitle, rawEntry.getKey(), rawEntry.getValue(), categoryLabels, translations)
                            .ifPresent(documents::add);
                } catch (RuntimeException ignored) {
                    // A malformed entry should not suppress the rest of the book.
                }
            }
        }

        documents.sort(Comparator.comparing(document -> document.id().toString()));
        return List.copyOf(documents);
    }

    private static Optional<AmiGuideDocument> documentFromEntry(ResourceLocation bookId,
                                                               String bookTitle,
                                                               String entryKey,
                                                               String entryJson,
                                                               Map<String, String> categoryLabels,
                                                               Map<String, String> translations) {
        Optional<JsonObject> maybeObject = parseObject(entryJson);
        if (maybeObject.isEmpty()) {
            return Optional.empty();
        }
        JsonObject object = maybeObject.get();
        String pageId = normalizePathKey(entryKey);
        String title = firstString(object, translations, "name", "title").orElse(humanize(pageId));
        ResourceLocation categoryId = firstString(object, translations, "category")
                .map(value -> entryScopedId(bookId, value))
                .orElse(ResourceLocation.fromNamespaceAndPath(bookId.getNamespace(), categoryFromPath(pageId)));
        ResourceLocation iconItemId = iconItemId(object).orElse(null);
        String chapter = categoryLabels.getOrDefault(categoryId.toString(), humanize(categoryId.getPath()));
        LinkedHashSet<ResourceLocation> referencedItems = new LinkedHashSet<>();
        List<String> summaryParts = new ArrayList<>();

        add(summaryParts, bookTitle);
        add(summaryParts, chapter);
        add(summaryParts, title);
        collectSummaryAndItems(object, translations, summaryParts, referencedItems);

        AmiGuideDocument document = AmiGuideDocument.builder(
                        ResourceLocation.fromNamespaceAndPath("ami",
                                "guide/modonomicon/" + bookId.getNamespace() + "/" + safePath(bookId.getPath()) + "/" + safePath(pageId)),
                        SOURCE_TYPE,
                        bookId.getNamespace(),
                        title)
                .bookId(bookId)
                .iconItemId(iconItemId)
                .pageId(pageId)
                .chapter(chapter)
                .referencedItems(new ArrayList<>(referencedItems))
                .tags(tags(categoryId))
                .summaryText(cap(String.join(" ", summaryParts).trim()))
                .openAction(AmiGuideOpeners.modonomicon(bookId, categoryId, entryScopedId(bookId, pageId), 0))
                .build();
        return Optional.of(document);
    }

    private static Optional<ResourceLocation> iconItemId(JsonObject object) {
        if (object == null) {
            return Optional.empty();
        }
        LinkedHashSet<ResourceLocation> icons = new LinkedHashSet<>();
        collectItem(object.get("icon"), icons);
        return icons.stream().findFirst();
    }

    private static Map<String, String> categoryLabels(ResourceLocation bookId,
                                                      Map<String, String> categoryJsonById,
                                                      Map<String, String> translations) {
        if (categoryJsonById.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : categoryJsonById.entrySet()) {
            ResourceLocation categoryId = entryScopedId(bookId, entry.getKey());
            parseObject(entry.getValue())
                    .flatMap(object -> firstString(object, translations, "name", "title"))
                    .ifPresent(label -> out.put(categoryId.toString(), label));
        }
        return Map.copyOf(out);
    }

    private static void collectSummaryAndItems(JsonElement element,
                                               Map<String, String> translations,
                                               List<String> summaryParts,
                                               Set<ResourceLocation> referencedItems) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element instanceof JsonArray array) {
            for (JsonElement child : array) {
                collectSummaryAndItems(child, translations, summaryParts, referencedItems);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        collectTextFields(object, translations, summaryParts,
                "title", "text", "body", "description", "landing_text", "recipe_id", "recipe_id_1", "recipe_id_2");
        collectItem(object.get("icon"), referencedItems);
        collectItem(object.get("item"), referencedItems);
        collectItem(object.get("items"), referencedItems);
        collectItem(object.get("output"), referencedItems);
        collectItem(object.get("outputs"), referencedItems);
        collectItem(object.get("ingredient"), referencedItems);
        collectItem(object.get("ingredients"), referencedItems);
        JsonElement pages = object.get("pages");
        if (pages != null) {
            collectSummaryAndItems(pages, translations, summaryParts, referencedItems);
        }
    }

    private static void collectItem(JsonElement element, Set<ResourceLocation> referencedItems) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive()) {
            itemId(element.getAsString()).ifPresent(referencedItems::add);
            return;
        }
        if (element instanceof JsonArray array) {
            for (JsonElement child : array) {
                collectItem(child, referencedItems);
            }
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            collectItem(object.get("item"), referencedItems);
            collectItem(object.get("id"), referencedItems);
        }
    }

    private static Optional<ResourceLocation> itemId(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.isEmpty() || value.startsWith("#") || !value.contains(":")) {
            return Optional.empty();
        }
        int cut = firstIndexOf(value, '{', ' ', '\t', '\n', '\r');
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        return parsed == null ? Optional.empty() : Optional.of(parsed);
    }

    private static Optional<String> firstString(JsonObject object, Map<String, String> translations, String... keys) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element != null && element.isJsonPrimitive()) {
                String value = cleanText(resolveTranslation(element.getAsString(), translations));
                if (!value.isEmpty()) {
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }

    private static void collectTextFields(JsonObject object, Map<String, String> translations,
                                          List<String> summaryParts, String... keys) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element != null && element.isJsonPrimitive()) {
                add(summaryParts, resolveTranslation(element.getAsString(), translations));
            }
        }
    }

    private static String resolveTranslation(String value, Map<String, String> translations) {
        String clean = value == null ? "" : value.trim();
        if (clean.isBlank()) {
            return "";
        }
        return translations.getOrDefault(clean, clean);
    }

    private static Map<String, String> translations(Map<ResourceLocation, String> langJsonById, String selectedLanguage) {
        if (langJsonById == null || langJsonById.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        putTranslations(out, langJsonById, DEFAULT_LANGUAGE);
        if (!DEFAULT_LANGUAGE.equals(selectedLanguage)) {
            putTranslations(out, langJsonById, selectedLanguage);
        }
        return Map.copyOf(out);
    }

    private static void putTranslations(Map<String, String> out, Map<ResourceLocation, String> langJsonById, String language) {
        String suffix = LANG_ROOT + "/" + language + ".json";
        langJsonById.entrySet().stream()
                .filter(entry -> entry.getKey().getPath().equals(suffix))
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> parseObject(entry.getValue()).ifPresent(object -> {
                    for (Map.Entry<String, JsonElement> value : object.entrySet()) {
                        if (value.getValue().isJsonPrimitive()) {
                            out.put(value.getKey(), cleanText(value.getValue().getAsString()));
                        }
                    }
                }));
    }

    private static Map<ResourceLocation, String> readJsonResources(ResourceManager resourceManager) {
        Map<ResourceLocation, String> out = new LinkedHashMap<>();
        resourceManager.listResources(ROOT, id -> id.getPath().endsWith(".json"))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> readResource(entry.getValue()).ifPresent(json -> out.put(entry.getKey(), json)));
        return out;
    }

    private static Map<ResourceLocation, String> readLangResources(ResourceManager resourceManager) {
        Map<ResourceLocation, String> out = new LinkedHashMap<>();
        resourceManager.listResources(LANG_ROOT, id -> id.getPath().endsWith(".json"))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> readResource(entry.getValue()).ifPresent(json -> out.put(entry.getKey(), json)));
        return out;
    }

    private static Optional<String> readResource(Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!out.isEmpty()) {
                    out.append('\n');
                }
                out.append(line);
            }
            return Optional.of(out.toString());
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<JsonObject> parseObject(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        JsonElement parsed = JsonParser.parseString(json);
        return parsed.isJsonObject() ? Optional.of(parsed.getAsJsonObject()) : Optional.empty();
    }

    private static Optional<ModonomiconResource> parseResource(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        String path = id.getPath().replace('\\', '/');
        String prefix = ROOT + "/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return Optional.empty();
        }
        String rest = path.substring(prefix.length(), path.length() - ".json".length());
        String[] parts = rest.split("/");
        if (parts.length < 2) {
            return Optional.empty();
        }
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), parts[0]);
        if ("book".equals(parts[1])) {
            return Optional.of(new ModonomiconResource(bookId, ResourceKind.BOOK, ""));
        }
        if (parts.length < 3) {
            return Optional.empty();
        }
        ResourceKind kind = switch (parts[1]) {
            case "categories" -> ResourceKind.CATEGORY;
            case "entries" -> ResourceKind.ENTRY;
            default -> null;
        };
        if (kind == null) {
            return Optional.empty();
        }
        String key = rest.substring((parts[0] + "/" + parts[1] + "/").length());
        return key.isBlank() ? Optional.empty() : Optional.of(new ModonomiconResource(bookId, kind, key));
    }

    private static ResourceLocation entryScopedId(ResourceLocation bookId, String raw) {
        String value = normalizePathKey(raw);
        if (value.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return ResourceLocation.fromNamespaceAndPath(bookId.getNamespace(), value);
    }

    private static String categoryFromPath(String pageId) {
        int slash = pageId.indexOf('/');
        return slash <= 0 ? pageId : pageId.substring(0, slash);
    }

    private static List<String> tags(ResourceLocation categoryId) {
        List<String> tags = new ArrayList<>();
        tags.add(SOURCE_TYPE);
        if (categoryId != null) {
            tags.add(categoryId.getPath());
        }
        return List.copyOf(tags);
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

    private static String normalizeLanguage(String rawLanguage) {
        String language = rawLanguage == null ? "" : rawLanguage.trim().toLowerCase(Locale.ROOT);
        if (language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        language = language.replace('-', '_').replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        return language.isBlank() ? DEFAULT_LANGUAGE : language;
    }

    private static String cleanText(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("$(br)", " ")
                .replaceAll("\\$\\([^)]*\\)", " ")
                .replaceAll("\\{\\d+}", " ")
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
        for (String part : value.split("[_\\-.]+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
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

    private static String cap(String text) {
        return text.length() <= SUMMARY_CAP ? text : text.substring(0, SUMMARY_CAP);
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

    private static ResourceManager resourceManager() {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server != null) {
            return server.getResourceManager();
        }
        return minecraft.getResourceManager();
    }

    private enum ResourceKind {
        BOOK,
        CATEGORY,
        ENTRY
    }

    private record ModonomiconResource(ResourceLocation bookId, ResourceKind kind, String key) {
    }

    private static final class BookResources {
        private String bookJson = "";
        private final Map<String, String> categoryJsonById = new LinkedHashMap<>();
        private final Map<String, String> entryJsonById = new LinkedHashMap<>();

        private void put(ModonomiconResource resource, String json) {
            if (json == null || json.isBlank()) {
                return;
            }
            switch (resource.kind()) {
                case BOOK -> bookJson = json;
                case CATEGORY -> categoryJsonById.put(resource.key(), json);
                case ENTRY -> entryJsonById.put(resource.key(), json);
            }
        }

        private String bookJson() {
            return bookJson;
        }

        private Map<String, String> categoryJsonById() {
            return categoryJsonById;
        }

        private Map<String, String> entryJsonById() {
            return entryJsonById;
        }
    }
}
