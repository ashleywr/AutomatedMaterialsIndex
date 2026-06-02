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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Renders each entity into a per-size framebuffer once, copies it into a dynamic texture,
 * then blits the cached texture on subsequent frames — replacing the full 3D render pipeline
 * with a single cheap blit.
 * <p>
 * The caller supplies a render callback that draws into a GuiGraphics backed by an off-screen
 * framebuffer sized exactly (size × size). Coordinates inside the callback should be 0-based
 * (i.e. entity feet at (size/2, size-1), not panel-relative).
 */
public class EntityIconCache {

    private static final Map<ResourceLocation, Map<Integer, ResourceLocation>> textureKeys = new HashMap<>();

    private EntityIconCache() {
    }

    /**
     * Blits the cached icon at (x, y). Populates the cache on first call for this (id, size)
     * pair by invoking renderToFramebuffer with a 0-based GuiGraphics context.
     */
    public static boolean blitCached(GuiGraphics g, ResourceLocation id, int size, int x, int y,
                                     Consumer<GuiGraphics> renderToFramebuffer) {
        Map<Integer, ResourceLocation> bySize = textureKeys.get(id);
        ResourceLocation texKey = bySize == null ? null : bySize.get(size);
        if (texKey == null) {
            g.flush();
            texKey = populate(id, size, renderToFramebuffer);
            if (texKey == null) {
                return false;
            }
            textureKeys.computeIfAbsent(id, ignored -> new HashMap<>()).put(size, texKey);
        }
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(texKey, x, y, 0f, 0f, size, size, size, size);
        } finally {
            state.restore();
        }
        return true;
    }

    /**
     * Release all GL resources. Call on world unload and resource-pack reload.
     */
    public static void invalidate() {
        Minecraft mc = Minecraft.getInstance();
        for (Map<Integer, ResourceLocation> bySize : textureKeys.values()) {
            bySize.values().forEach(mc.getTextureManager()::release);
        }
        textureKeys.clear();
    }

    private static ResourceLocation populate(ResourceLocation id, int size, Consumer<GuiGraphics> renderFunc) {
        Minecraft mc = Minecraft.getInstance();
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());

        RenderTarget rt = new RenderTarget(true) {
        };
        NativeImage image = null;
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

            image = Screenshot.takeScreenshot(rt);
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(savedProj, VertexSorting.ORTHOGRAPHIC_Z);
            state.restore();
            rt.destroyBuffers();
        }

        if (image == null || isBlankOrBlack(image)) {
            if (image != null) {
                image.close();
            }
            return null;
        }

        ResourceLocation texKey = Services.PLATFORM.rl("ami",
                "entity_icon/" + id.getNamespace() + "/" + id.getPath().replace('/', '_') + "_" + size);
        mc.getTextureManager().register(texKey, new DynamicTexture(image));
        return texKey;
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

}
