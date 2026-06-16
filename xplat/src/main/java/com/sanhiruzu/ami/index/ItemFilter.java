package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.GameType;

import java.util.*;

/**
 * Utility for filtering items based on creative-tab membership and mod configuration.
 * Side-safe: logic that depends on ClientLevel is deferred to a nested class.
 */
public final class ItemFilter {
    public static final String ACCESS_SURVIVAL = "survival";
    public static final String ACCESS_CREATIVE = "creative";
    public static final String ACCESS_CHEAT = "cheat";
    public static final String ACCESS_DEV = "dev";

    // Creative tab snapshot captured on the main thread before background indexing.
    // tab.buildContents() fires NeoForge events that mod authors expect on the main thread;
    // background callers read this field rather than calling buildContents() themselves.
    private static volatile Map<Item, List<CreativeStackInfo>> mainThreadCreativeSnapshot = Collections.emptyMap();

    private ItemFilter() {
    }

    /**
     * Captures creative tab data on the calling (main) thread so background indexing can use it
     * without calling tab.buildContents() off-thread. Must be called before background indexing begins.
     */
    public static void captureCreativeTabSnapshot(@org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level) {
        if (!Services.PLATFORM.isClient()) {
            mainThreadCreativeSnapshot = Collections.emptyMap();
            return;
        }
        mainThreadCreativeSnapshot = ClientItemFilter.buildCreativeStackMap(level);
    }

    /**
     * Returns the set of all items that appear in at least one standard creative tab.
     * On a dedicated server, this always returns an empty set.
     */
    public static Set<Item> buildCreativeItemSet(net.minecraft.world.level.Level level) {
        return buildCreativeTabMap(level).keySet();
    }

    /**
     * Returns the first standard creative tab each item is registered under.
     * On a dedicated server, this always returns an empty map.
     */
    public static Map<Item, CreativeTabInfo> buildCreativeTabMap(net.minecraft.world.level.Level level) {
        if (Services.PLATFORM.isClient()) {
            return ClientItemFilter.buildCreativeTabMap(level);
        }
        return Collections.emptyMap();
    }

    /**
     * Returns every displayed creative-tab stack grouped by item. Unlike
     * buildCreativeTabMap, this preserves multiple ItemStack component variants
     * for the same registered item.
     *
     * <p>Must only be called on the main client thread (tab.buildContents fires NeoForge events).
     * Background indexing threads receive the snapshot captured by {@link #captureCreativeTabSnapshot}.
     */
    public static Map<Item, List<CreativeStackInfo>> buildCreativeStackMap(net.minecraft.world.level.Level level) {
        if (!Services.PLATFORM.isClient()) return Collections.emptyMap();
        if (!ClientItemFilter.isMainClientThread()) {
            Map<Item, List<CreativeStackInfo>> snapshot = mainThreadCreativeSnapshot;
            if (snapshot.isEmpty()) {
                AmiCore.LOGGER.warn("AMI: buildCreativeStackMap called from background thread before captureCreativeTabSnapshot; creative data will be empty");
            }
            return snapshot;
        }
        return ClientItemFilter.buildCreativeStackMap(level);
    }

