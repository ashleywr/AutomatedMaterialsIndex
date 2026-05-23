package com.sanhiruzu.ami.index.special;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class PotionBrewingRecipe implements Recipe<RecipeInput> {
    private final ItemStack input;
    private final Ingredient ingredient;
    private final ItemStack output;
    private final RecipeType<?> type;

    public PotionBrewingRecipe(ItemStack input, Ingredient ingredient, ItemStack output, RecipeType<?> type) {
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
        this.type = type;
    }

    public ItemStack getInput() {
        return input;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getOutput() {
        return output;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(Ingredient.of(input));
        list.add(ingredient);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return null;
    }

    @Override
    public RecipeType<?> getType() {
        return type;
    }
}
