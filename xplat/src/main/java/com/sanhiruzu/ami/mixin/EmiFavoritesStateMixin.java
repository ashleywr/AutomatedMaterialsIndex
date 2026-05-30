package com.sanhiruzu.ami.mixin;

import com.google.gson.JsonArray;
import com.sanhiruzu.ami.compat.EmiStateMixinSupport;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.runtime.EmiFavorites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = EmiFavorites.class, remap = false)
public class EmiFavoritesStateMixin {
    @Inject(method = "addFavorite(Ldev/emi/emi/api/stack/EmiIngredient;)V", at = @At("RETURN"), remap = false)
    private static void ami$favoriteAdded(EmiIngredient stack, CallbackInfo ci) {
        EmiStateMixinSupport.favoritesChanged();
    }

    @Inject(method = "addFavorite(Ldev/emi/emi/api/stack/EmiIngredient;Ldev/emi/emi/api/recipe/EmiRecipe;)V", at = @At("RETURN"), remap = false)
    private static void ami$recipeFavoriteAdded(EmiIngredient stack, EmiRecipe recipe, CallbackInfo ci) {
        EmiStateMixinSupport.favoritesChanged();
    }

    @Inject(method = "addFavoriteAt", at = @At("RETURN"), remap = false)
    private static void ami$favoriteMovedOrAdded(EmiIngredient stack, int offset, CallbackInfo ci) {
        EmiStateMixinSupport.favoritesChanged();
    }

    @Inject(method = "removeFavorite", at = @At("RETURN"), remap = false)
    private static void ami$favoriteRemoved(EmiIngredient stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            EmiStateMixinSupport.favoritesChanged();
        }
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private static void ami$favoritesLoaded(JsonArray arr, CallbackInfo ci) {
        EmiStateMixinSupport.favoritesChanged();
    }

    @Inject(method = "updateSynthetic", at = @At("RETURN"), remap = false)
    private static void ami$syntheticFavoritesChanged(EmiPlayerInventory inv, CallbackInfo ci) {
        EmiStateMixinSupport.favoritesChanged();
    }
}
