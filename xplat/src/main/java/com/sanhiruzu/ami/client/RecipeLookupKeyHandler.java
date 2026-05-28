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
        Minecraft mc = Minecraft.getInstance();
        ItemStack hoveredSlotStack = ItemStack.EMPTY;

        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            var slot = containerScreen.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) {
                hoveredSlotStack = slot.getItem();
            }
        }

        return openHoveredLookup(
                InventoryOverlayHandler.getManager().getHoveredNode(),
                hoveredSlotStack,
                showRecipes
        );
    }

    public static boolean openHoveredLookup(SearchNode hoveredNode, ItemStack hoveredSlotStack, boolean showRecipes) {
        ItemStack stack = stackFromNode(hoveredNode);
        if (stack.isEmpty() && hoveredSlotStack != null) {
            stack = hoveredSlotStack;
        }
        if (stack.isEmpty()) return false;

        if (showRecipes) {
            RecipeViewerBridge.openRecipes(stack);
        } else {
            RecipeViewerBridge.openUses(stack);
        }
        return true;
    }

    private static ItemStack stackFromNode(SearchNode node) {
        if (node == null || node.type() != NodeType.ITEM) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(node.id())
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }
}
