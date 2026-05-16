package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Renders a 16×16 item icon followed by a label string on a single row.
 * Generic enough to represent any icon+value stat: attack damage, DPS, armor, etc.
 * Implements both TooltipComponent and ClientTooltipComponent for identity-factory registration.
 */
public final class StatIconRowTooltipComponent implements TooltipComponent, ClientTooltipComponent {

    private static final int ICON_SIZE  = 16;
    private static final int ICON_GAP   = 4;
    private static final int TOP_PAD    = 3; // gap above row, visually separates from heart bar

    private final ItemStack icon;
    private final String label;
    private final int labelColor;

    public StatIconRowTooltipComponent(ItemStack icon, String label, int labelColor) {
        this.icon       = icon;
        this.label      = label;
        this.labelColor = labelColor;
    }

    public StatIconRowTooltipComponent(ItemStack icon, String label) {
        this(icon, label, 0xFFFFFFFF);
    }

    @Override
    public int getHeight() {
        return TOP_PAD + ICON_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        return ICON_SIZE + ICON_GAP + font.width(label);
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        int iconY = y + TOP_PAD;

        g.pose().pushPose();
        g.pose().translate(0, 0, 150); // prevent z-clipping on 3D item models
        g.renderItem(icon, x, iconY);
        g.pose().popPose();

        int textX = x + ICON_SIZE + ICON_GAP;
        int textY = iconY + (ICON_SIZE - font.lineHeight) / 2;
        g.drawString(font, label, textX, textY, labelColor, false);
    }
}
