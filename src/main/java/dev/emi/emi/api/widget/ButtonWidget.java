package dev.emi.emi.api.widget;

import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;

public class ButtonWidget extends Widget {
    private final int x, y, width, height;

    @FunctionalInterface
    public interface ClickAction { void click(); }

    public ButtonWidget(int x, int y, int width, int height, int u, int v,
                        BooleanSupplier isActive, ClickAction action) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public ButtonWidget(int x, int y, int width, int height, int u, int v,
                        ResourceLocation texture, BooleanSupplier isActive, ClickAction action) {
        this(x, y, width, height, u, v, isActive, action);
    }

    @Override
    public Bounds getBounds() { return new Bounds(x, y, width, height); }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {}
}
