package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targeted by class name string rather than a typed {@code @Mixin(IngredientListOverlay.class)}
 * reference: JEI has changed this class's draw method set across versions (19.27.x calls a single
 * {@code drawScreen}; 19.43.x splits it into {@code drawBackground}/{@code drawForeground} instead
 * and only keeps {@code drawScreen} as unused dead API) and a typed reference would fail to compile
 * against whichever JEI version this module's compile-time dependency pins. Each injection is
 * independently optional ({@code require = 0}) so a method missing on a given JEI version doesn't
 * break the others.
 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.IngredientListOverlay")
public class JeiIngredientListOverlayMixin {

    private static boolean shouldSuppressJeiRendering() {
        return RecipeViewerBridge.isJeiLoaded()
                && InventoryOverlayHandler.shouldSuppressRecipeViewerChrome();
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressDrawScreen(Minecraft minecraft, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (shouldSuppressJeiRendering()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressDrawBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (shouldSuppressJeiRendering()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressDrawForeground(Minecraft minecraft, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (shouldSuppressJeiRendering()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressDrawTooltips(Minecraft minecraft, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiRendering()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawOnForeground", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressDrawOnForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiRendering()) {
            ci.cancel();
        }
    }
}
