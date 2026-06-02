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
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders each unique item into a 16×16 framebuffer once, then blits the cached texture on
 * subsequent frames — replacing the full 3D model pipeline (per item, per frame) with a
 * single cheap texture blit.
 * <p>
 * Usage pattern in a render method:
 * 1. Call primeVisible() once before the render loop, passing the GuiGraphics and a list of
 * all item IDs that will be visible. This flushes pending batches once and populates all
 * uncached entries in one shot.
 * 2. Call blit() per item inside the loop. Falls back to direct renderItem() on cache miss
 * (should only occur on the very first frame a given item becomes visible).
 */
public class ItemIconCache {

    private static final int ICON_SIZE = 16;

    // itemId → registered ResourceLocation key in TextureManager
    private static final Map<ResourceLocation, ResourceLocation> textureKeys = new HashMap<>();

    private ItemIconCache() {
    }

    public static boolean isCached(ResourceLocation id) {
        return textureKeys.containsKey(id);
    }

    /**
     * Bulk-prime: flushes pending vertex batches once, then renders each uncached item to its
     * own framebuffer. Call before the render loop with the set of visible item IDs + stacks.
     */
    public static void primeVisible(GuiGraphics g, Iterable<Map.Entry<ResourceLocation, ItemStack>> visible) {
        primeVisible(g, visible, Integer.MAX_VALUE);
    }

    /**
     * Budgeted variant for scrollable grids. Populating cached icons requires framebuffer
     * switches, so spread misses over multiple frames instead of doing a whole viewport at once.
     */
    public static void primeVisible(GuiGraphics g, Iterable<Map.Entry<ResourceLocation, ItemStack>> visible, int maxNewEntries) {
        boolean anyUncached = false;
        for (var e : visible) {
            if (!textureKeys.containsKey(e.getKey())) {
                anyUncached = true;
                break;
            }
        }
        if (!anyUncached) return;

        // One flush before we start switching framebuffers.
        g.flush();

        int populated = 0;
        for (var e : visible) {
            if (!textureKeys.containsKey(e.getKey())) {
                populateEntry(e.getKey(), e.getValue());
                populated++;
                if (populated >= maxNewEntries) {
                    return;
                }
            }
        }
    }

    /**
     * Blits the cached 16×16 icon at (x, y). No-op on cache miss — caller should fall back to
     * g.renderItem() and call primeVisible() earlier in the frame.
     */
    public static void blit(GuiGraphics g, ResourceLocation itemId, int x, int y) {
        ResourceLocation texKey = textureKeys.get(itemId);
        if (texKey == null) return;
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(texKey, x, y, 0.0f, 0.0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } finally {
            state.restore();
        }
    }

    /**
     * Release all GL resources. Call on world unload and resource-pack reload.
     */
    public static void invalidate() {
        Minecraft mc = Minecraft.getInstance();
        textureKeys.values().forEach(mc.getTextureManager()::release);
        textureKeys.clear();
    }

    // ---------------------------------------------------------------

    private static void populateEntry(ResourceLocation itemId, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());

        RenderTarget rt = new RenderTarget(true) {
        };
        NativeImage image;
        try {
            rt.resize(ICON_SIZE, ICON_SIZE, Minecraft.ON_OSX);
            rt.setClearColor(0f, 0f, 0f, 0f);
            rt.clear(Minecraft.ON_OSX);
            rt.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            com.mojang.blaze3d.platform.GlStateManager._viewport(0, 0, ICON_SIZE, ICON_SIZE);
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0, ICON_SIZE, ICON_SIZE, 0, -200, 3000),
                    VertexSorting.ORTHOGRAPHIC_Z);

            GuiGraphics cacheG = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            // Compensate for any model-view Z translation active during the GUI render frame.
            // In MC 1.20.x (Forge), GameRenderer.renderGui applies translate(0,0,-11000) to the
            // model-view stack, pushing items to view-space Z≈-10900 — outside the off-screen
            // frustum unless we neutralise it here.  In 1.21.x the offset is 0 so this is a no-op.
            float mvZ = RenderSystem.getModelViewMatrix().m32();
            if (mvZ != 0) {
                cacheG.pose().translate(0.0, 0.0, (double) -mvZ);
            }
            cacheG.renderItem(stack, 0, 0);
            cacheG.flush();
            image = Screenshot.takeScreenshot(rt);
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(savedProj, VertexSorting.ORTHOGRAPHIC_Z);
            state.restore();
            rt.destroyBuffers();
        }

        if (isBlankOrBlack(image)) {
            image.close();
            return;
        }

        ResourceLocation texKey = Services.PLATFORM.rl("ami", "icon/" + itemId.getNamespace() + "/" + itemId.getPath().replace('/', '_'));
        mc.getTextureManager().register(texKey, new DynamicTexture(image));
        textureKeys.put(itemId, texKey);
    }

    private static boolean isBlankOrBlack(NativeImage image) {
        boolean sawVisible = false;
        boolean sawNonBlack = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int alpha = (pixel >>> 24) & 0xFF;
                if (alpha == 0) continue;
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
}
