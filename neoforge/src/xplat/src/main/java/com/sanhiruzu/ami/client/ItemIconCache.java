package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
    // itemId → RenderTarget (kept alive so the GL texture remains valid)
    private static final Map<ResourceLocation, RenderTarget> renderTargets = new HashMap<>();

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

        for (var e : visible) {
            if (!textureKeys.containsKey(e.getKey())) {
                populateEntry(e.getKey(), e.getValue());
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
        g.blit(texKey, x, y, 0.0f, 0.0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    /**
     * Release all GL resources. Call on world unload and resource-pack reload.
     */
    public static void invalidate() {
        Minecraft mc = Minecraft.getInstance();
        textureKeys.values().forEach(mc.getTextureManager()::release);
        // release() calls FramebufferTexture.close() which destroys the framebuffer.
        renderTargets.clear();
        textureKeys.clear();
    }

    // ---------------------------------------------------------------

    private static void populateEntry(ResourceLocation itemId, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();

        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());

        // Off-screen framebuffer for this icon.
        RenderTarget rt = new RenderTarget(true) {
        };
        rt.resize(ICON_SIZE, ICON_SIZE, Minecraft.ON_OSX);
        rt.setClearColor(0f, 0f, 0f, 0f);
        rt.clear(Minecraft.ON_OSX);
        rt.bindWrite(true);
        GlStateManager._viewport(0, 0, ICON_SIZE, ICON_SIZE);
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(0, ICON_SIZE, ICON_SIZE, 0, -100, 3000),
                VertexSorting.ORTHOGRAPHIC_Z);

        // Render item into the cache framebuffer.
        GuiGraphics cacheG = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        cacheG.renderItem(stack, 0, 0);
        cacheG.flush();

        // Restore main framebuffer, viewport, and projection.
        mc.getMainRenderTarget().bindWrite(true);
        GlStateManager._viewport(0, 0, window.getWidth(), window.getHeight());
        RenderSystem.setProjectionMatrix(savedProj, VertexSorting.ORTHOGRAPHIC_Z);

        // Register the framebuffer's colour texture with TextureManager so blit() can use it.
        ResourceLocation texKey = ResourceLocation.fromNamespaceAndPath("ami", "icon/" + itemId.getNamespace() + "/" + itemId.getPath().replace('/', '_'));
        mc.getTextureManager().register(texKey, new FramebufferTexture(rt));
        renderTargets.put(itemId, rt);
        textureKeys.put(itemId, texKey);
    }

    // ---------------------------------------------------------------

    private static final class FramebufferTexture extends AbstractTexture {
        private final RenderTarget target;

        FramebufferTexture(RenderTarget target) {
            this.target = target;
            this.id = target.getColorTextureId();
        }

        @Override
        public void load(ResourceManager rm) {
        }

        @Override
        public void close() {
            target.destroyBuffers(); // frees the GL framebuffer and colour texture
            this.id = -1;           // prevent AbstractTexture from double-freeing
        }
    }
}
