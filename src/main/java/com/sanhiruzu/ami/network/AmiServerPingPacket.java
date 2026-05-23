package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.AMI;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Play-to-client packet: tells the client that this server has AMI installed.
 * Sent once per player login. Enables the custom cheat-give packet path.
 */
public record AmiServerPingPacket() implements CustomPacketPayload {
    public static final Type<AmiServerPingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AMI.MODID, "server_ping"));

    public static final StreamCodec<ByteBuf, AmiServerPingPacket> STREAM_CODEC =
            StreamCodec.unit(new AmiServerPingPacket());

    @Override
    public Type<AmiServerPingPacket> type() { return TYPE; }

    public static void handle(AmiServerPingPacket packet, IPayloadContext context) {
        // Only invoked client-side (play-to-client packet).
        AmiNetworkState.onServer = true;
        AMI.LOGGER.info("AMI: server has AMI installed — cheat actions use custom packets");
    }
}
