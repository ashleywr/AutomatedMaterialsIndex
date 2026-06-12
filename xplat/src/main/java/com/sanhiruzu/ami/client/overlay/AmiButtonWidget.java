package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
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
        boolean devMode = AmiConfig.devMode;
        boolean cheatMode = AmiConfig.cheatMode;
        boolean elevatedMode = devMode || cheatMode;

        int baseBgColor = isVanilla ? 0xFFC6C6C6 : 0xFF2D2D2D;
        int borderColor = isVanilla ? 0xFF555555 : 0xFF181818;
        int accentColor = isVanilla ? AMITheme.ACCENT_BLUE : 0xFF00AAFF;
        int modeAccent = devMode ? 0xFFFF5555 : 0xFFFFAA00;

        if (hovered) baseBgColor = isVanilla ? 0xFFD0D0D0 : 0xFF3D3D3D;
        if (isDown) baseBgColor = isVanilla ? 0xFFA0A0A0 : 0xFF1F1F1F;

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
        if (elevatedMode) {
            g.renderOutline(getX() + 1, getY() + 1, width - 2, height - 2, modeAccent);
        }

        if (panelOpen || hovered) {
            int indicatorW = panelOpen ? 18 : 12;
            int indicatorX = getX() + (width - indicatorW) / 2;
            int indicatorY = getY() + height - 3;
            int activeAccent = elevatedMode ? modeAccent : accentColor;
            int color = panelOpen ? (0xFF000000 | activeAccent) : (0x88000000 | (activeAccent & 0xFFFFFF));
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
        g.pose().translate(getX() + width / 2f, textY, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);
        g.pose().scale(scale, scale, 1.0f);
        Component label = Component.translatable("ami.gui.ami_button");
        int labelW = font.width(label);
        g.drawString(font, label, -labelW / 2, 0, textColor, false);
        g.pose().popPose();

        if (elevatedMode) {
            String badge = devMode ? "DEV" : "CHT";
            int badgeW = Math.min(width - 4, font.width(badge) + 4);
            int badgeH = 8;
            int badgeX = getX() + width - badgeW - 1;
            int badgeY = getY() + 1;
            g.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xEE000000 | (modeAccent & 0xFFFFFF));
            g.fill(badgeX, badgeY + badgeH - 1, badgeX + badgeW, badgeY + badgeH, 0xAA000000);
            float badgeScale = 0.55f;
            g.pose().pushPose();
            g.pose().translate(badgeX + 2, badgeY + 1, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);
            g.pose().scale(badgeScale, badgeScale, 1.0f);
            g.drawString(font, badge, 0, 0, 0xFFFFFFFF, false);
            g.pose().popPose();
        }

    }

    public void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return;

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("ami.gui.ami_button.tooltip"));
        tooltip.add(Component.translatable("ami.gui.ami_button.hotkey",
                Services.PLATFORM.keyMappings().toggleViewer().getTranslatedKeyMessage()));
        if (AmiConfig.devMode) {
            tooltip.add(Component.translatable("ami.gui.ami_button.dev_mode"));
            tooltip.add(Component.translatable("ami.gui.ami_button.cheat_actions_enabled"));
        } else if (AmiConfig.cheatMode) {
            tooltip.add(Component.translatable("ami.gui.ami_button.cheat_mode"));
        }
        g.renderTooltip(Minecraft.getInstance().font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
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
