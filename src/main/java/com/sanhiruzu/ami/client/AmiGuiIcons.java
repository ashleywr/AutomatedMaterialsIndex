package com.sanhiruzu.ami.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Pixel-art icon helpers for AMI GUI buttons.
 * All methods draw into a fixed-size area centred on (cx, cy).
 * Using direct fill calls keeps rendering free of font-hinting artefacts
 * and independent of the active font/locale.
 */
public final class AmiGuiIcons {

    private AmiGuiIcons() {
    }

    // ── Icons ─────────────────────────────────────────────────────────────────

    /**
     * 3×3 compact grid — represents the dense grid layout.
     * Shown on the toggle button when panel is in full mode (click → go compact).
     * Draws a 8×8 area centred on (cx, cy).
     */
    public static void compact(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 4, sy = cy - 4;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                g.fill(sx + col * 3, sy + row * 3,
                        sx + col * 3 + 2, sy + row * 3 + 2, color);
    }

    /**
     * 3-row list icon — represents the full list/tree layout.
     * Shown on the toggle button when panel is in compact mode (click → expand).
     * Each row: a 2×2 item dot + a 5px text bar.
     * Draws a 9×8 area centred on (cx, cy).
     */
    public static void expand(GuiGraphics g, int cx, int cy, int color) {
        // Faint bar colour: same hue, ~40% opacity
        int a = (int) (((color >>> 24) & 0xFF) * 0.40f);
        int barColor = (a << 24) | (color & 0x00FFFFFF);

        int sx = cx - 4, sy = cy - 4;
        for (int row = 0; row < 3; row++) {
            int ry = sy + row * 3;
            g.fill(sx, ry, sx + 2, ry + 2, color);    // icon dot  (2×2)
            g.fill(sx + 3, ry, sx + 8, ry + 1, barColor); // title bar (5×1, faint)
        }
    }

    /**
     * Circular refresh arrow (↺ shape) — represents reset/clear.
     * Draws a 10×10 area centred on (cx, cy).
     */
    public static void reset(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 5, sy = cy - 5;

        // Outer ring (8×8 stroked circle, 1px wide)
        g.fill(sx + 2, sy, sx + 8, sy + 1, color); // top
        g.fill(sx + 8, sy + 1, sx + 9, sy + 8, color); // right
        g.fill(sx + 1, sy + 8, sx + 8, sy + 9, color); // bottom
        g.fill(sx, sy + 2, sx + 1, sy + 8, color); // left
        // Corner rounding
        g.fill(sx + 1, sy + 1, sx + 2, sy + 2, color); // TL
        g.fill(sx + 7, sy + 1, sx + 8, sy + 2, color); // TR
        g.fill(sx + 1, sy + 7, sx + 2, sy + 8, color); // BL
        g.fill(sx + 7, sy + 7, sx + 8, sy + 8, color); // BR

        // Arrowhead at top-left — points counter-clockwise (upward + left)
        // Horizontal shaft extending left from the ring opening
        g.fill(sx - 1, sy + 1, sx + 2, sy + 2, color);
        // Tip: a short vertical stroke above the shaft
        g.fill(sx, sy, sx + 1, sy + 2, color);
    }
}
