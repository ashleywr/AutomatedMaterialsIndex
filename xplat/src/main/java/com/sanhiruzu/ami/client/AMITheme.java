package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Central palette for all AMI UI colours.
 * All rendering code reads from here so the entire look can be changed in one place.
 */
public final class AMITheme {
    // Color swatch map for item variants
    private static final java.util.Map<String, Integer> SWATCH_COLORS = new java.util.HashMap<>();
    private static final int SWATCH_DEFAULT = 0xFF888888;
    // Layout (CSS-like constants)
    public static int GLOBAL_PADDING = 6;
    public static int ROW_HEIGHT = 18;
    public static int ICON_SIZE = 16;
    public static int ELEMENT_GAP = 4;
    // Panel chrome — vanilla MC light grey palette
    public static int PANEL_BG = 0xFFC6C6C6; // Vanilla inventory grey
    public static int PANEL_INNER = 0xFFD0D0D0; // Slightly lighter inner surface
    public static int PANEL_TEXTURE_LIGHT = 0x08FFFFFF;
    public static int PANEL_TEXTURE_DARK = 0x12000000;
    public static int PANEL_HEADER_BG = 0x22FFFFFF;
    public static int PANEL_CONTENT_BG = 0x26000000;
    public static int PANEL_CONTENT_BORDER = 0x24FFFFFF;
    // Header bar
    public static int HEADER_BG = 0xFFBBBBBB;
    public static int HEADER_SEP = 0xFF999999;
    public static int HEADER_TEXT = 0xFF111111;
    // Navigation arrows in header
    public static int ARROW_NORMAL = 0xFF555555;
    public static int ARROW_HOVER = 0xFF000000;
    // Namespace group headers in atlas lists
    public static int GROUP_BG = 0xFFBBBBBB;
    public static int GROUP_BG_HOVER = 0xFFAAAAAA;
    public static int GROUP_TEXT = 0xFF222222;
    public static int GROUP_HEADER_BG = 0xFFB0B0B0; // slightly darker than panel to separate from leaf rows
    public static int GROUP_HEADER_TEXT = 0xFF333333;
    // Atlas entry rows
    public static int ENTRY_HOVER = 0x3300AAFF; // vanilla-style blue tint @ ~20%
    public static int ENTRY_TEXT = 0xFF111111; // near-black on light background
    public static int ENTRY_SUBTITLE = 0xFF555555;
    // Scrollbar
    public static int SCROLL_TRACK = 0xFFAAAAAA;
    public static int SCROLL_THUMB = 0xFF777777;
    public static int SCROLL_THUMB_ACTIVE = 0xFF555555; // darker when hovered or dragging
    // Dimension badges
    public static int DIM_NETHER = 0xFFCC4444;
    public static int DIM_END = 0xFF9944CC;
    // Accents & Status
    public static int ACCENT_GOLD = 0xFFD6C17A;
    public static int ACCENT_BLUE = 0xFF4488FF;
    public static int POSITIVE = 0xFF55FF55;
    public static int NEGATIVE = 0xFFFF5555;
    // Borders & Outlines
    public static int BORDER_LIGHT = 0x33FFFFFF;
    public static int BORDER_DARK = 0x33000000;
    public static int WHITE = 0xFFFFFFFF;
    public static int BLACK = 0xFF000000;
    // Gradients & Shadows
    public static int GRADIENT_SHADOW = 0xCC000000;
    public static int TRANSPARENT = 0x00000000;
    // Typography (light-panel palette — do not use ChatFormatting enums for these)
    public static int TEXT_HEADER = 0xFF404040; // static UI labels, no drop shadow
    public static int TEXT_PRIMARY = 0xFFFFFFFF; // primary item names, with drop shadow
    public static int TEXT_SUBTLE = 0xFF555555; // metadata, counts, badges, no drop shadow
    public static int TEXT_HIGHLIGHT = 0xFF55FFFF; // active query match (e.g. @modid), with drop shadow
    // Row and section separators (not configurable — aesthetic constants)
    public static int ROW_SEPARATOR = 0xFF999999; // 1px line between list rows
    public static int SECTION_SEP = 0xFF888888; // 1px line between Toolbar/Results
    // Item grid
    public static int SLOT_BG = 0;
    public static int SLOT_EDGE_LIGHT = 0x22FFFFFF;
    public static int SLOT_EDGE_DARK = 0x66000000;
    public static int SLOT_HOVER = 0xFFAAAAAA;
    // Dropdown buttons and panels
    public static int DROPDOWN_BG = 0xFFAAAAAA; // button idle + list item hover
    public static int DROPDOWN_BG_ACTIVE = 0xFF989898; // button when open or hovered
    public static int DROPDOWN_LIST_BG = 0xFFBBBBBB; // open list panel background
    public static int CONTROL_EDGE_LIGHT = 0x26FFFFFF;
    public static int CONTROL_EDGE_DARK = 0x66000000;
    public static int CONTROL_SHADOW = 0x66000000;
    // Current location indicators
    public static int CURRENT_BIOME_BG = 0xFF1A2E1A; // subtle green tint
    public static int CURRENT_BIOME_ACCENT = 0xFF44DD44; // bright green left-edge bar
    public static int CURRENT_STRUCT_BG = 0xFF2E2A14; // subtle amber tint
    public static int CURRENT_STRUCT_ACCENT = 0xFFDD9933; // amber left-edge bar
    // Cheat mode
    public static int CHEAT_HEADER_BG = 0xFF3A2800; // dark amber replaces normal header bg
    public static int CHEAT_HEADER_SEP = 0xFF7A5200;
    public static int CHEAT_INDICATOR = 0xFFFFAA00; // gold indicator text
    public static int CHEAT_ENTRY_HOVER = 0xFF5A4A00; // amber entry highlight
    // Temperature (biome tooltips)
    public static int TEMP_COOL = 0xFF44AACC;
    public static int TEMP_WARM = 0xFFCCCC44;
    public static int TEMP_HOT = 0xFFCC8844;
    // Fallback icon backgrounds per node type
    public static int FALLBACK_BG_ENTITY = 0xFF1A2020;
    public static int FALLBACK_BG_PLAYER = 0xFF1A1A30;
    public static int FALLBACK_BG_BIOME = 0xFF1A2A1A;
    public static int FALLBACK_BG_STRUCTURE = 0xFF2A2A14;
    public static int FALLBACK_BG_DIMENSION = 0xFF201020;
    public static int FALLBACK_BG_DEFAULT = 0xFF1E1E1E;
    public static int FALLBACK_BG_DEV = 0xFFAA1100;
    // Dimension icon backgrounds (proxy block renderer)
    public static int DIM_ICON_BG_OVERWORLD = 0xFF122010;
    public static int DIM_ICON_BG_NETHER = 0xFF2B1408;
    public static int DIM_ICON_BG_END = 0xFF16101E;
    public static int DIM_ICON_BG_STRUCTURE = 0xFF252512;
    // Entity tooltip text colors
    public static int ENTITY_ID_COLOR = 0xFF666666;
    public static int ENTITY_CATEGORY_COLOR = 0xFF888888;
    public static int ENTITY_TRAITS_COLOR = 0xFF55FFFF;
    public static int ENTITY_DAMAGE_COLOR = 0xFFFF5555;
    public static int ENTITY_PLAYER_TEXT = 0xFF5555FF;
    // Item grid group highlights
    public static int GRID_NO_RESULTS_TEXT = 0xFFCCCCCC;
    public static int GRID_GOLD_BORDER = 0xFFD6C17A;
    public static int GRID_GOLD_TINT = 0x44D6C17A;
    public static int GRID_HEADER_DARKEN = 0x66000000;
    public static int GRID_HEADER_WHITE_DOT = 0xFFFFFFFF;
    public static int GRID_ROW_TINT_EVEN = 0x08FFFFFF;
    public static int GRID_ROW_TINT_ODD = 0x15000000;
    public static int GRID_GROUP_BAND = 0x10FFFFFF;
    public static int GRID_GROUP_BAND_ALT = 0x20FFFFFF;
    public static int GRID_GROUP_RAIL = 0x18FFFFFF;
    public static int GRID_GROUP_ROOT_BG = 0x38000000;
    public static int GRID_GROUP_CHILD_BG = 0x1CFFFFFF;
    // Search bar element colors
    public static int SEARCH_PLACEHOLDER = 0xFF666666;
    public static int SEARCH_CLEAR_TEXT = 0xFFAAAAAA;
    public static int SEARCH_CLEAR_TEXT_HOVER = 0xFFFFFFFF;
    public static int SEARCH_CURSOR = 0xFFCCCCCC;
    public static int SEARCH_SELECTION = 0xFF0000FF;
    public static int SEARCH_DEFAULT_TEXT = 0xFFCCCCCC;
    public static int SEARCH_HELP_BG = 0xEE08080A;
    public static int SEARCH_HELP_BORDER = 0x884488FF;
    public static int SEARCH_HELP_SHADOW = 0x99000000;
    public static int SEARCH_HELP_CHIP_BG = 0x22000000;
    public static int SEARCH_HELP_CHIP_BORDER = 0x334488FF;
    public static int SEARCH_HELP_SECTION_LINE = 0x224488FF;
    public static int SEARCH_HELP_TITLE = 0xFF55FFFF;
    public static int SEARCH_HELP_TEXT = 0xFFE6E6E6;
    // Heart bar tooltip
    public static int HEART_OVERFLOW_COLOR = 0xFFCC3333;
    public static int HEART_LABEL_COLOR = 0xFFAAAAAA;
    // Scroll indicator
    public static int SCROLL_INDICATOR_BG = 0xAA111111;
    // Config screen
    public static int CONFIG_BRAND_GOLD = 0xFFFFAA00;
    public static int CONFIG_HEADER_GOLD = 0xFFFFAA00;
    public static int CONFIG_TEXT_PRIMARY = 0xFFFFFFFF;
    public static int CONFIG_TEXT_SECONDARY = 0xFFAAAAAA;
    public static int CONFIG_TEXT_MUTED = 0xFF777777;
    public static int CONFIG_BOOL_TRUE = 0x8800FF00;
    public static int CONFIG_BOOL_FALSE = 0x88FF0000;
    public static int CONFIG_SWATCH_BORDER = 0xFFFFFFFF;
    public static int CONFIG_PANEL_TITLE = 0xFFFFAA00;
    public static int CONFIG_CARD_BG = 0x15FFFFFF;
    public static int CONFIG_SEP = 0x33FFFFFF;
    // Registry utility colors
    public static int REGISTRY_CATEGORY_MONSTER = 0xFFCC4444;
    public static int REGISTRY_CATEGORY_CREATURE = 0xFF44AA44;
    public static int REGISTRY_CATEGORY_AMBIENT = 0xFFAAAA44;
    public static int REGISTRY_CATEGORY_AQUATIC = 0xFF4488CC;
    public static int REGISTRY_CATEGORY_DEFAULT = 0xFF888888;
    public static int REGISTRY_DIM_OVERWORLD = 0xFF66BB6A;
    public static int REGISTRY_DIM_NETHER = 0xFFCC4444;
    public static int REGISTRY_DIM_END = 0xFF7A51A6;
    // Mod name color
    public static int MOD_NAME;
    // Token colorizer
    public static int TOKEN_ENV = 0xFF44BB44;
    public static int TOKEN_PROP = 0xFFBBBB44;
    public static int TOKEN_ESSENTIAL = 0xFFBB44BB;
    public static int TOKEN_ESM = 0xFFBB8844;
    public static int TOKEN_META = 0xFF44CCCC;
    public static int TOKEN_PLAIN = 0xFFCCCCCC;
    // Sidebar toggle
    public static int SIDEBAR_TOGGLE_HOVER_HALO = 0x33FFFFFF;
    public static int SIDEBAR_TOGGLE_IDLE_HALO = 0x11FFFFFF;
    public static int SIDEBAR_TOGGLE_BORDER = 0x88FFFFFF;
    // AMI button state colors
    public static int BUTTON_ACTIVE = 0xFFFFDD44;
    public static int BUTTON_HOVER = 0xFFFFFFA0;
    // Player name color
    public static int PLAYER_NAME_COLOR;
    // Recipe Viewer dynamic theme variables
    public static int RECIPE_BG_OVERLAY = 0xFF101010;
    public static int RECIPE_PANEL = 0xFF1A1A1F;
    public static int RECIPE_PANEL_INNER = 0xFF22222A;
    public static int RECIPE_BORDER = 0xFF3A3A4A;
    public static int RECIPE_HEADER_LINE = 0xFF2E2E3A;
    public static int RECIPE_TAB_ACTIVE = 0xFF4488FF;
    public static int RECIPE_TAB_HOVER = 0xFF2E2E44;
    public static int RECIPE_TAB_IDLE = 0xFF1E1E28;
    public static int RECIPE_TAB_TEXT_A = 0xFFFFFFFF;
    public static int RECIPE_TAB_TEXT_I = 0xFF8888AA;
    public static int RECIPE_SLOT_BORDER = 0xFF555566;
    public static int RECIPE_SLOT_BG = 0xFF2A2A36;
    public static int RECIPE_ARROW = 0xFF6688CC;
    public static int RECIPE_ARROW_ANIM = 0xFF4466AA;
    public static int RECIPE_TEXT_TITLE = 0xFFFFFFFF;
    public static int RECIPE_TEXT_ITEM = 0xFFBBBBCC;
    public static int RECIPE_TEXT_CAT = 0xFF8888AA;
    public static int RECIPE_TEXT_NAV = 0xFF8888AA;
    public static int RECIPE_TEXT_FOOTER = 0xFF555566;
    public static int RECIPE_BTN_IDLE = 0xFF226622;
    public static int RECIPE_BTN_HOVER = 0xFF44AA44;
    public static int RECIPE_SHAPELESS = 0xFF5555AA;
    /**
     * Synchronizes theme fields with AmiConfig values.
     * Called during mod initialization and config reload events.
     */
    public static int SIDEBAR_BG;
    public static int SIDEBAR_TEXT;
    public static int SIDEBAR_TEXT_ACTIVE;
    public static int SIDEBAR_SELECTION;

