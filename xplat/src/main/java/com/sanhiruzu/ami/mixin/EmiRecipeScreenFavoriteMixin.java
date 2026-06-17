package com.sanhiruzu.ami.mixin;

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
        if (ami$handleFavoriteKey(this, keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }

    private static boolean ami$handleFavoriteKey(Object screen, int keyCode, int scanCode, int modifiers) {
        try {
            Object handled = Class.forName("com.sanhiruzu.ami.compat.EmiRecipeScreenFavoriteMixinSupport")
                    .getMethod("handleFavoriteKey", Object.class, int.class, int.class, int.class)
                    .invoke(null, screen, keyCode, scanCode, modifiers);
            return handled instanceof Boolean result && result;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
