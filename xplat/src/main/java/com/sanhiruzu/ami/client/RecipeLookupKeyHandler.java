package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shared handling for JEI/EMI-style recipe lookup keys.
 */
public final class RecipeLookupKeyHandler {
    private RecipeLookupKeyHandler() {
    }

    public static boolean openHoveredLookup(boolean showRecipes) {
        return openHoveredLookup(showRecipes, true, true);
    }

    public static boolean openHoveredLookup(boolean showRecipes, boolean allowAmiResultLookup, boolean allowSlotFallback) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack hoveredSlotStack = ItemStack.EMPTY;

        if (allowSlotFallback && mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot slot = Services.PLATFORM.getHoveredSlot(containerScreen);
            if (slot != null && slot.hasItem()) {
                hoveredSlotStack = slot.getItem();
            }
        }

        return openHoveredLookup(
                allowAmiResultLookup ? InventoryOverlayHandler.getManager().getHoveredNode() : null,
                hoveredSlotStack,
                showRecipes,
                allowSlotFallback
        );
    }

    public static boolean openHoveredLookup(SearchNode hoveredNode, ItemStack hoveredSlotStack, boolean showRecipes) {
        return openHoveredLookup(hoveredNode, hoveredSlotStack, showRecipes, true);
    }

    public static boolean openHoveredLookup(SearchNode hoveredNode, ItemStack hoveredSlotStack, boolean showRecipes,
                                           boolean allowSlotFallback) {
        if (hoveredNode != null && hoveredNode.type() != NodeType.ITEM) {
            boolean supported = showRecipes
                    ? RecipeViewerBridge.hasRecipes(hoveredNode)
                    : RecipeViewerBridge.hasUses(hoveredNode);
            if (!supported) {
                return false;
            }
            if (showRecipes) {
                RecipeViewerBridge.openRecipes(hoveredNode);
            } else {
                RecipeViewerBridge.openUses(hoveredNode);
            }
            return true;
        }

        ItemStack stack = lookupStack(hoveredNode, hoveredSlotStack, allowSlotFallback);
        if (stack.isEmpty()) return false;

        if (showRecipes) {
            RecipeViewerBridge.openRecipes(stack);
        } else {
            RecipeViewerBridge.openUses(stack);
        }
        return true;
    }

    static ItemStack lookupStack(SearchNode hoveredNode, ItemStack hoveredSlotStack, boolean allowSlotFallback) {
        ItemStack stack = stackFromNode(hoveredNode);
        if (stack.isEmpty() && allowSlotFallback && hoveredSlotStack != null) {
            stack = hoveredSlotStack;
        }
        return stack;
    }

    private static ItemStack stackFromNode(SearchNode node) {
        if (node == null || node.type() != NodeType.ITEM) return ItemStack.EMPTY;
        return com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node);
    }
}
