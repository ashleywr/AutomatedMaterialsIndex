package com.sanhiruzu.ami.fabric.recipe.special;

import com.sanhiruzu.ami.recipe.special.AnvilRepairRecipeView;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class AnvilRepairRecipe extends FabricSpecialRecipe implements AnvilRepairRecipeView {
    private final ItemStack tool;
    private final Ingredient material;

    public AnvilRepairRecipe(ItemStack tool, Ingredient material, ItemStack output, RecipeType<?> type) {
        super(output, type, List.of(Ingredient.of(tool), material));
        this.tool = tool;
        this.material = material;
    }

    public ItemStack getTool() {
        return tool;
    }

    public Ingredient getMaterial() {
        return material;
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
