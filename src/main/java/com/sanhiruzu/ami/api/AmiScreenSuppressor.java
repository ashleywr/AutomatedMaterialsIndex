package com.sanhiruzu.ami.api;

import net.minecraft.client.gui.screens.Screen;

/**
 * A predicate that determines whether AMI overlay should be suppressed on a given screen.
 * Mods can register suppressors via {@link AmiApi#registerScreenSuppressor(AmiScreenSuppressor)}.
 */
@FunctionalInterface
public interface AmiScreenSuppressor {
    /**
     * @return true if AMI overlay should be suppressed on this screen, false otherwise
     */
    boolean shouldSuppress(Screen screen);
}