    static {
        SWATCH_COLORS.put("red", 0xFFCC3333);
        SWATCH_COLORS.put("orange", 0xFFDD7722);
        SWATCH_COLORS.put("yellow", 0xFFDDCC22);
        SWATCH_COLORS.put("lime", 0xFF44AA44);
        SWATCH_COLORS.put("green", 0xFF44AA44);
        SWATCH_COLORS.put("cyan", 0xFF22AACC);
        SWATCH_COLORS.put("blue", 0xFF3355DD);
        SWATCH_COLORS.put("light_blue", 0xFF3355DD);
        SWATCH_COLORS.put("purple", 0xFF9933CC);
        SWATCH_COLORS.put("magenta", 0xFF9933CC);
        SWATCH_COLORS.put("pink", 0xFFFFB7C5);
        SWATCH_COLORS.put("white", 0xFFEEEEEE);
        SWATCH_COLORS.put("light_gray", 0xFFAAAAAA);
        SWATCH_COLORS.put("silver", 0xFFAAAAAA);
        SWATCH_COLORS.put("gray", 0xFF666666);
        SWATCH_COLORS.put("black", 0xFF222222);
        SWATCH_COLORS.put("brown", 0xFF885533);
    }

    private AMITheme() {
    }

    // ── Rendering helpers ─────────────────────────────────────────────────────

