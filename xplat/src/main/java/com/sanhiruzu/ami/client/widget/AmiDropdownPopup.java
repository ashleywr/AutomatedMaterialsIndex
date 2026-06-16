package com.sanhiruzu.ami.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface AmiDropdownPopup {
    boolean isOpen();

    void close();

    void renderDropdownList(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    boolean isMouseOverPopup(double mouseX, double mouseY);
}
