package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

public class AmiButtonWidget extends AbstractWidget {

    private static final ResourceLocation SPRITE_NORMAL = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation SPRITE_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    private static final int COLOR_ACTIVE = AMITheme.BUTTON_ACTIVE; // amber — panel is open
    private static final int COLOR_HOVER = AMITheme.BUTTON_HOVER;  // vanilla button hover tint

    private final Runnable onClickCallback;
    private final Runnable onAltClickCallback;
    private final BooleanSupplier isPanelVisible;
    private boolean isDown = false;

    public AmiButtonWidget(Runnable onClick, Runnable onAltClick, BooleanSupplier isPanelVisible) {
        super(2, 0, 22, 20, Component.empty());
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

        // Active (open) or hovered → highlighted sprite; otherwise normal
        ResourceLocation sprite = (panelOpen || hovered) ? SPRITE_HIGHLIGHTED : SPRITE_NORMAL;
        g.blitSprite(sprite, getX(), getY(), width, height);

        // Label: amber when panel is open, hover-yellow when hovered, white otherwise
        int textColor = panelOpen ? COLOR_ACTIVE : (hovered ? COLOR_HOVER : AMITheme.WHITE);
        var font = Minecraft.getInstance().font;
        // 1 px down-shift when button is held for tactile feel
        int textY = getY() + (height - font.lineHeight) / 2 + (isDown ? 2 : 1);
        g.drawCenteredString(font, Component.translatable("ami.gui.ami_button"), getX() + width / 2, textY, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            isDown = true;
            boolean altDown = net.minecraft.client.gui.screens.Screen.hasAltDown();
            com.sanhiruzu.ami.AMI.LOGGER.info("AMI Button Clicked! Alt down: {}", altDown);
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
