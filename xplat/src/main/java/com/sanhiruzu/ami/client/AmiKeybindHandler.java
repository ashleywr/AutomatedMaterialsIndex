package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;
import com.sanhiruzu.ami.compat.EmiFavoritesBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class AmiKeybindHandler {

    /**
     * Toggled by DEBUG_TOOLTIPS keybind. Persists until toggled off or the panel closes.
     */
    private static boolean debugTooltipsActive = false;

    public static boolean isDebugTooltipsActive() {
        return AmiConfig.devMode
                && Services.PLATFORM.supportsDebugTooltipToggle()
                && debugTooltipsActive;
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
        return onKeyPressed(keyCode, scanCode, modifiers, action, true, true);
    }

    public static boolean onKeyPressed(int keyCode, int scanCode, int modifiers, int action,
                                       boolean allowAmiResultLookup, boolean allowRecipeSlotFallback) {
        // Only handle on actual key press (action == 1), not repeat (2) or release (0)
        if (action != GLFW.GLFW_PRESS) return false;

        var keys = Services.PLATFORM.keyMappings();

        InputConstants.Key pressedKey = InputConstants.getKey(keyCode, scanCode);

        if (AmiKeybinds.activeAndMatches(keys.favorite(), pressedKey, modifiers)) {
            return handleFavoriteKey();
        }

        if (AmiConfig.devMode
                && Services.PLATFORM.supportsDebugTooltipToggle()
                && AmiKeybinds.activeAndMatches(keys.debugTooltips(), pressedKey, modifiers)) {
            debugTooltipsActive = !debugTooltipsActive;
            return true;
        }

        if (isToggleViewerKey(keyCode, scanCode)) {
            InventoryOverlayHandler.toggleAmi();
            return true;
        }

        if (AmiKeybinds.activeAndMatches(keys.showRecipes(), pressedKey, modifiers)) {
            return handleRecipeLookup(true, allowAmiResultLookup, allowRecipeSlotFallback);
        }

        if (AmiKeybinds.activeAndMatches(keys.showUses(), pressedKey, modifiers)) {
            return handleRecipeLookup(false, allowAmiResultLookup, allowRecipeSlotFallback);
        }

        if (AmiKeybinds.activeAndMatches(keys.cheatGiveStack(), pressedKey, modifiers)) {
            return handleGive(true);
        }

        if (AmiKeybinds.activeAndMatches(keys.cheatGiveOne(), pressedKey, modifiers)) {
            return handleGive(false);
        }

        return false;
    }

    public static boolean isToggleViewerKey(int keyCode, int scanCode) {
        return AmiKeybinds.activeAndMatches(
                Services.PLATFORM.keyMappings().toggleViewer(),
                InputConstants.getKey(keyCode, scanCode));
    }

    private static boolean handleRecipeLookup(boolean showRecipes, boolean allowAmiResultLookup, boolean allowSlotFallback) {
        return RecipeLookupKeyHandler.openHoveredLookup(showRecipes, allowAmiResultLookup, allowSlotFallback);
    }

    private static boolean handleGive(boolean fullStack) {
        if (!AMICheatMode.isEnabled()) return false;
        SearchNode hovered = InventoryOverlayHandler.getManager().getHoveredNode();
        if (hovered == null) return false;
        if (hovered.type() == NodeType.ITEM) {
            var itemStack = AmiFavoritesHandler.resolveStack(hovered);
            if (itemStack.isEmpty()) return false;
            if (fullStack) {
                AMICheatMode.giveStack(itemStack);
            } else {
                AMICheatMode.giveItem(itemStack);
            }
            return true;
        } else if (hovered.type() == NodeType.ENTITY) {
            if (fullStack) {
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
        AbstractContainerScreen<?> containerScreen = mc.screen instanceof AbstractContainerScreen<?> screen ? screen : null;

        // 1. Try hovering over an item in any AMI panel.
        SearchNode hoveredNode = InventoryOverlayHandler.getManager().getHoveredNode();

        if (hoveredNode != null) {
            AmiFavoritesHandler.getInstance().toggleFavorite(hoveredNode);
            return true;
        }

        // 2. If EMI is handling a recipe screen, its favorite key is add-only for recipe favorites.
        // Remove an existing hovered EMI favorite here and consume the key so EMI does not re-add it.
        if (Services.PLATFORM.isModLoaded("emi") && EmiFavoritesBridge.removeHoveredFavorite()) {
            AmiFavoritesHandler.getInstance().externalFavoritesChanged();
            return true;
        }

        // 3. Try hovering over a vanilla slot
        Slot slot = containerScreen == null ? null : Services.PLATFORM.getHoveredSlot(containerScreen);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            AmiFavoritesHandler.getInstance().toggleFavorite(stack);
            return true;
        }

        return false;
    }
}
