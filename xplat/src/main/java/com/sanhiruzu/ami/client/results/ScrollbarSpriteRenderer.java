package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.sanhiruzu.ami.config.AmiConfig;

final class ScrollbarSpriteRenderer {
    private static final int WIDTH = 6;
    private static final int HEIGHT = 16;
    private static final GeneratedGuiSprite TRACK = new GeneratedGuiSprite(
            ResourceLocation.fromNamespaceAndPath("ami", "generated/scrollbar_track"),
            WIDTH,
            HEIGHT,
            ScrollbarSpriteRenderer::trackSignature,
            ScrollbarSpriteRenderer::paintTrack
    );
    private static final GeneratedGuiSprite THUMB = thumb("thumb", false);
    private static final GeneratedGuiSprite THUMB_ACTIVE = thumb("thumb_active", true);

    private ScrollbarSpriteRenderer() {
    }

    static void renderTrack(GuiGraphics g, int x, int y, int width, int height) {
        TRACK.blit(g, x, y, width, height);
    }

    static void renderThumb(GuiGraphics g, int x, int y, int width, int height, boolean active) {
        (active ? THUMB_ACTIVE : THUMB).blit(g, x, y, width, height);
    }

    private static GeneratedGuiSprite thumb(String name, boolean active) {
        return new GeneratedGuiSprite(
                ResourceLocation.fromNamespaceAndPath("ami", "generated/scrollbar_" + name),
                WIDTH,
                HEIGHT,
                () -> thumbSignature(active),
                canvas -> paintThumb(canvas, active)
        );
    }

    private static int trackSignature() {
        int result = AMITheme.SCROLL_TRACK;
        result = 31 * result + AMITheme.CONTROL_EDGE_DARK;
        result = 31 * result + AMITheme.CONTROL_EDGE_DARK;
        return result;
    }

    private static int thumbSignature(boolean active) {
        int result = active ? AMITheme.SCROLL_THUMB_ACTIVE : AMITheme.SCROLL_THUMB;
        result = 31 * result + AMITheme.CONTROL_EDGE_DARK;
        result = 31 * result + AMITheme.CONTROL_EDGE_DARK;
        return result;
    }

    private static void paintTrack(GeneratedGuiSprite.Canvas canvas) {
        canvas.fill(0, 0, WIDTH, HEIGHT, AMITheme.SCROLL_TRACK);
        canvas.fill(0, 0, 1, HEIGHT, AMITheme.CONTROL_EDGE_DARK);
        canvas.fill(WIDTH - 1, 0, WIDTH, HEIGHT, AMITheme.CONTROL_EDGE_DARK);
    }

    private static void paintThumb(GeneratedGuiSprite.Canvas canvas, boolean active) {
        int fill = modernThemeThumbColor(active);
        canvas.fill(1, 0, WIDTH - 1, HEIGHT, fill);
        canvas.fill(0, 1, 1, HEIGHT - 1, AMITheme.CONTROL_EDGE_DARK);
        canvas.fill(WIDTH - 1, 1, WIDTH, HEIGHT - 1, AMITheme.CONTROL_EDGE_DARK);
        canvas.fill(1, 0, WIDTH - 1, 1, AMITheme.CONTROL_EDGE_DARK);
        canvas.fill(1, HEIGHT - 1, WIDTH - 1, HEIGHT, AMITheme.CONTROL_EDGE_DARK);
    }

    private static int modernThemeThumbColor(boolean active) {
        int color = active ? AMITheme.SCROLL_THUMB_ACTIVE : AMITheme.SCROLL_THUMB;
        if (AmiConfig.theme == AmiConfig.Theme.MODERN) {
            color = darken(color, 0x70);
            int alpha = active ? 0x55 : 0x33;
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
        return color;
    }

    private static int darken(int argb, int amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        r = (r * amount) >> 8;
        g = (g * amount) >> 8;
        b = (b * amount) >> 8;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
