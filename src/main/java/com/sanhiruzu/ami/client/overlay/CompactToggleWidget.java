package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.AMIConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Toggle button placed next to the AMI open/close button.
 * Switches between full panel mode and compact grid-only mode (like EMI/JEI).
 * State persists via AMIConfig.COMPACT_MODE.
 */
public class CompactToggleWidget extends AbstractWidget {

    private static final ResourceLocation SPRITE_NORMAL     = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation SPRITE_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    // Shown in the button — compact=off shows grid icon, compact=on shows list icon
    private static final String LABEL_TO_COMPACT = "⊡"; // ⊡  click to go compact
    private static final String LABEL_TO_FULL    = "≡"; // ≡  click to go full

    private static final int COLOR_COMPACT_ACTIVE = 0xFFAADDFF; // blue tint — compact mode is on
    private static final int COLOR_NORMAL         = 0xFFFFFFFF;
    private static final int COLOR_HOVER          = 0xFFFFFFA0;

    private boolean isDown = false;

    public CompactToggleWidget() {
        super(0, 0, 22, 20, Component.empty());
    }

    public void updateBounds(WidgetBounds bounds) {
        setX(bounds.x());
        setY(bounds.y());
        this.width  = bounds.width();
        this.height = bounds.height();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean compact = AMIConfig.COMPACT_MODE.get();
        boolean hovered = isMouseOver(mouseX, mouseY);

        // Compact-active or hovered → highlighted; otherwise normal
        ResourceLocation sprite = (compact || hovered) ? SPRITE_HIGHLIGHTED : SPRITE_NORMAL;
        g.blitSprite(sprite, getX(), getY(), width, height);

        // Label communicates current mode (what clicking will switch away from)
        String label = compact ? LABEL_TO_FULL : LABEL_TO_COMPACT;
        int textColor = compact ? COLOR_COMPACT_ACTIVE : (hovered ? COLOR_HOVER : COLOR_NORMAL);

        var font = Minecraft.getInstance().font;
        int textY = getY() + (height - font.lineHeight) / 2 + (isDown ? 2 : 1);
        g.drawCenteredString(font, label, getX() + width / 2, textY, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            isDown = true;
            AMIConfig.COMPACT_MODE.set(!AMIConfig.COMPACT_MODE.get());
            AMIConfig.SPEC.save();
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
