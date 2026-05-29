package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class JeiClientInputHandlerMixinSupport {
    private JeiClientInputHandlerMixinSupport() {
    }

    public static boolean shouldSuppressJeiChrome() {
        return InventoryOverlayHandler.shouldSuppressRecipeViewerChrome();
    }

    public static boolean shouldSuppressJeiInput() {
        if (!InventoryOverlayHandler.isAmiEnabled()) return false;
        if (RecipeViewerBridge.isRecipeViewActive()) return false;
        if (isJeiRecipeScreenActive()) return false;
        return true;
    }

    /**
     * JEI's RecipesGui uses the overlay's FocusInputHandler for ingredient click navigation.
     */
    private static boolean isJeiRecipeScreenActive() {
        Screen screen = Minecraft.getInstance().screen;
        return screen != null && screen.getClass().getName().equals("mezz.jei.gui.recipes.RecipesGui");
    }
}
