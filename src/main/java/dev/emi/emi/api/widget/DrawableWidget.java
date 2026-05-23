package dev.emi.emi.api.widget;

import net.minecraft.client.gui.GuiGraphics;

public class DrawableWidget extends Widget {
    private final int x, y, width, height;

    @FunctionalInterface
    public interface DrawableWidgetConsumer {
        void render(GuiGraphics draw, int mouseX, int mouseY, float delta);
    }

    public DrawableWidget(int x, int y, int width, int height, DrawableWidgetConsumer consumer) {
        this.x = x; this.y = y;
        this.width = width; this.height = height;
    }

    @Override
    public Bounds getBounds() { return new Bounds(x, y, width, height); }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {}
}
