package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.config.AmiConfig;

final class RecipeViewerMixinSupport {
    private RecipeViewerMixinSupport() {
    }

    static boolean shouldHideRecipeViewer() {
        return InventoryOverlayHandler.isAmiEnabled()
                && AmiConfig.suppressRecipeViewers;
    }
}
