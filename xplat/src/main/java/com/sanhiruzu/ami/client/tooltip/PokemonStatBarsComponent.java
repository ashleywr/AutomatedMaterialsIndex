package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Renders a 6-row base-stat table with colored bars and a BST total line.
 * Register with an identity factory: event.register(class, c -> c).
 *
 * Layout (per row):  [label]  [████░░░░░░]  [value]
 * Colors follow the competitive standard: HP=red ATK=orange DEF=yellow
 *                                         SpA=blue SpD=green SPE=pink
 */
public final class PokemonStatBarsComponent implements TooltipComponent, ClientTooltipComponent {

    private static final int ROW_H     = 9;   // height of each stat row
    private static final int BAR_H     = 4;   // height of the filled bar
    private static final int BAR_MAX_W = 64;  // pixel width at stat = 255
    private static final int MAX_STAT  = 255;
    private static final int TOP_PAD   = 2;
    private static final int LABEL_GAP = 3;   // gap between label col and bar
    private static final int NUM_GAP   = 3;   // gap between bar and number col

    // Competitive stat colors
    private static final int C_HP  = 0xFC636B;
    private static final int C_ATK = 0xF08030;
    private static final int C_DEF = 0xD0B030;
    private static final int C_SPA = 0x5090F0;
    private static final int C_SPD = 0x50C050;
    private static final int C_SPE = 0xF060A8;

    private final int hp, atk, def, spa, spd, spe, bst;

    private record Row(String label, int value, int color) {}

    public PokemonStatBarsComponent(int hp, int atk, int def, int spa, int spd, int spe) {
        this.hp  = hp;  this.atk = atk; this.def = def;
        this.spa = spa; this.spd = spd; this.spe = spe;
        this.bst = hp + atk + def + spa + spd + spe;
    }

    @Override
    public int getHeight() {
        // 6 stat rows + 1 BST divider row + top padding
        return TOP_PAD + 7 * ROW_H;
    }

    @Override
    public int getWidth(Font font) {
        int labelW = labelColumnWidth(font);
        int numW   = numberColumnWidth(font);
        return labelW + LABEL_GAP + BAR_MAX_W + NUM_GAP + numW;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        Row[] rows = {
            new Row("HP",  hp,  C_HP),
            new Row("ATK", atk, C_ATK),
            new Row("DEF", def, C_DEF),
            new Row("SpA", spa, C_SPA),
            new Row("SpD", spd, C_SPD),
            new Row("SPE", spe, C_SPE),
        };

        int labelW = labelColumnWidth(font);
        int numW   = numberColumnWidth(font);
        int barX   = x + labelW + LABEL_GAP;
        int numX   = barX + BAR_MAX_W + NUM_GAP;
        int cy     = y + TOP_PAD;

        for (Row row : rows) {
            int ty   = cy + (ROW_H - font.lineHeight) / 2;
            int barY = cy + (ROW_H - BAR_H) / 2;

            // Label — right-aligned inside its column
            g.drawString(font, row.label(), x + labelW - font.width(row.label()), ty, 0xFF999999, false);

            // Bar track + fill
            g.fill(barX, barY, barX + BAR_MAX_W, barY + BAR_H, 0xFF2A2A2A);
            int fillW = Math.round(BAR_MAX_W * Math.min(row.value(), MAX_STAT) / (float) MAX_STAT);
            if (fillW > 0) {
                g.fill(barX, barY, barX + fillW, barY + BAR_H, 0xFF000000 | row.color());
            }

            // Value — left-aligned in number column
            g.drawString(font, String.valueOf(row.value()), numX, ty, 0xFFCCCCCC, false);

            cy += ROW_H;
        }

        // BST divider + total
        int divY = cy + (ROW_H - 1) / 2;
        g.fill(barX, divY, barX + BAR_MAX_W, divY + 1, 0xFF444444);
        int ty = cy + (ROW_H - font.lineHeight) / 2;
        g.drawString(font, "BST", x + labelW - font.width("BST"), ty, 0xFF999999, false);
        g.drawString(font, String.valueOf(bst), numX, ty, 0xFFEEEEEE, false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int labelColumnWidth(Font font) {
        // "SpA" / "SpD" are the widest labels
        return font.width("SpD");
    }

    private int numberColumnWidth(Font font) {
        // BST can reach ~720; stats top at 255
        int maxVal = Math.max(bst, Math.max(hp, Math.max(atk, Math.max(def, Math.max(spa, Math.max(spd, spe))))));
        return font.width(String.valueOf(maxVal));
    }
}
