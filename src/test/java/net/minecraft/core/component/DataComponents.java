package net.minecraft.core.component;

public final class DataComponents {
    public static final DataComponentType<Object> FOOD = new DataComponentType<>();
    public static final DataComponentType<Object> POTION_CONTENTS = new DataComponentType<>();
    public static final DataComponentType<Object> ENCHANTMENTS = new DataComponentType<>();

    private DataComponents() {
    }
}
