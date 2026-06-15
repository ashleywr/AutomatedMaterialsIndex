package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric variant of {@code ContainerTooltipMixin}. Identical behavior, but targets the
 * <em>vanilla</em> {@link AbstractContainerScreen} with {@code remap = true} (the default)
 * so the refmap translates the Mojang-named target methods (isHovering/renderTooltip) to
 * their intermediary names at runtime. The shared xplat mixin uses {@code remap = false},
 * which is correct on NeoForge/Forge but fails on Fabric's intermediary-named runtime, so
 * the xplat copy is left out of the Fabric mixin config in favor of this one.
 */
@Mixin(AbstractContainerScreen.class)
public class FabricContainerTooltipMixin {

    @Inject(method = "isHovering(IIIIDD)Z", at = @At("HEAD"), cancellable = true)
    private void suppressSlotHoverBehindAmi(int x, int y, int width, int height,
                                            double mouseX, double mouseY,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (InventoryOverlayHandler.isMouseOverAmiOverlay(mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true)
    private void suppressSlotTooltipBehindAmi(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        if (InventoryOverlayHandler.isMouseOverAmiOverlay(x, y)) {
            ci.cancel();
        }
    }
}
