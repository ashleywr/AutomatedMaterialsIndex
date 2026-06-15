package com.sanhiruzu.ami.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.AmiCore;
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
 * Runtime reader for common non-Patchouli resource-backed guide books.
 */
public final class ResourceBookRuntimeGuideSource {
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final List<String> GUIDE_TEXT_ROOTS = List.of("book", "books", "guide", "manual");

    private ResourceBookRuntimeGuideSource() {
    }

    public static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
        if (documents == null) {
            return;
        }
        ResourceManager resourceManager = resourceManager();
        ResourceManager clientResources = clientResourceManager();
        String language = GlobalIndexCache.currentClientLanguageCacheKey();
        Map<ResourceLocation, String> textById = readGuideTextResources(resourceManager, clientResources);
        Map<ResourceLocation, String> langById = readGuideLanguageResources(clientResources, language);
        for (AmiGuideDocument document : documentsFromResources(textById, langById, language)) {
            documents.accept(document);
        }
    }

    private static Map<ResourceLocation, String> readGuideTextResources(ResourceManager resourceManager, ResourceManager clientResources) {
        Map<ResourceLocation, String> out = new LinkedHashMap<>();
        if (resourceManager != null) {
            readGuideTextResourcesInto(out, resourceManager);
        }
        if (clientResources != null && clientResources != resourceManager) {
            readGuideTextResourcesInto(out, clientResources);
        }
        return out;
    }

    private static void readGuideTextResourcesInto(Map<ResourceLocation, String> target, ResourceManager resourceManager) {
        for (String root : GUIDE_TEXT_ROOTS) {
            target.putAll(readResources(resourceManager, root, ResourceBookRuntimeGuideSource::isRelevantResource));
        }
    }

    private static Map<ResourceLocation, String> readGuideLanguageResources(ResourceManager resourceManager, String selectedLanguage) {
        Map<ResourceLocation, String> out = new LinkedHashMap<>();
        if (resourceManager == null) {
            return out;
        }
        for (String languagePath : languageRoots(selectedLanguage)) {
            targetLanguageResources(resourceManager, out, languagePath);
        }
        return out;
    }

    private static void targetLanguageResources(ResourceManager resourceManager,
                                               Map<ResourceLocation, String> out,
                                               String languagePath) {
        readResources(resourceManager, languagePath, id -> id.getPath().endsWith(".json"))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> out.put(entry.getKey(), entry.getValue()));
    }

    private static List<String> languageRoots(String selectedLanguage) {
        List<String> out = new ArrayList<>();
        out.add("lang/" + DEFAULT_LANGUAGE);
        String normalized = normalizeLanguage(selectedLanguage);
        if (!DEFAULT_LANGUAGE.equals(normalized)) {
            out.add("lang/" + normalized);
        }
        return out;
    }

    static List<AmiGuideDocument> documentsFromResources(Map<ResourceLocation, String> resourceTextById,
                                                         Map<ResourceLocation, String> langJsonById,
                                                         String selectedLanguage) {
        if (resourceTextById == null || resourceTextById.isEmpty()) {
            return List.of();
        }
        String language = normalizeLanguage(selectedLanguage);
        Map<String, String> translations = translations(langJsonById, language);
        List<AmiGuideDocument> documents = new ArrayList<>();
        documents.addAll(mantleDocuments(resourceTextById, language));
        documents.addAll(alexStyleDocuments(resourceTextById, translations, language));
        documents.addAll(mnaGuideDocuments(resourceTextById, language));
        documents.addAll(immersiveEngineeringDocuments(resourceTextById, language));
        documents.addAll(hexereiDocuments(resourceTextById, translations));
        documents.sort(Comparator.comparing(document -> document.id().toString()));
        return List.copyOf(documents);
    }

    private static List<AmiGuideDocument> mantleDocuments(Map<ResourceLocation, String> resources, String selectedLanguage) {
        Map<ResourceLocation, MantleBookResources> books = new LinkedHashMap<>();
        resources.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> parseMantleResource(entry.getKey()).ifPresent(resource -> {
                    if (!shouldIndexLanguage(resource.language(), selectedLanguage)) {
                        return;
                    }
                    books.computeIfAbsent(resource.bookId(), ignored -> new MantleBookResources())
                            .put(resource, entry.getValue(), selectedLanguage);
                }));

        List<AmiGuideDocument> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, MantleBookResources> entry : books.entrySet()) {
            out.addAll(mantleBookDocuments(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private static List<AmiGuideDocument> mantleBookDocuments(ResourceLocation bookId, MantleBookResources book) {
        List<AmiGuideDocument> out = new ArrayList<>();
        Set<String> emittedPages = new LinkedHashSet<>();
        List<MantleSectionRef> sectionRefs = parseMantleSectionRefs(book.indexJson());
        for (MantleSectionRef sectionRef : sectionRefs) {
            String sectionJson = book.sections().get(mantleResourceKey(sectionRef.data()));
            String chapter = humanize(sectionRef.name());
            for (MantlePageRef pageRef : parseMantlePageRefs(sectionJson)) {
                String pageKey = mantleResourceKey(pageRef.data());
                String pageId = pageRef.name().isBlank() ? pageKey : pageRef.name();
                mantleDocument(bookId, pageId, chapter, book.pages().get(pageKey))
                        .ifPresent(document -> {
                            out.add(document);
                            emittedPages.add(pageKey);
                        });
            }
        }
        for (Map.Entry<String, String> pageEntry : book.pages().entrySet()) {
            String pageKey = pageEntry.getKey();
            if (emittedPages.contains(pageKey) || book.sections().containsKey(pageKey)) {
                continue;
            }
            mantleDocument(bookId, pageKey, humanize(parentPath(pageKey)), pageEntry.getValue())
                    .ifPresent(out::add);
        }
        return out;
    }

    private static Optional<AmiGuideDocument> mantleDocument(ResourceLocation bookId, String pageId,
                                                            String chapter, String pageJson) {
        if (pageJson == null || pageJson.isBlank()) {
            return Optional.empty();
        }
        MantlePage page = parseMantlePage(pageJson, leaf(pageId));
        if (page.summary().isBlank()) {
            return Optional.empty();
        }
        ResourceLocation documentId = ResourceLocation.fromNamespaceAndPath(
                "ami",
                "guide/mantle/" + bookId.getNamespace() + "/" + safePath(bookId.getPath()) + "/" + safePath(pageId)
        );
        return Optional.of(AmiGuideDocument.builder(documentId, "mantle_book", bookId.getNamespace(), page.title())
                .bookId(bookId)
                .iconItemId(bookId)
                .pageId(pageId)
                .chapter(chapter)
                .referencedItems(page.referencedItems())
                .tag("mantle")
                .tag("guide")
                .tag(bookId.getPath())
                .summaryText(page.summary())
                .openAction(AmiGuideOpeners.mantleBook(bookId, pageId))
                .build());
    }

    private static List<AmiGuideDocument> alexStyleDocuments(Map<ResourceLocation, String> resources,
                                                            Map<String, String> translations,
                                                            String selectedLanguage) {
        List<AmiGuideDocument> out = new ArrayList<>();
        Map<ResourceLocation, String> pages = new LinkedHashMap<>();
        for (ResourceLocation id : resources.keySet()) {
            if (isAlexStylePage(id)) {
                pages.put(id, resources.get(id));
            }
        }
        pages.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> alexStyleDocument(entry.getKey(), entry.getValue(), resources, translations, selectedLanguage)
                        .ifPresent(out::add));
        return out;
    }

    private static Optional<AmiGuideDocument> alexStyleDocument(ResourceLocation pageId,
                                                               String pageJson,
                                                               Map<ResourceLocation, String> resources,
                                                               Map<String, String> translations,
                                                               String selectedLanguage) {
        JsonObject json = parseObject(pageJson);
        if (json == null || !json.has("text")) {
            return Optional.empty();
        }
        AlexBook book = alexBook(pageId);
        if (book == null) {
            return Optional.empty();
        }
        String pagePath = alexPagePath(book, pageId);
        if (pagePath.isBlank() || "root".equals(pagePath)) {
            return Optional.empty();
        }
        String title = translated(string(json, "title"), translations, humanize(leaf(pagePath)));
        String textResource = string(json, "text");
        ResourceLocation localizedTextId = ResourceLocation.fromNamespaceAndPath(
                pageId.getNamespace(),
                book.textRoot() + "/" + normalizeLanguage(selectedLanguage) + "/" + normalizeRelativeTextPath(pagePath, textResource)
        );
        String text = resources.get(localizedTextId);
        if (text == null && !DEFAULT_LANGUAGE.equals(normalizeLanguage(selectedLanguage))) {
            localizedTextId = ResourceLocation.fromNamespaceAndPath(
                    pageId.getNamespace(),
                    book.textRoot() + "/" + DEFAULT_LANGUAGE + "/" + normalizeRelativeTextPath(pagePath, textResource)
            );
            text = resources.get(localizedTextId);
        }
        String summary = cleanBookText(text);
        if (summary.isBlank()) {
            return Optional.empty();
        }
        Set<ResourceLocation> referencedItems = new LinkedHashSet<>();
        collectItems(json, referencedItems);
        ResourceLocation documentId = ResourceLocation.fromNamespaceAndPath(
                "ami",
                "guide/resource_book/" + pageId.getNamespace() + "/" + safePath(book.bookId().getPath()) + "/" + safePath(pagePath)
        );
        return Optional.of(AmiGuideDocument.builder(documentId, book.sourceType(), pageId.getNamespace(), title)
                .bookId(book.bookId())
                .iconItemId(book.iconItemId())
                .pageId(pagePath)
                .chapter(humanize(parentPath(pagePath)))
                .referencedItems(List.copyOf(referencedItems))
                .tag("guide")
                .tag(pageId.getNamespace())
                .summaryText(summary)
                .openAction(book.openAction(pagePath))
                .build());
    }

    private static List<AmiGuideDocument> mnaGuideDocuments(Map<ResourceLocation, String> resources, String selectedLanguage) {
        List<AmiGuideDocument> out = new ArrayList<>();
        resources.entrySet().stream()
                .filter(entry -> isMnaGuideResource(entry.getKey(), selectedLanguage))
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> out.addAll(mnaGuideFileDocuments(entry.getKey(), entry.getValue())));
        return out;
    }

    private static List<AmiGuideDocument> mnaGuideFileDocuments(ResourceLocation id, String jsonText) {
        JsonObject json = parseObject(jsonText);
        if (json == null) {
            return List.of();
        }
        String packPath = mnaGuidePackPath(id);
        List<AmiGuideDocument> out = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject guideEntry = entry.getValue().getAsJsonObject();
            String title = firstTitleSection(guideEntry, entry.getKey());
            String category = string(guideEntry, "category");
            String summary = cleanWhitespace(textFromJson(guideEntry.get("sections")));
            if (summary.isBlank()) {
                continue;
            }
            Set<ResourceLocation> referencedItems = new LinkedHashSet<>();
            collectItems(guideEntry, referencedItems);
            String pageId = id.getNamespace() + ":" + packPath + "#" + entry.getKey();
            ResourceLocation documentId = ResourceLocation.fromNamespaceAndPath(
                    "ami",
                    "guide/mna/" + id.getNamespace() + "/" + safePath(packPath) + "/" + safePath(entry.getKey())
            );
            out.add(AmiGuideDocument.builder(documentId, "mna_guide_json", id.getNamespace(), title)
                    .bookId(ResourceLocation.fromNamespaceAndPath("mna", "guide_book"))
                    .iconItemId(ResourceLocation.fromNamespaceAndPath("mna", "guide_book"))
                    .pageId(pageId)
                    .chapter(humanize(category))
                    .referencedItems(List.copyOf(referencedItems))
                    .tag("mna")
                    .tag("guide")
                    .tag(category)
                    .summaryText(summary)
                    .build());
        }
        return out;
    }

    private static List<AmiGuideDocument> immersiveEngineeringDocuments(Map<ResourceLocation, String> resources, String selectedLanguage) {
        List<AmiGuideDocument> out = new ArrayList<>();
        String language = normalizeLanguage(selectedLanguage);
        resources.entrySet().stream()
                .filter(entry -> isIeManualText(entry.getKey(), language) || isIeManualText(entry.getKey(), DEFAULT_LANGUAGE))
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> ieManualDocument(entry.getKey(), entry.getValue()).ifPresent(out::add));
        return out;
    }

    private static Optional<AmiGuideDocument> ieManualDocument(ResourceLocation id, String text) {
        String path = id.getPath();
        String prefix = "manual/";
        int languageEnd = path.indexOf('/', prefix.length());
        if (!path.startsWith(prefix) || languageEnd < 0 || !path.endsWith(".txt")) {
            return Optional.empty();
        }
        String page = path.substring(languageEnd + 1, path.length() - ".txt".length());
        List<String> lines = cleanLines(text);
        if (lines.isEmpty()) {
            return Optional.empty();
        }
        String title = lines.get(0);
        String chapter = lines.size() > 1 ? lines.get(1) : "";
        String summary = String.join("\n", lines.subList(Math.min(2, lines.size()), lines.size()));
        if (summary.isBlank()) {
            summary = String.join("\n", lines);
        }
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath("immersiveengineering", "manual");
        ResourceLocation documentId = ResourceLocation.fromNamespaceAndPath("ami", "guide/ie_manual/" + safePath(page));
        return Optional.of(AmiGuideDocument.builder(documentId, "immersiveengineering_manual", "immersiveengineering", title)
                .bookId(bookId)
                .iconItemId(bookId)
                .pageId(page)
                .chapter(chapter)
                .tag("manual")
                .tag("engineering")
                .summaryText(cleanBookText(summary))
                .build());
    }

    private static List<AmiGuideDocument> hexereiDocuments(Map<ResourceLocation, String> resources,
                                                           Map<String, String> translations) {
        List<AmiGuideDocument> out = new ArrayList<>();
        // Each hexerei book has its root at hexerei:book/{bookname}/{bookname}.json.
        // There is no single aggregate file; scan for individual roots instead.
        for (Map.Entry<ResourceLocation, String> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            if (!"hexerei".equals(id.getNamespace()) || !isHexereiBookRoot(id.getPath())) {
                continue;
            }
            String bookName = hexereiBookName(id.getPath());
            if (bookName.isBlank()) {
                continue;
            }
            ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath("hexerei", bookName);
            out.addAll(hexereiBookDocuments(bookId, entry.getValue(), resources, translations));
        }
        return out;
    }

    private static boolean isHexereiBookRoot(String path) {
        if (!path.startsWith("book/") || !path.endsWith(".json")) {
            return false;
        }
        // Matches book/{name}/{name}.json — root files only, not page files.
        String[] parts = path.split("/");
        return parts.length == 3 && stripJsonExtension(parts[2]).equals(parts[1]);
    }

    private static String hexereiBookName(String path) {
        String[] parts = path.split("/");
        return parts.length >= 2 ? parts[1] : "";
    }

    private static List<AmiGuideDocument> hexereiBookDocuments(ResourceLocation bookId,
                                                               String bookJson,
                                                               Map<ResourceLocation, String> resources,
                                                               Map<String, String> translations) {
        JsonObject bookObject = parseObject(bookJson);
        if (bookObject == null || !(bookObject.get("chapters") instanceof JsonArray chapters)) {
            return List.of();
        }
        List<AmiGuideDocument> out = new ArrayList<>();
        for (JsonElement chapterElement : chapters) {
            if (!chapterElement.isJsonObject()) {
                continue;
            }
            JsonObject chapterObject = chapterElement.getAsJsonObject();
            String chapter = humanize(string(chapterObject, "name"));
            if (!(chapterObject.get("pages") instanceof JsonArray pages)) {
                continue;
            }
            for (JsonElement pageElement : pages) {
                if (!pageElement.isJsonObject()) {
                    continue;
                }
                String pageLocation = string(pageElement.getAsJsonObject(), "page_location");
                ResourceLocation pageId = ResourceLocation.tryParse(pageLocation);
                if (pageId == null) {
                    continue;
                }
                String pageJson = resources.get(ResourceLocation.fromNamespaceAndPath(
                        pageId.getNamespace(),
                        "book/" + pageId.getPath() + ".json"
                ));
                hexereiDocument(bookId, pageId, pageJson, chapter, translations).ifPresent(out::add);
            }
        }
        return out;
    }

    private static Optional<AmiGuideDocument> hexereiDocument(ResourceLocation bookId,
                                                              ResourceLocation pageId,
                                                              String pageJson,
                                                              String chapter,
                                                              Map<String, String> translations) {
        JsonObject page = parseObject(pageJson);
        if (page == null) {
            return Optional.empty();
        }
        List<String> passages = new ArrayList<>();
        // "name" on hexerei pages is the entity/item translation key used as a title.
        String nameKey = string(page, "name");
        if (!nameKey.isBlank()) {
            passages.add(translated(nameKey, translations, nameKey));
        }
        String showTitle = string(page, "showTitle");
        if (!showTitle.isBlank()) {
            passages.add(translated(showTitle, translations, showTitle));
        }
        if (page.get("paragraphs") instanceof JsonArray paragraphs) {
            for (JsonElement element : paragraphs) {
                if (!element.isJsonObject()) {
                    continue;
                }
                String passageKey = string(element.getAsJsonObject(), "passage_text");
                String passage = translated(passageKey, translations, passageKey);
                if (!passage.isBlank()) {
                    passages.add(passage);
                }
            }
        }
        String summary = cleanBookText(String.join("\n", passages));
        if (summary.isBlank()) {
            return Optional.empty();
        }
        Set<ResourceLocation> referencedItems = new LinkedHashSet<>();
        collectItems(page, referencedItems);
        String title = firstNonBlankLine(summary).orElse(humanize(leaf(pageId.getPath())));
        ResourceLocation documentId = ResourceLocation.fromNamespaceAndPath(
                "ami",
                "guide/hexerei/" + safePath(bookId.getPath()) + "/" + safePath(stripJsonExtension(pageId.getPath()))
        );
        return Optional.of(AmiGuideDocument.builder(documentId, "hexerei_book", "hexerei", title)
                .bookId(bookId)
                .iconItemId(bookId)
                .pageId(pageId.toString())
                .chapter(chapter)
                .referencedItems(List.copyOf(referencedItems))
                .tag("hexerei")
                .tag("guide")
                .tag(bookId.getPath())
                .summaryText(summary)
                .openAction(AmiGuideOpeners.hexereiBook(bookId, pageId.toString()))
                .build());
    }

    private static Optional<MantleResource> parseMantleResource(ResourceLocation id) {
        String path = id.getPath().replace('\\', '/');
        if (!path.startsWith("book/") || !path.endsWith(".json")) {
            return Optional.empty();
        }
        String rest = path.substring("book/".length());
        String[] parts = rest.split("/");
        if (parts.length < 2) {
            return Optional.empty();
        }
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), parts[0]);
        if (parts.length == 2 && "index.json".equals(parts[1])) {
            return Optional.of(new MantleResource(bookId, DEFAULT_LANGUAGE, MantleKind.INDEX, ""));
        }
        if (parts.length == 3 && "sections".equals(parts[1])) {
            return Optional.of(new MantleResource(bookId, DEFAULT_LANGUAGE, MantleKind.SECTION, stripJsonExtension(parts[2])));
        }
        if (parts.length >= 3) {
            String language = normalizeLanguage(parts[1]);
            String key = rest.substring((parts[0] + "/" + parts[1] + "/").length());
            return Optional.of(new MantleResource(bookId, language, MantleKind.PAGE, stripJsonExtension(key)));
        }
        return Optional.empty();
    }

    private static List<MantleSectionRef> parseMantleSectionRefs(String jsonText) {
        JsonArray array = parseArray(jsonText);
        if (array == null) {
            return List.of();
        }
        List<MantleSectionRef> out = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String data = string(object, "data");
            if (data.isBlank() || "no-load".equals(data)) {
                continue;
            }
            out.add(new MantleSectionRef(string(object, "name"), data));
        }
        return out;
    }

    private static List<MantlePageRef> parseMantlePageRefs(String jsonText) {
        JsonArray array = parseArray(jsonText);
        if (array == null) {
            return List.of();
        }
        List<MantlePageRef> out = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String data = string(object, "data");
            if (!data.isBlank()) {
                out.add(new MantlePageRef(string(object, "name"), data));
            }
        }
        return out;
    }

    private static MantlePage parseMantlePage(String jsonText, String fallbackTitle) {
        JsonObject object = parseObject(jsonText);
        if (object == null) {
            return new MantlePage(humanize(fallbackTitle), "", List.of());
        }
        String title = string(object, "title");
        if (title.isBlank()) {
            title = humanize(fallbackTitle);
        }
        Set<ResourceLocation> referencedItems = new LinkedHashSet<>();
        collectItems(object, referencedItems);
        String summary = cleanWhitespace(textFromJson(object.get("text")));
        String effects = cleanWhitespace(textFromJson(object.get("effects")));
        if (!effects.isBlank()) {
            summary = summary.isBlank() ? effects : summary + "\n" + effects;
        }
        String properties = cleanWhitespace(textFromJson(object.get("properties")));
        if (!properties.isBlank()) {
            summary = summary.isBlank() ? properties : summary + "\n" + properties;
        }
        return new MantlePage(title, summary, List.copyOf(referencedItems));
    }

    private static boolean isAlexStylePage(ResourceLocation id) {
        String path = id.getPath();
        return (path.startsWith("book/animal_dictionary/") || path.startsWith("books/"))
                && path.endsWith(".json")
                && !path.contains("/en_us/")
                && !path.contains("/lang/")
                && !path.endsWith("/root.json");
    }

    private static AlexBook alexBook(ResourceLocation pageId) {
        String path = pageId.getPath();
        if ("alexsmobs".equals(pageId.getNamespace()) && path.startsWith("book/animal_dictionary/")) {
            return new AlexBook(
                    ResourceLocation.fromNamespaceAndPath("alexsmobs", "animal_dictionary"),
                    ResourceLocation.fromNamespaceAndPath("alexsmobs", "animal_dictionary"),
                    "book/animal_dictionary",
                    "alexsmobs_dictionary"
            );
        }
        if ("alexscaves".equals(pageId.getNamespace()) && path.startsWith("books/")) {
            return new AlexBook(
                    ResourceLocation.fromNamespaceAndPath("alexscaves", "cave_codex"),
                    ResourceLocation.fromNamespaceAndPath("alexscaves", "cave_codex"),
                    "books",
                    "alexscaves_book"
            );
        }
        return null;
    }

    private static String alexPagePath(AlexBook book, ResourceLocation pageId) {
        String path = pageId.getPath();
        String prefix = book.textRoot() + "/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return "";
        }
        return stripJsonExtension(path.substring(prefix.length()));
    }

    private static boolean isMnaGuideResource(ResourceLocation id, String selectedLanguage) {
        String path = id.getPath();
        if (!path.startsWith("guide/") || !path.endsWith(".json")) {
            return false;
        }
        String leaf = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
        String language = normalizeLanguage(leaf);
        return shouldIndexLanguage(language, normalizeLanguage(selectedLanguage));
    }

    private static String mnaGuidePackPath(ResourceLocation id) {
        String path = id.getPath();
        if (!path.startsWith("guide/")) {
            return "";
        }
        String rest = path.substring("guide/".length());
        int slash = rest.lastIndexOf('/');
        return slash < 0 ? id.getNamespace() : rest.substring(0, slash);
    }

    private static boolean isIeManualText(ResourceLocation id, String language) {
        return "immersiveengineering".equals(id.getNamespace())
                && id.getPath().startsWith("manual/" + normalizeLanguage(language) + "/")
                && id.getPath().endsWith(".txt");
    }

    private static boolean isRelevantResource(ResourceLocation id) {
        String path = id.getPath();
        return (path.startsWith("book/") && path.endsWith(".json"))
                || (path.startsWith("book/") && path.endsWith(".txt"))
                || (path.startsWith("books/") && (path.endsWith(".json") || path.endsWith(".txt")))
                || (path.startsWith("guide/") && path.endsWith(".json"))
                || (path.startsWith("manual/") && (path.endsWith(".json") || path.endsWith(".txt")));
    }

    private static Map<ResourceLocation, String> readResources(ResourceManager resourceManager,
                                                               String root,
                                                               java.util.function.Predicate<ResourceLocation> filter) {
        Map<ResourceLocation, String> out = new LinkedHashMap<>();
        if (resourceManager == null) {
            return out;
        }
        Map<ResourceLocation, Resource> listed = safeResourceListing(root, () -> resourceManager.listResources(root, filter));
        listed.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> readResource(entry.getValue()).ifPresent(text -> out.put(entry.getKey(), text)));
        return out;
    }

    static <K, V> Map<K, V> safeResourceListing(String root,
                                                java.util.function.Supplier<Map<K, V>> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            AmiCore.LOGGER.warn("AMI: Skipping resource-backed guide scan for root '{}' after resource enumeration failed: {}",
                    root, e.getMessage());
            return Map.of();
        }
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

    private static Map<String, String> translations(Map<ResourceLocation, String> langJsonById, String selectedLanguage) {
        if (langJsonById == null || langJsonById.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        addTranslations(out, langJsonById, DEFAULT_LANGUAGE);
        addTranslations(out, langJsonById, selectedLanguage);
        return Map.copyOf(out);
    }

    private static void addTranslations(Map<String, String> out,
                                        Map<ResourceLocation, String> langJsonById,
                                        String language) {
        String langPath = "lang/" + normalizeLanguage(language) + ".json";
        langJsonById.entrySet().stream()
                .filter(entry -> normalizeLanguage(entry.getKey().getPath()).equals(normalizeLanguage(langPath)))
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    JsonObject object = parseObject(entry.getValue());
                    if (object == null) {
                        return;
                    }
                    for (Map.Entry<String, JsonElement> translation : object.entrySet()) {
                        if (translation.getValue().isJsonPrimitive()) {
                            out.put(translation.getKey(), translation.getValue().getAsString());
                        }
                    }
                });
    }

    private static void collectItems(JsonElement element, Set<ResourceLocation> out) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element instanceof JsonArray array) {
            for (JsonElement child : array) {
                collectItems(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        for (String key : List.of("item", "name", "entity", "recipe", "recipe_id", "location", "modifier_id", "tool", "fluid")) {
            String value = string(object, key);
            if (value.isBlank()) {
                continue;
            }
            ResourceLocation parsed = ResourceLocation.tryParse(value);
            if (parsed != null) {
                out.add(parsed);
            }
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            collectItems(entry.getValue(), out);
        }
    }

    private static String textFromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element instanceof JsonArray array) {
            List<String> parts = new ArrayList<>();
            for (JsonElement child : array) {
                String text = textFromJson(child);
                if (!text.isBlank()) {
                    parts.add(text);
                }
            }
            return String.join("\n", parts);
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            List<String> parts = new ArrayList<>();
            for (String key : List.of("title", "value", "text")) {
                String value = string(object, key);
                if (!value.isBlank()) {
                    parts.add(value);
                }
            }
            if (object.has("json")) {
                String jsonText = textFromJson(object.get("json"));
                if (!jsonText.isBlank()) {
                    parts.add(jsonText);
                }
            }
            return String.join("\n", parts);
        }
        return "";
    }

    private static String firstTitleSection(JsonObject object, String fallback) {
        JsonElement sections = object.get("sections");
        if (sections instanceof JsonArray array) {
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    JsonObject section = element.getAsJsonObject();
                    if ("title".equals(string(section, "type"))) {
                        String title = string(section, "value");
                        if (!title.isBlank()) {
                            return title;
                        }
                    }
                }
            }
        }
        return fallback;
    }

    private static JsonObject parseObject(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(jsonText);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static JsonArray parseArray(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(jsonText);
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static String translated(String key, Map<String, String> translations, String fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        return translations.getOrDefault(key, key.contains(".") ? fallback : key);
    }

    private static List<String> cleanLines(String text) {
        List<String> out = new ArrayList<>();
        for (String line : cleanBookText(text).split("\\R+")) {
            String clean = line.trim();
            if (!clean.isBlank()) {
                out.add(clean);
            }
        }
        return out;
    }

    private static Optional<String> firstNonBlankLine(String text) {
        for (String line : cleanBookText(text).split("\\R+")) {
            String clean = line.trim();
            if (!clean.isBlank()) {
                return Optional.of(clean);
            }
        }
        return Optional.empty();
    }

    private static String cleanBookText(String text) {
        if (text == null) {
            return "";
        }
        return cleanWhitespace(text
                .replace("<NEWLINE>", "\n")
                .replaceAll("§[0-9A-FK-ORa-fk-or]", "")
                .replaceAll("<[^>]+>", " "));
    }

    private static String cleanWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\r', '\n')
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .trim();
    }

    private static String normalizeRelativeTextPath(String pagePath, String textResource) {
        String text = textResource == null ? "" : textResource.replace('\\', '/').trim();
        if (text.isBlank()) {
            text = leaf(pagePath) + ".txt";
        }
        if (text.contains("/")) {
            return text;
        }
        String parent = parentPath(pagePath);
        return parent.isBlank() ? text : parent + "/" + text;
    }

    private static String normalizeLanguage(String rawLanguage) {
        String language = rawLanguage == null ? "" : rawLanguage.trim().toLowerCase(Locale.ROOT);
        if (language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        language = language.replace('-', '_').replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        return language.isBlank() ? DEFAULT_LANGUAGE : language;
    }

    private static boolean shouldIndexLanguage(String resourceLanguage, String selectedLanguage) {
        return DEFAULT_LANGUAGE.equals(resourceLanguage) || selectedLanguage.equals(resourceLanguage);
    }

    private static String stripJsonExtension(String path) {
        if (path == null) {
            return "";
        }
        return path.endsWith(".json") ? path.substring(0, path.length() - ".json".length()) : path;
    }

    private static String mantleResourceKey(String path) {
        String key = stripJsonExtension(path);
        if (key.startsWith("sections/")) {
            key = key.substring("sections/".length());
        }
        return key;
    }

    private static String parentPath(String path) {
        if (path == null) {
            return "";
        }
        int idx = path.lastIndexOf('/');
        return idx < 0 ? "" : path.substring(0, idx);
    }

    private static String leaf(String path) {
        if (path == null) {
            return "";
        }
        int idx = path.lastIndexOf('/');
        return idx < 0 ? path : path.substring(idx + 1);
    }

    private static String humanize(String value) {
        StringBuilder out = new StringBuilder();
        for (String part : (value == null ? "" : value).split("[_\\-/]+")) {
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
        return out.toString();
    }

    private static String safePath(String value) {
        String path = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\\', '/')
                .replaceAll("[^a-z0-9_./-]", "_")
                .replaceAll("_+", "_");
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path.isBlank() ? "page" : path;
    }

    private static ResourceManager resourceManager() {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server != null) {
            return server.getResourceManager();
        }
        return minecraft.getResourceManager();
    }

    private static ResourceManager clientResourceManager() {
        return net.minecraft.client.Minecraft.getInstance().getResourceManager();
    }

    private enum MantleKind {
        INDEX,
        SECTION,
        PAGE
    }

    private record MantleResource(ResourceLocation bookId, String language, MantleKind kind, String key) {
    }

    private record MantleSectionRef(String name, String data) {
    }

    private record MantlePageRef(String name, String data) {
    }

    private record MantlePage(String title, String summary, List<ResourceLocation> referencedItems) {
    }

    private record LocalizedText(String language, String text) {
    }

    private record AlexBook(ResourceLocation bookId, ResourceLocation iconItemId, String textRoot, String sourceType) {
        private Runnable openAction(String pagePath) {
            if ("alexsmobs_dictionary".equals(sourceType)) {
                return AmiGuideOpeners.alexsMobsAnimalDictionary(pagePath + ".json");
            }
            if ("alexscaves_book".equals(sourceType)) {
                return AmiGuideOpeners.alexsCavesBook(ResourceLocation.fromNamespaceAndPath("alexscaves", pagePath + ".json"));
            }
            return null;
        }
    }

    private static final class MantleBookResources {
        private LocalizedText indexJson;
        private final Map<String, LocalizedText> sections = new LinkedHashMap<>();
        private final Map<String, LocalizedText> pages = new LinkedHashMap<>();

        private void put(MantleResource resource, String text, String selectedLanguage) {
            if (text == null || text.isBlank()) {
                return;
            }
            LocalizedText localized = new LocalizedText(resource.language(), text);
            switch (resource.kind()) {
                case INDEX -> {
                    if (shouldReplace(indexJson, localized, selectedLanguage)) {
                        indexJson = localized;
                    }
                }
                case SECTION -> putLocalized(sections, resource.key(), localized, selectedLanguage);
                case PAGE -> putLocalized(pages, resource.key(), localized, selectedLanguage);
            }
        }

        private String indexJson() {
            return indexJson == null ? "" : indexJson.text();
        }

        private Map<String, String> sections() {
            return localizedValues(sections);
        }

        private Map<String, String> pages() {
            return localizedValues(pages);
        }

        private static void putLocalized(Map<String, LocalizedText> target, String key,
                                         LocalizedText value, String selectedLanguage) {
            LocalizedText existing = target.get(key);
            if (shouldReplace(existing, value, selectedLanguage)) {
                target.put(key, value);
            }
        }

        private static boolean shouldReplace(LocalizedText existing, LocalizedText candidate, String selectedLanguage) {
            if (candidate == null) {
                return false;
            }
            if (existing == null) {
                return true;
            }
            return selectedLanguage.equals(candidate.language()) && !selectedLanguage.equals(existing.language());
        }

        private static Map<String, String> localizedValues(Map<String, LocalizedText> localized) {
            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<String, LocalizedText> entry : localized.entrySet()) {
                out.put(entry.getKey(), entry.getValue().text());
            }
            return out;
        }
    }
}
