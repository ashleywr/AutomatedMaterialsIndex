package com.sanhiruzu.ami.index;

import com.google.gson.*;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serialization cache for GlobalIndex.
 * Caches ITEM, FLUID, INGREDIENT, BIOME, and ENTITY nodes (keyed by mod list hash).
 * STRUCTURE and DIMENSION are always live-loaded (world/datapack-specific).
 */
public final class GlobalIndexCache {
    private static final int CACHE_VERSION = 55; // Bump this when index data format changes

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final String CACHE_DIR = "config/ami/cache";
    private static final boolean DISABLE_INDEX_CACHE = Boolean.getBoolean("ami.debug.disableIndexCache");

    private static final AtomicReference<JsonObject> preloadedData = new AtomicReference<>();
    // Path used when preloadedData was parsed; used to detect key drift (e.g. language change).
    private static volatile Path preloadedPath = null;

    private GlobalIndexCache() {
    }

    /**
     * Starts async deserialization of the on-disk cache into memory before any world join.
     * Call at game startup (first client tick). Safe to call more than once; no-ops after first
     * call. If the cache key changes by world-join time (language switch, etc.) the pre-loaded
     * data is discarded and tryLoad() falls back to normal disk read.
     */
    public static void preloadAsync() {
        if (DISABLE_INDEX_CACHE) return;
        if (preloadedData.get() != null || preloadedPath != null) return; // already pre-loading / done
        CompletableFuture.runAsync(() -> {
            Path cacheFile;
            try {
                cacheFile = resolveCacheFile();
            } catch (Exception e) {
                AmiCore.LOGGER.debug("AMI: Cache pre-load skipped — key unavailable: {}", e.getMessage());
                return;
            }
            if (!Files.exists(cacheFile)) {
                AmiCore.LOGGER.debug("AMI: Cache pre-load skipped — no cache file at {}", cacheFile.getFileName());
                return;
            }
            preloadedPath = cacheFile;
            try (var gzipIn = new GZIPInputStream(Files.newInputStream(cacheFile));
                 var reader = new InputStreamReader(gzipIn, StandardCharsets.UTF_8)) {
                JsonObject parsed = JsonParser.parseReader(reader).getAsJsonObject();
                preloadedData.set(parsed);
                AmiCore.LOGGER.info("AMI: Cache pre-loaded into memory ({})", cacheFile.getFileName());
            } catch (Exception e) {
                AmiCore.LOGGER.debug("AMI: Cache pre-load failed: {}", e.getMessage());
                preloadedPath = null;
            }
        }, Util.backgroundExecutor());
    }

    /**
     * Try to load cache. Returns true if successful, false if cache miss or error.
     */
    public static boolean tryLoad() {
        if (DISABLE_INDEX_CACHE) {
            AmiCore.LOGGER.info("AMI: Index cache disabled by -Dami.debug.disableIndexCache=true; rebuilding.");
            return false;
        }

        // Use pre-loaded data if the cache key still matches (avoids disk I/O entirely).
        JsonObject preloaded = preloadedData.getAndSet(null);
        if (preloaded != null) {
            Path expectedFile = resolveCacheFile();
            if (expectedFile.equals(preloadedPath)) {
                AmiCore.LOGGER.debug("AMI: Applying pre-loaded cache (no disk read)");
                AmiIndexerService.getInstance().beginProgress("Loading index cache");
                try {
                    deserializeInto(GlobalIndex.getInstance(), preloaded);
                    AmiCore.LOGGER.debug("AMI: Applied pre-loaded cache: {}", expectedFile.getFileName());
                    return true;
                } catch (Exception e) {
                    AmiCore.LOGGER.warn("AMI: Pre-loaded cache apply failed, will re-index: {}", e.getMessage());
                    return false;
                }
            } else {
                AmiCore.LOGGER.debug("AMI: Pre-loaded cache key drifted (language change?), falling back to disk read");
            }
        }
        preloadedPath = null;

        Path cacheFile = resolveCacheFile();
        if (!Files.exists(cacheFile)) return false;
        AmiIndexerService.getInstance().beginProgress("Loading index cache");
        try (var gzipIn = new GZIPInputStream(Files.newInputStream(cacheFile));
             var reader = new InputStreamReader(gzipIn, StandardCharsets.UTF_8)) {
            deserializeInto(GlobalIndex.getInstance(), reader);
            AmiCore.LOGGER.debug("AMI: Loaded index from cache: {}", cacheFile.getFileName());
            return true;
        } catch (Exception e) {
            AmiCore.LOGGER.warn("AMI: Cache load failed, will re-index: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Serialize the current GlobalIndex to the cache file.
     */
    public static void save() {
        if (DISABLE_INDEX_CACHE) {
            AmiCore.LOGGER.info("AMI: Index cache disabled by -Dami.debug.disableIndexCache=true; skipping cache save.");
            return;
        }

        Path cacheFile = resolveCacheFile();
        try {
            Files.createDirectories(cacheFile.getParent());
            try (var gzipOut = new GZIPOutputStream(Files.newOutputStream(cacheFile));
                 var writer = new OutputStreamWriter(gzipOut, StandardCharsets.UTF_8)) {
                serializeFrom(GlobalIndex.getInstance(), writer);
            }
            AmiCore.LOGGER.debug("AMI: Cache written: {}", cacheFile.getFileName());
        } catch (Exception e) {
            AmiCore.LOGGER.warn("AMI: Cache save failed: {}", e.getMessage());
        }
    }

    /**
     * Deletes the cache file for the current mod list/config/language key.
     *
     * @return true when an existing cache file was deleted, false when there was no current cache file.
     */
    public static boolean invalidateCurrent() throws IOException {
        Path cacheFile = resolveCacheFile();
        boolean deleted = Files.deleteIfExists(cacheFile);
        if (deleted) {
            AmiCore.LOGGER.info("AMI: Deleted index cache {}", cacheFile.toAbsolutePath());
        } else {
            AmiCore.LOGGER.info("AMI: No current index cache to delete at {}", cacheFile.toAbsolutePath());
        }
        return deleted;
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

                        // Recipe index is runtime-only and not serialized with the global cache. Rebuild it
                        // here so native recipe lookups function without requiring a full reindex.
                        if (!AmiRecipeIndex.getInstance().isBuilt()) {
                            try {
                                AmiRecipeIndex.getInstance().rebuild(level);
                            } catch (RuntimeException e) {
                                AmiCore.LOGGER.warn("AMI: Recipe index rebuild after cache restore failed: {}", e.getMessage(), e);
                            }
                        }
                    }
                    GlobalIndex.getInstance().markIndexReady();
                }, Util.backgroundExecutor())
                .thenRunAsync(onComplete, cmd -> Minecraft.getInstance().execute(cmd));
    }

