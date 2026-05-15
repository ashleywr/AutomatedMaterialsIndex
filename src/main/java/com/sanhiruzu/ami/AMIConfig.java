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

    static {
        BUILDER.pop();
    }

    // -------------------------------------------------------------------------
    // Overlay panel
    // -------------------------------------------------------------------------

    /** Which side of the inventory the AMI panel appears on. */
    public enum PanelSide {
        /** Use the left side when EMI/JEI are present, right side otherwise. */
        AUTO,
        LEFT,
        RIGHT
    }

    static {
        BUILDER.push("overlay");
    }

    public static final ModConfigSpec.EnumValue<PanelSide> PANEL_SIDE = BUILDER
            .comment("Which side of the inventory to render the AMI panel on.",
                     "AUTO = left when EMI/JEI present, right otherwise.")
            .defineEnum("side", PanelSide.AUTO);

    public static final ModConfigSpec.IntValue PANEL_WIDTH_OVERRIDE = BUILDER
            .comment("Override the AMI panel width in GUI pixels.",
                     "0 = auto-calculate from available space.")
            .defineInRange("widthOverride", 0, 0, 400);

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
            .defineInRange("overlayBg", 0x99000000, 0, 0xFFFFFFFF);

    // Search bar
    public static final ModConfigSpec.IntValue PALETTE_SEARCH_BAR_BG = BUILDER
            .comment("Search bar background color (RGB hex).")
            .defineInRange("searchBarBg", 0xFF1A1A1A, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_SEARCH_BAR_BORDER = BUILDER
            .comment("Search bar border color (RGB hex).")
            .defineInRange("searchBarBorder", 0xFF4488FF, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_SEARCH_TEXT = BUILDER
            .comment("Search bar text color (RGB hex).")
            .defineInRange("searchText", 0xFFEEEEEE, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_SEARCH_PLACEHOLDER = BUILDER
            .comment("Search bar placeholder text color (RGB hex).")
            .defineInRange("searchPlaceholder", 0xFF888888, 0, 0xFFFFFFFF);

    // Result cards
    public static final ModConfigSpec.IntValue PALETTE_CARD_BG = BUILDER
            .comment("Result card background color (RGB hex).")
            .defineInRange("cardBg", 0xFF2A2A2A, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_CARD_BG_HOVER = BUILDER
            .comment("Result card background color when hovered (RGB hex).")
            .defineInRange("cardBgHover", 0xFF3A3A3A, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_CARD_TEXT_NAME = BUILDER
            .comment("Result card item name text color (RGB hex).")
            .defineInRange("cardTextName", 0xFFEEEEEE, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_CARD_TEXT_SUBTITLE = BUILDER
            .comment("Result card subtitle text color (RGB hex).")
            .defineInRange("cardTextSubtitle", 0xFF888888, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_CARD_ACTION_HINT = BUILDER
            .comment("Result card action hint color (RGB hex).")
            .defineInRange("cardActionHint", 0xFF555555, 0, 0xFFFFFFFF);

    // Group headers
    public static final ModConfigSpec.IntValue PALETTE_GROUP_HEADER_BG = BUILDER
            .comment("Group header background color (RGB hex).")
            .defineInRange("groupHeaderBg", 0xFF1A1A1A, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_GROUP_HEADER_TEXT = BUILDER
            .comment("Group header text color (RGB hex).")
            .defineInRange("groupHeaderText", 0xFFAAAA00, 0, 0xFFFFFFFF);

    // Scrollbar
    public static final ModConfigSpec.IntValue PALETTE_SCROLLBAR_BG = BUILDER
            .comment("Scrollbar background color (RGB hex).")
            .defineInRange("scrollbarBg", 0xFF1A1A1A, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_SCROLLBAR_THUMB = BUILDER
            .comment("Scrollbar thumb color (RGB hex).")
            .defineInRange("scrollbarThumb", 0xFF555555, 0, 0xFFFFFFFF);

    public static final ModConfigSpec.IntValue PALETTE_SCROLLBAR_THUMB_HOVER = BUILDER
            .comment("Scrollbar thumb color when hovered (RGB hex).")
            .defineInRange("scrollbarThumbHover", 0xFF888888, 0, 0xFFFFFFFF);

    // Search behavior
    public static final ModConfigSpec.BooleanValue SUPPRESS_RECIPE_VIEWERS = BUILDER
            .comment("Suppress EMI/JEI rendering when AMI search is focused")
            .define("suppressRecipeViewers", true);

    static {
        BUILDER.pop();
    }

    // -------------------------------------------------------------------------

    public static final ModConfigSpec SPEC = BUILDER.build();
}
