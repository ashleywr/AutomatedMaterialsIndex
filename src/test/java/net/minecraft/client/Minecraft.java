package net.minecraft.client;

public class Minecraft {
    public static final Minecraft INSTANCE = new Minecraft();

    public static Minecraft getInstance() {
        return INSTANCE;
    }

    public final net.minecraft.client.gui.Font font = new net.minecraft.client.gui.Font();
}