    public static int getSwatchColor(String colorName) {
        return SWATCH_COLORS.getOrDefault(colorName.toLowerCase(java.util.Locale.ROOT), SWATCH_DEFAULT);
    }

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
        g.fill(x + 2, y, x + w - 2, y + 1, color); // Top edge
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color); // Bottom edge
        g.fill(x, y + 2, x + 1, y + h - 2, color); // Left edge
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, color); // Right edge
    }

    public static void fillPanelChrome(GuiGraphics g, int x, int y, int w, int h) {
        fillRounded(g, x, y, w, h, PANEL_BG);
        fillPixelTexture(g, x + 1, y + 1, w - 2, h - 2);

        g.fill(x + 2, y, x + w - 2, y + 1, CONTROL_EDGE_LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x14000000);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, CONTROL_EDGE_DARK);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, 0xAA000000);
        g.fill(x, y + 2, x + 1, y + h - 2, CONTROL_EDGE_LIGHT);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, CONTROL_EDGE_DARK);
    }

    public static void fillInsetRect(GuiGraphics g, int x, int y, int w, int h, int fill, boolean pressed) {
        g.fill(x, y, x + w, y + h, fill);
        fillPixelTexture(g, x + 1, y + 1, w - 2, h - 2);

        int top = pressed ? CONTROL_EDGE_DARK : CONTROL_EDGE_LIGHT;
        int bottom = pressed ? CONTROL_EDGE_LIGHT : CONTROL_EDGE_DARK;
        g.fill(x, y, x + w, y + 1, top);
        g.fill(x, y, x + 1, y + h, top);
        g.fill(x, y + h - 1, x + w, y + h, bottom);
        g.fill(x + w - 1, y, x + w, y + h, bottom);
    }

    public static void fillControlChrome(GuiGraphics g, int x, int y, int w, int h, int fill, boolean pressed) {
        if (!pressed && CONTROL_SHADOW != 0) {
            g.fill(x + 1, y + 1, x + w + 1, y + h + 1, CONTROL_SHADOW);
        }
        fillInsetRect(g, x, y, w, h, fill, pressed);
        if (!pressed) {
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x22000000);
        }
    }

    public static void fillPanelHeaderChrome(GuiGraphics g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, PANEL_HEADER_BG);
        fillPixelTexture(g, x + 1, y + 1, w - 2, h - 2);
        g.fill(x, y, x + w, y + 1, CONTROL_EDGE_LIGHT);
        g.fill(x, y, x + 1, y + h, CONTROL_EDGE_LIGHT);
        g.fill(x, y + h - 1, x + w, y + h, CONTROL_EDGE_DARK);
        g.fill(x + w - 1, y, x + w, y + h, CONTROL_EDGE_DARK);
    }

    public static void fillContentChrome(GuiGraphics g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        fillRounded(g, x, y, w, h, PANEL_CONTENT_BORDER);
        fillRounded(g, x + 1, y + 1, w - 2, h - 2, PANEL_CONTENT_BG);
        fillPixelTexture(g, x + 2, y + 2, w - 4, h - 4);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, CONTROL_EDGE_DARK);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, CONTROL_EDGE_DARK);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, CONTROL_EDGE_LIGHT);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, CONTROL_EDGE_LIGHT);
    }

    public static void fillPixelTexture(GuiGraphics g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;

        for (int py = y + 2; py < y + h; py += 7) {
            for (int px = x + ((py / 7) & 1) * 3; px < x + w; px += 11) {
                g.fill(px, py, Math.min(px + 1, x + w), Math.min(py + 1, y + h), PANEL_TEXTURE_LIGHT);
            }
        }
        for (int py = y + 5; py < y + h; py += 9) {
            for (int px = x + 2 + ((py / 9) & 1) * 4; px < x + w; px += 13) {
                g.fill(px, py, Math.min(px + 2, x + w), Math.min(py + 1, y + h), PANEL_TEXTURE_DARK);
            }
        }
    }

    public static void sync() {
        GLOBAL_PADDING = AmiConfig.globalPadding;
        ROW_HEIGHT = AmiConfig.rowHeight;
        ICON_SIZE = AmiConfig.iconSize;
        ELEMENT_GAP = AmiConfig.elementGap;

        PANEL_BG = AmiConfig.panelBg;
        PANEL_INNER = AmiConfig.searchBarBg;
        ENTRY_HOVER = AmiConfig.cardBgHover;
        ENTRY_TEXT = AmiConfig.cardTextName;
        ENTRY_SUBTITLE = AmiConfig.cardTextSubtitle;
        TEXT_PRIMARY = AmiConfig.cardTextName;
        TEXT_SUBTLE = AmiConfig.cardTextSubtitle;
        SLOT_BG = 0; // default transparent
        SLOT_HOVER = AmiConfig.cardBgHover;

        ACCENT_BLUE = AmiConfig.accentColor;

        if (AmiConfig.theme == AmiConfig.Theme.MODERN) {
            // "Modern" is now the glassy look the user liked for readability.
            PANEL_BG = 0xCC101014; // ~80% opacity deep dark glass
            PANEL_INNER = 0x33FFFFFF;
            PANEL_TEXTURE_LIGHT = 0x08FFFFFF;
            PANEL_TEXTURE_DARK = 0x18000000;
            PANEL_HEADER_BG = 0x2AFFFFFF;
            PANEL_CONTENT_BG = 0x38000000;
            PANEL_CONTENT_BORDER = 0x30FFFFFF;

            GROUP_HEADER_BG = 0x2AFFFFFF;
            GROUP_HEADER_TEXT = 0xFFFFFFFF;

            SIDEBAR_BG = 0xCC101014;
            SIDEBAR_TEXT = 0xFFBBBBBB;
            SIDEBAR_TEXT_ACTIVE = 0xFFFFFFFF;
            SIDEBAR_SELECTION = 0x44FFFFFF;

            ENTRY_HOVER = 0x44FFFFFF;
            ENTRY_TEXT = 0xFFE8E4D8;
            ENTRY_SUBTITLE = 0xFFB8B2A4;

            SECTION_SEP = 0x2CFFFFFF;
            ROW_SEPARATOR = 0x11FFFFFF;
            TEXT_HEADER = 0xFFE7E2D2;
            TEXT_PRIMARY = 0xFFECE6D8;
            TEXT_SUBTLE = 0xFFB9B2A3;
            TEXT_HIGHLIGHT = 0xFF55FFFF;
            MOD_NAME = 0xFF5555FF;

            SLOT_BG = 0x22000000;
            SLOT_EDGE_LIGHT = 0x34FFFFFF;
            SLOT_EDGE_DARK = 0x99000000;

            DROPDOWN_BG = 0x2CFFFFFF;
            DROPDOWN_BG_ACTIVE = 0x46FFFFFF;
            DROPDOWN_LIST_BG = 0xCC000000;
            CONTROL_EDGE_LIGHT = 0x30FFFFFF;
            CONTROL_EDGE_DARK = 0x88000000;
            CONTROL_SHADOW = 0x66000000;

            SCROLL_TRACK = 0x11FFFFFF;
            SCROLL_THUMB = 0x44FFFFFF;
            SCROLL_THUMB_ACTIVE = 0x66FFFFFF;
            SCROLL_INDICATOR_BG = 0x66000000;
            GRID_GROUP_BAND = 0x0F000000;
            GRID_GROUP_BAND_ALT = 0x16FFFFFF;
            GRID_GROUP_RAIL = 0x38FFFFFF;
            GRID_GROUP_ROOT_BG = 0x52000000;
            GRID_GROUP_CHILD_BG = 0x24FFFFFF;

            SEARCH_PLACEHOLDER = 0xFF888888;
            SEARCH_DEFAULT_TEXT = 0xFFFFFFFF;
            SEARCH_HELP_BG = 0xF0101014;
            SEARCH_HELP_BORDER = AmiConfig.searchBarBorder;
            SEARCH_HELP_SHADOW = 0xAA000000;
            SEARCH_HELP_CHIP_BG = 0x22FFFFFF;
            SEARCH_HELP_CHIP_BORDER = 0x33FFFFFF;
            SEARCH_HELP_SECTION_LINE = 0x22FFFFFF;
            SEARCH_HELP_TITLE = TEXT_HIGHLIGHT;
            SEARCH_HELP_TEXT = TEXT_PRIMARY;

            BORDER_LIGHT = 0x33FFFFFF;
            GRADIENT_SHADOW = 0x88000000;

            CONFIG_CARD_BG = 0x15FFFFFF;
            CONFIG_SEP = 0x33FFFFFF;

            PLAYER_NAME_COLOR = 0xFF4488FF;

            // Translucent glass recipe screen
            RECIPE_BG_OVERLAY = 0x66000000;
            RECIPE_PANEL = 0xD8121216; // 85% opacity dark glass
            RECIPE_PANEL_INNER = 0x88202028;
            RECIPE_BORDER = 0x33FFFFFF;
            RECIPE_HEADER_LINE = 0x22FFFFFF;
            RECIPE_TAB_ACTIVE = AmiConfig.accentColor;
            RECIPE_TAB_HOVER = 0x44FFFFFF;
            RECIPE_TAB_IDLE = 0x15FFFFFF;
            RECIPE_TAB_TEXT_A = 0xFFFFFFFF;
            RECIPE_TAB_TEXT_I = 0xFFAAAAAA;
            RECIPE_SLOT_BORDER = 0x44FFFFFF;
            RECIPE_SLOT_BG = 0x1AFFFFFF;
            RECIPE_ARROW = AmiConfig.accentColor;
            RECIPE_ARROW_ANIM = 0x884488FF;
            RECIPE_TEXT_TITLE = 0xFFFFFFFF;
            RECIPE_TEXT_ITEM = 0xFFCCCCCC;
            RECIPE_TEXT_CAT = 0xFFAAAAAA;
            RECIPE_TEXT_NAV = 0xFFAAAAAA;
            RECIPE_TEXT_FOOTER = 0xFF777777;
            RECIPE_BTN_IDLE = 0x4444AA44;
            RECIPE_BTN_HOVER = 0x8844AA44;
            RECIPE_SHAPELESS = 0xFF6666CC;
        } else if (AmiConfig.theme == AmiConfig.Theme.TRANSPARENT) {
            // "Transparent" is now ultra-minimal like EMI.
            PANEL_BG = 0x1A000000; // ~10% opacity black
            PANEL_INNER = 0;
            PANEL_TEXTURE_LIGHT = 0x04FFFFFF;
            PANEL_TEXTURE_DARK = 0x08000000;
            PANEL_HEADER_BG = 0x10FFFFFF;
            PANEL_CONTENT_BG = 0x18000000;
            PANEL_CONTENT_BORDER = 0x16FFFFFF;

            GROUP_HEADER_BG = 0x15FFFFFF;
            GROUP_HEADER_TEXT = 0xFFCCCCCC;

            SIDEBAR_BG = 0x1A000000;
            SIDEBAR_TEXT = 0xFFAAAAAA;
            SIDEBAR_TEXT_ACTIVE = 0xFFFFFFFF;
            SIDEBAR_SELECTION = 0x33FFFFFF;

            ENTRY_HOVER = 0x22FFFFFF;
            ENTRY_TEXT = 0xFFFFFFFF;
            ENTRY_SUBTITLE = 0xFF888888;

            SECTION_SEP = 0x11FFFFFF;
            ROW_SEPARATOR = 0x08FFFFFF;
            TEXT_HEADER = 0xFFCCCCCC;
            TEXT_PRIMARY = 0xFFFFFFFF;
            TEXT_SUBTLE = 0xFF888888;
            TEXT_HIGHLIGHT = 0xFF55FFFF;
            MOD_NAME = 0xFF5555FF;

            DROPDOWN_BG = 0x22FFFFFF;
            DROPDOWN_BG_ACTIVE = 0x44FFFFFF;
            DROPDOWN_LIST_BG = 0x99000000;
            SLOT_BG = 0x1A000000;
            SLOT_EDGE_LIGHT = 0x20FFFFFF;
            SLOT_EDGE_DARK = 0x66000000;
            CONTROL_EDGE_LIGHT = 0x18FFFFFF;
            CONTROL_EDGE_DARK = 0x44000000;
            CONTROL_SHADOW = 0x33000000;
            SEARCH_HELP_BG = 0xDD000000;
            SEARCH_HELP_BORDER = 0x33FFFFFF;
            SEARCH_HELP_SHADOW = 0x66000000;
            SEARCH_HELP_CHIP_BG = 0x18FFFFFF;
            SEARCH_HELP_CHIP_BORDER = 0x22FFFFFF;
            SEARCH_HELP_SECTION_LINE = 0x18FFFFFF;
            SEARCH_HELP_TITLE = TEXT_HIGHLIGHT;
            SEARCH_HELP_TEXT = TEXT_PRIMARY;

            SCROLL_TRACK = 0;
            SCROLL_THUMB = 0x33FFFFFF;
            SCROLL_THUMB_ACTIVE = 0x55FFFFFF;
            SCROLL_INDICATOR_BG = 0x33000000;
            GRID_GROUP_BAND = 0x0C000000;
            GRID_GROUP_BAND_ALT = 0x14FFFFFF;
            GRID_GROUP_RAIL = 0x14000000;
            GRID_GROUP_ROOT_BG = 0x26000000;
            GRID_GROUP_CHILD_BG = 0x12FFFFFF;

            BORDER_LIGHT = 0; // NO BORDERS
            ACCENT_BLUE = 0; // NO ACCENT LINE
            GRADIENT_SHADOW = 0;

            CONFIG_CARD_BG = 0x10000000;
            CONFIG_SEP = 0x11FFFFFF;

            PLAYER_NAME_COLOR = 0xFF4488FF;

            RECIPE_BG_OVERLAY = 0x33000000;
            RECIPE_PANEL = 0x99000000;
            RECIPE_PANEL_INNER = 0;
            RECIPE_BORDER = 0x22FFFFFF;
            RECIPE_HEADER_LINE = 0x11FFFFFF;
            RECIPE_TAB_ACTIVE = AmiConfig.accentColor;
            RECIPE_TAB_HOVER = 0x33FFFFFF;
            RECIPE_TAB_IDLE = 0;
            RECIPE_TAB_TEXT_A = 0xFFFFFFFF;
            RECIPE_TAB_TEXT_I = 0xFFCCCCCC;
            RECIPE_SLOT_BORDER = 0x22FFFFFF;
            RECIPE_SLOT_BG = 0x11FFFFFF;
            RECIPE_ARROW = 0xFFCCCCCC;
            RECIPE_ARROW_ANIM = AmiConfig.accentColor;
            RECIPE_TEXT_TITLE = 0xFFFFFFFF;
            RECIPE_TEXT_ITEM = 0xFFCCCCCC;
            RECIPE_TEXT_CAT = 0xFF888888;
            RECIPE_TEXT_NAV = 0xFF888888;
            RECIPE_TEXT_FOOTER = 0xFF666666;
            RECIPE_BTN_IDLE = 0x3344AA44;
            RECIPE_BTN_HOVER = 0x6644AA44;
            RECIPE_SHAPELESS = 0xFF8888FF;
        } else {
            // VANILLA
            PANEL_BG = 0xFFC6C6C6;
            PANEL_INNER = 0xFFD0D0D0;
            PANEL_TEXTURE_LIGHT = 0x22FFFFFF;
            PANEL_TEXTURE_DARK = 0x18000000;
            PANEL_HEADER_BG = 0xFFC0C0C0;
            PANEL_CONTENT_BG = 0xFFD0D0D0;
            PANEL_CONTENT_BORDER = 0xFF8A8A8A;
            SLOT_BG = 0xFF2D2D2D; // Darker slots for contrast as requested
            SLOT_EDGE_LIGHT = 0xFF5C5C5C;
            SLOT_EDGE_DARK = 0xFF141414;

            ENTRY_HOVER = 0x444488FF; // Subtle blue hover on dark slots
            ENTRY_TEXT = 0xFF111111;
            ENTRY_SUBTITLE = 0xFF555555;

            TEXT_PRIMARY = 0xFF404040;
            TEXT_SUBTLE = 0xFF666666;
            TEXT_HEADER = 0xFF333333;
            TEXT_HIGHLIGHT = 0xFF0000AA;
            MOD_NAME = 0xFF0000AA;

            SIDEBAR_BG = 0xFFC6C6C6;
            SIDEBAR_TEXT = 0xFF404040;
            SIDEBAR_TEXT_ACTIVE = 0xFF000000;
            SIDEBAR_SELECTION = 0x44000000;

            SECTION_SEP = 0xFF888888;
            ROW_SEPARATOR = 0xFFBBBBBB;

            DROPDOWN_BG = 0xFFC6C6C6;
            DROPDOWN_BG_ACTIVE = 0xFFB0B0B0;
            DROPDOWN_LIST_BG = 0xFFD0D0D0;
            CONTROL_EDGE_LIGHT = 0xAAFFFFFF;
            CONTROL_EDGE_DARK = 0x88000000;
            CONTROL_SHADOW = 0x55000000;
            SEARCH_HELP_BG = 0xFFF0F0F0;
            SEARCH_HELP_BORDER = AmiConfig.searchBarBorder;
            SEARCH_HELP_SHADOW = 0x66000000;
            SEARCH_HELP_CHIP_BG = 0xFFE0E0E0;
            SEARCH_HELP_CHIP_BORDER = 0xFF9A9A9A;
            SEARCH_HELP_SECTION_LINE = 0xFFB8B8B8;
            SEARCH_HELP_TITLE = TEXT_HIGHLIGHT;
            SEARCH_HELP_TEXT = TEXT_PRIMARY;

            SCROLL_TRACK = 0xFFC6C6C6;
            SCROLL_THUMB = 0xFF8B8B8B;
            SCROLL_THUMB_ACTIVE = 0xFF6B6B6B;
            SCROLL_INDICATOR_BG = 0x66000000;
            GRID_GROUP_BAND = 0x0C000000;
            GRID_GROUP_BAND_ALT = 0x14FFFFFF;
            GRID_GROUP_RAIL = 0x66000000;
            GRID_GROUP_ROOT_BG = 0x33000000;
            GRID_GROUP_CHILD_BG = 0x22FFFFFF;

            BORDER_LIGHT = 0x33000000;
            GRADIENT_SHADOW = 0x66000000;

            CONFIG_CARD_BG = 0x11000000;
            CONFIG_SEP = 0x22000000;

            PLAYER_NAME_COLOR = 0xFF0000AA;
        }
        ThemeResourceLoader.applyCurrentTheme();
    }

    public static void load() {
    }
}
