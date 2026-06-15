package com.sanhiruzu.ami.fabric.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.AnvilEnchantingRecipeView;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class AnvilEnchantingRecipe extends AmiSpecialRecipe implements Recipe<RecipeInput>, AnvilEnchantingRecipeView {
    private final ItemStack tool;
    private final ItemStack book;

    public AnvilEnchantingRecipe(ItemStack tool, ItemStack book, ItemStack enchantedTool, RecipeType<?> type) {
        super(enchantedTool, type, List.of(Ingredient.of(tool), Ingredient.of(book)));
        this.tool = tool;
        this.book = book;
    }

    public ItemStack getTool() {
        return tool;
    }

    public ItemStack getBook() {
        return book;
    }

    public ItemStack getEnchantedTool() {
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
