package com.sanhiruzu.ami.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record AmiServerPingPacket() implements CustomPacketPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger("AMI");

    public static final Type<AmiServerPingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ami", "server_ping"));

    public static final StreamCodec<ByteBuf, AmiServerPingPacket> STREAM_CODEC =
            StreamCodec.unit(new AmiServerPingPacket());

    /**
     * Client-side handler: records that the server has AMI installed.
     */
    public void handleOnClient() {
        AmiNetworkState.onServer = true;
        LOGGER.debug("AMI: server has AMI installed - cheat actions use custom packets");
    }

    @Override
    public Type<AmiServerPingPacket> type() {
        return TYPE;
    }
}
