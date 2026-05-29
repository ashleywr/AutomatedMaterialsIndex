package com.sanhiruzu.ami.client.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RecipeTransferHandler {
    private RecipeTransferHandler() {
    }

    public static boolean canTransfer(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen parentScreen) {
        if (!(parentScreen instanceof AbstractContainerScreen<?> cs)) return false;
        List<Ingredient> ingredients = recipe.value().getIngredients();
        if (ingredients.isEmpty()) return false;
        return getInputSlots(cs.getMenu(), ingredients.size()) != null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean transfer(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen parentScreen, Minecraft mc) {
        return transfer(new RecipeHolder(recipe.id(), recipe.value()), parentScreen, mc);
    }

    public static boolean canTransfer(RecipeHolder<?> recipe, Screen parentScreen) {
        if (!(parentScreen instanceof AbstractContainerScreen<?> cs)) return false;
        List<Ingredient> ingredients = recipe.value().getIngredients();
        if (ingredients.isEmpty()) return false;
        return getInputSlots(cs.getMenu(), ingredients.size()) != null;
    }

    public static boolean transfer(RecipeHolder<?> recipe, Screen parentScreen, Minecraft mc) {
        if (mc.player == null || mc.gameMode == null) return false;
        if (!(parentScreen instanceof AbstractContainerScreen<?> cs)) return false;

        AbstractContainerMenu menu = cs.getMenu();
        List<Ingredient> ingredients = recipe.value().getIngredients();
        if (ingredients.isEmpty()) return false;

        int[] inputSlots = getInputSlots(menu, ingredients.size());
        if (inputSlots == null) return false;

        int[] playerSlots = getPlayerSlots(menu);
        if (playerSlots.length == 0) return false;

        boolean[] usedPlayerSlots = new boolean[playerSlots.length];
        int[] assignments = new int[ingredients.size()];
        Arrays.fill(assignments, -1);

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) continue;
            for (int j = 0; j < playerSlots.length; j++) {
                if (usedPlayerSlots[j]) continue;
                Slot slot = menu.getSlot(playerSlots[j]);
                if (slot.hasItem() && ing.test(slot.getItem())) {
                    assignments[i] = j;
                    usedPlayerSlots[j] = true;
                    break;
                }
            }
            if (assignments[i] == -1) return false;
        }

        int containerId = menu.containerId;

        // Clear existing items from input slots back to inventory
        for (int slotIdx : inputSlots) {
            Slot slot = menu.getSlot(slotIdx);
            if (slot.hasItem()) {
                mc.gameMode.handleInventoryMouseClick(
                        containerId, slotIdx, 0, ClickType.QUICK_MOVE, mc.player);
            }
        }

        // Move matching items from inventory to input slots
        for (int i = 0; i < ingredients.size(); i++) {
            if (assignments[i] == -1) continue;
            int fromSlot = playerSlots[assignments[i]];
            int toSlot = inputSlots[i];

            mc.gameMode.handleInventoryMouseClick(
                    containerId, fromSlot, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(
                    containerId, toSlot, 0, ClickType.PICKUP, mc.player);
        }

        // If cursor still has items, return them
        if (!menu.getCarried().isEmpty()) {
            for (int i = playerSlots.length - 1; i >= 0; i--) {
                Slot slot = menu.getSlot(playerSlots[i]);
                if (!slot.hasItem()) {
                    mc.gameMode.handleInventoryMouseClick(
                            containerId, playerSlots[i], 0, ClickType.PICKUP, mc.player);
                    break;
                }
            }
        }

        return true;
    }

    private static int[] getPlayerSlots(AbstractContainerMenu menu) {
        List<Integer> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            // Player inventory and hotbar (not armor, not crafting result/input)
            if (slot.container instanceof Inventory inventory
                    && slot.index < inventory.items.size()) {
                slots.add(slot.index);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] getInputSlots(AbstractContainerMenu menu, int ingredientCount) {
        // Crafting table or player 2x2 crafting
        List<Integer> crafting = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof CraftingContainer) {
                crafting.add(slot.index);
            }
        }
        if (!crafting.isEmpty() && ingredientCount <= crafting.size()) {
            return crafting.stream().sorted().limit(ingredientCount)
                    .mapToInt(Integer::intValue).toArray();
        }

        // Furnace input: look for slots whose name suggests input, or the first non-fuel, non-result slot
        List<Integer> processingInputs = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)
                    && !(slot.container instanceof CraftingContainer)) {
                // Don't include result slots (slot index within their container is often 0)
                // For furnace: slot 0 = input, slot 1 = fuel, slot 2 = result
                processingInputs.add(slot.index);
            }
        }
        if (!processingInputs.isEmpty()) {
            // For furnaces (3 processing slots: input, fuel, result), take only the first
            // For smithing (4 processing slots: template, base, addition, result), take first 3
            int take = Math.min(ingredientCount,
                    Math.min(processingInputs.size(), ingredientCount <= 1 ? 1 : processingInputs.size() - 1));
            return processingInputs.stream().sorted().limit(take)
                    .mapToInt(Integer::intValue).toArray();
        }

        return null;
    }
}
