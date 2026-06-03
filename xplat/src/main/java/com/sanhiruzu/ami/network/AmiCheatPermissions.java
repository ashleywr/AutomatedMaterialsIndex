package com.sanhiruzu.ami.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AmiCheatPermissions {
    private AmiCheatPermissions() {
    }

    public static boolean canUseCheats(ServerPlayer player) {
        if (player == null) return false;
        if (player.getAbilities().instabuild || player.hasPermissions(2)) return true;

        MinecraftServer server = player.getServer();
        return server != null && server.isSingleplayerOwner(player.getGameProfile());
    }
}
