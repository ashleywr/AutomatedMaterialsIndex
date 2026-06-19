package com.sanhiruzu.ami.config;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Central configuration storage for AMI, using the custom annotation system.
 * Values here are synced with the custom GUI and localized tooltips.
 */
public class AmiConfig {
    /**
     * Keeps a stored config value out of the in-game config screen.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigHidden {
    }

    @ConfigGroupEnd
    public static final Object generalGroupEnd = null;
    @ConfigGroupEnd
    public static final Object displayGroupEnd = null;
    @ConfigGroupEnd
    public static final Object searchGroupEnd = null;
    @ConfigGroupEnd
    public static final Object interactionGroupEnd = null;
    @ConfigGroupEnd
    public static final Object sidepanelsGroupEnd = null;
    @ConfigGroupEnd
    public static final Object compatGroupEnd = null;
    @ConfigGroupEnd
    public static final Object cheatGroupEnd = null;
    @ConfigGroupEnd
    public static final Object layoutGroupEnd = null;
    @ConfigGroupEnd
    public static final Object paletteGroupEnd = null;
    // --- General Group ---
    @ConfigGroup(value = "general", icon = "general", order = 0)
    @ConfigValue("general.mode")
    public static AmiMode mode = AmiMode.FULL;
    @ConfigValue("general.enable-auto-indexing")
    public static boolean enableAutoIndexing = true;
    @ConfigValue("general.start-hidden")
    public static boolean startHidden = false;
    @ConfigValue("general.recipe-book-action")
    public static RecipeBookAction recipeBookAction = RecipeBookAction.TOGGLE_EXTERNAL_VIEWER;
    @ConfigValue("features.show-creative-items")
    public static boolean showCreativeItems = false;
    @ConfigValue("features.show-hidden-mod-items")
    public static boolean showHiddenModItems = false;
    @ConfigValue("features.strict-survival-mode")
    public static boolean strictSurvivalMode = false;
    @ConfigValue("features.enable-material-root-ui")
    public static boolean enableMaterialRootUI = true;
    @ConfigHidden
    @ConfigValue("features.enable-progression-graph")
    public static boolean enableProgressionGraph = true;
    @ConfigValue("features.enable-discovery-checklist")
    public static boolean enableDiscoveryChecklist = false;
    @ConfigValue("ui.block-subgroup")
    public static BlockSubgroup blockSubgroup = BlockSubgroup.SHAPE;
    @ConfigHidden
    @ConfigValue("features.dynamic-shape-min-count")
    public static int dynamicShapeMinCount = 40;
    @ConfigHidden
    @ConfigValue("features.dynamic-shape-min-mod-spread")
    public static int dynamicShapeMinModSpread = 3;
    // --- Theme & Appearance ---
    @ConfigGroup(value = "display", icon = "display", order = 1)
    @ConfigValue("ui.theme")
    public static Theme theme = Theme.MODERN;
    @ConfigSlider(min = 0, max = 100, step = 1)
    @ConfigValue("ui.theme-transparency")
    public static int themeTransparency = 0;
    @ConfigColor
    @ConfigValue("ui.accent-color")
    public static int accentColor = 0xFF5555;
    @ConfigValue("ui.show-header")
    public static boolean showHeader = true;
    @ConfigValue("ui.results-expanded-by-default")
    public static boolean resultsExpandedByDefault = true;
    @ConfigValue("ui.show-tooltip-tags")
    public static boolean showTooltipTags = false;
    @ConfigValue("general.compact-mode")
    public static boolean compactMode = false;
    @ConfigValue("general.disable-auto-compact")
    public static boolean disableAutoCompact = false;
    @ConfigHidden
    @ConfigValue("subtitles.fields")
    public static String subtitleFields = "MOD_NAME";
    public static int subtitleFieldsChecksum = 0;
    // --- Search Sources ---
    @ConfigGroup(value = "search", icon = "general", order = 2)
    @ConfigValue("search.include-guides")
    public static boolean searchIncludeGuides = true;
    @ConfigValue("search.include-quests")
    public static boolean searchIncludeQuests = true;
    @ConfigValue("search.include-advancements")
    public static boolean searchIncludeAdvancements = true;
    @ConfigValue("search.include-entities")
    public static boolean searchIncludeEntities = true;
    @ConfigValue("search.include-players")
    public static boolean searchIncludePlayers = true;
    @ConfigValue("search.include-waypoints")
    public static boolean searchIncludeWaypoints = true;
    @ConfigValue("search.include-enchantments")
    public static boolean searchIncludeEnchantments = true;
    @ConfigValue("search.include-mob-effects")
    public static boolean searchIncludeMobEffects = true;
    @ConfigValue("search.include-tags")
    public static boolean searchIncludeTags = true;
    @ConfigValue("search.include-paintings")
    public static boolean searchIncludePaintings = true;
    @ConfigValue("search.include-game-rules")
    public static boolean searchIncludeGameRules = false;

    // --- Interaction ---
    @ConfigGroup(value = "interaction", icon = "interaction", order = 3)
    @ConfigValue("ui.item-click-action")
    public static ItemClickAction itemClickAction = ItemClickAction.RECIPES;

    @ConfigValue("features.recipe-viewer-mode")
    public static RecipeViewerMode recipeViewerMode = RecipeViewerMode.AUTO;

    @ConfigHidden
    @ConfigValue("features.enable-ghost-crafting")
    public static boolean enableGhostCrafting = true;

    @ConfigValue("features.guide-indexing-mode")
    public static GuideIndexingMode guideIndexingMode = GuideIndexingMode.SUMMARY;

    @ConfigValue("features.pack-author-mode")
    public static boolean packAuthorMode = false;

    @ConfigValue("ui.confirm-external-links")
    public static boolean confirmExternalLinks = true;

    @ConfigHidden
    @ConfigValue("features.guide-summary-text-cap")
    public static int guideSummaryTextCap = 4096;

    @ConfigHidden
    @ConfigValue("features.default-waypoint-map-handler")
    public static String defaultWaypointMapHandler = "auto";

    @ConfigHidden
    @ConfigValue("waypoints.merge-duplicate-providers")
    public static boolean waypointMergeDuplicateProviders = true;

    @ConfigHidden
    @ConfigSlider(min = 1, max = 30, step = 1)
    @ConfigValue("waypoints.refresh-interval-seconds")
    public static int waypointRefreshIntervalSeconds = 5;

    @ConfigHidden
    @ConfigValue("waypoints.open-provider-priority")
    public static String waypointOpenProviderPriority = "ftbchunks,journeymap,xaero,waystones,manual";

    @ConfigHidden
    @ConfigValue("waypoints.tooltip-provider-details")
    public static String waypointTooltipProviderDetailsMode = "shift";

    @ConfigHidden
    @ConfigValue("ui.context-menu.enabled-actions")
    public static String contextMenuEnabledActions = "ami:copy_tooltip,ami:craft_one,ami:craft_stack,ami:recipes,ami:uses,ami:favorite,ami:chat,ami:wiki,ami:locate,ami:cheat_give_one,ami:cheat_give_stack,ami:cheat_spawn_egg,ami:cheat_spawn_egg_stack,ami:cheat_spawn_pokemon,ami:cheat_pokemon_party,ami:open_pokedex,ami:filter_pokemon_type,ami:filter_pokemon_secondary_type,ami:filter_pokemon_generation,ami:filter_pokemon_egg_group,ami:filter_pokemon_ability,ami:search_pokemon_drop_item,ami:recipes_pokemon_drop_item,ami:copy_pokemon_species,ami:copy_pokemon_dex_number,ami:filter_gregtech_tier,ami:filter_gregtech_kind,ami:filter_gregtech_fact,ami:filter_gregtech_circuit_grade,ami:copy_player_name,ami:cheat_give_player_head,ami:copy_player_waypoint,ami:teleport_to_player,ami:teleport_player_here,ami:group_toggle,ami:filter_category,ami:copy_group_key,ami:start_category_fix,ami:apply_category_fix,ami:clear_item_fix,ami:quests_for_item,ami:open_quest,ami:copy_quest_matches";

    @ConfigHidden
    @ConfigValue("ui.context-menu.disabled-by-mod")
    public static String contextMenuDisabledByMod = "";

    @ConfigHidden
    @ConfigValue("ui.context-menu.disabled-by-type")
    public static String contextMenuDisabledByType = "";

    @ConfigHidden
    @ConfigValue("ui.context-menu.disabled-by-category")
    public static String contextMenuDisabledByCategory = "";
    // --- Compat Category Policy ---
    @ConfigGroup(value = "compat", icon = "compat", order = 9)
    @ConfigValue("compat.cobblemon.category-policy")
    public static CompatCategoryPolicy cobblemonCategoryPolicy = CompatCategoryPolicy.FOCUSED;
    @ConfigValue("compat.cobblemon.use-resource-pack-sprites")
    public static boolean useCobblemonResourcePackSprites = true;
    @ConfigValue("compat.cobblemon.render-3d-pokemon-icons")
    public static boolean renderCobblemon3dPokemonIcons = false;
    @ConfigValue("compat.create.category-policy")
    public static CompatCategoryPolicy createCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.ae2.category-policy")
    public static CompatCategoryPolicy ae2CategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.mekanism.category-policy")
    public static CompatCategoryPolicy mekanismCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.gregtech.category-policy")
    public static CompatCategoryPolicy gregtechCategoryPolicy = CompatCategoryPolicy.FOCUSED;
    @ConfigValue("compat.sophisticated.category-policy")
    public static CompatCategoryPolicy sophisticatedCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.modular-gear.category-policy")
    public static CompatCategoryPolicy modularGearCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.minecolonies.category-policy")
    public static CompatCategoryPolicy minecoloniesCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.apotheosis.category-policy")
    public static CompatCategoryPolicy apotheosisCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.botania.category-policy")
    public static CompatCategoryPolicy botaniaCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.ars-nouveau.category-policy")
    public static CompatCategoryPolicy arsNouveauCategoryPolicy = CompatCategoryPolicy.FOCUSED;
    @ConfigValue("compat.pastel.category-policy")
    public static CompatCategoryPolicy pastelCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.malum.category-policy")
    public static CompatCategoryPolicy malumCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.swem.category-policy")
    public static CompatCategoryPolicy swemCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.cataclysm.category-policy")
    public static CompatCategoryPolicy cataclysmCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.map-utility.category-policy")
    public static CompatCategoryPolicy mapUtilityCategoryPolicy = CompatCategoryPolicy.HYBRID;
    @ConfigValue("compat.waystones.open-screen-from-anywhere")
    public static boolean waystonesOpenScreenFromAnywhere = false;
    // --- Side Panels Group ---
    @ConfigGroup(value = "sidepanels", icon = "sidepanels", order = 4)
    @ConfigValue("sidepanels.left.width")
    public static int leftPanelWidth = 0; // 0 = Auto
    @ConfigValue("sidepanels.right.width")
    public static int rightPanelWidth = 0; // 0 = Auto
    @ConfigValue("sidepanels.left.slots")
    public static String leftPanelSlots = "FAVORITES";
    @ConfigValue("sidepanels.left.alternate-slots")
    public static String leftPanelAlternateSlots = "NONE";
    @ConfigValue("sidepanels.right.slots")
    public static String rightPanelSlots = "GRID";
    @ConfigValue("sidepanels.right.alternate-slots")
    public static String rightPanelAlternateSlots = "LIST";
    // Legacy fields (kept for compatibility, but not annotated)
    public static PanelContent leftPanelContent = PanelContent.FAVORITES;
    public static PanelContent leftPanelAlternateContent = PanelContent.NONE;
    public static PanelContent leftPanelAlternateSecondaryContent = PanelContent.NONE;
    public static PanelContent leftPanelSecondaryContent = PanelContent.NONE;
    public static PanelContent rightPanelContent = PanelContent.GRID;
    public static PanelContent rightPanelAlternateContent = PanelContent.LIST;
    public static PanelContent rightPanelAlternateSecondaryContent = PanelContent.NONE;
    public static PanelContent rightPanelSecondaryContent = PanelContent.NONE;
    // --- Cheats & Developer ---
    @ConfigGroup(value = "cheat", icon = "cheat", order = 5)
    @ConfigValue("general.cheat-mode")
    public static boolean cheatMode = false;
    @ConfigValue("general.dev-mode")
    public static boolean devMode = false;
    @ConfigValue("cheat.give-mode")
    public static CheatGiveMode cheatGiveMode = CheatGiveMode.CURSOR;
    @ConfigValue("cheat.drop-to-delete")
    public static boolean cheatDropToDelete = true;
    @ConfigSlider(min = 1, max = 24, step = 1)
    @ConfigValue("cheat.player-head-suggestions-limit")
    public static int playerHeadSuggestionsLimit = 12;
    @ConfigSlider(min = 1, max = 24, step = 1)
    @ConfigValue("cheat.player-head-results-limit")
    public static int playerHeadResultsLimit = 16;
    @ConfigSlider(min = 8, max = 512, step = 1)
    @ConfigValue("cheat.live-player-results-limit")
    public static int livePlayerResultsLimit = 128;
    @ConfigValue("cheat.player-head-show-full-model")
    public static boolean playerHeadShowFullModel = false;
    @ConfigValue("general.highlight-exclusion-areas")
    public static boolean highlightExclusionAreas = false;
    // --- Layout & Sizing ---
    @ConfigGroup(value = "layout", icon = "sidepanels", order = 6)
    @ConfigValue("layout.search-bar-width")
    public static int searchBarWidth = 240;
    @ConfigValue("layout.grid-columns")
    public static int gridColumns = 0; // 0 = auto-fit to panel width
    @ConfigValue("layout.global-padding")
    public static int globalPadding = 6;
    @ConfigValue("layout.row-height")
    public static int rowHeight = 18;
    @ConfigValue("layout.icon-size")
    public static int iconSize = 16;
    @ConfigValue("layout.element-gap")
    public static int elementGap = 4;
    @ConfigValue("layout.list-scroll-rows")
    public static int listScrollRows = 10;
    // Panel position pinning (JSON map: {"search_bar": {"x": 100, "y": 200}, ...})
    @ConfigHidden
    @ConfigValue("layout.pinned-positions")
    public static String pinnedPositionsJson = "{}";
    // --- Palette Group ---
    @ConfigGroup(value = "palette", icon = "palette", order = 7)
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
    @ConfigColor
    @ConfigValue("palette.card-action-hint")
    public static int cardActionHint = 0xFF555555;
    @ConfigColor
    @ConfigValue("palette.group-header-bg")
    public static int groupHeaderBg = 0xFFB0B0B0;
    @ConfigColor
    @ConfigValue("palette.group-header-text")
    public static int groupHeaderText = 0xFF333333;
    @ConfigColor
    @ConfigValue("palette.scrollbar-bg")
    public static int scrollbarBg = 0xFFAAAAAA;
    @ConfigColor
    @ConfigValue("palette.scrollbar-thumb")
    public static int scrollbarThumb = 0xFF777777;
    @ConfigColor
    @ConfigValue("palette.scrollbar-thumb-hover")
    public static int scrollbarThumbHover = 0xFF555555;
    // --- Keybinds Group ---
    @ConfigGroup(value = "binds", icon = "keybinds", order = 8)
    @ConfigGroupEnd
    public static final Object bindsGroupEnd = null;

    /**
     * Resets all configuration fields to their hardcoded default values.
     */
    public static void resetToDefaults() {
        mode = AmiMode.FULL;
        enableAutoIndexing = true;
        startHidden = false;
        recipeBookAction = RecipeBookAction.TOGGLE_EXTERNAL_VIEWER;
        showCreativeItems = false;
        showHiddenModItems = false;
        strictSurvivalMode = false;
        enableMaterialRootUI = true;
        enableProgressionGraph = true;
        enableDiscoveryChecklist = false;
        blockSubgroup = BlockSubgroup.SHAPE;
        dynamicShapeMinCount = 40;
        dynamicShapeMinModSpread = 3;

        theme = Theme.MODERN;
        themeTransparency = 0;
        accentColor = 0xFF5555;
        showHeader = true;
        resultsExpandedByDefault = true;
        showTooltipTags = false;
        compactMode = false;
        disableAutoCompact = false;
        subtitleFields = "MOD_NAME";
        subtitleFieldsChecksum = 0;

        itemClickAction = ItemClickAction.RECIPES;
        searchIncludeGuides = true;
        searchIncludeQuests = true;
        searchIncludeAdvancements = true;
        searchIncludeEntities = true;
        searchIncludePlayers = true;
        searchIncludeWaypoints = true;
        searchIncludeEnchantments = true;
        searchIncludeMobEffects = true;
        searchIncludeTags = true;
        searchIncludePaintings = true;
        searchIncludeGameRules = false;
        recipeViewerMode = RecipeViewerMode.AUTO;
        enableGhostCrafting = true;
        guideIndexingMode = GuideIndexingMode.SUMMARY;
        packAuthorMode = false;
        confirmExternalLinks = true;
        guideSummaryTextCap = 4096;
        defaultWaypointMapHandler = "auto";
        waypointMergeDuplicateProviders = true;
        waypointRefreshIntervalSeconds = 5;
        waypointOpenProviderPriority = "ftbchunks,journeymap,xaero,waystones,manual";
        waypointTooltipProviderDetailsMode = "shift";
        contextMenuEnabledActions = "ami:copy_tooltip,ami:craft_one,ami:craft_stack,ami:recipes,ami:uses,ami:favorite,ami:chat,ami:wiki,ami:locate,ami:cheat_give_one,ami:cheat_give_stack,ami:cheat_spawn_egg,ami:cheat_spawn_egg_stack,ami:cheat_spawn_pokemon,ami:cheat_pokemon_party,ami:open_pokedex,ami:filter_pokemon_type,ami:filter_pokemon_secondary_type,ami:filter_pokemon_generation,ami:filter_pokemon_egg_group,ami:filter_pokemon_ability,ami:search_pokemon_drop_item,ami:recipes_pokemon_drop_item,ami:copy_pokemon_species,ami:copy_pokemon_dex_number,ami:filter_gregtech_tier,ami:filter_gregtech_kind,ami:filter_gregtech_fact,ami:filter_gregtech_circuit_grade,ami:copy_player_name,ami:cheat_give_player_head,ami:copy_player_waypoint,ami:teleport_to_player,ami:teleport_player_here,ami:group_toggle,ami:filter_category,ami:copy_group_key,ami:start_category_fix,ami:apply_category_fix,ami:clear_item_fix,ami:quests_for_item,ami:open_quest,ami:copy_quest_matches";
        contextMenuDisabledByMod = "";
        contextMenuDisabledByType = "";
        contextMenuDisabledByCategory = "";

        cobblemonCategoryPolicy = CompatCategoryPolicy.FOCUSED;
        useCobblemonResourcePackSprites = true;
        renderCobblemon3dPokemonIcons = false;
        createCategoryPolicy = CompatCategoryPolicy.HYBRID;
        ae2CategoryPolicy = CompatCategoryPolicy.HYBRID;
        mekanismCategoryPolicy = CompatCategoryPolicy.HYBRID;
        gregtechCategoryPolicy = CompatCategoryPolicy.FOCUSED;
        sophisticatedCategoryPolicy = CompatCategoryPolicy.HYBRID;
        modularGearCategoryPolicy = CompatCategoryPolicy.HYBRID;
        minecoloniesCategoryPolicy = CompatCategoryPolicy.HYBRID;
        apotheosisCategoryPolicy = CompatCategoryPolicy.HYBRID;
        botaniaCategoryPolicy = CompatCategoryPolicy.HYBRID;
        arsNouveauCategoryPolicy = CompatCategoryPolicy.FOCUSED;
        pastelCategoryPolicy = CompatCategoryPolicy.HYBRID;
        malumCategoryPolicy = CompatCategoryPolicy.HYBRID;
        swemCategoryPolicy = CompatCategoryPolicy.HYBRID;
        cataclysmCategoryPolicy = CompatCategoryPolicy.HYBRID;
        mapUtilityCategoryPolicy = CompatCategoryPolicy.HYBRID;
        waystonesOpenScreenFromAnywhere = false;

        leftPanelWidth = 0;
        rightPanelWidth = 0;
        leftPanelSlots = "FAVORITES";
        leftPanelAlternateSlots = "NONE";
        rightPanelSlots = "GRID";
        rightPanelAlternateSlots = "LIST";
        leftPanelContent = PanelContent.FAVORITES;
        leftPanelAlternateContent = PanelContent.NONE;
        leftPanelAlternateSecondaryContent = PanelContent.NONE;
        leftPanelSecondaryContent = PanelContent.NONE;
        rightPanelContent = PanelContent.GRID;
        rightPanelAlternateContent = PanelContent.LIST;
        rightPanelAlternateSecondaryContent = PanelContent.NONE;
        rightPanelSecondaryContent = PanelContent.NONE;

        cheatMode = false;
        devMode = false;
        cheatGiveMode = CheatGiveMode.CURSOR;
        cheatDropToDelete = true;
        playerHeadSuggestionsLimit = 12;
        playerHeadResultsLimit = 16;
        livePlayerResultsLimit = 128;
        playerHeadShowFullModel = false;
        highlightExclusionAreas = false;

        searchBarWidth = 240;
        gridColumns = 0;
        globalPadding = 6;
        rowHeight = 18;
        iconSize = 16;
        elementGap = 4;
        listScrollRows = 10;
        pinnedPositionsJson = "{}";

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
        cardActionHint = 0xFF555555;
        groupHeaderBg = 0xFFB0B0B0;
        groupHeaderText = 0xFF333333;
        scrollbarBg = 0xFFAAAAAA;
        scrollbarThumb = 0xFF777777;
        scrollbarThumbHover = 0xFF555555;

    }

