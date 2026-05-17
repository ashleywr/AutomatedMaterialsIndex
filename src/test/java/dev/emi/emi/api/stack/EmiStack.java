package dev.emi.emi.api.stack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

public interface EmiStack extends EmiIngredient {
    static EmiStack of(ItemStack stack) { return null; }
    ResourceLocation getId();
}
