package com.sanhiruzu.ami.index.providers;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecipeViewerIngredientProviderTest {

    @Test
    void skipsItemAndFluidBuiltins() {
        assertTrue(RecipeViewerIngredientIds.shouldSkipTypeUid("item_stack"));
        assertTrue(RecipeViewerIngredientIds.shouldSkipTypeUid("fluid_stack"));
        assertFalse(RecipeViewerIngredientIds.shouldSkipTypeUid("mekanism:chemical"));
    }

    @Test
    void syntheticIdKeepsNativeIdWhenUidMatchesResourceLocation() {
        Identifier oxygen = new Identifier("mekanism", "oxygen");

        Identifier resolved = RecipeViewerIngredientIds.syntheticId(
                "mekanism",
                oxygen,
                "mekanism:chemical",
                "mekanism:oxygen"
        );

        assertEquals(oxygen, resolved);
    }

    @Test
    void syntheticIdFallsBackToStableSyntheticPathForSubtypeUid() {
        Identifier oxygen = new Identifier("mekanism", "oxygen");

        Identifier resolved = RecipeViewerIngredientIds.syntheticId(
                "mekanism",
                oxygen,
                "mekanism:chemical",
                "mekanism:oxygen{amount=1}"
        );

        assertEquals("mekanism", resolved.getNamespace());
        assertTrue(resolved.getPath().startsWith("oxygen/rv/"));
        assertEquals(resolved, RecipeViewerIngredientIds.syntheticId(
                "mekanism",
                oxygen,
                "mekanism:chemical",
                "mekanism:oxygen{amount=1}"
        ));
    }
}
