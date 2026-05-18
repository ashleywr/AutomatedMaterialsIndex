package net.minecraft.core;

import java.util.stream.Stream;
import net.minecraft.tags.TagKey;

public interface Holder<T> {
    abstract class Reference<T> implements Holder<T> {
        public abstract Stream<TagKey<T>> tags();
        public abstract boolean is(TagKey<T> tag);
    }
}
