package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Bakes static entity thumbnails into a runtime atlas, then blits atlas regions
 * on normal grid frames. Persistent cache files are per-icon PNGs so a new bake
 * never rewrites the whole atlas image.
 */
public class EntityIconCache {

    private static final int ATLAS_SIZE = 2048;
    private static final boolean ADAPTIVE_BAKING =
            Boolean.parseBoolean(System.getProperty("ami.entityIconAtlasAdaptiveBake", "true"));
    private static final int ADAPTIVE_MIN_INTERVAL_TICKS =
            Math.max(1, Integer.getInteger("ami.entityIconAtlasAdaptiveMinIntervalTicks", 2));
    private static final int ADAPTIVE_MAX_INTERVAL_TICKS =
            Math.max(ADAPTIVE_MIN_INTERVAL_TICKS,
                    Integer.getInteger("ami.entityIconAtlasAdaptiveMaxIntervalTicks", 40));
    private static final long ADAPTIVE_TARGET_NANOS =
            Math.max(100_000L, Long.getLong("ami.entityIconAtlasAdaptiveTargetMs", 3L) * 1_000_000L);
    private static final long ADAPTIVE_BACKOFF_NANOS =
            Math.max(ADAPTIVE_TARGET_NANOS, Long.getLong("ami.entityIconAtlasAdaptiveBackoffMs", 8L) * 1_000_000L);
    // Queue entries retain render lambdas, which can retain entity instances. Keep this intentionally small.
    private static final int MAX_PENDING_BAKES =
            Math.max(1, Integer.getInteger("ami.entityIconAtlasPendingBakeLimit", 64));
    private static final AmiClientWorkScheduler.Lane BAKE_LANE = AmiClientWorkScheduler.lane(
            "entityIconAtlasBake",
            new AdaptiveTickScheduler.Config(
                    ADAPTIVE_BAKING,
                    Integer.getInteger("ami.entityIconAtlasBakeIntervalTicks", 10),
                    ADAPTIVE_MIN_INTERVAL_TICKS,
                    ADAPTIVE_MAX_INTERVAL_TICKS,
                    ADAPTIVE_TARGET_NANOS,
                    ADAPTIVE_BACKOFF_NANOS,
                    8));
    private static final Map<Integer, Atlas> atlases = new HashMap<>();
    private static final Set<CacheKey> failedKeys = new HashSet<>();
    private static final Map<CacheKey, BakeTask> pendingBakes = new LinkedHashMap<>();
    private static String activeFingerprint;
    private static long queuedBakeRequests;
    private static long droppedBakeRequests;
    private static long renderedBakeCount;
    private static long persistentLoadCount;
    private static long failedBakeCount;
    private static final ExecutorService PERSIST_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "AMI Entity Icon Atlas Writer");
        thread.setDaemon(true);
        thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
        return thread;
    });

    private EntityIconCache() {
    }

    /**
     * Blits the cached icon at (x, y). Cache misses are queued for later baking
     * and return false immediately so render frames do not block on framebuffer
     * captures.
     */
    public static boolean blitCached(GuiGraphics g, ResourceLocation id, int size, int x, int y,
                                     Consumer<GuiGraphics> renderToFramebuffer) {
        CacheKey cacheKey = new CacheKey(id, size);
        if (failedKeys.contains(cacheKey)) {
            return false;
        }

        Atlas atlas = atlas(size);
        EntityIconAtlasAllocator.AtlasEntry entry = atlas.entry(id);
        if (entry == null) {
            if (!failedKeys.contains(cacheKey)) {
                queueBake(cacheKey, renderToFramebuffer, true);
            }
            return false;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(atlas.textureKey(), x, y, entry.x(), entry.y(), size, size, ATLAS_SIZE, ATLAS_SIZE);
        return true;
    }

    /**
     * Requests a persistent atlas bake without drawing anything to the current
     * screen. Actual baking happens later through {@link #processPendingBakes}.
     */
    public static BakeRequestResult warmCached(ResourceLocation id, int size, Consumer<GuiGraphics> renderToFramebuffer) {
        CacheKey cacheKey = new CacheKey(id, size);
        if (failedKeys.contains(cacheKey)) {
            return BakeRequestResult.FAILED;
        }

        Atlas atlas = atlas(size);
        if (atlas.entry(id) != null) {
            return BakeRequestResult.CACHED;
        }

        return queueBake(cacheKey, renderToFramebuffer, false);
    }

    /**
     * Runs a bounded amount of queued atlas baking on the client thread.
     */
    public static void processPendingBakes(int maxBakes) {
        processPendingBakes(maxBakes, Long.MAX_VALUE);
    }

    public static void processPendingBakes(int maxBakes, long maxNanos) {
        int remaining = Math.max(0, maxBakes);
        long startedAt = System.nanoTime();
        while (remaining > 0 && !pendingBakes.isEmpty()) {
            BakeTask task = pendingBakes.values().iterator().next();
            pendingBakes.remove(task.key());
            long elapsedNanos = bakeNow(task);
            BAKE_LANE.recordWorkNanos(elapsedNanos);
            remaining--;
            if (System.nanoTime() - startedAt >= maxNanos) {
                break;
            }
        }
        flushAtlasUploads();
    }

    /**
     * Loads a cached icon from disk directly into the atlas without going through
     * the framebuffer bake queue. Returns true if the icon was already in the atlas
     * or was successfully loaded from persistent storage. Callers must call
     * {@link #flushAtlasUploads()} after batching multiple hydration calls.
     */
    public static boolean tryHydratePersistent(ResourceLocation id, int size) {
        CacheKey cacheKey = new CacheKey(id, size);
        if (failedKeys.contains(cacheKey)) return false;
        Atlas atlas = atlas(size);
        if (atlas.entry(id) != null) return true;
        return atlas.loadPersistent(id);
    }

    /** Uploads all dirty atlas textures to the GPU in a single batch. */
    public static void flushAtlasUploads() {
        for (Atlas atlas : atlases.values()) {
            atlas.flushUploads();
        }
    }

    public static void processPendingBakesAdaptive(int maxBakes) {
        processPendingBakesAdaptive(maxBakes, Long.MAX_VALUE);
    }

    public static void processPendingBakesAdaptive(int maxBakes, long maxNanos) {
        if (!BAKE_LANE.shouldRunThisTick()) {
            return;
        }
        processPendingBakes(maxBakes, maxNanos);
    }

    public static int pendingBakeCount() {
        return pendingBakes.size();
    }

    public static boolean isFailed(ResourceLocation id, int size) {
        return failedKeys.contains(new CacheKey(id, size));
    }

    public static Stats stats() {
        Map<Integer, AtlasStats> atlasStats = new LinkedHashMap<>();
        long pendingWrites = 0L;
        long droppedWrites = 0L;
        for (Map.Entry<Integer, Atlas> entry : atlases.entrySet()) {
            Atlas atlas = entry.getValue();
            atlasStats.put(entry.getKey(), atlas.stats());
            EntityIconPersistentStore.StoreStats storeStats = atlas.storeStats();
            pendingWrites += storeStats.pendingWrites();
            droppedWrites += storeStats.droppedWrites();
        }
        return new Stats(
                atlases.size(),
                pendingBakes.size(),
                failedKeys.size(),
                queuedBakeRequests,
                droppedBakeRequests,
                renderedBakeCount,
                persistentLoadCount,
                failedBakeCount,
                pendingWrites,
                droppedWrites,
                atlasStats
        );
    }

    private static BakeRequestResult queueBake(CacheKey cacheKey, Consumer<GuiGraphics> renderToFramebuffer, boolean priority) {
        if (failedKeys.contains(cacheKey)) {
            return BakeRequestResult.FAILED;
        }
        Atlas atlas = atlas(cacheKey.size());
        if (atlas.entry(cacheKey.id()) != null) {
            return BakeRequestResult.CACHED;
        }
        if (pendingBakes.containsKey(cacheKey)) {
            if (priority) {
                putPendingFirst(cacheKey, new BakeTask(cacheKey, renderToFramebuffer, true));
            }
            return BakeRequestResult.QUEUED;
        }
        if (pendingBakes.size() >= MAX_PENDING_BAKES) {
            if (!priority) {
                droppedBakeRequests++;
                return BakeRequestResult.QUEUE_FULL;
            }
            CacheKey evictableKey = null;
            for (Map.Entry<CacheKey, BakeTask> entry : pendingBakes.entrySet()) {
                if (!entry.getValue().priority()) {
                    evictableKey = entry.getKey();
                }
            }
            if (evictableKey == null) {
                return BakeRequestResult.QUEUE_FULL;
            }
            if (evictableKey != null) {
                pendingBakes.remove(evictableKey);
                droppedBakeRequests++;
            }
        }
        if (priority) {
            putPendingFirst(cacheKey, new BakeTask(cacheKey, renderToFramebuffer, true));
        } else {
            pendingBakes.put(cacheKey, new BakeTask(cacheKey, renderToFramebuffer, false));
        }
        queuedBakeRequests++;
        return BakeRequestResult.QUEUED;
    }

    private static void putPendingFirst(CacheKey cacheKey, BakeTask task) {
        LinkedHashMap<CacheKey, BakeTask> reordered = new LinkedHashMap<>();
        reordered.put(cacheKey, task);
        for (Map.Entry<CacheKey, BakeTask> entry : pendingBakes.entrySet()) {
            if (!entry.getKey().equals(cacheKey)) {
                reordered.put(entry.getKey(), entry.getValue());
            }
        }
        pendingBakes.clear();
        pendingBakes.putAll(reordered);
    }

    private static long bakeNow(BakeTask task) {
        long startedAt = System.nanoTime();
        CacheKey cacheKey = task.key();
        if (failedKeys.contains(cacheKey)) {
            return System.nanoTime() - startedAt;
        }

        Atlas atlas = atlas(cacheKey.size());
        if (atlas.entry(cacheKey.id()) != null) {
            return System.nanoTime() - startedAt;
        }
        if (atlas.loadPersistent(cacheKey.id())) {
            persistentLoadCount++;
            return System.nanoTime() - startedAt;
        }

        // Entity renderers are a Minecraft client-thread contract; the scheduler controls cadence, not threading.
        NativeImage image = null;
        try {
            image = bakeImage(cacheKey.size(), task.renderToFramebuffer());
        } catch (RuntimeException e) {
            failedKeys.add(cacheKey);
            failedBakeCount++;
            return System.nanoTime() - startedAt;
        }
        makeEdgeBackgroundTransparent(image);
        if (image == null || isBlankOrBlack(image)) {
            if (image != null) {
                image.close();
            }
            // Blank/black means textures probably haven't loaded yet — don't permanently fail.
            // The entity will be retried next time it's visible via blitCached.
            return System.nanoTime() - startedAt;
        }
        try {
            EntityIconAtlasAllocator.AtlasEntry entry = atlas.store(cacheKey.id(), image);
            if (entry == null) {
                failedKeys.add(cacheKey);
                failedBakeCount++;
            } else {
                renderedBakeCount++;
            }
        } finally {
            image.close();
        }
        return System.nanoTime() - startedAt;
    }

    /**
     * Release GL resources. Disk atlas files are intentionally kept for future
     * launches; resource-pack or mod-list changes derive a different cache key.
     */
    public static void invalidate() {
        Minecraft mc = Minecraft.getInstance();
        for (Atlas atlas : atlases.values()) {
            mc.getTextureManager().release(atlas.textureKey());
            atlas.close();
        }
        atlases.clear();
        failedKeys.clear();
        pendingBakes.clear();
        activeFingerprint = null;
        queuedBakeRequests = 0L;
        droppedBakeRequests = 0L;
        renderedBakeCount = 0L;
        persistentLoadCount = 0L;
        failedBakeCount = 0L;
        EntityIconSlowKeys.clear();
    }

    /**
     * Clears active atlas textures and removes all persistent cache files so
     * entities are fully re-rendered on next use.
     */
    public static void invalidateAndPurgePersistentCache() {
        invalidate();
        Path cacheRoot = cacheRootDir();
        if (!Files.exists(cacheRoot)) {
            return;
        }
        try {
            try (var cachePaths = Files.walk(cacheRoot)) {
                cachePaths
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                AmiCore.LOGGER.warn("AMI: Failed to clear entity icon cache directory: {}", ioException.getMessage());
            } else {
                throw e;
            }
        } catch (IOException e) {
            AmiCore.LOGGER.warn("AMI: Failed to clear entity icon cache directory: {}", e.getMessage());
        }
    }

    private static Atlas atlas(int size) {
        String fingerprint = activeFingerprint();
        Atlas existing = atlases.get(size);
        if (existing != null && existing.fingerprint().equals(fingerprint)) {
            return existing;
        }
        if (existing != null) {
            Minecraft.getInstance().getTextureManager().release(existing.textureKey());
            existing.close();
        }
        Atlas loaded = Atlas.load(size, fingerprint, persistentStore(fingerprint));
        atlases.put(size, loaded);
        return loaded;
    }

    private static String activeFingerprint() {
        if (activeFingerprint == null) {
            activeFingerprint = EntityIconCacheKey.currentFingerprint();
        }
        return activeFingerprint;
    }

    private static NativeImage bakeImage(int size, Consumer<GuiGraphics> renderFunc) {
        Minecraft mc = Minecraft.getInstance();
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());

        RenderTarget rt = new RenderTarget(true) {
        };
        try {
            rt.resize(size, size, Minecraft.ON_OSX);
            rt.setClearColor(0f, 0f, 0f, 0f);
            rt.clear(Minecraft.ON_OSX);
            rt.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            com.mojang.blaze3d.platform.GlStateManager._viewport(0, 0, size, size);
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0, size, size, 0, -100, 3000),
                    VertexSorting.ORTHOGRAPHIC_Z);

            GuiGraphics cacheG = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            renderFunc.accept(cacheG);
            cacheG.flush();

            return Screenshot.takeScreenshot(rt);
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(savedProj, VertexSorting.ORTHOGRAPHIC_Z);
            state.restore();
            rt.destroyBuffers();
        }
    }

    private static boolean isBlankOrBlack(NativeImage image) {
        boolean sawVisible = false;
        boolean sawNonBlack = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int alpha = (pixel >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }
                sawVisible = true;
                int red = pixel & 0xFF;
                int green = (pixel >>> 8) & 0xFF;
                int blue = (pixel >>> 16) & 0xFF;
                if (red > 8 || green > 8 || blue > 8) {
                    sawNonBlack = true;
                }
            }
        }
        return !sawVisible || !sawNonBlack;
    }

    private static void makeEdgeBackgroundTransparent(NativeImage image) {
        if (image == null) {
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] visited = new boolean[width * height];
        int[] queue = new int[width * height];
        int head = 0;
        int tail = 0;

        for (int x = 0; x < width; x++) {
            tail = enqueueBackground(image, visited, queue, tail, x, 0);
            tail = enqueueBackground(image, visited, queue, tail, x, height - 1);
        }
        for (int y = 1; y < height - 1; y++) {
            tail = enqueueBackground(image, visited, queue, tail, 0, y);
            tail = enqueueBackground(image, visited, queue, tail, width - 1, y);
        }

        while (head < tail) {
            int packed = queue[head++];
            int x = packed & 0xFFFF;
            int y = packed >>> 16;
            image.setPixelRGBA(x, y, 0);
            if (x > 0) {
                tail = enqueueBackground(image, visited, queue, tail, x - 1, y);
            }
            if (x + 1 < width) {
                tail = enqueueBackground(image, visited, queue, tail, x + 1, y);
            }
            if (y > 0) {
                tail = enqueueBackground(image, visited, queue, tail, x, y - 1);
            }
            if (y + 1 < height) {
                tail = enqueueBackground(image, visited, queue, tail, x, y + 1);
            }
        }
    }

    private static int enqueueBackground(NativeImage image, boolean[] visited, int[] queue, int tail, int x, int y) {
        int width = image.getWidth();
        int index = y * width + x;
        if (visited[index]) {
            return tail;
        }
        visited[index] = true;
        if (!isTransparentBackgroundPixel(image.getPixelRGBA(x, y))) {
            return tail;
        }
        queue[tail++] = (y << 16) | x;
        return tail;
    }

    private static boolean isTransparentBackgroundPixel(int pixel) {
        int alpha = (pixel >>> 24) & 0xFF;
        if (alpha <= 8) {
            return true;
        }
        int red = pixel & 0xFF;
        int green = (pixel >>> 8) & 0xFF;
        int blue = (pixel >>> 16) & 0xFF;
        return alpha >= 248 && red <= 3 && green <= 3 && blue <= 3;
    }

    private static Path cacheRootDir() {
        return Services.PLATFORM.getGameDir()
                .resolve("ami-icon-cache")
                .resolve("entity-atlas");
    }

    private static EntityIconPersistentStore persistentStore(String fingerprint) {
        return new EntityIconPersistentStore(cacheRootDir(), fingerprint, PERSIST_EXECUTOR);
    }

    private record CacheKey(ResourceLocation id, int size) {
    }

    private record BakeTask(CacheKey key, Consumer<GuiGraphics> renderToFramebuffer, boolean priority) {
    }

    public record Stats(
            int atlasCount,
            int pendingBakeCount,
            int failedKeyCount,
            long queuedBakeRequests,
            long droppedBakeRequests,
            long renderedBakeCount,
            long persistentLoadCount,
            long failedBakeCount,
            long pendingPersistentWrites,
            long droppedPersistentWrites,
            Map<Integer, AtlasStats> atlases
    ) {
    }

    public record AtlasStats(int size, int entryCount, int maxSlots) {
    }

    public enum BakeRequestResult {
        CACHED,
        QUEUED,
        QUEUE_FULL,
        FAILED
    }

    private static final class Atlas implements AutoCloseable {
        private final int size;
        private final String fingerprint;
        private final EntityIconPersistentStore persistentStore;
        private final ResourceLocation textureKey;
        private final EntityIconAtlasAllocator allocator;
        private NativeImage image;
        private DynamicTexture texture;

        private boolean dirty = false;

        private Atlas(int size, String fingerprint, EntityIconPersistentStore persistentStore, NativeImage image) {
            this.size = size;
            this.fingerprint = fingerprint;
            this.persistentStore = persistentStore;
            this.image = image;
            this.allocator = new EntityIconAtlasAllocator(ATLAS_SIZE, size);
            this.textureKey = Services.PLATFORM.rl("ami",
                    "entity_icon_atlas/" + fingerprint + "/" + size);
        }

        static Atlas load(int size, String fingerprint, EntityIconPersistentStore persistentStore) {
            NativeImage image = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, true);
            Atlas atlas = new Atlas(size, fingerprint, persistentStore, image);
            atlas.registerTexture();
            return atlas;
        }

        String fingerprint() {
            return fingerprint;
        }

        ResourceLocation textureKey() {
            return textureKey;
        }

        EntityIconAtlasAllocator.AtlasEntry entry(ResourceLocation id) {
            return allocator.entry(id);
        }

        AtlasStats stats() {
            return new AtlasStats(size, allocator.entryCount(), allocator.maxSlots());
        }

        EntityIconPersistentStore.StoreStats storeStats() {
            return persistentStore.stats();
        }

        boolean loadPersistent(ResourceLocation id) {
            if (allocator.entry(id) != null) {
                return true;
            }
            NativeImage cached = persistentStore.loadIcon(id, size);
            if (cached == null) {
                return false;
            }
            try {
                return store(id, cached, false) != null;
            } finally {
                cached.close();
            }
        }

        EntityIconAtlasAllocator.AtlasEntry store(ResourceLocation id, NativeImage source) {
            return store(id, source, true);
        }

        private EntityIconAtlasAllocator.AtlasEntry store(ResourceLocation id, NativeImage source, boolean persist) {
            if (source.getWidth() != size || source.getHeight() != size) {
                return null;
            }
            EntityIconAtlasAllocator.AtlasEntry entry = allocator.allocate(id);
            if (entry == null) {
                return null;
            }

            copyIconToAtlas(source, entry.x(), entry.y());
            dirty = true;
            if (persist) {
                persistentStore.enqueueWrite(id, size, source);
            }
            return entry;
        }

        void flushUploads() {
            if (dirty) {
                upload();
                dirty = false;
            }
        }

        private void registerTexture() {
            Minecraft.getInstance().getTextureManager().release(textureKey);
            texture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(textureKey, texture);
        }

        private void upload() {
            if (texture != null) {
                texture.upload();
            }
        }


        private void copyIconToAtlas(NativeImage icon, int atlasX, int atlasY) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    image.setPixelRGBA(atlasX + x, atlasY + y, icon.getPixelRGBA(x, y));
                }
            }
        }

        @Override
        public void close() {
            if (image != null) {
                image.close();
                image = null;
            }
            texture = null;
        }

    }
}
