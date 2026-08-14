package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.compat.JeiClientInputHandlerMixinSupport;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses JEI's overlay rendering (ingredient list, bookmarks, tooltips)
 * when AMI is active. Chrome suppression — always on when AMI is active,
 * even during recipe view. The JEI recipe screen is a standalone {@link Screen}
 * and renders itself independently.
 *
 * <p>Targeted by class name string rather than a typed {@code @Mixin(GuiEventHandler.class)}
 * reference, and every injection is optional ({@code require = 0}), because JEI renamed this
 * class's draw entry points across versions (19.27.x's {@code onDrawForeground}/
 * {@code onDrawScreenPost} became 19.43.x's {@code drawForContainerScreen}/{@code drawForScreen}).
 * Both old and new names are injected so this keeps working across that rename; a typed reference
 * would fail to compile against whichever JEI version this module's compile-time dependency pins.
 * This mixin is a secondary layer — {@link JeiIngredientListOverlayMixin} and
 * {@link JeiBookmarkOverlayMixin} suppress the same content at the overlay classes themselves,
 * which is the more version-resilient point since it doesn't depend on which {@code GuiEventHandler}
 * method happens to call into them.
 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.events.GuiEventHandler")
public class JeiGuiEventHandlerMixin {

    private static boolean shouldSuppressJeiChrome() {
        return RecipeViewerBridge.isJeiLoaded()
                && InventoryOverlayHandler.shouldSuppressRecipeViewerChrome();
    }

    // Screen-init hooks are gated separately: cancelling them on JEI's own RecipesGui leaves that
    // screen rendered but unclickable, since this is where JEI wires up its per-screen handling.
    // See JeiClientInputHandlerMixinSupport.shouldSuppressJeiScreenInit().
    private static boolean shouldSuppressJeiScreenInit() {
        return JeiClientInputHandlerMixinSupport.shouldSuppressJeiScreenInit();
    }

    @Inject(method = "onGuiInit", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressOnGuiInit(Screen screen, CallbackInfo ci) {
        if (shouldSuppressJeiScreenInit()) {
            ci.cancel();
        }
    }

    @Inject(method = "onGuiOpen", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressOnGuiOpen(Screen screen, CallbackInfo ci) {
        if (shouldSuppressJeiScreenInit()) {
            ci.cancel();
        }
    }

    @Inject(method = "onDrawForeground", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressOnDrawForeground(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "onDrawScreenPost", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressOnDrawScreenPost(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawForContainerScreen", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressDrawForContainerScreen(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawForScreen", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressDrawForScreen(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (shouldSuppressJeiChrome()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCompactPotionIndicators", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void suppressCompactPotionIndicators(CallbackInfoReturnable<Boolean> cir) {
        if (shouldSuppressJeiChrome()) {
            cir.setReturnValue(false);
        }
    }
}
