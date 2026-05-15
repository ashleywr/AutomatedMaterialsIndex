package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AMIConfig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public class RecipeViewerBridge {

    public static boolean isAvailable() {
        return ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");
    }

    /** Open the recipe viewer for the item's crafting recipes (what produces it). */
    public static void openRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (ModList.get().isLoaded("emi")) {
            EmiRecipeBridge.openRecipes(stack);
        }
        // JEI: deferred until JEI 1.21.1 plugin API is confirmed
    }

    /** Open the recipe viewer for uses of the item (what consumes it). */
    public static void openUses(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (ModList.get().isLoaded("emi")) {
            EmiRecipeBridge.openUses(stack);
        }
    }

    /**
     * Dispatch a click on an item according to the configured ITEM_CLICK_ACTION.
     * button: 0 = left, 1 = right.
     */
    public static void handleItemClick(ItemStack stack, int button) {
        if (stack == null || stack.isEmpty()) return;
        AMIConfig.ItemClickAction action = AMIConfig.ITEM_CLICK_ACTION.get();
        if (button == 1) {
            // Right-click always opens uses regardless of config
            openUses(stack);
            return;
        }
        switch (action) {
            case RECIPES -> openRecipes(stack);
            case USES    -> openUses(stack);
            case NONE    -> {}
        }
    }
}
