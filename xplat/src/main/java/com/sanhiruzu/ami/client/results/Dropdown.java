package com.sanhiruzu.ami.client.results;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Base interface for all dropdown components.
 * Allows generic handling of dropdowns without knowing their specific types.
 */
public interface Dropdown {
    static boolean contains(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }

    void updatePosition(int x, int y, int width);

    /**
     * Renders the button row only (no dropdown list).
     */
    void render(GuiGraphicsExtractor g, int mouseX, int mouseY);

    /**
     * Renders the open dropdown list overlay — call after other content so it appears on top.
     */
    void renderList(GuiGraphicsExtractor g, int mouseX, int mouseY);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    void close();

    boolean isOpen();
}
