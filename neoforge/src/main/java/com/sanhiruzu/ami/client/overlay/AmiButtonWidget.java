package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.neoforge.client.AMIKeyMappings;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class AmiButtonWidget extends AbstractWidget {

    private final Runnable onClickCallback;
    private final Runnable onAltClickCallback;
    private final BooleanSupplier isPanelVisible;
    private boolean isDown = false;

    public AmiButtonWidget(Runnable onClick, Runnable onAltClick, BooleanSupplier isPanelVisible) {
        super(2, 0, 32, 22, Component.empty());
        this.onClickCallback = onClick;
        this.onAltClickCallback = onAltClick;
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
        boolean panelOpen = isPanelVisible.getAsBoolean();
        boolean hovered = isMouseOver(mouseX, mouseY);
        boolean isVanilla = AmiConfig.theme == AmiConfig.Theme.VANILLA;

        int baseBgColor = isVanilla ? 0xFFC6C6C6 : 0xFF2D2D2D;
        int borderColor = isVanilla ? 0xFF555555 : 0xFF181818;
        int accentColor = isVanilla ? 0xFF4488FF : 0xFF00AAFF;

        if (hovered) baseBgColor = isVanilla ? 0xFFD0D0D0 : 0xFF3D3D3D;
        if (isDown)  baseBgColor = isVanilla ? 0xFFA0A0A0 : 0xFF1F1F1F;

        AMITheme.fillRounded(g, getX(), getY(), width, height, baseBgColor);

        if (!isDown) {
            int highlightColor = isVanilla ? 0xAAFFFFFF : 0x44FFFFFF;
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + 2, highlightColor);
            g.fill(getX() + 1, getY() + 1, getX() + 2, getY() + height - 1, highlightColor);
            g.fill(getX() + 1, getY() + height - 2, getX() + width - 1, getY() + height - 1, 0x44000000);
            g.fill(getX() + width - 2, getY() + 1, getX() + width - 1, getY() + height - 1, 0x44000000);
        } else {
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + 2, 0x66000000);
            g.fill(getX() + 1, getY() + 1, getX() + 2, getY() + height - 1, 0x66000000);
        }

        g.renderOutline(getX(), getY(), width, height, borderColor);

        if (panelOpen || hovered) {
            int indicatorW = panelOpen ? 18 : 12;
            int indicatorX = getX() + (width - indicatorW) / 2;
            int indicatorY = getY() + height - 3;
            int color = panelOpen ? (0xFF000000 | accentColor) : (0x88000000 | (accentColor & 0xFFFFFF));
            g.fill(indicatorX, indicatorY, indicatorX + indicatorW, indicatorY + 2, color);
        }

        int textColor;
        if (isVanilla) {
            textColor = panelOpen ? 0xFF0000AA : (hovered ? 0xFF000000 : 0xFF404040);
        } else {
            textColor = panelOpen ? 0xFF55FFFF : (hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
        }

        var font = Minecraft.getInstance().font;
        int textY = getY() + (height - font.lineHeight) / 2 + (isDown ? 2 : 1);

        g.pose().pushPose();
        float scale = 1.05f;
        g.pose().translate(getX() + width / 2f, textY, 0);
        g.pose().scale(scale, scale, 1.0f);
        Component label = Component.translatable("ami.gui.ami_button");
        int labelW = font.width(label);
        g.drawString(font, label, -labelW / 2, 0, textColor, false);
        g.pose().popPose();

        if (hovered) {
            Component tooltip = Component.translatable("ami.gui.ami_button.tooltip");
            Component hotkeyHint = Component.translatable("ami.gui.ami_button.hotkey", AMIKeyMappings.TOGGLE_VIEWER.getTranslatedKeyMessage());
            g.renderTooltip(font, java.util.List.of(tooltip, hotkeyHint), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            isDown = true;
            boolean altDown = net.minecraft.client.gui.screens.Screen.hasAltDown();
            if (altDown) {
                if (onAltClickCallback != null) onAltClickCallback.run();
            } else {
                if (onClickCallback != null) onClickCallback.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDown = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
