package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import mezz.jei.gui.overlay.IngredientListOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(IngredientListOverlay.class)
public class JeiIngredientListOverlayMixin {

    private static boolean shouldSuppressJeiRendering() {
        return RecipeViewerBridge.isJeiLoaded()
                && InventoryOverlayHandler.shouldSuppressRecipeViewerChrome();
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressDrawScreen(Minecraft minecraft, GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (shouldSuppressJeiRendering()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressDrawTooltips(Minecraft minecraft, GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiRendering()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawOnForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressDrawOnForeground(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiRendering()) {
            ci.cancel();
        }
    }
}
