package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.RecipeViewerHoverHintPolicy.Hint;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeViewerHoverHintPolicyTest {

    @Test
    void multiVariantIngredientSlotGetsScrollHint() {
        SlotPosition slot = new SlotPosition(4, 4, List.of(
                new ItemStack(Items.APPLE),
                new ItemStack(Items.CAKE),
                new ItemStack(Items.CHEST)
        ));

        assertEquals(List.of(Hint.INGREDIENT_SCROLL),
                RecipeViewerHoverHintPolicy.ingredientHints(slot, true));
    }

    @Test
    void singleVariantIngredientSlotGetsNoExtraHint() {
        SlotPosition slot = new SlotPosition(4, 4, List.of(new ItemStack(Items.APPLE)));

        assertEquals(List.of(), RecipeViewerHoverHintPolicy.ingredientHints(slot, true));
    }

    @Test
    void transferButtonAlwaysShowsTransferHint() {
        assertEquals(List.of(Hint.TRANSFER_BUTTON),
                RecipeViewerHoverHintPolicy.transferButtonHints());
    }

    @Test
    void outputSlotOnlyShowsShiftTransferHintWhenTransferIsAvailable() {
        assertEquals(List.of(Hint.OUTPUT_SHIFT_TRANSFER),
                RecipeViewerHoverHintPolicy.outputHints(true));
        assertEquals(List.of(),
                RecipeViewerHoverHintPolicy.outputHints(false));
    }

    @Test
    void workstationHoverShowsRecipesAndUsesHints() {
        assertEquals(List.of(Hint.WORKSTATION_LEFT_CLICK, Hint.WORKSTATION_RIGHT_CLICK),
                RecipeViewerHoverHintPolicy.workstationHints());
    }

    @Test
    void hintEnumNamesStayStableForTooltipMapping() {
        assertTrue(RecipeViewerHoverHintPolicy.transferButtonHints()
                .contains(Hint.TRANSFER_BUTTON));
        assertTrue(RecipeViewerHoverHintPolicy.outputHints(true)
                .contains(Hint.OUTPUT_SHIFT_TRANSFER));
        assertTrue(RecipeViewerHoverHintPolicy.workstationHints()
                .contains(Hint.WORKSTATION_LEFT_CLICK));
        assertTrue(RecipeViewerHoverHintPolicy.workstationHints()
                .contains(Hint.WORKSTATION_RIGHT_CLICK));
    }
}