    public static boolean shouldShowCreativeItems(GameType gameType) {
        if (cheatMode || devMode || showCreativeItems) {
            return true;
        }
        return gameType == null || gameType != GameType.SURVIVAL;
    }

    public static List<PanelContent> parsePanelSlots(String raw) {
        List<PanelContent> contents = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return contents;
        }

        for (String token : raw.split(",")) {
            String normalized = token.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            if (normalized.isEmpty()) continue;
            try {
                PanelContent content = PanelContent.valueOf(normalized);
                if (content != PanelContent.NONE) {
                    contents.add(content);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return contents;
    }

    public static String encodePanelSlots(List<PanelContent> contents) {
        if (contents == null || contents.isEmpty()) {
            return PanelContent.NONE.name();
        }
        StringBuilder out = new StringBuilder();
        for (PanelContent content : contents) {
            if (content == null || content == PanelContent.NONE) continue;
            if (!out.isEmpty()) out.append(',');
            out.append(content.name());
        }
        return out.isEmpty() ? PanelContent.NONE.name() : out.toString();
    }

    public enum AmiMode {
        FULL("ami.config.value.general.mode.full"),
        COMPACT("ami.config.value.general.mode.compact"),
        AMI_EMI_BRIDGE("ami.config.value.general.mode.ami_emi_bridge");

        public final Component displayName;

        AmiMode(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum PanelContent {
        NONE, EMPTY, FAVORITES, GRID, LIST, COMPACT, LOOKUP_HISTORY, CRAFTING_HISTORY, CRAFTABLE, QUESTS
    }

    public enum Theme {
        TRANSPARENT("ami.config.value.ui.theme.transparent"),
        VANILLA("ami.config.value.ui.theme.vanilla"),
        MODERN("ami.config.value.ui.theme.modern"),
        CUSTOM("ami.config.value.ui.theme.custom");

        public final Component displayName;

        Theme(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum BlockSubgroup {
        SHAPE("ami.configuration.ui.block_subgroup.shape"),
        MATERIAL("ami.configuration.ui.block_subgroup.material");

        public final Component displayName;

        BlockSubgroup(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum ItemClickAction {
        RECIPES("ami.configuration.ui.item_click_action.recipes"),
        USES("ami.configuration.ui.item_click_action.uses"),
        NONE("ami.configuration.ui.item_click_action.none");

        public final Component displayName;

        ItemClickAction(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum RecipeViewerMode {
        AUTO("ami.config.value.recipe_viewer.auto"),
        NATIVE("ami.config.value.recipe_viewer.native"),
        EMI_JEI("ami.config.value.recipe_viewer.emi_jei");

        public final Component displayName;

        RecipeViewerMode(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum GuideIndexingMode {
        OFF("ami.config.value.guide_indexing.off"),
        TITLES("ami.config.value.guide_indexing.titles"),
        SUMMARY("ami.config.value.guide_indexing.summary");

        public final Component displayName;

        GuideIndexingMode(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum CompatCategoryPolicy {
        FOCUSED("ami.config.value.compat.category_policy.focused"),
        SEMANTIC("ami.config.value.compat.category_policy.semantic"),
        HYBRID("ami.config.value.compat.category_policy.hybrid");

        public final Component displayName;

        CompatCategoryPolicy(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum CheatGiveMode {
        CURSOR("ami.config.value.cheat.give-mode.cursor"),
        INVENTORY("ami.config.value.cheat.give-mode.inventory");

        public final Component displayName;

        CheatGiveMode(String key) {
            this.displayName = Component.translatable(key);
        }
    }

    public enum RecipeBookAction {
        TOGGLE_AMI("ami.config.value.general.recipe-book-action.toggle_ami"),
        OPEN_VANILLA_BOOK("ami.config.value.general.recipe-book-action.open_vanilla_book",
                "ami.config.option-tooltip.general.recipe-book-action.open_vanilla_book"),
        TOGGLE_EXTERNAL_VIEWER("ami.config.value.general.recipe-book-action.toggle_external_viewer");

        public final Component displayName;
        public final Component optionTooltip;

        RecipeBookAction(String key) {
            this.displayName = Component.translatable(key);
            this.optionTooltip = null;
        }

        RecipeBookAction(String key, String tooltipKey) {
            this.displayName = Component.translatable(key);
            this.optionTooltip = Component.translatable(tooltipKey);
        }
    }
}
