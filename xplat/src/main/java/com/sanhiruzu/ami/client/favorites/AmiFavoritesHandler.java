package com.sanhiruzu.ami.client.favorites;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.EmiFavoritesBridge;
import com.sanhiruzu.ami.compat.JeiFavoritesBridge;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.resolvers.PlayerResolver;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.player.PlayerWaypointProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * AMI-owned canonical favorites store.
 * External viewers are optional sync peers, not the source of truth.
 */
public class AmiFavoritesHandler {
    private static final AmiFavoritesHandler INSTANCE = new AmiFavoritesHandler();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "favorites.json";
    private static final int FORMAT_VERSION = 2;
    private static boolean persistenceEnabled = true;

    private final List<FavoriteRecord> records = new ArrayList<>();
    private Runnable onChange;

    private AmiFavoritesHandler() {
        loadState();
        mergeExternalFavorites(false);
    }

    public static AmiFavoritesHandler getInstance() {
        return INSTANCE;
    }

    public static void disablePersistenceForTests() {
        persistenceEnabled = false;
    }

    public static void clearForTests() {
        INSTANCE.records.clear();
        if (persistenceEnabled) {
            Path file = resolveFile();
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static ItemStack resolveStack(SearchNode node) {
        ItemStack stack = ItemIconRenderer.resolveStack(node.id());
        if (stack.isEmpty() && node.type() == NodeType.ENTITY) {
            ResourceLocation eggId = Services.PLATFORM.rl(node.id().getNamespace(), node.id().getPath() + "_spawn_egg");
            Item item = BuiltInRegistries.ITEM.getOptional(eggId).orElse(null);
            stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
        return stack;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void externalFavoritesChanged() {
        mergeExternalFavorites(true);
    }

    public void toggleFavorite(SearchNode node) {
        if (node == null) return;
        if (isFavorite(node)) {
            removeFavorite(node);
        } else {
            addFavorite(node);
        }
    }

    public void toggleFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (isFavorite(stack)) {
            removeFavorite(stack);
        } else {
            addFavorite(stack);
        }
    }

    public boolean isFavorite(SearchNode node) {
        String key = recordKeyForNode(node);
        return key != null && findRecordIndexByKey(key) >= 0;
    }

    public boolean isFavorite(ItemStack stack) {
        String key = FavoriteRecord.itemRecordKey(stack);
        return !key.isBlank() && findRecordIndexByKey(key) >= 0;
    }

    public boolean isRecipeFavorite(ResourceLocation recipeId, ItemStack stack) {
        String key = FavoriteRecord.recipeRecordKey(recipeId, stack);
        return !key.isBlank() && findRecordIndexByKey(key) >= 0;
    }

    public void addFavorite(SearchNode node) {
        FavoriteRecord record = recordForNode(node);
        if (record == null) return;
        upsertRecord(record, records.size(), true);
        syncAdd(record, records.size() - 1);
        persistState();
        notifyChange();
    }

    public void addFavorite(ItemStack stack) {
        FavoriteRecord record = FavoriteRecord.forItemStack(stack, "ami");
        if (record == null) return;
        upsertRecord(record, records.size(), true);
        syncAdd(record, records.size() - 1);
        persistState();
        notifyChange();
    }

    public void addFavoriteAt(ItemStack stack, int index) {
        FavoriteRecord record = FavoriteRecord.forItemStack(stack, "ami");
        if (record == null) return;
        int clamped = clampInsertIndex(index);
        upsertRecord(record, clamped, true);
        syncAdd(record, clamped);
        persistState();
        notifyChange();
    }

    public void moveFavorite(SearchNode node, int index) {
        String key = recordKeyForNode(node);
        if (key == null) return;
        int existing = findRecordIndexByKey(key);
        if (existing < 0) return;
        FavoriteRecord record = records.remove(existing);
        records.add(clampInsertIndex(index), record);
        persistState();
        notifyChange();
    }

    public void removeFavorite(SearchNode node) {
        String key = recordKeyForNode(node);
        if (key == null) return;
        removeFavoriteByKey(key, true);
    }

    public void removeFavorite(ItemStack stack) {
        String key = FavoriteRecord.itemRecordKey(stack);
        if (key.isBlank()) return;
        removeFavoriteByKey(key, true);
    }

    public void addRecipeFavorite(ResourceLocation recipeId, ItemStack stack) {
        FavoriteRecord record = FavoriteRecord.forRecipeStack(stack, recipeId, "ami");
        if (record == null) return;
        upsertRecord(record, records.size(), true);
        syncAdd(record, records.size() - 1);
        persistState();
        notifyChange();
    }

    public void removeRecipeFavorite(ResourceLocation recipeId, ItemStack stack) {
        String key = FavoriteRecord.recipeRecordKey(recipeId, stack);
        if (key.isBlank()) return;
        removeFavoriteByKey(key, true);
    }

    public List<SearchNode> getFavorites() {
        mergeExternalFavorites(false);
        List<SearchNode> out = new ArrayList<>(records.size());
        for (FavoriteRecord record : records) {
            SearchNode node = toDisplayNode(record);
            if (node != null) {
                out.add(node);
            }
        }
        return List.copyOf(out);
    }

    private void removeFavoriteByKey(String key, boolean syncExternal) {
        int existing = findRecordIndexByKey(key);
        if (existing < 0) return;
        FavoriteRecord removed = records.remove(existing);
        if (syncExternal) {
            syncRemove(removed);
        }
        persistState();
        notifyChange();
    }

    private void upsertRecord(FavoriteRecord record, int index, boolean preferIncomingPayload) {
        int existing = findRecordIndexByKey(record.recordKey());
        if (existing >= 0) {
            FavoriteRecord current = records.remove(existing);
            FavoriteRecord merged = preferIncomingPayload ? current.merge(record) : record.merge(current);
            records.add(clampInsertIndex(index), merged);
            return;
        }
        records.add(clampInsertIndex(index), record);
    }

    private int clampInsertIndex(int index) {
        return Math.max(0, Math.min(index, records.size()));
    }

    private int findRecordIndexByKey(String key) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).recordKey().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private FavoriteRecord recordForNode(SearchNode node) {
        if (node == null || node.id() == null) {
            return null;
        }
        if (node.type() == NodeType.ITEM) {
            ResourceLocation recipeId = parseRecipeId(node);
            ItemStack stack = resolveStack(node);
            if (recipeId != null && !stack.isEmpty()) {
                return FavoriteRecord.forRecipeStack(stack, recipeId, "ami");
            }
            if (!stack.isEmpty()) {
                return FavoriteRecord.forItemStack(stack, "ami");
            }
            return null;
        }
        return FavoriteRecord.forRuntimeNode(node, "ami");
    }

    private static String recordKeyForNode(SearchNode node) {
        if (node == null || node.id() == null) {
            return null;
        }
        if (node.type() == NodeType.ITEM) {
            ResourceLocation recipeId = parseRecipeId(node);
            ItemStack stack = resolveStack(node);
            if (recipeId != null && !stack.isEmpty()) {
                return FavoriteRecord.recipeRecordKey(recipeId, stack);
            }
            if (!stack.isEmpty()) {
                return FavoriteRecord.itemRecordKey(stack);
            }
            return null;
        }
        return FavoriteRecord.runtimeRecordKey(node.type(), node.id());
    }

    private static ResourceLocation parseRecipeId(SearchNode node) {
        String recipeId = node.meta(FavoriteEntry.META_RECIPE_ID);
        return recipeId == null || recipeId.isBlank() ? null : ResourceLocation.tryParse(recipeId);
    }

    private SearchNode toDisplayNode(FavoriteRecord record) {
        if (record == null) return null;
        return switch (record.kind()) {
            case ITEM_STACK, RECIPE_OUTPUT -> record.toFavoriteNode();
            case RUNTIME_NODE -> resolveRuntimeFavorite(record.runtimeNodeSnapshot());
        };
    }

    private static SearchNode resolveRuntimeFavorite(SearchNode snapshot) {
        if (snapshot == null || snapshot.id() == null) {
            return null;
        }
        if (snapshot.type() == NodeType.PLAYER) {
            return PlayerResolver.livePlayerNodes().stream()
                    .filter(node -> snapshot.id().equals(node.id()))
                    .findFirst()
                    .orElseGet(() -> staleRuntimeFavorite(snapshot, "offline"));
        }
        if (snapshot.type() == NodeType.WAYPOINT) {
            return PlayerWaypointProviders.liveWaypointNodes().stream()
                    .filter(node -> snapshot.id().equals(node.id()))
                    .findFirst()
                    .orElseGet(() -> staleRuntimeFavorite(snapshot, "unavailable"));
        }
        return snapshot;
    }

    private static SearchNode staleRuntimeFavorite(SearchNode snapshot, String reason) {
        Map<String, String> metadata = new HashMap<>(snapshot.metadata());
        metadata.put(SearchNodeKeys.RUNTIME_FAVORITE_STATE, "stale");
        metadata.put(SearchNodeKeys.RUNTIME_FAVORITE_REASON, reason == null ? "" : reason);
        if (snapshot.type() == NodeType.PLAYER) {
            metadata.put(SearchNodeKeys.PLAYER_ONLINE, "false");
        }
        String suffix = switch (snapshot.type()) {
            case PLAYER -> " (Offline)";
            case WAYPOINT -> " (Unavailable)";
            default -> "";
        };
        String displayName = snapshot.displayName();
        if (!suffix.isBlank() && !displayName.endsWith(suffix)) {
            displayName += suffix;
        }
        return new SearchNode(snapshot.id(), snapshot.type(), displayName, snapshot.color(), snapshot.searchWeight(), metadata);
    }

    private void mergeExternalFavorites(boolean notify) {
        boolean changed = false;
        for (FavoriteEntry entry : externalFavoriteEntries()) {
            FavoriteRecord imported = FavoriteRecord.fromExternalEntry(entry);
            if (imported == null) continue;
            int existing = findRecordIndexByKey(imported.recordKey());
            if (existing >= 0) {
                FavoriteRecord merged = records.get(existing).merge(imported);
                if (!merged.equals(records.get(existing))) {
                    records.set(existing, merged);
                    changed = true;
                }
                continue;
            }
            records.add(imported);
            changed = true;
        }
        if (changed) {
            persistState();
            if (notify) {
                notifyChange();
            }
        } else if (notify) {
            notifyChange();
        }
    }

    private static List<FavoriteEntry> externalFavoriteEntries() {
        List<FavoriteEntry> entries = new ArrayList<>();
        try {
            if (Services.PLATFORM.isModLoaded("emi")) {
                entries.addAll(EmiFavoritesBridge.getFavoriteEntries());
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        try {
            if (Services.PLATFORM.isModLoaded("jei")) {
                entries.addAll(JeiFavoritesBridge.getFavoriteEntries());
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return entries;
    }

    private void syncAdd(FavoriteRecord record, int index) {
        if (record == null) return;
        switch (record.kind()) {
            case ITEM_STACK -> {
                ItemStack stack = record.renderStack();
                if (stack.isEmpty()) return;
                if (Services.PLATFORM.isModLoaded("emi")) {
                    EmiFavoritesBridge.addFavoriteAt(stack, index);
                }
                if (Services.PLATFORM.isModLoaded("jei")) {
                    JeiFavoritesBridge.addFavoriteAt(stack, index);
                }
            }
            case RECIPE_OUTPUT -> {
                ItemStack stack = record.renderStack();
                if (stack.isEmpty()) return;
                if (Services.PLATFORM.isModLoaded("emi")) {
                    EmiFavoritesBridge.addRecipeFavorite(stack, record.recipeId());
                }
            }
            case RUNTIME_NODE -> {
            }
        }
    }

    private void syncRemove(FavoriteRecord record) {
        if (record == null) return;
        switch (record.kind()) {
            case ITEM_STACK -> {
                ItemStack stack = record.renderStack();
                if (stack.isEmpty()) return;
                if (Services.PLATFORM.isModLoaded("emi")) {
                    EmiFavoritesBridge.removeFavorite(stack);
                }
                if (Services.PLATFORM.isModLoaded("jei")) {
                    JeiFavoritesBridge.removeFavorite(stack);
                }
            }
            case RECIPE_OUTPUT -> {
                ItemStack stack = record.renderStack();
                if (stack.isEmpty()) return;
                if (Services.PLATFORM.isModLoaded("emi")) {
                    EmiFavoritesBridge.removeRecipeFavorite(stack, record.recipeId());
                }
            }
            case RUNTIME_NODE -> {
            }
        }
    }

    private void notifyChange() {
        if (onChange != null) {
            onChange.run();
        }
    }

    private void loadState() {
        if (!persistenceEnabled) {
            return;
        }
        Path file = resolveFile();
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("records")) {
                loadCanonicalState(root.getAsJsonArray("records"));
                return;
            }
            loadLegacyState(root);
        } catch (RuntimeException | IOException e) {
            AmiCore.LOGGER.warn("AMI: Failed to load favorites store: {}", e.getMessage());
        }
    }

    private void loadCanonicalState(JsonArray array) {
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            FavoriteRecord record = FavoriteRecord.fromJson(element.getAsJsonObject());
            if (record != null) {
                records.add(record);
            }
        }
    }

    private void loadLegacyState(JsonObject root) {
        JsonArray localNodes = root.has("localNodes") ? root.getAsJsonArray("localNodes") : new JsonArray();
        for (JsonElement element : localNodes) {
            if (!element.isJsonObject()) continue;
            SearchNode node = nodeFromJson(element.getAsJsonObject());
            if (node != null) {
                FavoriteRecord record = FavoriteRecord.forRuntimeNode(node, "ami");
                if (record != null) {
                    records.add(record);
                }
            }
        }
    }

    private void persistState() {
        if (!persistenceEnabled) {
            return;
        }
        Path file = resolveFile();
        if (file == null) return;
        JsonObject root = new JsonObject();
        root.addProperty("version", FORMAT_VERSION);
        JsonArray array = new JsonArray();
        for (FavoriteRecord record : records) {
            array.add(record.toJson());
        }
        root.add("records", array);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            AmiCore.LOGGER.warn("AMI: Failed to save favorites store: {}", e.getMessage());
        }
    }

    private static SearchNode nodeFromJson(JsonObject json) {
        ResourceLocation id = json.has("id") ? ResourceLocation.tryParse(json.get("id").getAsString()) : null;
        if (id == null || !json.has("type")) {
            return null;
        }
        NodeType type = NodeType.valueOf(json.get("type").getAsString());
        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : id.toString();
        int color = json.has("color") ? json.get("color").getAsInt() : 0;
        int searchWeight = json.has("searchWeight") ? json.get("searchWeight").getAsInt() : 0;
        Map<String, String> metadata = new HashMap<>();
        JsonObject rawMetadata = json.has("metadata") && json.get("metadata").isJsonObject()
                ? json.getAsJsonObject("metadata")
                : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : rawMetadata.entrySet()) {
            metadata.put(entry.getKey(), entry.getValue().getAsString());
        }
        return new SearchNode(id, type, displayName, color, searchWeight, metadata);
    }

    private static JsonObject nodeToJson(SearchNode node) {
        JsonObject json = new JsonObject();
        json.addProperty("id", node.id().toString());
        json.addProperty("type", node.type().name());
        json.addProperty("displayName", node.displayName());
        json.addProperty("color", node.color());
        json.addProperty("searchWeight", node.searchWeight());
        JsonObject metadata = new JsonObject();
        for (Map.Entry<String, String> entry : node.metadata().entrySet()) {
            metadata.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("metadata", metadata);
        return json;
    }

    private static ItemStack deserializeStack(ResourceLocation itemId, String serialized) {
        return baseStack(itemId);
    }

    private static ItemStack baseStack(ResourceLocation itemId) {
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Path resolveFile() {
        try {
            return Services.PLATFORM.getConfigDir().resolve("ami").resolve(FILE_NAME);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    enum FavoriteKind {
        ITEM_STACK,
        RECIPE_OUTPUT,
        RUNTIME_NODE
    }

    record FavoriteRecord(
            FavoriteKind kind,
            String recordKey,
            ResourceLocation nodeId,
            ResourceLocation itemId,
            ItemStack stack,
            ResourceLocation recipeId,
            SearchNode runtimeNodeSnapshot,
            String source
    ) {
        FavoriteRecord {
            recordKey = recordKey == null ? "" : recordKey;
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
            source = source == null ? "ami" : source;
        }

        static FavoriteRecord forItemStack(ItemStack stack, String source) {
            if (stack == null || stack.isEmpty()) return null;
            FavoriteEntry entry = FavoriteEntry.item(stack, source);
            if (entry == null) return null;
            return new FavoriteRecord(
                    FavoriteKind.ITEM_STACK,
                    entry.key(),
                    entry.nodeId(),
                    entry.itemId(),
                    entry.stack(),
                    null,
                    null,
                    entry.source()
            );
        }

        static FavoriteRecord forRecipeStack(ItemStack stack, ResourceLocation recipeId, String source) {
            if (stack == null || stack.isEmpty() || recipeId == null) return null;
            FavoriteEntry entry = FavoriteEntry.recipe(stack, recipeId, source);
            if (entry == null) return null;
            return new FavoriteRecord(
                    FavoriteKind.RECIPE_OUTPUT,
                    entry.key(),
                    entry.nodeId(),
                    entry.itemId(),
                    entry.stack(),
                    recipeId,
                    null,
                    entry.source()
            );
        }

        static FavoriteRecord forRuntimeNode(SearchNode node, String source) {
            if (node == null || node.id() == null) return null;
            return new FavoriteRecord(
                    FavoriteKind.RUNTIME_NODE,
                    runtimeRecordKey(node.type(), node.id()),
                    node.id(),
                    null,
                    ItemStack.EMPTY,
                    null,
                    node,
                    source
            );
        }

        static FavoriteRecord fromExternalEntry(FavoriteEntry entry) {
            if (entry == null) return null;
            return entry.isRecipeFavorite()
                    ? forRecipeStack(entry.stack(), entry.recipeId(), entry.source())
                    : forItemStack(entry.stack(), entry.source());
        }

        static String itemRecordKey(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return "";
            return "item|" + FavoriteEntry.stackKey(stack);
        }

        static String recipeRecordKey(ResourceLocation recipeId, ItemStack stack) {
            if (recipeId == null || stack == null || stack.isEmpty()) return "";
            return "recipe|" + recipeId + "|" + FavoriteEntry.stackKey(stack);
        }

        static String runtimeRecordKey(NodeType type, ResourceLocation id) {
            return "node|" + (type == null ? "UNKNOWN" : type.name()) + "|" + id;
        }

        FavoriteRecord merge(FavoriteRecord incoming) {
            if (incoming == null || !recordKey.equals(incoming.recordKey)) {
                return this;
            }
            return new FavoriteRecord(
                    incoming.kind != null ? incoming.kind : kind,
                    recordKey,
                    incoming.nodeId != null ? incoming.nodeId : nodeId,
                    incoming.itemId != null ? incoming.itemId : itemId,
                    !incoming.stack.isEmpty() ? incoming.stack : stack,
                    incoming.recipeId != null ? incoming.recipeId : recipeId,
                    incoming.runtimeNodeSnapshot != null ? incoming.runtimeNodeSnapshot : runtimeNodeSnapshot,
                    incoming.source == null || incoming.source.isBlank() ? source : incoming.source
            );
        }

        ItemStack renderStack() {
            if (!stack.isEmpty()) {
                return stack.copy();
            }
            return baseStack(itemId);
        }

        SearchNode toFavoriteNode() {
            ItemStack renderStack = renderStack();
            if (renderStack.isEmpty() || itemId == null || nodeId == null) {
                return null;
            }
            ItemIconRenderer.registerStack(nodeId, renderStack);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put(FavoriteEntry.META_KIND, kind == FavoriteKind.RECIPE_OUTPUT ? "recipe" : "item");
            metadata.put(FavoriteEntry.META_BASE_ID, itemId.toString());
            metadata.put(FavoriteEntry.META_SOURCE, source);
            if (recipeId != null) {
                metadata.put(FavoriteEntry.META_RECIPE_ID, recipeId.toString());
            }
            return new SearchNode(nodeId, NodeType.ITEM, renderStack.getHoverName().getString(), 0, 0, metadata);
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("kind", kind.name());
            json.addProperty("recordKey", recordKey);
            if (nodeId != null) json.addProperty("nodeId", nodeId.toString());
            if (itemId != null) json.addProperty("itemId", itemId.toString());
            if (recipeId != null) json.addProperty("recipeId", recipeId.toString());
            if (source != null && !source.isBlank()) json.addProperty("source", source);
            if (kind == FavoriteKind.RUNTIME_NODE && runtimeNodeSnapshot != null) {
                json.add("runtimeNode", nodeToJson(runtimeNodeSnapshot));
            }
            return json;
        }

        static FavoriteRecord fromJson(JsonObject json) {
            try {
                FavoriteKind kind = FavoriteKind.valueOf(json.get("kind").getAsString());
                String recordKey = json.get("recordKey").getAsString();
                ResourceLocation nodeId = json.has("nodeId") ? ResourceLocation.tryParse(json.get("nodeId").getAsString()) : null;
                ResourceLocation itemId = json.has("itemId") ? ResourceLocation.tryParse(json.get("itemId").getAsString()) : null;
                ResourceLocation recipeId = json.has("recipeId") ? ResourceLocation.tryParse(json.get("recipeId").getAsString()) : null;
                String source = json.has("source") ? json.get("source").getAsString() : "ami";
                if (kind == FavoriteKind.RUNTIME_NODE) {
                    SearchNode snapshot = json.has("runtimeNode") && json.get("runtimeNode").isJsonObject()
                            ? nodeFromJson(json.getAsJsonObject("runtimeNode"))
                            : null;
                    return snapshot == null ? null : new FavoriteRecord(kind, recordKey, nodeId, null, ItemStack.EMPTY, null, snapshot, source);
                }
                String stackNbt = json.has("stackNbt") ? json.get("stackNbt").getAsString() : "";
                ItemStack stack = deserializeStack(itemId, stackNbt);
                return new FavoriteRecord(kind, recordKey, nodeId, itemId, stack, recipeId, null, source);
            } catch (RuntimeException e) {
                return null;
            }
        }
    }
}
