package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.index.AmiOntology;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Horizontal strip of quick-filter pills, one per ontology category.
 * Each pill renders the category's representative item icon (16×16).
 * Active pills receive a white 1-px border; hovering shows the category name
 * as a deferred tooltip (call {@link #renderTooltip} after the panel draws).
 */
public class FacetBar {

    public static final int HEIGHT = 22;

    private static final int ICON_SIZE = 16;
    private static final int PILL_W    = ICON_SIZE + 2; // 1px border each side
    private static final int PILL_H    = ICON_SIZE + 2;
    private static final int PILL_GAP  = 3;
    private static final int PAD_Y     = (HEIGHT - PILL_H) / 2;

    private static final Map<String, ItemStack> ICON_CACHE = new HashMap<>();

    private int x, y, width;
    private final Set<String> active = new HashSet<>();

    /** Set during render(); consumed by renderTooltip(). */
    private String hoveredTooltip = null;
    private int hoveredTooltipX, hoveredTooltipY;

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
        hoveredTooltip = null;

        // Clip pills to our strip so they don't spill right
        g.enableScissor(x, y, x + width, y + HEIGHT);

        int px = x + AMITheme.GLOBAL_PADDING;
        int py = y + PAD_Y;

        for (AmiOntology.Category cat : AmiOntology.CATEGORIES) {
            boolean isActive = active.contains(cat.id);
            boolean hovered  = mouseX >= px && mouseX < px + PILL_W
                    && mouseY >= py && mouseY < py + PILL_H;

            // Background fill — semi-transparent category color
            int alpha  = isActive ? 0xAA : (hovered ? 0x66 : 0x33);
            int bgRgb  = cat.color & 0x00FFFFFF;
            g.fill(px, py, px + PILL_W, py + PILL_H, (alpha << 24) | bgRgb);

            // White 1-px outline for active pills
            if (isActive) {
                int bx = px - 1, by = py - 1, bx2 = px + PILL_W + 1, by2 = py + PILL_H + 1;
                g.fill(bx,  by,      bx2,      by  + 1, 0xFFFFFFFF); // top
                g.fill(bx,  by2,     bx2,      by2 + 1, 0xFFFFFFFF); // bottom
                g.fill(bx,  by,      bx  + 1,  by2 + 1, 0xFFFFFFFF); // left
                g.fill(bx2, by,      bx2 + 1,  by2 + 1, 0xFFFFFFFF); // right
            } else if (hovered) {
                // Colored 1-px outline for hovered pills
                int bx = px - 1, by = py - 1, bx2 = px + PILL_W + 1, by2 = py + PILL_H + 1;
                int outline = 0xFF000000 | bgRgb;
                g.fill(bx,      by,  bx2,      by  + 1, outline); // top
                g.fill(bx,      by2, bx2,      by2 + 1, outline); // bottom
                g.fill(bx,      by,  bx  + 1,  by2 + 1, outline); // left
                g.fill(bx2,     by,  bx2 + 1,  by2 + 1, outline); // right
            }

            // Item icon (16×16 at px+1, py+1)
            ItemStack stack = getIconStack(cat.iconItemId);
            if (!stack.isEmpty()) {
                g.renderItem(stack, px + 1, py + 1);
            }

            if (hovered) {
                hoveredTooltip  = cat.displayName;
                hoveredTooltipX = mouseX;
                hoveredTooltipY = mouseY;
            }

            px += PILL_W + PILL_GAP;
        }

        g.disableScissor();
    }

    /**
     * Call this after the panel and all overlays have been drawn so the tooltip
     * appears on top of everything.
     */
    public void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (hoveredTooltip != null) {
            g.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(hoveredTooltip), hoveredTooltipX, hoveredTooltipY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (mouseY < y || mouseY >= y + HEIGHT) return false;

        int px = x + AMITheme.GLOBAL_PADDING;
        int py = y + PAD_Y;

        for (AmiOntology.Category cat : AmiOntology.CATEGORIES) {
            if (mouseX >= px && mouseX < px + PILL_W
                    && mouseY >= py && mouseY < py + PILL_H) {
                if (active.contains(cat.id)) active.remove(cat.id);
                else                         active.add(cat.id);
                return true;
            }
            px += PILL_W + PILL_GAP;
        }
        return false;
    }

    /** Returns the currently active category IDs (e.g. "food", "weapons"). */
    public Set<String> getActiveFacets() {
        return Collections.unmodifiableSet(active);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ItemStack getIconStack(String itemId) {
        return ICON_CACHE.computeIfAbsent(itemId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) return ItemStack.EMPTY;
            return BuiltInRegistries.ITEM.getOptional(rl)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
        });
    }
}
