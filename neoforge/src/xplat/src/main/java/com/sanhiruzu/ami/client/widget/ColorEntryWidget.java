package com.sanhiruzu.ami.client.widget;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A widget that displays a color preview box.
 */
public class ColorEntryWidget extends AbstractWidget {
    private int color;

    public ColorEntryWidget(int x, int y, int width, int height, int color) {
        super(x, y, width, height, Component.empty());
        this.color = color;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Draw the border
        graphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, AMITheme.CONFIG_SWATCH_BORDER);
        // Draw the color preview
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000 | color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // Implementation for accessibility
    }

    public void setColor(int color) {
        this.color = color;
    }
}
