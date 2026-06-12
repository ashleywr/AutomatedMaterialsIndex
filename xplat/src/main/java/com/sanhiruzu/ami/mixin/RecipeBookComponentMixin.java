package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Priority 500 ensures AMI fires before EMI's RecipeBookWidgetMixin (default priority 1000).
// EMI targets the same method as "toggleOpen" with remap=true, which resolves to toggleVisibility
// at runtime. Without a lower priority, EMI would intercept the button before AMI could cancel it.
// This priority relationship is verified by MixinConfigTest.recipeBookMixinPriorityIsLowerThanEmi().
@Mixin(value = RecipeBookComponent.class, priority = 500, remap = false)
public abstract class RecipeBookComponentMixin {

    @Shadow public abstract boolean isVisible();
    @Shadow protected abstract void setVisible(boolean visible);

    @Inject(method = "toggleVisibility", at = @At("HEAD"), cancellable = true, remap = false)
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
