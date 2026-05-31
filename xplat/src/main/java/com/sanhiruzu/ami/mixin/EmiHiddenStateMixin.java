package com.sanhiruzu.ami.mixin;

import com.google.gson.JsonArray;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.runtime.EmiHidden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = EmiHidden.class, remap = false)
public class EmiHiddenStateMixin {
    @Inject(method = "reload", at = @At("RETURN"), remap = false)
    private static void ami$visibilityReloaded(CallbackInfo ci) {
        ami$notifyVisibilityChanged();
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private static void ami$visibilityLoaded(JsonArray arr, CallbackInfo ci) {
        ami$notifyVisibilityChanged();
    }

    @Inject(method = "setVisibility", at = @At("RETURN"), remap = false)
    private static void ami$visibilityChanged(EmiIngredient stack, boolean hide, boolean similar, CallbackInfo ci) {
        ami$notifyVisibilityChanged();
    }

    private static void ami$notifyVisibilityChanged() {
        try {
            Class.forName("com.sanhiruzu.ami.compat.RecipeViewerStateSync")
                    .getMethod("visibilityChanged")
                    .invoke(null);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
