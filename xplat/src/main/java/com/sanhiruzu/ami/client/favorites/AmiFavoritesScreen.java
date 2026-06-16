package com.sanhiruzu.ami.client.favorites;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.RecipeViewerScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Browsable panel of all recipes the player has starred in the recipe viewer.
 * JEI equivalent: the bookmarks overlay.
 */
public class AmiFavoritesScreen extends Screen {

    private static final int GUI_WIDTH  = 240;
    private static final int GUI_HEIGHT = 220;
    private static final int HEADER_H   = 24;
    private static final int FOOTER_H   = 14;
    private static final int SLOT       = 18;   // slot size (item 16px + 1px border each side)
    private static final int COLS       = (GUI_WIDTH - 16) / SLOT;  // 12 columns

    private final Screen parent;

    private int guiLeft, guiTop;
    private int scrollOffset = 0;           // in rows
    private List<AmiFavoritesHandler.RecipeFavoriteEntry> favorites = List.of();

    public AmiFavoritesScreen(Screen parent) {
        super(Component.translatable("ami.favorites.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        guiLeft = (width  - GUI_WIDTH)  / 2;
        guiTop  = (height - GUI_HEIGHT) / 2;
        reload();
    }

    private void reload() {
        favorites = AmiFavoritesHandler.getInstance().getRecipeFavorites();
        int maxRow = Math.max(0, totalRows() - visibleRows());
        scrollOffset = Math.min(scrollOffset, maxRow);
    }

    private int totalRows() {
        return (favorites.size() + COLS - 1) / COLS;
    }

    private int visibleRows() {
        int contentH = GUI_HEIGHT - HEADER_H - FOOTER_H - 8;
        return contentH / SLOT;
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        AMITheme.sync();

        int bg    = AMITheme.RECIPE_PANEL;
        int bord  = AMITheme.RECIPE_BORDER;
        int inner = AMITheme.RECIPE_PANEL_INNER;

        // Panel
        g.fill(0, 0, width, height, AMITheme.RECIPE_BG_OVERLAY);
        g.fill(guiLeft - 1, guiTop - 1, guiLeft + GUI_WIDTH + 1, guiTop + GUI_HEIGHT + 1, bord);
        g.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, bg);
        g.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 1, AMITheme.RECIPE_TAB_ACTIVE);

        // Header
        g.centeredText(font, title,
                guiLeft + GUI_WIDTH / 2, guiTop + (HEADER_H - font.lineHeight) / 2,
                AMITheme.RECIPE_TEXT_TITLE);
        g.fill(guiLeft + 4, guiTop + HEADER_H, guiLeft + GUI_WIDTH - 4, guiTop + HEADER_H + 1,
                AMITheme.RECIPE_HEADER_LINE);

        // Item grid
        int gridX   = guiLeft + (GUI_WIDTH - COLS * SLOT) / 2;
        int gridTop = guiTop + HEADER_H + 4;
        int vis     = visibleRows();
        int contentH = vis * SLOT;

        // Inner tint
        g.fill(gridX - 1, gridTop, gridX + COLS * SLOT + 1, gridTop + contentH, inner);

        if (favorites.isEmpty()) {
            g.centeredText(font,
                    Component.translatable("ami.favorites.empty"),
                    guiLeft + GUI_WIDTH / 2,
                    gridTop + contentH / 2 - font.lineHeight,
                    AMITheme.RECIPE_TEXT_CAT);
        } else {
            int startItem = scrollOffset * COLS;
            int endItem   = Math.min(startItem + vis * COLS, favorites.size());

            for (int idx = startItem; idx < endItem; idx++) {
                int row = (idx - startItem) / COLS;
                int col = (idx - startItem) % COLS;
                int sx  = gridX + col * SLOT + 1;
                int sy  = gridTop + row * SLOT + 1;

                AmiFavoritesHandler.RecipeFavoriteEntry entry = favorites.get(idx);
                ItemStack stack = entry.stack();

                // Slot background + hover
                boolean hov = isHovering(mouseX, mouseY, sx, sy, SLOT - 2, SLOT - 2);
                if (hov) g.fill(sx, sy, sx + SLOT - 2, sy + SLOT - 2, AMITheme.RECIPE_TAB_HOVER);

                g.item(stack, sx, sy);
                g.itemDecorations(font, stack, sx, sy);

                if (hov) {
                    List<Component> tip = new java.util.ArrayList<>(
                            Screen.getTooltipFromItem(minecraft, stack));
                    tip.add(Component.translatable("ami.favorites.click_open")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                    tip.add(Component.translatable("ami.favorites.right_click_remove")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                    g.setTooltipForNextFrame(font, tip, java.util.Optional.empty(), mouseX, mouseY);
                }
            }

            // Scroll bar (if needed)
            if (totalRows() > vis) {
                int barH     = contentH;
                int thumbH   = Math.max(12, barH * vis / totalRows());
                int thumbY   = gridTop + (int) ((long) scrollOffset * (barH - thumbH) / Math.max(1, totalRows() - vis));
                int barX     = gridX + COLS * SLOT + 3;
                g.fill(barX, gridTop, barX + 3, gridTop + barH, AMITheme.SCROLL_TRACK);
                g.fill(barX, thumbY, barX + 3, thumbY + thumbH, AMITheme.SCROLL_THUMB);
            }
        }

        // Footer hint
        g.centeredText(font,
                Component.translatable("ami.favorites.footer_hint"),
                guiLeft + GUI_WIDTH / 2,
                guiTop + GUI_HEIGHT - FOOTER_H + 2,
                AMITheme.RECIPE_TEXT_FOOTER);
    }

    // ── Input ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button    = event.button();

        int gridX   = guiLeft + (GUI_WIDTH - COLS * SLOT) / 2;
        int gridTop = guiTop + HEADER_H + 4;
        int vis     = visibleRows();
        int start   = scrollOffset * COLS;
        int end     = Math.min(start + vis * COLS, favorites.size());

        for (int idx = start; idx < end; idx++) {
            int row = (idx - start) / COLS;
            int col = (idx - start) % COLS;
            int sx  = gridX + col * SLOT + 1;
            int sy  = gridTop + row * SLOT + 1;

            if (isHovering(mouseX, mouseY, sx, sy, SLOT - 2, SLOT - 2)) {
                AmiFavoritesHandler.RecipeFavoriteEntry entry = favorites.get(idx);
                if (button == 0) {
                    if (minecraft != null) {
                        minecraft.setScreen(new RecipeViewerScreen(entry.stack(), parent, true));
                    }
                    return true;
                } else if (button == 1) {
                    AmiFavoritesHandler.getInstance().removeRecipeFavorite(entry.recipeId(), entry.stack());
                    reload();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxRow = Math.max(0, totalRows() - visibleRows());
        if (maxRow == 0) return false;
        scrollOffset = Math.max(0, Math.min(scrollOffset + (scrollY > 0 ? -1 : 1), maxRow));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private boolean isHovering(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
