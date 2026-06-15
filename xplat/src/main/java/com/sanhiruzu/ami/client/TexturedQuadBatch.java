package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Batches same-texture GUI sprites into one POSITION_TEX_COLOR draw.
 *
 * <p>The vertex-buffer plumbing (begin/vertex/build/draw) lives behind
 * {@link Services#PLATFORM} because the API differs between MC versions and, on Fabric,
 * must be called directly (not via reflection by name) so Loom remaps it to intermediary.
 */
public final class TexturedQuadBatch {
    private final List<Quad> quads = new ArrayList<>();
    private int count;
    private ResourceLocation texture;

    public void setTexture(ResourceLocation texture) {
        if (this.texture != null && !this.texture.equals(texture) && count > 0) {
            throw new IllegalStateException("Cannot change AMI texture batch texture before flush");
        }
        this.texture = texture;
    }

    public void clear() {
        count = 0;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void add(int x, int y, int width, int height,
                    float u0, float v0, float u1, float v1, int color) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        Quad quad;
        if (count < quads.size()) {
            quad = quads.get(count);
        } else {
            quad = new Quad();
            quads.add(quad);
        }
        count++;
        quad.set(x, y, x + width, y + height, u0, v0, u1, v1, color);
    }

    public void flush(GuiGraphics graphics) {
        if (count == 0 || texture == null) return;

        graphics.flush();
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, texture);

            Matrix4f matrix = graphics.pose().last().pose();
            Object buffer = Services.PLATFORM.beginGuiQuadBatch(true);
            for (int i = 0; i < count; i++) {
                Quad quad = quads.get(i);
                float a = ((quad.color >>> 24) & 0xFF) / 255.0f;
                float r = ((quad.color >>> 16) & 0xFF) / 255.0f;
                float g = ((quad.color >>> 8) & 0xFF) / 255.0f;
                float b = (quad.color & 0xFF) / 255.0f;
                Services.PLATFORM.guiQuadVertex(buffer, matrix, quad.x1, quad.y2, quad.u0, quad.v1, r, g, b, a, true);
                Services.PLATFORM.guiQuadVertex(buffer, matrix, quad.x2, quad.y2, quad.u1, quad.v1, r, g, b, a, true);
                Services.PLATFORM.guiQuadVertex(buffer, matrix, quad.x2, quad.y1, quad.u1, quad.v0, r, g, b, a, true);
                Services.PLATFORM.guiQuadVertex(buffer, matrix, quad.x1, quad.y1, quad.u0, quad.v0, r, g, b, a, true);
            }
            Services.PLATFORM.endAndDrawGuiQuadBatch(buffer);
        } finally {
            state.restore();
            clear();
        }
    }

    private static final class Quad {
        private int x1;
        private int y1;
        private int x2;
        private int y2;
        private float u0;
        private float v0;
        private float u1;
        private float v1;
        private int color;

        private void set(int x1, int y1, int x2, int y2, float u0, float v0, float u1, float v1, int color) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.color = color;
        }
    }
}
