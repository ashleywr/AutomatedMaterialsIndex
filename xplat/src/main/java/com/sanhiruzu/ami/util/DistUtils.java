package com.sanhiruzu.ami.util;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.world.level.Level;

public final class DistUtils {
    private DistUtils() {
    }

    /**
     * Safely attempts to get the current client level if running on a client.
     * Returns null on dedicated server.
     */
    public static Level getClientLevel() {
        if (Services.PLATFORM.isClient()) {
            return ClientLevelGetter.getLevel();
        }
        return null;
    }

    private static class ClientLevelGetter {
        private static Level getLevel() {
            return net.minecraft.client.Minecraft.getInstance().level;
        }
    }
}
