package dev.emi.emi.api.render;

import net.minecraft.client.gui.GuiGraphics;

public interface EmiRenderable {
    void render(GuiGraphics draw, int x, int y, float delta);
}
