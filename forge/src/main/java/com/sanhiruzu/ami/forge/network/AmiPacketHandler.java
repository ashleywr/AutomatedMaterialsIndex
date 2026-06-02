package com.sanhiruzu.ami.forge.network;

import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.network.AmiCheatGivePacket;
import com.sanhiruzu.ami.network.AmiServerPingPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class AmiPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AMI.MODID, "main"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    private static int nextId = 0;

    public static void register() {
        INSTANCE.registerMessage(nextId++, AmiCheatGivePacket.class, AmiCheatGivePacket::encode, AmiCheatGivePacket::decode, AmiCheatGivePacket::handle);
        INSTANCE.registerMessage(nextId++, AmiServerPingPacket.class, AmiServerPingPacket::encode, AmiServerPingPacket::decode, AmiServerPingPacket::handle);
    }
}
