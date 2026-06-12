package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

final class AccessLevelVisuals {
    private static final int CROSS_SHADOW = 0xCC160000;
    private static final int CROSS_RED = 0xFFFF3030;
    private static final int CROSS_HIGHLIGHT = 0xFFFFD0D0;

    private AccessLevelVisuals() {
    }

    static boolean hasDevOnlyMarker(SearchNode node) {
        return AmiConfig.devMode && hiddenFromNormalPlayers(node);
    }

    static boolean hiddenFromNormalPlayers(SearchNode node) {
        if (node == null) {
            return false;
        }
        String accessLevel = node.meta(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL);
        return !ItemFilter.ACCESS_SURVIVAL.equals(accessLevel)
                || "hidden".equals(node.meta(SearchNodeKeys.VISIBILITY, ""));
    }

    static void renderIconOverlay(net.minecraft.client.gui.GuiGraphics g, SearchNode node, int x, int y, int size) {
        if (!hasDevOnlyMarker(node)) {
            return;
        }
        int inset = Math.max(1, size / 8);
        int left = x + inset;
        int top = y + inset;
        int right = x + size - inset - 1;
        int bottom = y + size - inset - 1;
        drawThickDiagonal(g, left, top, right, bottom, CROSS_SHADOW, 4);
        drawThickDiagonal(g, left, bottom, right, top, CROSS_SHADOW, 4);
        drawThickDiagonal(g, left, top, right, bottom, CROSS_RED, 3);
        drawThickDiagonal(g, left, bottom, right, top, CROSS_RED, 3);
        drawDiagonal(g, left + 1, top, right, bottom - 1, CROSS_HIGHLIGHT);
        drawDiagonal(g, left + 1, bottom, right, top + 1, CROSS_HIGHLIGHT);
    }

    private static void drawThickDiagonal(net.minecraft.client.gui.GuiGraphics g, int x1, int y1, int x2, int y2,
                                          int color, int thickness) {
        int radius = Math.max(0, thickness / 2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (Math.abs(dx) + Math.abs(dy) <= radius + 1) {
                    drawDiagonal(g, x1 + dx, y1 + dy, x2 + dx, y2 + dy, color);
                }
            }
        }
    }

    private static void drawDiagonal(net.minecraft.client.gui.GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int px = x1 + Math.round((x2 - x1) * (i / (float) steps));
            int py = y1 + Math.round((y2 - y1) * (i / (float) steps));
            g.fill(px, py, px + 1, py + 1, color);
        }
    }
}
