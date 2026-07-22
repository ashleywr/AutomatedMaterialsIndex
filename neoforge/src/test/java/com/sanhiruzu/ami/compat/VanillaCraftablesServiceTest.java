package com.sanhiruzu.ami.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaCraftablesServiceTest {

    @Test
    void nonRecipeBookMenusDoNotContributeContainerContents() {
        assertFalse(CraftablesScopePolicy.shouldAccountMenuContents(false));
        assertTrue(CraftablesScopePolicy.shouldAccountMenuContents(true));
    }

    @Test
    void recipeBookSlotPolicyCountsOnlyUsableCraftInputs() {
        assertTrue(CraftablesScopePolicy.shouldAccountRecipeBookSlot(true, false, true));
        assertFalse(CraftablesScopePolicy.shouldAccountRecipeBookSlot(false, false, true));
        assertFalse(CraftablesScopePolicy.shouldAccountRecipeBookSlot(true, true, true));
        assertFalse(CraftablesScopePolicy.shouldAccountRecipeBookSlot(true, false, false));
    }
}
