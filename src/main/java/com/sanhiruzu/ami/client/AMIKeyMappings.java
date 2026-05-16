package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.AMI;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AMIKeyMappings {
    public static final String CATEGORY = "key.categories.ami";

    public static final KeyMapping TOGGLE_AMI = new KeyMapping(
            "key.ami.toggle",
            InputConstants.KEY_I,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_AMI);
    }
}
