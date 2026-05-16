package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EmiScreenManager.class)
public class EmiScreenManagerMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hideEmiWhenAmiActive(EmiDrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (shouldHideEmi()) {
            ci.cancel();
        }
    }

    // EMI calls EmiPort.focus(search, false) inside addWidgets, auto-focusing its search bar
    // every time the screen is reinit'd. That focused widget then steals all key events through
    // Minecraft's normal widget routing before our ScreenEvent.KeyPressed.Pre handlers fire.
    @Inject(method = "addWidgets", at = @At("HEAD"), cancellable = true, remap = false)
    private static void suppressWidgetsWhenAmiActive(Screen screen, CallbackInfo ci) {
        if (shouldHideEmi()) {
            ci.cancel();
        }
    }

    // EMI hooks mouse and keyboard events via its own Mixin on Mouse/Keyboard — it does not use
    // NeoForge's ScreenEvent system, so our event handlers cannot cancel EMI's input processing.
    // We intercept the EmiScreenManager static methods directly so clicks/scrolls in EMI's panel
    // area are not consumed while AMI is showing.

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private static void suppressMouseClickedWhenAmiActive(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (shouldHideEmi()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, remap = false)
    private static void suppressMouseReleasedWhenAmiActive(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (shouldHideEmi()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, remap = false)
    private static void suppressMouseScrolledWhenAmiActive(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (shouldHideEmi()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private static void suppressKeyPressedWhenAmiActive(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (shouldHideEmi()) {
            cir.setReturnValue(false);
        }
    }

    private static boolean shouldHideEmi() {
        return InventoryOverlayHandler.isAmiEnabled()
                && AMIConfig.SUPPRESS_RECIPE_VIEWERS.get();
    }
}
