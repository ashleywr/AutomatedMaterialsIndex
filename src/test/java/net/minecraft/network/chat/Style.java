package net.minecraft.network.chat;

public final class Style {
    public static final Style EMPTY = new Style();
    private Style() {}
    public Style withColor(int color) { return this; }
}
