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
// Unlike the injection target list, @Shadow members still need explicit aliases when Forge
// production jars run without an AMI refmap. The production SRG names are m_100385_ and
// m_100369_ for isVisible/setVisible on 1.20.1 RecipeBookComponent.
@Mixin(value = RecipeBookComponent.class, priority = 500, remap = false)
public abstract class ForgeRecipeBookComponentMixin {

    @Shadow(aliases = "m_100385_") public abstract boolean isVisible();
    @Shadow(aliases = "m_100369_") protected abstract void setVisible(boolean visible);

    @Inject(method = {"toggleVisibility", "m_100384_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void onRecipeBookButtonClicked(CallbackInfo ci) {
        var mode = InventoryOverlayHandler.recipeBookIntercept();
        if (mode == InventoryOverlayHandler.RecipeBookIntercept.VANILLA) {
            setVisible(!isVisible());
            ci.cancel();
        } else if (mode == InventoryOverlayHandler.RecipeBookIntercept.AMI_TOGGLE) {
            InventoryOverlayHandler.handleRecipeBookToggle();
            ci.cancel();
        }
    }
}
