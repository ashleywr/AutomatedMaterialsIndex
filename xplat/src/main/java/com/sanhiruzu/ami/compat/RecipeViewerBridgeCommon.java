package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.RecipeViewerScreen;
import com.sanhiruzu.ami.client.favorites.AmiHistoryHandler;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

final class RecipeViewerBridgeCommon {
    private RecipeViewerBridgeCommon() {
    }

    static boolean shouldUseNativeViewer(boolean externalViewerAvailable) {
        return switch (AmiConfig.recipeViewerMode) {
            case NATIVE -> true;
            case EMI_JEI -> false;
            case AUTO -> !externalViewerAvailable;
        };
    }

    static void openNative(ItemStack stack, boolean showRecipes) {
        if (stack == null || stack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        mc.setScreen(new RecipeViewerScreen(stack.copy(), parent, showRecipes));
    }

    static void recordLookup(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        AmiHistoryHandler.getInstance().recordLookup(stack);
    }
}
