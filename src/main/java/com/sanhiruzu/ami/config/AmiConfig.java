package com.sanhiruzu.ami.config;

import net.minecraft.network.chat.Component;

/**
 * Central configuration storage for AMI, using the custom annotation system.
 * Values here are synced with the custom GUI and localized tooltips.
 */
public class AmiConfig {

    public enum AmiMode {
        FULL, COMPACT, AMI_EMI_BRIDGE
    }

    public enum PanelContent {
        NONE, EMPTY, FAVORITES, GRID, LIST, COMPACT, LOOKUP_HISTORY, CRAFTING_HISTORY, CRAFTABLE
    }

    public enum Theme {
        TRANSPARENT, VANILLA, MODERN
    }

    public enum BlockSubgroup {
        SHAPE("ami.configuration.ui.block_subgroup.shape"),
        MATERIAL("ami.configuration.ui.block_subgroup.material");

        public final Component displayName;
        BlockSubgroup(String key) { this.displayName = Component.translatable(key); }
    }

    public enum ItemClickAction {
        RECIPES("ami.configuration.ui.item_click_action.recipes"),
        USES("ami.configuration.ui.item_click_action.uses"),
        NONE("ami.configuration.ui.item_click_action.none");

        public final Component displayName;
        ItemClickAction(String key) { this.displayName = Component.translatable(key); }
    }

    // --- General Group ---
    @ConfigGroup("general")
    @ConfigValue("general.mode")
    public static AmiMode mode = AmiMode.FULL;

    @ConfigValue("general.enable-auto-indexing")
    public static boolean enableAutoIndexing = true;

    @ConfigValue("general.cheat-mode")
    public static boolean cheatMode = false;

    @ConfigValue("general.dev-mode")
    public static boolean devMode = false;

    @ConfigValue("general.compact-mode")
    public static boolean compactMode = false;

    @ConfigGroupEnd
    public static final Object generalGroupEnd = null;

    // --- Side Panels Group ---
    @ConfigGroup("sidepanels")
    
    @ConfigValue("sidepanels.left.content")
    public static PanelContent leftPanelContent = PanelContent.FAVORITES;

    @ConfigValue("sidepanels.left.secondary-content")
    public static PanelContent leftPanelSecondaryContent = PanelContent.NONE;

    @ConfigValue("sidepanels.right.content")
    public static PanelContent rightPanelContent = PanelContent.GRID;

    @ConfigValue("sidepanels.right.secondary-content")
    public static PanelContent rightPanelSecondaryContent = PanelContent.NONE;

    @ConfigValue("sidepanels.left.width")
    public static int leftPanelWidth = 140;

    @ConfigValue("sidepanels.right.width")
    public static int rightPanelWidth = 0; // 0 = Auto

    @ConfigGroupEnd
    public static final Object sidepanelsGroupEnd = null;

    // --- Features Group ---
    @ConfigGroup("features")

    @ConfigValue("features.enable-material-root-ui")
    public static boolean enableMaterialRootUI = true;

    @ConfigValue("features.enable-ghost-crafting")
    public static boolean enableGhostCrafting = true;

    @ConfigValue("features.enable-progression-graph")
    public static boolean enableProgressionGraph = true;

    @ConfigValue("features.show-spawn-eggs")
    public static boolean showSpawnEggs = false;

    @ConfigValue("features.hide-non-creative-items")
    public static boolean hideNonCreativeItems = true;

    @ConfigValue("features.strict-survival-mode")
    public static boolean strictSurvivalMode = false;

    @ConfigValue("features.suppress-recipe-viewers")
    public static boolean suppressRecipeViewers = true;

    @ConfigGroupEnd
    public static final Object featuresGroupEnd = null;

    // --- Layout Group ---
    @ConfigGroup("layout")
    @ConfigValue("layout.search-bar-width")
    public static int searchBarWidth = 240;

    @ConfigValue("layout.global-padding")
    public static int globalPadding = 6;

    @ConfigValue("layout.row-height")
    public static int rowHeight = 18;

    @ConfigValue("layout.icon-size")
    public static int iconSize = 16;

    @ConfigValue("layout.element-gap")
    public static int elementGap = 4;

    @ConfigGroupEnd
    public static final Object layoutGroupEnd = null;

    // --- UI & Palette ---
    @ConfigGroup("ui")
    
    @ConfigValue("ui.theme")
    public static Theme theme = Theme.MODERN;

