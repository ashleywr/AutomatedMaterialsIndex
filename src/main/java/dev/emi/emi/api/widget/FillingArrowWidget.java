package dev.emi.emi.api.widget;

import net.minecraft.client.gui.GuiGraphics;

public class FillingArrowWidget extends Widget {
    private final int x, y;

    public FillingArrowWidget(int x, int y, int time) {
        this.x = x; this.y = y;
    }

    @Override
    public Bounds getBounds() { return new Bounds(x, y, 24, 17); }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {}
}
