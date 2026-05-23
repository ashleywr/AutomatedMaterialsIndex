package dev.emi.emi.api;

import java.util.function.Consumer;
import java.util.function.Predicate;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public interface EmiRegistry {
    void addCategory(EmiRecipeCategory category);
    void addWorkstation(EmiRecipeCategory category, EmiIngredient workstation);
    void addRecipe(EmiRecipe recipe);
    void removeRecipes(Predicate<EmiRecipe> predicate);
    default void removeRecipes(net.minecraft.resources.ResourceLocation id) {
        removeRecipes(r -> id.equals(r.getId()));
    }
    void addDeferredRecipes(Consumer<Consumer<EmiRecipe>> consumer);
    void addEmiStack(EmiStack stack);
    void addEmiStackAfter(EmiStack stack, Predicate<EmiStack> predicate);

    default void addEmiStackAfter(EmiStack stack, EmiStack other) {
        addEmiStackAfter(stack, s -> s.equals(other));
    }

    void removeEmiStacks(Predicate<EmiStack> predicate);
    default void removeEmiStacks(EmiStack stack) { removeEmiStacks(s -> s.equals(stack)); }

    <T extends Screen> void addExclusionArea(Class<T> clazz, EmiExclusionArea<T> area);
    void addGenericExclusionArea(EmiExclusionArea<Screen> area);

    <T extends Screen> void addDragDropHandler(Class<T> clazz, EmiDragDropHandler<T> handler);
    void addGenericDragDropHandler(EmiDragDropHandler<Screen> handler);

    <T extends Screen> void addStackProvider(Class<T> clazz, EmiStackProvider<T> provider);
    void addGenericStackProvider(EmiStackProvider<Screen> provider);

    void addRecipeHandler(net.minecraft.world.inventory.MenuType<?> type, Object handler);
    void addGenericRecipeHandler(Object handler);
}
