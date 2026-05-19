package com.sanhiruzu.ami.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkOverlay")
public class JeiBookmarkOverlayMixin {
    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private void hideDrawScreen(Minecraft minecraft, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (RecipeViewerMixinSupport.shouldHideRecipeViewer()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void hideTooltips(Minecraft minecraft, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (RecipeViewerMixinSupport.shouldHideRecipeViewer()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawOnForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void hideForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (RecipeViewerMixinSupport.shouldHideRecipeViewer()) {
            ci.cancel();
        }
    }

    @Inject(method = "isListDisplayed", at = @At("HEAD"), cancellable = true, remap = false)
    private void hideList(CallbackInfoReturnable<Boolean> cir) {
        if (RecipeViewerMixinSupport.shouldHideRecipeViewer()) {
            cir.setReturnValue(false);
        }
    }
}
