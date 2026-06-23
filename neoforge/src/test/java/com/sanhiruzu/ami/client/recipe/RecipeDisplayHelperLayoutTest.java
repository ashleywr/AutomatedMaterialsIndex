package com.sanhiruzu.ami.client.recipe;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeDisplayHelperLayoutTest {
    @Test
    void singleItemShapedRecipesUseCenterSlotInFullCraftingGrid() {
        List<SlotPosition> slots = RecipeDisplayHelper.createCraftingGridSlots(
                List.of(List.of(new ItemStack(Items.APPLE))),
                1,
                1);

        assertFullCraftingGrid(slots);
        assertFilledSlots(slots, List.of(4));
    }

    @Test
    void twoWideShapedRecipesUseMiddleRowLikeJei() {
        List<SlotPosition> slots = RecipeDisplayHelper.createCraftingGridSlots(
                List.of(
                        List.of(new ItemStack(Items.APPLE)),
                        List.of(new ItemStack(Items.CAKE))
                ),
                2,
                1);

        assertFullCraftingGrid(slots);
        assertFilledSlots(slots, List.of(3, 4));
    }

    @Test
    void twoByTwoShapedRecipesStayInTopLeftQuadrantLikeJei() {
        List<SlotPosition> slots = RecipeDisplayHelper.createCraftingGridSlots(
                List.of(
                        List.of(new ItemStack(Items.APPLE)),
                        List.of(new ItemStack(Items.CAKE)),
                        List.of(new ItemStack(Items.CHEST)),
                        List.of(new ItemStack(Items.REDSTONE))
                ),
                2,
                2);

        assertFullCraftingGrid(slots);
        assertFilledSlots(slots, List.of(0, 1, 3, 4));
    }

    @Test
    void threeByTwoShapedRecipesUseBottomTwoRowsLikeJei() {
        List<SlotPosition> slots = RecipeDisplayHelper.createCraftingGridSlots(
                List.of(
                        List.of(new ItemStack(Items.APPLE)),
                        List.of(new ItemStack(Items.CAKE)),
                        List.of(new ItemStack(Items.CHEST)),
                        List.of(new ItemStack(Items.REDSTONE)),
                        List.of(new ItemStack(Items.RAIL)),
                        List.of(new ItemStack(Items.SHULKER_BOX))
                ),
                3,
                2);

        assertFullCraftingGrid(slots);
        assertFilledSlots(slots, List.of(3, 4, 5, 6, 7, 8));
    }

    private static void assertFullCraftingGrid(List<SlotPosition> slots) {
        assertEquals(9, slots.size());

        for (int i = 0; i < slots.size(); i++) {
            SlotPosition slot = slots.get(i);
            assertEquals(4 + (i % 3) * 18, slot.x(), "slot x at index " + i);
            assertEquals(4 + (i / 3) * 18, slot.y(), "slot y at index " + i);
        }
    }

    private static void assertFilledSlots(List<SlotPosition> slots, List<Integer> filledSlots) {
        for (int i = 0; i < slots.size(); i++) {
            if (filledSlots.contains(i)) {
                assertFalse(slots.get(i).alternatives().isEmpty(), "expected filled slot " + i);
            } else {
                assertTrue(slots.get(i).alternatives().isEmpty(), "expected placeholder slot " + i);
            }
        }
    }
}
