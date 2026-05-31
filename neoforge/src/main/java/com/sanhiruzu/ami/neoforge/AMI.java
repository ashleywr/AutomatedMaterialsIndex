package com.sanhiruzu.ami.neoforge;

import com.sanhiruzu.ami.command.AmiStructureCommand;
import com.sanhiruzu.ami.network.AmiCheatGivePacket;
import com.sanhiruzu.ami.network.AmiServerPingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AMI.MODID)
public class AMI {
    public static final String MODID = "ami";
    public static final Logger LOGGER = LoggerFactory.getLogger("AMI");
    private static final String DEBUG_COMMANDS_PROPERTY = "ami.debugCommands";
    private static final String REGISTER_GAME_TESTS_PROPERTY = "ami.registerGameTests";

    public AMI(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        if (Boolean.getBoolean(REGISTER_GAME_TESTS_PROPERTY)) {
            modEventBus.addListener(this::registerGameTests);
        }
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToServer(AmiCheatGivePacket.TYPE, AmiCheatGivePacket.STREAM_CODEC, AmiCheatGivePacket::handle);
        registrar.playToClient(AmiServerPingPacket.TYPE, AmiServerPingPacket.STREAM_CODEC, AmiServerPingPacket::handle);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.debug("================================");
        LOGGER.debug("Initializing Automated Materials Index...");
        LOGGER.debug("AMI is a client-side recipe UI mod");
        LOGGER.debug("================================");
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        try {
            event.register(Class.forName("com.sanhiruzu.ami.benchmark.AmiBenchmarkGameTests"));
            event.register(Class.forName("com.sanhiruzu.ami.benchmark.AmiOntologyDumpGameTest"));
        } catch (ClassNotFoundException e) {
            LOGGER.warn("AMI GameTests requested but benchmark classes are not present in this jar", e);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.debug("AMI server starting");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        if (!Boolean.getBoolean(DEBUG_COMMANDS_PROPERTY)) {
            return;
        }
        AmiStructureCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new AmiServerPingPacket());
        }
    }
}
