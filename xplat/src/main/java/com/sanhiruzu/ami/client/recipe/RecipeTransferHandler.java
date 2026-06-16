package com.sanhiruzu.ami.client.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RecipeTransferHandler {
    private RecipeTransferHandler() {
    }

    public static boolean canTransfer(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen parentScreen) {
        if (!(parentScreen instanceof AbstractContainerScreen<?> cs)) return false;
        TransferPlan plan = createPlan(recipe.value(), cs.getMenu());
        if (plan == null || plan.ingredients().isEmpty()) return false;
        int[] playerSlots = getPlayerSlots(cs.getMenu());
        return playerSlots.length > 0
                && hasRoomToClearInputs(cs.getMenu(), plan.inputSlots(), playerSlots)
                && canPlaceOneBatch(cs.getMenu(), plan.ingredients(), plan.inputSlots(), playerSlots, false);
    }

    public static boolean transfer(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen parentScreen, Minecraft mc) {
        return transfer(recipe, parentScreen, mc, false);
    }

    public static boolean transfer(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen parentScreen, Minecraft mc,
                                   boolean maxTransfer) {
        if (mc.player == null || mc.gameMode == null) return false;
        if (!(parentScreen instanceof AbstractContainerScreen<?> cs)) return false;

        AbstractContainerMenu menu = cs.getMenu();
        TransferPlan plan = createPlan(recipe.value(), menu);
        if (plan == null || plan.ingredients().isEmpty()) return false;

        int[] playerSlots = getPlayerSlots(menu);
        if (playerSlots.length == 0) return false;
        if (!hasRoomToClearInputs(menu, plan.inputSlots(), playerSlots)) return false;

        int containerId = menu.containerId;

        // Clear existing items from input slots back to inventory
        for (int slotIdx : plan.inputSlots()) {
            Slot slot = menu.getSlot(slotIdx);
            if (slot.hasItem()) {
                mc.gameMode.handleContainerInput(
                        containerId, slotIdx, 0, ContainerInput.QUICK_MOVE, mc.player);
            }
        }
        for (int slotIdx : plan.inputSlots()) {
            if (menu.getSlot(slotIdx).hasItem()) {
                return false;
            }
        }

        if (!canPlaceOneBatch(menu, plan.ingredients(), plan.inputSlots(), playerSlots)) {
            return false;
        }

        if (maxTransfer) {
            int craftedBatches = 0;
            while (craftedBatches < 64
                    && canPlaceOneBatch(menu, plan.ingredients(), plan.inputSlots(), playerSlots)
                    && placeOneBatch(menu, plan.ingredients(), plan.inputSlots(), playerSlots, mc)) {
                craftedBatches++;
            }
            return craftedBatches > 0;
        } else {
            if (!placeOneBatch(menu, plan.ingredients(), plan.inputSlots(), playerSlots, mc)) {
                return false;
            }
        }

        // If cursor still has items, return them
        if (!menu.getCarried().isEmpty()) {
            for (int i = playerSlots.length - 1; i >= 0; i--) {
                Slot slot = menu.getSlot(playerSlots[i]);
                if (!slot.hasItem()) {
                    mc.gameMode.handleContainerInput(
                            containerId, playerSlots[i], 0, ContainerInput.PICKUP, mc.player);
                    break;
                }
            }
        }

        return true;
    }

    private static TransferPlan createPlan(Recipe<?> recipe, AbstractContainerMenu menu) {
        RecipeType<?> type = recipe.getType();

        if (type == RecipeType.CRAFTING) {
            return createCraftingPlan(recipe, menu);
        }

        if (RecipeDisplayHelper.isFurnaceType(type) && menu instanceof AbstractFurnaceMenu) {
            List<Ingredient> ingredients = recipe.placementInfo().ingredients();
            if (ingredients.isEmpty() || ingredients.get(0).isEmpty()) return null;
            return new TransferPlan(new int[]{0}, List.of(ingredients.get(0)));
        }

        if (type == RecipeType.STONECUTTING && menu instanceof StonecutterMenu) {
            List<Ingredient> ingredients = recipe.placementInfo().ingredients();
            if (ingredients.isEmpty() || ingredients.get(0).isEmpty()) return null;
            return new TransferPlan(new int[]{0}, List.of(ingredients.get(0)));
        }

        if (type == RecipeType.SMITHING && menu instanceof ItemCombinerMenu) {
            List<Ingredient> ingredients = recipe.placementInfo().ingredients();
            if (ingredients.isEmpty()) return null;
            List<Integer> inputs = new ArrayList<>();
            for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
                Slot slot = menu.slots.get(menuSlot);
                if (!(slot.container instanceof Inventory) && slot.mayPlace(sampleFor(ingredients, inputs.size()))) {
                    inputs.add(menuSlot);
                    if (inputs.size() == ingredients.size()) break;
                }
            }
            if (inputs.size() < ingredients.size()) return null;
            return new TransferPlan(inputs.stream().mapToInt(Integer::intValue).toArray(), ingredients);
        }

        return null;
    }

    private static TransferPlan createCraftingPlan(Recipe<?> recipe, AbstractContainerMenu menu) {
        List<Integer> slots = new ArrayList<>();
        CraftingContainer craftingContainer = null;
        for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
            Slot slot = menu.slots.get(menuSlot);
            if (slot.container instanceof CraftingContainer crafting) {
                if (craftingContainer == null) {
                    craftingContainer = crafting;
                }
                if (slot.container == craftingContainer) {
                    slots.add(menuSlot);
                }
            }
        }
        if (craftingContainer == null || slots.isEmpty()) return null;

        int gridWidth = craftingContainer.getWidth();
        int gridHeight = craftingContainer.getHeight();
        if (gridWidth <= 0 || gridHeight <= 0 || slots.size() < gridWidth * gridHeight) return null;

        List<Ingredient> gridIngredients = new ArrayList<>(gridWidth * gridHeight);
        for (int i = 0; i < gridWidth * gridHeight; i++) {
            gridIngredients.add(null);
        }

        if (recipe instanceof ShapedRecipe shaped) {
            if (shaped.getWidth() > gridWidth || shaped.getHeight() > gridHeight) return null;
            int recipeWidth = shaped.getWidth();
            int recipeHeight = shaped.getHeight();
            List<Optional<Ingredient>> shapedIngs = shaped.getIngredients();
            for (int y = 0; y < recipeHeight; y++) {
                for (int x = 0; x < recipeWidth; x++) {
                    int idx = x + y * recipeWidth;
                    Ingredient ing = idx < shapedIngs.size() ? shapedIngs.get(idx).orElse(null) : null;
                    gridIngredients.set(x + y * gridWidth, ing);
                }
            }
        } else {
            List<Ingredient> recipeIngredients = recipe.placementInfo().ingredients();
            if (recipeIngredients.size() > gridIngredients.size()) return null;
            for (int i = 0; i < recipeIngredients.size(); i++) {
                gridIngredients.set(i, recipeIngredients.get(i));
            }
        }

        return new TransferPlan(slots.stream().sorted().limit(gridIngredients.size())
                .mapToInt(Integer::intValue).toArray(), gridIngredients);
    }

    private static net.minecraft.world.item.ItemStack sampleFor(List<Ingredient> ingredients, int index) {
        if (index < 0 || index >= ingredients.size() || ingredients.get(index).isEmpty()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return ingredients.get(index).items()
                .findFirst()
                .map(h -> new net.minecraft.world.item.ItemStack(h))
                .orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    private static boolean canPlaceOneBatch(AbstractContainerMenu menu, List<Ingredient> ingredients, int[] inputSlots,
                                            int[] playerSlots) {
        return canPlaceOneBatch(menu, ingredients, inputSlots, playerSlots, true);
    }

    private static boolean canPlaceOneBatch(AbstractContainerMenu menu, List<Ingredient> ingredients, int[] inputSlots,
                                            int[] playerSlots, boolean checkTargetCapacity) {
        int[] available = new int[playerSlots.length];
        for (int i = 0; i < playerSlots.length; i++) {
            available[i] = menu.getSlot(playerSlots[i]).getItem().getCount();
        }

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient == null || ingredient.isEmpty()) continue;

            Slot target = menu.getSlot(inputSlots[i]);
            if (checkTargetCapacity && target.hasItem()
                    && target.getItem().getCount() >= target.getMaxStackSize(target.getItem())) {
                return false;
            }

            boolean matched = false;
            for (int j = 0; j < playerSlots.length; j++) {
                if (available[j] <= 0) continue;
                Slot source = menu.getSlot(playerSlots[j]);
                if (source.hasItem() && ingredient.test(source.getItem())) {
                    available[j]--;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private static boolean placeOneBatch(AbstractContainerMenu menu, List<Ingredient> ingredients, int[] inputSlots,
                                         int[] playerSlots, Minecraft mc) {
        if (mc.player == null || mc.gameMode == null) return false;
        int containerId = menu.containerId;
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient == null || ingredient.isEmpty()) continue;
            int fromSlot = findMatchingPlayerSlot(menu, playerSlots, ingredient);
            if (fromSlot == -1) {
                return false;
            }
            int toSlot = inputSlots[i];
            mc.gameMode.handleContainerInput(containerId, fromSlot, 0, ContainerInput.PICKUP, mc.player);
            mc.gameMode.handleContainerInput(containerId, toSlot, 1, ContainerInput.PICKUP, mc.player);
            mc.gameMode.handleContainerInput(containerId, fromSlot, 0, ContainerInput.PICKUP, mc.player);
        }
        return true;
    }

    private static int findMatchingPlayerSlot(AbstractContainerMenu menu, int[] playerSlots, Ingredient ingredient) {
        for (int playerSlot : playerSlots) {
            Slot slot = menu.getSlot(playerSlot);
            if (slot.hasItem() && ingredient.test(slot.getItem())) {
                return playerSlot;
            }
        }
        return -1;
    }

    private static int[] getPlayerSlots(AbstractContainerMenu menu) {
        List<Integer> slots = new ArrayList<>();
        for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
            Slot slot = menu.slots.get(menuSlot);
            // Player inventory and hotbar (not armor, not crafting result/input)
            if (slot.container instanceof Inventory inventory
                    && slot.index < inventory.getContainerSize()) {
                slots.add(menuSlot);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private static boolean hasRoomToClearInputs(AbstractContainerMenu menu, int[] inputSlots, int[] playerSlots) {
        int occupiedInputs = 0;
        for (int slotIdx : inputSlots) {
            if (menu.getSlot(slotIdx).hasItem()) {
                occupiedInputs++;
            }
        }
        if (occupiedInputs == 0) return true;

        int emptyPlayerSlots = 0;
        for (int slotIdx : playerSlots) {
            if (!menu.getSlot(slotIdx).hasItem()) {
                emptyPlayerSlots++;
            }
        }
        return emptyPlayerSlots >= occupiedInputs;
    }

    private record TransferPlan(int[] inputSlots, List<Ingredient> ingredients) {
    }
}

