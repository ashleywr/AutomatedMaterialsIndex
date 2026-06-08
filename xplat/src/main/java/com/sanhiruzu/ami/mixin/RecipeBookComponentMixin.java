package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RecipeBookComponent.class, priority = 500, remap = false)
public abstract class RecipeBookComponentMixin {
    @Inject(method = "toggleVisibility", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRecipeBookButtonClicked(CallbackInfo ci) {
        if (!InventoryOverlayHandler.shouldInterceptRecipeBook()) return;
        InventoryOverlayHandler.handleRecipeBookToggle();
        ci.cancel();
    }
}
