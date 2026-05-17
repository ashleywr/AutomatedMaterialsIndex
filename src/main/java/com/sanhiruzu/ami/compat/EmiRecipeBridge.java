package com.sanhiruzu.ami.compat;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;

/**
 * Direct EMI API calls — only referenced behind a ModList.isLoaded("emi") guard
 * so this class is never loaded when EMI is absent.
 */
class EmiRecipeBridge {
    static void openRecipes(ItemStack stack) {
        EmiApi.displayRecipes(EmiStack.of(stack));
    }

    static void openUses(ItemStack stack) {
        EmiApi.displayUses(EmiStack.of(stack));
    }

    static void startDrag(ItemStack stack) {
        dev.emi.emi.screen.EmiScreenManager.draggedStack = EmiStack.of(stack);
    }

    static ItemStack getDraggedStack() {
        dev.emi.emi.api.stack.EmiIngredient stack = dev.emi.emi.screen.EmiScreenManager.draggedStack;
        if (stack instanceof EmiStack es) {
            return es.getItemStack();
        }
        return ItemStack.EMPTY;
    }

    static boolean isDragging() {
        return !dev.emi.emi.screen.EmiScreenManager.draggedStack.isEmpty();
    }

    static void stopDrag() {
        dev.emi.emi.screen.EmiScreenManager.draggedStack = EmiStack.EMPTY;
    }

    static boolean handleDrop(net.minecraft.client.gui.screens.Screen screen, double mouseX, double mouseY) {
        dev.emi.emi.api.stack.EmiIngredient stack = dev.emi.emi.screen.EmiScreenManager.draggedStack;
        if (stack.isEmpty()) return false;
        boolean handled = dev.emi.emi.registry.EmiDragDropHandlers.dropStack(screen, stack, (int)mouseX, (int)mouseY);
        dev.emi.emi.screen.EmiScreenManager.draggedStack = EmiStack.EMPTY;
        return handled;
    }

    static java.util.List<ItemStack> getCraftables() {
        return dev.emi.emi.runtime.EmiSidebars.craftables.stream()
                .map(EmiRecipeBridge::toItemStack)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    static java.util.List<ItemStack> getLookupHistory() {
        return dev.emi.emi.runtime.EmiSidebars.lookupHistory.stream()
                .map(EmiRecipeBridge::toItemStack)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    static java.util.List<ItemStack> getCraftHistory() {
        return dev.emi.emi.runtime.EmiSidebars.craftHistory.stream()
                .map(EmiRecipeBridge::toItemStack)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static ItemStack toItemStack(dev.emi.emi.api.stack.EmiIngredient ingredient) {
        if (ingredient instanceof EmiStack es) {
            return es.getItemStack();
        }
        var stacks = ingredient.getEmiStacks();
        if (!stacks.isEmpty()) {
            return stacks.get(0).getItemStack();
        }
        return ItemStack.EMPTY;
    }

    static void handleShiftClick(ItemStack stack) {
        EmiApi.displayRecipes(EmiStack.of(stack));
    }
}
