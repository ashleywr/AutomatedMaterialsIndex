package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Batches flat GUI rectangles into one POSITION_COLOR draw. This avoids many
 * tiny GuiGraphics.fill calls on dense AMI result grids.
 *
 * <p>The vertex-buffer plumbing (begin/vertex/build/draw) lives behind
 * {@link Services#PLATFORM} because the API differs between MC versions and, on Fabric,
 * must be called directly (not via reflection by name) so Loom remaps it to intermediary.
 */
public final class ColoredQuadBatch {
    private final List<Rect> rects = new ArrayList<>();
    private int count;

    public void clear() {
        count = 0;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1 || (color >>> 24) == 0) return;
        Rect rect;
        if (count < rects.size()) {
            rect = rects.get(count);
        } else {
            rect = new Rect();
            rects.add(rect);
        }
        count++;
        rect.set(x1, y1, x2, y2, color);
    }

    public void border(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        fill(x, y, x + width, y + 1, color);
        fill(x, y + height - 1, x + width, y + height, color);
        fill(x, y + 1, x + 1, y + height - 1, color);
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public void flush(GuiGraphics graphics) {
        if (count == 0) return;

        graphics.flush();
        RenderStateSnapshot state = RenderStateSnapshot.capture();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            Matrix4f matrix = graphics.pose().last().pose();
            Object buffer = Services.PLATFORM.beginGuiQuadBatch(false);
            for (int i = 0; i < count; i++) {
                Rect rect = rects.get(i);
                float a = ((rect.color >>> 24) & 0xFF) / 255.0f;
                float r = ((rect.color >>> 16) & 0xFF) / 255.0f;
                float g = ((rect.color >>> 8) & 0xFF) / 255.0f;
                float b = (rect.color & 0xFF) / 255.0f;
                Services.PLATFORM.guiQuadVertex(buffer, matrix, rect.x1, rect.y2, 0f, 0f, r, g, b, a, false);
                Services.PLATFORM.guiQuadVertex(buffer, matrix, rect.x2, rect.y2, 0f, 0f, r, g, b, a, false);
                Services.PLATFORM.guiQuadVertex(buffer, matrix, rect.x2, rect.y1, 0f, 0f, r, g, b, a, false);
                Services.PLATFORM.guiQuadVertex(buffer, matrix, rect.x1, rect.y1, 0f, 0f, r, g, b, a, false);
            }
            Services.PLATFORM.endAndDrawGuiQuadBatch(buffer);
        } finally {
            state.restore();
            clear();
        }
    }

    private static final class Rect {
        private int x1;
        private int y1;
        private int x2;
        private int y2;
        private int color;

        private void set(int x1, int y1, int x2, int y2, int color) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
        }
    }
}
