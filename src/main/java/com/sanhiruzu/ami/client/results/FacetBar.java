package com.sanhiruzu.ami.client.results;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Horizontal strip of quick-filter facet badges pinned to the top of the results panel.
 * Each badge is a 12×12 coloured square; an active badge gains a 1px white border.
 */
public class FacetBar {

    public static final int HEIGHT = 20;

    private static final int BADGE_SIZE = 12;
    private static final int BADGE_GAP  = 4;
    private static final int PAD_X      = 5;
    private static final int PAD_Y      = (HEIGHT - BADGE_SIZE) / 2;

    private record Facet(String id, String label, int color) {}

    private static final List<Facet> FACETS = List.of(
            new Facet("storage", "STR", 0xFF4169E1),
            new Facet("weapons", "WPN", 0xFFCC3333),
            new Facet("food",    "EAT", 0xFF33AA33),
            new Facet("tools",   "TLS", 0xFFCCAA00),
            new Facet("magic",   "MAG", 0xFF9933CC)
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
        g.fill(x, y, x + width, y + HEIGHT, 0xFF111118);

        var font = Minecraft.getInstance().font;
        int bx = x + PAD_X;
        int by = y + PAD_Y;

        for (Facet facet : FACETS) {
            boolean isActive = active.contains(facet.id());
            boolean hovered  = mouseX >= bx && mouseX < bx + BADGE_SIZE
                    && mouseY >= by && mouseY < by + BADGE_SIZE;

            if (isActive) {
                // White 1px border
                g.fill(bx - 1, by - 1, bx + BADGE_SIZE + 1, by + BADGE_SIZE + 1, 0xFFFFFFFF);
            }

            // Badge fill — dimmer when inactive, full colour when active/hovered
            int fill = (isActive || hovered) ? facet.color() : (0x99000000 | (facet.color() & 0x00FFFFFF));
            g.fill(bx, by, bx + BADGE_SIZE, by + BADGE_SIZE, fill);

            // Label — centred inside badge
            int tw = font.width(facet.label());
            // Only render label when badge is active or hovered; otherwise icon colour alone conveys identity
            if (isActive || hovered) {
                g.drawString(font, facet.label(), bx + (BADGE_SIZE - tw) / 2, by + 2, 0xFFFFFFFF, false);
            }

            bx += BADGE_SIZE + BADGE_GAP;
        }

        // "FILTERS" hint text at the right edge
        String hint = "FILTERS";
        g.drawString(font, hint, x + width - font.width(hint) - PAD_X, y + PAD_Y + 2, 0xFF444455, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (mouseY < y || mouseY >= y + HEIGHT) return false;

        int bx = x + PAD_X;
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
