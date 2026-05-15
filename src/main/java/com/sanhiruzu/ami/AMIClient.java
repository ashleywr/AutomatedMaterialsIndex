package com.sanhiruzu.ami;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.sanhiruzu.ami.client.AMIKeyMappings;
import com.sanhiruzu.ami.client.AMIScreen;
import com.sanhiruzu.ami.index.WorldAtlasIndexer;

@Mod(value = AMI.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AMIClient {
    private static int structureRetryTicks = -1;
    private static final int STRUCTURE_RETRY_DELAY = 20;

    public AMIClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        AMI.LOGGER.info("================================");
        AMI.LOGGER.info("AMI client setup initialized");
        AMI.LOGGER.info("================================");

        boolean jeiLoaded = ModList.get().isLoaded("jei");
        boolean emiLoaded = ModList.get().isLoaded("emi");

        if (jeiLoaded) {
            AMI.LOGGER.info("✓ JEI detected - plugin will integrate when ready");
        }
        if (emiLoaded) {
            AMI.LOGGER.info("✓ EMI detected - plugin will integrate when ready");
        }
        if (!jeiLoaded && !emiLoaded) {
            AMI.LOGGER.info("✓ No recipe UI detected - AMI shell UI will be used");
        }

        if (Minecraft.getInstance().getUser() != null) {
            AMI.LOGGER.debug("Player: {}", Minecraft.getInstance().getUser().getName());
        }
    }

    @SubscribeEvent
    static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // LoggingIn fires after mc.level is set and the client is fully connected —
        // safer than LevelEvent.Load which can fire before mc.level is assigned.
        var level = event.getPlayer().clientLevel;
        try {
            com.sanhiruzu.ami.index.Indexer.index();
            WorldAtlasIndexer.index(level);
            // Schedule structure retry in case registry wasn't synced yet
            structureRetryTicks = STRUCTURE_RETRY_DELAY;
        } catch (Exception e) {
            AMI.LOGGER.error("Error during world-load indexing", e);
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        // Retry structure indexing after delay using connection registry
        if (structureRetryTicks > 0) {
            structureRetryTicks--;
            if (structureRetryTicks == 0) {
                var minecraft = Minecraft.getInstance();
                if (minecraft.level != null) {
                    try {
                        WorldAtlasIndexer.indexStructuresFromConnection();
                    } catch (Exception e) {
                        AMI.LOGGER.error("Error during deferred structure indexing", e);
                    }
                }
            }
        }
    }

    // Disabled - focus on World Atlas overlay instead of full-screen Items GUI
    // @SubscribeEvent
    // static void onKeyInput(InputEvent.Key event) {
    //     if (AMIKeyMappings.OPEN_AMI.consumeClick()) {
    //         AMI.LOGGER.debug("Opening AMI screen");
    //         Minecraft.getInstance().setScreen(new AMIScreen());
    //     }
    // }
}
