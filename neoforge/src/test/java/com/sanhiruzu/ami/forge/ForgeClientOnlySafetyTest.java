package com.sanhiruzu.ami.forge;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForgeClientOnlySafetyTest {
    private static final Path MAIN_MOD = Paths.get("../forge/src/main/java/com/sanhiruzu/ami/forge/AMI.java");
    private static final Path CLIENT_MOD = Paths.get("../forge/src/main/java/com/sanhiruzu/ami/forge/AMIClient.java");
    private static final Path PACKET_HANDLER = Paths.get("../forge/src/main/java/com/sanhiruzu/ami/forge/network/AmiPacketHandler.java");

    @Test
    void forgeNetworkChannelAcceptsServersAndClientsWithoutAmi() throws Exception {
        String source = Files.readString(PACKET_HANDLER);

        assertTrue(source.contains("NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)"),
                "AMI's Forge network channel must accept vanilla/absent remote channels for client-only use");
        assertFalse(source.contains("PROTOCOL_VERSION::equals"),
                "An exact Forge channel predicate makes AMI mandatory on both sides");
    }

    @Test
    void forgeServerOnlyInstallDoesNotSendToClientsWithoutAmi() throws Exception {
        String source = Files.readString(MAIN_MOD);

        assertTrue(source.contains("AmiPacketHandler.INSTANCE.isRemotePresent(serverPlayer.connection.connection)"),
                "AMI's Forge server ping must only be sent to clients that registered AMI's channel");
        assertTrue(source.contains("AmiPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new AmiServerPingPacket())"),
                "AMI should still detect AMI-enabled servers when both sides negotiated the channel");
    }

    @Test
    void forgeClientEntrypointRemainsPhysicalClientOnly() throws Exception {
        String mainSource = Files.readString(MAIN_MOD);
        String clientSource = Files.readString(CLIENT_MOD);

        assertTrue(mainSource.contains("FMLEnvironment.dist == Dist.CLIENT"),
                "AMIClient initialization must stay gated to the physical client");
        assertTrue(clientSource.contains("@Mod.EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT"),
                "AMIClient Forge event subscribers must stay gated to the physical client");
    }
}
