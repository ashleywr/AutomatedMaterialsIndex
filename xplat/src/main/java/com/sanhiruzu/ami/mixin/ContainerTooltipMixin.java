package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class ContainerTooltipMixin {
    @Inject(method = "isHovering(IIIIDD)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressSlotHoverBehindAmi(int x, int y, int width, int height,
                                            double mouseX, double mouseY,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (InventoryOverlayHandler.isMouseOverAmiOverlay(mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressSlotTooltipBehindAmi(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        if (InventoryOverlayHandler.isMouseOverAmiOverlay(x, y)) {
            ci.cancel();
        }
    }
}
