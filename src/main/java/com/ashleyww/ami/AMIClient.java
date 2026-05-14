package com.ashleyww.ami;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = AMI.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AMIClient {
    public AMIClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        AMI.LOGGER.info("AMI client setup initialized");
        AMI.LOGGER.info("Player: {}", Minecraft.getInstance().getUser().getName());
    }
}
