package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.compat.JeiClientInputHandlerMixinSupport;
import mezz.jei.gui.input.ClientInputHandler;
import mezz.jei.gui.input.UserInput;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses JEI's overlay input handling when AMI is active. Forge targets
 * Minecraft 1.20.1 / JEI 15, where mouse scroll has a single scroll delta.
 */
@Pseudo
@Mixin(ClientInputHandler.class)
public class JeiClientInputHandlerMixin {
    @Inject(method = "onInitGui", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressOnInitGui(CallbackInfo ci) {
        if (JeiClientInputHandlerMixinSupport.shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "onKeyboardKeyPressedPre", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressKeyPressedPre(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (JeiClientInputHandlerMixinSupport.shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onKeyboardKeyPressedPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressKeyPressedPost(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (JeiClientInputHandlerMixinSupport.shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onKeyboardCharTypedPre", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressCharTypedPre(Screen screen, char codePoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (JeiClientInputHandlerMixinSupport.shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onKeyboardCharTypedPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressCharTypedPost(Screen screen, char codePoint, int modifiers, CallbackInfo ci) {
        if (JeiClientInputHandlerMixinSupport.shouldSuppressJeiInput()) {
            ci.cancel();
        }
    }

    @Inject(method = "onGuiMouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressMouseClicked(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (JeiClientInputHandlerMixinSupport.shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onGuiMouseReleased", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressMouseReleased(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (JeiClientInputHandlerMixinSupport.shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onGuiMouseScroll", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressMouseScroll(double mouseX, double mouseY, double scrollDelta, CallbackInfoReturnable<Boolean> cir) {
        if (JeiClientInputHandlerMixinSupport.shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }
}
