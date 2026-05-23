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

    // Accents & Status
    public static int ACCENT_GOLD    = 0xFFAAAA00;
    public static int ACCENT_BLUE    = 0xFF4488FF;
    public static int POSITIVE       = 0xFF55FF55;
    public static int NEGATIVE       = 0xFFFF5555;
    
    // Borders & Outlines
    public static int BORDER_LIGHT   = 0x33FFFFFF;
    public static int BORDER_DARK    = 0x33000000;
    public static int WHITE          = 0xFFFFFFFF;
    public static int BLACK          = 0xFF000000;

    // Gradients & Shadows
    public static int GRADIENT_SHADOW = 0xCC000000;
    public static int TRANSPARENT     = 0x00000000;

    // Typography (light-panel palette — do not use ChatFormatting enums for these)
    public static int TEXT_HEADER    = 0xFF404040; // static UI labels, no drop shadow
    public static int TEXT_PRIMARY   = 0xFFFFFFFF; // primary item names, with drop shadow
    public static int TEXT_SUBTLE    = 0xFF555555; // metadata, counts, badges, no drop shadow
    public static int TEXT_HIGHLIGHT = 0xFF55FFFF; // active query match (e.g. @modid), with drop shadow

    // Row and section separators (not configurable — aesthetic constants)
    public static int ROW_SEPARATOR = 0xFF999999; // 1px line between list rows
    public static int SECTION_SEP   = 0xFF888888; // 1px line between Toolbar/Results

    // Item grid
    public static int SLOT_BG        = 0xFF555555;
    public static int SLOT_HOVER     = 0xFFAAAAAA;

    // Dropdown buttons and panels
    public static int DROPDOWN_BG        = 0xFFAAAAAA; // button idle + list item hover
    public static int DROPDOWN_BG_ACTIVE = 0xFF989898; // button when open or hovered
    public static int DROPDOWN_LIST_BG   = 0xFFBBBBBB; // open list panel background

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

    // Temperature (biome tooltips)
    public static int TEMP_COOL   = 0xFF44AACC;
    public static int TEMP_WARM   = 0xFFCCCC44;
    public static int TEMP_HOT    = 0xFFCC8844;

    // Fallback icon backgrounds per node type
    public static int FALLBACK_BG_ENTITY    = 0xFF1A2020;
    public static int FALLBACK_BG_PLAYER    = 0xFF1A1A30;
    public static int FALLBACK_BG_BIOME     = 0xFF1A2A1A;
    public static int FALLBACK_BG_STRUCTURE = 0xFF2A2A14;
    public static int FALLBACK_BG_DIMENSION = 0xFF201020;
    public static int FALLBACK_BG_DEFAULT   = 0xFF1E1E1E;
    public static int FALLBACK_BG_DEV       = 0xFFAA1100;

    // Dimension icon backgrounds (proxy block renderer)
    public static int DIM_ICON_BG_OVERWORLD = 0xFF122010;
    public static int DIM_ICON_BG_NETHER    = 0xFF2B1408;
    public static int DIM_ICON_BG_END       = 0xFF16101E;
    public static int DIM_ICON_BG_STRUCTURE = 0xFF252512;

    // Entity tooltip text colors
    public static int ENTITY_ID_COLOR       = 0xFF666666;
    public static int ENTITY_CATEGORY_COLOR = 0xFF888888;
    public static int ENTITY_TRAITS_COLOR   = 0xFF55FFFF;
    public static int ENTITY_DAMAGE_COLOR   = 0xFFFF5555;
    public static int ENTITY_PLAYER_TEXT    = 0xFF5555FF;

    // Item grid group highlights
    public static int GRID_NO_RESULTS_TEXT     = 0xFFCCCCCC;
    public static int GRID_GOLD_BORDER         = 0xFFAAAA00;
    public static int GRID_GOLD_TINT           = 0x44AAAA00;
    public static int GRID_HEADER_DARKEN       = 0x66000000;
    public static int GRID_HEADER_WHITE_DOT    = 0xFFFFFFFF;
    public static int GRID_ROW_TINT_EVEN       = 0x08FFFFFF;
    public static int GRID_ROW_TINT_ODD        = 0x15000000;

    // Search bar element colors
    public static int SEARCH_PLACEHOLDER       = 0xFF666666;
    public static int SEARCH_CLEAR_TEXT        = 0xFFAAAAAA;
    public static int SEARCH_CLEAR_TEXT_HOVER  = 0xFFFFFFFF;
    public static int SEARCH_CURSOR            = 0xFFCCCCCC;
    public static int SEARCH_SELECTION         = 0xFF0000FF;
    public static int SEARCH_DEFAULT_TEXT      = 0xFFCCCCCC;

    // Heart bar tooltip
    public static int HEART_OVERFLOW_COLOR = 0xFFCC3333;
    public static int HEART_LABEL_COLOR    = 0xFFAAAAAA;

    // Scroll indicator
    public static int SCROLL_INDICATOR_BG = 0xAA111111;

    // Config screen
    public static int CONFIG_BRAND_GOLD     = 0xFFFFAA00;
    public static int CONFIG_HEADER_GOLD    = 0xFFFFAA00;
    public static int CONFIG_TEXT_PRIMARY   = 0xFFFFFFFF;
    public static int CONFIG_TEXT_SECONDARY = 0xFFAAAAAA;
    public static int CONFIG_TEXT_MUTED     = 0xFF777777;
    public static int CONFIG_BOOL_TRUE      = 0x8800FF00;
    public static int CONFIG_BOOL_FALSE     = 0x88FF0000;
    public static int CONFIG_SWATCH_BORDER  = 0xFFFFFFFF;
    public static int CONFIG_PANEL_TITLE    = 0xFFFFAA00;

    // Registry utility colors
    public static int REGISTRY_CATEGORY_MONSTER  = 0xFFCC4444;
    public static int REGISTRY_CATEGORY_CREATURE = 0xFF44AA44;
    public static int REGISTRY_CATEGORY_AMBIENT  = 0xFFAAAA44;
    public static int REGISTRY_CATEGORY_AQUATIC  = 0xFF4488CC;
    public static int REGISTRY_CATEGORY_DEFAULT  = 0xFF888888;
    public static int REGISTRY_DIM_OVERWORLD     = 0xFF66BB6A;
    public static int REGISTRY_DIM_NETHER        = 0xFFCC4444;
    public static int REGISTRY_DIM_END           = 0xFF7A51A6;

    // Token colorizer
    public static int TOKEN_ENV   = 0xFF44BB44;
    public static int TOKEN_PROP  = 0xFFBBBB44;
    public static int TOKEN_ESSENTIAL = 0xFFBB44BB;
    public static int TOKEN_ESM   = 0xFFBB8844;
    public static int TOKEN_META  = 0xFF44CCCC;
    public static int TOKEN_PLAIN = 0xFFCCCCCC;

    // Sidebar toggle
    public static int SIDEBAR_TOGGLE_HOVER_HALO = 0x33FFFFFF;
    public static int SIDEBAR_TOGGLE_IDLE_HALO  = 0x11FFFFFF;
    public static int SIDEBAR_TOGGLE_BORDER     = 0x88FFFFFF;

    // AMI button state colors
    public static int BUTTON_ACTIVE = 0xFFFFDD44;
    public static int BUTTON_HOVER  = 0xFFFFFFA0;

    // Player name color
    public static int PLAYER_NAME_COLOR = 0xFF4488FF;

    // Recipe Viewer dynamic theme variables
    public static int RECIPE_BG_OVERLAY  = 0xFF101010;
    public static int RECIPE_PANEL       = 0xFF1A1A1F;
    public static int RECIPE_PANEL_INNER = 0xFF22222A;
    public static int RECIPE_BORDER      = 0xFF3A3A4A;
    public static int RECIPE_HEADER_LINE = 0xFF2E2E3A;
    public static int RECIPE_TAB_ACTIVE  = 0xFF4488FF;
    public static int RECIPE_TAB_HOVER   = 0xFF2E2E44;
    public static int RECIPE_TAB_IDLE    = 0xFF1E1E28;
    public static int RECIPE_TAB_TEXT_A  = 0xFFFFFFFF;
    public static int RECIPE_TAB_TEXT_I  = 0xFF8888AA;
    public static int RECIPE_SLOT_BORDER = 0xFF555566;
    public static int RECIPE_SLOT_BG     = 0xFF2A2A36;
    public static int RECIPE_ARROW       = 0xFF6688CC;
    public static int RECIPE_ARROW_ANIM  = 0xFF4466AA;
    public static int RECIPE_TEXT_TITLE  = 0xFFFFFFFF;
    public static int RECIPE_TEXT_ITEM   = 0xFFBBBBCC;
    public static int RECIPE_TEXT_CAT    = 0xFF8888AA;
    public static int RECIPE_TEXT_NAV    = 0xFF8888AA;
    public static int RECIPE_TEXT_FOOTER = 0xFF555566;
    public static int RECIPE_BTN_IDLE    = 0xFF226622;
    public static int RECIPE_BTN_HOVER   = 0xFF44AA44;
    public static int RECIPE_SHAPELESS   = 0xFF5555AA;

    // Color swatch map for item variants
    private static final java.util.Map<String, Integer> SWATCH_COLORS = new java.util.HashMap<>();
    static {
        SWATCH_COLORS.put("red",        0xFFCC3333);
        SWATCH_COLORS.put("orange",     0xFFDD7722);
        SWATCH_COLORS.put("yellow",     0xFFDDCC22);
        SWATCH_COLORS.put("lime",       0xFF44AA44);
        SWATCH_COLORS.put("green",      0xFF44AA44);
        SWATCH_COLORS.put("cyan",       0xFF22AACC);
        SWATCH_COLORS.put("blue",       0xFF3355DD);
        SWATCH_COLORS.put("light_blue", 0xFF3355DD);
        SWATCH_COLORS.put("purple",     0xFF9933CC);
        SWATCH_COLORS.put("magenta",    0xFF9933CC);
        SWATCH_COLORS.put("pink",       0xFFFFB7C5);
        SWATCH_COLORS.put("white",      0xFFEEEEEE);
        SWATCH_COLORS.put("light_gray", 0xFFAAAAAA);
        SWATCH_COLORS.put("silver",     0xFFAAAAAA);
        SWATCH_COLORS.put("gray",       0xFF666666);
        SWATCH_COLORS.put("black",      0xFF222222);
        SWATCH_COLORS.put("brown",      0xFF885533);
    }
    private static final int SWATCH_DEFAULT = 0xFF888888;

    public static int getSwatchColor(String colorName) {
        return SWATCH_COLORS.getOrDefault(colorName.toLowerCase(java.util.Locale.ROOT), SWATCH_DEFAULT);
    }

    // ── Rendering helpers ─────────────────────────────────────────────────────

    /**
     * Draws a 1px border around a 2px-radius rounded rectangle.
     */
    public static void drawRoundedBorder(GuiGraphics g, int x, int y, int w, int h, int backgroundColor, int borderColor) {
        fillRounded(g, x, y, w, h, borderColor);
        fillRounded(g, x + 1, y + 1, w - 2, h - 2, backgroundColor);
    }

    /**
     * Fills a rectangle with 2px-radius rounded corners (3 fill calls, corner pixels omitted).
     * Falls back to a plain fill when the rectangle is too small to round.
     */
    public static void fillRounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w < 5 || h < 5) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        // Non-overlapping cross shape for transparent fills
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color); // Core
        g.fill(x + 2, y,     x + w - 2, y + 1,     color); // Top edge
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color); // Bottom edge
        g.fill(x,     y + 2, x + 1,     y + h - 2, color); // Left edge
        g.fill(x + w - 1, y + 2, x + w,     y + h - 2, color); // Right edge
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

        PANEL_BG       = AmiConfig.panelBg;
        PANEL_INNER    = AmiConfig.searchBarBg;
        ENTRY_HOVER    = AmiConfig.cardBgHover;
        ENTRY_TEXT     = AmiConfig.cardTextName;
        ENTRY_SUBTITLE = AmiConfig.cardTextSubtitle;
        TEXT_PRIMARY   = AmiConfig.cardTextName;
        TEXT_SUBTLE    = AmiConfig.cardTextSubtitle;
        SLOT_BG        = AmiConfig.cardBg;
        SLOT_HOVER     = AmiConfig.cardBgHover;
        GROUP_HEADER_BG = AmiConfig.cardBg;

        if (AmiConfig.useTransparentTheme) {
            SECTION_SEP   = 0x33FFFFFF;
            ROW_SEPARATOR = 0x11FFFFFF;
            TEXT_HEADER   = 0xFFEEEEEE;
            
            DROPDOWN_BG        = 0x44FFFFFF;
            DROPDOWN_BG_ACTIVE = 0x66FFFFFF; 
            DROPDOWN_LIST_BG   = 0xCC000000;

            SCROLL_TRACK        = 0x11FFFFFF;
            SCROLL_THUMB        = 0x44FFFFFF;
            SCROLL_THUMB_ACTIVE = 0x88FFFFFF;
            
            BORDER_LIGHT = 0x22FFFFFF;
            GRADIENT_SHADOW = 0x88000000;

            // Translucent glass recipe screen
            RECIPE_BG_OVERLAY  = 0x66000000;
            RECIPE_PANEL       = 0xD8121216; // 85% opacity dark glass
            RECIPE_PANEL_INNER = 0x88202028;
            RECIPE_BORDER      = 0x33FFFFFF;
            RECIPE_HEADER_LINE = 0x22FFFFFF;
            RECIPE_TAB_ACTIVE  = 0xFF4488FF;
            RECIPE_TAB_HOVER   = 0x44FFFFFF;
            RECIPE_TAB_IDLE    = 0x15FFFFFF;
            RECIPE_TAB_TEXT_A  = 0xFFFFFFFF;
            RECIPE_TAB_TEXT_I  = 0xFFAAAAAA;
            RECIPE_SLOT_BORDER = 0x44FFFFFF;
            RECIPE_SLOT_BG     = 0x1AFFFFFF;
            RECIPE_ARROW       = 0xFF4488FF;
            RECIPE_ARROW_ANIM  = 0x884488FF;
            RECIPE_TEXT_TITLE  = 0xFFFFFFFF;
            RECIPE_TEXT_ITEM   = 0xFFCCCCCC;
            RECIPE_TEXT_CAT    = 0xFFAAAAAA;
            RECIPE_TEXT_NAV    = 0xFFAAAAAA;
            RECIPE_TEXT_FOOTER = 0xFF777777;
            RECIPE_BTN_IDLE    = 0x4444AA44;
            RECIPE_BTN_HOVER   = 0x8844AA44;
            RECIPE_SHAPELESS   = 0xFF6666CC;
        } else {
            SECTION_SEP   = 0xFF888888;
            ROW_SEPARATOR = 0xFF999999;
            TEXT_HEADER   = 0xFF404040;
            
            DROPDOWN_BG        = 0xFFAAAAAA;
            DROPDOWN_BG_ACTIVE = 0xFF989898;
            DROPDOWN_LIST_BG   = 0xFFBBBBBB;

            SCROLL_TRACK        = 0xFFAAAAAA;
            SCROLL_THUMB        = 0xFF777777;
            SCROLL_THUMB_ACTIVE = 0xFF555555;
            
            BORDER_LIGHT = 0x33FFFFFF;
            GRADIENT_SHADOW = 0xCC000000;

            // Opaque solid premium dark recipe screen
            RECIPE_BG_OVERLAY  = 0xFF101010;
            RECIPE_PANEL       = 0xFF1A1A1F;
            RECIPE_PANEL_INNER = 0xFF22222A;
            RECIPE_BORDER      = 0xFF3A3A4A;
            RECIPE_HEADER_LINE = 0xFF2E2E3A;
            RECIPE_TAB_ACTIVE  = 0xFF4488FF;
            RECIPE_TAB_HOVER   = 0xFF2E2E44;
            RECIPE_TAB_IDLE    = 0xFF1E1E28;
            RECIPE_TAB_TEXT_A  = 0xFFFFFFFF;
            RECIPE_TAB_TEXT_I  = 0xFF8888AA;
            RECIPE_SLOT_BORDER = 0xFF555566;
            RECIPE_SLOT_BG     = 0xFF2A2A36;
            RECIPE_ARROW       = 0xFF6688CC;
            RECIPE_ARROW_ANIM  = 0xFF4466AA;
            RECIPE_TEXT_TITLE  = 0xFFFFFFFF;
            RECIPE_TEXT_ITEM   = 0xFFBBBBCC;
            RECIPE_TEXT_CAT    = 0xFF8888AA;
            RECIPE_TEXT_NAV    = 0xFF8888AA;
            RECIPE_TEXT_FOOTER = 0xFF555566;
            RECIPE_BTN_IDLE    = 0xFF226622;
            RECIPE_BTN_HOVER   = 0xFF44AA44;
            RECIPE_SHAPELESS   = 0xFF5555AA;
        }
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
