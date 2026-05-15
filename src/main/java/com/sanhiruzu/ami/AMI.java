package com.sanhiruzu.ami;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(AMI.MODID)
public class AMI {
    public static final String MODID = "ami";
    // Named "AMI" so log lines show [AMI/] — class-derived names abbreviate
    // com.sanhiruzu.ami → co.sa.am, making searches for "ami" miss all output.
    public static final Logger LOGGER = LoggerFactory.getLogger("AMI");

    public AMI(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.CLIENT, AMIConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, AMILayoutConfig.SPEC, "ami-layout.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("================================");
        LOGGER.info("Initializing Automated Materials Index");
        LOGGER.info("AMI is a client-side recipe UI mod");
        LOGGER.info("================================");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("AMI server starting");
    }
}
