package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks EMI's chrome (item list, search bar, config/recipe-tree buttons) when AMI
 * is active, so EMI cannot draw or interact inside the inventory screen. The
 * suppression lifts for input when a recipe view is triggered through AMI, allowing the
 * player to interact with the recipe, then re-engages when the recipe is
 * dismissed.
 * <p>
 * Two-tier suppression:
 * - Chrome (render, widgets, backgrounds): always suppressed when AMI is enabled,
 * even during recipe view. RecipeScreen renders its own content before calling
 * EmiScreenManager.render, so recipe content is unaffected.
 * - Input (mouse, keyboard): suppressed only when no recipe view is active,
 * so the player can interact with recipe tabs, widgets, and pages.
 */
@Mixin(EmiScreenManager.class)
public class EmiScreenManagerMixin {

    // Chrome suppression (always on when AMI active)

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hideEmiWhenAmiActive(EmiDrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (shouldSuppressEmiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hideEmiBackgroundWhenAmiActive(EmiDrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (shouldSuppressEmiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hideEmiForegroundWhenAmiActive(EmiDrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (shouldSuppressEmiChrome()) {
            ci.cancel();
        }
    }

    // EMI auto-focuses its search bar inside addWidgets on every screen reinit.
    // That steals key events before our ScreenEvent.KeyPressed.Pre handlers fire.
    @Inject(method = "addWidgets", at = @At("HEAD"), cancellable = true, remap = false)
    private static void suppressWidgetsWhenAmiActive(Screen screen, CallbackInfo ci) {
        if (shouldSuppressEmiChrome()) {
            ci.cancel();
        }
    }

    // Input suppression (lifted during recipe view)

    @Inject(method = "mouseClicked", at = @At("HEAD"), remap = false, cancellable = true)
    private static void suppressMouseClickedWhenAmiActive(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressEmiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), remap = false, cancellable = true)
    private static void suppressMouseReleasedWhenAmiActive(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressEmiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), remap = false, cancellable = true)
    private static void suppressMouseDraggedWhenAmiActive(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressEmiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), remap = false, cancellable = true)
    private static void suppressMouseScrolledWhenAmiActive(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressEmiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseMoved", at = @At("HEAD"), remap = false, cancellable = true)
    private static void suppressMouseMovedWhenAmiActive(double mouseX, double mouseY, CallbackInfo ci) {
        if (shouldSuppressEmiInput()) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), remap = false, cancellable = true)
    private static void suppressKeyPressedWhenAmiActive(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressEmiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), remap = false, cancellable = true)
    private static void suppressCharTypedWhenAmiActive(char codePoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressEmiInput()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false, cancellable = true)
    private static void suppressTickWhenAmiActive(Screen screen, CallbackInfo ci) {
        if (shouldSuppressEmiChrome()) {
            ci.cancel();
        }
    }

    // Gates

    /**
     * Always suppress EMI chrome rendering when AMI is active, even during recipe view.
     */
    private static boolean shouldSuppressEmiChrome() {
        return InventoryOverlayHandler.shouldSuppressRecipeViewerChrome();
    }

    /**
     * Suppress EMI input only when no recipe view is active — lifted so the player can interact with recipe tabs and widgets.
     */
    private static boolean shouldSuppressEmiInput() {
        if (!InventoryOverlayHandler.isAmiEnabled()) return false;
        if (RecipeViewerBridge.isRecipeViewActive()) return false;
        if (isEmiRecipeScreenActive()) return false;
        return true;
    }

    private static boolean isEmiRecipeScreenActive() {
        Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        return screen != null && screen.getClass().getName().equals("dev.emi.emi.screen.RecipeScreen");
    }
}
