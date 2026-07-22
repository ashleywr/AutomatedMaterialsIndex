package com.sanhiruzu.ami.client.widget;

import net.minecraft.client.gui.GuiGraphics;

public interface AmiDropdownPopup {
    boolean isOpen();

    void close();

    void renderDropdownList(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    boolean handlePopupClick(double mouseX, double mouseY, int button);

    boolean isMouseOverPopup(double mouseX, double mouseY);
}
