package com.sanhiruzu.ami.util;

import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLLoader;

public class DistUtils {
    /**
     * Safely attempts to get the current client level if running on a client.
     * Returns null on dedicated server.
     */
    public static Level getClientLevel() {
        if (FMLLoader.getDist().isClient()) {
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
