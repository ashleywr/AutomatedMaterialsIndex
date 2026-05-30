package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;

public final class RecipeViewerStateSync {
    private RecipeViewerStateSync() {
    }

    public static void favoritesChanged() {
        AmiFavoritesHandler.getInstance().externalFavoritesChanged();
    }

    public static void sidebarsChanged() {
        AmiFavoritesHandler.getInstance().externalFavoritesChanged();
    }

    public static void visibilityChanged() {
        AmiFavoritesHandler.getInstance().externalFavoritesChanged();
    }

    public static void recipesChanged() {
        AmiFavoritesHandler.getInstance().externalFavoritesChanged();
    }
}
