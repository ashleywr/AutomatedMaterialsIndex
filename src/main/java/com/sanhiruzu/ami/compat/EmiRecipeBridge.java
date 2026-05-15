package com.sanhiruzu.ami.compat;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;

/**
 * Direct EMI API calls — only referenced behind a ModList.isLoaded("emi") guard
 * so this class is never loaded when EMI is absent.
 */
class EmiRecipeBridge {
    static void openRecipes(ItemStack stack) {
        EmiApi.displayRecipes(EmiStack.of(stack));
    }

    static void openUses(ItemStack stack) {
        EmiApi.displayUses(EmiStack.of(stack));
    }
}
