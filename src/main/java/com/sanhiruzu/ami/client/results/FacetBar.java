package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Horizontal strip of quick-filter facet pills pinned to the top of the results panel.
 * Each pill shows its label at all times; active pills get a white 1px border and full
 * colour, inactive pills are darkened.
 */
public class FacetBar {

    public static final int HEIGHT = 20;

    private static final int PILL_H    = 12;
    private static final int PILL_PAD  = 5;  // horizontal padding each side
    private static final int PILL_GAP  = 4;
    private static final int PAD_Y     = (HEIGHT - PILL_H) / 2;

    private record Facet(String id, String translationKey, int color) {}

    private static final List<Facet> FACETS = List.of(
            new Facet("storage", "ami.gui.facet.storage", 0xFF4169E1),
            new Facet("weapons", "ami.gui.facet.weapons", 0xFFCC3333),
            new Facet("food",    "ami.gui.facet.food",    0xFF33AA33),
            new Facet("tools",   "ami.gui.facet.tools",   0xFFCCAA00),
            new Facet("magic",   "ami.gui.facet.magic",   0xFF9933CC)
    );

    private int x, y, width;
    private final Set<String> active = new HashSet<>();

    public FacetBar(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void updateLayout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(x, y, x + width, y + HEIGHT, AMITheme.PANEL_INNER);

        var font = Minecraft.getInstance().font;
        int bx = x + AMITheme.GLOBAL_PADDING;
        int by = y + PAD_Y;

        for (Facet facet : FACETS) {
            Component label = Component.translatable(facet.translationKey());
            int tw  = font.width(label);
            int pw  = tw + PILL_PAD * 2;

            boolean isActive = active.contains(facet.id());
            boolean hovered  = mouseX >= bx && mouseX < bx + pw
                    && mouseY >= by && mouseY < by + PILL_H;

            int fill = (isActive || hovered) ? facet.color() : darken(facet.color(), 0.40f);

            // White 1px border for active pills
            if (isActive) {
                renderPill(g, bx - 1, by - 1, pw + 2, PILL_H + 2, 0xFFFFFFFF);
            }
            renderPill(g, bx, by, pw, PILL_H, fill);

            int textColor = (isActive || hovered) ? 0xFFFFFFFF : 0xFFBBBBBB;
            int textY = by + (PILL_H - font.lineHeight) / 2;
            g.drawString(font, label, bx + PILL_PAD, textY, textColor, false);

            bx += pw + PILL_GAP;
        }

        Component hint = Component.translatable("ami.gui.facet.hint");
        g.drawString(font, hint, x + width - font.width(hint) - AMITheme.GLOBAL_PADDING,
                y + PAD_Y + 2, 0xFF555566, false);
    }

    /** Rounded rectangle with 1px corner cutouts. */
    private static void renderPill(GuiGraphics g, int px, int py, int pw, int ph, int color) {
        if (pw < 3 || ph < 3) return;
        g.fill(px + 1, py,      px + pw - 1, py + ph,     color); // main body
        g.fill(px,     py + 1,  px + 1,      py + ph - 1, color); // left cap
        g.fill(px + pw - 1, py + 1, px + pw, py + ph - 1, color); // right cap
    }

    /** Scale each RGB channel by {@code factor}, preserving alpha. */
    private static int darken(int argb, float factor) {
        int a = (argb >> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * factor);
        int g = (int) (((argb >> 8)  & 0xFF) * factor);
        int b = (int) ((argb         & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (mouseY < y || mouseY >= y + HEIGHT) return false;

        var font = Minecraft.getInstance().font;
        int bx = x + AMITheme.GLOBAL_PADDING;
        int by = y + PAD_Y;

        for (Facet facet : FACETS) {
            int pw = font.width(Component.translatable(facet.translationKey())) + PILL_PAD * 2;

            if (mouseX >= bx && mouseX < bx + pw
                    && mouseY >= by && mouseY < by + PILL_H) {
                if (active.contains(facet.id())) {
                    active.remove(facet.id());
                } else {
                    active.add(facet.id());
                }
                return true;
            }
            bx += pw + PILL_GAP;
        }
        return false;
    }

    /** Returns the currently active facet IDs (e.g. "storage", "weapons"). */
    public Set<String> getActiveFacets() {
        return Collections.unmodifiableSet(active);
    }
}
