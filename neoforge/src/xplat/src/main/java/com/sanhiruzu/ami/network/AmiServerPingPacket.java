package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.neoforge.AMI;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AmiServerPingPacket() implements CustomPacketPayload {
    public static final Type<AmiServerPingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AMI.MODID, "server_ping"));

    public static final StreamCodec<ByteBuf, AmiServerPingPacket> STREAM_CODEC =
            StreamCodec.unit(new AmiServerPingPacket());

    @Override
    public Type<AmiServerPingPacket> type() {
        return TYPE;
    }

    public static void handle(AmiServerPingPacket packet, IPayloadContext context) {
        AmiNetworkState.onServer = true;
        AMI.LOGGER.info("AMI: server has AMI installed - cheat actions use custom packets");
    }
}
