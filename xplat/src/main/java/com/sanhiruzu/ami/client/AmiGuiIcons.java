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
     * Split grid/list with a tiny arrow — represents switching result views.
     * Draws a 10×8 area centred on (cx, cy).
     */
    public static void viewSwitch(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 5, sy = cy - 4;

        // Grid half
        g.fill(sx, sy, sx + 2, sy + 2, color);
        g.fill(sx + 3, sy, sx + 5, sy + 2, color);
        g.fill(sx, sy + 3, sx + 2, sy + 5, color);
        g.fill(sx + 3, sy + 3, sx + 5, sy + 5, color);

        // List half
        g.fill(sx + 7, sy, sx + 9, sy + 2, color);
        g.fill(sx + 7, sy + 3, sx + 9, sy + 5, color);
        g.fill(sx + 10, sy, sx + 13, sy + 1, color);
        g.fill(sx + 10, sy + 3, sx + 13, sy + 4, color);

        // Switch arrow
        g.fill(sx + 5, sy + 6, sx + 10, sy + 7, color);
        g.fill(sx + 9, sy + 5, sx + 11, sy + 8, color);
    }

    /**
     * Two opposing arrows — represents swapping a sidebar panel to its alternate content.
     * Draws a 10x8 area centred on (cx, cy).
     */
    public static void swap(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 5;
        int sy = cy - 4;

        // Top arrow, left to right.
        g.fill(sx, sy + 1, sx + 8, sy + 2, color);
        g.fill(sx + 7, sy, sx + 9, sy + 3, color);
        g.fill(sx + 9, sy + 1, sx + 10, sy + 2, color);

        // Bottom arrow, right to left.
        g.fill(sx + 2, sy + 6, sx + 10, sy + 7, color);
        g.fill(sx + 1, sy + 5, sx + 3, sy + 8, color);
        g.fill(sx, sy + 6, sx + 1, sy + 7, color);
    }

    public static void dropdownChevron(GuiGraphics g, int cx, int cy, int color, boolean open) {
        int sx = cx - 2;
        int sy = cy - 2;
        if (open) {
            g.fill(sx + 2, sy, sx + 3, sy + 1, color);
            g.fill(sx + 1, sy + 1, sx + 4, sy + 2, color);
            g.fill(sx, sy + 2, sx + 5, sy + 3, color);
        } else {
            g.fill(sx, sy, sx + 5, sy + 1, color);
            g.fill(sx + 1, sy + 1, sx + 4, sy + 2, color);
            g.fill(sx + 2, sy + 2, sx + 3, sy + 3, color);
        }
    }

    public static void collapseAll(GuiGraphics g, int cx, int cy, int color) {
        g.fill(cx - 4, cy - 1, cx + 4, cy + 1, color);
    }

    public static void expandAll(GuiGraphics g, int cx, int cy, int color) {
        g.fill(cx - 4, cy - 1, cx + 4, cy + 1, color);
        g.fill(cx - 1, cy - 4, cx + 1, cy + 4, color);
    }

    public static void sortDirection(GuiGraphics g, int cx, int cy, int color, boolean ascending) {
        int sx = cx - 4;
        int sy = cy - 5;

        g.fill(sx + 5, sy + 1, sx + 8, sy + 2, color);
        g.fill(sx + 5, sy + 4, sx + 8, sy + 5, color);
        g.fill(sx + 5, sy + 7, sx + 8, sy + 8, color);

        if (ascending) {
            g.fill(sx + 1, sy + 1, sx + 2, sy + 8, color);
            g.fill(sx, sy + 2, sx + 3, sy + 3, color);
            g.fill(sx - 1, sy + 3, sx + 4, sy + 4, color);
        } else {
            g.fill(sx + 1, sy + 1, sx + 2, sy + 8, color);
            g.fill(sx, sy + 6, sx + 3, sy + 7, color);
            g.fill(sx - 1, sy + 5, sx + 4, sy + 6, color);
        }
    }

    /**
     * Cog/Gear icon - for General settings.
     */
    public static void general(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 4, sy = cy - 4;
        g.fill(sx + 3, sy, sx + 5, sy + 8, color); // vertical
        g.fill(sx, sy + 3, sx + 8, sy + 5, color); // horizontal
        g.fill(sx + 1, sy + 1, sx + 7, sy + 7, color); // diagonal TL-BR
        g.fill(sx + 1, sy + 6, sx + 7, sy + 1, color); // diagonal BL-TR
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFF000000); // center hole
    }

    /**
     * Screen icon - for Display settings.
     */
    public static void display(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 5, sy = cy - 4;
        g.fill(sx, sy, sx + 10, sy + 7, color); // monitor
        g.fill(sx + 1, sy + 1, sx + 9, sy + 6, 0x00000000); // screen (transparent)
        g.fill(cx - 2, cy + 3, cx + 2, cy + 4, color); // stand neck
        g.fill(cx - 3, cy + 4, cx + 3, cy + 5, color); // stand base
    }

    /**
     * Mouse cursor icon - for Interaction settings.
     */
    public static void interaction(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 3, sy = cy - 4;
        g.fill(sx, sy, sx + 1, sy + 8, color); // vertical stem
        g.fill(sx, sy, sx + 6, sy + 1, color); // top horizontal
        g.fill(sx + 1, sy + 1, sx + 2, sy + 2, color);
        g.fill(sx + 2, sy + 2, sx + 3, sy + 3, color);
        g.fill(sx + 3, sy + 3, sx + 4, sy + 4, color);
        g.fill(sx + 4, sy + 4, sx + 5, sy + 5, color);
    }

    /**
     * Ruler/Grid icon - for Layout settings.
     */
    public static void layout(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 4, sy = cy - 4;
        g.fill(sx, sy, sx + 8, sy + 1, color); // top
        g.fill(sx, sy + 1, sy + 8, sy + 1, color); // left
        g.fill(sx + 2, sy, sx + 3, sy + 3, color); // tick 1
        g.fill(sx + 5, sy, sx + 6, sy + 3, color); // tick 2
        g.fill(sx, sy + 5, sx + 3, sy + 6, color); // tick 3
    }

    /**
     * Paint palette icon - for Palette settings.
     */
    public static void palette(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 4, sy = cy - 4;
        g.fill(sx + 1, sy, sx + 7, sy + 8, color); // body
        g.fill(sx, sy + 2, sx + 8, sy + 6, color); // sides
        g.fill(sx + 2, sy + 2, sx + 3, sy + 3, 0xFF000000); // hole 1
        g.fill(sx + 5, sy + 2, sx + 6, sy + 3, 0xFF000000); // hole 2
        g.fill(sx + 2, sy + 5, sx + 3, sy + 6, 0xFF000000); // hole 3
    }

    /**
     * Split view icon - for Side Panels settings.
     */
    public static void sidepanels(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 5, sy = cy - 4;
        g.fill(sx, sy, sx + 3, sy + 8, color); // left panel
        g.fill(sx + 7, sy, sx + 10, sy + 8, color); // right panel
        g.fill(sx + 4, sy + 3, sx + 6, sy + 5, color); // middle dot
    }

    /**
     * Keyboard icon - for Keybinds.
     */
    public static void keybinds(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 5, sy = cy - 3;
        g.fill(sx, sy, sx + 10, sy + 6, color); // keyboard body
        g.fill(sx + 1, sy + 1, sx + 9, sy + 5, 0x00000000); // clear inner
        g.fill(sx + 2, sy + 2, sx + 3, sy + 3, color); // key 1
        g.fill(sx + 4, sy + 2, sx + 5, sy + 3, color); // key 2
        g.fill(sx + 6, sy + 2, sx + 7, sy + 3, color); // key 3
        g.fill(sx + 3, sy + 4, sx + 7, sy + 5, color); // spacebar
    }

    /**
     * Warning/Skull icon - for Cheat settings.
     */
    public static void cheat(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 4, sy = cy - 4;
        g.fill(sx + 1, sy, sx + 7, sy + 6, color); // head
        g.fill(sx, sy + 2, sx + 8, sy + 5, color); // ears area
        g.fill(sx + 2, sy + 2, sx + 3, sy + 3, 0xFF000000); // eye L
        g.fill(sx + 5, sy + 2, sx + 6, sy + 3, 0xFF000000); // eye R
        g.fill(sx + 2, sy + 6, sx + 6, sy + 8, color); // jaw
    }

    /**
     * Text bubble icon - for Subtitles settings.
     */
    public static void subtitles(GuiGraphics g, int cx, int cy, int color) {
        int sx = cx - 5, sy = cy - 4;
        g.fill(sx, sy, sx + 10, sy + 7, color); // bubble
        g.fill(sx + 2, sy + 2, sx + 8, sy + 3, 0xFF000000); // line 1
        g.fill(sx + 2, sy + 4, sx + 6, sy + 5, 0xFF000000); // line 2
        g.fill(sx + 1, sy + 7, sx + 3, sy + 9, color); // tail
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
