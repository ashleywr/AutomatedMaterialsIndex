package com.sanhiruzu.ami.compat;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiIngredientBridgeTest {
    @Test
    void skipsJeiPanelEntryTypesFromGlobalIngredientIndex() {
        assertFalse(JeiIngredientBridge.isBrowseableIngredientTypeForAmi(ItemStack.class));
        assertFalse(JeiIngredientBridge.isBrowseableIngredientTypeForAmi(Holder.class));
        assertFalse(JeiIngredientBridge.isBrowseableIngredientClassNameForAmi(
                "de.melanx.jea.api.client.IAdvancementInfo"));
        assertFalse(JeiIngredientBridge.isBrowseableIngredientClassNameForAmi(
                "net.minecraft.world.effect.MobEffectInstance"));
        assertFalse(JeiIngredientBridge.isBrowseableIngredientClassNameForAmi(
                "net.neoforged.neoforge.fluids.FluidStack"));

        assertTrue(JeiIngredientBridge.isBrowseableIngredientClassNameForAmi(
                "mekanism.api.chemical.ChemicalStack"));
    }
}
