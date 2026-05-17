package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AMI;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class AMIKeyMappings {
    public static final String CATEGORY = "key.categories.ami";

    public static final KeyMapping FAVORITE = new KeyMapping(
            "key.ami.favorite",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_A,
            CATEGORY
    );

    public static final KeyMapping DEBUG_TOOLTIPS = new KeyMapping(
            "key.ami.debug_tooltips",
            KeyConflictContext.GUI,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_A,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_VIEWER = new KeyMapping(
            "key.ami.toggle_viewer",
            KeyConflictContext.GUI,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FAVORITE);
        event.register(DEBUG_TOOLTIPS);
        event.register(TOGGLE_VIEWER);
    }
}
