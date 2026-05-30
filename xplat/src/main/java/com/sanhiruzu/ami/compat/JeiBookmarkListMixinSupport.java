package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;

public final class JeiBookmarkListMixinSupport {
    private JeiBookmarkListMixinSupport() {
    }

    public static void bookmarksChanged() {
        RecipeViewerStateSync.favoritesChanged();
    }
}
