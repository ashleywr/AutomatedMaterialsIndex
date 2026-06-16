package net.minecraft.core;

import net.minecraft.resources.Identifier;

public interface DefaultedRegistry<T> extends Registry<T> {
    T get(Identifier id);

    default T getValue(Identifier id) {
        return get(id);
    }

    boolean containsKey(Identifier id);

    int getId(T value);
}
