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

@Mixin(value = RecipeBookComponent.class, priority = 500, remap = false)
public abstract class RecipeBookComponentMixin {
    @Shadow(remap = false)
    public abstract boolean isVisible();

    @Shadow(remap = false)
    protected abstract void setVisible(boolean visible);

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void forceVanillaRecipeBookClosed(int width, int height, Minecraft minecraft, boolean widthTooNarrow,
                                              RecipeBookMenu menu, CallbackInfo ci) {
        if (shouldToggleAmi() && isVisible()) {
            setVisible(false);
        }
    }

    @Inject(method = "toggleVisibility", at = @At("HEAD"), cancellable = true, remap = false)
    private void toggleAmiInsteadOfVanillaRecipeBook(CallbackInfo ci) {
        if (!shouldToggleAmi()) return;

        InventoryOverlayHandler.toggleFromRecipeBookButton();
        ci.cancel();
    }

    private static boolean shouldToggleAmi() {
        if (!AmiConfig.enableAutoIndexing) return false;

        Screen screen = Minecraft.getInstance().screen;
        return screen instanceof AbstractContainerScreen<?> && screen instanceof RecipeUpdateListener;
    }
}
