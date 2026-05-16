package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Three-layer filter for the item index.
 *
 *  Layer 1 – access-level check: survival, creative, cheat, or dev.
 *  Layer 2 – creative-tab check: non-creative leftovers are tagged dev by default.
 *  Layer 3 – recipe-output check: opt-in (AMIConfig.STRICT_SURVIVAL_MODE).
 *             Items with no recipe are tagged OBTAINABILITY=no_recipe.
 */
public final class ItemFilter {
    public static final String ACCESS_SURVIVAL = "survival";
    public static final String ACCESS_CREATIVE = "creative";
    public static final String ACCESS_CHEAT = "cheat";
    public static final String ACCESS_DEV = "dev";

    /**
     * Items that are command/admin oriented rather than normal player-facing content.
     */
    public static final Set<String> CHEAT_ITEM_IDS = Set.of(
        "minecraft:command_block",
        "minecraft:chain_command_block",
        "minecraft:repeating_command_block",
        "minecraft:command_block_minecart",
        "minecraft:jigsaw",
        "minecraft:structure_block",
        "minecraft:structure_void",
        "minecraft:barrier",
        "minecraft:light",
        "minecraft:debug_stick",
        "minecraft:knowledge_book",
        "minecraft:bedrock",
        "minecraft:end_portal_frame",
        "minecraft:reinforced_deepslate",
        "minecraft:spawner"
    );

    /**
     * Internal placeholders and implementation-detail entries. Some of these
     * have generated subtype nodes; the base registry item is not useful to players.
     */
    public static final Set<String> DEV_ITEM_IDS = Set.of(
        "minecraft:air",
        "minecraft:petrified_oak_slab",
        "minecraft:infested_stone",
        "minecraft:infested_cobblestone",
        "minecraft:infested_stone_bricks",
        "minecraft:infested_mossy_stone_bricks",
        "minecraft:infested_cracked_stone_bricks",
        "minecraft:infested_chiseled_stone_bricks",
        "minecraft:infested_deepslate",
        "minecraft:potion",
        "minecraft:splash_potion",
        "minecraft:lingering_potion",
        "minecraft:tipped_arrow",
        "minecraft:enchanted_book",
        "minecraft:suspicious_stew"
    );

    private ItemFilter() {}

    /**
     * @deprecated Use {@link #classifyAccessLevel(ResourceLocation, boolean)} and
     * {@link #shouldShowAccessLevel(String)} for new filtering.
     */
    @Deprecated
    public static boolean isSystemItem(ResourceLocation id) {
        String accessLevel = classifyAccessLevel(id, true);
        return ACCESS_CHEAT.equals(accessLevel) || ACCESS_DEV.equals(accessLevel);
    }

    public static boolean isSpawnEgg(ResourceLocation id) {
        return id.getPath().endsWith("_spawn_egg");
    }

    public static String classifyAccessLevel(ResourceLocation id, boolean inCreative) {
        String key = id.toString();
        if (CHEAT_ITEM_IDS.contains(key)) return ACCESS_CHEAT;
        if (DEV_ITEM_IDS.contains(key)) return ACCESS_DEV;
        if (isSpawnEgg(id)) return ACCESS_CREATIVE;
        if (!inCreative) return ACCESS_DEV;
        return ACCESS_SURVIVAL;
    }

    public static boolean shouldShowAccessLevel(String accessLevel) {
        try {
            return switch (accessLevel) {
                case ACCESS_SURVIVAL -> true;
                case ACCESS_CREATIVE -> AMIConfig.SHOW_SPAWN_EGGS.get()
                        || AMIConfig.DEV_MODE.get()
                        || AMIConfig.CHEAT_MODE.get();
                case ACCESS_CHEAT -> AMIConfig.CHEAT_MODE.get() || AMIConfig.DEV_MODE.get();
                case ACCESS_DEV -> AMIConfig.DEV_MODE.get() || !AMIConfig.HIDE_NON_CREATIVE_ITEMS.get();
                default -> false;
            };
        } catch (IllegalStateException e) {
            // Configs might not be loaded during early GameTests
            return accessLevel.equals(ACCESS_SURVIVAL) || accessLevel.equals(ACCESS_CREATIVE);
        }
    }

    /**
     * @deprecated Use {@link #classifyAccessLevel(ResourceLocation, boolean)} and
     * {@link #shouldShowAccessLevel(String)} for new filtering.
     */
    @Deprecated
    public static boolean shouldHideSpawnEgg(ResourceLocation id) {
        return isSpawnEgg(id) && !shouldShowAccessLevel(ACCESS_CREATIVE);
    }

    /**
     * Returns the set of all items present in at least one standard creative-mode tab.
     * Skips meta-tabs (SEARCH_RESULT, HOTBAR, INVENTORY) since SEARCH_RESULT already
     * aggregates from all CATEGORY tabs and the other two reflect player inventory state.
     *
     * Returns an empty set on failure so callers treat the creative filter as disabled.
     */
    public static Set<Item> buildCreativeItemSet(ClientLevel level) {
        if (level == null) return Collections.emptySet();
        Set<Item> items = new HashSet<>();
        try {
            var params = new CreativeModeTab.ItemDisplayParameters(
                level.enabledFeatures(), false, level.registryAccess()
            );
            for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                CreativeModeTab.Type type = tab.getType();
                if (type == CreativeModeTab.Type.SEARCH
                        || type == CreativeModeTab.Type.HOTBAR
                        || type == CreativeModeTab.Type.INVENTORY) {
                    continue;
                }
                tab.buildContents(params);
                for (ItemStack stack : tab.getDisplayItems()) {
                    items.add(stack.getItem());
                }
            }
            AMI.LOGGER.debug("AMI: Creative item set built — {} unique items across all tabs.", items.size());
        } catch (Exception e) {
            AMI.LOGGER.warn("AMI: Could not build creative item set — programmatic filter disabled. ({})", e.getMessage());
        }
        return items;
    }

    /**
     * Returns the set of all items that appear as the output of at least one registered recipe.
     * Used to tag items as OBTAINABILITY=no_recipe when strict survival mode is active.
     *
     * Returns an empty set on failure so callers treat the recipe filter as disabled.
     */
    public static Set<Item> buildRecipeOutputSet(ClientLevel level) {
        if (level == null) return Collections.emptySet();
        Set<Item> items = new HashSet<>();
        try {
            for (var holder : level.getRecipeManager().getRecipes()) {
                try {
                    ItemStack result = holder.value().getResultItem(level.registryAccess());
                    if (!result.isEmpty()) {
                        items.add(result.getItem());
                    }
                } catch (Exception ignored) {}
            }
            AMI.LOGGER.debug("AMI: Recipe output set built — {} items have at least one recipe.", items.size());
        } catch (Exception e) {
            AMI.LOGGER.warn("AMI: Could not build recipe output set — recipe filter disabled. ({})", e.getMessage());
        }
        return items;
    }
}
