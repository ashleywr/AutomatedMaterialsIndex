package dev.emi.emi.api;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;

public interface EmiPlugin {
    default void initialize(EmiInitRegistry registry) {}
    void register(EmiRegistry registry);
}
