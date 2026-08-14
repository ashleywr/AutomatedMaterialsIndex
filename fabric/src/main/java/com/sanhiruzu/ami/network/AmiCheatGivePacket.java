package com.sanhiruzu.ami.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record AmiCheatGivePacket(ItemStack stack) implements CustomPacketPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger("AMI");

    public static final Type<AmiCheatGivePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ami", "cheat_give"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AmiCheatGivePacket> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.map(AmiCheatGivePacket::new, AmiCheatGivePacket::stack);

    /**
     * Server-side handler. Called on the server thread via Fabric's
     * ServerPlayNetworking.PlayPayloadHandler.
     */
    public void handleOnServer(ServerPlayer serverPlayer) {
        if (!AmiCheatPermissions.canUseCheats(serverPlayer)) {
            LOGGER.warn("AMI cheat: {} attempted give/delete without permission",
                    serverPlayer.getName().getString());
            return;
        }
        ItemStack toSet = stack().isEmpty() ? ItemStack.EMPTY : stack().copy();
        if (toSet.isEmpty()) {
            LOGGER.debug("AMI cheat delete cursor: {}", serverPlayer.getName().getString());
            serverPlayer.containerMenu.setCarried(ItemStack.EMPTY);
            serverPlayer.containerMenu.broadcastChanges();
            return;
        }
        LOGGER.debug("AMI cheat give: {} -> {} x{}",
                serverPlayer.getName().getString(),
                toSet.getItem().getDescriptionId(),
                toSet.getCount());
        // Cursor first, then inventory, then drop at the player's feet as a last resort —
        // never silently overwrite/discard whatever the player is already carrying.
        if (serverPlayer.containerMenu.getCarried().isEmpty()) {
            serverPlayer.containerMenu.setCarried(toSet);
        } else if (!serverPlayer.getInventory().add(toSet)) {
            serverPlayer.drop(toSet, false);
        }
        serverPlayer.containerMenu.broadcastChanges();
    }

    @Override
    public Type<AmiCheatGivePacket> type() {
        return TYPE;
    }
}
