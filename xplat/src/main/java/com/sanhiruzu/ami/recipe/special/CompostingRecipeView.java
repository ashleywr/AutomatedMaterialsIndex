package com.sanhiruzu.ami.recipe.special;

import net.minecraft.world.item.ItemStack;

public interface CompostingRecipeView {
    ItemStack getStack();

    float getChance();
}
