package com.sanhiruzu.ami;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AMILayoutConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // -------------------------------------------------------------------------
    // Layout anchors (config-safe subset)
    // -------------------------------------------------------------------------

    /** Preset anchor positions for widgets. Safe for config serialization. */
    public enum Anchor {
        CONTAINER_LEFT,
        CONTAINER_RIGHT,
        SCREEN_BOTTOM_LEFT,
        SCREEN_BOTTOM_CENTER,
        SCREEN_BOTTOM_RIGHT,
    }

    /** Which side of the inventory the AMI panel appears on. */
    public enum PanelSide {
        /** Use the left side when EMI/JEI are present, right side otherwise. */
        AUTO,
        LEFT,
        RIGHT
    }

    // -------------------------------------------------------------------------
    // Panel settings
    // -------------------------------------------------------------------------

    static {
        BUILDER.push("panel");
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
    // Search bar settings
    // -------------------------------------------------------------------------

    static {
        BUILDER.push("searchBar");
    }

    public static final ModConfigSpec.IntValue SEARCH_BAR_WIDTH = BUILDER
            .comment("Width of the search bar in GUI pixels.")
            .defineInRange("width", 240, 60, 400);

    public static final ModConfigSpec.EnumValue<Anchor> SEARCH_BAR_ANCHOR = BUILDER
            .comment("Anchor point for the search bar.",
                     "Valid: SCREEN_BOTTOM_CENTER, SCREEN_BOTTOM_LEFT, SCREEN_BOTTOM_RIGHT")
            .defineEnum("anchor", Anchor.SCREEN_BOTTOM_CENTER);

    static {
        BUILDER.pop();
    }

    // -------------------------------------------------------------------------
    // AMI button settings
    // -------------------------------------------------------------------------

    static {
        BUILDER.push("amiButton");
    }

    public static final ModConfigSpec.EnumValue<Anchor> AMI_BUTTON_ANCHOR = BUILDER
            .comment("Anchor point for the AMI open button.")
            .defineEnum("anchor", Anchor.SCREEN_BOTTOM_LEFT);

    static {
        BUILDER.pop();
    }

    // -------------------------------------------------------------------------

    public static final ModConfigSpec SPEC = BUILDER.build();
}
