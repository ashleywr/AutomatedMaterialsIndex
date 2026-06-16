package net.minecraft.core.registries;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class BuiltInRegistries {
    public static final DefaultedRegistry<Item> ITEM = new TestItemRegistry();
    public static final net.minecraft.core.Registry<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPE = null;

    public static TestItemRegistry itemRegistry() {
        return (TestItemRegistry) ITEM;
    }

    public static final class TestItemRegistry implements DefaultedRegistry<Item> {
        private final Map<Item, Identifier> ids = new LinkedHashMap<>();
        private final Map<Identifier, Item> itemsById = new LinkedHashMap<>();

        public void register(Identifier id, Item item) {
            ids.put(item, id);
            itemsById.put(id, item);
        }

        @Override
        public Identifier getKey(Item item) {
            return ids.getOrDefault(item, new Identifier("minecraft:item"));
        }

        @Override
        public Optional<Item> getOptional(Identifier id) {
            return Optional.ofNullable(itemsById.get(id));
        }

        @Override
        public Item get(Identifier id) {
            return itemsById.getOrDefault(id, net.minecraft.world.item.Items.AIR);
        }

        @Override
        public boolean containsKey(Identifier id) {
            return itemsById.containsKey(id);
        }

        @Override
        public int getId(Item value) {
            int i = 0;
            for (Item item : ids.keySet()) {
                if (item == value) return i;
                i++;
            }
            return -1;
        }

        public Stream<TestNamedTag<Item>> getTags() {
            return Stream.empty();
        }
    }

    public record TestNamedTag<T>(TagKey<T> first, Set<Holder.Reference<T>> values) {
        public TagKey<T> getFirst() {
            return first;
        }
    }
}
