package com.sanhiruzu.ami.client.results;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sanhiruzu.ami.client.AMICheatMode;
import com.sanhiruzu.ami.api.AmiContextMenuAction;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiGuideOpeners;
import com.sanhiruzu.ami.api.AmiItemContext;
import com.sanhiruzu.ami.api.AmiPluginRegistry;
import com.sanhiruzu.ami.api.AmiQuestItemMatch;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.api.IAmiPlugin;
import com.sanhiruzu.ami.author.PackAuthorDiagnostics;
import com.sanhiruzu.ami.client.favorites.FavoriteEntry;
import com.sanhiruzu.ami.client.screen.AmiCategoryFixScreen;
import com.sanhiruzu.ami.compat.CobblemonPokedexOpener;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.config.AmiDataFixes;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.resolvers.PlayerHeadHistory;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.player.PlayerWaypointContext;
import com.sanhiruzu.ami.player.PlayerWaypointAction;
import com.sanhiruzu.ami.player.PlayerWaypointExport;
import com.sanhiruzu.ami.player.PlayerWaypointProviders;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import com.sanhiruzu.searchableitems.api.SearchableItemAction;
import com.sanhiruzu.searchableitems.api.SearchableItemActionContext;
import com.sanhiruzu.searchableitems.api.SearchableItemActionProvider;
import com.sanhiruzu.searchableitems.api.SearchableItemActionProviders;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds result context-menu actions from explicit target contexts. Keep menu
 * policy here so future config flags, mod-specific actions, and item-type
 * actions can be added without changing the result panel input plumbing.
 */
public class ResultContextMenuActionBuilder {
    private static final Logger LOGGER = Logger.getLogger(ResultContextMenuActionBuilder.class.getName());
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String COPY_TOOLTIP = "ami:copy_tooltip";
    public static final String CRAFT_ONE = "ami:craft_one";
    public static final String CRAFT_STACK = "ami:craft_stack";
    public static final String RECIPES = "ami:recipes";
    public static final String USES = "ami:uses";
    public static final String SOURCES = "ami:sources";
    public static final String ENTITY_DETAILS = "ami:entity_details";
    public static final String FAVORITE = "ami:favorite";
    public static final String CHAT = "ami:chat";
    public static final String WIKI = "ami:wiki";
    public static final String LOCATE = "ami:locate";
    public static final String CHEAT_GIVE_ONE = "ami:cheat_give_one";
    public static final String CHEAT_GIVE_STACK = "ami:cheat_give_stack";
    public static final String CHEAT_SPAWN_EGG = "ami:cheat_spawn_egg";
    public static final String CHEAT_SPAWN_EGG_STACK = "ami:cheat_spawn_egg_stack";
    public static final String CHEAT_SPAWN_POKEMON = "ami:cheat_spawn_pokemon";
    public static final String CHEAT_POKEMON_PARTY = "ami:cheat_pokemon_party";
    public static final String OPEN_POKEDEX = "ami:open_pokedex";
    public static final String FILTER_POKEMON_TYPE = "ami:filter_pokemon_type";
    public static final String FILTER_POKEMON_SECONDARY_TYPE = "ami:filter_pokemon_secondary_type";
    public static final String FILTER_POKEMON_GENERATION = "ami:filter_pokemon_generation";
    public static final String FILTER_POKEMON_EGG_GROUP = "ami:filter_pokemon_egg_group";
    public static final String FILTER_POKEMON_ABILITY = "ami:filter_pokemon_ability";
    public static final String SEARCH_POKEMON_DROP_ITEM = "ami:search_pokemon_drop_item";
    public static final String RECIPES_POKEMON_DROP_ITEM = "ami:recipes_pokemon_drop_item";
    public static final String COPY_POKEMON_SPECIES = "ami:copy_pokemon_species";
    public static final String COPY_POKEMON_DEX_NUMBER = "ami:copy_pokemon_dex_number";
    public static final String FILTER_GREGTECH_TIER = "ami:filter_gregtech_tier";
    public static final String FILTER_GREGTECH_KIND = "ami:filter_gregtech_kind";
    public static final String FILTER_GREGTECH_FACT = "ami:filter_gregtech_fact";
    public static final String FILTER_GREGTECH_CIRCUIT_GRADE = "ami:filter_gregtech_circuit_grade";
    public static final String FILTER_MOD = "ami:filter_mod";
    public static final String COPY_ID = "ami:copy_id";
    public static final String COPY_PLAYER_NAME = "ami:copy_player_name";
    public static final String CHEAT_GIVE_PLAYER_HEAD = "ami:cheat_give_player_head";
    public static final String COPY_PLAYER_WAYPOINT = "ami:copy_player_waypoint";
    public static final String ADD_PLAYER_WAYPOINT = "ami:add_player_waypoint";
    public static final String PLAYER_WAYPOINTS_MENU = "ami:player_waypoints_menu";
    public static final String PLAYER_ADMIN_MENU = "ami:player_admin_menu";
    public static final String TELEPORT_TO_PLAYER = "ami:teleport_to_player";
    public static final String TELEPORT_PLAYER_HERE = "ami:teleport_player_here";
    public static final String OPEN_WAYPOINT = "ami:open_waypoint";
    public static final String COPY_WAYPOINT = "ami:copy_waypoint";
    public static final String CHAT_WAYPOINT = "ami:chat_waypoint";
    public static final String TELEPORT_TO_WAYPOINT = "ami:teleport_to_waypoint";
    public static final String GROUP_TOGGLE = "ami:group_toggle";
    public static final String FILTER_CATEGORY = "ami:filter_category";
    public static final String COPY_GROUP_KEY = "ami:copy_group_key";
    public static final String START_CATEGORY_FIX = "ami:start_category_fix";
    public static final String EDIT_CATEGORY_FIX = "ami:edit_category_fix";
    public static final String APPLY_CATEGORY_FIX = "ami:apply_category_fix";
    public static final String CLEAR_ITEM_FIX = "ami:clear_item_fix";
    public static final String QUESTS_FOR_ITEM = "ami:quests_for_item";
    public static final String OPEN_QUEST = "ami:open_quest";
    public static final String COPY_QUEST_MATCHES = "ami:copy_quest_matches";
    public static final String COPY_FTB_ITEM_TASK = "ami:copy_ftb_item_task";
    public static final String COPY_FTB_QUEST_SKELETON = "ami:copy_ftb_quest_skeleton";
    public static final String COPY_KUBEJS_RECIPE_STUB = "ami:copy_kubejs_recipe_stub";
    public static final String COPY_KUBEJS_RECIPE_REMOVAL_SCRIPT = "ami:copy_kubejs_recipe_removal_script";
    public static final String COPY_GAMESTAGE_CONDITION = "ami:copy_gamestage_condition";
    public static final String COPY_PACK_AUTHOR_REPORT = "ami:copy_pack_author_report";
    public static final String COPY_TAXONOMY_TEMPLATE = "ami:copy_taxonomy_template";
    public static final String CREATE_PACK_FILES = "ami:create_pack_files";
    public static final String MENU_CRAFTING = "ami:menu_crafting";
    public static final String MENU_RECIPES = "ami:menu_recipes";
    public static final String MENU_CHEAT = "ami:menu_cheat";
    public static final String MENU_FILTERS = "ami:menu_filters";
    public static final String MENU_POKEMON = "ami:menu_pokemon";
    public static final String MENU_GREGTECH = "ami:menu_gregtech";
    public static final String MENU_QUESTS = "ami:menu_quests";
    public static final String MENU_PACK_AUTHOR = "ami:menu_pack_author";
    public static final String MENU_DEVELOPER = "ami:menu_developer";
    private static final int MAX_GROUP_AUTHORING_ITEMS = 64;
    private static final int MAX_GROUP_REPORT_ITEMS = 512;
    private static final ResourceLocation AE2_GUIDE_BOOK = ResourceLocation.fromNamespaceAndPath("ae2", "guide");
    private static final ResourceLocation SILENT_GEAR_MATERIAL_BOOK = ResourceLocation.fromNamespaceAndPath("silentgear", "material_book");

    public static final Set<String> KNOWN_ACTIONS = Set.of(
            COPY_TOOLTIP, CRAFT_ONE, CRAFT_STACK, RECIPES, USES, SOURCES, ENTITY_DETAILS, FAVORITE, CHAT, WIKI, LOCATE,
            CHEAT_GIVE_ONE, CHEAT_GIVE_STACK, CHEAT_SPAWN_EGG, CHEAT_SPAWN_EGG_STACK,
            CHEAT_SPAWN_POKEMON, CHEAT_POKEMON_PARTY,
            OPEN_POKEDEX, FILTER_POKEMON_TYPE, FILTER_POKEMON_SECONDARY_TYPE,
            FILTER_POKEMON_GENERATION, FILTER_POKEMON_EGG_GROUP, FILTER_POKEMON_ABILITY,
            SEARCH_POKEMON_DROP_ITEM, RECIPES_POKEMON_DROP_ITEM,
            COPY_POKEMON_SPECIES, COPY_POKEMON_DEX_NUMBER,
            FILTER_GREGTECH_TIER, FILTER_GREGTECH_KIND, FILTER_GREGTECH_FACT, FILTER_GREGTECH_CIRCUIT_GRADE,
            FILTER_MOD, COPY_ID, COPY_PLAYER_NAME, CHEAT_GIVE_PLAYER_HEAD, COPY_PLAYER_WAYPOINT, ADD_PLAYER_WAYPOINT,
            PLAYER_WAYPOINTS_MENU, PLAYER_ADMIN_MENU,
            TELEPORT_TO_PLAYER, TELEPORT_PLAYER_HERE,
            OPEN_WAYPOINT, COPY_WAYPOINT, CHAT_WAYPOINT, TELEPORT_TO_WAYPOINT,
            GROUP_TOGGLE, FILTER_CATEGORY, COPY_GROUP_KEY,
            START_CATEGORY_FIX, EDIT_CATEGORY_FIX, APPLY_CATEGORY_FIX, CLEAR_ITEM_FIX,
            QUESTS_FOR_ITEM, OPEN_QUEST, COPY_QUEST_MATCHES,
            COPY_FTB_ITEM_TASK, COPY_FTB_QUEST_SKELETON, COPY_KUBEJS_RECIPE_STUB, COPY_KUBEJS_RECIPE_REMOVAL_SCRIPT, COPY_GAMESTAGE_CONDITION,
            COPY_PACK_AUTHOR_REPORT, COPY_TAXONOMY_TEMPLATE, CREATE_PACK_FILES,
            MENU_CRAFTING, MENU_RECIPES, MENU_CHEAT, MENU_FILTERS, MENU_POKEMON, MENU_GREGTECH, MENU_QUESTS,
            MENU_PACK_AUTHOR, MENU_DEVELOPER
    );
    public static final String DEFAULT_ACTIONS = String.join(",",
            COPY_TOOLTIP,
            CRAFT_ONE,
            CRAFT_STACK,
            RECIPES,
            USES,
            SOURCES,
            ENTITY_DETAILS,
            FAVORITE,
            CHAT,
            WIKI,
            LOCATE,
            CHEAT_GIVE_ONE,
            CHEAT_GIVE_STACK,
            CHEAT_SPAWN_EGG,
            CHEAT_SPAWN_EGG_STACK,
            CHEAT_SPAWN_POKEMON,
            CHEAT_POKEMON_PARTY,
            OPEN_POKEDEX,
            FILTER_POKEMON_TYPE,
            FILTER_POKEMON_SECONDARY_TYPE,
            FILTER_POKEMON_GENERATION,
            FILTER_POKEMON_EGG_GROUP,
            FILTER_POKEMON_ABILITY,
            SEARCH_POKEMON_DROP_ITEM,
            RECIPES_POKEMON_DROP_ITEM,
            COPY_POKEMON_SPECIES,
            COPY_POKEMON_DEX_NUMBER,
            FILTER_GREGTECH_TIER,
            FILTER_GREGTECH_KIND,
            FILTER_GREGTECH_FACT,
            FILTER_GREGTECH_CIRCUIT_GRADE,
            COPY_PLAYER_NAME,
            CHEAT_GIVE_PLAYER_HEAD,
            ADD_PLAYER_WAYPOINT,
            COPY_PLAYER_WAYPOINT,
            TELEPORT_TO_PLAYER,
            TELEPORT_PLAYER_HERE,
            OPEN_WAYPOINT,
            COPY_WAYPOINT,
            CHAT_WAYPOINT,
            TELEPORT_TO_WAYPOINT,
            GROUP_TOGGLE,
            FILTER_CATEGORY,
            COPY_GROUP_KEY,
            START_CATEGORY_FIX,
            APPLY_CATEGORY_FIX,
            CLEAR_ITEM_FIX,
            QUESTS_FOR_ITEM,
            OPEN_QUEST,
            COPY_QUEST_MATCHES
    );

    private static volatile SearchNode pendingCategoryFixNode;

    static void clearPendingCategoryFixForTests() {
        pendingCategoryFixNode = null;
    }

