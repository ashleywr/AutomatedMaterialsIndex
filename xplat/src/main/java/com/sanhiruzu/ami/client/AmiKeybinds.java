package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.KeyMapping;

/**
 * Thin xplat wrapper for keybind matching. Delegates to
 * {@link com.sanhiruzu.ami.platform.IPlatformHelper#keyActiveAndMatches} so that
 * the platform seam hides the Forge/NeoForge-patched
 * {@code KeyMapping.isActiveAndMatches(InputConstants.Key)} call from xplat code.
 */
public final class AmiKeybinds {
    private AmiKeybinds() {
    }

    /**
     * Returns true if {@code mapping} is active and matches {@code key}.
     */
    public static boolean activeAndMatches(KeyMapping mapping, InputConstants.Key key) {
        return Services.PLATFORM.keyActiveAndMatches(mapping, key);
    }

    /**
     * Returns true if {@code mapping} is active and matches {@code key}, including any
     * platform-specific modifier handling attached to the originating key event.
     */
    public static boolean activeAndMatches(KeyMapping mapping, InputConstants.Key key, int modifiers) {
        return Services.PLATFORM.keyActiveAndMatches(mapping, key, modifiers);
    }
}
