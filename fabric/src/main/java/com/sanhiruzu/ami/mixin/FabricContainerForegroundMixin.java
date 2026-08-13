package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric equivalent of NeoForge/Forge's {@code ContainerScreenEvent.Render.Foreground}: fires
 * right before the container screen draws its tooltip (vanilla, JEI, or a third-party tooltip
 * mod), after slots/labels/widgets are already drawn. {@code AbstractContainerScreen} itself
 * never calls {@code renderTooltip}; every concrete subclass (InventoryScreen, generic container
 * screens, etc.) calls it exactly once near the end of its own {@code render()} override, which
 * makes this method the one reliable choke point across all vanilla container screens without
 * hooking every subclass individually. By the time this fires, the container's own
 * leftPos/topPos pose translation (pushed and popped inside {@code AbstractContainerScreen.render})
 * has already been undone by vanilla itself, so the injected hook already runs in plain
 * screen-space coordinates — unlike NeoForge's Foreground event, which fires while that
 * translation is still active and must undo it manually.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class FabricContainerForegroundMixin {
    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void ami$onContainerForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryOverlayHandler.onContainerForeground(
                (AbstractContainerScreen<?>) (Object) this, guiGraphics, mouseX, mouseY);
    }
}
