package net.minecraft.world.item;

public class Item {
    @Override
    public String toString() { return "item"; }
    public net.minecraft.core.Holder.Reference<Item> builtInRegistryHolder() { return null; }
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return net.minecraft.network.chat.Component.literal("item");
    }
}
