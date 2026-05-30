package com.sanhiruzu.ami.neoforge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.GrindstoneDisenchantingRecipeView;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrindstoneDisenchantingRecipe extends AmiSpecialRecipe implements Recipe<RecipeInput>, GrindstoneDisenchantingRecipeView {
    private final ItemStack enchanted;

    public GrindstoneDisenchantingRecipe(ItemStack enchanted, ItemStack normal, RecipeType<?> type) {
        super(normal, type, List.of(Ingredient.of(enchanted)));
        this.enchanted = enchanted;
    }

    public ItemStack getEnchanted() {
        return enchanted;
    }

    public ItemStack getNormal() {
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
