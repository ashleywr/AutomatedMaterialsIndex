package com.sanhiruzu.ami.fabric.recipe.special;

import com.sanhiruzu.ami.recipe.special.CompostingRecipeView;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class CompostingRecipe extends FabricSpecialRecipe implements CompostingRecipeView {
    private final ItemStack stack;
    private final float chance;

    public CompostingRecipe(ItemStack stack, float chance, RecipeType<?> type) {
        super(new ItemStack(Items.BONE_MEAL), type, List.of(Ingredient.of(stack)));
        this.stack = stack;
        this.chance = chance;
    }

    public ItemStack getStack() {
        return stack;
    }

    public float getChance() {
        return chance;
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
