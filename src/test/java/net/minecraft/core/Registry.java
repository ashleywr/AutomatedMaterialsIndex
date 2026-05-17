package net.minecraft.core;

import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

public interface Registry<T> {
    ResourceLocation getKey(T value);
    Optional<T> getOptional(ResourceLocation id);
}
