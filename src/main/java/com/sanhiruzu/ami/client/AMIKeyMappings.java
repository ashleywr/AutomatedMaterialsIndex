package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AMI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AMIKeyMappings {
    public static final String CATEGORY = "key.categories.ami";

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // No key mappings registered - TOGGLE_AMI removed as obsolete
    }
}
