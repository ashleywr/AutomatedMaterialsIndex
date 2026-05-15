package com.sanhiruzu.ami.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class AmiButtonWidget implements AmiWidget {
    private static final int WIDTH = 22;
    private static final int HEIGHT = 14;
    private static final int PADDING = 2;

    private WidgetBounds bounds = new WidgetBounds(2, 0, WIDTH, HEIGHT);
    private final Runnable onOpen;

    public AmiButtonWidget(Runnable onOpen) {
        this.onOpen = onOpen;
    }

    public void updateBounds(WidgetBounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;

        boolean btnHovered = isMouseOver(mouseX, mouseY);
        int btnBorder = btnHovered ? 0xFFFFAA00 : 0xFF555555;
        int btnTextColor = btnHovered ? 0xFFFFDD44 : 0xFFFFAA00;

        int x = bounds.x();
        int y = bounds.y();
        int w = bounds.width();
        int h = bounds.height();

        g.fill(x, y, x + w, y + h, 0xFF0A0A0A);
        g.fill(x, y, x + w, y + 1, btnBorder);
        g.fill(x, y + h - 1, x + w, y + h, btnBorder);
        g.fill(x, y, x + 1, y + h, btnBorder);
        g.fill(x + w - 1, y, x + w, y + h, btnBorder);

        int labelW = font.width("AMI");
        g.drawString(font, "AMI", x + (w - labelW) / 2, y + (h - font.lineHeight) / 2 + 1, btnTextColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            if (onOpen != null) onOpen.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return false;
    }

    @Override
    public WidgetBounds getBounds() {
        return bounds;
    }
}
