package net.minecraft.tags;

import net.minecraft.resources.ResourceLocation;

public final class TagKey<T> {
    private final ResourceLocation location;

    private TagKey(ResourceLocation location) {
        this.location = location;
    }

    public static <T> TagKey<T> create(Object registry, ResourceLocation location) {
        return new TagKey<>(location);
    }

    public ResourceLocation location() {
        return location;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TagKey<?> other && location.equals(other.location);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }
}