    public static Map<Item, CreativeTabInfo> firstCreativeTabs(Map<Item, List<CreativeStackInfo>> stackMap) {
        Map<Item, CreativeTabInfo> result = new LinkedHashMap<>();
        for (var entry : stackMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                result.put(entry.getKey(), entry.getValue().get(0).tab());
            }
        }
        return result;
    }

    public static Optional<ItemStack> firstCreativeStack(Item item, Map<Item, List<CreativeStackInfo>> stackMap) {
        List<CreativeStackInfo> stacks = stackMap.get(item);
        if (stacks == null || stacks.isEmpty()) {
            return Optional.empty();
        }
        for (CreativeStackInfo info : stacks) {
            if (info == null || info.stack() == null || info.stack().isEmpty()) {
                continue;
            }
            ItemStack stack = info.stack().copy();
            stack.setCount(1);
            return Optional.of(stack);
        }
        return Optional.empty();
    }

    static boolean appendCreativeStack(Map<Item, List<CreativeStackInfo>> items,
                                       Map<Item, List<ItemStack>> seenStacks,
                                       ItemStack rawStack,
                                       CreativeTabInfo tabInfo) {
        if (rawStack == null || rawStack.isEmpty()) {
            return false;
        }
        ItemStack stack = rawStack.copy();
        stack.setCount(1);
        Item item = stack.getItem();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        List<CreativeStackInfo> itemStacks = items.computeIfAbsent(item, ignored -> new ArrayList<>());
        List<ItemStack> seenForItem = seenStacks.computeIfAbsent(item, ignored -> new ArrayList<>());
        for (ItemStack seen : seenForItem) {
            if (Services.PLATFORM.sameItemSameComponents(seen, stack)) {
                return false;
            }
        }
        if (IndexingHotItemPolicy.shouldCollapseCreativeStacks(itemId) && !itemStacks.isEmpty()) {
            return false;
        }
        seenForItem.add(stack.copy());
        itemStacks.add(new CreativeStackInfo(stack, tabInfo));
        return true;
    }

    /**
     * Returns the set of all items that appear as the output of at least one registered recipe.
     */
    public static Set<Item> buildRecipeOutputSet(net.minecraft.world.level.Level level) {
        if (level == null) return Collections.emptySet();
        Set<Item> items = new HashSet<>();
        try {
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                for (var holder : sl.getServer().getRecipeManager().getRecipes()) {
                    try {
                        ItemStack stack = Services.PLATFORM.getRecipeResultItem(holder, level.registryAccess());
                        if (!stack.isEmpty()) items.add(stack.getItem());
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            AmiCore.LOGGER.warn("AMI: Could not build recipe output set - recipe filter disabled. ({})", e.getMessage());
        }
        return items;
    }

    public static String classifyAccessLevel(Identifier id, boolean inCreative) {
        return classifyAccessLevel(id, null, inCreative);
    }

    public static String classifyAccessLevel(Identifier id, Item item, boolean inCreative) {
        String path = id.getPath();

        // Explicitly hidden/technical items are always dev-only
        if (path.contains("debug") || path.contains("test_") || path.contains("fireball")) return ACCESS_DEV;

        // Items whose path signals removal or deprecation should be cheat-only
        if (path.contains("removed") || path.contains("deprecated")) return ACCESS_CHEAT;

        // Unimplemented/deprecated mod namespaces
        if ("unimplemented_items".equals(id.getNamespace())) return ACCESS_CHEAT;

        // Special restricted items
        if (isCreativeOnlyPath(path)) return ACCESS_CREATIVE;
        if (isSpawnEgg(id, item) || path.contains("spawner")) return ACCESS_CREATIVE;
        if (path.contains("command_block") || path.equals("structure_block") || path.equals("barrier"))
            return ACCESS_CHEAT;

        // Items not in creative tabs get ACCESS_DEV by default, but this can be overridden
        if (!inCreative) return ACCESS_DEV;

        return ACCESS_SURVIVAL;
    }

    private static boolean isCreativeOnlyPath(String path) {
        return path.equals("creative")
                || path.startsWith("creative_")
                || path.endsWith("_creative")
                || path.contains("_creative_");
    }

    private static boolean isSpawnEgg(Identifier id, Item item) {
        if (item instanceof SpawnEggItem) {
            return true;
        }
        String path = id.getPath();
        return path.endsWith("_spawn_egg")
                || path.startsWith("spawn_egg_")
                || path.equals("spawn_egg");
    }

    /**
     * Returns true when the item carries a standard tag signalling it should be hidden
     * from recipe viewers and item indexes, such as {@code c:hidden_from_recipe_viewers}.
     * Both Fabric and NeoForge/Forge mods use this convention.
     */
    public static boolean isHiddenByRecipeViewerConvention(String tags) {
        if (tags == null || tags.isBlank()) return false;
        return tags.contains("c:hidden_from_recipe_viewers")
                || tags.contains("c:hidden");
    }

    public static boolean shouldShowAccessLevel(String accessLevel) {
        return shouldShowAccessLevel(accessLevel, null);
    }

    public static boolean shouldShowAccessLevel(String accessLevel, GameType gameMode) {
        return switch (accessLevel) {
            case ACCESS_SURVIVAL -> true;
            case ACCESS_CREATIVE -> AmiConfig.shouldShowCreativeItems(gameMode);
            case ACCESS_CHEAT -> AmiConfig.cheatMode || AmiConfig.devMode;
            case ACCESS_DEV -> AmiConfig.devMode || AmiConfig.showHiddenModItems;
            default -> false;
        };
    }

    public record CreativeTabInfo(String id, String label) {
    }

    public record CreativeStackInfo(ItemStack stack, CreativeTabInfo tab) {
    }

    /**
     * Internal class to prevent ClientLevel class loading on Dedicated Server.
     */
    private static class ClientItemFilter {
        static boolean isMainClientThread() {
            try {
                return net.minecraft.client.Minecraft.getInstance().isSameThread();
            } catch (Exception e) {
                return true;
            }
        }

        private static Map<Item, CreativeTabInfo> buildCreativeTabMap(net.minecraft.world.level.Level level) {
            return firstCreativeTabs(buildCreativeStackMap(level));
        }

        private static Map<Item, List<CreativeStackInfo>> buildCreativeStackMap(net.minecraft.world.level.Level level) {
            if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
                return Collections.emptyMap();
            }
            Map<Item, List<CreativeStackInfo>> items = new LinkedHashMap<>();
            Map<Item, List<ItemStack>> seenStacks = new LinkedHashMap<>();
            long started = System.currentTimeMillis();
            int displayStacks = 0;
            int searchDisplayStacks = 0;
            int copiedStacks = 0;
            int collapsedHotStacks = 0;
            try {
                var params = new CreativeModeTab.ItemDisplayParameters(
                        clientLevel.enabledFeatures(), false, clientLevel.registryAccess()
                );
                for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                    CreativeModeTab.Type type = tab.getType();
                    if (type == CreativeModeTab.Type.SEARCH
                            || type == CreativeModeTab.Type.HOTBAR
                            || type == CreativeModeTab.Type.INVENTORY) continue;

                    tab.buildContents(params);
                    Identifier tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                    String tabKey = tabId != null ? tabId.toString() : tab.getDisplayName().getString();
                    String tabLabel = tab.getDisplayName().getString();
                    CreativeTabInfo tabInfo = new CreativeTabInfo(tabKey, tabLabel);
                    for (ItemStack stack : tab.getDisplayItems()) {
                        if (!stack.isEmpty()) {
                            displayStacks++;
                            if (appendCreativeStack(items, seenStacks, stack, tabInfo)) {
                                copiedStacks++;
                            } else {
                                Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                                if (IndexingHotItemPolicy.shouldCollapseCreativeStacks(itemId)) {
                                    collapsedHotStacks++;
                                }
                            }
                        }
                    }
                    Collection<ItemStack> searchTabDisplayItems = tab.getSearchTabDisplayItems();
                    if (!tab.getDisplayItems().equals(searchTabDisplayItems)) {
                        for (ItemStack stack : searchTabDisplayItems) {
                            if (!stack.isEmpty()) {
                                searchDisplayStacks++;
                                if (appendCreativeStack(items, seenStacks, stack, tabInfo)) {
                                    copiedStacks++;
                                } else {
                                    Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                                    if (IndexingHotItemPolicy.shouldCollapseCreativeStacks(itemId)) {
                                        collapsedHotStacks++;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                AmiCore.LOGGER.warn("AMI: Failed to build creative tab map: {}", e.getMessage());
            }
            AmiCore.LOGGER.info("AMI indexing: creative tabs built in {}ms (items={} displayStacks={} searchDisplayStacks={} copiedStacks={} collapsedHotStacks={})",
                    System.currentTimeMillis() - started, items.size(), displayStacks, searchDisplayStacks, copiedStacks, collapsedHotStacks);
            return items;
        }

    }
}
