package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import mezz.jei.gui.input.ClientInputHandler;
import mezz.jei.gui.input.UserInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(ClientInputHandler.class)
public class JeiClientInputHandlerMixin {

    @Inject(method = "onInitGui", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressOnInitGui(CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "onKeyboardKeyPressedPre", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressKeyPressedPre(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onKeyboardKeyPressedPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressKeyPressedPost(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onKeyboardCharTypedPre", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressCharTypedPre(Screen screen, char codePoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onKeyboardCharTypedPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressCharTypedPost(Screen screen, char codePoint, int modifiers, CallbackInfo ci) {
        if (shouldSuppressJeiInput()) {
            ci.cancel();
        }
    }

    @Inject(method = "onGuiMouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressMouseClicked(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onGuiMouseReleased", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressMouseReleased(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onGuiMouseScroll", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressMouseScroll(double mouseX, double mouseY, double scrollDelta, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    private static boolean shouldSuppressJeiChrome() {
        return InventoryOverlayHandler.isAmiEnabled();
    }

    private static boolean shouldSuppressJeiInput() {
        if (!InventoryOverlayHandler.isAmiEnabled()) return false;
        if (RecipeViewerBridge.isRecipeViewActive()) return false;
        if (isJeiRecipeScreenActive()) return false;
        return true;
    }

    private static boolean isJeiRecipeScreenActive() {
        Screen screen = Minecraft.getInstance().screen;
        return screen != null && screen.getClass().getName().equals("mezz.jei.gui.recipes.RecipesGui");
    }
}
