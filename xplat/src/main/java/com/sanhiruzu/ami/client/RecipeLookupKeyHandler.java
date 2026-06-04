package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
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
            var slot = containerScreen.getSlotUnderMouse();
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
        return BuiltInRegistries.ITEM.getOptional(node.id())
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }
}
