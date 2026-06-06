package com.sanhiruzu.ami.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiGuideOpeners;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Runtime reader for Silent Gear's custom material book.
 */
public final class SilentGearMaterialBookGuideSource {
    private static final String MATERIAL_RESOURCE_PATH = "silentgear_materials";
    private static final String SOURCE_TYPE = "silentgear_materials";
    private static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath("silentgear", "material_book");

    private SilentGearMaterialBookGuideSource() {
    }

    public static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
        if (documents == null) {
            return;
        }
        ResourceManager resourceManager = resourceManager();
        if (resourceManager == null) {
            return;
        }
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(MATERIAL_RESOURCE_PATH,
                id -> "silentgear".equals(id.getNamespace()) && id.getPath().endsWith(".json"));
        resources.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> registerMaterialDocument(documents, entry.getKey(), entry.getValue()));
    }

    static AmiGuideDocument materialDocument(ResourceLocation resourceId, JsonObject json) {
        String materialPath = materialPath(resourceId);
        ResourceLocation materialId = ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), materialPath);
        MaterialSummary summary = materialSummary(materialId, json);
        return AmiGuideDocument.builder(
                        ResourceLocation.fromNamespaceAndPath("ami", "guide/silentgear/material/" + safePath(materialPath)),
                        SOURCE_TYPE,
                        "silentgear",
                        summary.name())
                .bookId(BOOK_ID)
                .iconItemId(BOOK_ID)
                .pageId(materialPath)
                .chapter("Materials")
                .referencedItems(summary.referencedItems())
                .tag("material")
                .tag("gear_material")
                .tag("silentgear")
                .tag(materialPath)
                .tags(summary.tags())
                .summaryText(summary.summaryText())
                .openAction(AmiGuideOpeners.silentGearMaterialBook(materialId))
                .build();
    }

    private static MaterialSummary materialSummary(ResourceLocation materialId, JsonObject json) {
        JsonObject display = object(json, "display");
        String name = componentText(display == null ? null : display.get("name"),
                "material." + materialId.getNamespace() + "." + materialId.getPath().replace('/', '.'),
                humanize(materialId.getPath()));
        List<String> tags = new ArrayList<>();
        Set<ResourceLocation> referencedItems = new LinkedHashSet<>();
        List<String> summaryParts = new ArrayList<>();
        add(summaryParts, name);

        JsonObject crafting = object(json, "crafting");
        if (crafting != null) {
            JsonElement categories = crafting.get("categories");
            if (categories instanceof JsonArray array) {
                List<String> categoryLabels = new ArrayList<>();
                for (JsonElement element : array) {
                    if (element.isJsonPrimitive()) {
                        String category = cleanToken(element.getAsString());
                        if (!category.isBlank()) {
                            tags.add(category);
                            categoryLabels.add(humanize(category));
                        }
                    }
                }
                if (!categoryLabels.isEmpty()) {
                    add(summaryParts, "Categories: " + String.join(", ", categoryLabels));
                }
            }
            collectItems(crafting.get("ingredient"), referencedItems);
            collectItems(crafting.get("part_substitutes"), referencedItems);
        }

        JsonObject properties = object(json, "properties");
        if (properties != null) {
            List<String> propertyParts = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                tags.add(cleanToken(entry.getKey()));
                JsonObject part = entry.getValue().getAsJsonObject();
                collectNumberProperty(part, propertyParts, "durability", "Durability");
                collectNumberProperty(part, propertyParts, "armor", "Armor");
                collectNumberProperty(part, propertyParts, "armor_toughness", "Armor toughness");
                collectNumberProperty(part, propertyParts, "attack_damage", "Attack damage");
                collectNumberProperty(part, propertyParts, "harvest_speed", "Harvest speed");
                collectNumberProperty(part, propertyParts, "enchantment_value", "Enchantment");
                collectTraits(part.get("traits"), tags, propertyParts);
            }
            if (!propertyParts.isEmpty()) {
                add(summaryParts, String.join("; ", propertyParts));
            }
        }

        return new MaterialSummary(
                name,
                List.copyOf(referencedItems),
                List.copyOf(dedupe(tags)),
                String.join("\n", summaryParts)
        );
    }

    private static void collectNumberProperty(JsonObject object, List<String> summaryParts, String key, String label) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            add(summaryParts, label + " " + trimDouble(element.getAsDouble()));
        } else if (element.isJsonObject()) {
            JsonObject value = element.getAsJsonObject();
            if (value.has("value") && value.get("value").isJsonPrimitive()) {
                add(summaryParts, label + " " + trimDouble(value.get("value").getAsDouble()));
            }
        }
    }

    private static void collectTraits(JsonElement element, List<String> tags, List<String> summaryParts) {
        if (!(element instanceof JsonArray array)) {
            return;
        }
        List<String> traits = new ArrayList<>();
        for (JsonElement child : array) {
            if (!child.isJsonObject()) {
                continue;
            }
            JsonObject object = child.getAsJsonObject();
            if (!object.has("trait") || !object.get("trait").isJsonPrimitive()) {
                continue;
            }
            String trait = object.get("trait").getAsString();
            String path = trait.contains(":") ? trait.substring(trait.indexOf(':') + 1) : trait;
            tags.add("trait_" + cleanToken(path));
            int level = object.has("level") && object.get("level").isJsonPrimitive()
                    ? object.get("level").getAsInt()
                    : 1;
            traits.add(humanize(path) + " " + roman(level));
        }
        if (!traits.isEmpty()) {
            add(summaryParts, "Traits: " + String.join(", ", traits));
        }
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
        collectItemId(object.get("item"), out);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            collectItems(entry.getValue(), out);
        }
    }

    private static void collectItemId(JsonElement element, Set<ResourceLocation> out) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
        if (id != null) {
            out.add(id);
        }
    }

    private static String componentText(JsonElement element, String fallbackTranslationKey, String fallbackText) {
        if (element != null && element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("translate")) {
                String translated = Component.translatable(object.get("translate").getAsString()).getString();
                if (!translated.equals(object.get("translate").getAsString())) {
                    return translated;
                }
            }
            if (object.has("text")) {
                return object.get("text").getAsString();
            }
        }
        if (fallbackTranslationKey != null && !fallbackTranslationKey.isBlank()) {
            String translated = Component.translatable(fallbackTranslationKey).getString();
            if (!translated.equals(fallbackTranslationKey)) {
                return translated;
            }
        }
        return fallbackText == null ? "" : fallbackText;
    }

    private static void registerMaterialDocument(Consumer<AmiGuideDocument> documents, ResourceLocation id, Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            documents.accept(materialDocument(id, json));
        } catch (IOException | RuntimeException ignored) {
            // Broken third-party material data should not block indexing.
        }
    }

    private static ResourceManager resourceManager() {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server != null) {
            return server.getResourceManager();
        }
        return minecraft.getResourceManager();
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            return null;
        }
        return parent.getAsJsonObject(key);
    }

    private static String materialPath(ResourceLocation resourceId) {
        String path = resourceId.getPath();
        if (path.startsWith(MATERIAL_RESOURCE_PATH + "/")) {
            path = path.substring((MATERIAL_RESOURCE_PATH + "/").length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return path;
    }

    private static String humanize(String value) {
        StringBuilder out = new StringBuilder();
        for (String part : value.split("[_\\-/]+")) {
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

    private static String cleanToken(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(':', '_').replace('/', '_');
    }

    private static List<String> dedupe(List<String> values) {
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank() && !out.contains(value)) {
                out.add(value);
            }
        }
        return out;
    }

    private static void add(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
    }

    private static String trimDouble(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(level);
        };
    }

    private static String safePath(String raw) {
        return Optional.ofNullable(raw)
                .orElse("material")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9/._-]", "_")
                .replaceAll("/+", "/");
    }

    private record MaterialSummary(String name,
                                   List<ResourceLocation> referencedItems,
                                   List<String> tags,
                                   String summaryText) {
    }
}
