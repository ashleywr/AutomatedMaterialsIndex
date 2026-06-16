package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeLookupKeyHandlerTest {
    @Test
    void slotFallbackCanBeDisabledWhenExternalViewerOwnsSlotKeys() {
        ItemStack stack = RecipeLookupKeyHandler.lookupStack(null, new ItemStack(stone()), false);

        assertTrue(stack.isEmpty());
    }

    @Test
    void slotFallbackStillWorksWhenAllowed() {
        ItemStack stack = RecipeLookupKeyHandler.lookupStack(null, new ItemStack(stone()), true);

        assertEquals(stone(), stack.getItem());
    }

    @Test
    void nonItemNodesDoNotMasqueradeAsStacks() {
        SearchNode oxygen = new SearchNode(
                new Identifier("mekanism", "oxygen/rv/test"),
                NodeType.INGREDIENT,
                "Oxygen",
                0,
                0,
                java.util.Map.of());

        ItemStack stack = RecipeLookupKeyHandler.lookupStack(oxygen, ItemStack.EMPTY, false);

        assertTrue(stack.isEmpty());
    }

    private static Item stone() {
        return BuiltInRegistries.ITEM.get(new Identifier("minecraft:stone"));
    }
}
