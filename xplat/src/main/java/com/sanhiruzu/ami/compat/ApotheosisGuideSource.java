package com.sanhiruzu.ami.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiGuideOpeners;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ApotheosisGuideSource {
    private static final String AFFIX_RESOURCE_PATH = "affixes";
    private static final String ENCHANTING_STATS_RESOURCE_PATH = "enchanting_stats";
    private static final String ENCHANTMENT_RESOURCE_PATH = "enchantment";
    private static final String SOURCE_TYPE = "apotheosis_data";
    private static final String AFFIX_GUIDE_ENTRY = "adventure/affix_loot/affixes";
    private static final String ENCHANTING_STATS_GUIDE_ENTRY = "enchanting/table/stats";
    private static final String ENCHANTMENT_GUIDE_ENTRY_PREFIX = "enchanting/enchantments/";
    private static final List<ResourceLocation> BOOK_IDS = List.of(
            ResourceLocation.fromNamespaceAndPath("apotheosis", "apoth_chronicle"),
            ResourceLocation.fromNamespaceAndPath("apotheosis", "guide"),
            ResourceLocation.fromNamespaceAndPath("apothic_enchanting", "guide"),
            ResourceLocation.fromNamespaceAndPath("apothic_enchanting", "apothic_enchanting"),
            ResourceLocation.fromNamespaceAndPath("apotheosis", "apothic_enchanting")
    );

    private ApotheosisGuideSource() {
    }

    public static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
        if (documents == null || !Services.PLATFORM.isClient()) {
            return;
        }
        ClientResourceAccess.registerGuideDocuments(documents);
    }

    static AmiGuideDocument affixDocument(ResourceLocation resourceId, JsonObject json) {
        String affixPath = resourcePath(resourceId, AFFIX_RESOURCE_PATH);
        String affixName = leafName(affixPath);
        String type = simplifyId(stringValue(json.get("type")));
        String attribute = simplifyId(stringValue(json.get("attribute")));
        List<String> categories = stringList(json.get("categories")).stream()
                .map(ApotheosisGuideSource::simplifyId)
                .toList();

        JsonObject definition = objectValue(json.get("definition"));
        String affixType = definition == null ? "" : stringValue(definition.get("affix_type"));
        List<String> details = new ArrayList<>();
        if (!type.isBlank()) details.add("Type: " + humanize(type));
        if (!affixType.isBlank()) details.add("Affix type: " + humanize(affixType));
        if (!attribute.isBlank()) details.add("Attribute: " + humanize(attribute));
        if (!categories.isEmpty()) details.add("Applies to: " + humanizedList(categories));
        String values = rarityValues(json.get("values"));
        if (!values.isBlank()) details.add("Values: " + values);

        return AmiGuideDocument.builder(
                        ResourceLocation.fromNamespaceAndPath("ami", "guide/apotheosis/affix/" + affixPath),
                        SOURCE_TYPE,
                        "apotheosis",
                        humanize(affixName) + " Affix"
                )
                .chapter("Affixes")
                .pageId(AFFIX_GUIDE_ENTRY)
                .bookId(BOOK_IDS.get(0))
                .tag("apotheosis")
                .tag("affix")
                .tag("apotheosis_affix")
                .tag(affixName)
                .tag("affix_" + affixName)
                .tags(pathTags(affixPath))
                .tags(categories)
                .tag(type)
                .tag(affixType)
                .tag(attribute)
                .summaryText(String.join("\n", details))
                .openAction(AmiGuideOpeners.patchouli(BOOK_IDS, AFFIX_GUIDE_ENTRY))
                .build();
    }

    static AmiGuideDocument enchantingStatsDocument(ResourceLocation resourceId, JsonObject json) {
        String statsPath = resourcePath(resourceId, ENCHANTING_STATS_RESOURCE_PATH);
        ResourceLocation block = resourceLocation(stringValue(json.get("block")));
        JsonObject stats = objectValue(json.get("stats"));
        List<String> details = new ArrayList<>();
        if (block != null) details.add("Block: " + humanize(block.getPath()));
        if (stats != null) {
            addStat(details, stats, "eterna");
            addStat(details, stats, "maxEterna");
            addStat(details, stats, "quanta");
            addStat(details, stats, "arcana");
        }

        AmiGuideDocument.Builder builder = AmiGuideDocument.builder(
                        ResourceLocation.fromNamespaceAndPath("ami", "guide/apotheosis/enchanting_stats/" + statsPath),
                        SOURCE_TYPE,
                        "apothic_enchanting",
                        humanize(block == null ? leafName(statsPath) : block.getPath()) + " Enchanting Stats"
                )
                .chapter("Enchanting Stats")
                .pageId(ENCHANTING_STATS_GUIDE_ENTRY)
                .bookId(BOOK_IDS.get(0))
                .tag("apotheosis")
                .tag("apothic_enchanting")
                .tag("enchanting")
                .tag("enchanting_stats")
                .tag("eterna")
                .tag("quanta")
                .tag("arcana")
                .tags(pathTags(statsPath))
                .summaryText(String.join("\n", details))
                .openAction(AmiGuideOpeners.patchouli(BOOK_IDS, ENCHANTING_STATS_GUIDE_ENTRY));
        if (block != null) {
            builder.referencedItem(block);
        }
        return builder.build();
    }

    static AmiGuideDocument enchantmentDocument(ResourceLocation resourceId, JsonObject json) {
        String enchantmentPath = resourcePath(resourceId, ENCHANTMENT_RESOURCE_PATH);
        ResourceLocation enchantmentId = ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), enchantmentPath);
        String title = componentText(json.get("description"),
                "enchantment." + enchantmentId.getNamespace() + "." + enchantmentPath,
                humanize(enchantmentPath));
        List<String> details = new ArrayList<>();
        addNumber(details, json, "max_level", "Max level");
        addNumber(details, json, "weight", "Weight");
        String supported = stringValue(json.get("supported_items"));
        if (!supported.isBlank()) details.add("Supported items: " + supported);
        String slots = humanizedList(stringList(json.get("slots")));
        if (!slots.isBlank()) details.add("Slots: " + slots);

        return AmiGuideDocument.builder(
                        ResourceLocation.fromNamespaceAndPath("ami", "guide/apotheosis/enchantment/" + enchantmentPath),
                        SOURCE_TYPE,
                        resourceId.getNamespace(),
                        title + " Enchantment"
                )
                .chapter("Enchantments")
                .pageId(ENCHANTMENT_GUIDE_ENTRY_PREFIX + enchantmentPath)
                .bookId(BOOK_IDS.get(0))
                .tag("apotheosis")
                .tag("apothic_enchanting")
                .tag("enchantment")
                .tag(enchantmentPath)
                .tag("enchantment_" + enchantmentPath)
                .tags(pathTags(enchantmentPath))
                .summaryText(String.join("\n", details))
                .openAction(AmiGuideOpeners.patchouli(BOOK_IDS, ENCHANTMENT_GUIDE_ENTRY_PREFIX + enchantmentPath))
                .build();
    }

    private static String rarityValues(JsonElement element) {
        JsonObject values = objectValue(element);
        if (values == null || values.size() == 0) {
            return "";
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            JsonObject range = objectValue(entry.getValue());
            if (range == null) {
                continue;
            }
            String rarity = simplifyId(entry.getKey());
            String min = numberString(range.get("min"));
            String max = numberString(range.get("max"));
            if (!min.isBlank() && !max.isBlank()) {
                out.add(humanize(rarity) + " " + min + "-" + max);
            }
        }
        return String.join("; ", out);
    }

    private static void addStat(List<String> details, JsonObject stats, String key) {
        String value = numberString(stats.get(key));
        if (!value.isBlank()) {
            details.add(humanize(key) + ": " + value);
        }
    }

    private static void addNumber(List<String> details, JsonObject json, String key, String label) {
        String value = numberString(json.get(key));
        if (!value.isBlank()) {
            details.add(label + ": " + value);
        }
    }

    private static JsonObject objectValue(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String stringValue(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString().trim() : "";
    }

    private static String numberString(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return "";
        }
        double value = element.getAsDouble();
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                String value = stringValue(child);
                if (!value.isBlank()) {
                    out.add(value);
                }
            }
        } else {
            String value = stringValue(element);
            if (!value.isBlank()) {
                out.add(value);
            }
        }
        return List.copyOf(out);
    }

    private static String componentText(JsonElement element, String fallbackTranslationKey, String fallbackText) {
        if (element != null && element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("translate")) {
                try {
                    return Component.translatable(object.get("translate").getAsString()).getString();
                } catch (RuntimeException | LinkageError ignored) {
                    return fallbackText == null ? "" : fallbackText;
                }
            }
            if (object.has("text")) {
                return object.get("text").getAsString();
            }
        }
        if (fallbackTranslationKey != null && !fallbackTranslationKey.isBlank()) {
            try {
                String translated = Component.translatable(fallbackTranslationKey).getString();
                if (!translated.equals(fallbackTranslationKey)) {
                    return translated;
                }
            } catch (RuntimeException | LinkageError ignored) {
                return fallbackText == null ? "" : fallbackText;
            }
        }
        return fallbackText == null ? "" : fallbackText;
    }

    private static ResourceLocation resourceLocation(String value) {
        if (value.isBlank() || !value.contains(":")) {
            return null;
        }
        return ResourceLocation.tryParse(value);
    }

    private static String resourcePath(ResourceLocation resourceId, String root) {
        String path = resourceId.getPath();
        if (path.startsWith(root + "/")) {
            path = path.substring((root + "/").length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return path;
    }

    private static String leafName(String path) {
        int separator = path.lastIndexOf('/');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }

    private static List<String> pathTags(String path) {
        Set<String> tags = new LinkedHashSet<>();
        for (String part : path.split("[/._-]+")) {
            if (!part.isBlank()) {
                tags.add(part.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(tags);
    }

    private static String simplifyId(String id) {
        if (id == null) {
            return "";
        }
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private static String humanizedList(List<String> values) {
        List<String> out = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(ApotheosisGuideSource::humanize)
                .toList();
        return String.join(", ", out);
    }

    private static String humanize(String value) {
        StringBuilder out = new StringBuilder();
        String clean = value == null ? "" : value.replaceAll("([a-z])([A-Z])", "$1 $2");
        for (String part : clean.split("[_\\-/ .]+")) {
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

    private static final class ClientResourceAccess {
        private ClientResourceAccess() {
        }

        private static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
            ResourceManager resourceManager = resourceManager();
            if (resourceManager == null) {
                return;
            }
            registerResources(resourceManager, AFFIX_RESOURCE_PATH,
                    id -> "apotheosis".equals(id.getNamespace()),
                    (id, json) -> documents.accept(affixDocument(id, json)));
            registerResources(resourceManager, ENCHANTING_STATS_RESOURCE_PATH,
                    id -> "apothic_enchanting".equals(id.getNamespace()),
                    (id, json) -> documents.accept(enchantingStatsDocument(id, json)));
            registerResources(resourceManager, ENCHANTMENT_RESOURCE_PATH,
                    id -> "apothic_enchanting".equals(id.getNamespace()),
                    (id, json) -> documents.accept(enchantmentDocument(id, json)));
        }

        private static ResourceManager resourceManager() {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var server = minecraft.getSingleplayerServer();
            if (server != null) {
                return server.getResourceManager();
            }
            return minecraft.getResourceManager();
        }

        private static void registerResources(ResourceManager resourceManager, String root,
                                              java.util.function.Predicate<ResourceLocation> namespace,
                                              ResourceRegistrar registrar) {
            resourceManager.listResources(root, id -> namespace.test(id) && id.getPath().endsWith(".json"))
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                    .forEach(entry -> registerDocument(registrar, entry.getKey(), entry.getValue()));
        }

        private static void registerDocument(ResourceRegistrar registrar, ResourceLocation id, Resource resource) {
            try (BufferedReader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                registrar.register(id, json);
            } catch (IOException | RuntimeException ignored) {
                // Broken third-party data should not block indexing.
            }
        }

        private interface ResourceRegistrar {
            void register(ResourceLocation id, JsonObject json);
        }
    }
}
