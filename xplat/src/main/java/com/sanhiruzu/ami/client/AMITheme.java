package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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
    public static int SEARCH_BAR_BG = 0x33000000;
    public static int SEARCH_BAR_BORDER = 0x884488FF;
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
    public static int RECIPE_WORKSTATION_BG = 0xFF181820;
    public static int RECIPE_WORKSTATION_TEXT = 0xFF6666AA;
    public static int RECIPE_TAB_ARROW = 0xFF6688CC;
    public static int RECIPE_TAB_ARROW_HOVER = 0xFF4488FF;
    public static int RECIPE_COUNT_BADGE = 0xFF444466;
    public static int RECIPE_COUNT_BADGE_TEXT = 0xFF8888BB;
    public static int RECIPE_OUTPUT_SLOT_BORDER = 0xFF886633;
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
        if (colorName == null) return SWATCH_DEFAULT;
        return SWATCH_COLORS.getOrDefault(colorName.toLowerCase(java.util.Locale.ROOT), SWATCH_DEFAULT);
    }

    /**
     * Draws a 1px border around a 2px-radius rounded rectangle.
     */
    public static void drawRoundedBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int backgroundColor, int borderColor) {
        fillPixelCornerBorder(g, x, y, w, h, backgroundColor, borderColor);
    }

    /**
     * Fills a rectangle with 2px-radius rounded corners (3 fill calls, corner pixels omitted).
     * Falls back to a plain fill when the rectangle is too small to round.
     */
    public static void fillRounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
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

    public static void fillPixelPopup(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border, int shadow, int accent) {
        if (w <= 0 || h <= 0) return;
        if ((shadow >>> 24) != 0) {
            fillPixelCorner(g, x + 2, y + 2, w, h, shadow);
        }
        fillPixelCornerBorder(g, x, y, w, h, fill, border);
        if ((accent >>> 24) != 0 && w > 4 && h > 4) {
            fillVisible(g, x + 2, y + 2, x + w - 2, y + 3, accent);
        }
    }

    public static void fillSuggestionPopup(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        fillPixelCorner(g, x, y, w, h, solidPopupColor(DROPDOWN_LIST_BG));
        fillVisible(g, x + 1, y, x + w - 1, y + 1, BORDER_LIGHT);
        fillVisible(g, x + 1, y + h - 1, x + w - 1, y + h, BORDER_DARK);
        fillVisible(g, x, y + 1, x + 1, y + h - 1, BORDER_LIGHT);
        fillVisible(g, x + w - 1, y + 1, x + w, y + h - 1, BORDER_DARK);
    }

    public static void fillPixelCornerBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        if (w <= 0 || h <= 0) return;
        if (w < 4 || h < 4) {
            fillBorderedRect(g, x, y, w, h, fill, border);
            return;
        }
        fillPixelCorner(g, x, y, w, h, fill);
        fillVisible(g, x + 1, y, x + w - 1, y + 1, border);
        fillVisible(g, x + 1, y + h - 1, x + w - 1, y + h, border);
        fillVisible(g, x, y + 1, x + 1, y + h - 1, border);
        fillVisible(g, x + w - 1, y + 1, x + w, y + h - 1, border);
    }

    public static void fillPixelCorner(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        if (w < 3 || h < 3) {
            fillVisible(g, x, y, x + w, y + h, color);
            return;
        }
        fillVisible(g, x + 1, y, x + w - 1, y + h, color);
        fillVisible(g, x, y + 1, x + 1, y + h - 1, color);
        fillVisible(g, x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void fillPanelChrome(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        fillVisible(g, x, y, x + w, y + h, PANEL_BG);
        fillVisible(g, x, y, x + w, y + 1, CONTROL_EDGE_LIGHT);
        fillVisible(g, x, y, x + 1, y + h, CONTROL_EDGE_LIGHT);
        fillVisible(g, x, y + h - 1, x + w, y + h, CONTROL_EDGE_DARK);
        fillVisible(g, x + w - 1, y, x + w, y + h, CONTROL_EDGE_DARK);
    }

    public static void fillInsetRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, boolean pressed) {
        if (w <= 0 || h <= 0) return;
        fillVisible(g, x, y, x + w, y + h, fill);
        fillPixelTexture(g, x + 1, y + 1, w - 2, h - 2);

        int top = pressed ? CONTROL_EDGE_DARK : CONTROL_EDGE_LIGHT;
        int bottom = pressed ? CONTROL_EDGE_LIGHT : CONTROL_EDGE_DARK;
        fillVisible(g, x, y, x + w, y + 1, top);
        fillVisible(g, x, y, x + 1, y + h, top);
        fillVisible(g, x, y + h - 1, x + w, y + h, bottom);
        fillVisible(g, x + w - 1, y, x + w, y + h, bottom);
    }

    public static void fillBorderedRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        if (w <= 0 || h <= 0) return;
        fillVisible(g, x, y, x + w, y + h, fill);
        fillVisible(g, x, y, x + w, y + 1, border);
        fillVisible(g, x, y + h - 1, x + w, y + h, border);
        fillVisible(g, x, y, x + 1, y + h, border);
        fillVisible(g, x + w - 1, y, x + w, y + h, border);
    }

    public static void fillControlChrome(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, boolean pressed) {
        if (w <= 0 || h <= 0) return;
        if (!pressed && (CONTROL_SHADOW >>> 24) != 0) {
            fillVisible(g, x + 1, y + 1, x + w + 1, y + h + 1, CONTROL_SHADOW);
        }
        fillInsetRect(g, x, y, w, h, fill, pressed);
        if (!pressed) {
            fillVisible(g, x + 1, y + h - 2, x + w - 1, y + h - 1, 0x22000000);
        }
    }

    public static void fillPanelHeaderChrome(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        fillVisible(g, x, y, x + w, y + h, PANEL_HEADER_BG);
        fillVisible(g, x, y, x + w, y + 1, CONTROL_EDGE_LIGHT);
        fillVisible(g, x, y, x + 1, y + h, CONTROL_EDGE_LIGHT);
        fillVisible(g, x, y + h - 1, x + w, y + h, CONTROL_EDGE_DARK);
        fillVisible(g, x + w - 1, y, x + w, y + h, CONTROL_EDGE_DARK);
    }

    public static void fillContentChrome(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        fillVisible(g, x, y, x + w, y + h, PANEL_CONTENT_BORDER);
        if (w <= 2 || h <= 2) return;
        fillVisible(g, x + 1, y + 1, x + w - 1, y + h - 1, PANEL_CONTENT_BG);
        fillVisible(g, x + 1, y + 1, x + w - 1, y + 2, CONTROL_EDGE_DARK);
        fillVisible(g, x + 1, y + 1, x + 2, y + h - 1, CONTROL_EDGE_DARK);
        fillVisible(g, x + 1, y + h - 2, x + w - 1, y + h - 1, CONTROL_EDGE_LIGHT);
        fillVisible(g, x + w - 2, y + 1, x + w - 1, y + h - 1, CONTROL_EDGE_LIGHT);
    }

    public static void fillPixelTexture(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        // Per-pixel GuiGraphicsExtractor.fill calls force costly GUI buffer flushes on 1.21.1.
        // Keep panel chrome flat; borders and translucent surfaces provide the depth.
    }

    private static void fillVisible(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1 || (color >>> 24) == 0) return;
        g.fill(x1, y1, x2, y2, color);
    }

    private static int solidPopupColor(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        return (Math.max(alpha, 0xF0) << 24) | (argb & 0x00FFFFFF);
    }

    private static int visibleColor(int color) {
        return (color >>> 24) == 0 ? 0xFF000000 | color : color;
    }

    private static int transparencyAdjusted(int color, int percent) {
        return transparencyAdjusted(color, percent, 0);
    }

    private static int transparencyAdjusted(int color, int percent, int minAlphaAtFull) {
        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0 || percent <= 0) return color;
        int nextAlpha = Math.max(0, Math.min(255, alpha * (100 - percent) / 100));
        if (percent >= 100 && minAlphaAtFull > 0) {
            nextAlpha = Math.max(nextAlpha, Math.min(alpha, minAlphaAtFull));
        }
        return (nextAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static void applyThemeTransparency() {
        int percent = Math.max(0, Math.min(99, AmiConfig.themeTransparency));
        if (percent <= 0) return;

        PANEL_BG = transparencyAdjusted(PANEL_BG, percent);
        PANEL_INNER = transparencyAdjusted(PANEL_INNER, percent);
        PANEL_TEXTURE_LIGHT = transparencyAdjusted(PANEL_TEXTURE_LIGHT, percent);
        PANEL_TEXTURE_DARK = transparencyAdjusted(PANEL_TEXTURE_DARK, percent);
        PANEL_HEADER_BG = transparencyAdjusted(PANEL_HEADER_BG, percent);
        PANEL_CONTENT_BG = transparencyAdjusted(PANEL_CONTENT_BG, percent);
        PANEL_CONTENT_BORDER = transparencyAdjusted(PANEL_CONTENT_BORDER, percent, 0x20);

        HEADER_BG = transparencyAdjusted(HEADER_BG, percent);
        HEADER_SEP = transparencyAdjusted(HEADER_SEP, percent);
        GROUP_BG = transparencyAdjusted(GROUP_BG, percent);
        GROUP_BG_HOVER = transparencyAdjusted(GROUP_BG_HOVER, percent);
        GROUP_HEADER_BG = transparencyAdjusted(GROUP_HEADER_BG, percent);

        SIDEBAR_BG = transparencyAdjusted(SIDEBAR_BG, percent);
        SIDEBAR_SELECTION = transparencyAdjusted(SIDEBAR_SELECTION, percent, 0x20);
        ENTRY_HOVER = transparencyAdjusted(ENTRY_HOVER, percent, 0x20);

        SECTION_SEP = transparencyAdjusted(SECTION_SEP, percent);
        ROW_SEPARATOR = transparencyAdjusted(ROW_SEPARATOR, percent);
        SLOT_BG = transparencyAdjusted(SLOT_BG, percent);
        SLOT_EDGE_LIGHT = transparencyAdjusted(SLOT_EDGE_LIGHT, percent, 0x20);
        SLOT_EDGE_DARK = transparencyAdjusted(SLOT_EDGE_DARK, percent, 0x20);
        SLOT_HOVER = transparencyAdjusted(SLOT_HOVER, percent, 0x20);

        DROPDOWN_BG = transparencyAdjusted(DROPDOWN_BG, percent, 0x20);
        DROPDOWN_BG_ACTIVE = transparencyAdjusted(DROPDOWN_BG_ACTIVE, percent, 0x28);
        DROPDOWN_LIST_BG = transparencyAdjusted(DROPDOWN_LIST_BG, percent, 0x28);
        CONTROL_EDGE_LIGHT = transparencyAdjusted(CONTROL_EDGE_LIGHT, percent, 0x20);
        CONTROL_EDGE_DARK = transparencyAdjusted(CONTROL_EDGE_DARK, percent, 0x20);
        CONTROL_SHADOW = transparencyAdjusted(CONTROL_SHADOW, percent);

        SCROLL_TRACK = transparencyAdjusted(SCROLL_TRACK, percent);
        SCROLL_THUMB = transparencyAdjusted(SCROLL_THUMB, percent, 0x20);
        SCROLL_THUMB_ACTIVE = transparencyAdjusted(SCROLL_THUMB_ACTIVE, percent, 0x28);
        SCROLL_INDICATOR_BG = transparencyAdjusted(SCROLL_INDICATOR_BG, percent);

        GRID_GOLD_TINT = transparencyAdjusted(GRID_GOLD_TINT, percent);
        GRID_HEADER_DARKEN = transparencyAdjusted(GRID_HEADER_DARKEN, percent);
        GRID_ROW_TINT_EVEN = transparencyAdjusted(GRID_ROW_TINT_EVEN, percent);
        GRID_ROW_TINT_ODD = transparencyAdjusted(GRID_ROW_TINT_ODD, percent);
        GRID_GROUP_BAND = transparencyAdjusted(GRID_GROUP_BAND, percent);
        GRID_GROUP_BAND_ALT = transparencyAdjusted(GRID_GROUP_BAND_ALT, percent);
        GRID_GROUP_RAIL = transparencyAdjusted(GRID_GROUP_RAIL, percent);
        GRID_GROUP_ROOT_BG = transparencyAdjusted(GRID_GROUP_ROOT_BG, percent);
        GRID_GROUP_CHILD_BG = transparencyAdjusted(GRID_GROUP_CHILD_BG, percent);

        SEARCH_BAR_BG = transparencyAdjusted(SEARCH_BAR_BG, percent, 0x20);
        SEARCH_BAR_BORDER = transparencyAdjusted(SEARCH_BAR_BORDER, percent, 0x28);
        SEARCH_SELECTION = transparencyAdjusted(SEARCH_SELECTION, percent, 0x30);
        SEARCH_HELP_BG = transparencyAdjusted(SEARCH_HELP_BG, percent, 0x28);
        SEARCH_HELP_BORDER = transparencyAdjusted(SEARCH_HELP_BORDER, percent, 0x28);
        SEARCH_HELP_SHADOW = transparencyAdjusted(SEARCH_HELP_SHADOW, percent);
        SEARCH_HELP_CHIP_BG = transparencyAdjusted(SEARCH_HELP_CHIP_BG, percent, 0x18);
        SEARCH_HELP_CHIP_BORDER = transparencyAdjusted(SEARCH_HELP_CHIP_BORDER, percent, 0x20);
        SEARCH_HELP_SECTION_LINE = transparencyAdjusted(SEARCH_HELP_SECTION_LINE, percent, 0x18);

        BORDER_LIGHT = transparencyAdjusted(BORDER_LIGHT, percent, 0x20);
        BORDER_DARK = transparencyAdjusted(BORDER_DARK, percent, 0x20);
        GRADIENT_SHADOW = transparencyAdjusted(GRADIENT_SHADOW, percent);
        CONFIG_CARD_BG = transparencyAdjusted(CONFIG_CARD_BG, percent, 0x18);
        CONFIG_SEP = transparencyAdjusted(CONFIG_SEP, percent, 0x20);

        RECIPE_BG_OVERLAY = transparencyAdjusted(RECIPE_BG_OVERLAY, percent);
        RECIPE_PANEL = transparencyAdjusted(RECIPE_PANEL, percent, 0x20);
        RECIPE_PANEL_INNER = transparencyAdjusted(RECIPE_PANEL_INNER, percent, 0x18);
        RECIPE_BORDER = transparencyAdjusted(RECIPE_BORDER, percent, 0x20);
        RECIPE_HEADER_LINE = transparencyAdjusted(RECIPE_HEADER_LINE, percent, 0x18);
        RECIPE_TAB_HOVER = transparencyAdjusted(RECIPE_TAB_HOVER, percent, 0x20);
        RECIPE_TAB_IDLE = transparencyAdjusted(RECIPE_TAB_IDLE, percent, 0x18);
        RECIPE_SLOT_BORDER = transparencyAdjusted(RECIPE_SLOT_BORDER, percent, 0x20);
        RECIPE_SLOT_BG = transparencyAdjusted(RECIPE_SLOT_BG, percent, 0x18);
        RECIPE_ARROW_ANIM = transparencyAdjusted(RECIPE_ARROW_ANIM, percent);
        RECIPE_BTN_IDLE = transparencyAdjusted(RECIPE_BTN_IDLE, percent, 0x20);
        RECIPE_BTN_HOVER = transparencyAdjusted(RECIPE_BTN_HOVER, percent, 0x28);
        RECIPE_WORKSTATION_BG = transparencyAdjusted(RECIPE_WORKSTATION_BG, percent, 0x18);
        RECIPE_COUNT_BADGE = transparencyAdjusted(RECIPE_COUNT_BADGE, percent, 0x18);
    }

    public static void sync() {
        GLOBAL_PADDING = AmiConfig.globalPadding;
        ROW_HEIGHT = AmiConfig.rowHeight;
        ICON_SIZE = AmiConfig.iconSize;
        ELEMENT_GAP = AmiConfig.elementGap;

        int accent = visibleColor(AmiConfig.accentColor);
        PANEL_BG = AmiConfig.panelBg;
        PANEL_INNER = AmiConfig.searchBarBg;
        PANEL_TEXTURE_LIGHT = 0x08FFFFFF;
        PANEL_TEXTURE_DARK = 0x12000000;
        PANEL_HEADER_BG = AmiConfig.cardBg;
        PANEL_CONTENT_BG = AmiConfig.cardBg;
        PANEL_CONTENT_BORDER = AmiConfig.searchBarBorder;

        HEADER_BG = AmiConfig.groupHeaderBg;
        HEADER_SEP = AmiConfig.searchBarBorder;
        HEADER_TEXT = visibleColor(AmiConfig.groupHeaderText);

        GROUP_BG = AmiConfig.groupHeaderBg;
        GROUP_BG_HOVER = AmiConfig.cardBgHover;
        GROUP_TEXT = visibleColor(AmiConfig.groupHeaderText);
        GROUP_HEADER_BG = AmiConfig.groupHeaderBg;
        GROUP_HEADER_TEXT = visibleColor(AmiConfig.groupHeaderText);

        SIDEBAR_BG = AmiConfig.panelBg;
        SIDEBAR_TEXT = visibleColor(AmiConfig.cardTextSubtitle);
        SIDEBAR_TEXT_ACTIVE = visibleColor(AmiConfig.cardTextName);
        SIDEBAR_SELECTION = AmiConfig.cardBgHover;

        SEARCH_BAR_BG = AmiConfig.searchBarBg;
        SEARCH_BAR_BORDER = AmiConfig.searchBarBorder;
        ENTRY_HOVER = AmiConfig.cardBgHover;
        ENTRY_TEXT = visibleColor(AmiConfig.cardTextName);
        ENTRY_SUBTITLE = visibleColor(AmiConfig.cardTextSubtitle);

        SECTION_SEP = AmiConfig.searchBarBorder;
        ROW_SEPARATOR = AmiConfig.searchBarBorder;
        TEXT_HEADER = visibleColor(AmiConfig.groupHeaderText);
        TEXT_PRIMARY = visibleColor(AmiConfig.cardTextName);
        TEXT_SUBTLE = visibleColor(AmiConfig.cardTextSubtitle);
        TEXT_HIGHLIGHT = accent;
        MOD_NAME = accent;

        SLOT_BG = AmiConfig.cardBg;
        SLOT_EDGE_LIGHT = AmiConfig.searchBarBorder;
        SLOT_EDGE_DARK = 0x66000000;
        SLOT_HOVER = AmiConfig.cardBgHover;

        DROPDOWN_BG = AmiConfig.cardBg;
        DROPDOWN_BG_ACTIVE = AmiConfig.cardBgHover;
        DROPDOWN_LIST_BG = AmiConfig.panelBg;
        CONTROL_EDGE_LIGHT = AmiConfig.searchBarBorder;
        CONTROL_EDGE_DARK = 0x66000000;
        CONTROL_SHADOW = AmiConfig.overlayBg;

        SCROLL_TRACK = AmiConfig.scrollbarBg;
        SCROLL_THUMB = AmiConfig.scrollbarThumb;
        SCROLL_THUMB_ACTIVE = AmiConfig.scrollbarThumbHover;
        SCROLL_INDICATOR_BG = AmiConfig.overlayBg;

        GRID_NO_RESULTS_TEXT = visibleColor(AmiConfig.cardTextSubtitle);
        GRID_GOLD_BORDER = accent;
        GRID_GOLD_TINT = AmiConfig.cardBgHover;
        GRID_GROUP_BAND = AmiConfig.cardBg;
        GRID_GROUP_BAND_ALT = AmiConfig.cardBgHover;
        GRID_GROUP_RAIL = AmiConfig.searchBarBorder;
        GRID_GROUP_ROOT_BG = AmiConfig.groupHeaderBg;
        GRID_GROUP_CHILD_BG = AmiConfig.cardBg;

        SEARCH_PLACEHOLDER = visibleColor(AmiConfig.searchPlaceholder);
        SEARCH_DEFAULT_TEXT = visibleColor(AmiConfig.searchText);
        SEARCH_CURSOR = visibleColor(AmiConfig.searchText);
        SEARCH_HELP_BG = AmiConfig.panelBg;
        SEARCH_HELP_BORDER = AmiConfig.searchBarBorder;
        SEARCH_HELP_SHADOW = AmiConfig.overlayBg;
        SEARCH_HELP_CHIP_BG = AmiConfig.cardBg;
        SEARCH_HELP_CHIP_BORDER = AmiConfig.searchBarBorder;
        SEARCH_HELP_SECTION_LINE = AmiConfig.searchBarBorder;
        SEARCH_HELP_TITLE = accent;
        SEARCH_HELP_TEXT = visibleColor(AmiConfig.cardTextName);

        BORDER_LIGHT = AmiConfig.searchBarBorder;
        BORDER_DARK = 0x66000000;
        GRADIENT_SHADOW = AmiConfig.overlayBg;

        CONFIG_CARD_BG = AmiConfig.cardBg;
        CONFIG_SEP = AmiConfig.searchBarBorder;

        PLAYER_NAME_COLOR = accent;
        ACCENT_BLUE = accent;

        RECIPE_BG_OVERLAY = AmiConfig.overlayBg;
        RECIPE_PANEL = AmiConfig.panelBg;
        RECIPE_PANEL_INNER = AmiConfig.cardBg;
        RECIPE_BORDER = AmiConfig.searchBarBorder;
        RECIPE_HEADER_LINE = AmiConfig.searchBarBorder;
        RECIPE_TAB_ACTIVE = accent;
        RECIPE_TAB_HOVER = AmiConfig.cardBgHover;
        RECIPE_TAB_IDLE = AmiConfig.cardBg;
        RECIPE_TAB_TEXT_A = visibleColor(AmiConfig.cardTextName);
        RECIPE_TAB_TEXT_I = visibleColor(AmiConfig.cardTextSubtitle);
        RECIPE_SLOT_BORDER = AmiConfig.searchBarBorder;
        RECIPE_SLOT_BG = AmiConfig.cardBg;
        RECIPE_ARROW = accent;
        RECIPE_ARROW_ANIM = AmiConfig.cardBgHover;
        RECIPE_TEXT_TITLE = visibleColor(AmiConfig.cardTextName);
        RECIPE_TEXT_ITEM = visibleColor(AmiConfig.cardTextName);
        RECIPE_TEXT_CAT = visibleColor(AmiConfig.cardTextSubtitle);
        RECIPE_TEXT_NAV = visibleColor(AmiConfig.cardTextSubtitle);
        RECIPE_TEXT_FOOTER = visibleColor(AmiConfig.cardActionHint);
        RECIPE_BTN_IDLE = AmiConfig.cardBg;
        RECIPE_BTN_HOVER = AmiConfig.cardBgHover;
        RECIPE_SHAPELESS = accent;
        RECIPE_WORKSTATION_BG = AmiConfig.cardBg;
        RECIPE_WORKSTATION_TEXT = visibleColor(AmiConfig.cardTextSubtitle);
        RECIPE_TAB_ARROW = visibleColor(AmiConfig.cardTextSubtitle);
        RECIPE_TAB_ARROW_HOVER = accent;
        RECIPE_COUNT_BADGE = AmiConfig.cardBg;
        RECIPE_COUNT_BADGE_TEXT = visibleColor(AmiConfig.cardTextSubtitle);
        RECIPE_OUTPUT_SLOT_BORDER = ACCENT_GOLD;


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
            SEARCH_BAR_BG = 0xFF101014;
            SEARCH_BAR_BORDER = AmiConfig.searchBarBorder;
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
            RECIPE_TAB_ACTIVE = accent;
            RECIPE_TAB_HOVER = 0x44FFFFFF;
            RECIPE_TAB_IDLE = 0x15FFFFFF;
            RECIPE_TAB_TEXT_A = 0xFFFFFFFF;
            RECIPE_TAB_TEXT_I = 0xFFAAAAAA;
            RECIPE_SLOT_BORDER = 0x44FFFFFF;
            RECIPE_SLOT_BG = 0x1AFFFFFF;
            RECIPE_ARROW = accent;
            RECIPE_ARROW_ANIM = 0x884488FF;
            RECIPE_TEXT_TITLE = 0xFFFFFFFF;
            RECIPE_TEXT_ITEM = 0xFFCCCCCC;
            RECIPE_TEXT_CAT = 0xFFAAAAAA;
            RECIPE_TEXT_NAV = 0xFFAAAAAA;
            RECIPE_TEXT_FOOTER = 0xFF777777;
            RECIPE_BTN_IDLE = 0x4444AA44;
            RECIPE_BTN_HOVER = 0x8844AA44;
            RECIPE_SHAPELESS = 0xFF6666CC;
            RECIPE_WORKSTATION_BG = 0x1AFFFFFF;
            RECIPE_WORKSTATION_TEXT = 0xFFAAAAAA;
            RECIPE_TAB_ARROW = 0xFFAAAAAA;
            RECIPE_TAB_ARROW_HOVER = accent;
            RECIPE_COUNT_BADGE = 0x33FFFFFF;
            RECIPE_COUNT_BADGE_TEXT = 0xFFAAAAAA;
            RECIPE_OUTPUT_SLOT_BORDER = 0xFFAA8833;

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
            SEARCH_BAR_BG = AmiConfig.searchBarBg;
            SEARCH_BAR_BORDER = 0x33FFFFFF;
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
            RECIPE_TAB_ACTIVE = accent;
            RECIPE_TAB_HOVER = 0x33FFFFFF;
            RECIPE_TAB_IDLE = 0;
            RECIPE_TAB_TEXT_A = 0xFFFFFFFF;
            RECIPE_TAB_TEXT_I = 0xFFCCCCCC;
            RECIPE_SLOT_BORDER = 0x22FFFFFF;
            RECIPE_SLOT_BG = 0x11FFFFFF;
            RECIPE_ARROW = 0xFFCCCCCC;
            RECIPE_ARROW_ANIM = accent;
            RECIPE_TEXT_TITLE = 0xFFFFFFFF;
            RECIPE_TEXT_ITEM = 0xFFCCCCCC;
            RECIPE_TEXT_CAT = 0xFF888888;
            RECIPE_TEXT_NAV = 0xFF888888;
            RECIPE_TEXT_FOOTER = 0xFF666666;
            RECIPE_BTN_IDLE = 0x3344AA44;
            RECIPE_BTN_HOVER = 0x6644AA44;
            RECIPE_SHAPELESS = 0xFF8888FF;
            RECIPE_WORKSTATION_BG = 0x0AFFFFFF;
            RECIPE_WORKSTATION_TEXT = 0xFF888888;
            RECIPE_TAB_ARROW = 0xFFCCCCCC;
            RECIPE_TAB_ARROW_HOVER = accent;
            RECIPE_COUNT_BADGE = 0x22FFFFFF;
            RECIPE_COUNT_BADGE_TEXT = 0xFF888888;
            RECIPE_OUTPUT_SLOT_BORDER = 0xFFAA8833;

        } else if (AmiConfig.theme == AmiConfig.Theme.VANILLA) {
            // VANILLA
            PANEL_BG = 0xFFC6C6C6;
            PANEL_INNER = 0xFFD0D0D0;
            PANEL_TEXTURE_LIGHT = 0x22FFFFFF;
            PANEL_TEXTURE_DARK = 0x18000000;
            PANEL_HEADER_BG = 0xFFC0C0C0;
            PANEL_CONTENT_BG = 0xFFD0D0D0;
            PANEL_CONTENT_BORDER = 0xFF8A8A8A;
            SLOT_BG = 0xFF8B8B8B;
            SLOT_EDGE_LIGHT = 0xFFD8D8D8;
            SLOT_EDGE_DARK = 0xFF555555;

            ACCENT_BLUE = 0xFFA02020;
            ENTRY_HOVER = 0x55A02020;
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
            SEARCH_BAR_BG = 0xFF000000;
            SEARCH_BAR_BORDER = 0xFFE0E0E0;
            SEARCH_PLACEHOLDER = 0xFFB0B0B0;
            SEARCH_DEFAULT_TEXT = 0xFFFFFFFF;
            SEARCH_CURSOR = 0xFFFFFFFF;
            SEARCH_HELP_BG = 0xFFF0F0F0;
            SEARCH_HELP_BORDER = 0xFF555555;
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

            // ── Recipe viewer: JEI-pixel-accurate palette ─────────────────

            RECIPE_BG_OVERLAY        = 0x55000000;  // subtle dim; JEI has none but we're a modal
            RECIPE_PANEL             = 0xFFC6C6C6;  // JEI panel grey
            RECIPE_PANEL_INNER       = 0xFF8B8B8B;  // inner recipe area
            RECIPE_BORDER            = 0xFF555555;  // MC inventory border charcoal
            RECIPE_HEADER_LINE       = 0xFF373737;  // separator dark line
            RECIPE_TAB_ACTIVE        = 0xFFC6C6C6;  // active tab = panel colour (open bottom)
            RECIPE_TAB_HOVER         = 0xFFD6D6D6;  // slight brighten on hover
            RECIPE_TAB_IDLE          = 0xFF8B8B8B;  // inactive tab mid-grey
            RECIPE_TAB_TEXT_A        = 0xFF000000;  // black text on active tab
            RECIPE_TAB_TEXT_I        = 0xFF373737;  // dark grey on inactive
            RECIPE_SLOT_BORDER       = 0xFF373737;  // slot shadow edge
            RECIPE_SLOT_BG           = 0xFF8B8B8B;  // slot interior
            RECIPE_ARROW             = 0xFF8B8B8B;  // arrow mid-grey
            RECIPE_ARROW_ANIM        = 0xFF373737;  // animated fill darker
            RECIPE_TEXT_TITLE        = 0xFF404040;  // dark title text
            RECIPE_TEXT_ITEM         = 0xFF404040;
            RECIPE_TEXT_CAT          = 0xFF555555;
            RECIPE_TEXT_NAV          = 0xFF404040;
            RECIPE_TEXT_FOOTER       = 0xFF555555;
            RECIPE_BTN_IDLE          = 0xFF8B8B8B;
            RECIPE_BTN_HOVER         = 0xFFB8B8B8;  // vanilla hover lightens, not darkens
            RECIPE_SHAPELESS         = 0xFF373737;
            RECIPE_WORKSTATION_BG    = 0xFFB5B5B5;
            RECIPE_WORKSTATION_TEXT  = 0xFF373737;
            RECIPE_TAB_ARROW         = 0xFF373737;
            RECIPE_TAB_ARROW_HOVER   = 0xFF000000;
            RECIPE_COUNT_BADGE       = 0xFF373737;
            RECIPE_COUNT_BADGE_TEXT  = 0xFFC6C6C6;
            RECIPE_OUTPUT_SLOT_BORDER = 0xFF373737; // sprites handle the visual; this is fill fallback
        }
        ThemeResourceLoader.applyCurrentTheme();
        applyThemeTransparency();
    }

    public static void load() {
    }
}
