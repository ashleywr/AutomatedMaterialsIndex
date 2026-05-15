package com.sanhiruzu.ami;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.sanhiruzu.ami.client.AMIKeyMappings;
import com.sanhiruzu.ami.client.AMIScreen;
import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.results.ItemGridView;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@Mod(value = AMI.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AMIClient {

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
    static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ItemIconCache.invalidate();
        ItemGridView.clearStackCache();
        RendererRegistry.invalidateAll();
    }

    // Disabled: Indexing now happens lazily on first inventory open
    // This gives the server more time to sync registries before we try to access them
    // @SubscribeEvent
    // static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
    //     ...
    // }

    // Disabled - focus on World Atlas overlay instead of full-screen Items GUI
    // @SubscribeEvent
    // static void onKeyInput(InputEvent.Key event) {
    //     if (AMIKeyMappings.OPEN_AMI.consumeClick()) {
    //         AMI.LOGGER.debug("Opening AMI screen");
    //         Minecraft.getInstance().setScreen(new AMIScreen());
    //     }
    // }
}
