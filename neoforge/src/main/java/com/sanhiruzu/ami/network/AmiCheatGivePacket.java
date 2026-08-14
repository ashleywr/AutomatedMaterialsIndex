package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.neoforge.AMI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AmiCheatGivePacket(ItemStack stack, boolean cursorEmpty) implements CustomPacketPayload {
    public static final Type<AmiCheatGivePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AMI.MODID, "cheat_give"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AmiCheatGivePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.stack());
                buf.writeBoolean(packet.cursorEmpty());
            },
            buf -> new AmiCheatGivePacket(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readBoolean()));

    public static void handle(AmiCheatGivePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!AmiCheatPermissions.canUseCheats(serverPlayer)) {
                AMI.LOGGER.warn("AMI cheat: {} attempted give/delete without permission",
                        player.getName().getString());
                return;
            }
            ItemStack toSet = packet.stack().isEmpty() ? ItemStack.EMPTY : packet.stack().copy();
            if (toSet.isEmpty()) {
                AMI.LOGGER.debug("AMI cheat delete cursor: {}", player.getName().getString());
                serverPlayer.containerMenu.setCarried(ItemStack.EMPTY);
                serverPlayer.containerMenu.broadcastChanges();
                return;
            }
            AMI.LOGGER.debug("AMI cheat give: {} -> {} x{}",
                    player.getName().getString(),
                    toSet.getItem().getDescriptionId(),
                    toSet.getCount());
            // Cursor first, then inventory, then drop at the player's feet as a last resort — never
            // silently overwrite/discard whatever the player is already carrying. Whether the cursor
            // is free is decided by the client (cursorEmpty), not re-derived from
            // containerMenu.getCarried() here: creative-mode slot interactions largely bypass the
            // normal click protocol the server tracks that field through, so it can't be trusted as
            // a live "what's on the cursor right now" signal.
            if (packet.cursorEmpty()) {
                serverPlayer.containerMenu.setCarried(toSet);
            } else if (!serverPlayer.getInventory().add(toSet)) {
                serverPlayer.drop(toSet, false);
            }
            serverPlayer.containerMenu.broadcastChanges();
        });
    }

    @Override
    public Type<AmiCheatGivePacket> type() {
        return TYPE;
    }
}
