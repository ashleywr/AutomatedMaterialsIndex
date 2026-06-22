package com.sanhiruzu.ami.client.tooltip;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Renders a row of heart icons representing entity max health, mirroring the vanilla HUD.
 * Each heart = 2 HP. Caps display at MAX_HEARTS hearts; shows overflow label for higher values.
 */
public final class HeartBarTooltipComponent implements TooltipComponent, ClientTooltipComponent {

    // In 1.20.1 hearts live on the icons texture sheet, not the sprite atlas.
    // UV offsets from Gui.HeartType: container index 0 is u=16; normal full is u=52; normal half is u=61.
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");
    private static final int U_CONTAINER = 16;
    private static final int U_FULL = 52;
    private static final int U_HALF = 61;

    private final HeartBarTooltipLayout layout;

    public HeartBarTooltipComponent(int maxHealth) {
        this.layout = new HeartBarTooltipLayout(maxHealth);
    }

    @Override
    public int getHeight() {
        return HeartBarTooltipLayout.ROW_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        int barW = layout.heartCount() * (HeartBarTooltipLayout.HEART_SIZE + HeartBarTooltipLayout.HEART_GAP);
        if (layout.hasOverflow()) {
            barW += 4 + font.width(overflowLabel());
        }
        return Math.max(barW, font.width(healthLabel()));
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        // Draw empty background hearts first
        for (int i = 0; i < layout.heartCount(); i++) {
            int hx = x + layout.heartXOffset(i);
            g.blit(ICONS, hx, y, U_CONTAINER, 0,
                    HeartBarTooltipLayout.HEART_SIZE, HeartBarTooltipLayout.HEART_SIZE);
        }

        // Draw filled hearts over backgrounds
        for (int i = 0; i < layout.heartCount(); i++) {
            int hx = x + layout.heartXOffset(i);
            g.blit(ICONS, hx, y, layout.isFullHeart(i) ? U_FULL : U_HALF, 0,
                    HeartBarTooltipLayout.HEART_SIZE, HeartBarTooltipLayout.HEART_SIZE);
        }

        // Overflow label when health exceeds MAX_HEARTS * 2 HP
        if (layout.hasOverflow()) {
            int labelX = x + layout.overflowLabelXOffset();
            g.drawString(font, overflowLabel(), labelX, y + 1, AMITheme.HEART_OVERFLOW_COLOR, false);
        }

        // HP value label below hearts
        g.drawString(font, healthLabel(), x,
                y + HeartBarTooltipLayout.HEART_SIZE + HeartBarTooltipLayout.LABEL_TOP_PAD,
                AMITheme.HEART_LABEL_COLOR, false);
    }

    private String overflowLabel() {
        return Component.translatable("ami.tooltip.heart.overflow", layout.overflowHearts()).getString();
    }

    private String healthLabel() {
        return Component.translatable(layout.healthLabelKey(), layout.healthLabelHearts()).getString();
    }
}
