package net.minecraft.tags;

import net.minecraft.resources.Identifier;

public final class TagKey<T> {
    private final Identifier location;

    private TagKey(Identifier location) {
        this.location = location;
    }

    public static <T> TagKey<T> create(Object registry, Identifier location) {
        return new TagKey<>(location);
    }

    public Identifier location() {
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
