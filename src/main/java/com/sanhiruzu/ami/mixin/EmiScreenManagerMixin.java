package com.sanhiruzu.ami.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.sanhiruzu.ami.AMIConfig;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;

@Mixin(EmiScreenManager.class)
public class EmiScreenManagerMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private static void onRender(EmiDrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (AMIConfig.SUPPRESS_RECIPE_VIEWERS.get()) {
			ci.cancel();
		}
	}
}
