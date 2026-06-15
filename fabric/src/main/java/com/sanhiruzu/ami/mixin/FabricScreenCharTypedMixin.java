package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.OverlayInputController;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes typed characters to AMI's overlay (search bar / context menu) on Fabric.
 *
 * <p>fabric-api's {@code ScreenKeyboardEvents} exposes key-press/release events but has
 * NO char-typed event, and vanilla {@code Screen} does not declare {@code charTyped}
 * (it inherits the interface default), so it can't be mixed into directly. The GLFW char
 * callback lives on {@link KeyboardHandler#charTyped}, which is where JEI/EMI also hook
 * char input on Fabric. We intercept at HEAD and, only when AMI is active and consumes the
 * character, cancel so vanilla doesn't also process it. {@code remap = true} (default) so
 * the refmap maps the vanilla target to its intermediary name.
 */
@Mixin(KeyboardHandler.class)
public class FabricScreenCharTypedMixin {

    @Inject(method = "charTyped(JII)V", at = @At("HEAD"), cancellable = true)
    private void amiCharTyped(long windowPointer, int codePoint, int modifiers, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) return;
        if (!InventoryOverlayHandler.isAmiEnabled()) return;
        if (!InventoryOverlayHandler.isAmiScreen(screen)) return;
        if (OverlayInputController.charTyped(screen, InventoryOverlayHandler.getManager(), true, (char) codePoint, modifiers)) {
            ci.cancel();
        }
    }
}
