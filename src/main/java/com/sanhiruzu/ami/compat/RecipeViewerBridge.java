package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public class RecipeViewerBridge {

    private static boolean recipeViewActive = false;

    /** True when EMI/JEI is currently displaying a recipe view triggered by AMI. */
    public static boolean isRecipeViewActive() { return recipeViewActive; }

    /** Called to notify that the recipe view has been dismissed. */
    public static void clearRecipeView() { recipeViewActive = false; }

    private static void markRecipeViewActive() {
        recipeViewActive = true;
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");
    }

    public static boolean supportsSearchSync() {
        if (ModList.get().isLoaded("emi")) return callEmiMethod("isAvailable", boolean.class);
        if (ModList.get().isLoaded("jei")) return JeiSearchSyncBridge.isAvailable();
        return false;
    }

    /** Returns the current search text from the active recipe viewer, or "" if none loaded. */
    public static String getSearchText() {
        if (ModList.get().isLoaded("emi") && supportsSearchSync()) {
            String text = callEmiMethod("getSearchText", String.class);
            if (text != null) return text;
        }
        if (ModList.get().isLoaded("jei") && JeiSearchSyncBridge.isAvailable()) return JeiSearchSyncBridge.getSearchText();
        return "";
    }

    /** Pushes a search string into the active recipe viewer's search bar. */
    public static void setSearchText(String text) {
        if (ModList.get().isLoaded("emi") && supportsSearchSync()) {
            callEmiMethod("setSearchText", void.class, text);
        }
        if (ModList.get().isLoaded("jei") && JeiSearchSyncBridge.isAvailable()) JeiSearchSyncBridge.setSearchText(text);
    }

    /** Open the recipe viewer for the item's crafting recipes (what produces it). */
    public static void openRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (ModList.get().isLoaded("emi")) {
            callEmiRecipeMethod("openRecipes", stack);
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.openRecipes(stack);
        }
    }

    /** Open the recipe viewer for uses of the item (what consumes it). */
    public static void openUses(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (ModList.get().isLoaded("emi")) {
            callEmiRecipeMethod("openUses", stack);
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.openUses(stack);
        }
    }

    public static void startDrag(ItemStack stack) {
        if (ModList.get().isLoaded("emi")) {
            callEmiRecipeMethod("startDrag", stack);
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.startDrag(stack);
        }
    }

    public static ItemStack getDraggedStack() {
        if (ModList.get().isLoaded("emi")) {
            Object result = callEmiRecipeMethod("getDraggedStack");
            return result instanceof ItemStack ? (ItemStack) result : ItemStack.EMPTY;
        }
        if (ModList.get().isLoaded("jei")) return JeiRecipeBridge.getDraggedStack();
        return ItemStack.EMPTY;
    }

    public static boolean isDragging() {
        if (ModList.get().isLoaded("emi")) {
            Object result = callEmiRecipeMethod("isDragging");
            return result instanceof Boolean ? (Boolean) result : false;
        }
        if (ModList.get().isLoaded("jei")) return JeiRecipeBridge.isDragging();
        return false;
    }

    public static void stopDrag() {
        if (ModList.get().isLoaded("emi")) {
            callEmiRecipeMethod("stopDrag");
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.stopDrag();
        }
    }

    public static boolean handleDrop(double mouseX, double mouseY) {
        if (ModList.get().isLoaded("emi")) {
            Object result = callEmiRecipeMethod("handleDrop", net.minecraft.client.Minecraft.getInstance().screen, mouseX, mouseY);
            return result instanceof Boolean ? (Boolean) result : false;
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
            markRecipeViewActive();
            openUses(stack);
            return;
        }
        switch (AmiConfig.itemClickAction) {
            case RECIPES -> { markRecipeViewActive(); openRecipes(stack); }
            case USES    -> { markRecipeViewActive(); openUses(stack); }
            case NONE    -> {}
        }
    }

    /** Shift+click: propagate to EMI/JEI for crafting grid insertion or their native shift+click behavior. */
    private static void handleShiftClick(ItemStack stack) {
        if (ModList.get().isLoaded("emi")) {
            callEmiRecipeMethod("handleShiftClick", stack);
        } else if (ModList.get().isLoaded("jei")) {
            JeiRecipeBridge.handleShiftClick(stack);
        }
    }

    /** Overload for backward compatibility with code not passing shift state. */
    public static void handleItemClick(ItemStack stack, int button) {
        handleItemClick(stack, button, false);
    }

    public static java.util.List<ItemStack> getCraftables() {
        return VanillaCraftablesService.getCraftables();
    }

    public static java.util.List<ItemStack> getLookupHistory() {
        if (ModList.get().isLoaded("emi")) {
            Object result = callEmiRecipeMethod("getLookupHistory");
            if (result instanceof java.util.List<?>) {
                var emiHistory = (java.util.List<ItemStack>) result;
                if (!emiHistory.isEmpty()) return emiHistory;
            }
        }
        return com.sanhiruzu.ami.client.favorites.AmiHistoryHandler.getInstance().getLookupHistory();
    }

    public static java.util.List<ItemStack> getCraftHistory() {
        if (ModList.get().isLoaded("emi")) {
            Object result = callEmiRecipeMethod("getCraftHistory");
            if (result instanceof java.util.List<?>) return (java.util.List<ItemStack>) result;
        }
        return java.util.List.of();
    }

    private static <T> T callEmiMethod(String methodName, Class<T> returnType, Object... args) {
        try {
            Class<?> bridgeClass = Class.forName("com.sanhiruzu.ami.compat.EmiSearchSyncBridge");
            java.lang.reflect.Method method = findMethod(bridgeClass, methodName, args);
            if (method != null) {
                return (T) method.invoke(null, args);
            }
        } catch (Exception e) {
            // EMI integration silently fails if the bridge class isn't available
        }
        return null;
    }

    private static Object callEmiRecipeMethod(String methodName, Object... args) {
        try {
            Class<?> bridgeClass = Class.forName("com.sanhiruzu.ami.compat.EmiRecipeBridge");
            java.lang.reflect.Method method = findMethod(bridgeClass, methodName, args);
            if (method != null) {
                return method.invoke(null, args);
            }
        } catch (Exception e) {
            // EMI integration silently fails if the bridge class isn't available
        }
        return null;
    }

    private static java.lang.reflect.Method findMethod(Class<?> clazz, String name, Object... args) {
        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                return method;
            }
        }
        return null;
    }
}
