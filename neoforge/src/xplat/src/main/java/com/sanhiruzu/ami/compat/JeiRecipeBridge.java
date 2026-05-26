package com.sanhiruzu.ami.compat;

import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

/**
 * Direct JEI API calls — only referenced behind a ModList.isLoaded("jei") guard
 * so this class is never loaded when JEI is absent.
 */
class JeiRecipeBridge {
    private static ItemStack draggedStack = ItemStack.EMPTY;

    static void openRecipes(ItemStack stack) {
        JeiRuntimeAccessor.withRuntime(runtime -> {
            IIngredientType<ItemStack> itemType = mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
            if (itemType == null) return;
            IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
            IFocus<ItemStack> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, itemType, stack);
            runtime.getRecipesGui().show(focus);
        });
    }

    static void openUses(ItemStack stack) {
        JeiRuntimeAccessor.withRuntime(runtime -> {
            IIngredientType<ItemStack> itemType = mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
            if (itemType == null) return;
            IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
            IFocus<ItemStack> focus = focusFactory.createFocus(RecipeIngredientRole.INPUT, itemType, stack);
            runtime.getRecipesGui().show(focus);
        });
    }

    static void startDrag(ItemStack stack) {
        draggedStack = stack.copy();
    }

    static ItemStack getDraggedStack() {
        return draggedStack;
    }

    static boolean isDragging() {
        return !draggedStack.isEmpty();
    }

    static void stopDrag() {
        draggedStack = ItemStack.EMPTY;
    }

    static boolean handleDrop(Screen screen, double mouseX, double mouseY) {
        if (draggedStack.isEmpty()) {
            return false;
        }
        draggedStack = ItemStack.EMPTY;
        return false;
    }

    static void handleShiftClick(ItemStack stack) {
        openRecipes(stack);
    }
}
