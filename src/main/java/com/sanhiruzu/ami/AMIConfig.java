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

    public static final ModConfigSpec SPEC = BUILDER.build();
}
