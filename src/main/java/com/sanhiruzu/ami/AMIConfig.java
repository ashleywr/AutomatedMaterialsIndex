package com.sanhiruzu.ami;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AMIConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // -------------------------------------------------------------------------
    // Features
    // -------------------------------------------------------------------------
    static {
        BUILDER.push("features");
    }

    public static final ModConfigSpec.BooleanValue ENABLE_AUTO_INDEXING = BUILDER
            .comment("Automatically index items and world data on world load")
            .define("enableAutoIndexing", true);

    public static final ModConfigSpec.BooleanValue RENDER_OVERLAY = BUILDER
            .comment("Show AMI overlay on inventory screens (toggle with I key).",
                     "When disabled, AMI is completely hidden until activated.")
            .define("renderOverlay", false);

    public static final ModConfigSpec.BooleanValue ENABLE_MATERIAL_ROOT_UI = BUILDER
            .comment("Enable the Material Root UI (collapse blockstate variants into one node)")
            .define("enableMaterialRootUI", true);

    public static final ModConfigSpec.BooleanValue ENABLE_GHOST_CRAFTING = BUILDER
            .comment("Enable Ghost Crafting with the Architect's Gauntlet")
            .define("enableGhostCrafting", true);

    public static final ModConfigSpec.BooleanValue ENABLE_PROGRESSION_GRAPH = BUILDER
            .comment("Enable the directed progression graph")
            .define("enableProgressionGraph", true);

    public static final ModConfigSpec.BooleanValue CHEAT_MODE = BUILDER
            .comment("Enable cheat-mode features: give items, locate biomes and structures.",
                     "In singleplayer (and as LAN host) this works immediately.",
                     "On a dedicated server the player must have permission level 2 (OP).")
            .define("cheatMode", false);

    public static final ModConfigSpec.BooleanValue HIDE_NON_CREATIVE_ITEMS = BUILDER
            .comment("Filter items that do not appear in any creative-mode tab.",
                     "Removes mod placeholders, internal system items, and developer leftovers.",
                     "Disable only if you need to see every registered item regardless of tab status.")
            .define("hideNonCreativeItems", true);

    public static final ModConfigSpec.BooleanValue STRICT_SURVIVAL_MODE = BUILDER
            .comment("Hide items that have no crafting, smelting, or other recipe output.",
                     "WARNING: naturally spawning items without recipes (ores, mob drops) are also affected.",
                     "Intended for progression-focused servers. Disabled by default.")
            .define("strictSurvivalMode", false);

    static {
        BUILDER.pop();
    }

    // -------------------------------------------------------------------------
    // Command Palette Colors (all configurable for in-mod menu customization)
    // -------------------------------------------------------------------------

    static {
        BUILDER.push("palette");
    }

    // Overlay & background
    public static final ModConfigSpec.IntValue PALETTE_OVERLAY_BG = BUILDER
            .comment("Overlay background color (ARGB hex). Alpha in high byte.",
                     "0x99000000 = 60% transparent black.")
            .defineInRange("overlayBg", 0x99000000, Integer.MIN_VALUE, Integer.MAX_VALUE);

    // Search bar
    public static final ModConfigSpec.IntValue PALETTE_SEARCH_BAR_BG = BUILDER
            .comment("Search bar background color (RGB hex).")
            .defineInRange("searchBarBg", 0xFF1A1A1A, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PALETTE_SEARCH_BAR_BORDER = BUILDER
            .comment("Search bar border color (RGB hex).")
            .defineInRange("searchBarBorder", 0xFF4488FF, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PALETTE_SEARCH_TEXT = BUILDER
            .comment("Search bar text color (RGB hex).")
            .defineInRange("searchText", 0xFFEEEEEE, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PALETTE_SEARCH_PLACEHOLDER = BUILDER
            .comment("Search bar placeholder text color (RGB hex).")
            .defineInRange("searchPlaceholder", 0xFF888888, Integer.MIN_VALUE, Integer.MAX_VALUE);
// Result cards
public static final ModConfigSpec.IntValue PALETTE_CARD_BG = BUILDER
        .comment("Result card background color (RGB hex).")
        .defineInRange("cardBg", 0xFF2A2A2A, Integer.MIN_VALUE, Integer.MAX_VALUE);

public static final ModConfigSpec.IntValue PALETTE_CARD_BG_HOVER = BUILDER
        .comment("Result card background color when hovered (RGB hex).")
        .defineInRange("cardBgHover", 0x4DFFB7C5, Integer.MIN_VALUE, Integer.MAX_VALUE);

public static final ModConfigSpec.IntValue PALETTE_CARD_TEXT_NAME = BUILDER
        .comment("Result card item name text color (RGB hex).")
        .defineInRange("cardTextName", 0xFFDDDDDD, Integer.MIN_VALUE, Integer.MAX_VALUE);

public static final ModConfigSpec.IntValue PALETTE_CARD_TEXT_SUBTITLE = BUILDER
        .comment("Result card subtitle text color (RGB hex).")
        .defineInRange("cardTextSubtitle", 0xFF888888, Integer.MIN_VALUE, Integer.MAX_VALUE);

// Layout (CSS-like)
static {
    BUILDER.push("layout");
}

public static final ModConfigSpec.IntValue GLOBAL_PADDING = BUILDER
        .comment("Global internal padding for the results panel (px).")
        .defineInRange("padding", 6, 0, 20);

public static final ModConfigSpec.IntValue ROW_HEIGHT = BUILDER
        .comment("Height of each item row in the list view (px).")
        .defineInRange("rowHeight", 18, 14, 48);

public static final ModConfigSpec.IntValue ICON_SIZE = BUILDER
        .comment("Size of the item icon (px).")
        .defineInRange("iconSize", 16, 8, 32);

public static final ModConfigSpec.IntValue ELEMENT_GAP = BUILDER
        .comment("Vertical gap between UI elements (px).")
        .defineInRange("elementGap", 4, 0, 10);

static {
    BUILDER.pop();
}

// Search behavior
    public static final ModConfigSpec.IntValue PALETTE_CARD_ACTION_HINT = BUILDER
            .comment("Result card action hint color (RGB hex).")
            .defineInRange("cardActionHint", 0xFF555555, Integer.MIN_VALUE, Integer.MAX_VALUE);

    // Group headers
    public static final ModConfigSpec.IntValue PALETTE_GROUP_HEADER_BG = BUILDER
            .comment("Group header background color (RGB hex).")
            .defineInRange("groupHeaderBg", 0xFF1E1E2A, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PALETTE_GROUP_HEADER_TEXT = BUILDER
            .comment("Group header text color (RGB hex).")
            .defineInRange("groupHeaderText", 0xFFAAAA00, Integer.MIN_VALUE, Integer.MAX_VALUE);

    // Scrollbar
    public static final ModConfigSpec.IntValue PALETTE_SCROLLBAR_BG = BUILDER
            .comment("Scrollbar background color (RGB hex).")
            .defineInRange("scrollbarBg", 0xFF1A1A1A, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PALETTE_SCROLLBAR_THUMB = BUILDER
            .comment("Scrollbar thumb color (RGB hex).")
            .defineInRange("scrollbarThumb", 0xFF555555, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PALETTE_SCROLLBAR_THUMB_HOVER = BUILDER
            .comment("Scrollbar thumb color when hovered (RGB hex).")
            .defineInRange("scrollbarThumbHover", 0xFF888888, Integer.MIN_VALUE, Integer.MAX_VALUE);

    // Search behavior
    public static final ModConfigSpec.BooleanValue SUPPRESS_RECIPE_VIEWERS = BUILDER
            .comment("Suppress EMI/JEI rendering when AMI search is focused")
            .define("suppressRecipeViewers", true);

    public enum ItemClickAction { RECIPES, USES, NONE }

    public static final ModConfigSpec.EnumValue<ItemClickAction> ITEM_CLICK_ACTION = BUILDER
            .comment("What left-clicking an item in the AMI grid does.",
                     "RECIPES = open recipe viewer for crafting recipes (default),",
                     "USES = open recipe viewer for item uses,",
                     "NONE = do nothing. Right-click always opens uses.")
            .defineEnum("itemClickAction", ItemClickAction.RECIPES);

    static {
        BUILDER.pop();
    }

    // -------------------------------------------------------------------------
    // Row field configuration
    // -------------------------------------------------------------------------

    static {
        BUILDER.push("rowfields");
    }

    public static final ModConfigSpec.ConfigValue<String> SUBTITLE_FIELDS = BUILDER
            .comment("Comma-separated subtitle fields shown on list-row line 2.",
                     "Valid values: MOD_NAME, STORAGE_CAPACITY, DPS",
                     "Empty string hides the subtitle line entirely.")
            .define("subtitleFields", "MOD_NAME");

    public static final ModConfigSpec.IntValue SUBTITLE_FIELDS_CHECKSUM = BUILDER
            .comment("Checksum of the mod list when subtitle fields were last configured.",
                     "Changes automatically on mod-list change to reset fields to defaults.")
            .defineInRange("subtitleFieldsChecksum", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    // -------------------------------------------------------------------------

    public static final ModConfigSpec SPEC = BUILDER.build();
}
