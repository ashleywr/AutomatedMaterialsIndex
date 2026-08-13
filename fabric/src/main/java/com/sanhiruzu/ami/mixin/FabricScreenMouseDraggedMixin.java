package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric API's {@code ScreenMouseEvents} covers click/release/scroll but has no mouse-drag hook,
 * so AMI never saw continued mouse movement while a button was held on Fabric — a drag-driven
 * widget (e.g. the result panel's scrollbar thumb) would register the initial click but never
 * update as the mouse moved. {@code Screen} never overrides {@code mouseDragged} itself; it
 * inherits {@link ContainerEventHandler}'s default implementation (declared in the interface, not
 * synthesized onto {@code Screen}'s own class file, so a {@code @Mixin(Screen.class)} injection
 * cannot locate it there), which only forwards to whichever child previously claimed vanilla
 * focus via {@code setFocused}/{@code setDragging} — AMI's overlay widgets are not part of that
 * vanilla focus chain. Targeting the interface directly instead, and gating on {@code this} being
 * the live screen, mirrors NeoForge/Forge's {@code ScreenEvent.MouseDragged.Pre}, which fires
 * independently of vanilla focus.
 */
@Mixin(ContainerEventHandler.class)
public interface FabricScreenMouseDraggedMixin {
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void ami$onMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Screen screen) || screen != Minecraft.getInstance().screen) return;
        if (InventoryOverlayHandler.onScreenMouseDragged(screen, mouseX, mouseY, button, dragX, dragY)) {
            cir.setReturnValue(true);
        }
    }
}
