package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import mezz.jei.gui.events.GuiEventHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses JEI's overlay rendering (ingredient list, bookmarks, tooltips)
 * when AMI is active. Chrome suppression — always on when AMI is active,
 * even during recipe view. The JEI recipe screen is a standalone {@link Screen}
 * and renders itself independently.
 */
@Pseudo
@Mixin(GuiEventHandler.class)
public class JeiGuiEventHandlerMixin {

    @Inject(method = "onGuiInit", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressOnGuiInit(Screen screen, CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "onGuiOpen", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressOnGuiOpen(Screen screen, CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "onDrawForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressOnDrawForeground(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "onDrawScreenPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressOnDrawScreenPost(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCompactPotionIndicators", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressCompactPotionIndicators(CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiChrome()) {
            cir.setReturnValue(false);
        }
    }

    private static boolean shouldSuppressJeiChrome() {
        return InventoryOverlayHandler.shouldSuppressRecipeViewerChrome();
    }
}
