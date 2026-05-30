package com.sanhiruzu.ami.neoforge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.GrindstoneRepairRecipeView;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrindstoneRepairRecipe extends AmiSpecialRecipe implements Recipe<RecipeInput>, GrindstoneRepairRecipeView {
    private final ItemStack tool1;
    private final ItemStack tool2;

    public GrindstoneRepairRecipe(ItemStack tool1, ItemStack tool2, ItemStack output, RecipeType<?> type) {
        super(output, type, List.of(Ingredient.of(tool1), Ingredient.of(tool2)));
        this.tool1 = tool1;
        this.tool2 = tool2;
    }

    public ItemStack getTool1() {
        return tool1;
    }

    public ItemStack getTool2() {
        return tool2;
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
