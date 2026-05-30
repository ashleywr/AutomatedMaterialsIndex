package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.client.recipe.RecipeTransferHandler;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class RecipeViewerBridge {

    private static boolean recipeViewActive = false;

    /**
     * True when EMI/JEI is currently displaying a recipe view triggered by AMI.
     */
    public static boolean isRecipeViewActive() {
        return recipeViewActive;
    }

    /**
     * Called to notify that the recipe view has been dismissed.
     */
    public static void clearRecipeView() {
        recipeViewActive = false;
    }

    private static void markRecipeViewActive() {
        recipeViewActive = true;
    }

    public static boolean isAvailable() {
        return Services.PLATFORM.isModLoaded("emi") || Services.PLATFORM.isModLoaded("jei");
    }

    public static boolean supportsSearchSync() {
        if (Services.PLATFORM.isModLoaded("emi")) return EmiSearchSyncBridge.isAvailable();
        if (Services.PLATFORM.isModLoaded("jei")) return JeiSearchSyncBridge.isAvailable();
        return false;
    }

    /**
     * Returns the current search text from the active recipe viewer, or "" if none loaded.
     */
    public static String getSearchText() {
        if (Services.PLATFORM.isModLoaded("emi") && EmiSearchSyncBridge.isAvailable())
            return EmiSearchSyncBridge.getSearchText();
        if (Services.PLATFORM.isModLoaded("jei") && JeiSearchSyncBridge.isAvailable())
            return JeiSearchSyncBridge.getSearchText();
        return "";
    }

    /**
     * Pushes a search string into the active recipe viewer's search bar.
     */
    public static void setSearchText(String text) {
        if (Services.PLATFORM.isModLoaded("emi") && EmiSearchSyncBridge.isAvailable())
            EmiSearchSyncBridge.setSearchText(text);
        if (Services.PLATFORM.isModLoaded("jei") && JeiSearchSyncBridge.isAvailable())
            JeiSearchSyncBridge.setSearchText(text);
    }

    /**
     * Open the recipe viewer for the item's crafting recipes (what produces it).
     */
    public static void openRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        markRecipeViewActive();
        if (RecipeViewerBridgeCommon.shouldUseNativeViewer(isAvailable())) {
            RecipeViewerBridgeCommon.recordLookup(stack);
            RecipeViewerBridgeCommon.openNative(stack, true);
            return;
        }
        if (Services.PLATFORM.isModLoaded("emi")) {
            EmiRecipeBridge.openRecipes(stack);
        } else if (Services.PLATFORM.isModLoaded("jei")) {
            JeiRecipeBridge.openRecipes(stack);
        }
        RecipeViewerBridgeCommon.recordLookup(stack);
    }

    /**
     * Open the recipe viewer for uses of the item (what consumes it).
     */
    public static void openUses(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        markRecipeViewActive();
        if (RecipeViewerBridgeCommon.shouldUseNativeViewer(isAvailable())) {
            RecipeViewerBridgeCommon.recordLookup(stack);
            RecipeViewerBridgeCommon.openNative(stack, false);
            return;
        }
        if (Services.PLATFORM.isModLoaded("emi")) {
            EmiRecipeBridge.openUses(stack);
        } else if (Services.PLATFORM.isModLoaded("jei")) {
            JeiRecipeBridge.openUses(stack);
        }
        RecipeViewerBridgeCommon.recordLookup(stack);
    }

    public static void startDrag(ItemStack stack) {
        if (Services.PLATFORM.isModLoaded("emi")) {
            EmiRecipeBridge.startDrag(stack);
        } else if (Services.PLATFORM.isModLoaded("jei")) {
            JeiRecipeBridge.startDrag(stack);
        }
    }

    public static ItemStack getDraggedStack() {
        if (Services.PLATFORM.isModLoaded("emi")) return EmiRecipeBridge.getDraggedStack();
        if (Services.PLATFORM.isModLoaded("jei")) return JeiRecipeBridge.getDraggedStack();
        return ItemStack.EMPTY;
    }

    public static boolean isDragging() {
        if (Services.PLATFORM.isModLoaded("emi")) return EmiRecipeBridge.isDragging();
        if (Services.PLATFORM.isModLoaded("jei")) return JeiRecipeBridge.isDragging();
        return false;
    }

    public static boolean isEmiRecipeScreenActive() {
        var screen = net.minecraft.client.Minecraft.getInstance().screen;
        return screen != null && screen.getClass().getName().equals("dev.emi.emi.screen.RecipeScreen");
    }

    public static void stopDrag() {
        if (Services.PLATFORM.isModLoaded("emi")) {
            EmiRecipeBridge.stopDrag();
        } else if (Services.PLATFORM.isModLoaded("jei")) {
            JeiRecipeBridge.stopDrag();
        }
    }

    public static boolean handleDrop(double mouseX, double mouseY) {
        if (Services.PLATFORM.isModLoaded("emi")) {
            return EmiRecipeBridge.handleDrop(net.minecraft.client.Minecraft.getInstance().screen, mouseX, mouseY);
        }
        if (Services.PLATFORM.isModLoaded("jei")) {
            return JeiRecipeBridge.handleDrop(net.minecraft.client.Minecraft.getInstance().screen, mouseX, mouseY);
        }
        return false;
    }

    /**
     * Dispatch a click on an item. Left-click crafts one when the current screen
     * can accept a recipe transfer, and shift-left crafts as many as possible.
     * button: 0 = left, 1 = right.
     */
    public static void handleItemClick(ItemStack stack, int button, boolean shiftDown) {
        handleItemClick(stack, button, shiftDown, false);
    }

    public static void handleItemClick(ItemStack stack, int button, boolean shiftDown, boolean controlDown) {
        if (stack == null || stack.isEmpty()) return;

        if (button == 0 && (shiftDown || controlDown || canTransferStack(stack))) {
            handleTransferClick(stack, shiftDown, false);
            return;
        }

        if (button == 1) {
            // Right-click always opens uses regardless of config
            openUses(stack);
            return;
        }
        switch (AmiConfig.itemClickAction) {
            case RECIPES -> {
                openRecipes(stack);
            }
            case USES -> {
                openUses(stack);
            }
            case NONE -> {
            }
        }
    }

    private static void handleTransferClick(ItemStack stack, boolean maxTransfer, boolean toCursor) {
        if (tryTransferRecipe(stack, maxTransfer, toCursor)) {
            return;
        }
        if (Services.PLATFORM.isModLoaded("emi")) {
            EmiRecipeBridge.handleShiftClick(stack);
        } else if (Services.PLATFORM.isModLoaded("jei")) {
            JeiRecipeBridge.handleShiftClick(stack);
        }
    }

    private static boolean tryTransferRecipe(ItemStack stack, boolean maxTransfer, boolean toCursor) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (stack == null || stack.isEmpty() || screen == null) {
            return false;
        }
        if (!toCursor && Services.PLATFORM.isModLoaded("emi")
                && EmiRecipeBridge.transferStack(stack, screen, maxTransfer)) {
            RecipeViewerBridgeCommon.recordCraft(stack);
            return true;
        }
        if (!toCursor && Services.PLATFORM.isModLoaded("jei")
                && JeiRecipeBridge.transferStack(stack, screen, maxTransfer)) {
            RecipeViewerBridgeCommon.recordCraft(stack);
            return true;
        }
        if (!Services.PLATFORM.isRecipeIndexBuilt()) {
            return false;
        }
        for (var recipe : Services.PLATFORM.getRecipesFor(stack)) {
            if (transferRecipe(recipe, screen, mc, maxTransfer, toCursor)) {
                RecipeViewerBridgeCommon.recordCraft(stack);
                return true;
            }
        }
        return false;
    }

    public static boolean canTransferStack(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (stack == null || stack.isEmpty() || screen == null) {
            return false;
        }
        if (Services.PLATFORM.isModLoaded("emi") && EmiRecipeBridge.canTransferStack(stack, screen)) {
            return true;
        }
        if (Services.PLATFORM.isModLoaded("jei") && JeiRecipeBridge.canTransferStack(stack, screen)) {
            return true;
        }
        if (!Services.PLATFORM.isRecipeIndexBuilt()) {
            return false;
        }
        for (var recipe : Services.PLATFORM.getRecipesFor(stack)) {
            if (canTransferRecipe(recipe, screen)) {
                return true;
            }
        }
        return false;
    }

    public static boolean transferRecipe(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen screen, Minecraft mc,
                                         boolean maxTransfer, boolean toCursor) {
        if (recipe == null || screen == null || mc == null) {
            return false;
        }
        if (Services.PLATFORM.isModLoaded("emi") && EmiRecipeBridge.transfer(recipe, screen, maxTransfer, toCursor)) {
            return true;
        }
        if (!toCursor && Services.PLATFORM.isModLoaded("jei") && JeiRecipeBridge.transfer(recipe, screen, maxTransfer, false)) {
            return true;
        }
        return !toCursor
                && RecipeTransferHandler.canTransfer(recipe, screen)
                && RecipeTransferHandler.transfer(recipe, screen, mc, maxTransfer);
    }

    public static boolean canTransferRecipe(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen screen) {
        if (recipe == null || screen == null) {
            return false;
        }
        if (Services.PLATFORM.isModLoaded("emi") && EmiRecipeBridge.canTransfer(recipe, screen)) {
            return true;
        }
        if (Services.PLATFORM.isModLoaded("jei") && JeiRecipeBridge.canTransfer(recipe, screen)) {
            return true;
        }
        return RecipeTransferHandler.canTransfer(recipe, screen);
    }

    /**
     * Overload for backward compatibility with code not passing shift state.
     */
    public static void handleItemClick(ItemStack stack, int button) {
        handleItemClick(stack, button, false);
    }

    public static java.util.List<ItemStack> getCraftables() {
        return VanillaCraftablesService.getCraftables();
    }

    public static java.util.List<ItemStack> getLookupHistory() {
        if (Services.PLATFORM.isModLoaded("emi")) {
            var emiHistory = EmiRecipeBridge.getLookupHistory();
            if (!emiHistory.isEmpty()) return emiHistory;
        }
        return com.sanhiruzu.ami.client.favorites.AmiHistoryHandler.getInstance().getLookupHistory();
    }

    public static java.util.List<ItemStack> getCraftHistory() {
        if (Services.PLATFORM.isModLoaded("emi")) return EmiRecipeBridge.getCraftHistory();
        return com.sanhiruzu.ami.client.favorites.AmiHistoryHandler.getInstance().getCraftHistory();
    }
}
