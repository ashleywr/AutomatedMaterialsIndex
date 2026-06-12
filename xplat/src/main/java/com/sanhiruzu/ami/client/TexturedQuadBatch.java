package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Batches same-texture GUI sprites into one POSITION_TEX_COLOR draw.
 */
public final class TexturedQuadBatch {
    private final List<Quad> quads = new ArrayList<>();
    private int count;
    private ResourceLocation texture;
    private static Method tesselatorBeginMethod;
    private static Method tesselatorGetBuilderMethod;
    private static Method legacyBufferBeginMethod;
    private static Method addVertexMethod;
    private static Method setUvMethod;
    private static Method setColorMethod;
    private static Method legacyVertexMethod;
    private static Method legacyUvMethod;
    private static Method legacyColorMethod;
    private static Method legacyEndVertexMethod;
    private static Method buildOrThrowMethod;
    private static Method legacyEndMethod;
    private static Method drawWithShaderMethod;

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
            Object buffer = beginBuffer();
            for (int i = 0; i < count; i++) {
                Quad quad = quads.get(i);
                float a = ((quad.color >>> 24) & 0xFF) / 255.0f;
                float r = ((quad.color >>> 16) & 0xFF) / 255.0f;
                float g = ((quad.color >>> 8) & 0xFF) / 255.0f;
                float b = (quad.color & 0xFF) / 255.0f;
                addVertex(buffer, matrix, quad.x1, quad.y2, quad.u0, quad.v1, r, g, b, a);
                addVertex(buffer, matrix, quad.x2, quad.y2, quad.u1, quad.v1, r, g, b, a);
                addVertex(buffer, matrix, quad.x2, quad.y1, quad.u1, quad.v0, r, g, b, a);
                addVertex(buffer, matrix, quad.x1, quad.y1, quad.u0, quad.v0, r, g, b, a);
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
            return begin.invoke(tesselator, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
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
                begin.invoke(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                return buffer;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to begin AMI textured quad batch", e);
            }
        }
    }

    private static void addVertex(Object buffer, Matrix4f matrix, int x, int y, float u, float v,
                                  float r, float g, float b, float a) {
        try {
            Method addVertex = addVertexMethod;
            if (addVertex == null) {
                addVertex = buffer.getClass().getMethod("addVertex", Matrix4f.class, float.class, float.class, float.class);
                addVertexMethod = addVertex;
            }
            Object vertex = addVertex.invoke(buffer, matrix, (float) x, (float) y, 0.0f);
            Method setUv = setUvMethod;
            if (setUv == null) {
                setUv = vertex.getClass().getMethod("setUv", float.class, float.class);
                setUvMethod = setUv;
            }
            setUv.invoke(vertex, u, v);
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
                Method uv = legacyUvMethod;
                if (uv == null) {
                    uv = vertexConsumer.getClass().getMethod("uv", float.class, float.class);
                    legacyUvMethod = uv;
                }
                Object textured = uv.invoke(vertexConsumer, u, v);
                Method color = legacyColorMethod;
                if (color == null) {
                    color = textured.getClass().getMethod("color", float.class, float.class, float.class, float.class);
                    legacyColorMethod = color;
                }
                Object colored = color.invoke(textured, r, g, b, a);
                Method endVertex = legacyEndVertexMethod;
                if (endVertex == null) {
                    endVertex = colored.getClass().getMethod("endVertex");
                    legacyEndVertexMethod = endVertex;
                }
                endVertex.invoke(colored);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to append AMI textured quad vertex", e);
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
                throw new IllegalStateException("Unable to build AMI textured quad batch", e);
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
            throw new IllegalStateException("Unable to draw AMI textured quad batch", e);
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
