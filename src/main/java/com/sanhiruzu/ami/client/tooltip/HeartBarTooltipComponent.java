package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Renders a row of heart icons representing entity max health, mirroring the vanilla HUD.
 * Each heart = 2 HP. Caps display at MAX_HEARTS hearts; shows overflow label for higher values.
 * Implements both TooltipComponent (marker) and ClientTooltipComponent (renderer) so it can be
 * passed directly as the Optional<TooltipComponent> image argument after identity-factory registration.
 */
public final class HeartBarTooltipComponent implements TooltipComponent, ClientTooltipComponent {

    private static final ResourceLocation ICONS = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");

    private static final int MAX_HEARTS   = 10;
    private static final int HEART_SIZE   = 9;
    private static final int HEART_GAP    = 1;
    private static final int ROW_HEIGHT   = HEART_SIZE + 4;

    // icons.png UVs (64×256 sheet)
    private static final int HEART_BG_U  = 16; // empty heart background
    private static final int HEART_BG_V  = 0;
    private static final int HEART_U     = 52; // full red heart
    private static final int HEART_V     = 0;
    private static final int HALF_U      = 61; // half red heart
    private static final int HALF_V      = 0;

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
        int shown  = Math.min(MAX_HEARTS * 2, halves);
        int heartCount = (int) Math.ceil(shown / 2.0);

        // Draw empty background hearts first
        for (int i = 0; i < heartCount; i++) {
            int hx = x + i * (HEART_SIZE + HEART_GAP);
            g.blit(ICONS, hx, y, HEART_BG_U, HEART_BG_V, HEART_SIZE, HEART_SIZE, 256, 256);
        }

        // Draw filled hearts over backgrounds
        for (int i = 0; i < heartCount; i++) {
            int hx = x + i * (HEART_SIZE + HEART_GAP);
            boolean isFull = (shown - i * 2) >= 2;
            if (isFull) {
                g.blit(ICONS, hx, y, HEART_U, HEART_V, HEART_SIZE, HEART_SIZE, 256, 256);
            } else {
                g.blit(ICONS, hx, y, HALF_U, HALF_V, HEART_SIZE, HEART_SIZE, 256, 256);
            }
        }

        // Overflow label when health exceeds MAX_HEARTS * 2 HP
        if (maxHealth > MAX_HEARTS * 2) {
            int labelX = x + MAX_HEARTS * (HEART_SIZE + HEART_GAP) + 2;
            g.drawString(font, overflowLabel(), labelX, y + 1, 0xFFCC3333, false);
        }

        // HP value label below hearts
        g.drawString(font, healthLabel(), x, y + HEART_SIZE + 1, 0xFFAAAAAA, false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int halfHearts() {
        return maxHealth; // maxHealth is already raw HP; 1 HP = 0.5 heart
    }

    private String overflowLabel() {
        return "+" + (maxHealth / 2 - MAX_HEARTS) + " ♥";
    }

    private String healthLabel() {
        // Display as whole hearts if even, half-heart if odd
        if (maxHealth % 2 == 0) {
            return maxHealth / 2 + " ♥";
        } else {
            return (maxHealth / 2) + "½ ♥";
        }
    }
}
