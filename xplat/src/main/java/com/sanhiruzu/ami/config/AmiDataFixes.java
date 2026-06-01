package com.sanhiruzu.ami.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Local and pack-supplied metadata overrides for indexed nodes.
 *
 * User fixes are written to config/ami/fixes.json. Pack fixes can be shipped as
 * JSON files under <gameDir>/ami/fixes/ and are applied before user fixes.
 */
public final class AmiDataFixes {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();
    private static Map<NodeKey, FixEntry> packFixes = Map.of();
    private static Map<NodeKey, FixEntry> userFixes = Map.of();
    private static Map<NodeKey, FixEntry> mergedFixes = Map.of();

    private AmiDataFixes() {
    }

    public static Path userFile() {
        return Services.PLATFORM.getConfigDir().resolve("ami").resolve("fixes.json");
    }

    public static Path packDirectory() {
        return Services.PLATFORM.getGameDir().resolve("ami").resolve("fixes");
    }

    public static void reload() {
        synchronized (LOCK) {
            Map<NodeKey, FixEntry> loadedPackFixes = new LinkedHashMap<>();
            for (Path path : packFixFiles(packDirectory())) {
                loadedPackFixes.putAll(read(path, "pack:" + path.getFileName()));
            }

            Map<NodeKey, FixEntry> loadedUserFixes = read(userFile(), "user");
            packFixes = Map.copyOf(loadedPackFixes);
            userFixes = Map.copyOf(loadedUserFixes);
            mergedFixes = merge(packFixes, userFixes);
        }
    }

    public static boolean hasUserFix(ResourceLocation id, NodeType type) {
        if (id == null || type == null) return false;
        synchronized (LOCK) {
            return userFixes.containsKey(new NodeKey(id.toString(), type));
        }
    }

    public static Optional<Map<String, String>> userMetadata(ResourceLocation id, NodeType type) {
        if (id == null || type == null) return Optional.empty();
        synchronized (LOCK) {
            FixEntry entry = userFixes.get(new NodeKey(id.toString(), type));
            return entry == null ? Optional.empty() : Optional.of(entry.metadata());
        }
    }

    public static void putUserMetadataFix(ResourceLocation id, NodeType type, Map<String, String> metadata) {
        if (id == null || type == null || metadata == null || metadata.isEmpty()) return;
        synchronized (LOCK) {
            Map<NodeKey, FixEntry> mutable = new LinkedHashMap<>(userFixes);
            NodeKey key = new NodeKey(id.toString(), type);
            Map<String, String> mergedMetadata = new LinkedHashMap<>();
            FixEntry existing = mutable.get(key);
            if (existing != null) {
                mergedMetadata.putAll(existing.metadata());
            }
            mergedMetadata.putAll(cleanMetadata(metadata));
            mutable.put(key, new FixEntry(type, mergedMetadata, "user"));
            userFixes = Map.copyOf(mutable);
            mergedFixes = merge(packFixes, userFixes);
            writeUserFile(userFixes);
        }
    }

    public static void removeUserFix(ResourceLocation id, NodeType type) {
        if (id == null || type == null) return;
        synchronized (LOCK) {
            Map<NodeKey, FixEntry> mutable = new LinkedHashMap<>(userFixes);
            mutable.remove(new NodeKey(id.toString(), type));
            userFixes = Map.copyOf(mutable);
            mergedFixes = merge(packFixes, userFixes);
            writeUserFile(userFixes);
        }
    }

    public static Map<String, String> apply(ResourceLocation id, NodeType type, Map<String, String> metadata) {
        if (id == null || type == null || metadata == null) return metadata;
        synchronized (LOCK) {
            FixEntry entry = mergedFixes.get(new NodeKey(id.toString(), type));
            if (entry == null || entry.metadata().isEmpty()) return metadata;

            Map<String, String> updated = new LinkedHashMap<>(metadata);
            updated.putAll(entry.metadata());
            updated.put(SearchNodeKeys.DATA_FIX_SOURCE, entry.source());
            return updated;
        }
    }

    public static void applyToIndex(GlobalIndex index) {
        if (index == null) return;
        synchronized (LOCK) {
            if (mergedFixes.isEmpty()) return;
        }

        for (NodeType type : NodeType.values()) {
            List<SearchNode> nodes = new ArrayList<>(index.getNodes(type));
            for (SearchNode node : nodes) {
                Map<String, String> updated = apply(node.id(), node.type(), node.metadata());
                if (!updated.equals(node.metadata())) {
                    index.replaceNode(node.id(), node.type(), node.withMetadata(updated));
                }
            }
        }
    }

