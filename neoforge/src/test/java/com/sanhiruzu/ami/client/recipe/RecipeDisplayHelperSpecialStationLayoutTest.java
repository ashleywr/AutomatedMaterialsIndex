package com.sanhiruzu.ami.client.recipe;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.GenericFallbackLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeDisplayHelperSpecialStationLayoutTest {
    @Test
    void anvilLikeLayoutUsesSharedNativeSpacingForTwoInputs() {
        GenericFallbackLayout layout = RecipeDisplayHelper.createAnvilLikeLayout(
                List.of(new ItemStack(Items.APPLE)),
                List.of(new ItemStack(Items.CAKE))
        );

        assertEquals(2, layout.gridWidth());
        assertEquals(1, layout.gridHeight());
        assertSlot(layout.inputs().get(0), 0, 0);
        assertSlot(layout.inputs().get(1), 36, 0);
        assertEquals(58, layout.arrowX());
        assertEquals(1, layout.arrowY());
        assertEquals(84, layout.outputX());
        assertEquals(0, layout.outputY());
    }

    @Test
    void singleInputStationLayoutUsesSharedNativeSpacingForOneInput() {
        GenericFallbackLayout layout = RecipeDisplayHelper.createSingleInputStationLayout(
                List.of(new ItemStack(Items.APPLE))
        );

        assertEquals(1, layout.gridWidth());
        assertEquals(1, layout.gridHeight());
        assertSlot(layout.inputs().getFirst(), 0, 0);
        assertEquals(22, layout.arrowX());
        assertEquals(1, layout.arrowY());
        assertEquals(48, layout.outputX());
        assertEquals(0, layout.outputY());
    }

    private static void assertSlot(SlotPosition slot, int x, int y) {
        assertEquals(x, slot.x());
        assertEquals(y, slot.y());
    }
}
