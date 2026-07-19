package com.sanhiruzu.ami.compat;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct EMI API calls — only referenced behind a Services.PLATFORM.isModLoaded("emi") guard
 * so this class is never loaded when EMI is absent.
 */
public class EmiRecipeViewerItemAuditBridge {
    public static List<ItemStack> indexStacks() {
        return toItemStacks(EmiApi.getIndexStacks());
    }

    public static List<ItemStack> filteredStacks() {
        return toItemStacks(EmiStackList.filteredStacks);
    }

    private static List<ItemStack> toItemStacks(List<EmiStack> emiStacks) {
        List<ItemStack> stacks = new ArrayList<>();
        if (emiStacks == null) {
            return stacks;
        }
        for (EmiStack emiStack : emiStacks) {
            ItemStack stack = emiStack.getItemStack();
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }
}
