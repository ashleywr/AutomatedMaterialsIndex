package com.sanhiruzu.ami.recipe.special;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public interface AnvilRepairRecipeView {
    ItemStack getTool();

    Ingredient getMaterial();
}
