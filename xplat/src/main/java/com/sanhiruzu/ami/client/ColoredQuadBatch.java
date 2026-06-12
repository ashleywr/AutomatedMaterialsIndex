package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Batches flat GUI rectangles into one POSITION_COLOR draw. This avoids many
 * tiny GuiGraphics.fill calls on dense AMI result grids.
 */
public final class ColoredQuadBatch {
    private final List<Rect> rects = new ArrayList<>();
    private int count;
    private static Method tesselatorBeginMethod;
    private static Method tesselatorGetBuilderMethod;
    private static Method legacyBufferBeginMethod;
    private static Method addVertexMethod;
    private static Method setColorMethod;
    private static Method legacyVertexMethod;
    private static Method legacyColorMethod;
    private static Method legacyEndVertexMethod;
    private static Method buildOrThrowMethod;
    private static Method legacyEndMethod;
    private static Method drawWithShaderMethod;

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
            Object buffer = beginBuffer();
            for (int i = 0; i < count; i++) {
                Rect rect = rects.get(i);
                float a = ((rect.color >>> 24) & 0xFF) / 255.0f;
                float r = ((rect.color >>> 16) & 0xFF) / 255.0f;
                float g = ((rect.color >>> 8) & 0xFF) / 255.0f;
                float b = (rect.color & 0xFF) / 255.0f;
                addVertex(buffer, matrix, rect.x1, rect.y2, r, g, b, a);
                addVertex(buffer, matrix, rect.x2, rect.y2, r, g, b, a);
                addVertex(buffer, matrix, rect.x2, rect.y1, r, g, b, a);
                addVertex(buffer, matrix, rect.x1, rect.y1, r, g, b, a);
            }
            drawWithShader(buildBuffer(buffer));
        } finally {
            state.restore();
            clear();
        }
    }

    private static Object beginBuffer() {
        try {
            Tesselator tesselator = Tesselator.getInstance();
            Method begin = tesselatorBeginMethod;
            if (begin == null) {
                begin = Tesselator.class.getMethod("begin", VertexFormat.Mode.class, com.mojang.blaze3d.vertex.VertexFormat.class);
                tesselatorBeginMethod = begin;
            }
            return begin.invoke(tesselator, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        } catch (ReflectiveOperationException newerApiUnavailable) {
            try {
                Tesselator tesselator = Tesselator.getInstance();
                Method getBuilder = tesselatorGetBuilderMethod;
                if (getBuilder == null) {
                    getBuilder = Tesselator.class.getMethod("getBuilder");
                    tesselatorGetBuilderMethod = getBuilder;
                }
                Object buffer = getBuilder.invoke(tesselator);
                Method begin = legacyBufferBeginMethod;
                if (begin == null) {
                    begin = buffer.getClass().getMethod("begin", VertexFormat.Mode.class, com.mojang.blaze3d.vertex.VertexFormat.class);
                    legacyBufferBeginMethod = begin;
                }
                begin.invoke(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                return buffer;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to begin AMI colored quad batch", e);
            }
        }
    }

    private static void addVertex(Object buffer, Matrix4f matrix, int x, int y, float r, float g, float b, float a) {
        try {
            Method addVertex = addVertexMethod;
            if (addVertex == null) {
                addVertex = buffer.getClass().getMethod("addVertex", Matrix4f.class, float.class, float.class, float.class);
                addVertexMethod = addVertex;
            }
            Object vertex = addVertex.invoke(buffer, matrix, (float) x, (float) y, 0.0f);
            Method setColor = setColorMethod;
            if (setColor == null) {
                setColor = vertex.getClass().getMethod("setColor", float.class, float.class, float.class, float.class);
                setColorMethod = setColor;
            }
            setColor.invoke(vertex, r, g, b, a);
            return;
        } catch (ReflectiveOperationException newerApiUnavailable) {
            try {
                Method vertex = legacyVertexMethod;
                if (vertex == null) {
                    vertex = buffer.getClass().getMethod("vertex", Matrix4f.class, float.class, float.class, float.class);
                    legacyVertexMethod = vertex;
                }
                Object vertexConsumer = vertex.invoke(buffer, matrix, (float) x, (float) y, 0.0f);
                Method color = legacyColorMethod;
                if (color == null) {
                    color = vertexConsumer.getClass().getMethod("color", float.class, float.class, float.class, float.class);
                    legacyColorMethod = color;
                }
                Object colored = color.invoke(vertexConsumer, r, g, b, a);
                Method endVertex = legacyEndVertexMethod;
                if (endVertex == null) {
                    endVertex = colored.getClass().getMethod("endVertex");
                    legacyEndVertexMethod = endVertex;
                }
                endVertex.invoke(colored);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to append AMI colored quad vertex", e);
            }
        }
    }

    private static Object buildBuffer(Object buffer) {
        try {
            Method buildOrThrow = buildOrThrowMethod;
            if (buildOrThrow == null) {
                buildOrThrow = buffer.getClass().getMethod("buildOrThrow");
                buildOrThrowMethod = buildOrThrow;
            }
            return buildOrThrow.invoke(buffer);
        } catch (ReflectiveOperationException newerApiUnavailable) {
            try {
                Method end = legacyEndMethod;
                if (end == null) {
                    end = buffer.getClass().getMethod("end");
                    legacyEndMethod = end;
                }
                return end.invoke(buffer);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to build AMI colored quad batch", e);
            }
        }
    }

    private static void drawWithShader(Object meshData) {
        try {
            Method draw = drawWithShaderMethod;
            if (draw == null) {
                draw = com.mojang.blaze3d.vertex.BufferUploader.class.getMethod("drawWithShader", meshData.getClass());
                drawWithShaderMethod = draw;
            }
            draw.invoke(null, meshData);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to draw AMI colored quad batch", e);
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
