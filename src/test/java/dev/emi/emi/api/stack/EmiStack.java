package dev.emi.emi.api.stack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface EmiStack extends EmiIngredient {
    static EmiStack of(ItemStack stack) {
        return null;
    }

    ResourceLocation getId();
}