    private static Path resolveCacheFile() {
        String hash = computeModListHash();
        String language = currentClientLanguageCacheKey();
        Path gameDir = Services.PLATFORM.getGameDir();
        return gameDir.resolve(CACHE_DIR).resolve(language).resolve(hash + ".json.gz");
    }

    public static String currentClientLanguageCacheKey() {
        return normalizeLanguageCodeForCache(Services.PLATFORM.getClientLanguageCode());
    }

    static String computeModListHash() {
        try {
            String input = Services.PLATFORM.getLoadedModFingerprintEntries().stream()
                    .sorted(Comparator.naturalOrder())
                    .reduce("", (a, b) -> a + "|" + b)
                    + "_v" + CACHE_VERSION
                    + "_lang=" + currentClientLanguageCacheKey()
                    + "_hidden=" + AmiConfig.showHiddenModItems
                    + "_strictSurvival=" + AmiConfig.strictSurvivalMode
                    + "_cheat=" + AmiConfig.cheatMode
                    + "_dev=" + AmiConfig.devMode
                    + IndexingHotItemPolicy.cacheKeyFragment();

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            AmiCore.LOGGER.warn("AMI: Hash computation failed, using fallback key");
            return "fallback";
        }
    }

    static String normalizeLanguageCodeForCache(String rawLanguageCode) {
        String language = rawLanguageCode == null ? "" : rawLanguageCode.trim().toLowerCase(Locale.ROOT);
        if (language.isEmpty()) {
            return "en_us";
        }

        language = language.replace('-', '_');
        String normalized = language.replaceAll("[^a-z0-9_]", "_");
        normalized = normalized.replaceAll("_+", "_");
        return normalized.isBlank() ? "en_us" : normalized;
    }

    private static void serializeFrom(GlobalIndex index, OutputStreamWriter writer) throws IOException {
        JsonObject root = new JsonObject();

        // Only cache non-deferred types.
        // STRUCTURE, DIMENSION, and RECIPE are always live-loaded (world/datapack-specific).
        for (NodeType type : NodeType.values()) {
            JsonArray array = new JsonArray();
            for (SearchNode node : index.getNodes(type)) {
                if (type == NodeType.STRUCTURE || type == NodeType.DIMENSION || type == NodeType.RECIPE) {
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
        deserializeInto(index, JsonParser.parseReader(reader).getAsJsonObject());
    }

    private static void deserializeInto(GlobalIndex index, JsonObject root) throws IOException {
        index.clear();
        int total = 0;
        for (String typeStr : root.keySet()) {
            JsonArray array = root.getAsJsonArray(typeStr);
            if (array != null) {
                total += array.size();
            }
        }
        AmiIndexerService progress = AmiIndexerService.getInstance();
        progress.beginProgress("Loading index cache", "", total);
        int loaded = 0;

        for (String typeStr : root.keySet()) {
            try {
                NodeType type = NodeType.valueOf(typeStr);
                JsonArray array = root.getAsJsonArray(typeStr);

                for (JsonElement elem : array) {
                    SearchNode node = nodeFromJson(elem.getAsJsonObject(), type);
                    index.addNode(node);
                    loaded++;
                    if ((loaded & 255) == 0 || loaded == total) {
                        progress.updateProgress(loaded);
                        progress.updateProgressDetail(node.id().toString());
                    }
                }
            } catch (IllegalArgumentException e) {
                AmiCore.LOGGER.warn("Unknown NodeType in cache: {}", typeStr);
            }
        }

        // Mark deferred types as still loading since they will be populated on first frame
        index.setLoading(NodeType.STRUCTURE, true);
        index.setLoading(NodeType.DIMENSION, true);
        index.setLoading(NodeType.RECIPE, true);
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
        ResourceLocation id = Services.PLATFORM.rl(obj.get("id").getAsString());
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
