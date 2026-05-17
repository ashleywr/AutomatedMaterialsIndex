package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Horizontal strip of quick-filter pills, one per ontology category.
 * Each pill renders the category's representative item icon (16×16).
 * When pills overflow the strip width, the bar is scrollable with the mouse wheel.
 * Active pills receive a white 1-px border; hovering shows the category name
 * as a deferred tooltip (call {@link #renderTooltip} after the panel draws).
 */
public class FacetBar implements SearchState.Listener {

    public static final int HEIGHT = 22;

    private static final int ICON_SIZE = 16;
    private static final int PILL_W    = ICON_SIZE + 2; // 1px border each side
    private static final int PILL_H    = ICON_SIZE + 2;
    private static final int PILL_GAP  = 3;
    private static final int PAD_Y     = (HEIGHT - PILL_H) / 2;

    // How many pixels to scroll per wheel notch
    private static final int SCROLL_STEP = PILL_W + PILL_GAP;

    private static final Map<String, ItemStack> ICON_CACHE = new HashMap<>();

    private int x, y, width;
    private SearchState state;

    // Horizontal scroll position (pixels). 0 = leftmost pill visible.
    private int scrollOffsetX = 0;

    /**
     * Called on right-click with the $categoryId token to inject into the search bar.
     * Wired by OverlayWidgetManager via UniversalResultsPanel.
     */
    private Consumer<String> onTokenInject;

    /** Set during render(); consumed by renderTooltip(). */
    private Component hoveredTooltip = null;
    private int hoveredTooltipX, hoveredTooltipY;

    public FacetBar(int x, int y, int width, SearchState state) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.state = state;
        state.addListener(this);
    }

    public void setOnTokenInject(Consumer<String> callback) {
        this.onTokenInject = callback;
    }

    public void updateLayout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        clampScroll(); // re-clamp in case visible width changed
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEIGHT;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        hoveredTooltip = null;

        g.enableScissor(x, y, x + width, y + HEIGHT);

        int px = x + AMITheme.GLOBAL_PADDING - scrollOffsetX;
        int py = y + PAD_Y;

        Set<String> activeFacets = state.getActiveFacets();
        String currentQuery = state.getQuery().toLowerCase(java.util.Locale.ROOT);
        for (AmiOntology.Category cat : AmiOntology.CATEGORIES) {
            // Skip pills entirely outside the visible region (both left and right)
            if (px + PILL_W > x && px < x + width) {
                boolean isActive = activeFacets.contains(cat.id)
                        || currentQuery.contains("$" + cat.id);
                // Hover only counts when the pill is actually visible and mouse is over it
                boolean pillVisible = px >= x && px + PILL_W <= x + width;
                boolean hovered = pillVisible
                        && mouseX >= px && mouseX < px + PILL_W
                        && mouseY >= py && mouseY < py + PILL_H;

                int alpha = isActive ? 0xAA : (hovered ? 0x66 : 0x33);
                int bgRgb = cat.color & 0x00FFFFFF;
                g.fill(px, py, px + PILL_W, py + PILL_H, (alpha << 24) | bgRgb);

                if (isActive) {
                    int bx = px - 1, by = py - 1, bx2 = px + PILL_W + 1, by2 = py + PILL_H + 1;
                    g.fill(bx,  by,  bx2,      by  + 1, 0xFFFFFFFF);
                    g.fill(bx,  by2, bx2,      by2 + 1, 0xFFFFFFFF);
                    g.fill(bx,  by,  bx  + 1,  by2 + 1, 0xFFFFFFFF);
                    g.fill(bx2, by,  bx2 + 1,  by2 + 1, 0xFFFFFFFF);
                } else if (hovered) {
                    int bx = px - 1, by = py - 1, bx2 = px + PILL_W + 1, by2 = py + PILL_H + 1;
                    int outline = 0xFF000000 | bgRgb;
                    g.fill(bx,  by,  bx2,      by  + 1, outline);
                    g.fill(bx,  by2, bx2,      by2 + 1, outline);
                    g.fill(bx,  by,  bx  + 1,  by2 + 1, outline);
                    g.fill(bx2, by,  bx2 + 1,  by2 + 1, outline);
                }

                ItemStack stack = getIconStack(cat.iconItemId);
                if (!stack.isEmpty()) {
                    g.renderItem(stack, px + 1, py + 1);
                }

                if (hovered) {
                    hoveredTooltip  = Component.literal(cat.displayName.getString()
                            + " §7[Right-click: inject $" + cat.id + " into search]");
                    hoveredTooltipX = mouseX;
                    hoveredTooltipY = mouseY;
                }
            }

            px += PILL_W + PILL_GAP;
        }

        // Fade edges to hint at hidden content
        if (scrollOffsetX > 0) {
            // Left fade: content is hidden to the left
            g.fillGradient(x, y, x + 8, y + HEIGHT, 0xCC000000, 0x00000000);
        }
        if (scrollOffsetX < maxScroll()) {
            // Right fade: content is hidden to the right
            g.fillGradient(x + width - 8, y, x + width, y + HEIGHT, 0x00000000, 0xCC000000);
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
                    hoveredTooltip, hoveredTooltipX, hoveredTooltipY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;

        int px = x + AMITheme.GLOBAL_PADDING - scrollOffsetX;
        int py = y + PAD_Y;

        for (AmiOntology.Category cat : AmiOntology.CATEGORIES) {
            // Only register clicks on fully-visible pills
            boolean pillVisible = px >= x && px + PILL_W <= x + width;
            if (pillVisible
                    && mouseX >= px && mouseX < px + PILL_W
                    && mouseY >= py && mouseY < py + PILL_H) {

                if (button == 1) {
                    if (onTokenInject != null) onTokenInject.accept("$" + cat.id);
                } else if (Screen.hasShiftDown()) {
                    state.toggleFacet(cat.id);
                } else {
                    state.selectOnlyFacet(cat.id);
                }
                return true;
            }
            px += PILL_W + PILL_GAP;
        }
        return false;
    }

    /** Scrolls the pill strip horizontally. Wheel-up (positive delta) scrolls toward the start. */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        scrollOffsetX -= (int) Math.round(scrollDelta * SCROLL_STEP);
        clampScroll();
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            if (hoveredTooltip != null) {
                AmiClipboardHelper.copyToClipboard(hoveredTooltip.getString());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSearchStateChanged(SearchState state) {}

    /** Returns the currently active category IDs (e.g. "food", "weapons"). */
    public Set<String> getActiveFacets() {
        return state.getActiveFacets();
    }

    // ── Scroll helpers ────────────────────────────────────────────────────────

    private int contentWidth() {
        int n = AmiOntology.CATEGORIES.size();
        return n > 0 ? n * PILL_W + (n - 1) * PILL_GAP : 0;
    }

    private int maxScroll() {
        int visible = width - AMITheme.GLOBAL_PADDING;
        return Math.max(0, contentWidth() - visible);
    }

    private void clampScroll() {
        scrollOffsetX = Math.max(0, Math.min(scrollOffsetX, maxScroll()));
    }

    // ── Icon cache ────────────────────────────────────────────────────────────

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
