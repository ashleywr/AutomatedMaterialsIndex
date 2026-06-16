package com.sanhiruzu.ami.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public final class AmiCheatPermissions {
    private AmiCheatPermissions() {
    }

    public static boolean canUseCheats(ServerPlayer player) {
        if (player == null) return false;
        if (player.getAbilities().instabuild) return true;

        MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        NameAndId nameAndId = new NameAndId(player.getGameProfile());
        return server.getPlayerList().isOp(nameAndId) || server.isSingleplayerOwner(nameAndId);
    }
}
