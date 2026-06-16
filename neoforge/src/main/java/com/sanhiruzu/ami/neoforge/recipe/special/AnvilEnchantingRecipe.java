package com.sanhiruzu.ami.neoforge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.AnvilEnchantingRecipeView;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class AnvilEnchantingRecipe extends AmiSpecialRecipe implements Recipe<RecipeInput>, AnvilEnchantingRecipeView {
    private final ItemStack tool;
    private final ItemStack book;

    public AnvilEnchantingRecipe(ItemStack tool, ItemStack book, ItemStack enchantedTool, RecipeType<?> type) {
        super(enchantedTool, type, List.of(Ingredient.of(tool.getItem()), Ingredient.of(book.getItem())));
        this.tool = tool;
        this.book = book;
    }

    public ItemStack getTool() { return tool; }
    public ItemStack getBook() { return book; }
    public ItemStack getEnchantedTool() { return result(); }

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
