package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AMI;
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
 *  Layer 1 – hardcoded system-item blacklist: always excluded, no config toggle.
 *  Layer 2 – creative-tab check: excluded by default (AMIConfig.HIDE_NON_CREATIVE_ITEMS).
 *             Items that pass when the setting is off are tagged VISIBILITY=hidden.
 *  Layer 3 – recipe-output check: opt-in (AMIConfig.STRICT_SURVIVAL_MODE).
 *             Items with no recipe are tagged OBTAINABILITY=no_recipe.
 */
public final class ItemFilter {

    /**
     * Items that are useless or harmful in a player-facing search index.
     * Covers admin/debug tools, unobtainable survival blocks, and "dummy base"
     * items whose variants (potions, enchanted books) are handled separately.
     */
    public static final Set<String> SYSTEM_ITEM_IDS = Set.of(
        // Admin & debug tools
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
        // Unobtainable survival blocks
        "minecraft:air",
        "minecraft:bedrock",
        "minecraft:end_portal_frame",
        "minecraft:reinforced_deepslate",
        "minecraft:spawner",
        "minecraft:petrified_oak_slab",
        "minecraft:infested_stone",
        "minecraft:infested_cobblestone",
        "minecraft:infested_stone_bricks",
        "minecraft:infested_mossy_stone_bricks",
        "minecraft:infested_cracked_stone_bricks",
        "minecraft:infested_chiseled_stone_bricks",
        "minecraft:infested_deepslate",
        // Unspecialized base items — specific variants are indexed separately
        "minecraft:potion",
        "minecraft:splash_potion",
        "minecraft:lingering_potion",
        "minecraft:tipped_arrow",
        "minecraft:enchanted_book",
        "minecraft:suspicious_stew"
    );

    private ItemFilter() {}

    public static boolean isSystemItem(ResourceLocation id) {
        return SYSTEM_ITEM_IDS.contains(id.toString());
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
