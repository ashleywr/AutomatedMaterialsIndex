package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

final class DiscoveryVisuals {
    private DiscoveryVisuals() {
    }

    static boolean isUndiscovered(SearchNode node) {
        return AmiConfig.enableDiscoveryChecklist
                && node != null
                && "undiscovered".equals(node.meta(SearchNodeKeys.DISCOVERY_STATE, ""));
    }

    static boolean hasDiscoveryState(SearchNode node) {
        return AmiConfig.enableDiscoveryChecklist
                && node != null
                && !node.meta(SearchNodeKeys.DISCOVERY_STATE, "").isBlank();
    }

    static boolean isDiscovered(SearchNode node) {
        return AmiConfig.enableDiscoveryChecklist
                && node != null
                && "discovered".equals(node.meta(SearchNodeKeys.DISCOVERY_STATE, ""));
    }

    static int primaryTextColor(SearchNode node, int fallback) {
        return isUndiscovered(node) ? 0xFF9A9A9A : fallback;
    }

    static int subtitleTextColor(SearchNode node, int fallback) {
        return isUndiscovered(node) ? 0xFF777777 : fallback;
    }

    static void renderIconMask(net.minecraft.client.gui.GuiGraphics g, int x, int y, int size) {
        g.fill(x, y, x + size, y + size, 0xE6181818);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x663C3C3C);
        for (int offset = -size; offset < size; offset += 5) {
            drawClippedDiagonal(g, x, y, size, offset, 0xFF777777);
        }
        drawFrame(g, x, y, size, 0xFF8A8A8A);
    }

    static void renderIconOverlay(net.minecraft.client.gui.GuiGraphics g, SearchNode node, int x, int y, int size) {
        if (isUndiscovered(node)) {
            renderIconMask(g, x, y, size);
        } else if (isDiscovered(node)) {
            renderCheckBadge(g, x, y, size);
        }
    }

    private static void renderCheckBadge(net.minecraft.client.gui.GuiGraphics g, int x, int y, int size) {
        int badgeSize = Math.max(7, size / 2);
        int bx = x + size - badgeSize;
        int by = y + size - badgeSize;
        g.fill(bx - 1, by - 1, x + size, y + size, 0xDD0B1F0B);
        g.fill(bx, by, x + size - 1, y + size - 1, 0xFF1F8F3A);
        g.fill(bx + 2, by + 4, bx + 3, by + 6, 0xFFFFFFFF);
        g.fill(bx + 3, by + 5, bx + 4, by + 7, 0xFFFFFFFF);
        g.fill(bx + 4, by + 4, bx + 5, by + 6, 0xFFFFFFFF);
        g.fill(bx + 5, by + 3, bx + 6, by + 5, 0xFFFFFFFF);
        g.fill(bx + 6, by + 2, bx + 7, by + 4, 0xFFFFFFFF);
    }

    private static void drawFrame(net.minecraft.client.gui.GuiGraphics g, int x, int y, int size, int color) {
        g.fill(x, y, x + size, y + 1, color);
        g.fill(x, y + size - 1, x + size, y + size, color);
        g.fill(x, y, x + 1, y + size, color);
        g.fill(x + size - 1, y, x + size, y + size, color);
    }

    private static void drawClippedDiagonal(net.minecraft.client.gui.GuiGraphics g, int x, int y, int size,
                                            int offset, int color) {
        for (int i = 0; i < size; i++) {
            int px = x + i;
            int py = y + i + offset;
            if (py >= y && py < y + size) {
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
    }
}
