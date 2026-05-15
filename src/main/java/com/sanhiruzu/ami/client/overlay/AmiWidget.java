package com.sanhiruzu.ami.client.overlay;

import net.minecraft.client.gui.GuiGraphics;

public interface AmiWidget {
    void render(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    default void renderOverlay(GuiGraphics g, int mouseX, int mouseY) {}

    boolean mouseClicked(double mouseX, double mouseY, int button);

    boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY);

    boolean mouseReleased(double mouseX, double mouseY, int button);

    boolean mouseScrolled(double mouseX, double mouseY, double delta);

    boolean keyPressed(int keyCode, int scanCode, int modifiers);

    boolean charTyped(char c, int modifiers);

    WidgetBounds getBounds();

    default boolean isMouseOver(double mx, double my) {
        return getBounds().contains(mx, my);
    }
}
