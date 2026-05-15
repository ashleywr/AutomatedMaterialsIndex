package net.minecraft.network.chat;

public final class Component {
    private final String key;

    private Component(String key) { this.key = key; }

    public static Component translatable(String key) { return new Component(key); }

    @Override
    public String toString() { return key; }
}
