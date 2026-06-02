package net.minecraft.client;

public class Minecraft {
    public static final Minecraft INSTANCE = new Minecraft();
    public final net.minecraft.client.gui.Font font = new net.minecraft.client.gui.Font();
    public net.minecraft.client.gui.screens.Screen screen;

    public static Minecraft getInstance() {
        return INSTANCE;
    }

    public void setScreen(net.minecraft.client.gui.screens.Screen screen) {
        this.screen = screen;
    }
}
