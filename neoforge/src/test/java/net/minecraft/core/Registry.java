package net.minecraft.core;

import net.minecraft.resources.Identifier;

import java.util.Optional;

public interface Registry<T> {
    Identifier getKey(T value);

    Optional<T> getOptional(Identifier id);
}
