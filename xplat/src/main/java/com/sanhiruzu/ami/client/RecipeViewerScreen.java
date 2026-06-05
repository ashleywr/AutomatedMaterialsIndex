package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecipeViewerScreen extends Screen {

    // ── Layout constants ────────────────────────────────────────────────────
    private static final int GUI_WIDTH = 240;
    private static final int HEADER_H = 28;
    private static final int TAB_H = 18;
    private static final int TAB_BAR_Y = HEADER_H + 2;
    private static final int CONTENT_Y = TAB_BAR_Y + TAB_H + 6;
    private static final int FOOTER_H = 18;
    // ── Palette (synchronized dynamically from AMITheme in render) ───────────
    private static int COL_BG_OVERLAY = 0xFF101010;
    private static int COL_PANEL = 0xFF1A1A1F;
    private static int COL_PANEL_INNER = 0xFF22222A;
    private static int COL_BORDER = 0xFF3A3A4A;
    private static int COL_HEADER_LINE = 0xFF2E2E3A;
    private static int COL_TAB_ACTIVE = 0xFF4488FF;
    private static int COL_TAB_HOVER = 0xFF2E2E44;
    private static int COL_TAB_IDLE = 0xFF1E1E28;
    private static int COL_TAB_TEXT_A = 0xFFFFFFFF;
    private static int COL_TAB_TEXT_I = 0xFF8888AA;
    private static int COL_SLOT_BORDER = 0xFF555566;
    private static int COL_SLOT_BG = 0xFF2A2A36;
    private static int COL_ARROW = 0xFF6688CC;
    private static int COL_ARROW_ANIM = 0xFF4466AA;
    private static int COL_TEXT_TITLE = 0xFFFFFFFF;
    private static int COL_TEXT_ITEM = 0xFFBBBBCC;
    private static int COL_TEXT_CAT = 0xFF8888AA;
    private static int COL_TEXT_NAV = 0xFF8888AA;
    private static int COL_TEXT_FOOTER = 0xFF555566;
    private static int COL_BTN_IDLE = 0xFF226622;
    private static int COL_BTN_HOVER = 0xFF44AA44;
    private static int COL_SHAPELESS = 0xFF5555AA;
    private final Screen parentScreen;
    private final List<HistoryEntry> history = new java.util.ArrayList<>();
    private final java.util.Map<String, Integer> slotOffsets = new java.util.HashMap<>();
    private final boolean shouldRestoreAmiEnabled;
    private int guiHeight = 180;
    private int recipesPerPage = 1;
    // ── State ────────────────────────────────────────────────────────────────
    private ItemStack target;
    private boolean showRecipes;
    private RecipeType<?> focusType = null;
    private List<Tab> tabs = List.of();
    private int selectedTab;
    private int pageIndex;
    private int guiLeft, guiTop;
    private RecipeLayout currentLayout;
    private long animStart;
    private boolean canTransfer;
    public RecipeViewerScreen(ItemStack target, Screen parentScreen, boolean showRecipes) {
        super(showRecipes
                ? Component.translatable("ami.recipe_viewer.recipes_title", target.getHoverName())
                : Component.translatable("ami.recipe_viewer.uses_title", target.getHoverName()));
        this.target = target;
        this.showRecipes = showRecipes;
        this.shouldRestoreAmiEnabled = InventoryOverlayHandler.isAmiEnabled();

        if (parentScreen instanceof RecipeViewerScreen rvs) {
            this.parentScreen = rvs.parentScreen;
            this.history.addAll(rvs.history);
            this.history.add(new HistoryEntry(rvs.target, rvs.showRecipes, rvs.selectedTab, rvs.pageIndex, rvs.focusType));
        } else {
            this.parentScreen = parentScreen;
        }
    }

    @Override
    protected void init() {
        int maxH = height - 30;
        int availableForRecipes = maxH - 88;
        int N = Math.max(1, availableForRecipes / 74);
        N = Math.min(N, 3);
        this.recipesPerPage = N;

        if (N == 1) {
            this.guiHeight = 180;
        } else {
            this.guiHeight = 88 + N * 74;
        }

        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - guiHeight) / 2;
        animStart = System.currentTimeMillis();

        if (tabs.isEmpty()) {
            rebuildTabs();
        } else {
            refreshLayout();
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    private void rebuildTabs() {
        tabs = List.of();
        if (Services.PLATFORM.isRecipeIndexBuilt() && minecraft != null && minecraft.level != null) {
            List<AmiRecipeHolder<?>> all;
            if (focusType != null) {
                all = Services.PLATFORM.getAllRecipesOfType(focusType);
            } else {
                all = showRecipes
                        ? Services.PLATFORM.getRecipesFor(target)
                        : Services.PLATFORM.getUsesFor(target);
            }

            Map<RecipeType<?>, List<AmiRecipeHolder<?>>> grouped = new LinkedHashMap<>();
            for (AmiRecipeHolder<?> recipe : all) {
                grouped.computeIfAbsent(recipe.value().getType(), k -> new ArrayList<>()).add(recipe);
            }

            List<Tab> list = new ArrayList<>();
            for (var entry : grouped.entrySet()) {
                RecipeType<?> type = entry.getKey();
                list.add(new Tab(
                        type,
                        RecipeDisplayHelper.tabComponent(type),
                        RecipeDisplayHelper.tabShortLabel(type),
                        entry.getValue()));
            }
            tabs = list;
        }

        selectedTab = 0;
        pageIndex = 0;
        refreshLayout();
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    public Component getTitle() {
        if (focusType != null) {
            String catName = tabs.isEmpty() ? "Recipes" : tabs.get(selectedTab).label().getString();
            return Component.literal(catName + " Recipes");
        }
        return showRecipes
                ? Component.translatable("ami.recipe_viewer.recipes_title", target.getHoverName())
                : Component.translatable("ami.recipe_viewer.uses_title", target.getHoverName());
    }

    private void navigateTo(ItemStack newTarget, boolean newShowRecipes) {
        if (newTarget.isEmpty()) return;
        history.add(new HistoryEntry(target, showRecipes, selectedTab, pageIndex, focusType));

        this.target = newTarget;
        this.showRecipes = newShowRecipes;
        this.focusType = null;
        rebuildTabs();
    }

    private void navigateToType(RecipeType<?> type) {
        if (type == null) return;
        history.add(new HistoryEntry(target, showRecipes, selectedTab, pageIndex, focusType));

        this.focusType = type;
        rebuildTabs();
    }

    private void goBack() {
        if (!history.isEmpty()) {
            HistoryEntry entry = history.remove(history.size() - 1);
            this.target = entry.target();
            this.showRecipes = entry.showRecipes();
            this.focusType = entry.focusType();
            rebuildTabs();

            this.selectedTab = Math.min(entry.selectedTab(), tabs.size() - 1);
            if (selectedTab >= 0) {
                Tab tab = tabs.get(selectedTab);
                int totalRecipes = tab.recipes().size();
                int totalPages = (int) Math.ceil((double) totalRecipes / recipesPerPage);
                this.pageIndex = Math.min(entry.pageIndex(), totalPages - 1);
            }
            refreshLayout();
        } else {
            onClose();
        }
    }

    private void refreshLayout() {
        canTransfer = false;
        slotOffsets.clear();
        if (!tabs.isEmpty() && minecraft != null && minecraft.level != null) {
            Tab tab = tabs.get(selectedTab);
            if (!tab.recipes().isEmpty()) {
                int totalRecipes = tab.recipes().size();
                int totalPages = (int) Math.ceil((double) totalRecipes / recipesPerPage);
                pageIndex = Math.min(pageIndex, totalPages - 1);
                if (pageIndex < 0) pageIndex = 0;

                int firstVisibleIdx = pageIndex * recipesPerPage;
                if (firstVisibleIdx < totalRecipes) {
                    currentLayout = RecipeDisplayHelper.getLayout(
                            tab.recipes().get(firstVisibleIdx), minecraft.level.registryAccess());
                    canTransfer = RecipeViewerBridge.canTransferRecipe(
                            tab.recipes().get(firstVisibleIdx), parentScreen);
                    return;
                }
            }
        }
        currentLayout = null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        AMITheme.sync();
        COL_BG_OVERLAY = AMITheme.RECIPE_BG_OVERLAY;
        COL_PANEL = AMITheme.RECIPE_PANEL;
        COL_PANEL_INNER = AMITheme.RECIPE_PANEL_INNER;
        COL_BORDER = AMITheme.RECIPE_BORDER;
        COL_HEADER_LINE = AMITheme.RECIPE_HEADER_LINE;
        COL_TAB_ACTIVE = AMITheme.RECIPE_TAB_ACTIVE;
        COL_TAB_HOVER = AMITheme.RECIPE_TAB_HOVER;
        COL_TAB_IDLE = AMITheme.RECIPE_TAB_IDLE;
        COL_TAB_TEXT_A = AMITheme.RECIPE_TAB_TEXT_A;
        COL_TAB_TEXT_I = AMITheme.RECIPE_TAB_TEXT_I;
        COL_SLOT_BORDER = AMITheme.RECIPE_SLOT_BORDER;
        COL_SLOT_BG = AMITheme.RECIPE_SLOT_BG;
        COL_ARROW = AMITheme.RECIPE_ARROW;
        COL_ARROW_ANIM = AMITheme.RECIPE_ARROW_ANIM;
        COL_TEXT_TITLE = AMITheme.RECIPE_TEXT_TITLE;
        COL_TEXT_ITEM = AMITheme.RECIPE_TEXT_ITEM;
        COL_TEXT_CAT = AMITheme.RECIPE_TEXT_CAT;
        COL_TEXT_NAV = AMITheme.RECIPE_TEXT_NAV;
        COL_TEXT_FOOTER = AMITheme.RECIPE_TEXT_FOOTER;
        COL_BTN_IDLE = AMITheme.RECIPE_BTN_IDLE;
        COL_BTN_HOVER = AMITheme.RECIPE_BTN_HOVER;
        COL_SHAPELESS = AMITheme.RECIPE_SHAPELESS;

        g.fill(0, 0, width, height, COL_BG_OVERLAY);
        drawPanel(g);
        drawHeader(g);

        if (tabs.isEmpty()) {
            drawNoRecipes(g);
        } else {
            drawTabBar(g, mouseX, mouseY);
            drawContent(g, mouseX, mouseY);
        }

        g.drawCenteredString(font,
                Component.translatable("ami.recipe_viewer.go_back"),
                guiLeft + GUI_WIDTH / 2,
                guiTop + guiHeight - FOOTER_H + 4,
                COL_TEXT_FOOTER);
    }

    private void drawPanel(GuiGraphics g) {
        int x = guiLeft, y = guiTop, w = GUI_WIDTH, h = guiHeight;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, COL_BORDER);
        g.fill(x, y, x + w, y + h, COL_PANEL);
        g.fill(x, y, x + w, y + 1, COL_TAB_ACTIVE);
        int contentTop = guiTop + CONTENT_Y - 2;
        int contentBot = guiTop + guiHeight - FOOTER_H - 2;
        g.fill(guiLeft + 4, contentTop, guiLeft + GUI_WIDTH - 4, contentBot, COL_PANEL_INNER);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void drawHeader(GuiGraphics g) {
        Component modeLabel = showRecipes
                ? Component.translatable("ami.recipe_viewer.mode.recipes")
                : Component.translatable("ami.recipe_viewer.mode.uses");
        g.drawCenteredString(font, modeLabel,
                guiLeft + GUI_WIDTH / 2, guiTop + 5, COL_TEXT_TITLE);

        g.drawCenteredString(font, target.getHoverName(),
                guiLeft + GUI_WIDTH / 2, guiTop + 5 + font.lineHeight + 2, COL_TEXT_ITEM);

        g.fill(guiLeft + 4, guiTop + HEADER_H, guiLeft + GUI_WIDTH - 4, guiTop + HEADER_H + 1, COL_HEADER_LINE);
    }

    // ── Panel chrome ──────────────────────────────────────────────────────────

    private void drawTabBar(GuiGraphics g, int mouseX, int mouseY) {
        int barY = guiTop + TAB_BAR_Y;
        int tabW = Math.min(GUI_WIDTH / tabs.size(), 72);
        int totalW = tabW * tabs.size();
        int startX = guiLeft + (GUI_WIDTH - totalW) / 2;

            for (int i = 0; i < tabs.size(); i++) {
                int tx = startX + i * tabW;
                int tw = tabW - 1;
                boolean active = (i == selectedTab);
                boolean hovered = isHovering(mouseX, mouseY, tx, barY, tw, TAB_H);

                int bg = active ? COL_TAB_ACTIVE : hovered ? COL_TAB_HOVER : COL_TAB_IDLE;
                AMITheme.fillRounded(g, tx, barY, tw, TAB_H, bg);

                if (active) {
                    g.fill(tx + 1, barY + TAB_H - 2, tx + tw - 1, barY + TAB_H, COL_TAB_ACTIVE);
                }

                ItemStack icon = RecipeDisplayHelper.getRepresentativeWorkstation(tabs.get(i).type());
                if (icon != null && !icon.isEmpty()) {
                    int iconX = tx + (tw - 16) / 2;
                    int iconY = barY + (TAB_H - 16) / 2;
                    g.renderItem(icon, iconX, iconY);
                } else {
                    String label = tabs.get(i).shortLabel();
                    int textColor = active ? COL_TAB_TEXT_A : COL_TAB_TEXT_I;
                    int textX = tx + (tw - font.width(label)) / 2;
                    int textY = barY + (TAB_H - font.lineHeight) / 2;
                    g.drawString(font, label, textX, textY, textColor, false);
                }

                if (hovered) {
                    g.renderTooltip(font, tabs.get(i).label(), mouseX, mouseY);
                }
            }
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private void drawContent(GuiGraphics g, int mouseX, int mouseY) {
        Tab tab = tabs.get(selectedTab);
        int tabSize = tab.recipes().size();
        if (tabSize == 0) return;

        int totalPages = (int) Math.ceil((double) tabSize / recipesPerPage);
        int startIdx = pageIndex * recipesPerPage;
        int endIdx = Math.min(startIdx + recipesPerPage, tabSize);

        int cardW = GUI_WIDTH - 8;
        int cardH = 70;
        int cardX = guiLeft + 4;

        for (int i = startIdx; i < endIdx; i++) {
            AmiRecipeHolder<?> recipe = tab.recipes().get(i);
            RecipeLayout layout = RecipeDisplayHelper.getLayout(recipe, minecraft.level.registryAccess());
            if (layout == null) continue;

            int cardY = guiTop + CONTENT_Y + (i - startIdx) * 74 + (recipesPerPage == 1 ? 22 : 0);

            g.fill(cardX - 1, cardY - 1, cardX + cardW + 1, cardY + cardH + 1, COL_BORDER);
            g.fill(cardX, cardY, cardX + cardW, cardY + cardH, COL_PANEL);

            int rx = cardX + 24;

            int layoutHeight;
            if (layout.backgroundTexture() != null) {
                layoutHeight = layout.bgRenderY() + layout.bgH();
            } else {
                layoutHeight = 14 + layout.gridHeight() * 18;
            }
            int yOffset = (cardH - layoutHeight) / 2;
            int ry = cardY + yOffset;

            if (layout.backgroundTexture() != null) {
                int bx = rx + layout.bgRenderX();
                int by = ry + layout.bgRenderY();
                int bw = layout.bgW();
                int bh = layout.bgH();

                g.fill(bx - 1, by - 1, bx + bw + 1, by, COL_SLOT_BORDER);
                g.fill(bx - 1, by + bh, bx + bw + 1, by + bh + 1, COL_SLOT_BORDER);
                g.fill(bx - 1, by, bx, by + bh, COL_SLOT_BORDER);
                g.fill(bx + bw, by, bx + bw + 1, by + bh, COL_SLOT_BORDER);

                g.blit(layout.backgroundTexture(),
                        bx, by, bw, bh,
                        layout.bgX(), layout.bgY(), bw, bh,
                        256, 256);
            }

            if (layout.shapeless()) {
                Component shapeless = Component.translatable("ami.recipe_viewer.shapeless");
                int sw = font.width(shapeless);
                g.drawString(font, shapeless, cardX + cardW - sw - 6, cardY + 4, COL_SHAPELESS, false);
            }

            if (!layout.output().isEmpty()) {
                int favX = cardX + 6;
                int favY = cardY + 4;
                boolean favorite = AmiFavoritesHandler.getInstance().isRecipeFavorite(recipe.id(), layout.output());
                boolean favHovered = isHovering(mouseX, mouseY, favX, favY, 14, 14);
                AMITheme.fillRounded(g, favX, favY, 14, 14,
                        favorite ? COL_TAB_ACTIVE : favHovered ? COL_TAB_HOVER : COL_TAB_IDLE);
                g.drawCenteredString(font, Component.translatable("ami.recipe_viewer.favorite_icon"),
                        favX + 7, favY + 3, favorite ? 0xFFFFFFFF : COL_TAB_TEXT_I);
                if (favHovered) {
                    g.renderTooltip(font, Component.translatable(favorite
                            ? "ami.recipe_viewer.unfavorite"
                            : "ami.recipe_viewer.favorite"), mouseX, mouseY);
                }
            }

            int slotIdx = 0;
            for (SlotPosition slot : layout.inputs()) {
                int sx = rx + slot.x();
                int sy = ry + slot.y();
                if (layout.drawSlotBackground()) {
                    drawSlot(g, sx, sy);
                }
                if (!slot.alternatives().isEmpty()) {
                    String slotKey = recipe.id().toString() + "_" + slotIdx;
                    int altIdx = getSlotAltIndex(slotKey, slot.alternatives().size());
                    ItemStack stack = slot.alternatives().get(altIdx);
                    g.renderItem(stack, sx + 1, sy + 1);
                    if (isHovering(mouseX, mouseY, sx, sy, 18, 18)) {
                        g.renderTooltip(font, stack, mouseX, mouseY);
                    }
                }
                slotIdx++;
            }

            RecipeType<?> rType = recipe.value().getType();
            if (layout.backgroundTexture() == null) {
                boolean furnace = !layout.inputs().isEmpty()
                        && RecipeDisplayHelper.isFurnaceType(rType);
                int arrowX = rx + layout.arrowX();
                int arrowY = ry + layout.arrowY();
                if (furnace) {
                    drawAnimatedArrow(g, arrowX, arrowY);
                } else {
                    drawArrow(g, arrowX, arrowY);
                }
            } else {
                long elapsed = System.currentTimeMillis() - animStart;

                if (RecipeDisplayHelper.isFurnaceType(rType)) {
                    float flameProgress = 1.0f - ((elapsed % 4000) / 4000f);
                    int flameHeight = (int) (14 * flameProgress);
                    if (flameHeight > 0) {
                        g.blit(layout.backgroundTexture(),
                                rx + 37, ry + 24 + (14 - flameHeight), 14, flameHeight,
                                176, 14 - flameHeight, 14, flameHeight, 256, 256);
                    }

                    float cookProgress = (elapsed % 2000) / 2000f;
                    int arrowWidth = (int) (24 * cookProgress);
                    if (arrowWidth > 0) {
                        g.blit(layout.backgroundTexture(),
                                rx + 60, ry + 22, arrowWidth, 17,
                                176, 14, arrowWidth, 17, 256, 256);
                    }
                } else if (rType.toString().equals("ami:brewing")) {
                    float bubbleProgress = (elapsed % 1500) / 1500f;
                    int bubbleHeight = (int) (29 * bubbleProgress);
                    if (bubbleHeight > 0) {
                        g.blit(layout.backgroundTexture(),
                                rx + 75, ry + 4 + (29 - bubbleHeight), 12, bubbleHeight,
                                185, 29 - bubbleHeight, 12, bubbleHeight, 256, 256);
                    }

                    float brewProgress = (elapsed % 3000) / 3000f;
                    int brewHeight = (int) (28 * brewProgress);
                    if (brewHeight > 0) {
                        g.blit(layout.backgroundTexture(),
                                rx + 109, ry + 6, 9, brewHeight,
                                176, 0, 9, brewHeight, 256, 256);
                    }
                } else if (rType == RecipeType.STONECUTTING) {
                    int frame = (int) ((elapsed / 100) % 2);
                    int srcX = 176 + frame * 16;
                    g.blit(layout.backgroundTexture(),
                            rx + 39, ry + 17, 16, 16,
                            srcX, 0, 16, 16, 256, 256);
                } else if (rType.toString().equals("ami:composting")) {
                    if (recipe.value() instanceof com.sanhiruzu.ami.recipe.special.CompostingRecipeView cr) {
                        String chanceStr = String.format(java.util.Locale.ROOT, "%.0f%%", cr.getChance() * 100);
                        g.drawString(font, chanceStr, rx + 32, ry + 5, 0xFFFFFFFF, false);
                        g.drawString(font, "x7", rx + 74, ry + 5, COL_TEXT_NAV, false);
                        drawArrow(g, rx + 46, ry + 5);
                    }
                } else if (rType.toString().equals("ami:fuel")) {
                    if (recipe.value() instanceof com.sanhiruzu.ami.recipe.special.FuelRecipeView fr) {
                        float itemsSmelted = fr.getTime() / 200f;
                        String fuelStr = String.format(java.util.Locale.ROOT, "smelts %.1f items", itemsSmelted);
                        g.drawString(font, fuelStr, rx + 38, ry + 5, 0xFFFFFFFF, false);

                        g.blit(Services.PLATFORM.rl("minecraft", "textures/gui/container/furnace.png"),
                                rx + 12, ry + 2, 14, 14,
                                176, 0, 14, 14, 256, 256);
                    }
                }
            }

            int outX = rx + layout.outputX();
            int outY = ry + layout.outputY();
            if (layout.drawSlotBackground()) {
                drawSlot(g, outX, outY);
            }
            if (!layout.output().isEmpty()) {
                g.renderItem(layout.output(), outX + 1, outY + 1);
                if (isHovering(mouseX, mouseY, outX, outY, 18, 18)) {
                    g.renderTooltip(font, layout.output(), mouseX, mouseY);
                }
            }

            boolean recipeCanTransfer = RecipeViewerBridge.canTransferRecipe(recipe, parentScreen);
            if (recipeCanTransfer) {
                int btnX = cardX + cardW - 22;
                int btnY = cardY + (cardH - 14) / 2;
                boolean bHovered = isHovering(mouseX, mouseY, btnX, btnY, 18, 14);
                AMITheme.fillRounded(g, btnX, btnY, 18, 14, bHovered ? COL_BTN_HOVER : COL_BTN_IDLE);
                g.drawCenteredString(font, Component.translatable("ami.recipe_viewer.transfer_icon"),
                        btnX + 9, btnY + 2, 0xFFFFFFFF);
                if (bHovered) {
                    g.renderTooltip(font,
                            Component.translatable("ami.recipe_viewer.transfer"), mouseX, mouseY);
                }
            }
        }

        if (totalPages > 1) {
            drawPageNav(g, mouseX, mouseY, pageIndex + 1, totalPages);
        }
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private void drawNoRecipes(GuiGraphics g) {
        g.drawCenteredString(font,
                Component.translatable("ami.recipe_viewer.no_recipes"),
                guiLeft + GUI_WIDTH / 2,
                guiTop + guiHeight / 2 - font.lineHeight,
                COL_TEXT_CAT);
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private void drawPageNav(GuiGraphics g, int mouseX, int mouseY, int current, int total) {
        int navY = guiTop + guiHeight - FOOTER_H - 8;
        int navCX = guiLeft + GUI_WIDTH / 2;

        boolean prevHov = isHovering(mouseX, mouseY, navCX - 40, navY - 2, 14, 12);
        boolean nextHov = isHovering(mouseX, mouseY, navCX + 26, navY - 2, 14, 12);

        g.drawString(font, Component.translatable("ami.recipe_viewer.prev"),
                navCX - 36, navY, prevHov ? 0xFFFFFFFF : COL_TEXT_NAV, false);
        g.drawString(font, Component.translatable("ami.recipe_viewer.next"),
                navCX + 28, navY, nextHov ? 0xFFFFFFFF : COL_TEXT_NAV, false);

        Component counter = Component.translatable("ami.recipe_viewer.page", current, total);
        g.drawCenteredString(font, counter, navCX, navY, COL_TEXT_NAV);
    }

    private void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, COL_SLOT_BORDER);
        g.fill(x + 1, y + 1, x + 17, y + 17, COL_SLOT_BG);
    }

    // ── Page nav ──────────────────────────────────────────────────────────────

    private void drawArrow(GuiGraphics g, int x, int y) {
        int c = COL_ARROW;
        g.fill(x, y + 3, x + 6, y + 6, c);
        g.fill(x + 4, y + 1, x + 5, y + 8, c);
        g.fill(x + 5, y + 2, x + 6, y + 7, c);
        g.fill(x + 6, y + 3, x + 7, y + 6, c);
        g.fill(x + 7, y + 4, x + 8, y + 5, c);
    }

    // ── Slot + arrow drawing ──────────────────────────────────────────────────

    private void drawAnimatedArrow(GuiGraphics g, int x, int y) {
        long elapsed = System.currentTimeMillis() - animStart;
        float progress = (elapsed % 2500) / 2500f;
        int shaft = (int) (20 * progress);
        if (shaft > 0) g.fill(x, y + 3, x + shaft, y + 6, COL_ARROW_ANIM);
        g.fill(x, y + 3, x + 20, y + 4, COL_ARROW);
        g.fill(x, y + 5, x + 20, y + 6, COL_ARROW);
        drawArrow(g, x + 20, y);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (Services.PLATFORM.keyMappings().recipeBack().isActiveAndMatches(
                com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode))
                || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            goBack();
            return true;
        }
        if (tabs.isEmpty()) return super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (hasShiftDown()) prevPage();
            else prevTab();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (hasShiftDown()) nextPage();
            else nextTab();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (canTransfer) {
                doTransfer();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // NeoForge 1.21.1+: 4-param signature (scrollX added by NeoForge); no @Override — Forge's Screen still uses 3-param
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return handleScroll(mouseX, mouseY, scrollY);
    }

    // ── Input handling ────────────────────────────────────────────────────────

    // Forge 1.20.1 compat — 3-param version; no @Override since that signature doesn't exist in NeoForge 1.21.1
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return handleScroll(mouseX, mouseY, scrollY);
    }

    private boolean handleScroll(double mouseX, double mouseY, double scrollY) {
        if (tabs.isEmpty()) return false;
        Tab tab = tabs.get(selectedTab);
        int tabSize = tab.recipes().size();
        if (tabSize == 0) return false;

        int startIdx = pageIndex * recipesPerPage;
        int endIdx = Math.min(startIdx + recipesPerPage, tabSize);

        int cardW = GUI_WIDTH - 8;
        int cardH = 70;
        int cardX = guiLeft + 4;

        if (scrollY != 0) {
            for (int i = startIdx; i < endIdx; i++) {
                AmiRecipeHolder<?> recipe = tab.recipes().get(i);
                RecipeLayout layout = RecipeDisplayHelper.getLayout(recipe, minecraft.level.registryAccess());
                if (layout == null) continue;

                int cardY = guiTop + CONTENT_Y + (i - startIdx) * 74 + (recipesPerPage == 1 ? 22 : 0);
                int rx = cardX + 24;

                int layoutHeight;
                if (layout.backgroundTexture() != null) {
                    layoutHeight = layout.bgRenderY() + layout.bgH();
                } else {
                    layoutHeight = 14 + layout.gridHeight() * 18;
                }
                int yOffset = (cardH - layoutHeight) / 2;
                int ry = cardY + yOffset;

                int slotIdx = 0;
                for (SlotPosition slot : layout.inputs()) {
                    if (slot.alternatives().size() <= 1) {
                        slotIdx++;
                        continue;
                    }
                    int sx = rx + slot.x();
                    int sy = ry + slot.y();
                    if (isHovering(mouseX, mouseY, sx, sy, 18, 18)) {
                        String slotKey = recipe.id().toString() + "_" + slotIdx;
                        int cur = slotOffsets.getOrDefault(slotKey, 0);
                        int size = slot.alternatives().size();
                        slotOffsets.put(slotKey, ((cur + (scrollY > 0 ? 1 : -1)) % size + size) % size);
                        return true;
                    }
                    slotIdx++;
                }
            }
        }

        int totalPages = (int) Math.ceil((double) tabSize / recipesPerPage);
        if (totalPages > 1) {
            if (scrollY > 0) prevPage();
            else if (scrollY < 0) nextPage();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tabs.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);

        Tab tab = tabs.get(selectedTab);
        int tabSize = tab.recipes().size();

        int barY = guiTop + TAB_BAR_Y;
        int tabW = Math.min(GUI_WIDTH / tabs.size(), 72);
        int totalW = tabW * tabs.size();
        int startX = guiLeft + (GUI_WIDTH - totalW) / 2;
        if (mouseY >= barY && mouseY <= barY + TAB_H) {
            int idx = (int) ((mouseX - startX) / tabW);
            if (idx >= 0 && idx < tabs.size()) {
                selectedTab = idx;
                pageIndex = 0;
                refreshLayout();
                return true;
            }
        }

        int totalPages = (int) Math.ceil((double) tabSize / recipesPerPage);
        int startIdx = pageIndex * recipesPerPage;
        int endIdx = Math.min(startIdx + recipesPerPage, tabSize);

        int cardW = GUI_WIDTH - 8;
        int cardH = 70;
        int cardX = guiLeft + 4;

        for (int i = startIdx; i < endIdx; i++) {
            AmiRecipeHolder<?> recipe = tab.recipes().get(i);
            RecipeLayout layout = RecipeDisplayHelper.getLayout(recipe, minecraft.level.registryAccess());
            if (layout == null) continue;

            int cardY = guiTop + CONTENT_Y + (i - startIdx) * 74 + (recipesPerPage == 1 ? 22 : 0);
            int rx = cardX + 24;

            int layoutHeight;
            if (layout.backgroundTexture() != null) {
                layoutHeight = layout.bgRenderY() + layout.bgH();
            } else {
                layoutHeight = 14 + layout.gridHeight() * 18;
            }
            int yOffset = (cardH - layoutHeight) / 2;
            int ry = cardY + yOffset;

            boolean recipeCanTransfer = RecipeViewerBridge.canTransferRecipe(recipe, parentScreen);
            if (!layout.output().isEmpty() && button == 0) {
                int favX = cardX + 6;
                int favY = cardY + 4;
                if (isHovering(mouseX, mouseY, favX, favY, 14, 14)) {
                    AmiFavoritesHandler favorites = AmiFavoritesHandler.getInstance();
                    if (favorites.isRecipeFavorite(recipe.id(), layout.output())) {
                        favorites.removeRecipeFavorite(recipe.id(), layout.output());
                    } else {
                        favorites.addRecipeFavorite(recipe.id(), layout.output());
                    }
                    return true;
                }
            }

            if (recipeCanTransfer && button == 0) {
                int btnX = cardX + cardW - 22;
                int btnY = cardY + (cardH - 14) / 2;
                if (isHovering(mouseX, mouseY, btnX, btnY, 18, 14)) {
                    doTransfer(recipe, hasShiftDown());
                    return true;
                }
            }

            if (button == 0 || button == 1) {
                int slotIdx = 0;
                for (SlotPosition slot : layout.inputs()) {
                    int sx = rx + slot.x();
                    int sy = ry + slot.y();
                    if (isHovering(mouseX, mouseY, sx, sy, 18, 18) && !slot.alternatives().isEmpty()) {
                        String slotKey = recipe.id().toString() + "_" + slotIdx;
                        int altIdx = getSlotAltIndex(slotKey, slot.alternatives().size());
                        ItemStack clicked = slot.alternatives().get(altIdx);
                        if (!clicked.isEmpty()) {
                            navigateTo(clicked, button == 0);
                        }
                        return true;
                    }
                    slotIdx++;
                }
            }

            if (button == 0 || button == 1) {
                int outX = rx + layout.outputX();
                int outY = ry + layout.outputY();
                if (isHovering(mouseX, mouseY, outX, outY, 18, 18) && !layout.output().isEmpty()) {
                    if (recipeCanTransfer && hasShiftDown() && button == 0) {
                        doTransfer(recipe, true);
                    } else {
                        navigateTo(layout.output(), button == 0);
                    }
                    return true;
                }
            }
        }

        if (totalPages > 1) {
            int navY = guiTop + guiHeight - FOOTER_H - 8;
            int navCX = guiLeft + GUI_WIDTH / 2;
            if (mouseY >= navY - 2 && mouseY <= navY + 12) {
                if (mouseX >= navCX - 40 && mouseX <= navCX - 26) {
                    prevPage();
                    return true;
                }
                if (mouseX >= navCX + 26 && mouseX <= navCX + 40) {
                    nextPage();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void prevTab() {
        selectedTab = (selectedTab - 1 + tabs.size()) % tabs.size();
        pageIndex = 0;
        refreshLayout();
    }

    private void nextTab() {
        selectedTab = (selectedTab + 1) % tabs.size();
        pageIndex = 0;
        refreshLayout();
    }

    // ── Navigation helpers ────────────────────────────────────────────────────

    private void prevPage() {
        Tab tab = tabs.get(selectedTab);
        int totalRecipes = tab.recipes().size();
        int totalPages = (int) Math.ceil((double) totalRecipes / recipesPerPage);
        if (totalPages > 1) {
            pageIndex = (pageIndex - 1 + totalPages) % totalPages;
            refreshLayout();
        }
    }

    private void nextPage() {
        Tab tab = tabs.get(selectedTab);
        int totalRecipes = tab.recipes().size();
        int totalPages = (int) Math.ceil((double) totalRecipes / recipesPerPage);
        if (totalPages > 1) {
            pageIndex = (pageIndex + 1) % totalPages;
            refreshLayout();
        }
    }

    private void doTransfer() {
        if (minecraft == null) return;
        Tab tab = tabs.get(selectedTab);
        int firstVisibleIdx = pageIndex * recipesPerPage;
        if (firstVisibleIdx < tab.recipes().size()) {
            doTransfer(tab.recipes().get(firstVisibleIdx), hasShiftDown());
        }
    }

    private void doTransfer(AmiRecipeHolder<?> recipe) {
        doTransfer(recipe, false);
    }

    private void doTransfer(AmiRecipeHolder<?> recipe, boolean maxTransfer) {
        if (minecraft == null || recipe == null) return;
        if (RecipeViewerBridge.transferRecipe(recipe, parentScreen, minecraft, maxTransfer, false)) {
            onClose();
        }
    }

    @Override
    public void onClose() {
        RecipeViewerBridge.clearRecipeView();
        InventoryOverlayHandler.setAmiEnabled(shouldRestoreAmiEnabled);
        if (minecraft != null) minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private int getSlotAltIndex(String slotKey, int size) {
        int timeIdx = (int) (System.currentTimeMillis() / 1000 % size);
        return ((timeIdx + slotOffsets.getOrDefault(slotKey, 0)) % size + size) % size;
    }

    private boolean isHovering(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private record HistoryEntry(ItemStack target, boolean showRecipes, int selectedTab, int pageIndex,
                                RecipeType<?> focusType) {
    }

    private record Tab(RecipeType<?> type, Component label, String shortLabel, List<AmiRecipeHolder<?>> recipes) {
    }
}
