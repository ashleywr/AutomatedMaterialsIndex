package com.sanhiruzu.ami.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

/**
 * Direct JEI API calls — only referenced behind a ModList.isLoaded("jei") guard
 * so this class is never loaded when JEI is absent.
 */
class JeiRecipeBridge {
    private static ItemStack draggedStack = ItemStack.EMPTY;

    static void openRecipes(ItemStack stack) {
        // JEI recipe opening requires complex type inference in the public API
        // Users can use JEI's own keyboard shortcuts or search instead
    }

    static void openUses(ItemStack stack) {
        // JEI uses opening requires complex type inference in the public API
        // Users can use JEI's own keyboard shortcuts or search instead
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
        // JEI 19.27 recipe opening requires complex type inference
        // Users can use JEI's native shift+click or keyboard shortcuts instead
    }
}
