package com.sanhiruzu.ami.client.widget;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for all configuration entry widgets in the AMI config screen.
 */
public abstract class AmiConfigEntry {
    public abstract void render(GuiGraphics graphics, int x, int y, int width);

    public abstract int getHeight();
}
