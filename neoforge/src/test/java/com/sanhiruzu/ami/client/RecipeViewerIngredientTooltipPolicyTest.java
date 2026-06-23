package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.RecipeViewerHoverHintPolicy.Hint;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewerIngredientTooltipPolicyTest {

    @Test
    void multiVariantIngredientSlotOnlyShowsScrollHintWhileShiftIsHeld() {
        SlotPosition slot = new SlotPosition(4, 4, List.of(
                new ItemStack(Items.APPLE),
                new ItemStack(Items.CAKE),
                new ItemStack(Items.CHEST)
        ));

        assertEquals(List.of(), RecipeViewerHoverHintPolicy.ingredientHints(slot, false));
        assertEquals(List.of(Hint.INGREDIENT_SCROLL), RecipeViewerHoverHintPolicy.ingredientHints(slot, true));
    }

    @Test
    void ingredientCycleCounterOnlyShowsTheVariantPosition() {
        assertEquals("1/4", RecipeViewerHoverHintPolicy.ingredientCycleCounter(0, 4).getString());
    }
}
