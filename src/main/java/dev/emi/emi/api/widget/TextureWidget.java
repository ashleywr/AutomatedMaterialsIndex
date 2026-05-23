package dev.emi.emi.api.widget;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;

public class TextureWidget extends Widget {
    private final int x, y, width, height;

    public TextureWidget(ResourceLocation texture, int x, int y, int width, int height, int u, int v) {
        this.x = x; this.y = y;
        this.width = width; this.height = height;
    }

    @Override
    public Bounds getBounds() { return new Bounds(x, y, width, height); }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {}
}
