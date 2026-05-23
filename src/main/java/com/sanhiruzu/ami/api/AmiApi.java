package com.sanhiruzu.ami.api;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Public API for interacting with AMI from other mods.
 * Currently provides screen suppression for mods that want to hide AMI's overlay.
 */
public class AmiApi {
    private static final List<AmiScreenSuppressor> SUPPRESSORS = new ArrayList<>();

    /**
     * Register a screen suppressor. When the suppressor's predicate returns true,
     * AMI will hide its overlay and ignore input for that screen.
     *
     * @param suppressor the predicate that determines if AMI should be suppressed
     */
    public static void registerScreenSuppressor(AmiScreenSuppressor suppressor) {
        SUPPRESSORS.add(suppressor);
    }

    /**
     * Check if AMI should be suppressed on the given screen.
     * Called by AMI's render system; not typically called by other mods.
     *
     * @return true if any registered suppressor returns true
     */
    public static boolean shouldSuppressAmi(Screen screen) {
        for (AmiScreenSuppressor suppressor : SUPPRESSORS) {
            if (suppressor.shouldSuppress(screen)) {
                return true;
            }
        }
        return false;
    }
}
