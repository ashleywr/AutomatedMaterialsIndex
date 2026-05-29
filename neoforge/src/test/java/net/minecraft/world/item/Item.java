package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

public class Item {
    private final String name;
    private final Set<Object> components = new LinkedHashSet<>();
    private final Set<TagKey<Item>> tags = new LinkedHashSet<>();
    private final Holder.Reference<Item> holder = new TestHolder(this.tags);

    public Item() {
        this("item");
    }

    public Item(String name) {
        this.name = name;
    }

    public DataComponentMap components() {
        return new DataComponentMap.Impl(components);
    }

    public Item withComponent(Object component) {
        components.add(component);
        return this;
    }

    public Item withTag(TagKey<Item> tag) {
        tags.add(tag);
        return this;
    }

    @Override
    public String toString() {
        return name;
    }

    public net.minecraft.core.Holder.Reference<Item> builtInRegistryHolder() {
        return holder;
    }

    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return Component.literal(name);
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.FOOD) ? UseAnim.EAT : UseAnim.NONE;
    }

    private static final class TestHolder extends Holder.Reference<Item> {
        private final Set<TagKey<Item>> tagSet;

        private TestHolder(Set<TagKey<Item>> tagSet) {
            this.tagSet = tagSet;
        }

        @Override
        public Stream<TagKey<Item>> tags() {
            return tagSet.stream();
        }

        @Override
        public boolean is(TagKey<Item> tag) {
            return tagSet.contains(tag);
        }
    }
}
