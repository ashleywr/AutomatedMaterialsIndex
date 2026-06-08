package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Forge production jars run this 1.20.1 class with SRG method names and no AMI refmap.
// Priority 500 ensures AMI fires before EMI's RecipeBookWidgetMixin (default priority 1000).
// See RecipeBookComponentMixin for the full explanation of the priority contract.
// Shadow method names use official (Mojang) names, not SRG names — consistent with the
// injection target convention verified by MixinConfigTest.forgeRecipeBookMixinTargetsDevAndProductionNames.
@Mixin(value = RecipeBookComponent.class, priority = 500, remap = false)
public abstract class ForgeRecipeBookComponentMixin {

    @Shadow public abstract boolean isVisible();
    @Shadow protected abstract void setVisible(boolean visible);

    @Inject(method = {"toggleVisibility", "m_100384_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void onRecipeBookButtonClicked(CallbackInfo ci) {
        switch (InventoryOverlayHandler.recipeBookIntercept()) {
            case VANILLA    -> { setVisible(!isVisible()); ci.cancel(); }
            case AMI_TOGGLE -> { InventoryOverlayHandler.handleRecipeBookToggle(); ci.cancel(); }
            case PASS       -> {}
        }
    }
}
