package com.sanhiruzu.ami.compat;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiRecipeFiller;
import dev.emi.emi.registry.EmiRecipes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Direct EMI API calls — only referenced behind a ModList.isLoaded("emi") guard
 * so this class is never loaded when EMI is absent.
 */
class EmiRecipeBridge {
    // ResourceLocation is inaccessible in 26.x compile context; use reflection for getRecipe().
    private static EmiRecipe emiGetRecipeById(Identifier id) {
        if (id == null) return null;
        try {
            var mgr = EmiApi.getRecipeManager();
            String idStr = id.toString();
            for (java.lang.reflect.Method m : mgr.getClass().getMethods()) {
                if (m.getName().equals("getRecipe") && m.getParameterCount() == 1) {
                    Class<?> paramType = m.getParameterTypes()[0];
                    Object rl = paramType.getMethod("parse", String.class).invoke(null, idStr);
                    Object result = m.invoke(mgr, rl);
                    return result instanceof EmiRecipe r ? r : null;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    static void openRecipes(ItemStack stack) {
        EmiApi.displayRecipes(EmiStack.of(firstRecipeStack(stack)));
    }

    static void openUses(ItemStack stack) {
        EmiApi.displayUses(EmiStack.of(firstUseStack(stack)));
    }

    static boolean hasRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (ItemStack candidate : RecipeLookupStackResolver.candidates(stack)) {
            if (hasRecipesDirect(candidate)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasUses(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (ItemStack candidate : RecipeLookupStackResolver.candidates(stack)) {
            if (hasUsesDirect(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack firstRecipeStack(ItemStack stack) {
        for (ItemStack candidate : RecipeLookupStackResolver.candidates(stack)) {
            if (hasRecipesDirect(candidate)) {
                return candidate;
            }
        }
        return stack;
    }

    private static ItemStack firstUseStack(ItemStack stack) {
        for (ItemStack candidate : RecipeLookupStackResolver.candidates(stack)) {
            if (hasUsesDirect(candidate)) {
                return candidate;
            }
        }
        return stack;
    }

    private static boolean hasRecipesDirect(ItemStack stack) {
        EmiStack output = EmiStack.of(stack);
        return EmiApi.getRecipeManager().getRecipesByOutput(output).stream()
                .anyMatch(recipe -> recipe.getOutputs().stream().anyMatch(recipeOutput -> recipeOutput.isEqual(output)));
    }

    private static boolean hasUsesDirect(ItemStack stack) {
        EmiStack input = EmiStack.of(stack);
        boolean recipeUse = EmiApi.getRecipeManager().getRecipesByInput(input).stream()
                .anyMatch(recipe -> recipe.getInputs().stream().anyMatch(ingredient -> containsAll(ingredient, input))
                        || recipe.getCatalysts().stream().anyMatch(ingredient -> containsAll(ingredient, input)));
        return recipeUse || !EmiRecipes.byWorkstation.getOrDefault(input, List.of()).isEmpty();
    }

    private static boolean containsAll(EmiIngredient collection, EmiIngredient ingredient) {
        outer:
        for (EmiStack stack : ingredient.getEmiStacks()) {
            for (EmiStack candidate : collection.getEmiStacks()) {
                if (candidate.isEqual(stack)) {
                    continue outer;
                }
            }
            return false;
        }
        return true;
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
        boolean handled = dev.emi.emi.registry.EmiDragDropHandlers.dropStack(screen, stack, (int) mouseX, (int) mouseY);
        dev.emi.emi.screen.EmiScreenManager.draggedStack = EmiStack.EMPTY;
        return handled;
    }

    static boolean canStartDrag(Screen screen, ItemStack stack) {
        if (stack == null || stack.isEmpty() || screen == null) {
            return false;
        }
        try {
            for (var entry : dev.emi.emi.registry.EmiDragDropHandlers.fromClass.entrySet()) {
                List<dev.emi.emi.api.EmiDragDropHandler<?>> handlers = entry.getValue();
                if (handlers == null || handlers.isEmpty()) continue;
                Class<?> handledScreen = entry.getKey();
                if (handledScreen != null && handledScreen.isAssignableFrom(screen.getClass())) {
                    return true;
                }
            }

            return !dev.emi.emi.registry.EmiDragDropHandlers.generic.isEmpty();
        } catch (RuntimeException | LinkageError ignored) {
            return screen instanceof AbstractContainerScreen<?>;
        }
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

    static boolean transfer(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen screen, boolean maxTransfer,
                            boolean toCursor) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        EmiRecipe emiRecipe = emiGetRecipeById(recipe.id());
        if (emiRecipe == null) {
            return false;
        }
        int amount = maxTransfer ? Integer.MAX_VALUE : 1;
        return EmiRecipeFiller.performFill(emiRecipe, containerScreen, EmiCraftContext.Type.CRAFTABLE,
                EmiCraftContext.Destination.NONE, amount);
    }

    static boolean transferStack(ItemStack stack, Screen screen, boolean maxTransfer) {
        if (stack == null || stack.isEmpty() || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        EmiRecipe recipe = preferredCraftableRecipe(stack, containerScreen);
        if (recipe == null) {
            return false;
        }
        int amount = maxTransfer ? Integer.MAX_VALUE : 1;
        return EmiRecipeFiller.performFill(recipe, containerScreen, EmiCraftContext.Type.CRAFTABLE,
                EmiCraftContext.Destination.NONE, amount);
    }

    static boolean canTransferStack(ItemStack stack, Screen screen) {
        return stack != null
                && !stack.isEmpty()
                && screen instanceof AbstractContainerScreen<?> containerScreen
                && preferredCraftableRecipe(stack, containerScreen) != null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static EmiRecipe preferredCraftableRecipe(ItemStack stack, AbstractContainerScreen<?> screen) {
        EmiStack output = EmiStack.of(stack);
        for (EmiRecipe recipe : EmiApi.getRecipeManager().getRecipesByOutput(output)) {
            EmiRecipeHandler handler = EmiRecipeFiller.getFirstValidHandler(recipe, screen);
            if (handler == null || !handler.supportsRecipe(recipe)) {
                continue;
            }
            EmiCraftContext context = new EmiCraftContext<>(
                    screen, handler.getInventory(screen), EmiCraftContext.Type.CRAFTABLE,
                    EmiCraftContext.Destination.NONE, 1);
            if (handler.canCraft(recipe, context)) {
                return recipe;
            }
        }
        var player = net.minecraft.client.Minecraft.getInstance().player;
        return EmiUtil.getPreferredRecipe(output, EmiPlayerInventory.of(player), true);
    }

    static boolean canTransfer(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        EmiRecipe emiRecipe = emiGetRecipeById(recipe.id());
        if (emiRecipe == null) {
            return false;
        }
        try {
            return canTransferUnchecked(emiRecipe, containerScreen);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> boolean canTransferUnchecked(
            EmiRecipe recipe, AbstractContainerScreen<T> screen) {
        EmiRecipeHandler<T> handler = EmiRecipeFiller.getFirstValidHandler(recipe, screen);
        if (handler == null || !handler.supportsRecipe(recipe)) {
            return false;
        }
        EmiCraftContext<T> context = new EmiCraftContext<>(
                screen, handler.getInventory(screen), EmiCraftContext.Type.CRAFTABLE,
                EmiCraftContext.Destination.NONE, 1);
        return handler.canCraft(recipe, context);
    }
}
