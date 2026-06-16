package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.neoforge.AMI;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AmiServerPingPacket() implements CustomPacketPayload {
    public static final Type<AmiServerPingPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AMI.MODID, "server_ping"));

    public static final StreamCodec<ByteBuf, AmiServerPingPacket> STREAM_CODEC =
            StreamCodec.unit(new AmiServerPingPacket());

    public static void handle(AmiServerPingPacket packet, IPayloadContext context) {
        AmiNetworkState.onServer = true;
        AMI.LOGGER.debug("AMI: server has AMI installed - cheat actions use custom packets");
    }

    @Override
    public Type<AmiServerPingPacket> type() {
        return TYPE;
    }
}
