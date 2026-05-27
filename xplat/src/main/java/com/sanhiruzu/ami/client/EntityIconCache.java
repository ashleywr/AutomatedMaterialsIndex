package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Renders each entity into a per-size framebuffer once, then blits the cached texture on
 * subsequent frames — replacing the full 3D render pipeline (with its GPU flush per entity)
 * with a single cheap blit.
 *
 * The caller supplies a render callback that draws into a GuiGraphics backed by an off-screen
 * framebuffer sized exactly (size × size). Coordinates inside the callback should be 0-based
 * (i.e. entity feet at (size/2, size-1), not panel-relative).
 */
public class EntityIconCache {

    private static final Map<String, ResourceLocation> textureKeys = new HashMap<>();
    private static final Map<String, RenderTarget> renderTargets = new HashMap<>();

    private EntityIconCache() {}

    /**
     * Blits the cached icon at (x, y). Populates the cache on first call for this (id, size)
     * pair by invoking renderToFramebuffer with a 0-based GuiGraphics context.
     */
    public static void blitCached(GuiGraphics g, ResourceLocation id, int size, int x, int y,
                                   Consumer<GuiGraphics> renderToFramebuffer) {
        String key = cacheKey(id, size);
        if (!textureKeys.containsKey(key)) {
            g.flush();
            populate(key, id, size, renderToFramebuffer);
        }
        ResourceLocation texKey = textureKeys.get(key);
        if (texKey != null) {
            g.blit(texKey, x, y, 0f, 0f, size, size, size, size);
        }
    }

    /** Release all GL resources. Call on world unload and resource-pack reload. */
    public static void invalidate() {
        Minecraft mc = Minecraft.getInstance();
        textureKeys.values().forEach(mc.getTextureManager()::release);
        renderTargets.clear();
        textureKeys.clear();
    }

    private static void populate(String key, ResourceLocation id, int size,
                                  Consumer<GuiGraphics> renderFunc) {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();

        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());

        RenderTarget rt = new RenderTarget(true) {};
        rt.resize(size, size, Minecraft.ON_OSX);
        rt.setClearColor(0f, 0f, 0f, 0f);
        rt.clear(Minecraft.ON_OSX);
        rt.bindWrite(true);
        GlStateManager._viewport(0, 0, size, size);
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(0, size, size, 0, -100, 3000),
                VertexSorting.ORTHOGRAPHIC_Z);

        GuiGraphics cacheG = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        renderFunc.accept(cacheG);
        cacheG.flush();

        mc.getMainRenderTarget().bindWrite(true);
        GlStateManager._viewport(0, 0, window.getWidth(), window.getHeight());
        RenderSystem.setProjectionMatrix(savedProj, VertexSorting.ORTHOGRAPHIC_Z);

        ResourceLocation texKey = Services.PLATFORM.rl("ami",
                "entity_icon/" + id.getNamespace() + "/" + id.getPath().replace('/', '_') + "_" + size);
        mc.getTextureManager().register(texKey, new FramebufferTexture(rt));
        renderTargets.put(key, rt);
        textureKeys.put(key, texKey);
    }

    private static String cacheKey(ResourceLocation id, int size) {
        return id + "@" + size;
    }

    private static final class FramebufferTexture extends AbstractTexture {
        private final RenderTarget target;

        FramebufferTexture(RenderTarget target) {
            this.target = target;
            this.id = target.getColorTextureId();
        }

        @Override
        public void load(ResourceManager rm) {}

        @Override
        public void close() {
            target.destroyBuffers();
            this.id = -1;
        }
    }
}
