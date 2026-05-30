package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

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
    private ItemFilter() {
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
     * Returns the set of all items that appear as the output of at least one registered recipe.
     */
    public static Set<Item> buildRecipeOutputSet(net.minecraft.world.level.Level level) {
        if (level == null) return Collections.emptySet();
        Set<Item> items = new HashSet<>();
        try {
            for (var holder : level.getRecipeManager().getRecipes()) {
                try {
                    ItemStack stack = Services.PLATFORM.getRecipeResultItem(holder, level.registryAccess());
                    if (!stack.isEmpty()) items.add(stack.getItem());
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            AmiCore.LOGGER.warn("AMI: Could not build recipe output set - recipe filter disabled. ({})", e.getMessage());
        }
        return items;
    }

    public static String classifyAccessLevel(ResourceLocation id, boolean inCreative) {
        return classifyAccessLevel(id, null, inCreative);
    }

    public static String classifyAccessLevel(ResourceLocation id, Item item, boolean inCreative) {
        String path = id.getPath();

        // Explicitly hidden/technical items are always dev-only
        if (path.contains("debug") || path.contains("test_") || path.contains("fireball")
                || path.contains("effect") || path.contains("particle")) return ACCESS_DEV;

        // Special restricted items
        if (isSpawnEgg(id, item) || path.contains("spawner")) return ACCESS_CREATIVE;
        if (path.contains("command_block") || path.equals("structure_block") || path.equals("barrier"))
            return ACCESS_CHEAT;

        // Items not in creative tabs get ACCESS_DEV by default, but this can be overridden
        if (!inCreative) return ACCESS_DEV;

        return ACCESS_SURVIVAL;
    }

    private static boolean isSpawnEgg(ResourceLocation id, Item item) {
        if (item instanceof SpawnEggItem) {
            return true;
        }
        String path = id.getPath();
        return path.endsWith("_spawn_egg")
                || path.startsWith("spawn_egg_")
                || path.equals("spawn_egg");
    }

    public static boolean shouldShowAccessLevel(String accessLevel) {
        return switch (accessLevel) {
            case ACCESS_SURVIVAL -> true;
            case ACCESS_CREATIVE -> AmiConfig.showSpawnEggs
                    || AmiConfig.devMode
                    || AmiConfig.cheatMode;
            case ACCESS_CHEAT -> AmiConfig.cheatMode || AmiConfig.devMode;
            case ACCESS_DEV -> AmiConfig.devMode || AmiConfig.showHiddenModItems;
            default -> false;
        };
    }

    public record CreativeTabInfo(String id, String label) {
    }

    /**
     * Internal class to prevent ClientLevel class loading on Dedicated Server.
     */
    private static class ClientItemFilter {
        private static Map<Item, CreativeTabInfo> buildCreativeTabMap(net.minecraft.world.level.Level level) {
            if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
                return Collections.emptyMap();
            }
            Map<Item, CreativeTabInfo> items = new LinkedHashMap<>();
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
                    ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                    String tabKey = tabId != null ? tabId.toString() : tab.getDisplayName().getString();
                    String tabLabel = tab.getDisplayName().getString();
                    for (ItemStack stack : tab.getDisplayItems()) {
                        if (!stack.isEmpty()) {
                            items.putIfAbsent(stack.getItem(), new CreativeTabInfo(tabKey, tabLabel));
                        }
                    }
                }
            } catch (Exception e) {
                AmiCore.LOGGER.warn("AMI: Failed to build creative tab map: {}", e.getMessage());
            }
            return items;
        }
    }
}
