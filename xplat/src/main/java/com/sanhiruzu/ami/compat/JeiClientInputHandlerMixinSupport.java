package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class JeiClientInputHandlerMixinSupport {
    private JeiClientInputHandlerMixinSupport() {
    }

    public static boolean shouldSuppressJeiChrome() {
        if (!RecipeViewerBridge.isJeiLoaded()) return false;
        return InventoryOverlayHandler.shouldSuppressRecipeViewerChrome();
    }

    /**
     * Whether to cancel JEI's per-screen init hooks ({@code ClientInputHandler.onInitGui},
     * {@code GuiEventHandler.onGuiInit}/{@code onGuiOpen}).
     *
     * <p>Deliberately more permissive than {@link #shouldSuppressJeiChrome()}: on JEI's own
     * RecipesGui, those hooks are what wire up that screen's input handling, so cancelling them
     * leaves the recipe view rendered but completely unclickable. AMI keeps its panel visible
     * over the recipe screen (only the toggle keybind / recipe-book icon hide it), so chrome
     * suppression is still true there — suppressing the drawing is fine, suppressing the init is
     * not.
     */
    public static boolean shouldSuppressJeiScreenInit() {
        return shouldSuppressJeiChrome() && !isJeiRecipeScreenActive();
    }

    public static boolean shouldSuppressJeiInput() {
        if (!RecipeViewerBridge.isJeiLoaded()) return false;
        if (!InventoryOverlayHandler.shouldSuppressRecipeViewerChrome()) return false;
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
