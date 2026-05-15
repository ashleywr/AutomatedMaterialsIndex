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
 * Horizontal strip of quick-filter facet badges pinned to the top of the results panel.
 * Each badge is a 12×12 coloured square; an active badge gains a 1px white border.
 */
public class FacetBar {

    public static final int HEIGHT = 20;

    private static final int BADGE_SIZE = 12;
    private static final int BADGE_GAP  = 4;
    private static final int PAD_Y      = (HEIGHT - BADGE_SIZE) / 2;

    private record Facet(String id, String translationKey, int color) {}

    private static final List<Facet> FACETS = List.of(
            new Facet("storage", "ami.gui.facet.storage", 0xFF4169E1),
            new Facet("weapons", "ami.gui.facet.weapons", 0xFF69E1),
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
        // Bar background - slightly lighter than the panel background
        g.fill(x, y, x + width, y + HEIGHT, AMITheme.PANEL_INNER);

        var font = Minecraft.getInstance().font;
        int bx = x + AMITheme.GLOBAL_PADDING;
        int by = y + PAD_Y;

        for (Facet facet : FACETS) {
            boolean isActive = active.contains(facet.id());
            boolean hovered  = mouseX >= bx && mouseX < bx + BADGE_SIZE
                    && mouseY >= by && mouseY < by + BADGE_SIZE;

            if (isActive) {
                // White circular border (slightly larger than badge)
                renderCircle(g, bx - 1, by - 1, BADGE_SIZE + 2, 0xFFFFFFFF);
            }

            // Badge fill — dimmer when inactive, full colour when active/hovered
            int fill = (isActive || hovered) ? facet.color() : (0x99000000 | (facet.color() & 0x00FFFFFF));
            renderCircle(g, bx, by, BADGE_SIZE, fill);

            // Label — centred inside badge
            Component label = Component.translatable(facet.translationKey());
            int tw = font.width(label);
            // Only render label when badge is active or hovered; otherwise icon colour alone conveys identity
            if (isActive || hovered) {
                g.drawString(font, label, bx + (BADGE_SIZE - tw) / 2, by + 2, 0xFFFFFFFF, false);
            }

            bx += BADGE_SIZE + BADGE_GAP;
        }

        // "FILTERS" hint text at the right edge
        Component hint = Component.translatable("ami.gui.facet.hint");
        g.drawString(font, hint, x + width - font.width(hint) - AMITheme.GLOBAL_PADDING, y + PAD_Y + 2, 0xFF444455, false);
    }

    /**
     * Approximates a circular fill for a 12x12 or 14x14 area.
     */
    private void renderCircle(GuiGraphics g, int cx, int cy, int size, int color) {
        if (size <= 12) {
            // 12x12 circle approximation
            g.fill(cx + 2, cy,     cx + 10, cy + 1,  color); // Row 0
            g.fill(cx + 1, cy + 1, cx + 11, cy + 2,  color); // Row 1
            g.fill(cx,     cy + 2, cx + 12, cy + 10, color); // Rows 2-9
            g.fill(cx + 1, cy + 10, cx + 11, cy + 11, color); // Row 10
            g.fill(cx + 2, cy + 11, cx + 10, cy + 12, color); // Row 11
        } else {
            // 14x14 border approximation
            g.fill(cx + 3, cy,     cx + 11, cy + 1,  color);
            g.fill(cx + 1, cy + 1, cx + 13, cy + 2,  color);
            g.fill(cx,     cy + 2, cx + 14, cy + 12, color);
            g.fill(cx + 1, cy + 12, cx + 13, cy + 13, color);
            g.fill(cx + 3, cy + 13, cx + 11, cy + 14, color);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (mouseY < y || mouseY >= y + HEIGHT) return false;

        int bx = x + AMITheme.GLOBAL_PADDING;
        int by = y + PAD_Y;
        for (Facet facet : FACETS) {
            if (mouseX >= bx && mouseX < bx + BADGE_SIZE
                    && mouseY >= by && mouseY < by + BADGE_SIZE) {
                if (active.contains(facet.id())) {
                    active.remove(facet.id());
                } else {
                    active.add(facet.id());
                }
                return true;
            }
            bx += BADGE_SIZE + BADGE_GAP;
        }
        return false;
    }

    /** Returns the currently active facet IDs (e.g. "storage", "weapons"). */
    public Set<String> getActiveFacets() {
        return Collections.unmodifiableSet(active);
    }
}
