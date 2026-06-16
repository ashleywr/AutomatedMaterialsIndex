package com.sanhiruzu.ami.client.results;

import com.mojang.blaze3d.platform.NativeImage;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

final class AccessLevelOverlayRenderer {
    private static final int CROSS_SHADOW = 0xCC160000;
    private static final int CROSS_RED = 0xFFFF3030;
    private static final int CROSS_HIGHLIGHT = 0xFFFFD0D0;
    private static final int SPRITE_SIZE = 16;
    private static final Identifier SPRITE_TEXTURE =
            Identifier.fromNamespaceAndPath("ami", "generated/access_level_overlay");
    private static boolean spriteRegistered;

    private AccessLevelOverlayRenderer() {
    }

    static void renderIconOverlay(GuiGraphicsExtractor g, SearchNode node, int x, int y, int size) {
        if (!AccessLevelVisuals.hasDevOnlyMarker(node)) {
            return;
        }
        ensureSpriteRegistered();
        g.blit(RenderPipelines.GUI_TEXTURED, SPRITE_TEXTURE, x, y, 0.0f, 0.0f, size, size, SPRITE_SIZE, SPRITE_SIZE, SPRITE_SIZE, SPRITE_SIZE);
    }

    private static void appendIconOverlay(RectSink sink, int x, int y, int size) {
        int inset = Math.max(1, size / 8);
        int left = x + inset;
        int top = y + inset;
        int right = x + size - inset - 1;
        int bottom = y + size - inset - 1;
        drawThickDiagonal(sink, left, top, right, bottom, CROSS_SHADOW, 4);
        drawThickDiagonal(sink, left, bottom, right, top, CROSS_SHADOW, 4);
        drawThickDiagonal(sink, left, top, right, bottom, CROSS_RED, 3);
        drawThickDiagonal(sink, left, bottom, right, top, CROSS_RED, 3);
        drawDiagonal(sink, left + 1, top, right, bottom - 1, CROSS_HIGHLIGHT);
        drawDiagonal(sink, left + 1, bottom, right, top + 1, CROSS_HIGHLIGHT);
    }

    private static void drawThickDiagonal(RectSink sink, int x1, int y1, int x2, int y2,
                                          int color, int thickness) {
        int radius = Math.max(0, thickness / 2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (Math.abs(dx) + Math.abs(dy) <= radius + 1) {
                    drawDiagonal(sink, x1 + dx, y1 + dy, x2 + dx, y2 + dy, color);
                }
            }
        }
    }

    private static void drawDiagonal(RectSink sink, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) {
            sink.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int px = x1 + Math.round((x2 - x1) * (i / (float) steps));
            int py = y1 + Math.round((y2 - y1) * (i / (float) steps));
            sink.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static void ensureSpriteRegistered() {
        if (spriteRegistered) {
            return;
        }
        NativeImage image = new NativeImage(SPRITE_SIZE, SPRITE_SIZE, true);
        appendIconOverlay((x1, y1, x2, y2, color) -> {
            int rgba = argbToNativeRgba(color);
            for (int y = Math.max(0, y1); y < Math.min(SPRITE_SIZE, y2); y++) {
                for (int x = Math.max(0, x1); x < Math.min(SPRITE_SIZE, x2); x++) {
                    image.setPixelABGR(x, y, rgba);
                }
            }
        }, 0, 0, SPRITE_SIZE);
        Minecraft.getInstance().getTextureManager().register(SPRITE_TEXTURE, new DynamicTexture(() -> "ami:access_level_overlay", image));
        spriteRegistered = true;
    }

    private static int argbToNativeRgba(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private interface RectSink {
        void fill(int x1, int y1, int x2, int y2, int color);
    }
}
