package com.ashleyww.ami;

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

import com.ashleyww.ami.client.AMIKeyMappings;
import com.ashleyww.ami.client.AMIScreen;

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

        boolean jeiLoaded = ModList.get().isLoaded("jei");
        boolean emiLoaded = ModList.get().isLoaded("emi");
        if (jeiLoaded || emiLoaded) {
            AMI.LOGGER.info("JEI or EMI detected - using shell UI as fallback");
        }
    }

    @SubscribeEvent
    static void onKeyInput(InputEvent.Key event) {
        if (AMIKeyMappings.OPEN_AMI.consumeClick()) {
            Minecraft.getInstance().setScreen(new AMIScreen());
        }
    }
}
