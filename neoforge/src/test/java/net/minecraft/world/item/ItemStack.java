package net.minecraft.world.item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;

import java.util.Objects;
import java.util.stream.Stream;

public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(null);
    private final Item item;
    private final Object componentSignature;
    private final String hoverName;
    private int count = 1;

    public ItemStack(Item item) {
        this(item, null, null);
    }

    private ItemStack(Item item, Object componentSignature, String hoverName) {
        this.item = item;
        this.componentSignature = componentSignature;
        this.hoverName = hoverName;
    }

    public static boolean isSameItemSameComponents(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.item == b.item && Objects.equals(a.componentSignature, b.componentSignature);
    }

    public ItemStack withComponentSignature(Object componentSignature) {
        return new ItemStack(item, componentSignature, hoverName);
    }

    public ItemStack withHoverName(String hoverName) {
        return new ItemStack(item, componentSignature, hoverName);
    }

    public boolean isEmpty() {
        return this == EMPTY || item == null;
    }

    public Item getItem() {
        return item;
    }

    public ItemStack copy() {
        ItemStack copy = new ItemStack(item, componentSignature, hoverName);
        copy.count = count;
        return copy;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean has(DataComponentType<?> component) {
        return item != null && item.components().has(component);
    }

    public <T> T get(DataComponentType<T> component) {
        return item != null ? item.components().get(component) : null;
    }

    public boolean is(TagKey<Item> tag) {
        return item != null && item.builtInRegistryHolder().is(tag);
    }

    public boolean is(Item expected) {
        return item == expected;
    }

    public Stream<TagKey<Item>> getTags() {
        return item == null ? Stream.empty() : item.builtInRegistryHolder().tags();
    }

    public ItemUseAnimation getUseAnimation() {
        return item == null ? ItemUseAnimation.NONE : item.getUseAnimation(this);
    }

    public boolean is(Object item) {
        if (item instanceof Item i) return is(i);
        if (item instanceof net.minecraft.tags.TagKey<?> tag) return is((net.minecraft.tags.TagKey<Item>) tag);
        return false;
    }

    public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot() {
        return item instanceof Equipable equipable ? equipable.getEquipmentSlot() : null;
    }

    public Component getHoverName() {
        return Component.literal(hoverName != null ? hoverName : item != null ? item.toString() : "empty");
    }
}
