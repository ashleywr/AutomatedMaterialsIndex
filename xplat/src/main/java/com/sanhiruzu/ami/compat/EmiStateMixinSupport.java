package com.sanhiruzu.ami.compat;

public final class EmiStateMixinSupport {
    private EmiStateMixinSupport() {
    }

    public static void favoritesChanged() {
        RecipeViewerStateSync.favoritesChanged();
    }

    public static void sidebarsChanged() {
        RecipeViewerStateSync.sidebarsChanged();
    }

    public static void visibilityChanged() {
        RecipeViewerStateSync.visibilityChanged();
    }

    public static void recipesChanged() {
        RecipeViewerStateSync.recipesChanged();
    }
}
