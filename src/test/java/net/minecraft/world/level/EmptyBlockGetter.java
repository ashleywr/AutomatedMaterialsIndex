package net.minecraft.world.level;

public final class EmptyBlockGetter implements BlockGetter {
    public static final EmptyBlockGetter INSTANCE = new EmptyBlockGetter();

    private EmptyBlockGetter() {
    }
}
