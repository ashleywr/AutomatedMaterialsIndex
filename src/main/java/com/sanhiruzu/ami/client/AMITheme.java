package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Central palette for all AMI UI colours.
 * All rendering code reads from here so the entire look can be changed in one place.
 */
public final class AMITheme {
    private AMITheme() {}

    // Layout (CSS-like constants)
    public static int GLOBAL_PADDING = 6;
    public static int ROW_HEIGHT     = 18;
    public static int ICON_SIZE      = 16;
    public static int ELEMENT_GAP    = 4;

    // Panel chrome — vanilla MC light grey palette
    public static int PANEL_BG       = 0xFFC6C6C6; // Vanilla inventory grey
    public static int PANEL_INNER    = 0xFFD0D0D0; // Slightly lighter inner surface

    // Header bar
    public static int HEADER_BG      = 0xFFBBBBBB;
    public static int HEADER_SEP     = 0xFF999999;
    public static int HEADER_TEXT    = 0xFF111111;

    // Navigation arrows in header
    public static int ARROW_NORMAL   = 0xFF555555;
    public static int ARROW_HOVER    = 0xFF000000;

    // Namespace group headers in atlas lists
    public static int GROUP_BG       = 0xFFBBBBBB;
    public static int GROUP_BG_HOVER = 0xFFAAAAAA;
    public static int GROUP_TEXT     = 0xFF222222;

    public static int GROUP_HEADER_BG   = 0xFFB0B0B0; // slightly darker than panel to separate from leaf rows
    public static int GROUP_HEADER_TEXT = 0xFF333333;

    // Atlas entry rows
    public static int ENTRY_HOVER    = 0x3300AAFF; // vanilla-style blue tint @ ~20%
    public static int ENTRY_TEXT     = 0xFF111111; // near-black on light background
    public static int ENTRY_SUBTITLE = 0xFF555555;

    // Scrollbar
    public static int SCROLL_TRACK        = 0xFFAAAAAA;
    public static int SCROLL_THUMB        = 0xFF777777;
    public static int SCROLL_THUMB_ACTIVE = 0xFF555555; // darker when hovered or dragging

    // Dimension badges
    public static int DIM_NETHER     = 0xFFCC4444;
    public static int DIM_END        = 0xFF9944CC;

    // Typography (light-panel palette — do not use ChatFormatting enums for these)
    public static final int TEXT_HEADER    = 0xFF404040; // static UI labels, no drop shadow
    public static final int TEXT_PRIMARY   = 0xFFFFFFFF; // primary item names, with drop shadow
    public static final int TEXT_SUBTLE    = 0xFF555555; // metadata, counts, badges, no drop shadow
    public static final int TEXT_HIGHLIGHT = 0xFF55FFFF; // active query match (e.g. @modid), with drop shadow

    // Row and section separators (not configurable — aesthetic constants)
    public static final int ROW_SEPARATOR = 0xFF999999; // 1px line between list rows
    public static final int SECTION_SEP   = 0xFF888888; // 1px line between FacetBar/Toolbar/Results

    // Item grid
    public static int SLOT_BG        = 0xFF555555;
    public static int SLOT_HOVER     = 0xFFAAAAAA;

    // Dropdown buttons and panels
    public static final int DROPDOWN_BG        = 0xFFAAAAAA; // button idle + list item hover
    public static final int DROPDOWN_BG_ACTIVE = 0xFF989898; // button when open or hovered
    public static final int DROPDOWN_LIST_BG   = 0xFFBBBBBB; // open list panel background

    // Current location indicators
    public static int CURRENT_BIOME_BG       = 0xFF1A2E1A; // subtle green tint
    public static int CURRENT_BIOME_ACCENT   = 0xFF44DD44; // bright green left-edge bar
    public static int CURRENT_STRUCT_BG      = 0xFF2E2A14; // subtle amber tint
    public static int CURRENT_STRUCT_ACCENT  = 0xFFDD9933; // amber left-edge bar

    // Cheat mode
    public static int CHEAT_HEADER_BG   = 0xFF3A2800; // dark amber replaces normal header bg
    public static int CHEAT_HEADER_SEP  = 0xFF7A5200;
    public static int CHEAT_INDICATOR   = 0xFFFFAA00; // gold indicator text
    public static int CHEAT_ENTRY_HOVER = 0xFF5A4A00; // amber entry highlight

    // ── Rendering helpers ─────────────────────────────────────────────────────

    /**
     * Fills a rectangle with 2px-radius rounded corners (3 fill calls, corner pixels omitted).
     * Falls back to a plain fill when the rectangle is too small to round.
     */
    public static void fillRounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w < 5 || h < 5) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + 2, y,     x + w - 2, y + h,     color); // centre strip — cuts top/bottom corners
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color); // wider middle band
        g.fill(x,     y + 2, x + w,     y + h - 2, color); // full width — cuts side corners
    }

    /**
     * Synchronizes theme fields with AmiConfig values.
     * Called during mod initialization and config reload events.
     */
    public static void sync() {
        GLOBAL_PADDING = AmiConfig.globalPadding;
        ROW_HEIGHT     = AmiConfig.rowHeight;
        ICON_SIZE      = AmiConfig.iconSize;
        ELEMENT_GAP    = AmiConfig.elementGap;
    }

    public static void load() {
        // PANEL_BG       = AmiConfig.overlayBg; // These are not explicitly in the new config yet but we could add them
        // PANEL_INNER    = AmiConfig.searchBarBg;
        // ENTRY_HOVER    = AmiConfig.cardBgHover;
        // ENTRY_TEXT     = AmiConfig.cardTextName;
        // ENTRY_SUBTITLE = AmiConfig.cardTextSubtitle;

        // For now, mapping the ones we have in AmiConfig
    }
}
