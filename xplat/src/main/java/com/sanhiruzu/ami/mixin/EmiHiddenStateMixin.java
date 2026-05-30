package com.sanhiruzu.ami.mixin;

import com.google.gson.JsonArray;
import com.sanhiruzu.ami.compat.EmiStateMixinSupport;
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
        EmiStateMixinSupport.visibilityChanged();
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private static void ami$visibilityLoaded(JsonArray arr, CallbackInfo ci) {
        EmiStateMixinSupport.visibilityChanged();
    }

    @Inject(method = "setVisibility", at = @At("RETURN"), remap = false)
    private static void ami$visibilityChanged(EmiIngredient stack, boolean hide, boolean similar, CallbackInfo ci) {
        EmiStateMixinSupport.visibilityChanged();
    }
}
