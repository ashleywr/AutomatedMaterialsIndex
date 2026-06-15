package com.sanhiruzu.ami.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.client.AmiKeybinds;
import com.sanhiruzu.ami.platform.Services;
import dev.emi.emi.config.EmiConfig;

public final class EmiRecipeScreenFavoriteMixinSupport {
    private EmiRecipeScreenFavoriteMixinSupport() {
    }

    public static boolean handleFavoriteKey(Object screen, int keyCode, int scanCode) {
        boolean amiFavoriteKey = AmiKeybinds.activeAndMatches(
                Services.PLATFORM.keyMappings().favorite(),
                InputConstants.getKey(keyCode, scanCode));
        boolean emiFavoriteKey = EmiConfig.favorite.matchesKey(keyCode, scanCode);
        return (amiFavoriteKey || emiFavoriteKey) && EmiFavoritesBridge.toggleRecipeScreenHoveredFavorite(screen);
    }
}
