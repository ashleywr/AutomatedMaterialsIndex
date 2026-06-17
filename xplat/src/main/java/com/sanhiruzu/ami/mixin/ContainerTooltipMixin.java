package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
