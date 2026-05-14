package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import com.sanhiruzu.ami.AMI;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AMIKeyMappings {
    public static final String CATEGORY = "key.categories.ami";

    public static final KeyMapping OPEN_AMI = new KeyMapping(
            "key.ami.open",
            InputConstants.KEY_I,
            CATEGORY
    );

    public static final KeyMapping CYCLE_ATLAS = new KeyMapping(
            "key.ami.cycle_atlas",
            InputConstants.KEY_TAB,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_AMI);
        event.register(CYCLE_ATLAS);
    }
}
