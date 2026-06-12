package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

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
        result = 31 * result + AMITheme.CONTROL_EDGE_LIGHT;
        return result;
    }

    private static int thumbSignature(boolean active) {
        int result = active ? AMITheme.SCROLL_THUMB_ACTIVE : AMITheme.SCROLL_THUMB;
        result = 31 * result + AMITheme.CONTROL_EDGE_LIGHT;
        result = 31 * result + AMITheme.CONTROL_EDGE_DARK;
        result = 31 * result + AMITheme.ACCENT_BLUE;
        return result;
    }

    private static void paintTrack(GeneratedGuiSprite.Canvas canvas) {
        canvas.fill(0, 0, WIDTH, HEIGHT, AMITheme.SCROLL_TRACK);
        canvas.fill(0, 0, 1, HEIGHT, AMITheme.CONTROL_EDGE_DARK);
        canvas.fill(WIDTH - 1, 0, WIDTH, HEIGHT, AMITheme.CONTROL_EDGE_LIGHT);
    }

    private static void paintThumb(GeneratedGuiSprite.Canvas canvas, boolean active) {
        int fill = active ? AMITheme.SCROLL_THUMB_ACTIVE : AMITheme.SCROLL_THUMB;
        canvas.fill(1, 0, WIDTH - 1, HEIGHT, fill);
        canvas.fill(0, 1, 1, HEIGHT - 1, fill);
        canvas.fill(WIDTH - 1, 1, WIDTH, HEIGHT - 1, fill);
        canvas.fill(1, 0, WIDTH - 1, 1, AMITheme.CONTROL_EDGE_LIGHT);
        canvas.fill(1, HEIGHT - 1, WIDTH - 1, HEIGHT, AMITheme.CONTROL_EDGE_DARK);
        canvas.fill(0, 1, 1, HEIGHT - 1, AMITheme.CONTROL_EDGE_LIGHT);
        canvas.fill(WIDTH - 1, 1, WIDTH, HEIGHT - 1, AMITheme.CONTROL_EDGE_DARK);
        if (active && (AMITheme.ACCENT_BLUE >>> 24) != 0) {
            canvas.fill(2, 2, WIDTH - 2, 3, AMITheme.ACCENT_BLUE);
        }
    }
}
