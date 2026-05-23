package com.sanhiruzu.ami.index.special;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class FuelRecipe implements Recipe<RecipeInput> {
    private final ItemStack stack;
    private final int time;
    private final RecipeType<?> type;

    public FuelRecipe(ItemStack stack, int time, RecipeType<?> type) {
        this.stack = stack;
        this.time = time;
        this.type = type;
    }

    public ItemStack getStack() {
        return stack;
    }

    public int getTime() {
        return time;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(Ingredient.of(stack));
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
