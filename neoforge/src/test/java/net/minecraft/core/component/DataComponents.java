package net.minecraft.core.component;

public final class DataComponents {
    public static final DataComponentType<Object> CUSTOM_DATA = new DataComponentType<>();
    public static final DataComponentType<Object> MAX_STACK_SIZE = new DataComponentType<>();
    public static final DataComponentType<Object> MAX_DAMAGE = new DataComponentType<>();
    public static final DataComponentType<Object> DAMAGE = new DataComponentType<>();
    public static final DataComponentType<Object> TOOL = new DataComponentType<>();
    public static final DataComponentType<Object> WEAPON = new DataComponentType<>();
    public static final DataComponentType<Object> EQUIPPABLE = new DataComponentType<>();
    public static final DataComponentType<Object> FOOD = new DataComponentType<>();
    public static final DataComponentType<Object> POTION_CONTENTS = new DataComponentType<>();
    public static final DataComponentType<Object> ENCHANTMENTS = new DataComponentType<>();
    public static final DataComponentType<Object> CONTAINER = new DataComponentType<>();
    public static final DataComponentType<Object> BUNDLE_CONTENTS = new DataComponentType<>();
    public static final DataComponentType<Object> ENTITY_DATA = new DataComponentType<>();
    public static final DataComponentType<Object> BUCKET_ENTITY_DATA = new DataComponentType<>();
    public static final DataComponentType<Object> BLOCK_ENTITY_DATA = new DataComponentType<>();

    private DataComponents() {
    }
}
