package net.minecraft.core;

import net.minecraft.resources.ResourceLocation;

public interface DefaultedRegistry<T> extends Registry<T> {
    T get(ResourceLocation id);

    boolean containsKey(ResourceLocation id);

    int getId(T value);
}
