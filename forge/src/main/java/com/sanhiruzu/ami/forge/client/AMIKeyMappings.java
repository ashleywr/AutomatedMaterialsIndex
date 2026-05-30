package com.sanhiruzu.ami.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

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

    public static final KeyMapping CHEAT_GIVE_ONE = new KeyMapping(
            "key.ami.cheat_give_one",
            KeyConflictContext.GUI,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            CATEGORY
    );

    public static final KeyMapping CHEAT_GIVE_STACK = new KeyMapping(
            "key.ami.cheat_give_stack",
            KeyConflictContext.GUI,
            KeyModifier.SHIFT,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            CATEGORY
    );

    public static final KeyMapping RECIPE_BACK = new KeyMapping(
            "key.ami.recipe_back",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSPACE,
            CATEGORY
    );

    public static final KeyMapping SHOW_RECIPES = new KeyMapping(
            "key.ami.show_recipes",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping SHOW_USES = new KeyMapping(
            "key.ami.show_uses",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FAVORITE);
        event.register(DEBUG_TOOLTIPS);
        event.register(TOGGLE_VIEWER);
        event.register(CHEAT_GIVE_ONE);
        event.register(CHEAT_GIVE_STACK);
        event.register(RECIPE_BACK);
        event.register(SHOW_RECIPES);
        event.register(SHOW_USES);
    }
}
