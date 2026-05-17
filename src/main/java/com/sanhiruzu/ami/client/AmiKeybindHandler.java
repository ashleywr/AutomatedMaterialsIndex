package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Handles the logic for AMI-specific keybinds.
 */
public class AmiKeybindHandler {

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
            // Toggle debug tooltips display (handled by DebugTooltip component)
            return true;
        }

        if (AMIKeyMappings.TOGGLE_VIEWER.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            InventoryOverlayHandler.toggleAmi();
            return true;
        }

        return false;
    }

    private static boolean handleFavoriteKey() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) return false;

        // 1. Try hovering over an item in the AMI panels
        SearchNode hoveredNode = InventoryOverlayHandler.getManager().getResultsPanel().getInnerPanel().getHoveredNode();
        if (hoveredNode == null && InventoryOverlayHandler.getManager().getLeftPanel() != null) {
            hoveredNode = InventoryOverlayHandler.getManager().getLeftPanel().getInnerPanel().getHoveredNode();
        }

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
