package com.sanhiruzu.ami.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.sanhiruzu.ami.AMIConfig;
import dev.emi.emi.screen.EmiScreenManager;

@Mixin(EmiScreenManager.class)
public class EmiButtonMixin {

	/**
	 * Suppress EMI's button click handling when AMI is suppressing recipe viewers.
	 * This prevents EMI's config button from intercepting clicks on our overlay.
	 */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
	private static void suppressEmiMouseClick(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
		if (AMIConfig.SUPPRESS_RECIPE_VIEWERS.get()) {
			// Don't process EMI's click handling; let other handlers (AMI) take it
			cir.setReturnValue(false);
		}
	}
}
