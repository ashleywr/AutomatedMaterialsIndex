package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
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
import org.lwjgl.opengl.GL11;

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

    private static final Map<String, ResourceLocation> textureKeys = new HashMap<>();

    private EntityIconCache() {
    }

    /**
     * Blits the cached icon at (x, y). Populates the cache on first call for this (id, size)
     * pair by invoking renderToFramebuffer with a 0-based GuiGraphics context.
     */
    public static boolean blitCached(GuiGraphics g, ResourceLocation id, int size, int x, int y,
                                     Consumer<GuiGraphics> renderToFramebuffer) {
        String key = cacheKey(id, size);
        if (!textureKeys.containsKey(key)) {
            g.flush();
            if (!populate(key, id, size, renderToFramebuffer)) {
                return false;
            }
        }
        ResourceLocation texKey = textureKeys.get(key);
        if (texKey != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(texKey, x, y, 0f, 0f, size, size, size, size);
            RenderSystem.disableBlend();
            return true;
        }
        return false;
    }

    /**
     * Release all GL resources. Call on world unload and resource-pack reload.
     */
    public static void invalidate() {
        Minecraft mc = Minecraft.getInstance();
        textureKeys.values().forEach(mc.getTextureManager()::release);
        textureKeys.clear();
    }

    private static boolean populate(String key, ResourceLocation id, int size,
                                    Consumer<GuiGraphics> renderFunc) {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();

        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] scissorBox = new int[4];
        if (scissorEnabled) {
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        }
        float[] shaderColor = RenderSystem.getShaderColor();
        float savedRed = shaderColor[0];
        float savedGreen = shaderColor[1];
        float savedBlue = shaderColor[2];
        float savedAlpha = shaderColor[3];

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
            GlStateManager._viewport(0, 0, size, size);
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0, size, size, 0, -100, 3000),
                    VertexSorting.ORTHOGRAPHIC_Z);

            GuiGraphics cacheG = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            renderFunc.accept(cacheG);
            cacheG.flush();

            image = Screenshot.takeScreenshot(rt);
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
            GlStateManager._viewport(0, 0, window.getWidth(), window.getHeight());
            RenderSystem.setProjectionMatrix(savedProj, VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.setShaderColor(savedRed, savedGreen, savedBlue, savedAlpha);
            if (scissorEnabled) {
                RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            } else {
                RenderSystem.disableScissor();
            }
            rt.destroyBuffers();
        }

        if (image == null || isBlankOrBlack(image)) {
            if (image != null) {
                image.close();
            }
            return false;
        }

        ResourceLocation texKey = Services.PLATFORM.rl("ami",
                "entity_icon/" + id.getNamespace() + "/" + id.getPath().replace('/', '_') + "_" + size);
        mc.getTextureManager().register(texKey, new DynamicTexture(image));
        textureKeys.put(key, texKey);
        return true;
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

    private static String cacheKey(ResourceLocation id, int size) {
        return id + "@" + size;
    }
}
