package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.compat.EmiRecipeScreenFavoriteMixinSupport;
import dev.emi.emi.screen.RecipeScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = RecipeScreen.class, remap = false)
public class EmiRecipeScreenFavoriteMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private void ami$toggleHoveredFavorite(int keyCode, int scanCode, int modifiers,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (EmiRecipeScreenFavoriteMixinSupport.handleFavoriteKey(this, keyCode, scanCode)) {
            cir.setReturnValue(true);
        }
    }
}
