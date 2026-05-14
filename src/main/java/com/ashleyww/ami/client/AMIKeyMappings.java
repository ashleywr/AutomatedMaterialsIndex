package com.ashleyww.ami.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import com.ashleyww.ami.AMI;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AMIKeyMappings {
    public static final KeyMapping OPEN_AMI = new KeyMapping(
            "key.ami.open",
            InputConstants.KEY_I,
            "key.categories.gameplay"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_AMI);
    }
}
