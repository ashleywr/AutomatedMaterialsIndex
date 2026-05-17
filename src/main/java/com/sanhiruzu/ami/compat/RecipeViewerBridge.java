package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public class RecipeViewerBridge {

    public static boolean isAvailable() {
        return ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");
    }

    /** Returns the current search text from the active recipe viewer, or "" if none loaded. */
    public static String getSearchText() {
        if (ModList.get().isLoaded("emi")) return EmiSearchSyncBridge.getSearchText();
        return "";
    }

    /** Pushes a search string into the active recipe viewer's search bar. */
    public static void setSearchText(String text) {
        if (ModList.get().isLoaded("emi")) EmiSearchSyncBridge.setSearchText(text);
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

    public static void startDrag(ItemStack stack) {
        if (ModList.get().isLoaded("emi")) EmiRecipeBridge.startDrag(stack);
    }

    public static ItemStack getDraggedStack() {
        if (ModList.get().isLoaded("emi")) return EmiRecipeBridge.getDraggedStack();
        return ItemStack.EMPTY;
    }

    public static boolean isDragging() {
        return ModList.get().isLoaded("emi") && EmiRecipeBridge.isDragging();
    }

    public static void stopDrag() {
        if (ModList.get().isLoaded("emi")) EmiRecipeBridge.stopDrag();
    }

    public static boolean handleDrop(double mouseX, double mouseY) {
        if (ModList.get().isLoaded("emi")) {
            return EmiRecipeBridge.handleDrop(net.minecraft.client.Minecraft.getInstance().screen, mouseX, mouseY);
        }
        return false;
    }

    /**
     * Dispatch a click on an item according to the configured ITEM_CLICK_ACTION.
     * button: 0 = left, 1 = right.
     */
    public static void handleItemClick(ItemStack stack, int button) {
        if (stack == null || stack.isEmpty()) return;
        if (button == 1) {
            // Right-click always opens uses regardless of config
            openUses(stack);
            return;
        }
        switch (AmiConfig.itemClickAction) {
            case RECIPES -> openRecipes(stack);
            case USES    -> openUses(stack);
            case NONE    -> {}
        }
    }
}