    static SearchNode pendingCategoryFixNodeForTests() {
        return pendingCategoryFixNode;
    }

    private final BooleanSupplier cheatEnabled;
    private final Predicate<ItemStack> craftable;
    private final Supplier<List<IAmiPlugin>> pluginSupplier;
    private final BooleanSupplier externalRecipeViewerAvailable;

    public ResultContextMenuActionBuilder() {
        this(ResultContextMenuActionBuilder::isCheatEnabled, ResultContextMenuActionBuilder::canTransferCraft,
                AmiPluginRegistry::getPlugins, RecipeViewerBridge::isAvailable);
    }

    ResultContextMenuActionBuilder(BooleanSupplier cheatEnabled) {
        this(cheatEnabled, ResultContextMenuActionBuilder::canTransferCraft,
                AmiPluginRegistry::getPlugins, RecipeViewerBridge::isAvailable);
    }

    ResultContextMenuActionBuilder(BooleanSupplier cheatEnabled, Predicate<ItemStack> craftable) {
        this(cheatEnabled, craftable, AmiPluginRegistry::getPlugins, RecipeViewerBridge::isAvailable);
    }

    ResultContextMenuActionBuilder(BooleanSupplier cheatEnabled, Predicate<ItemStack> craftable,
                                   Supplier<List<IAmiPlugin>> pluginSupplier) {
        this(cheatEnabled, craftable, pluginSupplier, RecipeViewerBridge::isAvailable);
    }

    ResultContextMenuActionBuilder(BooleanSupplier cheatEnabled, Predicate<ItemStack> craftable,
                                   Supplier<List<IAmiPlugin>> pluginSupplier,
                                   BooleanSupplier externalRecipeViewerAvailable) {
        this.cheatEnabled = cheatEnabled == null ? () -> false : cheatEnabled;
        this.craftable = craftable == null ? stack -> false : craftable;
        this.pluginSupplier = pluginSupplier == null ? List::of : pluginSupplier;
        this.externalRecipeViewerAvailable = externalRecipeViewerAvailable == null
                ? () -> false
                : externalRecipeViewerAvailable;
    }

