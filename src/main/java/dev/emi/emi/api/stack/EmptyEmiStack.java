package dev.emi.emi.api.stack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

class EmptyEmiStack extends EmiStack {
    @Override
    public EmiStack copy() { return this; }
    @Override
    public boolean isEmpty() { return true; }
    @Override
    public Object getKey() { return Items.AIR; }
    @Override
    public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("minecraft", "air"); }
    @Override
    public ItemStack getItemStack() { return ItemStack.EMPTY; }
    @Override
    public Component getName() { return Component.empty(); }
}
