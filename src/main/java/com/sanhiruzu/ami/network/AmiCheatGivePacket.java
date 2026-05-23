package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.AMI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Play-to-server packet: set the cursor/carried item on the server.
 * An empty stack means "delete whatever is on the cursor".
 * Requires the player to be in creative mode or have OP level 2+.
 */
public record AmiCheatGivePacket(ItemStack stack) implements CustomPacketPayload {
    public static final Type<AmiCheatGivePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AMI.MODID, "cheat_give"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AmiCheatGivePacket> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.map(AmiCheatGivePacket::new, AmiCheatGivePacket::stack);

    @Override
    public Type<AmiCheatGivePacket> type() { return TYPE; }

    public static void handle(AmiCheatGivePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!player.getAbilities().instabuild && !player.hasPermissions(2)) {
                AMI.LOGGER.warn("AMI cheat: {} attempted give/delete without permission",
                        player.getName().getString());
                return;
            }
            ItemStack toSet = packet.stack().isEmpty() ? ItemStack.EMPTY : packet.stack().copy();
            if (!toSet.isEmpty()) {
                AMI.LOGGER.info("AMI cheat give: {} → {} x{}",
                        player.getName().getString(),
                        toSet.getItem().getDescriptionId(),
                        toSet.getCount());
            } else {
                AMI.LOGGER.info("AMI cheat delete cursor: {}", player.getName().getString());
            }
            serverPlayer.containerMenu.setCarried(toSet);
            serverPlayer.containerMenu.broadcastChanges();
        });
    }
}
