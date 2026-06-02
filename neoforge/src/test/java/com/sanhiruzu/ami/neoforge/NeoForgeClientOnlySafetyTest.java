package com.sanhiruzu.ami.neoforge;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NeoForgeClientOnlySafetyTest {
    private static final Path MAIN_MOD = Paths.get("../neoforge/src/main/java/com/sanhiruzu/ami/neoforge/AMI.java");
    private static final Path CLIENT_MOD = Paths.get("../neoforge/src/main/java/com/sanhiruzu/ami/neoforge/AMIClient.java");
    private static final Path MIXIN_CONFIG = Paths.get("../neoforge/src/main/resources/ami.mixins.json");
    private static final Path MODS_TOML_TEMPLATE = Paths.get("../xplat/src/main/templates/META-INF/neoforge.mods.toml");

    @Test
    void neoforgePayloadsAreOptionalForServersWithoutAmi() throws Exception {
        String source = Files.readString(MAIN_MOD);

        assertTrue(source.contains("event.registrar(\"1\").optional()"),
                "AMI's NeoForge payloads must be optional so clients can join servers that do not have AMI installed");
    }

    @Test
    void neoforgeServerOnlyInstallDoesNotSendToClientsWithoutAmi() throws Exception {
        String source = Files.readString(MAIN_MOD);

        assertTrue(source.contains("serverPlayer.connection.hasChannel(AmiServerPingPacket.TYPE)"),
                "AMI's NeoForge server ping must only be sent to clients that negotiated AMI's payload channel");
        assertTrue(source.contains("PacketDistributor.sendToPlayer(serverPlayer, new AmiServerPingPacket())"),
                "AMI should still detect AMI-enabled servers when both sides negotiated the channel");
    }

    @Test
    void commonEntrypointDoesNotReferenceMinecraftClientClasses() throws Exception {
        String source = Files.readString(MAIN_MOD);

        assertFalse(source.contains("net.minecraft.client"),
                "The common NeoForge @Mod entrypoint loads on dedicated servers and must not reference client-only Minecraft classes");
        assertFalse(source.contains("com.sanhiruzu.ami.client"),
                "The common NeoForge @Mod entrypoint loads on dedicated servers and must not reference AMI client classes");
    }

    @Test
    void clientEntrypointIsPhysicalClientOnly() throws Exception {
        String source = Files.readString(CLIENT_MOD);

        assertTrue(source.contains("@Mod(value = AMI.MODID, dist = Dist.CLIENT)"),
                "AMIClient must stay gated to the physical client");
        assertTrue(source.contains("@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)"),
                "AMIClient event subscribers must stay gated to the physical client");
    }

    @Test
    void mixinsRemainDeclaredOnlyAsClientMixins() throws Exception {
        String metadata = Files.readString(MODS_TOML_TEMPLATE);
        String mixins = Files.readString(MIXIN_CONFIG);

        assertTrue(metadata.contains("[[mixins]]"),
                "NeoForge metadata must declare ami.mixins.json for packaged runs");
        assertTrue(metadata.contains("config = \"${mod_id}.mixins.json\""),
                "NeoForge metadata must point at the AMI mixin config");
        assertTrue(mixins.contains("\"mixins\": []"),
                "AMI's common mixin list must stay empty because common mixins load on dedicated servers too");
        assertTrue(mixins.contains("\"client\""),
                "AMI's runtime mixins should be declared in the client mixin list");
    }
}
