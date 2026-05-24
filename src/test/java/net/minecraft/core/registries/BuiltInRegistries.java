package net.minecraft.core.registries;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
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

    public static final class TestItemRegistry implements DefaultedRegistry<Item> {
        private final Map<Item, ResourceLocation> ids = new LinkedHashMap<>();
        private final Map<ResourceLocation, Item> itemsById = new LinkedHashMap<>();

        public void register(ResourceLocation id, Item item) {
            ids.put(item, id);
            itemsById.put(id, item);
        }

        @Override
        public ResourceLocation getKey(Item item) {
            return ids.getOrDefault(item, ResourceLocation.parse("minecraft:item"));
        }

        @Override
        public Optional<Item> getOptional(ResourceLocation id) {
            return Optional.ofNullable(itemsById.get(id));
        }

        public Stream<TestNamedTag<Item>> getTags() {
            return Stream.empty();
        }
    }

    public static TestItemRegistry itemRegistry() {
        return (TestItemRegistry) ITEM;
    }

    public record TestNamedTag<T>(TagKey<T> first, Set<Holder.Reference<T>> values) {
        public TagKey<T> getFirst() {
            return first;
        }
    }
}
