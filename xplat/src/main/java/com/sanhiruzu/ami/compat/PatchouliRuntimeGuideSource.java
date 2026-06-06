package com.sanhiruzu.ami.compat;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Deferred runtime reader for Patchouli guide resources.
 * <p>
 * This source intentionally runs outside the main item indexing pass because
 * guide books can contain a large amount of text and page metadata.
 */
public final class PatchouliRuntimeGuideSource {
    private static final String ROOT = "patchouli_books";
    private static final String DEFAULT_LANGUAGE = "en_us";

    private PatchouliRuntimeGuideSource() {
    }

    public static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
        if (documents == null) {
            return;
        }
        ResourceManager resourceManager = resourceManager();
        if (resourceManager == null) {
            return;
        }
        Map<ResourceLocation, String> resourceJson = readPatchouliResourceJson(resourceManager);
        for (AmiGuideDocument document : documentsFromResources(resourceJson, GlobalIndexCache.currentClientLanguageCacheKey())) {
            documents.accept(document);
        }
    }

    static List<AmiGuideDocument> documentsFromResources(Map<ResourceLocation, String> resourceJsonById,
                                                         String selectedLanguage) {
        if (resourceJsonById == null || resourceJsonById.isEmpty()) {
            return List.of();
        }

        String language = normalizeLanguage(selectedLanguage);
        Map<ResourceLocation, BookResources> books = new LinkedHashMap<>();
        resourceJsonById.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> parseResource(entry.getKey()).ifPresent(resource -> {
                    if (!shouldIndexLanguage(resource.language(), language)) {
                        return;
                    }
                    BookResources book = books.computeIfAbsent(resource.bookId(), ignored -> new BookResources());
                    book.put(resource, entry.getValue(), language);
                }));

        List<AmiGuideDocument> documents = new ArrayList<>();
        for (Map.Entry<ResourceLocation, BookResources> entry : books.entrySet()) {
            ResourceLocation bookId = entry.getKey();
            BookResources resources = entry.getValue();
            List<AmiGuideDocument> parsed;
            try {
                parsed = PatchouliGuideFixtureAdapter.parse(
                        bookId,
                        resources.bookJson(),
                        resources.categoryJsonById(),
                        resources.entryJsonById()
                );
            } catch (RuntimeException ignored) {
                continue;
            }
            for (AmiGuideDocument document : parsed) {
                documents.add(openable(document));
            }
        }
        documents.sort(Comparator.comparing(document -> document.id().toString()));
        return List.copyOf(documents);
    }

    private static Map<ResourceLocation, String> readPatchouliResourceJson(ResourceManager resourceManager) {
        Map<ResourceLocation, String> out = new LinkedHashMap<>();
        resourceManager.listResources(ROOT, id -> id.getPath().endsWith(".json"))
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

    private static AmiGuideDocument openable(AmiGuideDocument document) {
        return AmiGuideDocument.builder(document.id(), document.sourceType(), document.modId(), document.title())
                .bookId(document.bookId())
                .iconItemId(document.iconItemId())
                .pageId(document.pageId())
                .chapter(document.chapter())
                .referencedItems(document.referencedItems())
                .tags(document.tags())
                .summaryText(document.summaryText())
                .openAction(AmiGuideOpeners.patchouli(document.bookId(), document.pageId()))
                .build();
    }

    private static Optional<PatchouliResource> parseResource(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        String path = id.getPath().replace('\\', '/');
        String prefix = ROOT + "/";
        if (!path.startsWith(prefix)) {
            return Optional.empty();
        }
        String rest = path.substring(prefix.length());
        String[] parts = rest.split("/");
        if (parts.length < 3) {
            return Optional.empty();
        }

        String bookPath = parts[0];
        String language = normalizeLanguage(parts[1]);
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), bookPath);
        if ("book.json".equals(parts[2])) {
            return Optional.of(new PatchouliResource(bookId, language, ResourceKind.BOOK, ""));
        }
        if (parts.length < 4) {
            return Optional.empty();
        }

        ResourceKind kind = switch (parts[2]) {
            case "categories" -> ResourceKind.CATEGORY;
            case "entries" -> ResourceKind.ENTRY;
            default -> null;
        };
        if (kind == null) {
            return Optional.empty();
        }

        String key = rest.substring((bookPath + "/" + parts[1] + "/" + parts[2] + "/").length());
        if (key.endsWith(".json")) {
            key = key.substring(0, key.length() - ".json".length());
        }
        return key.isBlank()
                ? Optional.empty()
                : Optional.of(new PatchouliResource(bookId, language, kind, key));
    }

    private static boolean shouldIndexLanguage(String resourceLanguage, String selectedLanguage) {
        return DEFAULT_LANGUAGE.equals(resourceLanguage) || selectedLanguage.equals(resourceLanguage);
    }

    private static String normalizeLanguage(String rawLanguage) {
        String language = rawLanguage == null ? "" : rawLanguage.trim().toLowerCase(Locale.ROOT);
        if (language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        language = language.replace('-', '_').replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        return language.isBlank() ? DEFAULT_LANGUAGE : language;
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

    private record PatchouliResource(ResourceLocation bookId, String language, ResourceKind kind, String key) {
    }

    private record LocalizedJson(String language, String json) {
    }

    private static final class BookResources {
        private LocalizedJson bookJson;
        private final Map<String, LocalizedJson> categoryJsonById = new LinkedHashMap<>();
        private final Map<String, LocalizedJson> entryJsonById = new LinkedHashMap<>();

        private void put(PatchouliResource resource, String json, String selectedLanguage) {
            if (json == null || json.isBlank()) {
                return;
            }
            LocalizedJson localized = new LocalizedJson(resource.language(), json);
            switch (resource.kind()) {
                case BOOK -> {
                    if (shouldReplace(bookJson, localized, selectedLanguage)) {
                        bookJson = localized;
                    }
                }
                case CATEGORY -> putLocalized(categoryJsonById, resource.key(), localized, selectedLanguage);
                case ENTRY -> putLocalized(entryJsonById, resource.key(), localized, selectedLanguage);
            }
        }

        private String bookJson() {
            return bookJson == null ? "" : bookJson.json();
        }

        private Map<String, String> categoryJsonById() {
            return localizedValues(categoryJsonById);
        }

        private Map<String, String> entryJsonById() {
            return localizedValues(entryJsonById);
        }

        private static void putLocalized(Map<String, LocalizedJson> target, String key,
                                         LocalizedJson value, String selectedLanguage) {
            LocalizedJson existing = target.get(key);
            if (shouldReplace(existing, value, selectedLanguage)) {
                target.put(key, value);
            }
        }

        private static boolean shouldReplace(LocalizedJson existing, LocalizedJson candidate, String selectedLanguage) {
            if (candidate == null) {
                return false;
            }
            if (existing == null) {
                return true;
            }
            return selectedLanguage.equals(candidate.language()) && !selectedLanguage.equals(existing.language());
        }

        private static Map<String, String> localizedValues(Map<String, LocalizedJson> localized) {
            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<String, LocalizedJson> entry : localized.entrySet()) {
                out.put(entry.getKey(), entry.getValue().json());
            }
            return out;
        }
    }
}
