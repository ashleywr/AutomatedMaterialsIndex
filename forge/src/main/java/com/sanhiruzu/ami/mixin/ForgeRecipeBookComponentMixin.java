package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Forge production jars run this 1.20.1 class with SRG method names and no AMI refmap.
@Mixin(value = RecipeBookComponent.class, priority = 500, remap = false)
public abstract class ForgeRecipeBookComponentMixin {
    @Shadow(remap = false, aliases = "m_100385_")
    public abstract boolean isVisible();

    @Shadow(remap = false, aliases = "m_100369_")
    protected abstract void setVisible(boolean visible);

    @Inject(method = {"init", "m_100309_"}, at = @At("RETURN"), remap = false, require = 1)
    private void forceVanillaRecipeBookClosed(int width, int height, Minecraft minecraft, boolean widthTooNarrow,
                                              RecipeBookMenu menu, CallbackInfo ci) {
        if (shouldToggleAmi() && isVisible()) {
            setVisible(false);
        }
    }

    @Inject(method = {"toggleVisibility", "m_100384_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void toggleAmiInsteadOfVanillaRecipeBook(CallbackInfo ci) {
        if (!shouldToggleAmi()) return;

        InventoryOverlayHandler.toggleAmi();
        ci.cancel();
    }

    private static boolean shouldToggleAmi() {
        if (!AmiConfig.enableAutoIndexing) return false;
        if (ModList.get().isLoaded("nerb")) return false;

        Screen screen = Minecraft.getInstance().screen;
        return screen instanceof AbstractContainerScreen<?> && screen instanceof RecipeUpdateListener;
    }
}