    @ConfigValue("ui.use-transparent-theme")
    public static boolean useTransparentTheme = true;

    @ConfigColor
    @ConfigValue("ui.accent-color")
    public static int accentColor = 0xFF5555;

    @ConfigValue("ui.show-header")
    public static boolean showHeader = true;

    @ConfigValue("ui.block-subgroup")
    public static BlockSubgroup blockSubgroup = BlockSubgroup.SHAPE;

    @ConfigValue("ui.item-click-action")
    public static ItemClickAction itemClickAction = ItemClickAction.RECIPES;

    @ConfigGroupEnd
    public static final Object uiGroupEnd = null;

    // --- Palette Group ---
    @ConfigGroup("palette")
    @ConfigColor
    @ConfigValue("palette.overlay-bg")
    public static int overlayBg = 0x66000000;

    @ConfigColor
    @ConfigValue("palette.panel-bg")
    public static int panelBg = 0x66000000;

    @ConfigColor
    @ConfigValue("palette.search-bar-bg")
    public static int searchBarBg = 0x33000000;

    @ConfigColor
    @ConfigValue("palette.search-bar-border")
    public static int searchBarBorder = 0x884488FF;

    @ConfigColor
    @ConfigValue("palette.search-text")
    public static int searchText = 0xFFFFFFFF;

    @ConfigColor
    @ConfigValue("palette.search-placeholder")
    public static int searchPlaceholder = 0xFFAAAAAA;

    @ConfigColor
    @ConfigValue("palette.card-bg")
    public static int cardBg = 0x22FFFFFF;

    @ConfigColor
    @ConfigValue("palette.card-bg-hover")
    public static int cardBgHover = 0x44FFFFFF;

    @ConfigColor
    @ConfigValue("palette.card-text-name")
    public static int cardTextName = 0xFFFFFFFF;

    @ConfigColor
    @ConfigValue("palette.card-text-subtitle")
    public static int cardTextSubtitle = 0xFFAAAAAA;

    @ConfigGroupEnd
    public static final Object paletteGroupEnd = null;

    // --- Subtitles ---
    public static String subtitleFields = "MOD_NAME";
    public static int subtitleFieldsChecksum = 0;

    // --- Binds Group ---
    @ConfigGroup("binds")
    @ConfigValue("binds.favorite")
    public static String favoriteKey = "A";

    @ConfigValue("binds.toggle-ami")
    public static String toggleAmiKey = "ALT+A";

    @ConfigValue("binds.cheat-one")
    public static String cheatOneKey = "ctrl-click";

    @ConfigGroupEnd
    public static final Object bindsGroupEnd = null;

    /**
     * Resets all configuration fields to their hardcoded default values.
     */
    public static void resetToDefaults() {
        mode = AmiMode.FULL;
        enableAutoIndexing = true;
        cheatMode = false;
        devMode = false;
        compactMode = false;
        
        leftPanelContent = PanelContent.FAVORITES;
        rightPanelContent = PanelContent.GRID;
        leftPanelWidth = 140;
        rightPanelWidth = 0;
        
        enableMaterialRootUI = true;
        enableGhostCrafting = true;
        enableProgressionGraph = true;
        showSpawnEggs = false;
        hideNonCreativeItems = true;
        strictSurvivalMode = false;
        suppressRecipeViewers = true;
        
        searchBarWidth = 240;
        globalPadding = 6;
        rowHeight = 18;
        iconSize = 16;
        elementGap = 4;
        
        theme = Theme.MODERN;
        useTransparentTheme = true;
        accentColor = 0xFF5555;
        showHeader = true;
        blockSubgroup = BlockSubgroup.SHAPE;
        itemClickAction = ItemClickAction.RECIPES;
        
        overlayBg = 0x66000000;
        panelBg = 0x66000000;
        searchBarBg = 0x33000000;
        searchBarBorder = 0x884488FF;
        searchText = 0xFFFFFFFF;
        searchPlaceholder = 0xFFAAAAAA;
        cardBg = 0x22FFFFFF;
        cardBgHover = 0x44FFFFFF;
        cardTextName = 0xFFFFFFFF;
        cardTextSubtitle = 0xFFAAAAAA;
        
        subtitleFields = "MOD_NAME";
        subtitleFieldsChecksum = 0;
        
        favoriteKey = "A";
        toggleAmiKey = "ALT+A";
        cheatOneKey = "ctrl-click";
    }
}
