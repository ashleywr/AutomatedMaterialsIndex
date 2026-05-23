package com.sanhiruzu.ami.emi;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/**
 * Synthetic Recipe that wraps an {@link AmiCapturedRecipe} so it can be stored
 * in {@link com.sanhiruzu.ami.index.AmiRecipeIndex} and rendered by
 * {@link com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper}.
 */
public class CapturedRecipe implements Recipe<RecipeInput> {

    private final AmiCapturedRecipe captured;
    private final RecipeType<?> type;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;

    public CapturedRecipe(AmiCapturedRecipe captured, RecipeType<?> type) {
        this.captured = captured;
        this.type = type;
        this.ingredients = NonNullList.create();
        ItemStack primaryOutput = ItemStack.EMPTY;
        for (var slot : captured.slots()) {
            if (!slot.isOutput()) {
                var stacks = slot.alternatives().stream()
                    .filter(s -> !s.isEmpty())
                    .toArray(ItemStack[]::new);
                if (stacks.length > 0) {
                    ingredients.add(Ingredient.of(stacks));
                }
            } else if (primaryOutput.isEmpty() && !slot.alternatives().isEmpty()) {
                primaryOutput = slot.alternatives().get(0).copy();
            }
        }
        this.result = primaryOutput;
    }

    public AmiCapturedRecipe getCaptured() { return captured; }

    @Override
    public boolean matches(RecipeInput input, net.minecraft.world.level.Level level) { return false; }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return result.copy(); }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) { return result.copy(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return null; }

    @Override
    public RecipeType<?> getType() { return type; }

    @Override
    public NonNullList<Ingredient> getIngredients() { return ingredients; }

    @Override
    public ItemStack getToastSymbol() { return captured.categoryIcon().copy(); }

    @Override
    public boolean isSpecial() { return true; }
}
