package dev.emi.emi.api.widget;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public abstract class Widget {
    public abstract Bounds getBounds();
    public abstract void render(GuiGraphics draw, int mouseX, int mouseY, float delta);

    public List<TooltipComponent> getTooltip(int mouseX, int mouseY) { return List.of(); }
    public boolean mouseClicked(int mouseX, int mouseY, int button) { return false; }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
}
