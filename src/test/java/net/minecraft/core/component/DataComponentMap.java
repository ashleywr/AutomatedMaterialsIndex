package net.minecraft.core.component;

import java.util.Set;

public interface DataComponentMap {
    boolean has(DataComponentType<?> component);

    final class Impl implements DataComponentMap {
        private final Set<Object> components;

        public Impl(Set<Object> components) {
            this.components = components;
        }

        @Override
        public boolean has(DataComponentType<?> component) {
            return components.contains(component);
        }
    }
}
