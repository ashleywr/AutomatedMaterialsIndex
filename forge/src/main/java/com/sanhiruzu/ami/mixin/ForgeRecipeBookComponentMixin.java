package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Forge production jars run this 1.20.1 class with SRG method names and no AMI refmap.
@Mixin(value = RecipeBookComponent.class, priority = 500, remap = false)
public abstract class ForgeRecipeBookComponentMixin {
    @Shadow
    public abstract boolean m_100385_();

    @Shadow
    protected abstract void m_100369_(boolean visible);

    @Inject(method = "m_100309_", at = @At("RETURN"))
    private void forceVanillaRecipeBookClosed(int width, int height, Minecraft minecraft, boolean widthTooNarrow,
                                              RecipeBookMenu menu, CallbackInfo ci) {
        if (shouldToggleAmi() && m_100385_()) {
            m_100369_(false);
        }
    }

    @Inject(method = "m_100384_", at = @At("HEAD"), cancellable = true)
    private void toggleAmiInsteadOfVanillaRecipeBook(CallbackInfo ci) {
        if (!shouldToggleAmi()) return;

        InventoryOverlayHandler.toggleAmi();
        ci.cancel();
    }

    private static boolean shouldToggleAmi() {
        if (!AmiConfig.enableAutoIndexing) return false;

        Screen screen = Minecraft.getInstance().screen;
        return screen instanceof AbstractContainerScreen<?> && screen instanceof RecipeUpdateListener;
    }
}
