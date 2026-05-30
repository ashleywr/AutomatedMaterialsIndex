package com.sanhiruzu.ami.forge;

import com.sanhiruzu.ami.command.AmiStructureCommand;
import com.sanhiruzu.ami.forge.network.AmiPacketHandler;
import com.sanhiruzu.ami.network.AmiServerPingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AMI.MODID)
public class AMI {
    public static final String MODID = "ami";
    public static final Logger LOGGER = LoggerFactory.getLogger("AMI");
    private static final String DEBUG_COMMANDS_PROPERTY = "ami.debugCommands";

    public AMI() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            AMIClient.init();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.debug("================================");
        LOGGER.debug("Initializing Automated Materials Index...");
        LOGGER.debug("AMI is a client-side recipe UI mod");
        LOGGER.debug("================================");
        AmiPacketHandler.register();
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
            AmiPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new AmiServerPingPacket());
        }
    }
}
