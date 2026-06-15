package com.sanhiruzu.ami.fabric;

import com.sanhiruzu.ami.command.AmiStructureCommand;
import com.sanhiruzu.ami.network.AmiCheatGivePacket;
import com.sanhiruzu.ami.network.AmiCheatPokemonPacket;
import com.sanhiruzu.ami.network.AmiServerPingPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AMI common (server + client) entrypoint for Fabric.
 * <p>
 * Responsibilities:
 * - Registers payload types for all three AMI packets on the play channel.
 * - Installs the server-side handler for cheat packets (C2S).
 * - Sends the server-ping packet to players on join (S2C) so they know AMI is present.
 * - Registers the debug /ami-structure command (opt-in via system property).
 */
public class AmiFabric implements ModInitializer {
    public static final String MODID = "ami";
    public static final Logger LOGGER = LoggerFactory.getLogger("AMI");

    private static final String DEBUG_COMMANDS_PROPERTY = "ami.debugCommands";

    @Override
    public void onInitialize() {
        LOGGER.debug("================================");
        LOGGER.debug("Initializing Automated Materials Index (Fabric)...");
        LOGGER.debug("AMI is a client-side recipe UI mod");
        LOGGER.debug("================================");

        registerPayloads();
        registerServerEvents();

        if (Boolean.getBoolean(DEBUG_COMMANDS_PROPERTY)) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                    AmiStructureCommand.register(dispatcher));
        }
    }

    // -------------------------------------------------------------------------
    // Networking
    // -------------------------------------------------------------------------

    /**
     * Registers payload types with Fabric's play-channel registry.
     * <p>
     * C2S (client-to-server): cheat give + pokemon — both optional so vanilla servers
     * without AMI still connect.
     * S2C (server-to-client): server-ping — tells the client AMI is installed on the server.
     * <p>
     * All three types must be registered here (common init) so the payload codec is
     * available on both sides at connection time.
     */
    private void registerPayloads() {
        // C2S payloads
        PayloadTypeRegistry.playC2S().register(AmiCheatGivePacket.TYPE, AmiCheatGivePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AmiCheatPokemonPacket.TYPE, AmiCheatPokemonPacket.STREAM_CODEC);

        // S2C payload
        PayloadTypeRegistry.playS2C().register(AmiServerPingPacket.TYPE, AmiServerPingPacket.STREAM_CODEC);

        // Server-side receivers for the C2S packets
        ServerPlayNetworking.registerGlobalReceiver(AmiCheatGivePacket.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> payload.handleOnServer(player));
                });
        ServerPlayNetworking.registerGlobalReceiver(AmiCheatPokemonPacket.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> payload.handleOnServer(player));
                });
    }

    // -------------------------------------------------------------------------
    // Server lifecycle
    // -------------------------------------------------------------------------

    private void registerServerEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                LOGGER.debug("AMI server starting"));

        // Send server-ping to each player on join so AmiNetworkState.onServer becomes true
        // on their client, enabling the custom cheat-give packet path.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (ServerPlayNetworking.canSend(player, AmiServerPingPacket.TYPE)) {
                ServerPlayNetworking.send(player, new AmiServerPingPacket());
            }
        });
    }
}
