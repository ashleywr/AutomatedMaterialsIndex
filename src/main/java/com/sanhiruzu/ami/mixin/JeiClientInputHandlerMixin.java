package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import mezz.jei.gui.input.ClientInputHandler;
import mezz.jei.gui.input.UserInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses JEI's overlay input handling (clicks, keys, scroll) when AMI is
 * active. Uses two-tier suppression mirroring
 * {@link com.sanhiruzu.ami.mixin.EmiScreenManagerMixin}:
 * <ul>
 *   <li>Chrome: {@code onInitGui} always suppressed when AMI is active.</li>
 *   <li>Input: mouse/keyboard handling suppressed only when no recipe view is
 *       active AND the JEI RecipesGui screen is not open. JEI's recipe screen
 *       relies on the overlay's {@code FocusInputHandler} (via
 *       {@code ClientInputHandler}) for ingredient click navigation, so
 *       suppression must be fully lifted when RecipesGui is the active screen.</li>
 * </ul>
 */
@Pseudo
@Mixin(ClientInputHandler.class)
public class JeiClientInputHandlerMixin {

    // ── Chrome suppression ────────────────────────────────────────────

    @Inject(method = "onInitGui", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressOnInitGui(CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    // ── Input suppression (lifted during recipe view) ──────────────────

    @Inject(method = "onKeyboardKeyPressedPre", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressKeyPressedPre(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onKeyboardKeyPressedPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressKeyPressedPost(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onKeyboardCharTypedPre", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressCharTypedPre(Screen screen, char codePoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onKeyboardCharTypedPost", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressCharTypedPost(Screen screen, char codePoint, int modifiers, CallbackInfo ci) {
        if (shouldSuppressJeiInput()) {
            ci.cancel();
        }
    }

    @Inject(method = "onGuiMouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressMouseClicked(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onGuiMouseReleased", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressMouseReleased(Screen screen, UserInput input, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onGuiMouseScroll", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressMouseScroll(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiInput()) {
            cir.setReturnValue(false);
        }
    }

    // ── Gates ──────────────────────────────────────────────────────────

    private static boolean shouldSuppressJeiChrome() {
        return InventoryOverlayHandler.isAmiEnabled();
    }

    private static boolean shouldSuppressJeiInput() {
        if (!InventoryOverlayHandler.isAmiEnabled()) return false;
        if (RecipeViewerBridge.isRecipeViewActive()) return false;
        if (isJeiRecipeScreenActive()) return false;
        return true;
    }

    /** JEI's RecipesGui uses the overlay's FocusInputHandler for ingredient click navigation. */
    private static boolean isJeiRecipeScreenActive() {
        Screen screen = Minecraft.getInstance().screen;
        return screen != null && screen.getClass().getName().equals("mezz.jei.gui.recipes.RecipesGui");
    }
}
