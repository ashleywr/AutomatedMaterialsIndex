package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public class RecipeViewerBridge {

    public static boolean isAvailable() {
        return ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");
    }

    public static boolean supportsSearchSync() {
        if (ModList.get().isLoaded("emi")) return EmiSearchSyncBridge.isAvailable();
        if (ModList.get().isLoaded("jei")) return JeiSearchSyncBridge.isAvailable();
        return false;
    }

    /** Returns the current search text from the active recipe viewer, or "" if none loaded. */
    public static String getSearchText() {
        if (ModList.get().isLoaded("emi") && EmiSearchSyncBridge.isAvailable()) return EmiSearchSyncBridge.getSearchText();
        if (ModList.get().isLoaded("jei") && JeiSearchSyncBridge.isAvailable()) return JeiSearchSyncBridge.getSearchText();
        return "";
    }

    /** Pushes a search string into the active recipe viewer's search bar. */
    public static void setSearchText(String text) {
        if (ModList.get().isLoaded("emi") && EmiSearchSyncBridge.isAvailable()) EmiSearchSyncBridge.setSearchText(text);
        if (ModList.get().isLoaded("jei") && JeiSearchSyncBridge.isAvailable()) JeiSearchSyncBridge.setSearchText(text);
    }

    /** Open the recipe viewer for the item's crafting recipes (what produces it). */
    public static void openRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (ModList.get().isLoaded("emi")) {
            EmiRecipeBridge.openRecipes(stack);
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.openRecipes(stack);
        }
    }

    /** Open the recipe viewer for uses of the item (what consumes it). */
    public static void openUses(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (ModList.get().isLoaded("emi")) {
            EmiRecipeBridge.openUses(stack);
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.openUses(stack);
        }
    }

    public static void startDrag(ItemStack stack) {
        if (ModList.get().isLoaded("emi")) {
            EmiRecipeBridge.startDrag(stack);
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.startDrag(stack);
        }
    }

    public static ItemStack getDraggedStack() {
        if (ModList.get().isLoaded("emi")) return EmiRecipeBridge.getDraggedStack();
        if (ModList.get().isLoaded("jei")) return JeiRecipeBridge.getDraggedStack();
        return ItemStack.EMPTY;
    }

    public static boolean isDragging() {
        if (ModList.get().isLoaded("emi")) return EmiRecipeBridge.isDragging();
        if (ModList.get().isLoaded("jei")) return JeiRecipeBridge.isDragging();
        return false;
    }

    public static void stopDrag() {
        if (ModList.get().isLoaded("emi")) {
            EmiRecipeBridge.stopDrag();
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.stopDrag();
        }
    }

    public static boolean handleDrop(double mouseX, double mouseY) {
        if (ModList.get().isLoaded("emi")) {
            return EmiRecipeBridge.handleDrop(net.minecraft.client.Minecraft.getInstance().screen, mouseX, mouseY);
        }
        if (ModList.get().isLoaded("jei")) {
            return JeiRecipeBridge.handleDrop(net.minecraft.client.Minecraft.getInstance().screen, mouseX, mouseY);
        }
        return false;
    }

    /**
     * Dispatch a click on an item. Shift+click is propagated to EMI/JEI for their handling.
     * button: 0 = left, 1 = right.
     */
    public static void handleItemClick(ItemStack stack, int button, boolean shiftDown) {
        if (stack == null || stack.isEmpty()) return;

        if (shiftDown) {
            handleShiftClick(stack);
            return;
        }

        if (button == 0 || button == 1) {
            com.sanhiruzu.ami.client.favorites.AmiHistoryHandler.getInstance().recordLookup(stack);
        }

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

    /** Shift+click: propagate to EMI/JEI for crafting grid insertion or their native shift+click behavior. */
    private static void handleShiftClick(ItemStack stack) {
        if (ModList.get().isLoaded("emi")) {
            EmiRecipeBridge.handleShiftClick(stack);
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.handleShiftClick(stack);
        }
    }

    /** Overload for backward compatibility with code not passing shift state. */
    public static void handleItemClick(ItemStack stack, int button) {
        handleItemClick(stack, button, false);
    }

    public static java.util.List<ItemStack> getCraftables() {
        if (ModList.get().isLoaded("emi")) return EmiRecipeBridge.getCraftables();
        return java.util.List.of();
    }

    public static java.util.List<ItemStack> getLookupHistory() {
        if (ModList.get().isLoaded("emi")) {
            var emiHistory = EmiRecipeBridge.getLookupHistory();
            if (!emiHistory.isEmpty()) return emiHistory;
        }
        return com.sanhiruzu.ami.client.favorites.AmiHistoryHandler.getInstance().getLookupHistory();
    }

    public static java.util.List<ItemStack> getCraftHistory() {
        if (ModList.get().isLoaded("emi")) return EmiRecipeBridge.getCraftHistory();
        return java.util.List.of();
    }
}
