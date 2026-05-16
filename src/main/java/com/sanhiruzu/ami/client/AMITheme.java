package com.sanhiruzu.ami.client;

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

    // Panel chrome — neutral MC-standard dark grey
    public static int PANEL_BG       = 0xF0100010; // Near-black with slight depth (like MC tooltips)
    public static int PANEL_INNER    = 0xFF1A1A1A; // Dark grey content area

    // Header bar (mode label row at the top of every panel)
    public static int HEADER_BG      = 0xFF252525; // Slightly lighter than inner
    public static int HEADER_SEP     = 0xFF3A3A3A; // Subtle 1px separator
    public static int HEADER_TEXT    = 0xFFE0E0E0; // MC standard text color

    // Navigation arrows in header
    public static int ARROW_NORMAL   = 0xFF888888; // Grey when not hovered
    public static int ARROW_HOVER    = 0xFFFFFFFF; // White on hover

    // Namespace group headers in atlas lists
    public static int GROUP_BG       = 0xFF2A2A2A;
    public static int GROUP_BG_HOVER = 0xFF363636;
    public static int GROUP_TEXT     = 0xFFBBBBBB;

    public static int GROUP_HEADER_BG   = 0xFF1E1E2A; // slightly blue-tinted to read apart from leaf rows
    public static int GROUP_HEADER_TEXT = 0xFFAAAA00;

    // Atlas entry rows
    public static int ENTRY_HOVER    = 0x4DFFB7C5; // Cherry Blossom Pink @ 30%
    public static int ENTRY_TEXT     = 0xFFDDDDDD; // Standard MC text grey
    public static int ENTRY_SUBTITLE = 0xFF888888;

    // Scrollbar
    public static int SCROLL_TRACK        = 0xFF1A1A1A;
    public static int SCROLL_THUMB        = 0xFF555555;
    public static int SCROLL_THUMB_ACTIVE = 0xFF777777; // hovered or dragging

    // Dimension badges
    public static int DIM_NETHER     = 0xFFCC4444;
    public static int DIM_END        = 0xFF9944CC;

    // Row and section separators (not configurable — aesthetic constants)
    public static final int ROW_SEPARATOR = 0xFF1F1F26; // 1px line between list rows
    public static final int SECTION_SEP   = 0xFF282838; // 1px line between FacetBar/Toolbar/Results

    // Item grid
    public static int SLOT_BG        = 0xFF555555;
    public static int SLOT_HOVER     = 0xFFAAAAAA;

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
     * Synchronizes theme fields with AMIConfig values.
     * Called during mod initialization and config reload events.
     */
    public static void sync() {
        GLOBAL_PADDING = com.sanhiruzu.ami.AMIConfig.GLOBAL_PADDING.get();
        ROW_HEIGHT     = com.sanhiruzu.ami.AMIConfig.ROW_HEIGHT.get();
        ICON_SIZE      = com.sanhiruzu.ami.AMIConfig.ICON_SIZE.get();
        ELEMENT_GAP    = com.sanhiruzu.ami.AMIConfig.ELEMENT_GAP.get();

        PANEL_INNER    = com.sanhiruzu.ami.AMIConfig.PALETTE_SEARCH_BAR_BG.get();
        ENTRY_HOVER    = com.sanhiruzu.ami.AMIConfig.PALETTE_CARD_BG_HOVER.get();
        ENTRY_TEXT     = com.sanhiruzu.ami.AMIConfig.PALETTE_CARD_TEXT_NAME.get();
        ENTRY_SUBTITLE = com.sanhiruzu.ami.AMIConfig.PALETTE_CARD_TEXT_SUBTITLE.get();

        SCROLL_TRACK   = com.sanhiruzu.ami.AMIConfig.PALETTE_SCROLLBAR_BG.get();
        SCROLL_THUMB   = com.sanhiruzu.ami.AMIConfig.PALETTE_SCROLLBAR_THUMB.get();
        SCROLL_THUMB_ACTIVE = com.sanhiruzu.ami.AMIConfig.PALETTE_SCROLLBAR_THUMB_HOVER.get();
        
        GROUP_HEADER_BG = com.sanhiruzu.ami.AMIConfig.PALETTE_GROUP_HEADER_BG.get();
        GROUP_HEADER_TEXT = com.sanhiruzu.ami.AMIConfig.PALETTE_GROUP_HEADER_TEXT.get();
    }
}
