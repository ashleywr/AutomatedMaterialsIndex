package net.minecraft.client.gui.components;

import net.minecraft.network.chat.Component;

public abstract class AbstractWidget {
    public AbstractWidget(int x, int y, int w, int h, Component c) {
    }

    public int getX() {
        return 0;
    }

    public int getY() {
        return 0;
    }

    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }
}
