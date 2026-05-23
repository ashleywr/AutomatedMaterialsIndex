package dev.emi.emi.api.widget;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

public class TextWidget extends Widget {
    private final int x, y;

    public TextWidget(Component text, int x, int y, int color, boolean shadow) {
        this.x = x; this.y = y;
    }

    @Override
    public Bounds getBounds() { return new Bounds(x, y, 0, 9); }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {}
}
