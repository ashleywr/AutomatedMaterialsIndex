package com.sanhiruzu.ami.neoforge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.GrindstoneRepairRecipeView;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrindstoneRepairRecipe extends AmiSpecialRecipe implements Recipe<RecipeInput>, GrindstoneRepairRecipeView {
    private final ItemStack tool1;
    private final ItemStack tool2;

    public GrindstoneRepairRecipe(ItemStack tool1, ItemStack tool2, ItemStack output, RecipeType<?> type) {
        super(output, type, List.of(Ingredient.of(tool1.getItem()), Ingredient.of(tool2.getItem())));
        this.tool1 = tool1;
        this.tool2 = tool2;
    }

    public ItemStack getTool1() { return tool1; }
    public ItemStack getTool2() { return tool2; }
    public ItemStack getOutput() { return result(); }

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
