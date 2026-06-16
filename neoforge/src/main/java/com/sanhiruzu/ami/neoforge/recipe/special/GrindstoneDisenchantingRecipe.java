package com.sanhiruzu.ami.neoforge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.GrindstoneDisenchantingRecipeView;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrindstoneDisenchantingRecipe extends AmiSpecialRecipe implements Recipe<RecipeInput>, GrindstoneDisenchantingRecipeView {
    private final ItemStack enchanted;

    public GrindstoneDisenchantingRecipe(ItemStack enchanted, ItemStack normal, RecipeType<?> type) {
        super(normal, type, List.of(Ingredient.of(enchanted.getItem())));
        this.enchanted = enchanted;
    }

    public ItemStack getEnchanted() { return enchanted; }
    public ItemStack getNormal() { return result(); }

    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input) { return resultCopy(); }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public RecipeBookCategory recipeBookCategory() { return new RecipeBookCategory(); }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public net.minecraft.world.item.crafting.RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() { return null; }
    @Override @SuppressWarnings("unchecked")
    public RecipeType<? extends Recipe<RecipeInput>> getType() { return (RecipeType<? extends Recipe<RecipeInput>>) super.getType(); }
}
