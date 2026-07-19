package com.sanhiruzu.ami.compat;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientVisibility;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Direct JEI API calls — only referenced behind a Services.PLATFORM.isModLoaded("jei") guard
 * so this class is never loaded when JEI is absent.
 */
public class JeiRecipeViewerItemAuditBridge {
    public static List<ItemStack> allItemStacks() {
        return JeiRuntimeAccessor.withRuntime(
                runtime -> new ArrayList<>(runtime.getIngredientManager().getAllItemStacks()),
                List.of());
    }

    public static List<ItemStack> visibleItemStacks() {
        return JeiRuntimeAccessor.withRuntime(JeiRecipeViewerItemAuditBridge::collectVisible, List.of());
    }

    private static List<ItemStack> collectVisible(IJeiRuntime runtime) {
        Collection<ItemStack> allStacks = runtime.getIngredientManager().getAllItemStacks();
        IIngredientVisibility visibility = runtime.getJeiHelpers().getIngredientVisibility();
        List<ItemStack> visibleStacks = new ArrayList<>();
        for (ItemStack stack : allStacks) {
            try {
                if (visibility.isIngredientVisible(VanillaTypes.ITEM_STACK, stack)) {
                    visibleStacks.add(stack);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return visibleStacks;
    }
}
