package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMICheatMode;
import com.sanhiruzu.ami.api.AmiContextMenuAction;
import com.sanhiruzu.ami.api.AmiItemContext;
import com.sanhiruzu.ami.api.AmiPluginRegistry;
import com.sanhiruzu.ami.api.AmiQuestItemMatch;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.api.IAmiPlugin;
import com.sanhiruzu.ami.author.PackAuthorDiagnostics;
import com.sanhiruzu.ami.client.screen.AmiCategoryFixScreen;
import com.sanhiruzu.ami.compat.CobblemonPokedexOpener;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.config.AmiDataFixes;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
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
    public static final String COPY_TOOLTIP = "ami:copy_tooltip";
    public static final String CRAFT_ONE = "ami:craft_one";
    public static final String CRAFT_STACK = "ami:craft_stack";
    public static final String RECIPES = "ami:recipes";
    public static final String USES = "ami:uses";
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
    public static final String FILTER_MOD = "ami:filter_mod";
    public static final String COPY_ID = "ami:copy_id";
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
    public static final String COPY_GAMESTAGE_CONDITION = "ami:copy_gamestage_condition";
    public static final String COPY_PACK_AUTHOR_REPORT = "ami:copy_pack_author_report";
    private static final int MAX_GROUP_AUTHORING_ITEMS = 64;
    private static final int MAX_GROUP_REPORT_ITEMS = 512;

    public static final Set<String> KNOWN_ACTIONS = Set.of(
            COPY_TOOLTIP, CRAFT_ONE, CRAFT_STACK, RECIPES, USES, FAVORITE, CHAT, WIKI, LOCATE,
            CHEAT_GIVE_ONE, CHEAT_GIVE_STACK, CHEAT_SPAWN_EGG, CHEAT_SPAWN_EGG_STACK,
            CHEAT_SPAWN_POKEMON, CHEAT_POKEMON_PARTY,
            OPEN_POKEDEX, FILTER_POKEMON_TYPE, FILTER_POKEMON_SECONDARY_TYPE,
            FILTER_POKEMON_GENERATION, FILTER_POKEMON_EGG_GROUP, FILTER_POKEMON_ABILITY,
            SEARCH_POKEMON_DROP_ITEM, RECIPES_POKEMON_DROP_ITEM,
            COPY_POKEMON_SPECIES, COPY_POKEMON_DEX_NUMBER,
            FILTER_MOD, COPY_ID,
            GROUP_TOGGLE, FILTER_CATEGORY, COPY_GROUP_KEY,
            START_CATEGORY_FIX, EDIT_CATEGORY_FIX, APPLY_CATEGORY_FIX, CLEAR_ITEM_FIX,
            QUESTS_FOR_ITEM, OPEN_QUEST, COPY_QUEST_MATCHES,
            COPY_FTB_ITEM_TASK, COPY_FTB_QUEST_SKELETON, COPY_KUBEJS_RECIPE_STUB, COPY_GAMESTAGE_CONDITION,
            COPY_PACK_AUTHOR_REPORT
    );
    public static final String DEFAULT_ACTIONS = String.join(",",
            COPY_TOOLTIP,
            CRAFT_ONE,
            CRAFT_STACK,
            RECIPES,
            USES,
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

    private final BooleanSupplier cheatEnabled;
    private final Predicate<ItemStack> craftable;
    private final Supplier<List<IAmiPlugin>> pluginSupplier;

    public ResultContextMenuActionBuilder() {
        this(ResultContextMenuActionBuilder::isCheatEnabled, ResultContextMenuActionBuilder::canTransferCraft, AmiPluginRegistry::getPlugins);
    }

    ResultContextMenuActionBuilder(BooleanSupplier cheatEnabled) {
        this(cheatEnabled, ResultContextMenuActionBuilder::canTransferCraft, AmiPluginRegistry::getPlugins);
    }

    ResultContextMenuActionBuilder(BooleanSupplier cheatEnabled, Predicate<ItemStack> craftable) {
        this(cheatEnabled, craftable, AmiPluginRegistry::getPlugins);
    }

    ResultContextMenuActionBuilder(BooleanSupplier cheatEnabled, Predicate<ItemStack> craftable,
                                   Supplier<List<IAmiPlugin>> pluginSupplier) {
        this.cheatEnabled = cheatEnabled == null ? () -> false : cheatEnabled;
        this.craftable = craftable == null ? stack -> false : craftable;
        this.pluginSupplier = pluginSupplier == null ? List::of : pluginSupplier;
    }

    public List<ResultContextMenu.Action> forItem(ItemContext context) {
        List<ResultContextMenu.Action> actions = new ArrayList<>();
        if (context == null || context.node() == null) return actions;
        ResultContextMenuActionPolicy policy = ResultContextMenuActionPolicy.fromConfig();

        SearchNode node = context.node();
        ResourceLocation id = node.id();
        ItemStack stack = context.stack() == null ? ItemStack.EMPTY : context.stack().copy();
        boolean hasStack = !stack.isEmpty();
        List<AmiQuestItemMatch> questMatches = node.type() == NodeType.ITEM
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
                        AmiDataFixes.removeUserFix(id, node.type());
                        AmiIndexerService.getInstance().rebuild();
                    }
            ));
        }

        if (policy.allows(node, LOCATE) && id != null && node.type() == NodeType.BIOME && AMICheatMode.isEnabled()) {
            actions.add(ResultContextMenu.Action.enabled(
                    LOCATE,
                    Component.translatable("ami.context.locate_biome"),
                    'n',
                    () -> AMICheatMode.locateBiome(id)
            ));
        } else if (policy.allows(node, LOCATE) && id != null && node.type() == NodeType.STRUCTURE && AMICheatMode.isEnabled()) {
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

        return actions;
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
            return;
        }
        if (plugins == null || plugins.isEmpty()) return;

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
                        () -> AMICheatMode.giveItem(stack)
                ));
            }
            if (policy.allows(node, CHEAT_GIVE_STACK)) {
                actions.add(ResultContextMenu.Action.enabled(
                        CHEAT_GIVE_STACK,
                        Component.translatable("ami.context.cheat_give_stack"),
                        's',
                        () -> AMICheatMode.giveStack(stack)
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
        if (isPackAuthorMode() && !authoringItems.isEmpty() && policy.allowsDevGroup(node, COPY_FTB_QUEST_SKELETON)) {
            boolean capped = countAuthoringItems(node, MAX_GROUP_AUTHORING_ITEMS + 1) > MAX_GROUP_AUTHORING_ITEMS;
            actions.add(ResultContextMenu.Action.enabled(
                    COPY_FTB_QUEST_SKELETON,
                    Component.translatable("ami.context.copy_ftb_quest_skeleton"),
                    's',
                    () -> AmiClipboardHelper.copyToClipboard(ftbQuestSkeleton(groupTitle(node), authoringItems, capped))
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
                        AmiDataFixes.putUserMetadataFix(pending.id(), pending.type(), metadata);
                        SearchNode updated = pending.withMetadata(AmiDataFixes.apply(pending.id(), pending.type(), pending.metadata()));
                        GlobalIndex.getInstance().replaceNode(pending.id(), pending.type(), updated);
                        pendingCategoryFixNode = null;
                        AmiIndexerService.getInstance().rebuild();
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
        pendingCategoryFixNode = node;
        if (node != null) {
            showClientStatus(Component.translatable("ami.context.category_fix_selected", node.displayName()));
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

    private static Optional<CategoryTarget> categoryTargetFor(String groupKey) {
        if (groupKey == null || groupKey.isBlank()) return Optional.empty();

        String[] parts = groupKey.toLowerCase(Locale.ROOT).split("/");
        String categoryId = parts[0].trim();
        String subcategoryId = parts.length > 1 ? parts[1].trim() : "";
        if ("none".equals(subcategoryId)) {
            subcategoryId = "";
        }

        for (AmiOntology.Category category : AmiOntology.CATEGORIES) {
            if (!category.id.equals(categoryId)) continue;
            if (subcategoryId.isBlank() || hasSubcategory(category, subcategoryId)) {
                return Optional.of(new CategoryTarget(categoryId, subcategoryId));
            }
            return Optional.empty();
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
            if (metadataCount.isPresent()) return metadataCount.get() > 0;
            if (!Services.PLATFORM.isRecipeIndexBuilt()) return false;
            return !Services.PLATFORM.getRecipesFor(stack).isEmpty();
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to query recipes for context menu stack " + stack, e);
            return false;
        }
    }

    private boolean hasUses(SearchNode node, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        try {
            Optional<Integer> metadataCount = positiveMetadataCount(node, SearchNodeKeys.RECIPE_USE_COUNT);
            if (metadataCount.isPresent()) return metadataCount.get() > 0;
            if (!Services.PLATFORM.isRecipeIndexBuilt()) return false;
            return !Services.PLATFORM.getUsesFor(stack).isEmpty();
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to query uses for context menu stack " + stack, e);
            return false;
        }
    }

    private Optional<Integer> positiveMetadataCount(SearchNode node, String key) {
        if (node == null || key == null) return Optional.empty();

        String raw = node.meta(key, "");
        if (raw == null || raw.isBlank()) {
            return node.type() == NodeType.ITEM ? Optional.of(0) : Optional.empty();
        }
        try {
            return Optional.of(Math.max(0, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "AMI: Ignoring malformed recipe count metadata " + key + "=" + raw
                    + " for " + node.id(), e);
            return Optional.empty();
        }
    }

    private boolean isFavorite(com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler favorites, SearchNode node) {
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

    private static String chatText(SearchNode node) {
        if (node == null) return "";
        if (node.id() != null) return node.id().toString();
        String name = node.displayName();
        return name == null ? "" : name;
    }

    private static void openChatDraft(String text) {
        if (text == null || text.isBlank()) return;

        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
            Class<?> chatScreenClass = Class.forName("net.minecraft.client.gui.screens.ChatScreen");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            Object chatScreen = chatScreenClass.getConstructor(String.class).newInstance(text);
            minecraftClass.getMethod("setScreen", screenClass).invoke(minecraft, chatScreen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to open chat draft from context menu", e);
        }
    }

    private static boolean isPokemonSpecies(SearchNode node) {
        return node != null && "pokemon_species".equals(node.meta(SearchNodeKeys.ENTITY_CATEGORY, ""));
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
            if (openAe2Guide()) {
                return;
            }
            openUri(node, target.fallbackUri());
            return;
        }
        openUri(node, target.uri());
    }

    private static boolean openAe2Guide() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return false;

            Class<?> guidesCommon = Class.forName("guideme.GuidesCommon");
            for (var method : guidesCommon.getMethods()) {
                if (!method.getName().equals("openGuide") || method.getParameterCount() != 2) continue;
                method.invoke(null, minecraft.player, Services.PLATFORM.rl("ae2", "guide"));
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: GuideME unavailable for AE2 documentation action", e);
        }
        return false;
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

        ResourceLocation id = node.id();
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
        ResourceLocation id = node == null ? null : node.id();
        String modText = id == null ? "" : modMetadataText(id.getNamespace());
        String search = query;
        if (id != null) {
            search += " " + id;
        }
        if (!modText.isBlank()) {
            search += " " + modText;
        }
        search += " minecraft mod";
        return URI.create("https://duckduckgo.com/?q=" + URLEncoder.encode(search, StandardCharsets.UTF_8));
    }

    private static String modMetadataText(String namespace) {
        if (namespace == null || namespace.isBlank()) return "";
        try {
            return Services.PLATFORM.getModMetadataText(namespace).orElse(namespace);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Mod metadata unavailable while building documentation search for " + namespace, e);
            return namespace;
        }
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
        runOnClient(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new AmiCategoryFixScreen(minecraft.screen, node));
        });
    }

    private static void runOnClient(Runnable action) {
        if (action == null) return;

        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            minecraftClass.getMethod("tell", Runnable.class).invoke(minecraft, action);
        } catch (ReflectiveOperationException | RuntimeException e) {
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
            Consumer<String> tokenInject
    ) {
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

    enum DocumentationKind {
        MINECRAFT_WIKI,
        AE2_GUIDE,
        MEKANISM_WIKI,
        COBBLEMON_TOOLS,
        WEB_SEARCH
    }

    record DocumentationTarget(DocumentationKind kind, Component label, URI uri, URI fallbackUri) {
    }
}
