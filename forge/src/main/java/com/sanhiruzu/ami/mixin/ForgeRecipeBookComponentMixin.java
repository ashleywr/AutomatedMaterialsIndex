package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Forge production jars run this 1.20.1 class with SRG method names and no AMI refmap.
@Mixin(value = RecipeBookComponent.class, priority = 500, remap = false)
public abstract class ForgeRecipeBookComponentMixin {
    @Inject(method = {"toggleVisibility", "m_100384_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void onRecipeBookButtonClicked(CallbackInfo ci) {
        if (!InventoryOverlayHandler.shouldInterceptRecipeBook()) return;
        InventoryOverlayHandler.handleRecipeBookToggle();
        ci.cancel();
    }
}
