package com.sanhiruzu.ami.client.results;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base interface for all dropdown components.
 * Allows generic handling of dropdowns without knowing their specific types.
 */
public interface Dropdown {
    void updatePosition(int x, int y, int width);
    void render(GuiGraphics g, int mouseX, int mouseY);
    boolean mouseClicked(double mouseX, double mouseY, int button);
    void close();
    boolean isOpen();
}
