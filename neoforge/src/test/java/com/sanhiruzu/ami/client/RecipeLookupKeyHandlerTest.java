package com.sanhiruzu.ami.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

    private static Item stone() {
        return BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft:stone"));
    }
}
