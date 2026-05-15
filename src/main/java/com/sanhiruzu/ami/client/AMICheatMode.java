package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class AMICheatMode {
    private AMICheatMode() {}

    /**
     * Config toggle is on AND the player is allowed to perform elevated actions.
     * Always true in singleplayer / as LAN host. On a dedicated server requires OP (level 2).
     */
    public static boolean isEnabled() {
        return AMIConfig.CHEAT_MODE.get() && isAllowed();
    }

    /**
     * Returns whether the current player has the authority to run cheat actions,
     * regardless of whether the config toggle is on.
     */
    public static boolean isAllowed() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        if (mc.hasSingleplayerServer()) return true;           // singleplayer or LAN host
        return mc.player.hasPermissions(2);                    // OP on dedicated server
    }

    /** Give one of an item to the local player via /give. */
    public static void giveItem(ResourceLocation itemId) {
        sendCommand("give @s " + itemId);
    }

    /** Locate the nearest biome via /locate biome. */
    public static void locateBiome(ResourceLocation biomeId) {
        sendCommand("locate biome " + biomeId);
    }

    /** Locate the nearest structure via /locate structure. */
    public static void locateStructure(ResourceLocation structureId) {
        sendCommand("locate structure " + structureId);
    }

    private static void sendCommand(String command) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AMI.LOGGER.debug("AMI cheat: /{}", command);
        mc.player.connection.sendCommand(command);
    }
}
