package com.sanhiruzu.ami.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.platform.IAmiKeyMappings;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric key mappings for AMI. Uses vanilla KeyMapping constructors (no KeyConflictContext/KeyModifier).
 * Registration with Fabric's KeyBindingHelper happens in a later milestone (Milestone C entrypoints).
 */
public class FabricAmiKeyMappings implements IAmiKeyMappings {
    public static final String CATEGORY = "key.categories.ami";

    private final KeyMapping favorite = new KeyMapping(
            "key.ami.favorite",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private final KeyMapping debugTooltips = new KeyMapping(
            "key.ami.debug_tooltips",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    private final KeyMapping toggleViewer = new KeyMapping(
            "key.ami.toggle_viewer",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    private final KeyMapping cheatGiveOne = new KeyMapping(
            "key.ami.cheat_give_one",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            CATEGORY
    );

    private final KeyMapping cheatGiveStack = new KeyMapping(
            "key.ami.cheat_give_stack",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            CATEGORY
    );

    private final KeyMapping recipeBack = new KeyMapping(
            "key.ami.recipe_back",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSPACE,
            CATEGORY
    );

    private final KeyMapping showRecipes = new KeyMapping(
            "key.ami.show_recipes",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    private final KeyMapping showUses = new KeyMapping(
            "key.ami.show_uses",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY
    );

    @Override
    public KeyMapping favorite() {
        return favorite;
    }

    @Override
    public KeyMapping toggleViewer() {
        return toggleViewer;
    }

    @Override
    public KeyMapping showRecipes() {
        return showRecipes;
    }

    @Override
    public KeyMapping showUses() {
        return showUses;
    }

    @Override
    public KeyMapping cheatGiveStack() {
        return cheatGiveStack;
    }

    @Override
    public KeyMapping cheatGiveOne() {
        return cheatGiveOne;
    }

    @Override
    public KeyMapping debugTooltips() {
        return debugTooltips;
    }

    @Override
    public KeyMapping recipeBack() {
        return recipeBack;
    }

    @Override
    public KeyMapping[] all() {
        return new KeyMapping[]{
                favorite, toggleViewer, showRecipes, showUses,
                cheatGiveStack, cheatGiveOne, debugTooltips, recipeBack
        };
    }
}
