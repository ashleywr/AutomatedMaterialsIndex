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

    public AMI(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerGameTests);
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(AmiCheatGivePacket.TYPE, AmiCheatGivePacket.STREAM_CODEC, AmiCheatGivePacket::handle);
        registrar.playToClient(AmiServerPingPacket.TYPE, AmiServerPingPacket.STREAM_CODEC, AmiServerPingPacket::handle);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("================================");
        LOGGER.info("Initializing Automated Materials Index...");
        LOGGER.info("AMI is a client-side recipe UI mod");
        LOGGER.info("================================");
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(com.sanhiruzu.ami.benchmark.AmiBenchmarkGameTests.class);
        event.register(com.sanhiruzu.ami.benchmark.AmiOntologyDumpGameTest.class);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("AMI server starting");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        AmiStructureCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new AmiServerPingPacket());
        }
    }
}
