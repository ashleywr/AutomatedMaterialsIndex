package com.sanhiruzu.ami.neoforge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.AnvilRepairRecipeView;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class AnvilRepairRecipe extends AmiSpecialRecipe implements Recipe<RecipeInput>, AnvilRepairRecipeView {
    private final ItemStack tool;
    private final Ingredient material;

    public AnvilRepairRecipe(ItemStack tool, Ingredient material, ItemStack output, RecipeType<?> type) {
        super(output, type, List.of(Ingredient.of(tool.getItem()), material));
        this.tool = tool;
        this.material = material;
    }

    public ItemStack getTool() { return tool; }
    public Ingredient getMaterial() { return material; }
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
