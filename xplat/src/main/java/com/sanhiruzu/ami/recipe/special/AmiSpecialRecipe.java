package com.sanhiruzu.ami.recipe.special;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

// Deliberately does NOT implement Recipe: this base is shared with the 1.20.1 Forge
// module, where Recipe's type argument is Container and RecipeInput does not exist.
// Each loader's concrete subclasses declare the loader-appropriate Recipe<...> interface.
// On Fabric, the intermediate FabricSpecialRecipe re-declares the vanilla methods below so
// the remapper recognizes them as interface implementations (see that class for details).
public abstract class AmiSpecialRecipe {
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;
    private final RecipeType<?> type;

    protected AmiSpecialRecipe(ItemStack result, RecipeType<?> type, List<Ingredient> ingredients) {
        this.result = result;
        this.type = type;
        this.ingredients = NonNullList.create();
        this.ingredients.addAll(ingredients);
    }

    protected ItemStack result() {
        return result;
    }

    protected ItemStack resultCopy() {
        return result.copy();
    }

    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> copy = NonNullList.create();
        copy.addAll(ingredients);
        return copy;
    }

    public boolean isSpecial() {
        return true;
    }

    public RecipeSerializer<?> getSerializer() {
        return null;
    }

    public RecipeType<?> getType() {
        return type;
    }
}
