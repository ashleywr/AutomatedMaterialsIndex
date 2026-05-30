package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.favorites.FavoriteEntry;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.runtime.EmiFavorites;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct EMI favorites API calls — only referenced behind a ModList.isLoaded("emi") guard
 * so this class is never loaded when EMI is absent.
 */
public final class EmiFavoritesBridge {
    private EmiFavoritesBridge() {
    }

    public static boolean isFavorite(ResourceLocation id) {
        for (EmiFavorite favorite : EmiFavorites.favorites) {
            EmiIngredient stack = favorite.getStack();
            if (stack instanceof EmiStack emiStack && emiStack.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static void addFavorite(ItemStack stack) {
        EmiFavorites.addFavorite(EmiStack.of(stack));
    }

    public static void addFavoriteAt(ItemStack stack, int index) {
        EmiFavorites.removeFavorite(EmiStack.of(stack));
        if (index < 0 || index > EmiFavorites.favorites.size()) {
            EmiFavorites.addFavorite(EmiStack.of(stack));
        } else {
            EmiFavorites.addFavoriteAt(EmiStack.of(stack), index);
        }
    }

    public static void removeFavorite(ItemStack stack) {
        EmiFavorites.removeFavorite(EmiStack.of(stack));
    }

    public static void addRecipeFavorite(ItemStack stack, ResourceLocation recipeId) {
        EmiRecipe recipe = recipeId == null ? null : EmiApi.getRecipeManager().getRecipe(recipeId);
        if (recipe != null) {
            EmiFavorites.addFavorite(EmiStack.of(stack), recipe);
        } else {
            EmiFavorites.addFavorite(EmiStack.of(stack));
        }
    }

    public static void removeRecipeFavorite(ItemStack stack, ResourceLocation recipeId) {
        EmiRecipe recipe = recipeId == null ? null : EmiApi.getRecipeManager().getRecipe(recipeId);
        if (recipe == null) {
            EmiFavorites.removeFavorite(EmiStack.of(stack));
            return;
        }
        EmiFavorites.favorites.removeIf(favorite -> {
            EmiIngredient ingredient = favorite.getStack();
            EmiRecipe favoriteRecipe = favorite.getRecipe();
            if (!(ingredient instanceof EmiStack emiStack) || favoriteRecipe == null || favoriteRecipe.getId() == null) {
                return false;
            }
            return favoriteRecipe.getId().equals(recipeId) && emiStack.getId().equals(EmiStack.of(stack).getId());
        });
    }

    public static List<ResourceLocation> getFavoriteIds() {
        List<ResourceLocation> result = new ArrayList<>();
        for (EmiFavorite favorite : EmiFavorites.favorites) {
            EmiIngredient stack = favorite.getStack();
            if (stack instanceof EmiStack emiStack) {
                result.add(emiStack.getId());
            }
        }
        return result;
    }

    public static List<FavoriteEntry> getFavoriteEntries() {
        List<FavoriteEntry> result = new ArrayList<>();
        for (EmiFavorite favorite : EmiFavorites.favorites) {
            EmiIngredient ingredient = favorite.getStack();
            ItemStack stack = firstItemStack(ingredient);
            if (stack.isEmpty()) continue;

            EmiRecipe recipe = favorite.getRecipe();
            ResourceLocation recipeId = recipe == null ? null : recipe.getId();
            FavoriteEntry entry = recipeId == null
                    ? FavoriteEntry.item(stack, "emi")
                    : FavoriteEntry.recipe(stack, recipeId, "emi");
            if (entry != null) {
                result.add(entry);
            }
        }
        return result;
    }

    private static ItemStack firstItemStack(EmiIngredient ingredient) {
        if (ingredient instanceof EmiStack emiStack) {
            return emiStack.getItemStack().copy();
        }
        for (EmiStack stack : ingredient.getEmiStacks()) {
            ItemStack itemStack = stack.getItemStack();
            if (!itemStack.isEmpty()) {
                return itemStack.copy();
            }
        }
        return ItemStack.EMPTY;
    }
}
