package net.minecraft.world.item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.TagKey;

import java.util.stream.Stream;

public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(null);
    private final Item item;

    public ItemStack(Item item) {
        this.item = item;
    }

    public boolean isEmpty() {
        return this == EMPTY || item == null;
    }

    public Item getItem() {
        return item;
    }

    public ItemStack copy() {
        return new ItemStack(item);
    }

    public boolean has(DataComponentType<?> component) {
        return item != null && item.components().has(component);
    }

    public boolean is(TagKey<Item> tag) {
        return item != null && item.builtInRegistryHolder().is(tag);
    }

    public Stream<TagKey<Item>> getTags() {
        return item == null ? Stream.empty() : item.builtInRegistryHolder().tags();
    }

    public UseAnim getUseAnimation() {
        return item == null ? UseAnim.NONE : item.getUseAnimation(this);
    }

    public static boolean isSameItemSameComponents(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.item == b.item;
    }

    public net.minecraft.network.chat.Component getHoverName() {
        return net.minecraft.network.chat.Component.literal(item != null ? item.toString() : "empty");
    }
}
