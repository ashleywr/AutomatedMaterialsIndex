package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import com.sanhiruzu.ami.neoforge.AMI;

import com.sanhiruzu.ami.neoforge.client.AMIKeyMappings;
/**
 * Handles the logic for AMI-specific keybinds.
 */
public class AmiKeybindHandler {

    /**
     * Toggled by DEBUG_TOOLTIPS keybind. Persists until toggled off or the panel closes.
     */
    private static boolean debugTooltipsActive = false;

    public static boolean isDebugTooltipsActive() {
        return debugTooltipsActive;
    }

    /**
     * Call when the AMI panel is hidden so debug mode doesn't linger across sessions.
     */
    public static void resetDebugTooltips() {
        debugTooltipsActive = false;
    }

    /**
     * Checks if any AMI keybinds were pressed and performs the associated actions.
     * Called from InventoryOverlayHandler.onKeyPressed.
     */
    public static boolean onKeyPressed(int keyCode, int scanCode, int modifiers, int action) {
        // Only handle on actual key press (action == 1), not repeat (2) or release (0)
        if (action != GLFW.GLFW_PRESS) return false;

        if (AMIKeyMappings.FAVORITE.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            return handleFavoriteKey();
        }

        if (AMIKeyMappings.DEBUG_TOOLTIPS.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            debugTooltipsActive = !debugTooltipsActive;
            return true;
        }

        if (AMIKeyMappings.TOGGLE_VIEWER.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            InventoryOverlayHandler.toggleAmi();
            return true;
        }

        if (AMIKeyMappings.SHOW_RECIPES.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            return handleRecipeLookup(true);
        }

        if (AMIKeyMappings.SHOW_USES.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            return handleRecipeLookup(false);
        }

        if (AMIKeyMappings.CHEAT_GIVE_STACK.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            return handleGive(true);
        }

        if (AMIKeyMappings.CHEAT_GIVE_ONE.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            return handleGive(false);
        }

        return false;
    }

    private static boolean handleRecipeLookup(boolean showRecipes) {
        return RecipeLookupKeyHandler.openHoveredLookup(showRecipes);
    }

    private static boolean handleGive(boolean stack) {
        if (!AMICheatMode.isEnabled()) return false;
        SearchNode hovered = InventoryOverlayHandler.getManager().getHoveredNode();
        if (hovered == null) return false;
        if (hovered.type() == NodeType.ITEM) {
            if (stack) {
                AMICheatMode.giveStack(hovered.id());
            } else {
                AMICheatMode.giveItem(hovered.id());
            }
            return true;
        } else if (hovered.type() == NodeType.ENTITY) {
            if (stack) {
                AMICheatMode.giveEntityStackAsSpawnEgg(hovered.id());
            } else {
                AMICheatMode.giveEntityAsSpawnEgg(hovered.id());
            }
            return true;
        }
        return false;
    }

    private static boolean handleFavoriteKey() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) return false;

        // 1. Try hovering over an item in any AMI panel.
        SearchNode hoveredNode = InventoryOverlayHandler.getManager().getHoveredNode();

        if (hoveredNode != null) {
            AmiFavoritesHandler.getInstance().toggleFavorite(hoveredNode);
            return true;
        }

        // 2. Try hovering over a vanilla slot
        var slot = containerScreen.getSlotUnderMouse();
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            AmiFavoritesHandler.getInstance().addFavorite(stack);
            return true;
        }

        return false;
    }
}
