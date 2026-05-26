package com.sanhiruzu.ami.index;

import com.google.gson.*;
import com.sanhiruzu.ami.forge.AMI;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serialization cache for GlobalIndex.
 * Caches ITEM, BIOME, and ENTITY nodes (keyed by mod list hash).
 * STRUCTURE and DIMENSION are always live-loaded (world/datapack-specific).
 */
public final class GlobalIndexCache {
    private static final int CACHE_VERSION = 5; // Bump this when index data format changes

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final String CACHE_DIR = "config/ami/cache";

    private GlobalIndexCache() {
    }

    /**
     * Try to load cache. Returns true if successful, false if cache miss or error.
     */
    public static boolean tryLoad() {
        Path cacheFile = resolveCacheFile();
        if (!Files.exists(cacheFile)) return false;
        try (var gzipIn = new GZIPInputStream(Files.newInputStream(cacheFile));
             var reader = new InputStreamReader(gzipIn, StandardCharsets.UTF_8)) {
            deserializeInto(GlobalIndex.getInstance(), reader);
            AMI.LOGGER.info("AMI: Loaded index from cache: {}", cacheFile.getFileName());
            return true;
        } catch (Exception e) {
            AMI.LOGGER.warn("AMI: Cache load failed, will re-index: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Serialize the current GlobalIndex to the cache file.
     */
    public static void save() {
        Path cacheFile = resolveCacheFile();
        try {
            Files.createDirectories(cacheFile.getParent());
            try (var gzipOut = new GZIPOutputStream(Files.newOutputStream(cacheFile));
                 var writer = new OutputStreamWriter(gzipOut, StandardCharsets.UTF_8)) {
                serializeFrom(GlobalIndex.getInstance(), writer);
            }
            AMI.LOGGER.info("AMI: Cache written: {}", cacheFile.getFileName());
        } catch (Exception e) {
            AMI.LOGGER.warn("AMI: Cache save failed: {}", e.getMessage());
        }
    }

    /**
     * Try cache load first; if miss, run full indexing and save to cache.
     * Deferred structure/dimension indexing still runs regardless of cache hit.
     *
     * @deprecated Use loadOrIndexAsync for non-blocking load.
     */
    @Deprecated
    public static void loadOrIndex(ClientLevel level) {
        if (!tryLoad()) {
            ProviderRegistry.indexAll(level);
            save();
        }
    }

    /**
     * Async entry point. Captures level reference on calling (render) thread,
     * then dispatches background work.
     *
     * @param level      captured on render thread before returning
     * @param onComplete scheduled back on render thread when indexing is done
     * @return future that completes when indexing is done
     */
    public static CompletableFuture<Void> loadOrIndexAsync(ClientLevel level, Runnable onComplete) {
        return CompletableFuture
                .runAsync(() -> {
                    GroupingEngine.initialize(level);
                    if (!tryLoad()) {
                        ProviderRegistry.indexAll(level);
                        save();
                    } else {
                        // Index data restored from cache, but per-session ItemStacks for synthetic
                        // nodes (potions, enchanted books, etc.) are not serialized. Rebuild them.
                        ProviderRegistry.rehydrateSubtypeStacks(level);
                    }
                    GlobalIndex.getInstance().markIndexReady();
                }, Util.backgroundExecutor())
                .thenRunAsync(onComplete, cmd -> Minecraft.getInstance().execute(cmd));
    }

    private static Path resolveCacheFile() {
        String hash = computeModListHash();
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        return gameDir.resolve(CACHE_DIR).resolve(hash + ".json.gz");
    }

    static String computeModListHash() {
        try {
            String input = ModList.get().getMods().stream()
                    .sorted(Comparator.comparing(info -> info.getModId()))
                    .map(info -> info.getModId() + ":" + info.getVersion())
                    .reduce("", (a, b) -> a + "|" + b) + "_v" + CACHE_VERSION;

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            AMI.LOGGER.warn("AMI: Hash computation failed, using fallback key");
            return "fallback";
        }
    }

    private static void serializeFrom(GlobalIndex index, OutputStreamWriter writer) throws IOException {
        JsonObject root = new JsonObject();

        // Only cache non-deferred types (ITEM, BIOME, ENTITY)
        // STRUCTURE and DIMENSION are always empty in cache (live-loaded)
        for (NodeType type : NodeType.values()) {
            JsonArray array = new JsonArray();
            for (SearchNode node : index.getNodes(type)) {
                if (type == NodeType.STRUCTURE || type == NodeType.DIMENSION) {
                    continue;
                }
                array.add(nodeToJson(node));
            }
            root.add(type.name(), array);
        }

        GSON.toJson(root, writer);
        writer.flush();
    }

    private static void deserializeInto(GlobalIndex index, InputStreamReader reader) throws IOException {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

        index.clear();

        for (String typeStr : root.keySet()) {
            try {
                NodeType type = NodeType.valueOf(typeStr);
                JsonArray array = root.getAsJsonArray(typeStr);

                for (JsonElement elem : array) {
                    SearchNode node = nodeFromJson(elem.getAsJsonObject(), type);
                    index.addNode(node);
                }
            } catch (IllegalArgumentException e) {
                AMI.LOGGER.warn("Unknown NodeType in cache: {}", typeStr);
            }
        }

        // Mark deferred types as still loading since they will be populated on first frame
        index.setLoading(NodeType.STRUCTURE, true);
        index.setLoading(NodeType.DIMENSION, true);
    }

    private static JsonObject nodeToJson(SearchNode node) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", node.id().toString());
        obj.addProperty("type", node.type().name());
        obj.addProperty("displayName", node.displayName());
        obj.addProperty("color", node.color());
        obj.addProperty("searchWeight", node.searchWeight());

        JsonObject meta = new JsonObject();
        for (Map.Entry<String, String> entry : node.metadata().entrySet()) {
            meta.addProperty(entry.getKey(), entry.getValue());
        }
        obj.add("metadata", meta);

        return obj;
    }

    private static SearchNode nodeFromJson(JsonObject obj, NodeType type) {
        ResourceLocation id = new ResourceLocation(obj.get("id").getAsString());
        String displayName = obj.get("displayName").getAsString();
        int color = obj.get("color").getAsInt();
        int searchWeight = obj.get("searchWeight").getAsInt();

        Map<String, String> metadata = new LinkedHashMap<>();
        JsonObject metaObj = obj.getAsJsonObject("metadata");
        for (String key : metaObj.keySet()) {
            metadata.put(key, metaObj.get(key).getAsString());
        }

        return new SearchNode(id, type, displayName, color, searchWeight, metadata);
    }
}