    static Map<NodeKey, FixEntry> read(Path path, String source) {
        if (path == null || !Files.isRegularFile(path)) return Map.of();
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return Map.of();

            JsonObject root = parsed.getAsJsonObject();
            JsonObject items = object(root, "items").orElse(root);
            Map<NodeKey, FixEntry> result = new LinkedHashMap<>();
            for (String id : items.keySet()) {
                JsonElement element = items.get(id);
                if (!element.isJsonObject()) continue;

                JsonObject rawEntry = element.getAsJsonObject();
                NodeType type = parseType(rawEntry).orElse(NodeType.ITEM);
                Map<String, String> metadata = readMetadata(rawEntry);
                if (metadata.isEmpty()) continue;

                result.put(new NodeKey(id, type), new FixEntry(type, metadata, source));
            }
            return result;
        } catch (IOException | RuntimeException e) {
            AmiCore.LOGGER.warn("AMI: Failed to load data fixes from {}", path, e);
            return Map.of();
        }
    }

    private static List<Path> packFixFiles(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) return List.of();
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            AmiCore.LOGGER.warn("AMI: Failed to list pack data fixes from {}", directory, e);
            return List.of();
        }
    }

    private static Optional<JsonObject> object(JsonObject root, String key) {
        JsonElement element = root.get(key);
        return element != null && element.isJsonObject()
                ? Optional.of(element.getAsJsonObject())
                : Optional.empty();
    }

    private static Optional<NodeType> parseType(JsonObject entry) {
        JsonElement typeElement = entry.get("type");
        if (typeElement == null || !typeElement.isJsonPrimitive()) return Optional.empty();
        try {
            return Optional.of(NodeType.valueOf(typeElement.getAsString().trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Map<String, String> readMetadata(JsonObject entry) {
        Map<String, String> metadata = new LinkedHashMap<>();
        object(entry, "metadata").ifPresent(meta -> {
            for (String key : meta.keySet()) {
                JsonElement value = meta.get(key);
                if (value != null && value.isJsonPrimitive()) {
                    metadata.put(key, value.getAsString());
                }
            }
        });

        addAlias(entry, metadata, "category", SearchNodeKeys.ONTOLOGY_CATEGORY);
        addAlias(entry, metadata, "subcategory", SearchNodeKeys.ONTOLOGY_SUBCATEGORY);
        addAlias(entry, metadata, "ontologyCategory", SearchNodeKeys.ONTOLOGY_CATEGORY);
        addAlias(entry, metadata, "ontologySubcategory", SearchNodeKeys.ONTOLOGY_SUBCATEGORY);
        return cleanMetadata(metadata);
    }

    private static void addAlias(JsonObject entry, Map<String, String> metadata, String alias, String metadataKey) {
        JsonElement value = entry.get(alias);
        if (value != null && value.isJsonPrimitive()) {
            metadata.put(metadataKey, value.getAsString());
        }
    }

    private static Map<String, String> cleanMetadata(Map<String, String> metadata) {
        Map<String, String> cleaned = new LinkedHashMap<>();
        for (var entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank() || value == null) continue;
            cleaned.put(key.trim(), value.trim());
        }
        return cleaned;
    }

    private static Map<NodeKey, FixEntry> merge(Map<NodeKey, FixEntry> pack, Map<NodeKey, FixEntry> user) {
        Map<NodeKey, FixEntry> merged = new LinkedHashMap<>();
        if (pack != null) merged.putAll(pack);
        if (user != null) merged.putAll(user);
        return Map.copyOf(merged);
    }

    private static void writeUserFile(Map<NodeKey, FixEntry> fixes) {
        Path file = userFile();
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);

        JsonObject items = new JsonObject();
        fixes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    JsonObject item = new JsonObject();
                    item.addProperty("type", entry.getValue().type().name());
                    JsonObject metadata = new JsonObject();
                    entry.getValue().metadata().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(meta -> metadata.addProperty(meta.getKey(), meta.getValue()));
                    item.add("metadata", metadata);
                    items.add(entry.getKey().id(), item);
                });
        root.add("items", items);

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            AmiCore.LOGGER.warn("AMI: Failed to save data fixes to {}", file, e);
        }
    }

    record NodeKey(String id, NodeType type) implements Comparable<NodeKey> {
        @Override
        public int compareTo(NodeKey other) {
            int typeCompare = type.compareTo(other.type);
            return typeCompare != 0 ? typeCompare : id.compareTo(other.id);
        }
    }

    record FixEntry(NodeType type, Map<String, String> metadata, String source) {
        FixEntry {
            metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
            source = source == null || source.isBlank() ? "unknown" : source;
        }
    }
}
