package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.forge.AMI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Play-to-server packet: set the cursor/carried item on the server.
 */
public class AmiCheatGivePacket {
    private final ItemStack stack;

    public AmiCheatGivePacket(ItemStack stack) {
        this.stack = stack;
    }

    public static void encode(AmiCheatGivePacket packet, FriendlyByteBuf buf) {
        buf.writeItem(packet.stack);
    }

    public static AmiCheatGivePacket decode(FriendlyByteBuf buf) {
        return new AmiCheatGivePacket(buf.readItem());
    }

    public static void handle(AmiCheatGivePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!AmiCheatPermissions.canUseCheats(serverPlayer)) {
                AMI.LOGGER.warn("AMI cheat: {} attempted give/delete without permission",
                        player.getName().getString());
                return;
            }
            ItemStack toSet = packet.stack.isEmpty() ? ItemStack.EMPTY : packet.stack.copy();
            if (toSet.isEmpty()) {
                AMI.LOGGER.debug("AMI cheat delete cursor: {}", player.getName().getString());
                serverPlayer.containerMenu.setCarried(ItemStack.EMPTY);
                serverPlayer.containerMenu.broadcastChanges();
                return;
            }
            AMI.LOGGER.debug("AMI cheat give: {} → {} x{}",
                    player.getName().getString(),
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
        });
        context.setPacketHandled(true);
    }
}
