package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiGuideOpeners;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime reader for GuideME markdown guides such as AE2's in-game guide.
 */
public final class GuideMeRuntimeGuideSource {
    private static final String ROOT = "ae2guide";
    private static final String SOURCE_TYPE = "guideme";
    private static final int SUMMARY_CAP = 4096;
    private static final Pattern HEADING = Pattern.compile("^\\s*#{1,6}\\s+(.+?)\\s*$");
    private static final Pattern FRONT_MATTER_VALUE = Pattern.compile("^\\s*([A-Za-z0-9_-]+)\\s*:\\s*(.+?)\\s*$");
    private static final Pattern FRONT_MATTER_KEY = Pattern.compile("^\\s*([A-Za-z0-9_-]+)\\s*:\\s*$");
    private static final Pattern ITEM_TAG = Pattern.compile("<(?:ItemLink|ItemImage|RecipeFor)\\b[^>]*\\bid\\s*=\\s*\"([^\"]+)\"[^>]*/?>");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^]]*]\\(([^)]+\\.md)(?:#[^)]+)?\\)");

    private GuideMeRuntimeGuideSource() {
    }

    public static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
        if (documents == null) {
            return;
        }
        ResourceManager resourceManager = resourceManager();
        if (resourceManager == null) {
            return;
        }
        for (AmiGuideDocument document : documentsFromResources(readGuideMeMarkdown(resourceManager))) {
            documents.accept(document);
        }
    }

    static List<AmiGuideDocument> documentsFromResources(Map<ResourceLocation, String> markdownById) {
        if (markdownById == null || markdownById.isEmpty()) {
            return List.of();
        }

        List<AmiGuideDocument> documents = new ArrayList<>();
        markdownById.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> documentFromMarkdown(entry.getKey(), entry.getValue()).ifPresent(documents::add));
        return List.copyOf(documents);
    }

    private static Optional<AmiGuideDocument> documentFromMarkdown(ResourceLocation resourceId, String markdown) {
        GuideMeResource resource = parseResource(resourceId).orElse(null);
        if (resource == null || markdown == null || markdown.isBlank()) {
            return Optional.empty();
        }

        ParsedMarkdown parsed = parseMarkdown(markdown, resource.namespace());
        String title = !parsed.navigationTitle().isBlank()
                ? parsed.navigationTitle()
                : !parsed.firstHeading().isBlank() ? parsed.firstHeading() : humanize(resource.pageId());
        String chapter = chapter(resource.pageId(), parsed);
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath(resource.namespace(), "guide");
        ResourceLocation documentId = ResourceLocation.fromNamespaceAndPath(
                "ami",
                "guide/guideme/" + resource.namespace() + "/" + safePath(resource.pageId())
        );

        AmiGuideDocument document = AmiGuideDocument.builder(documentId, SOURCE_TYPE, resource.namespace(), title)
                .bookId(bookId)
                .pageId(resource.pageId())
                .chapter(chapter)
                .referencedItems(parsed.referencedItems())
                .tags(parsed.tags())
                .summaryText(parsed.summary())
                .openAction(AmiGuideOpeners.guideME(bookId, resource.pageId()))
                .build();
        return Optional.of(document);
    }

    private static ParsedMarkdown parseMarkdown(String markdown, String namespace) {
        Map<String, String> frontMatter = new LinkedHashMap<>();
        List<String> categories = new ArrayList<>();
        List<ResourceLocation> items = new ArrayList<>();
        String firstHeading = "";
        StringBuilder summary = new StringBuilder();
        boolean inFrontMatter = false;
        boolean frontMatterDone = false;
        String listKey = "";

        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (i == 0 && "---".equals(trimmed)) {
                inFrontMatter = true;
                continue;
            }
            if (inFrontMatter) {
                if ("---".equals(trimmed)) {
                    inFrontMatter = false;
                    frontMatterDone = true;
                    listKey = "";
                    continue;
                }
                Matcher value = FRONT_MATTER_VALUE.matcher(line);
                if (value.matches()) {
                    listKey = value.group(1);
                    frontMatter.put(listKey, stripQuotes(value.group(2)));
                    continue;
                }
                Matcher key = FRONT_MATTER_KEY.matcher(line);
                if (key.matches()) {
                    listKey = key.group(1);
                    continue;
                }
                if (trimmed.startsWith("- ")) {
                    String listValue = stripQuotes(trimmed.substring(2));
                    if ("categories".equals(listKey)) {
                        categories.add(listValue);
                    } else if ("item_ids".equals(listKey)) {
                        parseItemId(listValue, namespace).ifPresent(items::add);
                    }
                }
                continue;
            }

            Matcher heading = HEADING.matcher(line);
            if (heading.matches() && firstHeading.isBlank()) {
                firstHeading = cleanInline(heading.group(1));
            }
            collectInlineItems(line, namespace, items);
            appendSummary(summary, line);
        }

        String navigationTitle = navigationTitle(frontMatter);
        List<String> tags = new ArrayList<>(categories);
        collectMarkdownPageTags(markdown, tags);
        return new ParsedMarkdown(
                cleanInline(navigationTitle),
                cleanInline(firstHeading),
                List.copyOf(dedupeItems(items)),
                List.copyOf(dedupeStrings(tags)),
                cap(summary.toString().trim()),
                frontMatterDone
        );
    }

    private static void collectInlineItems(String line, String namespace, List<ResourceLocation> items) {
        Matcher matcher = ITEM_TAG.matcher(line);
        while (matcher.find()) {
            parseItemId(matcher.group(1), namespace).ifPresent(items::add);
        }
    }

    private static void collectMarkdownPageTags(String markdown, List<String> tags) {
        Matcher matcher = MARKDOWN_LINK.matcher(markdown);
        while (matcher.find()) {
            String page = matcher.group(1).replace('\\', '/');
            int slash = page.lastIndexOf('/');
            String leaf = slash >= 0 ? page.substring(slash + 1) : page;
            if (leaf.endsWith(".md")) {
                leaf = leaf.substring(0, leaf.length() - 3);
            }
            if (!leaf.isBlank()) {
                tags.add(leaf.replace('-', '_'));
            }
        }
    }

    private static void appendSummary(StringBuilder summary, String line) {
        if (summary.length() >= SUMMARY_CAP) {
            return;
        }
        String clean = cleanInline(line);
        if (clean.isBlank()) {
            return;
        }
        if (!summary.isEmpty()) {
            summary.append(' ');
        }
        summary.append(clean);
    }

    private static String cleanInline(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String clean = raw.replaceAll("<[^>]+>", " ")
                .replaceAll("!\\[([^]]*)]\\([^)]+\\)", "$1")
                .replaceAll("\\[([^]]*)]\\([^)]+\\)", "$1")
                .replace('`', ' ')
                .replace("*", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return clean;
    }

    private static String navigationTitle(Map<String, String> frontMatter) {
        String direct = frontMatter.getOrDefault("title", "");
        if (!direct.isBlank()) {
            return direct;
        }
        return frontMatter.getOrDefault("navigation.title", "");
    }

    private static String chapter(String pageId, ParsedMarkdown parsed) {
        if (!parsed.tags().isEmpty()) {
            return humanize(parsed.tags().get(0));
        }
        int slash = pageId.lastIndexOf('/');
        if (slash > 0) {
            return humanize(pageId.substring(0, slash));
        }
        return "";
    }

    private static Optional<ResourceLocation> parseItemId(String raw, String namespace) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String id = raw.trim();
        if (!id.contains(":")) {
            id = namespace + ":" + id;
        }
        return Optional.ofNullable(ResourceLocation.tryParse(id));
    }

    private static List<ResourceLocation> dedupeItems(List<ResourceLocation> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private static List<String> dedupeStrings(List<String> values) {
        Set<String> deduped = new LinkedHashSet<>();
        for (String value : values) {
            String clean = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            if (!clean.isBlank()) {
                deduped.add(clean);
            }
        }
        return new ArrayList<>(deduped);
    }

    private static String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String leaf = raw.replace('\\', '/');
        int slash = leaf.lastIndexOf('/');
        if (slash >= 0) {
            leaf = leaf.substring(slash + 1);
        }
        StringBuilder out = new StringBuilder();
        for (String part : leaf.split("[_-]+")) {
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

    private static String safePath(String path) {
        return path.replaceAll("[^a-zA-Z0-9_./-]", "_").replace('/', '/');
    }

    private static String stripQuotes(String raw) {
        String value = raw == null ? "" : raw.trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String cap(String text) {
        return text.length() <= SUMMARY_CAP ? text : text.substring(0, SUMMARY_CAP);
    }

    private static Map<ResourceLocation, String> readGuideMeMarkdown(ResourceManager resourceManager) {
        Map<ResourceLocation, String> out = new LinkedHashMap<>();
        resourceManager.listResources(ROOT, id -> id.getPath().endsWith(".md"))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> readResource(entry.getValue()).ifPresent(markdown -> out.put(entry.getKey(), markdown)));
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

    private static Optional<GuideMeResource> parseResource(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        String path = id.getPath().replace('\\', '/');
        String prefix = ROOT + "/";
        if (!path.startsWith(prefix) || !path.endsWith(".md")) {
            return Optional.empty();
        }
        String pageId = path.substring(prefix.length(), path.length() - ".md".length());
        return pageId.isBlank() ? Optional.empty() : Optional.of(new GuideMeResource(id.getNamespace(), pageId));
    }

    private static ResourceManager resourceManager() {
        return net.minecraft.client.Minecraft.getInstance().getResourceManager();
    }

    private record GuideMeResource(String namespace, String pageId) {
    }

    private record ParsedMarkdown(
            String navigationTitle,
            String firstHeading,
            List<ResourceLocation> referencedItems,
            List<String> tags,
            String summary,
            boolean hadFrontMatter
    ) {
    }
}
