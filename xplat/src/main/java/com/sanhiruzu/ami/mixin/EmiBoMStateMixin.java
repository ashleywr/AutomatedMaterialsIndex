package com.sanhiruzu.ami.mixin;

import com.google.gson.JsonObject;
import com.sanhiruzu.ami.compat.EmiStateMixinSupport;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.data.RecipeDefaults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = BoM.class, remap = false)
public class EmiBoMStateMixin {
    @Inject(method = "setDefaults", at = @At("RETURN"), remap = false)
    private static void ami$defaultsSet(RecipeDefaults defaults, CallbackInfo ci) {
        EmiStateMixinSupport.recipesChanged();
    }

    @Inject(method = "loadAdded", at = @At("RETURN"), remap = false)
    private static void ami$addedDefaultsLoaded(JsonObject object, CallbackInfo ci) {
        EmiStateMixinSupport.recipesChanged();
    }

    @Inject(method = "reload", at = @At("RETURN"), remap = false)
    private static void ami$defaultsReloaded(CallbackInfo ci) {
        EmiStateMixinSupport.recipesChanged();
    }

    @Inject(method = "setGoal", at = @At("RETURN"), remap = false)
    private static void ami$goalChanged(EmiRecipe recipe, CallbackInfo ci) {
        EmiStateMixinSupport.recipesChanged();
    }

    @Inject(method = "addRecipe(Ldev/emi/emi/api/recipe/EmiRecipe;)V", at = @At("RETURN"), remap = false)
    private static void ami$recipeAdded(EmiRecipe recipe, CallbackInfo ci) {
        EmiStateMixinSupport.recipesChanged();
    }

    @Inject(method = "addRecipe(Ldev/emi/emi/api/stack/EmiIngredient;Ldev/emi/emi/api/recipe/EmiRecipe;)V", at = @At("RETURN"), remap = false)
    private static void ami$recipeResolutionAdded(EmiIngredient stack, EmiRecipe recipe, CallbackInfo ci) {
        EmiStateMixinSupport.recipesChanged();
    }

    @Inject(method = "removeRecipe(Ldev/emi/emi/api/recipe/EmiRecipe;)V", at = @At("RETURN"), remap = false)
    private static void ami$recipeRemoved(EmiRecipe recipe, CallbackInfo ci) {
        EmiStateMixinSupport.recipesChanged();
    }

    @Inject(method = "removeRecipe(Ldev/emi/emi/api/stack/EmiIngredient;Ldev/emi/emi/api/recipe/EmiRecipe;)V", at = @At("RETURN"), remap = false)
    private static void ami$recipeResolutionRemoved(EmiIngredient stack, EmiRecipe recipe, CallbackInfo ci) {
        EmiStateMixinSupport.recipesChanged();
    }

    @Inject(method = "addResolution", at = @At("RETURN"), remap = false)
    private static void ami$resolutionAdded(EmiIngredient ingredient, EmiRecipe recipe, CallbackInfo ci) {
        EmiStateMixinSupport.recipesChanged();
    }
}
