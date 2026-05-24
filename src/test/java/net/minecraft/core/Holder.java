package net.minecraft.core;

import net.minecraft.tags.TagKey;

import java.util.stream.Stream;

public interface Holder<T> {
    abstract class Reference<T> implements Holder<T> {
        public abstract Stream<TagKey<T>> tags();

        public abstract boolean is(TagKey<T> tag);
    }
}
