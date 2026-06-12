package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

/**
 * Bakes static entity thumbnails into persistent per-size atlas PNGs, then blits
 * atlas regions on normal grid frames. The expensive entity renderer only runs
 * on a first bake for an entity/size pair or when the cache key changes.
 */
public class EntityIconCache {

    private static final int ATLAS_SIZE = 2048;
    private static final String CACHE_VERSION = "entity-atlas-v1";
    private static final Map<Integer, Atlas> atlases = new HashMap<>();
    private static final Set<CacheKey> failedKeys = new HashSet<>();
    private static final Map<CacheKey, BakeTask> pendingBakes = new LinkedHashMap<>();

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
        AtlasEntry entry = atlas.entry(id);
        if (entry == null) {
            queueBake(cacheKey, renderToFramebuffer);
            return false;
        }

        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(atlas.textureKey(), x, y, entry.x(), entry.y(), size, size, ATLAS_SIZE, ATLAS_SIZE);
        } finally {
            state.restore();
        }
        return true;
    }

    /**
     * Requests a persistent atlas bake without drawing anything to the current
     * screen. Actual baking happens later through {@link #processPendingBakes}.
     */
    public static boolean warmCached(ResourceLocation id, int size, Consumer<GuiGraphics> renderToFramebuffer) {
        CacheKey cacheKey = new CacheKey(id, size);
        if (failedKeys.contains(cacheKey)) {
            return false;
        }

        Atlas atlas = atlas(size);
        if (atlas.entry(id) != null) {
            return true;
        }

        queueBake(cacheKey, renderToFramebuffer);
        return false;
    }

    /**
     * Runs a bounded amount of queued atlas baking on the client thread.
     */
    public static void processPendingBakes(int maxBakes) {
        int remaining = Math.max(0, maxBakes);
        while (remaining > 0 && !pendingBakes.isEmpty()) {
            BakeTask task = pendingBakes.values().iterator().next();
            pendingBakes.remove(task.key());
            bakeNow(task);
            remaining--;
        }
    }

    public static int pendingBakeCount() {
        return pendingBakes.size();
    }

    private static void queueBake(CacheKey cacheKey, Consumer<GuiGraphics> renderToFramebuffer) {
        if (failedKeys.contains(cacheKey) || pendingBakes.containsKey(cacheKey)) {
            return;
        }
        Atlas atlas = atlas(cacheKey.size());
        if (atlas.entry(cacheKey.id()) != null) {
            return;
        }
        pendingBakes.put(cacheKey, new BakeTask(cacheKey, renderToFramebuffer));
    }

    private static void bakeNow(BakeTask task) {
        CacheKey cacheKey = task.key();
        if (failedKeys.contains(cacheKey)) {
            return;
        }

        Atlas atlas = atlas(cacheKey.size());
        if (atlas.entry(cacheKey.id()) != null) {
            return;
        }

        NativeImage image = null;
        try {
            image = bakeImage(cacheKey.size(), task.renderToFramebuffer());
        } catch (RuntimeException e) {
            failedKeys.add(cacheKey);
            return;
        }
        makeEdgeBackgroundTransparent(image);
        if (image == null || isBlankOrBlack(image)) {
            if (image != null) {
                image.close();
            }
            failedKeys.add(cacheKey);
            return;
        }
        try {
            AtlasEntry entry = atlas.store(cacheKey.id(), image);
            if (entry == null) {
                failedKeys.add(cacheKey);
            }
        } finally {
            image.close();
        }
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
    }

    private static Atlas atlas(int size) {
        return atlases.computeIfAbsent(size, Atlas::load);
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

    private static Path cacheDir() {
        return Services.PLATFORM.getGameDir()
                .resolve("ami-icon-cache")
                .resolve("entity-atlas")
                .resolve(cacheFingerprint());
    }

    private static String cacheFingerprint() {
        List<String> parts = new ArrayList<>();
        parts.add(CACHE_VERSION);
        parts.add("language=" + Services.PLATFORM.getClientLanguageCode());
        for (String entry : Services.PLATFORM.getLoadedModFingerprintEntries()) {
            parts.add("mod=" + entry);
        }
        for (String pack : selectedResourcePacks()) {
            parts.add("pack=" + pack);
        }
        parts.sort(Comparator.naturalOrder());
        return sha256(String.join("\n", parts)).substring(0, 16);
    }

    private static List<String> selectedResourcePacks() {
        try {
            Object repository = Minecraft.getInstance().getResourcePackRepository();
            Method method = repository.getClass().getMethod("getSelectedIds");
            Object selected = method.invoke(repository);
            if (selected instanceof Collection<?> collection) {
                return collection.stream().map(String::valueOf).sorted().toList();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return List.of();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private record CacheKey(ResourceLocation id, int size) {
    }

    private record BakeTask(CacheKey key, Consumer<GuiGraphics> renderToFramebuffer) {
    }

    private record AtlasEntry(int x, int y) {
    }

    private static final class Atlas implements AutoCloseable {
        private final int size;
        private final Path directory;
        private final Path imageFile;
        private final Path metadataFile;
        private final ResourceLocation textureKey;
        private final Map<ResourceLocation, AtlasEntry> entries = new HashMap<>();
        private NativeImage image;
        private DynamicTexture texture;

        private Atlas(int size, Path directory, NativeImage image) {
            this.size = size;
            this.directory = directory;
            this.image = image;
            this.imageFile = directory.resolve("atlas.png");
            this.metadataFile = directory.resolve("atlas.tsv");
            this.textureKey = Services.PLATFORM.rl("ami",
                    "entity_icon_atlas/" + cacheFingerprint() + "/" + size);
        }

        static Atlas load(int size) {
            Path directory = cacheDir().resolve(Integer.toString(size));
            NativeImage image = readAtlasImage(directory.resolve("atlas.png"));
            if (image == null || image.getWidth() != ATLAS_SIZE || image.getHeight() != ATLAS_SIZE) {
                if (image != null) {
                    image.close();
                }
                image = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, true);
            }

            Atlas atlas = new Atlas(size, directory, image);
            atlas.readMetadata();
            atlas.registerTexture();
            return atlas;
        }

        ResourceLocation textureKey() {
            return textureKey;
        }

        AtlasEntry entry(ResourceLocation id) {
            return entries.get(id);
        }

        AtlasEntry store(ResourceLocation id, NativeImage source) {
            if (source.getWidth() != size || source.getHeight() != size) {
                return null;
            }
            int slot = entries.size();
            int columns = ATLAS_SIZE / size;
            if (columns <= 0 || slot >= columns * columns) {
                return null;
            }

            int atlasX = (slot % columns) * size;
            int atlasY = (slot / columns) * size;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    image.setPixelRGBA(atlasX + x, atlasY + y, source.getPixelRGBA(x, y));
                }
            }

            AtlasEntry entry = new AtlasEntry(atlasX, atlasY);
            entries.put(id, entry);
            upload();
            writeToDisk();
            return entry;
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

        private void readMetadata() {
            if (!Files.isRegularFile(metadataFile)) {
                return;
            }
            try {
                for (String line : Files.readAllLines(metadataFile, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    if (parts.length != 3) {
                        continue;
                    }
                    ResourceLocation id = Services.PLATFORM.rl(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    if (x >= 0 && y >= 0 && x + size <= ATLAS_SIZE && y + size <= ATLAS_SIZE) {
                        entries.put(id, new AtlasEntry(x, y));
                    }
                }
            } catch (IOException | NumberFormatException ignored) {
                entries.clear();
            }
        }

        private void writeToDisk() {
            try {
                Files.createDirectories(directory);
                image.writeToFile(imageFile);
                List<String> lines = new ArrayList<>(entries.size());
                for (Map.Entry<ResourceLocation, AtlasEntry> entry : entries.entrySet()) {
                    lines.add(entry.getKey() + "\t" + entry.getValue().x() + "\t" + entry.getValue().y());
                }
                lines.sort(Comparator.naturalOrder());
                Files.write(metadataFile, lines, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // Disk persistence is best-effort; the in-memory atlas remains valid.
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

        private static NativeImage readAtlasImage(Path file) {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            try (InputStream in = Files.newInputStream(file)) {
                return NativeImage.read(in);
            } catch (IOException ignored) {
                return null;
            }
        }
    }
}