    public List<ResultContextMenu.Action> forItem(ItemContext context) {
        List<ResultContextMenu.Action> actions = new ArrayList<>();
        if (context == null || context.node() == null) return actions;
        ResultContextMenuActionPolicy policy = ResultContextMenuActionPolicy.fromConfig();

        SearchNode node = context.node();
        if (node.type() == NodeType.PLAYER) {
            addPlayerActions(actions, policy, context, node, cheatEnabled.getAsBoolean());
            return actions;
        }
        if (node.type() == NodeType.WAYPOINT) {
            addLiveWaypointActions(actions, policy, context, node, cheatEnabled.getAsBoolean());
            return actions;
        }
        ResourceLocation id = resolvedItemId(node);
        ItemStack stack = context.stack() == null ? ItemStack.EMPTY : context.stack().copy();
        boolean hasStack = !stack.isEmpty();
        List<AmiQuestItemMatch> questMatches = AmiConfig.searchIncludeQuests && node.type() == NodeType.ITEM
                ? AmiQuestsApi.getQuestMatchesForItem(id)
                : List.of();

        if (policy.allows(node, COPY_TOOLTIP) && hasStack) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_TOOLTIP,
                    Component.translatable("ami.context.copy_tooltip"),
                    't',
                    () -> AmiClipboardHelper.copyItemTooltipToClipboard(stack)
            ));
        }

        if (hasStack && canCraft(stack)) {
            if (policy.allows(node, CRAFT_ONE)) {
                actions.add(ResultContextMenu.Action.enabled(
                        CRAFT_ONE,
                        Component.translatable("ami.context.craft_one"),
                        '1',
                        () -> craftOneLater(stack)
                ));
            }
            if (policy.allows(node, CRAFT_STACK)) {
                actions.add(ResultContextMenu.Action.enabled(
                        CRAFT_STACK,
                        Component.translatable("ami.context.craft_stack"),
                        'k',
                        () -> craftStackLater(stack)
                ));
            }
        }

        if (policy.allows(node, RECIPES) && hasStack && hasRecipes(node, stack)) {
            actions.add(ResultContextMenu.Action.enabled(
                    RECIPES,
                    Component.translatable("ami.context.recipes"),
                    'r',
                    () -> openRecipesLater(stack)
            ));
        }
        if (policy.allows(node, USES) && hasStack && hasUses(node, stack)) {
            actions.add(ResultContextMenu.Action.enabled(
                    USES,
                    Component.translatable("ami.context.uses"),
                    'u',
                    () -> openUsesLater(stack)
            ));
        }
        if (policy.allows(node, SOURCES) && node.type() == NodeType.ITEM && context.openSources() != null) {
            actions.add(ResultContextMenu.Action.enabled(
                    SOURCES,
                    Component.translatable("ami.context.sources"),
                    's',
                    () -> context.openSources().accept(node)
            ));
        }
        if (policy.allows(node, ENTITY_DETAILS) && node.type() == NodeType.ENTITY && context.openEntityDetails() != null) {
            actions.add(ResultContextMenu.Action.enabled(
                    ENTITY_DETAILS,
                    Component.translatable("ami.context.mob_info"),
                    'i',
                    () -> context.openEntityDetails().accept(node)
            ));
        }

        if (isPokemonSpecies(node)) {
            if (policy.allows(node, OPEN_POKEDEX)) {
                boolean hasPokedex = hasCobblemonPokedex();
                if (hasPokedex) {
                    actions.add(ResultContextMenu.Action.enabled(
                            OPEN_POKEDEX,
                            Component.translatable("ami.context.open_pokedex"),
                            'v',
                            () -> openCobblemonPokedex(node)
                    ));
                } else {
                    actions.add(ResultContextMenu.Action.disabled(
                            OPEN_POKEDEX,
                            Component.translatable("ami.context.open_pokedex_no_item"),
                            'v'
                    ));
                }
            }
            String primaryType = node.meta(SearchNodeKeys.POKEMON_PRIMARY_TYPE, "");
            if (policy.allows(node, FILTER_POKEMON_TYPE) && context.tokenInject() != null && !primaryType.isBlank()) {
                actions.add(ResultContextMenu.Action.enabled(
                        FILTER_POKEMON_TYPE,
                        Component.translatable("ami.context.filter_pokemon_type_named", titleCaseUnderscorePath(primaryType)),
                        'y',
                        () -> context.tokenInject().accept(primaryType)
                ));
            }
            String secondaryType = node.meta(SearchNodeKeys.POKEMON_SECONDARY_TYPE, "");
            if (policy.allows(node, FILTER_POKEMON_SECONDARY_TYPE)
                    && context.tokenInject() != null
                    && !secondaryType.isBlank()
                    && !secondaryType.equalsIgnoreCase(primaryType)) {
                actions.add(ResultContextMenu.Action.enabled(
                        FILTER_POKEMON_SECONDARY_TYPE,
                        Component.translatable("ami.context.filter_pokemon_type_named", titleCaseUnderscorePath(secondaryType)),
                        't',
                        () -> context.tokenInject().accept(secondaryType)
                ));
            }
            String generation = node.meta(SearchNodeKeys.POKEMON_GENERATION, "");
            if (policy.allows(node, FILTER_POKEMON_GENERATION) && context.tokenInject() != null && !generation.isBlank()) {
                actions.add(ResultContextMenu.Action.enabled(
                        FILTER_POKEMON_GENERATION,
                        Component.translatable("ami.context.filter_pokemon_generation_named", generation),
                        'g',
                        () -> context.tokenInject().accept("?generation:" + generation)
                ));
            }
            for (String eggGroup : splitMetadataTokens(node.meta(SearchNodeKeys.POKEMON_EGG_GROUPS, ""))) {
                if (!policy.allows(node, FILTER_POKEMON_EGG_GROUP) || context.tokenInject() == null) {
                    break;
                }
                actions.add(ResultContextMenu.Action.enabled(
                        FILTER_POKEMON_EGG_GROUP,
                        Component.translatable("ami.context.filter_pokemon_egg_group_named", titleCaseUnderscorePath(eggGroup)),
                        'e',
                        () -> context.tokenInject().accept("%egg:" + eggGroup)
                ));
            }
            for (String ability : splitMetadataTokens(node.meta(SearchNodeKeys.POKEMON_ABILITIES, ""))) {
                if (!policy.allows(node, FILTER_POKEMON_ABILITY) || context.tokenInject() == null) {
                    break;
                }
                actions.add(ResultContextMenu.Action.enabled(
                        FILTER_POKEMON_ABILITY,
                        Component.translatable("ami.context.filter_pokemon_ability_named", titleCaseUnderscorePath(ability)),
                        'a',
                        () -> context.tokenInject().accept("?ability:" + ability)
                ));
            }
            for (ResourceLocation dropItemId : pokemonDropItemIds(node)) {
                String itemName = itemDisplayName(dropItemId);
                if (policy.allows(node, SEARCH_POKEMON_DROP_ITEM) && context.tokenInject() != null) {
                    actions.add(ResultContextMenu.Action.enabled(
                            SEARCH_POKEMON_DROP_ITEM,
                            Component.translatable("ami.context.search_pokemon_drop_item_named", itemName),
                            'd',
                            () -> context.tokenInject().accept(dropSearchQuery(dropItemId))
                    ));
                }

                ItemStack dropStack = stackForItem(dropItemId);
                if (policy.allows(node, RECIPES_POKEMON_DROP_ITEM) && !dropStack.isEmpty()) {
                    actions.add(ResultContextMenu.Action.enabled(
                            RECIPES_POKEMON_DROP_ITEM,
                            Component.translatable("ami.context.recipes_pokemon_drop_item_named", itemName),
                            'r',
                            () -> openRecipesLater(dropStack)
                    ));
                }
            }
            if (policy.allows(node, COPY_POKEMON_SPECIES)) {
                ResourceLocation speciesId = pokemonSpeciesId(node);
                if (speciesId != null) {
                    actions.add(ResultContextMenu.Action.enabled(
                            COPY_POKEMON_SPECIES,
                            Component.translatable("ami.context.copy_pokemon_species"),
                            'i',
                            () -> AmiClipboardHelper.copyToClipboard(speciesId.toString())
                    ));
                }
            }
            if (policy.allows(node, COPY_POKEMON_DEX_NUMBER)) {
                String dexNumber = node.meta(SearchNodeKeys.POKEMON_DEX_NUMBER, "");
                if (!dexNumber.isBlank()) {
                    actions.add(ResultContextMenu.Action.enabled(
                            COPY_POKEMON_DEX_NUMBER,
                            Component.translatable("ami.context.copy_pokemon_dex_number"),
                            'x',
                            () -> AmiClipboardHelper.copyToClipboard(dexNumber)
                    ));
                }
            }
        }

        addGregTechFilterActions(actions, policy, context, node);

        if (cheatEnabled.getAsBoolean()) {
            addCheatActions(actions, policy, node, id, stack, hasStack);
        }

        addQuestActions(actions, policy, context, node, id, questMatches);
        addPluginActions(actions, context, node, stack);

        if (policy.allows(node, FAVORITE) && id != null && node.type() != null && context.favorites() != null) {
            actions.add(ResultContextMenu.Action.enabled(
                    FAVORITE,
                    Component.translatable(isFavorite(context.favorites(), node) ? "ami.context.unfavorite" : "ami.context.favorite"),
                    'f',
                    () -> context.favorites().toggleFavorite(node)
            ));
        }

        if (policy.allows(node, CHAT) && id != null) {
            actions.add(ResultContextMenu.Action.enabled(
                    CHAT,
                    Component.translatable("ami.context.chat"),
                    'c',
                    () -> openChatDraft(chatText(node))
            ));
        }

        if (policy.allows(node, WIKI) && id != null) {
            DocumentationTarget documentationTarget = documentationTargetFor(node);
            actions.add(ResultContextMenu.Action.enabled(
                    WIKI,
                    documentationTarget.label(),
                    'w',
                    () -> openDocumentationTarget(node, documentationTarget)
            ));
        }

        if (policy.allows(node, START_CATEGORY_FIX) && id != null && node.type() == NodeType.ITEM) {
            actions.add(ResultContextMenu.Action.enabled(
                    START_CATEGORY_FIX,
                    Component.translatable("ami.context.start_category_fix"),
                    'x',
                    () -> startCategoryFix(node)
            ));
        }

        if (allowsEditCategoryFix(policy, node) && id != null && node.type() == NodeType.ITEM) {
            actions.add(ResultContextMenu.Action.enabled(
                    EDIT_CATEGORY_FIX,
                    Component.translatable("ami.context.edit_category_fix"),
                    'e',
                    () -> openCategoryFixScreenLater(node)
            ));
        }

        if (policy.allows(node, CLEAR_ITEM_FIX) && id != null && AmiDataFixes.hasUserFix(id, node.type())) {
            actions.add(ResultContextMenu.Action.enabled(
                    CLEAR_ITEM_FIX,
                    Component.translatable("ami.context.clear_item_fix"),
                    'l',
                    () -> {
                        AmiDataFixes.clearUserFixFromRuntimeIndex(id, node.type());
                        if (context.onDataFixApplied() != null) {
                            context.onDataFixApplied().run();
                        }
                    }
            ));
        }

        if (policy.allows(node, LOCATE) && id != null && node.type() == NodeType.BIOME && AMICheatMode.isAllowed()) {
            actions.add(ResultContextMenu.Action.enabled(
                    LOCATE,
                    Component.translatable("ami.context.locate_biome"),
                    'n',
                    () -> AMICheatMode.locateBiome(id)
            ));
        } else if (policy.allows(node, LOCATE) && id != null && node.type() == NodeType.STRUCTURE && AMICheatMode.isAllowed()) {
            actions.add(ResultContextMenu.Action.enabled(
                    LOCATE,
                    Component.translatable("ami.context.locate_structure"),
                    'n',
                    () -> AMICheatMode.locateStructure(id)
            ));
        }

        if (policy.allows(node, FILTER_MOD) && context.tokenInject() != null && id != null) {
            actions.add(ResultContextMenu.Action.enabled(
                    FILTER_MOD,
                    Component.translatable("ami.context.filter_mod"),
                    'm',
                    () -> context.tokenInject().accept("@" + id.getNamespace())
            ));
        }

        if (policy.allows(node, COPY_ID) && id != null) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_ID,
                    Component.translatable("ami.context.copy_id"),
                    'i',
                    () -> AmiClipboardHelper.copyToClipboard(id.toString())
            ));
        }

        return groupItemActions(actions);
    }

    private static List<ResultContextMenu.Action> groupItemActions(List<ResultContextMenu.Action> actions) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }

        Map<String, List<ResultContextMenu.Action>> grouped = new LinkedHashMap<>();
        for (ResultContextMenu.Action action : actions) {
            String family = menuFamily(action);
            if (family != null) {
                grouped.computeIfAbsent(family, ignored -> new ArrayList<>()).add(action);
            }
        }

        List<ResultContextMenu.Action> out = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        for (ResultContextMenu.Action action : actions) {
            String family = menuFamily(action);
            if (family == null) {
                out.add(action);
                continue;
            }
            if (!emitted.add(family)) {
                continue;
            }
            List<ResultContextMenu.Action> children = grouped.getOrDefault(family, List.of());
            addFlatOrSubmenu(out, family, menuFamilyLabel(family), menuFamilyMnemonic(family), children);
        }
        return List.copyOf(out);
    }

    private static String menuFamily(ResultContextMenu.Action action) {
        if (action == null || action.isSubmenu()) {
            return null;
        }
        return switch (action.id()) {
            case CRAFT_ONE, CRAFT_STACK -> MENU_CRAFTING;
            case RECIPES, USES, RECIPES_POKEMON_DROP_ITEM -> MENU_RECIPES;
            case CHEAT_GIVE_ONE, CHEAT_GIVE_STACK, CHEAT_SPAWN_EGG, CHEAT_SPAWN_EGG_STACK,
                 CHEAT_SPAWN_POKEMON, CHEAT_POKEMON_PARTY -> MENU_CHEAT;
            case FILTER_MOD -> MENU_FILTERS;
            case OPEN_POKEDEX, FILTER_POKEMON_TYPE, FILTER_POKEMON_SECONDARY_TYPE, FILTER_POKEMON_GENERATION,
                 FILTER_POKEMON_EGG_GROUP, FILTER_POKEMON_ABILITY, SEARCH_POKEMON_DROP_ITEM,
                 COPY_POKEMON_SPECIES, COPY_POKEMON_DEX_NUMBER -> MENU_POKEMON;
            case FILTER_GREGTECH_TIER, FILTER_GREGTECH_KIND, FILTER_GREGTECH_FACT,
                 FILTER_GREGTECH_CIRCUIT_GRADE -> MENU_GREGTECH;
            case QUESTS_FOR_ITEM, OPEN_QUEST, COPY_QUEST_MATCHES -> MENU_QUESTS;
            case COPY_FTB_ITEM_TASK, COPY_FTB_QUEST_SKELETON, COPY_KUBEJS_RECIPE_STUB,
                 COPY_KUBEJS_RECIPE_REMOVAL_SCRIPT, COPY_GAMESTAGE_CONDITION, COPY_PACK_AUTHOR_REPORT,
                 COPY_TAXONOMY_TEMPLATE, CREATE_PACK_FILES -> MENU_PACK_AUTHOR;
            case START_CATEGORY_FIX, EDIT_CATEGORY_FIX, APPLY_CATEGORY_FIX, CLEAR_ITEM_FIX -> MENU_DEVELOPER;
            default -> null;
        };
    }

    private static Component menuFamilyLabel(String family) {
        return switch (family) {
            case MENU_CRAFTING -> Component.translatable("ami.context.menu.crafting");
            case MENU_RECIPES -> Component.translatable("ami.context.menu.recipes");
            case MENU_CHEAT -> Component.translatable("ami.context.menu.cheat");
            case MENU_FILTERS -> Component.translatable("ami.context.menu.filters");
            case MENU_POKEMON -> Component.translatable("ami.context.menu.pokemon");
            case MENU_GREGTECH -> Component.translatable("ami.context.menu.gregtech");
            case MENU_QUESTS -> Component.translatable("ami.context.menu.quests");
            case MENU_PACK_AUTHOR -> Component.translatable("ami.context.menu.pack_author");
            case MENU_DEVELOPER -> Component.translatable("ami.context.menu.developer");
            default -> Component.literal("More");
        };
    }

    private static char menuFamilyMnemonic(String family) {
        return switch (family) {
            case MENU_CRAFTING -> 'c';
            case MENU_RECIPES -> 'r';
            case MENU_CHEAT -> 'g';
            case MENU_FILTERS -> 'f';
            case MENU_POKEMON -> 'p';
            case MENU_GREGTECH -> 't';
            case MENU_QUESTS -> 'q';
            case MENU_PACK_AUTHOR -> 'a';
            case MENU_DEVELOPER -> 'd';
            default -> 'm';
        };
    }

    private static void addPlayerActions(List<ResultContextMenu.Action> actions,
                                         ResultContextMenuActionPolicy policy,
                                         ItemContext context,
                                         SearchNode node,
                                         boolean canCheat) {
        String playerName = playerName(node);
        if (playerName.isBlank()) {
            return;
        }
        addFavoriteAction(actions, policy, context, node);
        if (policy.allows(node, COPY_PLAYER_NAME)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_PLAYER_NAME,
                    Component.translatable("ami.context.copy_player_name"),
                    'c',
                    () -> AmiClipboardHelper.copyToClipboard(playerName)
            ));
        }
        if (policy.allows(node, CHAT)) {
            actions.add(ResultContextMenu.Action.enabled(
                    CHAT,
                    Component.translatable("ami.context.chat"),
                    '@',
                    () -> openChatDraft("@" + playerName + " ")
            ));
        }
        List<ResultContextMenu.Action> waypointActions = new ArrayList<>();
        if (policy.allows(node, ADD_PLAYER_WAYPOINT)) {
            for (PlayerWaypointAction action : PlayerWaypointProviders.waypointActions(playerWaypointContext(node))) {
                waypointActions.add(ResultContextMenu.Action.enabled(
                        action.id(),
                        Component.literal(action.label()),
                        action.mnemonic(),
                        action.action()
                ));
            }
        }
        if (policy.allows(node, COPY_PLAYER_WAYPOINT)) {
            for (PlayerWaypointExport export : PlayerWaypointProviders.waypointExports(playerWaypointContext(node))) {
                waypointActions.add(ResultContextMenu.Action.enabled(
                        COPY_PLAYER_WAYPOINT,
                        Component.translatable("ami.context.copy_player_waypoint_named", export.label()),
                        'w',
                        () -> AmiClipboardHelper.copyToClipboard(export.payload())
                ));
            }
        }
        addFlatOrSubmenu(actions, PLAYER_WAYPOINTS_MENU, Component.translatable("ami.context.player_waypoints"), 'w',
                waypointActions);

        List<ResultContextMenu.Action> adminActions = new ArrayList<>();
        if (canCheat && policy.allows(node, CHEAT_GIVE_PLAYER_HEAD)) {
            adminActions.add(ResultContextMenu.Action.enabled(
                    CHEAT_GIVE_PLAYER_HEAD,
                    Component.translatable("ami.context.cheat_give_player_head"),
                    'h',
                    () -> {
                        AMICheatMode.runCommand(Services.PLATFORM.playerHeadGiveCommand(playerName));
                        PlayerHeadHistory.record(playerName);
                    }
            ));
        }
        if (canCheat && policy.allows(node, TELEPORT_TO_PLAYER)) {
            adminActions.add(ResultContextMenu.Action.enabled(
                    TELEPORT_TO_PLAYER,
                    Component.translatable("ami.context.teleport_to_player"),
                    't',
                    () -> AMICheatMode.runCommand("tp @s " + playerName)
            ));
        }
        if (canCheat && policy.allows(node, TELEPORT_PLAYER_HERE)) {
            adminActions.add(ResultContextMenu.Action.enabled(
                    TELEPORT_PLAYER_HERE,
                    Component.translatable("ami.context.teleport_player_here"),
                    'p',
                    () -> AMICheatMode.runCommand("tp " + playerName + " @s")
            ));
        }
        addFlatOrSubmenu(actions, PLAYER_ADMIN_MENU, Component.translatable("ami.context.player_admin"), 'a',
                adminActions);
    }

    private static void addLiveWaypointActions(List<ResultContextMenu.Action> actions,
                                               ResultContextMenuActionPolicy policy,
                                               ItemContext context,
                                               SearchNode node,
                                               boolean canCheat) {
        addFavoriteAction(actions, policy, context, node);
        PlayerWaypointProviders.openLiveWaypointAction(node).ifPresent(action -> actions.add(ResultContextMenu.Action.enabled(
                OPEN_WAYPOINT,
                Component.literal(action.label()),
                action.mnemonic(),
                action.action()
        )));
        for (PlayerWaypointAction action : PlayerWaypointProviders.liveWaypointActions(node)) {
            String id = action.id() == null || action.id().isBlank() ? "ami:waypoint_action" : action.id();
            actions.add(ResultContextMenu.Action.enabled(
                    id,
                    Component.literal(action.label()),
                    action.mnemonic(),
                    action.action()
            ));
        }

        String waypointText = waypointChatText(node);
        if (!waypointText.isBlank()) {
            actions.add(ResultContextMenu.Action.enabled(
                    CHAT_WAYPOINT,
                    Component.literal("Send Waypoint to Chat"),
                    's',
                    () -> openChatDraft(waypointText)
            ));
        }

        if (canCheat && hasWaypointCoordinates(node)) {
            actions.add(ResultContextMenu.Action.enabled(
                    TELEPORT_TO_WAYPOINT,
                    Component.literal("Teleport to Waypoint"),
                    't',
                    () -> AMICheatMode.runCommand("tp @s "
                            + node.meta(SearchNodeKeys.WAYPOINT_X, "0") + " "
                            + node.meta(SearchNodeKeys.WAYPOINT_Y, "0") + " "
                            + node.meta(SearchNodeKeys.WAYPOINT_Z, "0"))
            ));
        }
    }

    private static void addFavoriteAction(List<ResultContextMenu.Action> actions,
                                          ResultContextMenuActionPolicy policy,
                                          ItemContext context,
                                          SearchNode node) {
        if (actions == null || policy == null || context == null || node == null
                || !policy.allows(node, FAVORITE) || context.favorites() == null) {
            return;
        }
        actions.add(ResultContextMenu.Action.enabled(
                FAVORITE,
                Component.translatable(isFavorite(context.favorites(), node) ? "ami.context.unfavorite" : "ami.context.favorite"),
                'f',
                () -> context.favorites().toggleFavorite(node)
        ));
    }

    private static void addFlatOrSubmenu(List<ResultContextMenu.Action> actions,
                                         String submenuId,
                                         Component submenuLabel,
                                         char mnemonic,
                                         List<ResultContextMenu.Action> children) {
        if (actions == null || children == null || children.isEmpty()) {
            return;
        }
        if (children.size() == 1) {
            actions.add(children.get(0));
            return;
        }
        actions.add(ResultContextMenu.Action.submenu(submenuId, submenuLabel, mnemonic, children));
    }

    private static void addGregTechFilterActions(List<ResultContextMenu.Action> actions,
                                                 ResultContextMenuActionPolicy policy,
                                                 ItemContext context,
                                                 SearchNode node) {
        if (actions == null || policy == null || context == null || node == null || context.tokenInject() == null) {
            return;
        }
        if (!isGregTechNode(node)) {
            return;
        }

        String tier = node.meta(SearchNodeKeys.GREGTECH_TIER, "");
        if (policy.allows(node, FILTER_GREGTECH_TIER) && !tier.isBlank()) {
            actions.add(ResultContextMenu.Action.enabled(
                    FILTER_GREGTECH_TIER,
                    Component.translatable("ami.context.filter_gregtech_tier_named", formatGregTechTier(tier)),
                    'v',
                    () -> context.tokenInject().accept("?gregtechTier:" + tier)
            ));
        }

        String kind = node.meta(SearchNodeKeys.GREGTECH_ITEM_KIND, "");
        if (policy.allows(node, FILTER_GREGTECH_KIND) && !kind.isBlank()) {
            actions.add(ResultContextMenu.Action.enabled(
                    FILTER_GREGTECH_KIND,
                    Component.translatable("ami.context.filter_gregtech_kind_named", titleCaseUnderscorePath(kind)),
                    'k',
                    () -> context.tokenInject().accept("?gregtechKind:" + kind)
            ));
        }

        String circuitGrade = node.meta(SearchNodeKeys.GREGTECH_CIRCUIT_GRADE, "");
        if (policy.allows(node, FILTER_GREGTECH_CIRCUIT_GRADE) && !circuitGrade.isBlank()) {
            actions.add(ResultContextMenu.Action.enabled(
                    FILTER_GREGTECH_CIRCUIT_GRADE,
                    Component.translatable("ami.context.filter_gregtech_circuit_grade_named",
                            titleCaseUnderscorePath(circuitGrade)),
                    'b',
                    () -> context.tokenInject().accept("?gregtechCircuit:" + circuitGrade)
            ));
        }

        for (String fact : splitMetadataTokens(node.meta(SearchNodeKeys.GREGTECH_FACTS, ""))) {
            if (!policy.allows(node, FILTER_GREGTECH_FACT)) {
                break;
            }
            if (isRedundantGregTechFact(kind, fact)) {
                continue;
            }
            actions.add(ResultContextMenu.Action.enabled(
                    FILTER_GREGTECH_FACT,
                    Component.translatable("ami.context.filter_gregtech_fact_named", titleCaseUnderscorePath(fact)),
                    'g',
                    () -> context.tokenInject().accept("?gregtechFact:" + fact)
            ));
            break;
        }
    }

    private static boolean isRedundantGregTechFact(String kind, String fact) {
        if (kind == null || fact == null || kind.isBlank() || fact.isBlank()) {
            return false;
        }
        String normalizedKind = kind.toLowerCase(Locale.ROOT);
        String normalizedFact = fact.toLowerCase(Locale.ROOT);
        return normalizedKind.equals(normalizedFact)
                || normalizedKind.equals(normalizedFact + "s")
                || (normalizedKind.endsWith("s")
                && normalizedKind.substring(0, normalizedKind.length() - 1).equals(normalizedFact));
    }

    private static void addQuestActions(List<ResultContextMenu.Action> actions,
                                        ResultContextMenuActionPolicy policy,
                                        ItemContext context,
                                        SearchNode node,
                                        ResourceLocation id,
                                        List<AmiQuestItemMatch> questMatches) {
        if (actions == null || policy == null || context == null || node == null || id == null || node.type() != NodeType.ITEM) {
            return;
        }

        if (questMatches != null && !questMatches.isEmpty()) {
            if (policy.allows(node, QUESTS_FOR_ITEM) && context.tokenInject() != null) {
                actions.add(ResultContextMenu.Action.enabled(
                        QUESTS_FOR_ITEM,
                        Component.translatable("ami.context.quests_for_item"),
                        'q',
                        () -> context.tokenInject().accept("quest " + id.getPath().replace('_', ' '))
                ));
            }

            questMatches.stream()
                    .map(AmiQuestItemMatch::quest)
                    .filter(quest -> quest != null && quest.canOpen())
                    .findFirst()
                    .ifPresent(quest -> {
                        if (policy.allows(node, OPEN_QUEST)) {
                            actions.add(ResultContextMenu.Action.enabled(
                                    OPEN_QUEST,
                                    Component.translatable("ami.context.open_quest"),
                                    'o',
                                    quest::open
                            ));
                        }
                    });

            if (policy.allows(node, COPY_QUEST_MATCHES)) {
                actions.add(ResultContextMenu.Action.enabled(
                        COPY_QUEST_MATCHES,
                        Component.translatable("ami.context.copy_quest_matches"),
                        'v',
                        () -> AmiClipboardHelper.copyToClipboard(questMatchesSummary(id, questMatches))
                ));
            }
        }

        if (isPackAuthorMode() && policy.allowsDev(node, COPY_FTB_ITEM_TASK)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_FTB_ITEM_TASK,
                    Component.translatable("ami.context.copy_ftb_item_task"),
                    'b',
                    () -> AmiClipboardHelper.copyToClipboard(ftbItemTaskTemplate(id, 1))
            ));
        }
        if (isPackAuthorMode() && policy.allowsDev(node, COPY_FTB_QUEST_SKELETON)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_FTB_QUEST_SKELETON,
                    Component.translatable("ami.context.copy_ftb_quest_skeleton"),
                    's',
                    () -> AmiClipboardHelper.copyToClipboard(ftbQuestSkeleton(node))
            ));
        }
        if (isPackAuthorMode() && policy.allowsDev(node, COPY_KUBEJS_RECIPE_STUB)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_KUBEJS_RECIPE_STUB,
                    Component.translatable("ami.context.copy_kubejs_recipe_stub"),
                    'j',
                    () -> AmiClipboardHelper.copyToClipboard(kubeJsRecipeStub(id))
            ));
        }
        if (isPackAuthorMode() && policy.allowsDev(node, COPY_GAMESTAGE_CONDITION)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_GAMESTAGE_CONDITION,
                    Component.translatable("ami.context.copy_gamestage_condition"),
                    'g',
                    () -> AmiClipboardHelper.copyToClipboard(gameStageConditionStub(id))
            ));
        }
        if (isPackAuthorMode() && policy.allowsDev(node, COPY_PACK_AUTHOR_REPORT)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_PACK_AUTHOR_REPORT,
                    Component.translatable("ami.context.copy_pack_author_item_report"),
                    'p',
                    () -> AmiClipboardHelper.copyToClipboard(PackAuthorDiagnostics.itemReport(node, questMatches))
            ));
        }
    }

    static String ftbItemTaskTemplate(ResourceLocation itemId, long count) {
        if (itemId == null) {
            return "";
        }
        long safeCount = Math.max(1L, count);
        return "{type:\"item\",item:\"" + itemId + "\",count:" + safeCount + "}";
    }

    static String ftbQuestSkeleton(SearchNode node) {
        if (node == null || node.id() == null) {
            return "";
        }
        return ftbQuestSkeleton(displayNameOrPath(node), List.of(node), false);
    }

    static String ftbQuestSkeleton(String title, List<SearchNode> nodes, boolean capped) {
        List<SearchNode> items = nodes == null ? List.of() : nodes.stream()
                .filter(node -> node != null && node.id() != null && node.type() == NodeType.ITEM)
                .limit(MAX_GROUP_AUTHORING_ITEMS)
                .toList();
        if (items.isEmpty()) {
            return "";
        }

        String safeTitle = snbtString(title == null || title.isBlank() ? "AMI Quest" : title.trim());
        StringBuilder out = new StringBuilder();
        out.append("{").append(System.lineSeparator());
        out.append("  id:\"REPLACE_WITH_16_HEX_ID\",").append(System.lineSeparator());
        out.append("  title:\"").append(safeTitle).append("\",").append(System.lineSeparator());
        out.append("  x:0.0d,").append(System.lineSeparator());
        out.append("  y:0.0d,").append(System.lineSeparator());
        out.append("  tasks:[").append(System.lineSeparator());
        for (int i = 0; i < items.size(); i++) {
            SearchNode item = items.get(i);
            out.append("    {id:\"REPLACE_TASK_").append(String.format(Locale.ROOT, "%03d", i + 1))
                    .append("\",type:\"item\",item:\"").append(item.id()).append("\",count:1}");
            if (i < items.size() - 1) {
                out.append(",");
            }
            out.append(System.lineSeparator());
        }
        out.append("  ],").append(System.lineSeparator());
        out.append("  rewards:[]");
        if (capped) {
            out.append(",").append(System.lineSeparator());
            out.append("  subtitle:\"AMI export capped at ").append(MAX_GROUP_AUTHORING_ITEMS).append(" items\"");
        }
        out.append(System.lineSeparator()).append("}");
        return out.toString();
    }

    static String kubeJsRecipeStub(ResourceLocation outputId) {
        if (outputId == null) {
            return "";
        }
        return "ServerEvents.recipes(event => {" + System.lineSeparator()
                + "  event.shaped('" + outputId + "', [" + System.lineSeparator()
                + "    'AAA'," + System.lineSeparator()
                + "    'ABA'," + System.lineSeparator()
                + "    'AAA'" + System.lineSeparator()
                + "  ], {" + System.lineSeparator()
                + "    A: 'minecraft:stone'," + System.lineSeparator()
                + "    B: 'minecraft:crafting_table'" + System.lineSeparator()
                + "  })" + System.lineSeparator()
                + "})";
    }

    static String kubeJsRecipeRemovalScript(String title, List<SearchNode> nodes, boolean capped) {
        List<SearchNode> items = nodes == null ? List.of() : nodes.stream()
                .filter(node -> node != null && node.id() != null && node.type() == NodeType.ITEM)
                .limit(MAX_GROUP_AUTHORING_ITEMS)
                .toList();
        if (items.isEmpty()) {
            return "";
        }

        String safeTitle = title == null || title.isBlank() ? "Selected Items" : title.trim();
        StringBuilder out = new StringBuilder();
        out.append("// AMI export: remove recipes for ").append(safeTitle).append(System.lineSeparator());
        out.append("// Review each line before using it in a pack.").append(System.lineSeparator());
        if (capped) {
            out.append("// Export capped at ").append(MAX_GROUP_AUTHORING_ITEMS).append(" items.").append(System.lineSeparator());
        }
        out.append("ServerEvents.recipes(event => {").append(System.lineSeparator());
        for (SearchNode item : items) {
            out.append("  event.remove({ output: '").append(item.id()).append("' })").append(System.lineSeparator());
        }
        out.append("})");
        return out.toString();
    }

    static List<PackAuthorExportFile> packAuthorGroupFiles(TreeNode groupNode, List<SearchNode> nodes, boolean capped) {
        List<SearchNode> items = nodes == null ? List.of() : nodes.stream()
                .filter(node -> node != null && node.id() != null && node.type() == NodeType.ITEM)
                .limit(MAX_GROUP_AUTHORING_ITEMS)
                .toList();
        if (items.isEmpty()) {
            return List.of();
        }

        String title = groupTitle(groupNode);
        String slug = normalizeTaxonomyId(title, "ami_group");
        return List.of(
                new PackAuthorExportFile(
                        Path.of("ami", "ami-export-" + slug + "-README.md"),
                        packAuthorReadme(groupNode, items, capped)
                ),
                new PackAuthorExportFile(
                        Path.of("ami", "taxonomy", "ami-export-" + slug + ".json"),
                        customTaxonomyTemplate(groupNode, items, capped)
                ),
                new PackAuthorExportFile(
                        Path.of("kubejs", "server_scripts", "ami", "ami-export-" + slug + ".js"),
                        kubeJsRecipeRemovalScript(title, items, capped)
                )
        );
    }

    static String packAuthorReadme(TreeNode groupNode, List<SearchNode> nodes, boolean capped) {
        List<SearchNode> items = nodes == null ? List.of() : nodes.stream()
                .filter(node -> node != null && node.id() != null && node.type() == NodeType.ITEM)
                .limit(MAX_GROUP_AUTHORING_ITEMS)
                .toList();
        if (items.isEmpty()) {
            return "";
        }

        String title = groupTitle(groupNode);
        String slug = normalizeTaxonomyId(title, "ami_group");
        StringBuilder out = new StringBuilder();
        out.append("# AMI Pack Export: ").append(title).append(System.lineSeparator()).append(System.lineSeparator());
        out.append("AMI created these files for the selected result group:").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("- `ami/taxonomy/ami-export-").append(slug).append(".json`").append(System.lineSeparator());
        out.append("  - Use this to move the group into a category or keep similar items together.").append(System.lineSeparator());
        out.append("- `kubejs/server_scripts/ami/ami-export-").append(slug).append(".js`").append(System.lineSeparator());
        out.append("  - Use this to remove the current recipes for these items before replacing them.").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("## What to edit first").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("1. Open the taxonomy JSON if the group belongs in a different category or needs a broader match.").append(System.lineSeparator());
        out.append("2. Open the KubeJS script if you want to hide or replace the recipes for this group.").append(System.lineSeparator());
        out.append("3. Review the item list below before shipping the files in a pack.").append(System.lineSeparator()).append(System.lineSeparator());
        out.append("## Exported items").append(System.lineSeparator()).append(System.lineSeparator());
        for (SearchNode item : items) {
            out.append("- `").append(item.id()).append("`");
            String displayName = item.displayName() == null ? "" : item.displayName().trim();
            if (!displayName.isBlank()) {
                out.append(" — ").append(displayName);
            }
            out.append(System.lineSeparator());
        }
        if (capped) {
            out.append(System.lineSeparator())
                    .append("AMI capped this export at ")
                    .append(MAX_GROUP_AUTHORING_ITEMS)
                    .append(" items. If this group should cover more items, widen the taxonomy match before relying on it.")
                    .append(System.lineSeparator());
        }
        return out.toString();
    }

    static List<Path> writePackAuthorGroupFiles(TreeNode groupNode, List<SearchNode> nodes, boolean capped, Path gameDir) throws IOException {
        if (gameDir == null) {
            return List.of();
        }
        List<Path> written = new ArrayList<>();
        for (PackAuthorExportFile export : packAuthorGroupFiles(groupNode, nodes, capped)) {
            Path target = nextAvailablePath(gameDir, export.relativePath());
            Files.createDirectories(target.getParent());
            Files.writeString(target, export.content(), StandardCharsets.UTF_8);
            written.add(target);
        }
        return List.copyOf(written);
    }

    private static void writePackAuthorGroupFilesLater(TreeNode groupNode, List<SearchNode> nodes, boolean capped) {
        try {
            Path gameDir = Services.PLATFORM.getGameDir();
            List<Path> written = writePackAuthorGroupFiles(groupNode, nodes, capped, gameDir);
            if (written.isEmpty()) {
                showClientStatus(Component.translatable("ami.context.pack_files_failed"));
                return;
            }
            String summary = written.stream()
                    .map(path -> describeGameRelativePath(gameDir, path))
                    .collect(java.util.stream.Collectors.joining(", "));
            showClientStatus(Component.translatable("ami.context.pack_files_created", summary));
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to create pack files for group export", e);
            showClientStatus(Component.translatable("ami.context.pack_files_failed"));
        }
    }

    static String gameStageConditionStub(ResourceLocation outputId) {
        if (outputId == null) {
            return "";
        }
        return "ServerEvents.recipes(event => {" + System.lineSeparator()
                + "  const stage = 'replace_stage_id'" + System.lineSeparator()
                + "  event.remove({ output: '" + outputId + "' })" + System.lineSeparator()
                + "  // Requires RecipeStages or another GameStages recipe bridge that exposes .stage(stage)." + System.lineSeparator()
                + "  event.shaped('" + outputId + "', [" + System.lineSeparator()
                + "    'AAA'," + System.lineSeparator()
                + "    'ABA'," + System.lineSeparator()
                + "    'AAA'" + System.lineSeparator()
                + "  ], {" + System.lineSeparator()
                + "    A: 'minecraft:stone'," + System.lineSeparator()
                + "    B: 'minecraft:crafting_table'" + System.lineSeparator()
                + "  }).stage(stage)" + System.lineSeparator()
                + "})";
    }

    static String customTaxonomyTemplate(TreeNode groupNode, List<SearchNode> nodes, boolean capped) {
        List<SearchNode> items = nodes == null ? List.of() : nodes.stream()
                .filter(node -> node != null && node.id() != null && node.type() == NodeType.ITEM)
                .limit(MAX_GROUP_AUTHORING_ITEMS)
                .toList();
        if (items.isEmpty()) {
            return "";
        }

        String title = groupTitle(groupNode);
        TaxonomyTemplateTarget target = taxonomyTemplateTarget(groupNode, items);

        JsonObject root = new JsonObject();
        JsonArray notes = new JsonArray();
        notes.add("Starter export from AMI pack-author mode.");
        notes.add("This export intentionally matches exact item IDs first; broaden to mods, paths, or tags after review.");
        if (capped) {
            notes.add("Export capped at " + MAX_GROUP_AUTHORING_ITEMS + " items; broaden the match before relying on it.");
        }
        if (target.categoryNeedsDefinition()) {
            notes.add("Edit the generated category id/label if you want a different name or icon.");
        }
        root.add("notes", notes);

        if (target.categoryNeedsDefinition()) {
            JsonObject categories = new JsonObject();
            JsonObject category = new JsonObject();
            category.addProperty("label", target.categoryLabel());
            category.addProperty("iconItem", "minecraft:paper");
            if (!target.subcategoryId().isBlank()) {
                JsonObject subcategories = new JsonObject();
                subcategories.addProperty(target.subcategoryId(), target.subcategoryLabel());
                category.add("subcategories", subcategories);
            }
            categories.add(target.categoryId(), category);
            root.add("categories", categories);
        }

        JsonObject rule = new JsonObject();
        JsonObject match = new JsonObject();
        JsonArray ids = new JsonArray();
        items.stream()
                .map(SearchNode::id)
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(ids::add);
        match.add("ids", ids);
        rule.add("match", match);
        rule.addProperty("category", target.categoryId());
        if (!target.subcategoryId().isBlank()) {
            rule.addProperty("subcategory", target.subcategoryId());
        }

        JsonObject metadata = commonTaxonomyMetadata(items, title);
        if (metadata.size() > 0) {
            rule.add("metadata", metadata);
        }

        JsonArray rules = new JsonArray();
        rules.add(rule);
        root.add("rules", rules);
        return JSON.toJson(root);
    }

    static String questMatchesSummary(ResourceLocation itemId, List<AmiQuestItemMatch> matches) {
        StringBuilder out = new StringBuilder();
        out.append("Quests for ").append(itemId == null ? "item" : itemId.toString());
        if (matches == null || matches.isEmpty()) {
            return out.append(": none").toString();
        }
        for (AmiQuestItemMatch match : matches) {
            if (match == null || match.quest() == null || match.task() == null) {
                continue;
            }
            out.append(System.lineSeparator())
                    .append("- ");
            if (!match.quest().chapterTitle().isBlank()) {
                out.append(match.quest().chapterTitle()).append(" > ");
            }
            out.append(match.quest().title().isBlank() ? match.quest().id() : match.quest().title());
            if (match.task().requiredCount() > 1) {
                out.append(" x").append(match.task().requiredCount());
            }
            out.append(" (").append(match.task().role().name().toLowerCase(Locale.ROOT)).append(")");
        }
        return out.toString();
    }

    private void addPluginActions(List<ResultContextMenu.Action> actions, ItemContext context, SearchNode node, ItemStack stack) {
        if (actions == null || context == null || node == null) return;

        List<IAmiPlugin> plugins;
        try {
            plugins = pluginSupplier.get();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to obtain plugins for context menu", e);
            plugins = List.of();
        }
        if (plugins == null) {
            plugins = List.of();
        }

        AmiItemContext apiContext = new AmiItemContext(
                node.id(),
                node.type() == null ? "" : node.type().name(),
                Component.literal(node.displayName() == null ? "" : node.displayName()),
                stack,
                node.metadata(),
                cheatEnabled.getAsBoolean(),
                context.tokenInject()
        );

        for (IAmiPlugin plugin : plugins) {
            try {
                plugin.addItemContextMenuActions(apiContext, action -> addPluginAction(actions, action));
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.WARNING,
                        "AMI: Context menu plugin failed: " + plugin.getClass().getName(), e);
            }
        }

        SearchableItemActionContext sharedContext = new SearchableItemActionContext(
                apiContext.id(),
                apiContext.type(),
                apiContext.displayName(),
                apiContext.stack(),
                apiContext.metadata(),
                apiContext.cheatEnabled(),
                apiContext.tokenInject()
        );
        for (SearchableItemActionProvider provider : SearchableItemActionProviders.getProviders()) {
            try {
                provider.addItemActions(sharedContext, action -> addSharedItemAction(actions, action));
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.WARNING,
                        "AMI: Shared item action provider failed: " + sharedActionProviderId(provider), e);
            }
        }
    }

    private static void addPluginAction(List<ResultContextMenu.Action> actions, AmiContextMenuAction action) {
        if (actions == null || action == null || action.label() == null) return;
        actions.add(new ResultContextMenu.Action(
                action.id(),
                action.label(),
                action.mnemonic(),
                action.enabled(),
                action.onClick()
        ));
    }

    private static void addSharedItemAction(List<ResultContextMenu.Action> actions, SearchableItemAction action) {
        if (actions == null || action == null || action.label() == null) return;
        actions.add(new ResultContextMenu.Action(
                action.id(),
                action.label(),
                action.mnemonic(),
                action.enabled(),
                action.onClick()
        ));
    }

    private static String sharedActionProviderId(SearchableItemActionProvider provider) {
        try {
            String id = provider.id();
            return id == null || id.isBlank() ? provider.getClass().getName() : id;
        } catch (RuntimeException | LinkageError ignored) {
            return provider.getClass().getName();
        }
    }

    private static boolean allowsEditCategoryFix(ResultContextMenuActionPolicy policy, SearchNode node) {
        if (policy == null) return false;
        return policy.allows(node, EDIT_CATEGORY_FIX)
                || (AmiConfig.devMode && policy.allowsDev(node, EDIT_CATEGORY_FIX));
    }

    private void addCheatActions(List<ResultContextMenu.Action> actions, ResultContextMenuActionPolicy policy,
                                 SearchNode node, ResourceLocation id, ItemStack stack, boolean hasStack) {
        if (actions == null || policy == null || node == null || id == null) return;

        if (node.type() == NodeType.ITEM && hasStack) {
            if (policy.allows(node, CHEAT_GIVE_ONE)) {
                actions.add(ResultContextMenu.Action.enabled(
                        CHEAT_GIVE_ONE,
                        Component.translatable("ami.context.cheat_give_one"),
                        'g',
                        () -> {
                            AMICheatMode.giveItem(stack);
                            recordPlayerHeadHistory(node);
                        }
                ));
            }
            if (policy.allows(node, CHEAT_GIVE_STACK)) {
                actions.add(ResultContextMenu.Action.enabled(
                        CHEAT_GIVE_STACK,
                        Component.translatable("ami.context.cheat_give_stack"),
                        's',
                        () -> {
                            AMICheatMode.giveStack(stack);
                            recordPlayerHeadHistory(node);
                        }
                ));
            }
            return;
        }

        if (node.type() != NodeType.ENTITY) return;

        if (isPokemonSpecies(node)) {
            if (policy.allows(node, CHEAT_SPAWN_POKEMON)) {
                actions.add(ResultContextMenu.Action.enabled(
                        CHEAT_SPAWN_POKEMON,
                        Component.translatable("ami.context.cheat_spawn_pokemon"),
                        'p',
                        () -> AMICheatMode.spawnPokemon(id)
                ));
            }
            if (policy.allows(node, CHEAT_POKEMON_PARTY)) {
                actions.add(ResultContextMenu.Action.enabled(
                        CHEAT_POKEMON_PARTY,
                        Component.translatable("ami.context.cheat_pokemon_party"),
                        'a',
                        () -> AMICheatMode.pokemonToParty(id)
                ));
            }
            return;
        }

        if (!hasSpawnEgg(id)) return;

        if (policy.allows(node, CHEAT_SPAWN_EGG)) {
            actions.add(ResultContextMenu.Action.enabled(
                    CHEAT_SPAWN_EGG,
                    Component.translatable("ami.context.cheat_spawn_egg"),
                    'g',
                    () -> AMICheatMode.giveEntityAsSpawnEgg(id)
            ));
        }
        if (policy.allows(node, CHEAT_SPAWN_EGG_STACK)) {
            actions.add(ResultContextMenu.Action.enabled(
                    CHEAT_SPAWN_EGG_STACK,
                    Component.translatable("ami.context.cheat_spawn_egg_stack"),
                    's',
                    () -> AMICheatMode.giveEntityStackAsSpawnEgg(id)
            ));
        }
    }

    public List<ResultContextMenu.Action> forGroup(GroupContext context) {
        List<ResultContextMenu.Action> actions = new ArrayList<>();
        if (context == null || context.node() == null || context.node().isLeaf()) return actions;
        ResultContextMenuActionPolicy policy = ResultContextMenuActionPolicy.fromConfig();

        TreeNode node = context.node();
        if (policy.allowsGroup(node, GROUP_TOGGLE)) {
            actions.add(ResultContextMenu.Action.enabled(
                    GROUP_TOGGLE,
                    Component.translatable(node.isExpanded() ? "ami.context.collapse_group" : "ami.context.expand_group"),
                    node.isExpanded() ? 'c' : 'e',
                    () -> {
                        node.setExpanded(!node.isExpanded());
                        if (context.onTreeChanged() != null) context.onTreeChanged().run();
                    }
            ));
        }

        String key = node.getKey();
        if (policy.allowsGroup(node, FILTER_CATEGORY) && context.tokenInject() != null && key != null && !key.isBlank()) {
            actions.add(ResultContextMenu.Action.enabled(
                    FILTER_CATEGORY,
                    Component.translatable("ami.context.filter_category"),
                    'f',
                    () -> context.tokenInject().accept("$" + key)
            ));
        }

        if (policy.allowsGroup(node, COPY_GROUP_KEY) && key != null && !key.isBlank()) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_GROUP_KEY,
                    Component.translatable("ami.context.copy_group_key"),
                    'k',
                    () -> AmiClipboardHelper.copyToClipboard(key)
            ));
        }

        List<SearchNode> authoringItems = collectAuthoringItems(node, MAX_GROUP_AUTHORING_ITEMS);
        boolean capped = countAuthoringItems(node, MAX_GROUP_AUTHORING_ITEMS + 1) > MAX_GROUP_AUTHORING_ITEMS;
        if (isPackAuthorMode() && !authoringItems.isEmpty() && policy.allowsDevGroup(node, CREATE_PACK_FILES)) {
            actions.add(ResultContextMenu.Action.enabled(
                    CREATE_PACK_FILES,
                    Component.translatable("ami.context.create_pack_files"),
                    'a',
                    () -> writePackAuthorGroupFilesLater(node, authoringItems, capped)
            ));
        }
        if (isPackAuthorMode() && !authoringItems.isEmpty() && policy.allowsDevGroup(node, COPY_FTB_QUEST_SKELETON)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_FTB_QUEST_SKELETON,
                    Component.translatable("ami.context.copy_ftb_quest_skeleton"),
                    's',
                    () -> AmiClipboardHelper.copyToClipboard(ftbQuestSkeleton(groupTitle(node), authoringItems, capped))
            ));
        }
        if (isPackAuthorMode() && !authoringItems.isEmpty() && policy.allowsDevGroup(node, COPY_TAXONOMY_TEMPLATE)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_TAXONOMY_TEMPLATE,
                    Component.translatable("ami.context.copy_taxonomy_template"),
                    'x',
                    () -> AmiClipboardHelper.copyToClipboard(customTaxonomyTemplate(node, authoringItems, capped))
            ));
        }
        if (isPackAuthorMode() && !authoringItems.isEmpty() && policy.allowsDevGroup(node, COPY_KUBEJS_RECIPE_REMOVAL_SCRIPT)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_KUBEJS_RECIPE_REMOVAL_SCRIPT,
                    Component.translatable("ami.context.copy_kubejs_recipe_removal_script"),
                    'j',
                    () -> AmiClipboardHelper.copyToClipboard(kubeJsRecipeRemovalScript(groupTitle(node), authoringItems, capped))
            ));
        }

        List<SearchNode> reportItems = collectAuthoringItems(node, MAX_GROUP_REPORT_ITEMS);
        if (isPackAuthorMode() && !reportItems.isEmpty() && policy.allowsDevGroup(node, COPY_PACK_AUTHOR_REPORT)) {
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_PACK_AUTHOR_REPORT,
                    Component.translatable("ami.context.copy_pack_author_group_report"),
                    'p',
                    () -> AmiClipboardHelper.copyToClipboard(PackAuthorDiagnostics.groupReport(
                            groupTitle(node),
                            reportItems,
                            AmiQuestsApi.getQuestDocuments()
                    ))
            ));
        }

        SearchNode pending = pendingCategoryFixNode;
        Optional<CategoryTarget> target = categoryTargetFor(key);
        if (pending != null && target.isPresent() && policy.allowsGroup(node, APPLY_CATEGORY_FIX)) {
            CategoryTarget categoryTarget = target.get();
            actions.add(ResultContextMenu.Action.enabled(
                    APPLY_CATEGORY_FIX,
                    Component.translatable("ami.context.apply_category_fix", pending.displayName()),
                    'm',
                    () -> {
                        Map<String, String> metadata = new LinkedHashMap<>();
                        metadata.put(SearchNodeKeys.ONTOLOGY_CATEGORY, categoryTarget.categoryId());
                        metadata.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, categoryTarget.subcategoryId());
                        AmiDataFixes.applyUserMetadataFixToRuntimeIndex(pending.id(), pending.type(), metadata);
                        pendingCategoryFixNode = null;
                        showClientStatus(Component.translatable(
                                "ami.context.category_fix_applied",
                                pending.displayName(),
                                groupTitle(node)
                        ));
                        if (context.onDataFixApplied() != null) {
                            context.onDataFixApplied().run();
                        } else if (context.onTreeChanged() != null) {
                            context.onTreeChanged().run();
                        }
                    }
            ));
        }

        return actions;
    }

    private static boolean isPackAuthorMode() {
        return AmiConfig.packAuthorMode || AmiConfig.devMode;
    }

    private static void startCategoryFix(SearchNode node) {
        pendingCategoryFixNode = resolvedNodeForFix(node);
        if (pendingCategoryFixNode != null) {
            showClientStatus(Component.translatable("ami.context.category_fix_selected", pendingCategoryFixNode.displayName()));
        }
    }

    private static List<SearchNode> collectAuthoringItems(TreeNode node, int limit) {
        List<SearchNode> out = new ArrayList<>();
        collectAuthoringItems(node, Math.max(0, limit), out);
        return out;
    }

    private static void collectAuthoringItems(TreeNode node, int limit, List<SearchNode> out) {
        if (node == null || out == null || out.size() >= limit) {
            return;
        }
        if (node.isLeaf()) {
            SearchNode entry = node.getEntry();
            if (entry != null && entry.type() == NodeType.ITEM && entry.id() != null) {
                out.add(entry);
            }
            return;
        }
        for (TreeNode child : node.getChildren()) {
            collectAuthoringItems(child, limit, out);
            if (out.size() >= limit) {
                return;
            }
        }
    }

    private static int countAuthoringItems(TreeNode node, int limit) {
        if (node == null || limit <= 0) {
            return 0;
        }
        if (node.isLeaf()) {
            SearchNode entry = node.getEntry();
            return entry != null && entry.type() == NodeType.ITEM && entry.id() != null ? 1 : 0;
        }
        int count = 0;
        for (TreeNode child : node.getChildren()) {
            count += countAuthoringItems(child, limit - count);
            if (count >= limit) {
                return count;
            }
        }
        return count;
    }

    private static String groupTitle(TreeNode node) {
        if (node == null || node.getLabel() == null) {
            return "AMI Quest";
        }
        String label = node.getLabel().getString();
        return label == null || label.isBlank() ? "AMI Quest" : label;
    }

    private static String displayNameOrPath(SearchNode node) {
        if (node == null) {
            return "AMI Quest";
        }
        if (node.displayName() != null && !node.displayName().isBlank()) {
            return node.displayName();
        }
        ResourceLocation id = node.id();
        return id == null ? "AMI Quest" : id.getPath().replace('_', ' ');
    }

    private static String snbtString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static TaxonomyTemplateTarget taxonomyTemplateTarget(TreeNode groupNode, List<SearchNode> items) {
        Optional<CategoryTarget> directTarget = categoryTargetFor(groupNode == null ? null : groupNode.getKey());
        if (directTarget.isPresent()) {
            CategoryTarget target = directTarget.get();
            String subcategoryLabel = target.subcategoryId().isBlank()
                    ? ""
                    : titleCaseUnderscorePath(target.subcategoryId());
            return new TaxonomyTemplateTarget(
                    target.categoryId(),
                    target.subcategoryId(),
                    AmiOntology.categoryForId(target.categoryId()).displayName().getString(),
                    subcategoryLabel,
                    !AmiOntology.isDefinedCategoryId(target.categoryId())
            );
        }

        String inferredCategory = dominantMetadata(items, SearchNodeKeys.ONTOLOGY_CATEGORY).orElse("");
        String inferredSubcategory = dominantMetadata(items, SearchNodeKeys.ONTOLOGY_SUBCATEGORY).orElse("");
        if (!inferredCategory.isBlank()) {
            return new TaxonomyTemplateTarget(
                    inferredCategory,
                    inferredSubcategory,
                    AmiOntology.categoryForId(inferredCategory).displayName().getString(),
                    inferredSubcategory.isBlank() ? "" : titleCaseUnderscorePath(inferredSubcategory),
                    !AmiOntology.isDefinedCategoryId(inferredCategory)
            );
        }

        String title = groupTitle(groupNode);
        return new TaxonomyTemplateTarget(
                normalizeTaxonomyId(title, "replace_category_id"),
                "group",
                title,
                "Group",
                true
        );
    }

    private static JsonObject commonTaxonomyMetadata(List<SearchNode> items, String title) {
        JsonObject metadata = new JsonObject();
        addCommonMetadata(metadata, items, SearchNodeKeys.COLLAPSE_FAMILY);
        addCommonMetadata(metadata, items, SearchNodeKeys.VARIANT_COLLAPSE_MODE);
        addCommonMetadata(metadata, items, SearchNodeKeys.MATERIAL_GROUP);
        addCommonMetadata(metadata, items, SearchNodeKeys.VARIANT_GROUP);
        addCommonMetadata(metadata, items, SearchNodeKeys.BLOCKS_MATERIAL);

        String collapseLabel = commonMetadata(items, SearchNodeKeys.COLLAPSE_LABEL).orElse("");
        if (collapseLabel.isBlank() && !commonMetadata(items, SearchNodeKeys.COLLAPSE_FAMILY).orElse("").isBlank()) {
            collapseLabel = title == null ? "" : title.trim();
        }
        if (!collapseLabel.isBlank()) {
            metadata.addProperty(SearchNodeKeys.COLLAPSE_LABEL, collapseLabel);
        }
        return metadata;
    }

    private static void addCommonMetadata(JsonObject out, List<SearchNode> items, String key) {
        commonMetadata(items, key).ifPresent(value -> out.addProperty(key, value));
    }

    private static Optional<String> commonMetadata(List<SearchNode> items, String key) {
        if (items == null || items.isEmpty() || key == null || key.isBlank()) {
            return Optional.empty();
        }
        String common = null;
        for (SearchNode item : items) {
            String value = item == null ? "" : item.meta(key, "");
            if (value.isBlank()) {
                return Optional.empty();
            }
            if (common == null) {
                common = value;
            } else if (!common.equals(value)) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(common);
    }

    private static Optional<String> dominantMetadata(List<SearchNode> items, String key) {
        if (items == null || items.isEmpty() || key == null || key.isBlank()) {
            return Optional.empty();
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SearchNode item : items) {
            String value = item == null ? "" : item.meta(key, "").trim().toLowerCase(Locale.ROOT);
            if (!value.isBlank()) {
                counts.merge(value, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey);
    }

    private static String normalizeTaxonomyId(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace('/', '_');
        StringBuilder out = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == ':') {
                out.append(c);
            }
        }
        return out.isEmpty() ? fallback : out.toString();
    }

    private static Path nextAvailablePath(Path baseDir, Path relativePath) {
        Path candidate = baseDir.resolve(relativePath);
        if (!Files.exists(candidate)) {
            return candidate;
        }

        String fileName = relativePath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String stem = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot >= 0 ? fileName.substring(dot) : "";
        Path parent = relativePath.getParent();
        for (int index = 2; index < 1000; index++) {
            Path numbered = parent == null
                    ? Path.of(stem + "-" + index + extension)
                    : parent.resolve(stem + "-" + index + extension);
            candidate = baseDir.resolve(numbered);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        return candidate;
    }

    private static String describeGameRelativePath(Path gameDir, Path path) {
        if (path == null) {
            return "";
        }
        if (gameDir != null) {
            try {
                return gameDir.relativize(path).toString().replace('\\', '/');
            } catch (IllegalArgumentException ignored) {
            }
        }
        return path.toString().replace('\\', '/');
    }

    private static Optional<CategoryTarget> categoryTargetFor(String groupKey) {
        if (groupKey == null || groupKey.isBlank()) return Optional.empty();

        String[] parts = groupKey.toLowerCase(Locale.ROOT).split("/");
        String categoryId = parts[0].trim();
        String subcategoryId = parts.length > 1 ? parts[1].trim() : "";
        if ("none".equals(subcategoryId)) {
            subcategoryId = "";
        }

        if (!AmiOntology.isDefinedCategoryId(categoryId)) {
            return Optional.empty();
        }

        AmiOntology.Category category = AmiOntology.categoryForId(categoryId);
        if (subcategoryId.isBlank() || hasSubcategory(category, subcategoryId)) {
            return Optional.of(new CategoryTarget(categoryId, subcategoryId));
        }
        return Optional.empty();
    }

    private static boolean hasSubcategory(AmiOntology.Category category, String subcategoryId) {
        if (category == null || subcategoryId == null || subcategoryId.isBlank()) return false;
        return category.subCategories.stream().anyMatch(subcategory -> subcategory.id().equals(subcategoryId));
    }

    private boolean hasRecipes(SearchNode node, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        try {
            Optional<Integer> metadataCount = positiveMetadataCount(node, SearchNodeKeys.RECIPE_OUTPUT_COUNT);
            if (metadataCount.isPresent() && metadataCount.get() > 0) return true;
            if (externalRecipeViewerAvailable.getAsBoolean()) return true;
            if (RecipeViewerBridge.hasRecipes(stack)) return true;
            if (metadataCount.isPresent()) return false;
            if (!Services.PLATFORM.isRecipeIndexBuilt()) return false;
            return !Services.PLATFORM.getRecipesFor(stack).isEmpty();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to query recipes for context menu stack " + stack, e);
            return false;
        }
    }

    private boolean hasUses(SearchNode node, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        try {
            Optional<Integer> metadataCount = positiveMetadataCount(node, SearchNodeKeys.RECIPE_USE_COUNT);
            if (metadataCount.isPresent() && metadataCount.get() > 0) return true;
            if (externalRecipeViewerAvailable.getAsBoolean()) return true;
            if (RecipeViewerBridge.hasUses(stack)) return true;
            if (metadataCount.isPresent()) return false;
            if (!Services.PLATFORM.isRecipeIndexBuilt()) return false;
            return !Services.PLATFORM.getUsesFor(stack).isEmpty();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to query uses for context menu stack " + stack, e);
            return false;
        }
    }

    private Optional<Integer> positiveMetadataCount(SearchNode node, String key) {
        if (node == null || key == null) return Optional.empty();

        String raw = node.meta(key, "");
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Math.max(0, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "AMI: Ignoring malformed recipe count metadata " + key + "=" + raw
                    + " for " + node.id(), e);
            return Optional.empty();
        }
    }

    private static boolean isFavorite(com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler favorites, SearchNode node) {
        if (favorites == null || node == null) return false;

        try {
            return favorites.isFavorite(node);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to read favorite state for context menu node " + node.id(), e);
            return false;
        }
    }

    private boolean canCraft(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        try {
            return craftable.test(stack);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Craft transfer unavailable for context menu stack " + stack, e);
            return false;
        }
    }

    static String chatText(SearchNode node) {
        if (node == null) return "";
        if (node.type() == NodeType.WAYPOINT) return waypointChatText(node);
        ResourceLocation resolved = resolvedItemId(node);
        if (resolved != null) return resolved.toString();
        String name = node.displayName();
        return name == null ? "" : name;
    }

    private static String waypointChatText(SearchNode node) {
        if (node == null) return "";
        String name = node.meta(SearchNodeKeys.WAYPOINT_NAME, node.displayName());
        String dimension = node.meta(SearchNodeKeys.WAYPOINT_DIMENSION, "minecraft:overworld");
        String x = node.meta(SearchNodeKeys.WAYPOINT_X, "");
        String y = node.meta(SearchNodeKeys.WAYPOINT_Y, "");
        String z = node.meta(SearchNodeKeys.WAYPOINT_Z, "");
        String provider = node.meta(SearchNodeKeys.WAYPOINT_PROVIDER_LABEL, "");
        String suffix = provider.isBlank() ? "" : " [" + provider + "]";
        return name + " @ " + dimension + " " + x + " " + y + " " + z + suffix;
    }

    private static boolean hasWaypointCoordinates(SearchNode node) {
        return node != null
                && !node.meta(SearchNodeKeys.WAYPOINT_X, "").isBlank()
                && !node.meta(SearchNodeKeys.WAYPOINT_Y, "").isBlank()
                && !node.meta(SearchNodeKeys.WAYPOINT_Z, "").isBlank();
    }

    private static ResourceLocation resolvedItemId(SearchNode node) {
        return FavoriteEntry.resolvedId(node);
    }

    private static String playerName(SearchNode node) {
        if (node == null) return "";
        String name = node.meta(SearchNodeKeys.PLAYER_NAME, "");
        if (!name.isBlank()) return name;
        return node.displayName() == null ? "" : node.displayName();
    }

    private static PlayerWaypointContext playerWaypointContext(SearchNode node) {
        return new PlayerWaypointContext(
                playerName(node),
                node == null ? "" : node.meta(SearchNodeKeys.PLAYER_UUID, ""),
                node == null ? Map.of() : node.metadata()
        );
    }

    private static void recordPlayerHeadHistory(SearchNode node) {
        String playerName = node == null ? "" : node.meta(SearchNodeKeys.PLAYER_HEAD_NAME, "");
        if (!playerName.isBlank()) {
            PlayerHeadHistory.record(playerName);
        }
    }

    private static SearchNode resolvedNodeForFix(SearchNode node) {
        if (node == null) return null;
        ResourceLocation resolved = resolvedItemId(node);
        if (resolved == null || resolved.equals(node.id())) return node;
        return new SearchNode(resolved, node.type(), node.displayName(), node.color(), node.searchWeight(), node.metadata());
    }

    private static void openChatDraft(String text) {
        if (text == null || text.isBlank()) return;

        try {
            // ChatScreen is a client-only class. A direct reference here would force the
            // bytecode verifier to load it whenever this xplat class is touched, which breaks
            // the client-stripped unit-test classpath. The Class.forName(Mojmap-name) approach
            // also failed on Fabric's intermediary runtime. So the screen open is delegated to a
            // platform seam, implemented with direct (Loom-remappable) calls in each loader module.
            com.sanhiruzu.ami.platform.Services.PLATFORM.openChatDraftScreen(text);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to open chat draft from context menu", e);
        }
    }

    private static boolean isPokemonSpecies(SearchNode node) {
        return node != null && "pokemon_species".equals(node.meta(SearchNodeKeys.ENTITY_CATEGORY, ""));
    }

    private static boolean isGregTechNode(SearchNode node) {
        if (node == null) {
            return false;
        }
        ResourceLocation id = node.id();
        return (id != null && ("gtceu".equals(id.getNamespace()) || "gregtech".equals(id.getNamespace())))
                || containsMetadataToken(node.meta(SearchNodeKeys.COMPAT_FAMILIES, ""), "gregtech")
                || "gregtech".equalsIgnoreCase(node.meta(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, ""))
                || "gregtech".equalsIgnoreCase(node.meta(SearchNodeKeys.COMPAT_FAMILY, ""));
    }

    private static boolean hasCobblemonPokedex() {
        try {
            return CobblemonPokedexOpener.hasPokedex();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Cobblemon Pokedex unavailable for context menu", e);
            return false;
        }
    }

    private static void openCobblemonPokedex(SearchNode node) {
        try {
            CobblemonPokedexOpener.handlePrimaryClick(node);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to open Cobblemon Pokedex for " + chatText(node), e);
        }
    }

    private static List<String> splitMetadataTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String part : raw.split("[,\\s]+")) {
            String value = part == null ? "" : part.trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static List<ResourceLocation> pokemonDropItemIds(SearchNode node) {
        if (node == null) {
            return List.of();
        }

        LinkedHashSet<ResourceLocation> values = new LinkedHashSet<>();
        for (String raw : node.meta(SearchNodeKeys.POKEMON_DROP_ITEM, "").split(",")) {
            ResourceLocation itemId = ResourceLocation.tryParse(raw.trim());
            if (isUsableItemId(itemId)) {
                values.add(itemId);
            }
        }
        return List.copyOf(values);
    }

    private static ItemStack stackForItem(ResourceLocation itemId) {
        if (!isUsableItemId(itemId)) {
            return ItemStack.EMPTY;
        }

        try {
            var item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            return defaultStackForItem(item, itemId);
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "AMI: Failed to resolve Pokemon drop item " + itemId, e);
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack defaultStackForItem(Object item, ResourceLocation itemId) {
        try {
            Object stack = item.getClass().getMethod("getDefaultInstance").invoke(item);
            if (stack instanceof ItemStack itemStack) {
                return itemStack;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            for (var constructor : ItemStack.class.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(item.getClass())) {
                    Object stack = constructor.newInstance(item);
                    return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
                }
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.FINE, "AMI: Failed to create Pokemon drop stack for " + itemId, e);
        }
        return ItemStack.EMPTY;
    }

    private static String itemDisplayName(ResourceLocation itemId) {
        return itemId == null ? "" : titleCaseUnderscorePath(itemId.getPath());
    }

    private static String dropSearchQuery(ResourceLocation itemId) {
        if (itemId == null) {
            return "";
        }
        if ("minecraft".equals(itemId.getNamespace())) {
            return itemId.getPath().replace('_', ' ');
        }
        return itemId.toString();
    }

    private static boolean isUsableItemId(ResourceLocation itemId) {
        if (itemId == null || itemId.getPath().isBlank()) {
            return false;
        }
        try {
            return BuiltInRegistries.ITEM.get(itemId) != Items.AIR;
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "AMI: Ignoring unresolved item id " + itemId, e);
            return false;
        }
    }

    private static boolean hasSpawnEgg(ResourceLocation entityId) {
        if (entityId == null) return false;

        try {
            ResourceLocation spawnEggId = ResourceLocation.fromNamespaceAndPath(
                    entityId.getNamespace(),
                    entityId.getPath() + "_spawn_egg"
            );
            return BuiltInRegistries.ITEM.get(spawnEggId) != Items.AIR;
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to resolve spawn egg for " + entityId, e);
            return false;
        }
    }

    private static boolean isCheatEnabled() {
        try {
            Class<?> cheatMode = Class.forName("com.sanhiruzu.ami.client.AMICheatMode");
            Object result = cheatMode.getMethod("isEnabled").invoke(null);
            return result instanceof Boolean enabled && enabled;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Cheat mode unavailable for context menu", e);
            return false;
        }
    }

    private static void openDocumentationTarget(SearchNode node, DocumentationTarget target) {
        if (target == null) return;
        if (target.kind() == DocumentationKind.AE2_GUIDE) {
            if (openAe2Guide(node)) {
                return;
            }
            openUri(node, target.fallbackUri());
            return;
        }
        if (target.kind() == DocumentationKind.SILENT_GEAR_MATERIAL_BOOK) {
            if (!AmiGuideOpeners.tryOpenSilentGearMaterialBook(null)) {
                openUri(node, target.fallbackUri());
            }
            return;
        }
        openUri(node, target.uri());
    }

    private static boolean openAe2Guide(SearchNode node) {
        AmiGuideSearchIndex guideIndex = AmiIndexerService.getInstance().getGuideSearchIndex();
        Optional<AmiGuideDocument> document = ae2GuideDocumentForNode(node, guideIndex);
        if (document.isPresent()) {
            AmiGuideDocument guideDocument = document.get();
            return AmiGuideOpeners.tryOpenGuideME(guideDocument.bookId(), guideDocument.pageId());
        }
        return AmiGuideOpeners.tryOpenGuideME(AE2_GUIDE_BOOK, null);
    }

    static Optional<AmiGuideDocument> ae2GuideDocumentForNode(SearchNode node, AmiGuideSearchIndex guideIndex) {
        ResourceLocation id = resolvedItemId(node);
        if (id == null || guideIndex == null || !"ae2".equals(id.getNamespace())) {
            return Optional.empty();
        }

        Optional<AmiGuideDocument> exactReference = guideIndex.allDocuments().stream()
                .filter(ResultContextMenuActionBuilder::isAe2GuideDocument)
                .filter(AmiGuideDocument::isVisible)
                .filter(document -> document.referencedItems().contains(id))
                .min((left, right) -> Integer.compare(ae2GuideDocumentScore(right, id), ae2GuideDocumentScore(left, id)));
        if (exactReference.isPresent()) {
            return exactReference;
        }

        String query = wikiQueryText(node);
        if (query.isBlank()) {
            query = id.getPath().replace('_', ' ');
        }
        return guideIndex.search(query).stream()
                .filter(ResultContextMenuActionBuilder::isAe2GuideDocument)
                .filter(AmiGuideDocument::isVisible)
                .findFirst();
    }

    private static boolean isAe2GuideDocument(AmiGuideDocument document) {
        return document != null && AE2_GUIDE_BOOK.equals(document.bookId()) && !document.pageId().isBlank();
    }

    private static int ae2GuideDocumentScore(AmiGuideDocument document, ResourceLocation itemId) {
        if (document == null || itemId == null) {
            return 0;
        }
        String path = itemId.getPath();
        String pageId = document.pageId();
        int score = 0;
        if (pageId.equals(path) || pageId.endsWith("/" + path)) {
            score += 100;
        }
        if (containsWord(document.title(), path)) {
            score += 40;
        }
        if (containsWord(document.chapter(), path)) {
            score += 10;
        }
        return score;
    }

    private static boolean containsWord(String text, String token) {
        String normalizedText = normalizeDocumentationText(text);
        String normalizedToken = normalizeDocumentationText(token);
        return !normalizedToken.isBlank() && normalizedText.contains(normalizedToken);
    }

    private static String normalizeDocumentationText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void openUri(SearchNode node, URI uri) {
        if (uri == null) return;
        if (!AmiConfig.confirmExternalLinks) {
            openUriDirect(node, uri);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                openUriDirect(node, uri);
            }
            minecraft.setScreen(screenAfterExternalLinkPrompt(parent));
        }, Component.translatable("ami.external_link.title"), Component.literal(uri.toString()), uri.toString(),
                Component.translatable("ami.external_link.cancel"), true));
    }

    private static Screen screenAfterExternalLinkPrompt(Screen parent) {
        if (parent instanceof AbstractContainerScreen<?>) {
            return null;
        }
        return parent;
    }

    private static void openUriDirect(SearchNode node, URI uri) {
        try {
            Util.getPlatform().openUri(uri);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to open documentation target from context menu for " + chatText(node), e);
        }
    }

    static Optional<URI> wikiUriFor(SearchNode node) {
        DocumentationTarget target = documentationTargetFor(node);
        return Optional.ofNullable(target.uri());
    }

    static DocumentationTarget documentationTargetFor(SearchNode node) {
        if (node == null) {
            return new DocumentationTarget(DocumentationKind.WEB_SEARCH, Component.translatable("ami.context.search_web"), null, null);
        }

        ResourceLocation id = resolvedItemId(node);
        String query = wikiQueryText(node);
        if (isPokemonSpecies(node)) {
            return new DocumentationTarget(
                    DocumentationKind.COBBLEMON_TOOLS,
                    Component.translatable("ami.context.open_cobblemon_tools"),
                    cobblemonPokemonToolsUri(node),
                    null
            );
        }

        if (query.isBlank()) {
            return new DocumentationTarget(DocumentationKind.WEB_SEARCH, Component.translatable("ami.context.search_web"), null, null);
        }

        if (id != null && "minecraft".equals(id.getNamespace())) {
            return new DocumentationTarget(
                    DocumentationKind.MINECRAFT_WIKI,
                    Component.translatable("ami.context.wiki"),
                    URI.create("https://minecraft.wiki/w/" + encodeWikiPath(wikiPageTitle(node, query))),
                    null
            );
        }

        if (id != null && "ae2".equals(id.getNamespace()) && Services.PLATFORM.isModLoaded("guideme")) {
            return new DocumentationTarget(
                    DocumentationKind.AE2_GUIDE,
                    Component.translatable("ami.context.open_ae2_guide"),
                    null,
                    ae2WebGuideUri(node, query)
            );
        }

        if (isSilentGearMaterialBookNode(node, id)) {
            return new DocumentationTarget(
                    DocumentationKind.SILENT_GEAR_MATERIAL_BOOK,
                    Component.translatable("ami.context.open_silentgear_material_book"),
                    null,
                    silentGearMaterialBookWikiUri()
            );
        }

        if (id != null && "mekanism".equals(id.getNamespace())) {
            return new DocumentationTarget(
                    DocumentationKind.MEKANISM_WIKI,
                    Component.translatable("ami.context.open_mekanism_wiki"),
                    URI.create("https://wiki.aidancbrady.com/wiki/" + encodeWikiPath(wikiPageTitle(node, query))),
                    null
            );
        }

        return new DocumentationTarget(
                DocumentationKind.WEB_SEARCH,
                Component.translatable("ami.context.search_web"),
                webSearchUri(node, query),
                null
        );
    }

    private static URI ae2WebGuideUri(SearchNode node, String query) {
        return URI.create("https://guide.appliedenergistics.org/1.21.1/?search="
                + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    private static boolean isSilentGearMaterialBookNode(SearchNode node, ResourceLocation id) {
        return SILENT_GEAR_MATERIAL_BOOK.equals(id)
                || (node != null
                && node.meta(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT)
                .contains("net.silentchaos512.gear.item.materialbookitem"))
                || (node != null
                && "silentgear_materials".equals(node.meta(SearchNodeKeys.GUIDE_BOOK_SYSTEM, ""))
                && containsMetadataToken(node.meta(SearchNodeKeys.FACETS, ""), "guide_book"));
    }

    private static URI silentGearMaterialBookWikiUri() {
        return URI.create("https://github.com/SilentChaos512/Silent-Gear/wiki");
    }

    private static URI cobblemonPokemonToolsUri(SearchNode node) {
        return URI.create("https://cobblemon.tools/pokedex/pokemon/" + encodePathSegment(cobblemonPokemonToolsSlug(node)));
    }

    private static String cobblemonPokemonToolsSlug(SearchNode node) {
        ResourceLocation speciesId = pokemonSpeciesId(node);
        String path = speciesId == null ? "" : speciesId.getPath();
        if (!path.isBlank()) {
            return path.toLowerCase(Locale.ROOT);
        }

        String fallback = node == null ? "" : node.displayName();
        return fallback.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static ResourceLocation pokemonSpeciesId(SearchNode node) {
        if (node == null) {
            return null;
        }

        String species = node.meta(SearchNodeKeys.POKEMON_SPECIES, "");
        if (!species.isBlank()) {
            ResourceLocation parsed = ResourceLocation.tryParse(species);
            if (parsed != null) return parsed;
        }

        ResourceLocation id = node.id();
        if (id != null && "cobblemon".equals(id.getNamespace())) {
            String path = id.getPath();
            if (path.startsWith("species/")) {
                return ResourceLocation.fromNamespaceAndPath("cobblemon", path.substring("species/".length()));
            }
        }

        return null;
    }

    private static URI webSearchUri(SearchNode node, String query) {
        String search = webSearchText(node, query);
        return URI.create("https://duckduckgo.com/?q=" + URLEncoder.encode(search, StandardCharsets.UTF_8));
    }

    private static String webSearchText(SearchNode node, String query) {
        List<String> terms = new ArrayList<>();
        String itemText = query == null ? "" : query.trim();
        if (!itemText.isBlank()) {
            terms.add(itemText);
        }

        String modText = modSearchText(node);
        if (!modText.isBlank() && terms.stream().noneMatch(term -> term.equalsIgnoreCase(modText))) {
            terms.add(modText);
        }

        return String.join(" ", terms);
    }

    private static String modSearchText(SearchNode node) {
        ResourceLocation id = resolvedItemId(node);
        String namespace = id == null ? "" : id.getNamespace();
        if (namespace.isBlank()) return "";

        try {
            String modName = Services.PLATFORM.getModName(namespace).orElse("");
            if (!modName.isBlank()) {
                return modName.trim();
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Mod name unavailable while building documentation search for " + namespace, e);
        }

        if (isGregTechNode(node)) {
            return "GregTech";
        }
        return namespace;
    }

    private static String wikiPageTitle(SearchNode node, String fallbackQuery) {
        String raw = fallbackQuery;
        ResourceLocation id = node.id();
        if ((raw == null || raw.isBlank()) && id != null) {
            raw = id.getPath();
        }
        raw = stripWikiTypeSuffix(raw, node.type());
        String normalized = raw == null ? "" : raw.trim().replace('-', '_').replace(' ', '_');
        if (normalized.isBlank() && id != null) {
            normalized = stripWikiTypeSuffix(id.getPath(), node.type()).replace('-', '_');
        }
        return titleCaseUnderscorePath(normalized);
    }

    private static String wikiQueryText(SearchNode node) {
        String query = node.displayName();
        if (query == null || query.isBlank()) {
            ResourceLocation id = node.id();
            query = id == null ? "" : id.getPath().replace('_', ' ');
        }
        return query == null ? "" : query.trim();
    }

    private static String stripWikiTypeSuffix(String value, NodeType type) {
        if (value == null) return "";

        String trimmed = value.trim();
        if (type == NodeType.BIOME) {
            return trimmed.replaceFirst("(?i)(?:[ _-]biome)$", "");
        }
        if (type == NodeType.STRUCTURE) {
            return trimmed.replaceFirst("(?i)(?:[ _-]structure)$", "");
        }
        if (type == NodeType.DIMENSION) {
            return trimmed.replaceFirst("(?i)(?:[ _-]dimension)$", "");
        }
        return trimmed;
    }

    private static String titleCaseUnderscorePath(String value) {
        if (value == null || value.isBlank()) return "";

        StringBuilder out = new StringBuilder(value.length());
        boolean capitalize = true;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '_') {
                out.append(c);
                capitalize = true;
            } else {
                out.append(capitalize ? Character.toUpperCase(c) : Character.toLowerCase(c));
                capitalize = false;
            }
        }
        return out.toString();
    }

    private static String formatGregTechTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return "";
        }
        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "steam" -> "Steam";
            case "ulv" -> "ULV";
            case "lv" -> "LV";
            case "mv" -> "MV";
            case "hv" -> "HV";
            case "ev" -> "EV";
            case "iv" -> "IV";
            case "luv" -> "LuV";
            case "zpm" -> "ZPM";
            case "uv" -> "UV";
            case "uhv" -> "UHV";
            case "uev" -> "UEV";
            case "uiv" -> "UIV";
            case "uxv" -> "UXV";
            case "opv" -> "OpV";
            case "max" -> "MAX";
            default -> titleCaseUnderscorePath(tier);
        };
    }

    private static boolean containsMetadataToken(String raw, String token) {
        if (raw == null || raw.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        for (String part : raw.split("[,\\s]+")) {
            if (part.trim().equalsIgnoreCase(normalizedToken)) {
                return true;
            }
        }
        return false;
    }

    private static String encodeWikiPath(String title) {
        if (title == null || title.isBlank()) return "";
        return URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "_");
    }

    private static String encodePathSegment(String value) {
        if (value == null || value.isBlank()) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void openRecipesLater(ItemStack stack) {
        runOnClient(() -> {
            ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
            if (!copy.isEmpty()) {
                RecipeViewerBridge.openRecipes(copy);
            }
        });
    }

    private static void craftOneLater(ItemStack stack) {
        runOnClient(() -> {
            ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
            if (!copy.isEmpty()) {
                RecipeViewerBridge.handleItemClick(copy, 0, false, false);
            }
        });
    }

    private static void craftStackLater(ItemStack stack) {
        runOnClient(() -> {
            ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
            if (!copy.isEmpty()) {
                RecipeViewerBridge.handleItemClick(copy, 0, true, false);
            }
        });
    }

    private static boolean canTransferCraft(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        try {
            return RecipeViewerBridge.canTransferStack(stack);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Unable to check craft transfer for context menu stack " + stack, e);
            return false;
        }
    }

    private static void openUsesLater(ItemStack stack) {
        runOnClient(() -> {
            ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
            if (!copy.isEmpty()) {
                RecipeViewerBridge.openUses(copy);
            }
        });
    }

    private static void openCategoryFixScreenLater(SearchNode node) {
        if (node == null) return;
        SearchNode resolved = resolvedNodeForFix(node);
        runOnClient(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new AmiCategoryFixScreen(minecraft.screen, resolved));
        });
    }

    private static void runOnClient(Runnable action) {
        if (action == null) return;

        try {
            Minecraft.getInstance().tell(action);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to schedule context menu action", e);
        }
    }

    private static void showClientStatus(Component message) {
        if (message == null) return;

        try {
            Object minecraft = Minecraft.getInstance();
            Object player = publicFieldValue(minecraft, "player");
            if (player != null) {
                player.getClass().getMethod("displayClientMessage", Component.class, boolean.class)
                        .invoke(player, message, true);
                return;
            }

            Object gui = publicFieldValue(minecraft, "gui");
            if (gui != null) {
                gui.getClass().getMethod("setOverlayMessage", Component.class, boolean.class)
                        .invoke(gui, message, false);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.FINE, "AMI: Failed to show context menu status", e);
        }
    }

    private static Object publicFieldValue(Object owner, String fieldName) throws IllegalAccessException {
        if (owner == null || fieldName == null || fieldName.isBlank()) return null;
        try {
            return owner.getClass().getField(fieldName).get(owner);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public record ItemContext(
            SearchNode node,
            ItemStack stack,
            com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler favorites,
            Consumer<String> tokenInject,
            Runnable onDataFixApplied,
            Consumer<SearchNode> openSources,
            Consumer<SearchNode> openEntityDetails
    ) {
        public ItemContext(SearchNode node,
                           ItemStack stack,
                           com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler favorites,
                           Consumer<String> tokenInject) {
            this(node, stack, favorites, tokenInject, null, null, null);
        }

        public ItemContext(SearchNode node,
                           ItemStack stack,
                           com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler favorites,
                           Consumer<String> tokenInject,
                           Runnable onDataFixApplied) {
            this(node, stack, favorites, tokenInject, onDataFixApplied, null, null);
        }

        public ItemContext(SearchNode node,
                           ItemStack stack,
                           com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler favorites,
                           Consumer<String> tokenInject,
                           Runnable onDataFixApplied,
                           Consumer<SearchNode> openSources) {
            this(node, stack, favorites, tokenInject, onDataFixApplied, openSources, null);
        }
    }

    public record GroupContext(
            TreeNode node,
            Consumer<String> tokenInject,
            Runnable onTreeChanged,
            Runnable onDataFixApplied
    ) {
        public GroupContext(TreeNode node, Consumer<String> tokenInject, Runnable onTreeChanged) {
            this(node, tokenInject, onTreeChanged, null);
        }
    }

    private record CategoryTarget(String categoryId, String subcategoryId) {
    }

    private record TaxonomyTemplateTarget(
            String categoryId,
            String subcategoryId,
            String categoryLabel,
            String subcategoryLabel,
            boolean categoryNeedsDefinition
    ) {
    }

    record PackAuthorExportFile(Path relativePath, String content) {
        PackAuthorExportFile {
            relativePath = relativePath == null ? Path.of("ami-export.txt") : relativePath;
            content = content == null ? "" : content;
        }
    }

    enum DocumentationKind {
        MINECRAFT_WIKI,
        AE2_GUIDE,
        SILENT_GEAR_MATERIAL_BOOK,
        MEKANISM_WIKI,
        COBBLEMON_TOOLS,
        WEB_SEARCH
    }

    record DocumentationTarget(DocumentationKind kind, Component label, URI uri, URI fallbackUri) {
    }
}
