package net.minecraft.network.chat;

public class Component {
    protected final String key;

    protected Component(String key) { this.key = key; }

    public static Component translatable(String key) { return new Component(key); }

    public static MutableComponent literal(String s) { return new MutableComponent(s); }

    @Override
    public String toString() { return key; }
}
