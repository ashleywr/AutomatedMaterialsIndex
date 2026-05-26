package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import mezz.jei.api.registration.IRuntimeRegistration;
import mezz.jei.forge.plugins.forge.ForgeGuiPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ForgeGuiPlugin.class)
public class ForgeGuiPluginMixin {

    @Inject(method = "registerRuntime", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressRegisterRuntime(IRuntimeRegistration registration, CallbackInfo ci) {
        if (shouldSuppressJeiGui()) {
            ci.cancel();
        }
    }

    private static boolean shouldSuppressJeiGui() {
        if (!InventoryOverlayHandler.isAmiEnabled()) return false;
        var manager = InventoryOverlayHandler.getManager();
        return manager != null && manager.isPanelVisible();
    }
}
