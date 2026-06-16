package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Renders a 16×16 item icon followed by a label string on a single row.
 * Generic enough to represent any icon+value stat: attack damage, DPS, armor, etc.
 * Implements both TooltipComponent and ClientTooltipComponent for identity-factory registration.
 */
public final class StatIconRowTooltipComponent implements TooltipComponent, ClientTooltipComponent {

    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int TOP_PAD = 3; // gap above row, visually separates from heart bar

    private final ItemStack icon;
    private final String label;
    private final int labelColor;

    public StatIconRowTooltipComponent(ItemStack icon, String label, int labelColor) {
        this.icon = icon;
        this.label = label;
        this.labelColor = labelColor;
    }

    public StatIconRowTooltipComponent(ItemStack icon, String label) {
        this(icon, label, 0xFFFFFFFF);
    }

    @Override
    public int getHeight(Font font) {
        return TOP_PAD + ICON_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        return ICON_SIZE + ICON_GAP + font.width(label);
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor g) {
        int iconY = y + TOP_PAD;

        g.item(icon, x, iconY);

        int textX = x + ICON_SIZE + ICON_GAP;
        int textY = iconY + (ICON_SIZE - font.lineHeight) / 2;
        g.text(font, label, textX, textY, labelColor, false);
    }
}
