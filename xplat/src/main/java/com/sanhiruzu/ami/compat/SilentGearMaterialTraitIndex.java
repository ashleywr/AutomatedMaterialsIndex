package com.sanhiruzu.ami.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SilentGearMaterialTraitIndex {
    private static final String MATERIAL_RESOURCE_PATH = "silentgear_materials";

    private SilentGearMaterialTraitIndex() {
    }

    public static void applyToIndex(GlobalIndex index) {
        if (index == null || !Services.PLATFORM.isClient()) {
            return;
        }
        ClientResourceAccess.applyToIndex(index);
    }

    static MaterialRecord materialRecord(Identifier resourceId, JsonObject json) {
        String material = materialPath(resourceId);
        LinkedHashSet<IngredientRef> ingredients = new LinkedHashSet<>();
        LinkedHashSet<String> traits = new LinkedHashSet<>();
        LinkedHashSet<String> details = new LinkedHashSet<>();

        JsonObject crafting = object(json, "crafting");
        if (crafting != null) {
            collectIngredients(crafting.get("ingredient"), ingredients);
            JsonObject substitutes = object(crafting, "part_substitutes");
            if (substitutes != null) {
                for (var entry : substitutes.entrySet()) {
                    collectIngredients(entry.getValue(), ingredients);
                }
            }
        }

        JsonObject properties = object(json, "properties");
        if (properties != null) {
            for (var property : properties.entrySet()) {
                String part = simplifyId(property.getKey());
                JsonObject propertyObject = property.getValue().isJsonObject()
                        ? property.getValue().getAsJsonObject()
                        : null;
                if (propertyObject == null) {
                    continue;
                }
                JsonArray traitArray = propertyObject.getAsJsonArray("traits");
                if (traitArray == null) {
                    continue;
                }
                for (JsonElement element : traitArray) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject traitObject = element.getAsJsonObject();
                    String trait = traitPath(traitObject);
                    if (trait.isBlank()) {
                        continue;
                    }
                    int level = traitObject.has("level") && traitObject.get("level").isJsonPrimitive()
                            ? Math.max(1, traitObject.get("level").getAsInt())
                            : 1;
                    String levelToken = trait + "_" + roman(level).toLowerCase(Locale.ROOT);
                    traits.add(trait);
                    traits.add(levelToken);
                    details.add(material + ":" + part + ":" + levelToken);
                }
            }
        }

        return new MaterialRecord(material, List.copyOf(ingredients), List.copyOf(traits), List.copyOf(details));
    }

    static int applyMaterialRecords(GlobalIndex index, Collection<MaterialRecord> records) {
        if (index == null || records == null || records.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (SearchNode node : index.getNodes(NodeType.ITEM)) {
            MaterialMerge merge = mergeFor(node, records);
            if (merge.isEmpty()) {
                continue;
            }
            Map<String, String> metadata = new LinkedHashMap<>(node.metadata());
            addCsv(metadata, SearchNodeKeys.COMPAT_FAMILIES, CompatFamilyDetector.MODULAR_GEAR);
            addCsv(metadata, SearchNodeKeys.COMPAT_FAMILIES, CompatFamilyDetector.SILENT_GEAR);
            metadata.putIfAbsent(SearchNodeKeys.MODULAR_GEAR_FAMILY, CompatFamilyDetector.SILENT_GEAR);
            metadata.put(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, "materials");
            addCsv(metadata, SearchNodeKeys.MODULAR_GEAR_FACTS, "material");
            for (String material : merge.materials()) {
                addCsv(metadata, SearchNodeKeys.MODULAR_GEAR_MATERIAL, material);
                addCsv(metadata, SearchNodeKeys.MODULAR_GEAR_TIER, material);
                addSearchToken(metadata, "gear_material_" + material);
            }
            for (String trait : merge.traits()) {
                addCsv(metadata, SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAITS, trait);
                addSearchToken(metadata, "gear_trait_" + trait);
            }
            for (String detail : merge.details()) {
                addCsv(metadata, SearchNodeKeys.MODULAR_GEAR_MATERIAL_TRAIT_DETAILS, detail);
            }
            if (!metadata.equals(node.metadata())) {
                index.replaceNode(node.id(), node.type(), node.withMetadata(metadata));
                changed++;
            }
        }
        return changed;
    }

    private static MaterialMerge mergeFor(SearchNode node, Collection<MaterialRecord> records) {
        LinkedHashSet<String> materials = new LinkedHashSet<>();
        LinkedHashSet<String> traits = new LinkedHashSet<>();
        LinkedHashSet<String> details = new LinkedHashSet<>();
        for (MaterialRecord record : records) {
            if (record.traits().isEmpty() || !record.matches(node)) {
                continue;
            }
            materials.add(record.material());
            traits.addAll(record.traits());
            details.addAll(record.details());
        }
        return new MaterialMerge(materials, traits, details);
    }

    private static void collectIngredients(JsonElement element, Set<IngredientRef> out) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectIngredients(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("item")) {
            addIngredient(out, object.get("item").getAsString(), false);
        }
        if (object.has("id")) {
            addIngredient(out, object.get("id").getAsString(), false);
        }
        if (object.has("tag")) {
            addIngredient(out, object.get("tag").getAsString(), true);
        }
        if (object.has("items")) {
            collectIngredients(object.get("items"), out);
        }
        if (object.has("values")) {
            collectIngredients(object.get("values"), out);
        }
    }

    private static void addIngredient(Set<IngredientRef> out, String value, boolean tag) {
        Identifier id = parseId(value);
        if (id == null) {
            return;
        }
        out.add(tag ? IngredientRef.tag(id) : IngredientRef.item(id));
    }

    private static String traitPath(JsonObject traitObject) {
        if (!traitObject.has("trait") || !traitObject.get("trait").isJsonPrimitive()) {
            return "";
        }
        return simplifyId(traitObject.get("trait").getAsString()).toLowerCase(Locale.ROOT);
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            return null;
        }
        return parent.getAsJsonObject(key);
    }

    private static String materialPath(Identifier resourceId) {
        String path = resourceId.getPath();
        if (path.startsWith(MATERIAL_RESOURCE_PATH + "/")) {
            path = path.substring((MATERIAL_RESOURCE_PATH + "/").length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return path.replace('/', '_').toLowerCase(Locale.ROOT);
    }

    private static Identifier parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Identifier.tryParse(raw.toLowerCase(Locale.ROOT));
    }

    private static String simplifyId(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private static void addCsv(Map<String, String> metadata, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String existing = metadata.getOrDefault(key, "");
        for (String part : existing.split(",")) {
            if (!part.isBlank()) {
                values.add(part.trim());
            }
        }
        if (values.add(value)) {
            metadata.put(key, String.join(",", values));
        }
    }

    private static void addSearchToken(Map<String, String> metadata, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String existing = metadata.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        for (String value : existing.split("\\s+")) {
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        if (values.add(token)) {
            metadata.put(SearchNodeKeys.SEARCH_TOKENS, String.join(" ", values));
        }
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

    record MaterialRecord(String material, List<IngredientRef> ingredients, List<String> traits, List<String> details) {
        MaterialRecord {
            ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
            traits = traits == null ? List.of() : List.copyOf(traits);
            details = details == null ? List.of() : List.copyOf(details);
        }

        boolean matches(SearchNode node) {
            if (node == null || ingredients.isEmpty()) {
                return false;
            }
            for (IngredientRef ingredient : ingredients) {
                if (ingredient.matches(node)) {
                    return true;
                }
            }
            return false;
        }
    }

    record IngredientRef(Identifier id, boolean tag) {
        static IngredientRef item(Identifier id) {
            return new IngredientRef(id, false);
        }

        static IngredientRef tag(Identifier id) {
            return new IngredientRef(id, true);
        }

        boolean matches(SearchNode node) {
            if (tag) {
                String expected = id.toString();
                for (String value : node.meta(SearchNodeKeys.TAGS, "").split(",")) {
                    if (expected.equals(value.trim())) {
                        return true;
                    }
                }
                return false;
            }
            return id.equals(node.id());
        }
    }

    private record MaterialMerge(Set<String> materials, Set<String> traits, Set<String> details) {
        boolean isEmpty() {
            return materials.isEmpty() && traits.isEmpty() && details.isEmpty();
        }
    }

    private static final class ClientResourceAccess {
        private ClientResourceAccess() {
        }

        private static void applyToIndex(GlobalIndex index) {
            ResourceManager resourceManager = resourceManager();
            if (resourceManager == null) {
                return;
            }
            List<MaterialRecord> records = materialRecords(resourceManager);
            applyMaterialRecords(index, records);
        }

        private static List<MaterialRecord> materialRecords(ResourceManager resourceManager) {
            Map<Identifier, Resource> resources = resourceManager.listResources(MATERIAL_RESOURCE_PATH,
                    id -> "silentgear".equals(id.getNamespace()) && id.getPath().endsWith(".json"));
            List<MaterialRecord> out = new ArrayList<>();
            resources.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                    .forEach(entry -> readMaterial(out, entry.getKey(), entry.getValue()));
            return List.copyOf(out);
        }

        private static ResourceManager resourceManager() {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var server = minecraft.getSingleplayerServer();
            if (server != null) {
                return server.getResourceManager();
            }
            return minecraft.getResourceManager();
        }

        private static void readMaterial(List<MaterialRecord> out, Identifier id, Resource resource) {
            try (BufferedReader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                MaterialRecord record = materialRecord(id, json);
                if (!record.ingredients().isEmpty() && !record.traits().isEmpty()) {
                    out.add(record);
                }
            } catch (IOException | RuntimeException ignored) {
                // Broken third-party data should not block indexing.
            }
        }
    }
}
