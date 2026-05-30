package com.sanhiruzu.ami.neoforge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.PotionBrewingRecipeView;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class PotionBrewingRecipe extends AmiSpecialRecipe implements Recipe<RecipeInput>, PotionBrewingRecipeView {
    private final ItemStack input;
    private final Ingredient ingredient;

    public PotionBrewingRecipe(ItemStack input, Ingredient ingredient, ItemStack output, RecipeType<?> type) {
        super(output, type, List.of(Ingredient.of(input), ingredient));
        this.input = input;
        this.ingredient = ingredient;
    }

    public ItemStack getInput() {
        return input;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getOutput() {
        return result();
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
