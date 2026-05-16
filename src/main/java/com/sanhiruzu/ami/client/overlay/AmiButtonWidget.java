package com.sanhiruzu.ami.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class AmiButtonWidget extends AbstractWidget {
    private final Runnable onClickCallback;
    private final BooleanSupplier isPanelVisible;

    public AmiButtonWidget(Runnable onClick, BooleanSupplier isPanelVisible) {
        super(2, 0, 22, 14, Component.empty());
        this.onClickCallback = onClick;
        this.isPanelVisible = isPanelVisible;
    }

    public void updateBounds(WidgetBounds bounds) {
        setX(bounds.x());
        setY(bounds.y());
        this.width = bounds.width();
        this.height = bounds.height();
    }

    public WidgetBounds getBounds() {
        return new WidgetBounds(getX(), getY(), width, height);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;

        boolean panelVisible = isPanelVisible.getAsBoolean();
        boolean hovered = isMouseOver(mouseX, mouseY);

        int bg     = panelVisible ? 0xFF0A0A0A : 0xFF181818;
        int border = hovered ? (panelVisible ? 0xFFFFAA00 : 0xFF888888) : (panelVisible ? 0xFF555555 : 0xFF333333);
        int text   = hovered ? (panelVisible ? 0xFFFFDD44 : 0xFFAAAAAA) : (panelVisible ? 0xFFFFAA00 : 0xFF666666);

        int x = getX(), y = getY(), w = width, h = height;

        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);

        int labelW = font.width("AMI");
        g.drawString(font, "AMI", x + (w - labelW) / 2, y + (h - font.lineHeight) / 2 + 1, text, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            if (onClickCallback != null) onClickCallback.run();
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
