package com.sanhiruzu.ami.compat;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.runtime.EmiFavorites;
import net.minecraft.core.registries.BuiltInRegistries;
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
}
