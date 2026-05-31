package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMICheatMode;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
    public static final String FILTER_MOD = "ami:filter_mod";
    public static final String COPY_ID = "ami:copy_id";
    public static final String GROUP_TOGGLE = "ami:group_toggle";
    public static final String FILTER_CATEGORY = "ami:filter_category";
    public static final String COPY_GROUP_KEY = "ami:copy_group_key";

    public static final Set<String> KNOWN_ACTIONS = Set.of(
            COPY_TOOLTIP, CRAFT_ONE, CRAFT_STACK, RECIPES, USES, FAVORITE, CHAT, WIKI, LOCATE,
            CHEAT_GIVE_ONE, CHEAT_GIVE_STACK, CHEAT_SPAWN_EGG, CHEAT_SPAWN_EGG_STACK,
            CHEAT_SPAWN_POKEMON, CHEAT_POKEMON_PARTY,
            FILTER_MOD, COPY_ID,
            GROUP_TOGGLE, FILTER_CATEGORY, COPY_GROUP_KEY
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
            GROUP_TOGGLE,
            FILTER_CATEGORY,
            COPY_GROUP_KEY
    );

    private final BooleanSupplier cheatEnabled;
    private final Predicate<ItemStack> craftable;

    public ResultContextMenuActionBuilder() {
        this(ResultContextMenuActionBuilder::isCheatEnabled, ResultContextMenuActionBuilder::canTransferCraft);
    }

    ResultContextMenuActionBuilder(BooleanSupplier cheatEnabled) {
        this(cheatEnabled, ResultContextMenuActionBuilder::canTransferCraft);
    }

    ResultContextMenuActionBuilder(BooleanSupplier cheatEnabled, Predicate<ItemStack> craftable) {
        this.cheatEnabled = cheatEnabled == null ? () -> false : cheatEnabled;
        this.craftable = craftable == null ? stack -> false : craftable;
    }

    public List<ResultContextMenu.Action> forItem(ItemContext context) {
        List<ResultContextMenu.Action> actions = new ArrayList<>();
        if (context == null || context.node() == null) return actions;
        ResultContextMenuActionPolicy policy = ResultContextMenuActionPolicy.fromConfig();

        SearchNode node = context.node();
        ResourceLocation id = node.id();
        ItemStack stack = context.stack() == null ? ItemStack.EMPTY : context.stack().copy();
        boolean hasStack = !stack.isEmpty();

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

        if (cheatEnabled.getAsBoolean()) {
            addCheatActions(actions, policy, node, id, stack, hasStack);
        }

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
            actions.add(ResultContextMenu.Action.enabled(
                    WIKI,
                    Component.translatable("ami.context.wiki"),
                    'w',
                    () -> openWikiSearch(node)
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

        return actions;
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

    private static void openWikiSearch(SearchNode node) {
        try {
            Optional<URI> uri = wikiUriFor(node);
            if (uri.isEmpty()) return;
            Class<?> utilClass = Class.forName("net.minecraft.Util");
            Object platform = utilClass.getMethod("getPlatform").invoke(null);
            platform.getClass()
                    .getMethod("openUri", URI.class)
                    .invoke(platform, uri.get());
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to open wiki search from context menu for " + chatText(node), e);
        }
    }

    static Optional<URI> wikiUriFor(SearchNode node) {
        if (node == null) return Optional.empty();

        ResourceLocation id = node.id();
        String query = wikiQueryText(node);
        if (query.isBlank()) return Optional.empty();

        if (id != null && "minecraft".equals(id.getNamespace())) {
            return Optional.of(URI.create("https://minecraft.wiki/w/" + encodeWikiPath(wikiPageTitle(node, query))));
        }

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return Optional.of(URI.create("https://minecraft.wiki/w/Special:Search?search=" + encoded));
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
            Runnable onTreeChanged
    ) {
    }
}
