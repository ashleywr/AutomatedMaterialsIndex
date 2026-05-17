package net.minecraft.world.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(null);
    private final Item item;

    public ItemStack(Item item) { this.item = item; }
    public boolean isEmpty() { return this == EMPTY || item == null; }
    public Item getItem() { return item; }
    public ItemStack copy() { return new ItemStack(item); }

    public static boolean isSameItemSameComponents(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.item == b.item;
    }

    public net.minecraft.network.chat.Component getHoverName() {
        return net.minecraft.network.chat.Component.literal(item != null ? item.toString() : "empty");
    }
}
