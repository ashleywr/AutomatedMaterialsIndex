package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewerLayoutPlacementTest {

    @Test
    void narrowGenericLayoutsReceiveASmallRightNudge() {
        RecipeLayout layout = genericLayout(66, 9, 40, 10);

        assertEquals(36, RecipeViewerLayoutPlacement.layoutOriginX(4, layout));
    }

    @Test
    void widerGenericLayoutsKeepTheLegacyAnchor() {
        RecipeLayout layout = genericLayout(84, 0, 58, 1);

        assertEquals(28, RecipeViewerLayoutPlacement.layoutOriginX(4, layout));
    }

    @Test
    void texturedLayoutsKeepTheLegacyAnchor() {
        RecipeLayout layout = new RecipeLayout(
                ResourceLocation.parse("ami:test"),
                ItemStack.EMPTY,
                "",
                List.of(new SlotPosition(0, 0, List.of(new ItemStack(Items.APPLE)))),
                ItemStack.EMPTY,
                1,
                1,
                false,
                61,
                19,
                24,
                18,
                ResourceLocation.parse("ami:textures/gui/test.png"),
                0,
                0,
                82,
                54,
                36,
                4,
                false
        );

        assertEquals(28, RecipeViewerLayoutPlacement.layoutOriginX(4, layout));
    }

    private static RecipeLayout genericLayout(int outputX, int outputY, int arrowX, int arrowY) {
        return new RecipeLayout(
                ResourceLocation.parse("ami:test"),
                ItemStack.EMPTY,
                "",
                List.of(
                        new SlotPosition(0, 0, List.of(new ItemStack(Items.APPLE))),
                        new SlotPosition(18, 0, List.of(new ItemStack(Items.CAKE))),
                        new SlotPosition(0, 18, List.of(new ItemStack(Items.CHEST))),
                        new SlotPosition(18, 18, List.of(new ItemStack(Items.REDSTONE)))
                ),
                ItemStack.EMPTY,
                2,
                2,
                false,
                outputX,
                outputY,
                arrowX,
                arrowY,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                true
        );
    }
}
