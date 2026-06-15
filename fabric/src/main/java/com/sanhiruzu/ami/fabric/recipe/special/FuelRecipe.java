package com.sanhiruzu.ami.fabric.recipe.special;

import com.sanhiruzu.ami.recipe.special.FuelRecipeView;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class FuelRecipe extends FabricSpecialRecipe implements FuelRecipeView {
    private final ItemStack stack;
    private final int time;

    public FuelRecipe(ItemStack stack, int time, RecipeType<?> type) {
        super(ItemStack.EMPTY, type, List.of(Ingredient.of(stack)));
        this.stack = stack;
        this.time = time;
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
        return resultCopy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return result();
    }
}
