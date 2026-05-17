package net.minecraft.core.registries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.DefaultedRegistry;
import java.util.Optional;

public class BuiltInRegistries {
    public static final DefaultedRegistry<Item> ITEM = new DefaultedRegistry<Item>() {
        @Override
        public ResourceLocation getKey(Item item) {
            return ResourceLocation.parse("minecraft:item");
        }
        @Override
        public Optional<Item> getOptional(ResourceLocation id) {
            return Optional.empty();
        }
    };
    public static final net.minecraft.core.Registry<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPE = null;
}
