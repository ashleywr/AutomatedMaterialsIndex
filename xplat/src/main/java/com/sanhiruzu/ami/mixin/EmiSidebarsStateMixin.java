package com.sanhiruzu.ami.mixin;

import com.google.gson.JsonObject;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.runtime.EmiSidebars;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = EmiSidebars.class, remap = false)
public class EmiSidebarsStateMixin {
    @Inject(method = "lookup", at = @At("RETURN"), remap = false)
    private static void ami$lookupHistoryChanged(EmiIngredient stack, CallbackInfo ci) {
        ami$notifySidebarsChanged();
    }

    @Inject(method = "craft", at = @At("RETURN"), remap = false)
    private static void ami$craftHistoryChanged(EmiRecipe recipe, CallbackInfo ci) {
        ami$notifySidebarsChanged();
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private static void ami$sidebarsLoaded(JsonObject json, CallbackInfo ci) {
        ami$notifySidebarsChanged();
    }

    private static void ami$notifySidebarsChanged() {
        try {
            Class.forName("com.sanhiruzu.ami.compat.RecipeViewerStateSync")
                    .getMethod("sidebarsChanged")
                    .invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
