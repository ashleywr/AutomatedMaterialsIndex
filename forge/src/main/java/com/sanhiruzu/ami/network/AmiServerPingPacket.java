package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.forge.AMI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Play-to-client packet: tells the client that this server has AMI installed.
 */
public class AmiServerPingPacket {
    public AmiServerPingPacket() {}

    public static void encode(AmiServerPingPacket packet, FriendlyByteBuf buf) {}

    public static AmiServerPingPacket decode(FriendlyByteBuf buf) {
        return new AmiServerPingPacket();
    }

    public static void handle(AmiServerPingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Only invoked client-side (play-to-client packet).
            AmiNetworkState.onServer = true;
            AMI.LOGGER.info("AMI: server has AMI installed — cheat actions use custom packets");
        });
        context.setPacketHandled(true);
    }
}
