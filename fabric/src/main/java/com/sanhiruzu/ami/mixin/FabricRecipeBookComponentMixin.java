package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric variant of {@code RecipeBookComponentMixin}. Identical behavior, but targets
 * the <em>vanilla</em> {@link RecipeBookComponent} with {@code remap = true} (the default)
 * so the refmap translates the Mojang-named members (isVisible/setVisible/toggleVisibility)
 * to their intermediary names at runtime. The shared xplat mixin uses {@code remap = false},
 * which is correct on NeoForge/Forge (Mojang-named runtime) but fails on Fabric's
 * intermediary-named runtime. The xplat copy is therefore left out of the Fabric mixin
 * config in favor of this one.
 *
 * <p>Priority 500 keeps AMI ahead of EMI's RecipeBookWidgetMixin (default priority 1000),
 * matching the xplat mixin.
 */
@Mixin(value = RecipeBookComponent.class, priority = 500)
public abstract class FabricRecipeBookComponentMixin {

    @Shadow public abstract boolean isVisible();

    @Shadow protected abstract void setVisible(boolean visible);

    @Inject(method = "toggleVisibility", at = @At("HEAD"), cancellable = true)
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
