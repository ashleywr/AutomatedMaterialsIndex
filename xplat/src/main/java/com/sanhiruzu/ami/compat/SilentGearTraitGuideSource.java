package com.sanhiruzu.ami.compat;

import com.google.gson.JsonArray;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class SilentGearTraitGuideSource {
    private static final String TRAIT_RESOURCE_PATH = "silentgear_traits";
    private static final String SOURCE_TYPE = "silentgear_traits";

    private SilentGearTraitGuideSource() {
    }

    public static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
        if (documents == null || !Services.PLATFORM.isClient()) {
            return;
        }
        ClientResourceAccess.registerGuideDocuments(documents);
    }

    static AmiGuideDocument traitDocument(ResourceLocation resourceId, JsonObject json) {
        String traitPath = traitPath(resourceId);
        ResourceLocation traitId = ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), traitPath);
        String name = componentText(json.get("name"), "trait." + traitId.getNamespace() + "." + traitPath, humanize(traitPath));
        String description = componentText(json.get("description"), "trait." + traitId.getNamespace() + "." + traitPath + ".desc", "");
        int maxLevel = json.has("max_level") && json.get("max_level").isJsonPrimitive()
                ? Math.max(1, json.get("max_level").getAsInt())
                : 1;
        List<String> effects = effectSummaries(json.getAsJsonArray("effects"));

        StringBuilder summary = new StringBuilder();
        if (!description.isBlank()) {
            summary.append(description);
        }
        summary.append(summary.isEmpty() ? "" : "\n")
                .append("Max level: ").append(maxLevel);
        if (!effects.isEmpty()) {
            summary.append("\nEffects: ").append(String.join("; ", effects));
        }

        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath("silentgear", "guide_book");
        String pageId = "trait/" + traitPath;
        return AmiGuideDocument.builder(
                        ResourceLocation.fromNamespaceAndPath("ami", "guide/silentgear/trait/" + traitPath),
                        SOURCE_TYPE,
                        "silentgear",
                        name + " Trait"
                )
                .bookId(bookId)
                .pageId(pageId)
                .chapter("Traits")
                .tag("trait")
                .tag("gear_trait")
                .tag("silentgear")
                .tag(traitPath)
                .tag("gear_trait_" + traitPath)
                .tags(levelTags(traitPath, maxLevel))
                .summaryText(summary.toString())
                .openAction(AmiGuideOpeners.patchouli(bookId, pageId))
                .build();
    }

    private static List<String> levelTags(String traitPath, int maxLevel) {
        List<String> tags = new ArrayList<>();
        for (int level = 1; level <= maxLevel; level++) {
            tags.add("gear_trait_" + traitPath + "_" + roman(level).toLowerCase(Locale.ROOT));
            tags.add(traitPath + "_" + roman(level).toLowerCase(Locale.ROOT));
        }
        return tags;
    }

    private static List<String> effectSummaries(JsonArray effects) {
        if (effects == null || effects.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonElement element : effects) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject effect = element.getAsJsonObject();
            String type = effect.has("type") ? simplifyId(effect.get("type").getAsString()) : "effect";
            List<String> details = new ArrayList<>();
            if (effect.has("activation_chance")) {
                details.add("chance " + percent(effect.get("activation_chance").getAsDouble()));
            }
            if (effect.has("effect_scale")) {
                details.add("scale " + trimDouble(effect.get("effect_scale").getAsDouble()));
            }
            out.add(details.isEmpty() ? humanize(type) : humanize(type) + " (" + String.join(", ", details) + ")");
        }
        return List.copyOf(out);
    }

    private static String componentText(JsonElement element, String fallbackTranslationKey, String fallbackText) {
        if (element != null && element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("translate")) {
                return Component.translatable(object.get("translate").getAsString()).getString();
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

    private static String traitPath(ResourceLocation resourceId) {
        String path = resourceId.getPath();
        if (path.startsWith(TRAIT_RESOURCE_PATH + "/")) {
            path = path.substring((TRAIT_RESOURCE_PATH + "/").length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return path;
    }

    private static String simplifyId(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
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

    private static String percent(double value) {
        return trimDouble(value * 100.0D) + "%";
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

    private static final class ClientResourceAccess {
        private ClientResourceAccess() {
        }

        private static void registerGuideDocuments(Consumer<AmiGuideDocument> documents) {
            ResourceManager resourceManager = resourceManager();
            if (resourceManager == null) {
                return;
            }
            Map<ResourceLocation, Resource> resources = resourceManager.listResources(TRAIT_RESOURCE_PATH,
                    id -> "silentgear".equals(id.getNamespace()) && id.getPath().endsWith(".json"));
            resources.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                    .forEach(entry -> registerTraitDocument(documents, entry.getKey(), entry.getValue()));
        }

        private static ResourceManager resourceManager() {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var server = minecraft.getSingleplayerServer();
            if (server != null) {
                return server.getResourceManager();
            }
            return minecraft.getResourceManager();
        }

        private static void registerTraitDocument(Consumer<AmiGuideDocument> documents, ResourceLocation id, Resource resource) {
            try (BufferedReader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                documents.accept(traitDocument(id, json));
            } catch (IOException | RuntimeException ignored) {
                // Broken third-party data should not block indexing.
            }
        }
    }
}
