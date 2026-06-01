package com.sanhiruzu.ami.forge.recipe.special;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

final class ForgeSpecialRecipeData {
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;
    private final RecipeType<?> type;

    ForgeSpecialRecipeData(ItemStack result, RecipeType<?> type, List<Ingredient> ingredients) {
        this.result = result;
        this.type = type;
        this.ingredients = NonNullList.create();
        this.ingredients.addAll(ingredients);
    }

    ItemStack result() {
        return result;
    }

    ItemStack resultCopy() {
        return result.copy();
    }

    NonNullList<Ingredient> ingredientsCopy() {
        NonNullList<Ingredient> copy = NonNullList.create();
        copy.addAll(ingredients);
        return copy;
    }

    RecipeType<?> type() {
        return type;
    }

    RecipeSerializer<?> serializer() {
        return null;
    }
}
