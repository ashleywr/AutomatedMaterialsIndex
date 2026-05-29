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
 * Updated for 1.21.1 to use sprites instead of a texture sheet.
 */
public final class HeartBarTooltipComponent implements TooltipComponent, ClientTooltipComponent {

    // In 1.20.1 hearts live on the icons texture sheet, not the sprite atlas.
    // UV offsets from Gui.HeartType: CONTAINER index=0 → u=16, NORMAL index=2 → full u=52, half u=61.
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final int U_CONTAINER = 16;
    private static final int U_FULL      = 52;
    private static final int U_HALF      = 61;

    private static final int MAX_HEARTS = 10;
    private static final int HEART_SIZE = 9;
    private static final int HEART_GAP = 1;
    private static final int TEXT_LINE_HEIGHT = 10;
    private static final int LABEL_TOP_PAD = 1;
    private static final int ROW_HEIGHT = HEART_SIZE + LABEL_TOP_PAD + TEXT_LINE_HEIGHT;

    private final int maxHealth; // raw HP value (e.g. 20 = 10 hearts)

    public HeartBarTooltipComponent(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    @Override
    public int getHeight() {
        return ROW_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        int displayHearts = Math.min(MAX_HEARTS, halfHearts());
        int barW = displayHearts * (HEART_SIZE + HEART_GAP);
        if (maxHealth > MAX_HEARTS * 2) {
            barW += 4 + font.width(overflowLabel());
        }
        return Math.max(barW, font.width(healthLabel()));
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        int halves = halfHearts();
        int shown = Math.min(MAX_HEARTS * 2, halves);
        int heartCount = (int) Math.ceil(shown / 2.0);

        // Draw empty background hearts first
        for (int i = 0; i < heartCount; i++) {
            int hx = x + i * (HEART_SIZE + HEART_GAP);
            g.blit(ICONS, hx, y, U_CONTAINER, 0, HEART_SIZE, HEART_SIZE);
        }

        // Draw filled hearts over backgrounds
        for (int i = 0; i < heartCount; i++) {
            int hx = x + i * (HEART_SIZE + HEART_GAP);
            boolean isFull = (shown - i * 2) >= 2;
            g.blit(ICONS, hx, y, isFull ? U_FULL : U_HALF, 0, HEART_SIZE, HEART_SIZE);
        }

        // Overflow label when health exceeds MAX_HEARTS * 2 HP
        if (maxHealth > MAX_HEARTS * 2) {
            int labelX = x + MAX_HEARTS * (HEART_SIZE + HEART_GAP) + 2;
            g.drawString(font, overflowLabel(), labelX, y + 1, AMITheme.HEART_OVERFLOW_COLOR, false);
        }

        // HP value label below hearts
        g.drawString(font, healthLabel(), x, y + HEART_SIZE + LABEL_TOP_PAD, AMITheme.HEART_LABEL_COLOR, false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int halfHearts() {
        return maxHealth; // maxHealth is already raw HP; 1 HP = 0.5 heart
    }

    private String overflowLabel() {
        return Component.translatable("ami.tooltip.heart.overflow", maxHealth / 2 - MAX_HEARTS).getString();
    }

    private String healthLabel() {
        // Display as whole hearts if even, half-heart if odd
        if (maxHealth % 2 == 0) {
            return Component.translatable("ami.tooltip.heart.even", maxHealth / 2).getString();
        } else {
            return Component.translatable("ami.tooltip.heart.odd", maxHealth / 2).getString();
        }
    }
}
