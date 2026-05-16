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

    private static boolean shouldHideEmi() {
        return InventoryOverlayHandler.isAmiEnabled()
                && AMIConfig.SUPPRESS_RECIPE_VIEWERS.get();
    }
}
