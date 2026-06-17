package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.RecipeLayout;
import com.sanhiruzu.ami.client.recipe.RecipeDisplayHelper.SlotPosition;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecipeViewerScreen extends Screen {

    // ── Sprite sheet (per-theme, same UV layout) ────────────────────────────
    private static final ResourceLocation WIDGETS_VANILLA     = Services.PLATFORM.rl("ami", "textures/gui/recipe_viewer_vanilla.png");
    private static final ResourceLocation WIDGETS_MODERN      = Services.PLATFORM.rl("ami", "textures/gui/recipe_viewer_modern.png");
    private static final ResourceLocation WIDGETS_TRANSPARENT = Services.PLATFORM.rl("ami", "textures/gui/recipe_viewer_transparent.png");

    private static ResourceLocation widgets() {
        return switch (com.sanhiruzu.ami.config.AmiConfig.theme) {
            case VANILLA     -> WIDGETS_VANILLA;
            case TRANSPARENT -> WIDGETS_TRANSPARENT;
            default          -> WIDGETS_MODERN;
        };
    }
    // UV offsets in WIDGETS (256×256) — sprites are pixel-accurate copies of JEI's textures
    private static final int UV_SLOT_X      =  0, UV_SLOT_Y      =  0; // 18×18 input slot
    private static final int UV_OUT_X       = 18, UV_OUT_Y       =  0; // 26×26 output slot (JEI output_slot.png)
    private static final int UV_ARROW_X     = 44, UV_ARROW_Y     =  0; // 22×16 crafting arrow
    private static final int UV_TAB_SEL_X   =  0, UV_TAB_SEL_Y   = 26; // 24×24 tab selected
    private static final int UV_TAB_UNS_X   = 24, UV_TAB_UNS_Y   = 26; // 24×24 tab unselected
    private static final int UV_BTN_PREV_X  = 48, UV_BTN_PREV_Y  = 26; //  9×9  arrow prev
    private static final int UV_BTN_NEXT_X  = 57, UV_BTN_NEXT_Y  = 26; //  9×9  arrow next

    // ── Layout constants ────────────────────────────────────────────────────
    private static final int GUI_WIDTH        = 240;
    private static final int HEADER_H         = 32;   // two 13px nav button rows + padding
    private static final int TAB_H            = 24;   // JEI TAB_HEIGHT
    private static final int TAB_W            = 24;   // JEI TAB_WIDTH
    private static final int TAB_GUI_OVERLAP  = 3;    // tabs dip into panel top edge by this much
    private static final int TAB_ARROW_W      = 14;   // scroll arrow for overflow
    private static final int SMALL_BTN_W      = 13;   // JEI smallButtonWidth
    private static final int SMALL_BTN_H      = 13;   // JEI smallButtonHeight
    private static final int BORDER_PAD       = 6;    // JEI borderPadding
    private static final int NAV_PAD          = 2;    // JEI navBarPadding
    private static final int CONTENT_Y        = HEADER_H + 4; // 36
    private static final int FOOTER_H         = 18;
    private static final int CHROME_OVERHEAD  = CONTENT_Y + 8 + FOOTER_H; // 62

    // ── Palette (synchronized from AMITheme in render) ──────────────────────
    private static int COL_BG_OVERLAY      = 0xFF101010;
    private static int COL_PANEL           = 0xFF1A1A1F;
    private static int COL_PANEL_INNER     = 0xFF22222A;
    private static int COL_BORDER          = 0xFF3A3A4A;
    private static int COL_HEADER_LINE     = 0xFF2E2E3A;
    private static int COL_TAB_ACTIVE      = 0xFF4488FF;
    private static int COL_TAB_HOVER       = 0xFF2E2E44;
    private static int COL_TAB_IDLE        = 0xFF1E1E28;
    private static int COL_TAB_TEXT_A      = 0xFFFFFFFF;
    private static int COL_TAB_TEXT_I      = 0xFF8888AA;
    private static int COL_SLOT_BORDER     = 0xFF555566;
    private static int COL_SLOT_BG         = 0xFF2A2A36;
    private static int COL_ARROW           = 0xFF6688CC;
    private static int COL_ARROW_ANIM      = 0xFF4466AA;
    private static int COL_TEXT_TITLE      = 0xFFFFFFFF;
    private static int COL_TEXT_ITEM       = 0xFFBBBBCC;
    private static int COL_TEXT_CAT        = 0xFF8888AA;
    private static int COL_TEXT_NAV        = 0xFF8888AA;
    private static int COL_TEXT_FOOTER     = 0xFF555566;
    private static int COL_BTN_IDLE        = 0xFF226622;
    private static int COL_BTN_HOVER       = 0xFF44AA44;
    private static int COL_SHAPELESS       = 0xFF5555AA;
    // New palette entries
    private static int COL_WORKSTATION_BG       = 0xFF181820;
    private static int COL_WORKSTATION_TEXT      = 0xFF6666AA;
    private static int COL_TAB_ARROW            = 0xFF6688CC;
    private static int COL_TAB_ARROW_HOVER      = 0xFF4488FF;
    private static int COL_COUNT_BADGE          = 0xFF444466;
    private static int COL_COUNT_BADGE_TEXT     = 0xFF8888BB;
    private static int COL_OUTPUT_SLOT_BORDER   = 0xFF886633;

    private final Screen parentScreen;
    private final List<HistoryEntry> history = new ArrayList<>();
    private final java.util.Map<String, Integer> slotOffsets = new java.util.HashMap<>();
    private final boolean shouldRestoreAmiEnabled;

    private int guiHeight      = 204;
    private int recipesPerPage = 1;
    private int currentCardH   = 70;   // computed from recipe layout, drives panel sizing

    // ── State ─────────────────────────────────────────────────────────────
    private ItemStack target;
    private boolean showRecipes;
    private RecipeType<?> focusType = null;
    private List<Tab> tabs = List.of();
    private int selectedTab;
    private int tabScrollOffset = 0;
    private int pageIndex;
    private int guiLeft, guiTop;
    private RecipeLayout currentLayout;
    private List<RecipeLayout> cachedLayouts = new ArrayList<>();
    private boolean loggedLayoutCacheDrift;
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

    // ── Init ──────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        guiLeft   = (width  - GUI_WIDTH) / 2;
        animStart = System.currentTimeMillis();

        if (tabs.isEmpty()) {
            rebuildTabs();
        } else {
            recomputePanelSize();
            refreshLayout();
        }
        recomputePanelSize();
    }

    // Returns the natural card height for a recipe layout.
    private static int getCardHeight(RecipeLayout layout) {
        if (layout == null) return 70;
        int natural;
        if (layout.backgroundTexture() != null) {
            natural = layout.bgRenderY() + layout.bgH() + 10;
        } else {
            natural = 14 + layout.gridHeight() * 18 + 10;
        }
        return Math.max(44, natural);
    }

    // Fixes panel geometry (guiHeight, guiTop) from screen size — called only at init/resize,
    // never from recipe navigation. This keeps the panel from jumping size as recipes change.
    private void recomputePanelSize() {
        int screenH = minecraft != null ? minecraft.getWindow().getGuiScaledHeight() : height;
        // Match JEI: height = screen - 76, clamped to [minH, 420]
        int minH = CHROME_OVERHEAD + 102;   // enough for one standard recipe card
        this.guiHeight = Math.max(minH, Math.min(screenH - 76, 420));
        this.guiTop    = (screenH - guiHeight) / 2;
        recomputeRecipesPerPage();
    }

    // Recomputes only recipesPerPage from the fixed panel height. Safe to call on tab/recipe change.
    private void recomputeRecipesPerPage() {
        int slotH = currentCardH + 4;
        int avail = guiHeight - CHROME_OVERHEAD - 16;
        this.recipesPerPage = Math.max(1, Math.min(3, avail / slotH));
    }

    public com.sanhiruzu.ami.compat.RecipeViewerBridge.RecipeViewerBounds getViewerBounds() {
        int effectiveTop = guiTop - TAB_H + TAB_GUI_OVERLAP;
        int totalH = guiHeight + (guiTop - effectiveTop);
        // Extend left to cover the workstation panel (28px wide, 4px overlaps main panel)
        boolean hasCatalysts = !tabs.isEmpty() && selectedTab < tabs.size()
                && !tabs.get(selectedTab).workstations().isEmpty();
        int catalystPad = hasCatalysts ? 24 : 0;
        return new com.sanhiruzu.ami.compat.RecipeViewerBridge.RecipeViewerBounds(
                guiLeft - catalystPad, effectiveTop, GUI_WIDTH + catalystPad, totalH);
    }

    private void rebuildTabs() {
        tabs = List.of();
        tabScrollOffset = 0;
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
                        entry.getValue(),
                        RecipeDisplayHelper.getRepresentativeWorkstation(type),
                        RecipeDisplayHelper.getWorkstations(type)));
            }
            tabs = list;
        }

        selectedTab = 0;
        pageIndex   = 0;
        refreshLayout();
    }

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

    // ── Navigation ────────────────────────────────────────────────────────

    private void navigateTo(ItemStack newTarget, boolean newShowRecipes) {
        if (newTarget.isEmpty()) return;
        history.add(new HistoryEntry(target, showRecipes, selectedTab, pageIndex, focusType));
        this.target      = newTarget;
        this.showRecipes = newShowRecipes;
        this.focusType   = null;
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
            this.target      = entry.target();
            this.showRecipes = entry.showRecipes();
            this.focusType   = entry.focusType();
            rebuildTabs();

            this.selectedTab = Math.min(entry.selectedTab(), Math.max(0, tabs.size() - 1));
            ensureSelectedTabVisible();
            if (selectedTab >= 0 && selectedTab < tabs.size()) {
                int total = tabs.get(selectedTab).recipes().size();
                int pages = Math.max(1, (int) Math.ceil((double) total / recipesPerPage));
                this.pageIndex = Math.min(entry.pageIndex(), pages - 1);
            }
            refreshLayout();
        } else {
            onClose();
        }
    }

    private void refreshLayout() {
        canTransfer = false;
        slotOffsets.clear();
        currentLayout = null;
        cachedLayouts = new ArrayList<>();
        loggedLayoutCacheDrift = false;

        if (!tabs.isEmpty() && minecraft != null && minecraft.level != null) {
            Tab tab = tabs.get(selectedTab);
            if (!tab.recipes().isEmpty()) {
                int totalRecipes = tab.recipes().size();
                int totalPages   = Math.max(1, (int) Math.ceil((double) totalRecipes / recipesPerPage));
                pageIndex = Math.min(Math.max(pageIndex, 0), totalPages - 1);

                int firstIdx = pageIndex * recipesPerPage;
                if (firstIdx < totalRecipes) {
                    currentLayout = RecipeDisplayHelper.getLayout(
                            tab.recipes().get(firstIdx), minecraft.level.registryAccess());
                    canTransfer = RecipeViewerBridge.canTransferRecipe(
                            tab.recipes().get(firstIdx), parentScreen);
                }
            }
        }

        // Recompute recipesPerPage if card height changed — panel geometry stays fixed.
        int newCardH = getCardHeight(currentLayout);
        if (newCardH != currentCardH) {
            currentCardH = newCardH;
            recomputeRecipesPerPage();
        }

        // Cache layouts for all visible recipes using the now-final recipesPerPage value.
        if (!tabs.isEmpty() && minecraft != null && minecraft.level != null) {
            Tab tab = tabs.get(selectedTab);
            int firstIdx = pageIndex * recipesPerPage;
            int endIdx   = Math.min(firstIdx + recipesPerPage, tab.recipes().size());
            for (int i = firstIdx; i < endIdx; i++) {
                cachedLayouts.add(RecipeDisplayHelper.getLayout(
                        tab.recipes().get(i), minecraft.level.registryAccess()));
            }
        }
    }

    // ── Tab helpers ───────────────────────────────────────────────────────

    private boolean tabsOverflow() {
        return tabs.size() * TAB_W > GUI_WIDTH - 8;
    }

    private int visibleTabCount() {
        if (!tabsOverflow()) return tabs.size();
        return (GUI_WIDTH - 8 - 2 * TAB_ARROW_W - 2) / TAB_W;
    }

    private void ensureSelectedTabVisible() {
        int vis    = visibleTabCount();
        int maxOff = Math.max(0, tabs.size() - vis);
        if (selectedTab < tabScrollOffset) {
            tabScrollOffset = selectedTab;
        } else if (selectedTab >= tabScrollOffset + vis) {
            tabScrollOffset = selectedTab - vis + 1;
        }
        tabScrollOffset = Math.max(0, Math.min(tabScrollOffset, maxOff));
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        AMITheme.sync();
        syncPalette();

        g.fill(0, 0, width, height, COL_BG_OVERLAY);
        drawPanel(g);
        drawTabBar(g, mouseX, mouseY);
        drawHeader(g, mouseX, mouseY);

        if (tabs.isEmpty()) {
            drawNoRecipes(g);
        } else {
            drawContent(g, mouseX, mouseY);
        }

        drawCentered(g,
                Component.translatable("ami.recipe_viewer.go_back"),
                guiLeft + GUI_WIDTH / 2,
                guiTop + guiHeight - FOOTER_H + 4,
                COL_TEXT_FOOTER);

        drawWorkstationPanel(g, mouseX, mouseY);

        // Category title hover tooltip — must render last so it's on top
        if (!tabs.isEmpty() && focusType == null) {
            Component catTitle = tabs.get(selectedTab).label();
            int titleH = font.lineHeight + BORDER_PAD;
            int ty = guiTop + (titleH - font.lineHeight) / 2 + 1;
            int tw = font.width(catTitle);
            int tx = guiLeft + GUI_WIDTH / 2 - tw / 2;
            if (isHovering(mouseX, mouseY, tx, ty, tw, font.lineHeight)) {
                g.renderTooltip(font,
                        Component.translatable("ami.recipe_viewer.category_show_all"), mouseX, mouseY);
            }
        }
    }

    private void syncPalette() {
        COL_BG_OVERLAY          = AMITheme.RECIPE_BG_OVERLAY;
        COL_PANEL               = AMITheme.RECIPE_PANEL;
        COL_PANEL_INNER         = AMITheme.RECIPE_PANEL_INNER;
        COL_BORDER              = AMITheme.RECIPE_BORDER;
        COL_HEADER_LINE         = AMITheme.RECIPE_HEADER_LINE;
        COL_TAB_ACTIVE          = AMITheme.RECIPE_TAB_ACTIVE;
        COL_TAB_HOVER           = AMITheme.RECIPE_TAB_HOVER;
        COL_TAB_IDLE            = AMITheme.RECIPE_TAB_IDLE;
        COL_TAB_TEXT_A          = AMITheme.RECIPE_TAB_TEXT_A;
        COL_TAB_TEXT_I          = AMITheme.RECIPE_TAB_TEXT_I;
        COL_SLOT_BORDER         = AMITheme.RECIPE_SLOT_BORDER;
        COL_SLOT_BG             = AMITheme.RECIPE_SLOT_BG;
        COL_ARROW               = AMITheme.RECIPE_ARROW;
        COL_ARROW_ANIM          = AMITheme.RECIPE_ARROW_ANIM;
        COL_TEXT_TITLE          = AMITheme.RECIPE_TEXT_TITLE;
        COL_TEXT_ITEM           = AMITheme.RECIPE_TEXT_ITEM;
        COL_TEXT_CAT            = AMITheme.RECIPE_TEXT_CAT;
        COL_TEXT_NAV            = AMITheme.RECIPE_TEXT_NAV;
        COL_TEXT_FOOTER         = AMITheme.RECIPE_TEXT_FOOTER;
        COL_BTN_IDLE            = AMITheme.RECIPE_BTN_IDLE;
        COL_BTN_HOVER           = AMITheme.RECIPE_BTN_HOVER;
        COL_SHAPELESS           = AMITheme.RECIPE_SHAPELESS;
        COL_WORKSTATION_BG      = AMITheme.RECIPE_WORKSTATION_BG;
        COL_WORKSTATION_TEXT    = AMITheme.RECIPE_WORKSTATION_TEXT;
        COL_TAB_ARROW           = AMITheme.RECIPE_TAB_ARROW;
        COL_TAB_ARROW_HOVER     = AMITheme.RECIPE_TAB_ARROW_HOVER;
        COL_COUNT_BADGE         = AMITheme.RECIPE_COUNT_BADGE;
        COL_COUNT_BADGE_TEXT    = AMITheme.RECIPE_COUNT_BADGE_TEXT;
        COL_OUTPUT_SLOT_BORDER  = AMITheme.RECIPE_OUTPUT_SLOT_BORDER;
    }

    // ── Panel chrome ──────────────────────────────────────────────────────

    private void drawPanel(GuiGraphics g) {
        int x = guiLeft, y = guiTop, w = GUI_WIDTH, h = guiHeight;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, COL_BORDER);
        g.fill(x, y, x + w, y + h, COL_PANEL);
        g.fill(x, y, x + w, y + 1, COL_TAB_ACTIVE);

        int contentTop = guiTop + CONTENT_Y - 2;
        int contentBot = guiTop + guiHeight - FOOTER_H - 2;
        g.fill(guiLeft + 4, contentTop, guiLeft + GUI_WIDTH - 4, contentBot, COL_PANEL_INNER);
    }

    private void drawHeader(GuiGraphics g, int mx, int my) {
        // JEI-style: two nav button rows inside the header
        // Row 1 y: titleH - SMALL_BTN_H + NAV_PAD  (= 15 - 13 + 2 = 4)
        int titleH = font.lineHeight + BORDER_PAD;
        int btnY1  = guiTop + titleH - SMALL_BTN_H + NAV_PAD;
        int btnL   = guiLeft + BORDER_PAD;
        int btnR   = guiLeft + GUI_WIDTH - BORDER_PAD - SMALL_BTN_W;

        if (tabs.isEmpty()) {
            Component modeLabel = showRecipes
                    ? Component.translatable("ami.recipe_viewer.mode.recipes")
                    : Component.translatable("ami.recipe_viewer.mode.uses");
            drawCentered(g, modeLabel, guiLeft + GUI_WIDTH / 2, guiTop + (titleH - font.lineHeight) / 2 + 1, COL_TEXT_TITLE);
            drawCentered(g, target.getHoverName(),
                    guiLeft + GUI_WIDTH / 2, btnY1 + (SMALL_BTN_H - font.lineHeight) / 2, COL_TEXT_ITEM);
        } else {
            // Row 1: category prev/next + category title
            boolean canPrevCat = selectedTab > 0;
            boolean canNextCat = selectedTab < tabs.size() - 1;
            drawNavBtn(g, mx, my, btnL, btnY1, true,  canPrevCat);
            drawNavBtn(g, mx, my, btnR, btnY1, false, canNextCat);
            g.fill(btnL + SMALL_BTN_W, btnY1, btnR, btnY1 + SMALL_BTN_H, 0x30000000);
            drawCentered(g, tabs.get(selectedTab).label(),
                    guiLeft + GUI_WIDTH / 2, guiTop + (titleH - font.lineHeight) / 2 + 1, COL_TEXT_TITLE);

            // Row 2: page prev/next + page number (or item name if single page)
            int btnY2 = btnY1 + SMALL_BTN_H + NAV_PAD;
            Tab tab = tabs.get(selectedTab);
            int totalPages = (int) Math.ceil((double) tab.recipes().size() / Math.max(1, recipesPerPage));
            if (totalPages > 1) {
                drawNavBtn(g, mx, my, btnL, btnY2, true,  pageIndex > 0);
                drawNavBtn(g, mx, my, btnR, btnY2, false, pageIndex < totalPages - 1);
                g.fill(btnL + SMALL_BTN_W, btnY2, btnR, btnY2 + SMALL_BTN_H, 0x30000000);
                drawCentered(g, Component.translatable("ami.recipe_viewer.page", pageIndex + 1, totalPages),
                        guiLeft + GUI_WIDTH / 2, btnY2 + (SMALL_BTN_H - font.lineHeight) / 2, COL_TEXT_NAV);
            } else {
                drawCentered(g, target.getHoverName(),
                        guiLeft + GUI_WIDTH / 2, btnY2 + (SMALL_BTN_H - font.lineHeight) / 2, COL_TEXT_ITEM);
            }
        }

        g.fill(guiLeft, guiTop + HEADER_H, guiLeft + GUI_WIDTH, guiTop + HEADER_H + 1, COL_HEADER_LINE);
    }

    private void drawNavBtn(GuiGraphics g, int mx, int my, int x, int y, boolean isPrev, boolean active) {
        if (!active) {
            drawCentered(g, isPrev ? "❮" : "❯",
                    x + SMALL_BTN_W / 2, y + (SMALL_BTN_H - font.lineHeight) / 2, COL_TAB_TEXT_I);
            return;
        }
        int uvX = isPrev ? UV_BTN_PREV_X : UV_BTN_NEXT_X;
        int uvY = isPrev ? UV_BTN_PREV_Y : UV_BTN_NEXT_Y;
        boolean hov = isHovering(mx, my, x, y, SMALL_BTN_W, SMALL_BTN_H);
        int sx = x + (SMALL_BTN_W - 9) / 2;
        int sy = y + (SMALL_BTN_H - 9) / 2;
        g.blit(widgets(), sx, sy, 9, 9, uvX, uvY, 9, 9, 256, 256);
        if (hov) g.fill(x, y, x + SMALL_BTN_W, y + SMALL_BTN_H, 0x30FFFFFF);
    }

    // ── Tab bar (24×24 squares above the panel, JEI-style) ───────────────

    private int tabTopY() { return guiTop - TAB_H + TAB_GUI_OVERLAP; }

    private void drawTabBar(GuiGraphics g, int mouseX, int mouseY) {
        if (tabs.isEmpty()) return;
        int ty = tabTopY();
        int startX = guiLeft + 4;  // 4px inset — matches mouseClicked hit-test

        if (tabsOverflow()) {
            boolean canL = tabScrollOffset > 0;
            boolean canR = tabScrollOffset + visibleTabCount() < tabs.size();
            int arrowLX = startX;
            int arrowRX = guiLeft + GUI_WIDTH - 4 - TAB_ARROW_W;
            int arrowMidY = ty + (TAB_H - 9) / 2;
            if (canL) {
                g.blit(widgets(), arrowLX + (TAB_ARROW_W - 9) / 2, arrowMidY, 9, 9, UV_BTN_PREV_X, UV_BTN_PREV_Y, 9, 9, 256, 256);
            } else {
                drawCentered(g, "❮", arrowLX + TAB_ARROW_W / 2, ty + (TAB_H - font.lineHeight) / 2, COL_TAB_TEXT_I);
            }
            if (canR) {
                g.blit(widgets(), arrowRX + (TAB_ARROW_W - 9) / 2, arrowMidY, 9, 9, UV_BTN_NEXT_X, UV_BTN_NEXT_Y, 9, 9, 256, 256);
            } else {
                drawCentered(g, "❯", arrowRX + TAB_ARROW_W / 2, ty + (TAB_H - font.lineHeight) / 2, COL_TAB_TEXT_I);
            }
            int tabAreaX = arrowLX + TAB_ARROW_W + 1;
            int vis = visibleTabCount();
            for (int i = 0; i < vis; i++) {
                int tabIdx = tabScrollOffset + i;
                if (tabIdx >= tabs.size()) break;
                drawSingleTab(g, mouseX, mouseY, tabIdx, tabAreaX + i * TAB_W, ty);
            }
        } else {
            for (int i = 0; i < tabs.size(); i++) {
                drawSingleTab(g, mouseX, mouseY, i, startX + i * TAB_W, ty);
            }
        }
    }

    private void drawSingleTab(GuiGraphics g, int mx, int my, int tabIdx, int tx, int ty) {
        boolean active  = (tabIdx == selectedTab);
        boolean hovered = isHovering(mx, my, tx, ty, TAB_W, TAB_H);
        int uvX = active ? UV_TAB_SEL_X : UV_TAB_UNS_X;
        int uvY = active ? UV_TAB_SEL_Y : UV_TAB_UNS_Y;
        g.blit(widgets(), tx, ty, TAB_W, TAB_H, uvX, uvY, TAB_W, TAB_H, 256, 256);
        if (hovered && !active) g.fill(tx, ty, tx + TAB_W, ty + TAB_H, 0x30FFFFFF);

        ItemStack icon = tabs.get(tabIdx).icon();
        if (icon != null && !icon.isEmpty()) {
            g.renderItem(icon, tx + (TAB_W - 16) / 2, ty + (TAB_H - 16) / 2);
        } else {
            String label = tabs.get(tabIdx).shortLabel();
            g.drawString(font, label,
                    tx + (TAB_W - font.width(label)) / 2, ty + (TAB_H - font.lineHeight) / 2,
                    active ? COL_TAB_TEXT_A : COL_TAB_TEXT_I, false);
        }

        if (hovered) {
            List<Component> tabTip = new ArrayList<>();
            tabTip.add(tabs.get(tabIdx).label());
            tabTip.add(Component.translatable("ami.recipe_viewer.tab_count", tabs.get(tabIdx).recipes().size())
                    .withStyle(ChatFormatting.GRAY));
            g.renderTooltip(font, tabTip, java.util.Optional.empty(), mx, my);
        }
    }


    // ── Content ───────────────────────────────────────────────────────────

    private void drawNoRecipes(GuiGraphics g) {
        drawCentered(g,
                Component.translatable("ami.recipe_viewer.no_recipes"),
                guiLeft + GUI_WIDTH / 2,
                guiTop + guiHeight / 2 - font.lineHeight,
                COL_TEXT_CAT);
    }

    private void drawContent(GuiGraphics g, int mouseX, int mouseY) {
        Tab tab = tabs.get(selectedTab);
        int tabSize = tab.recipes().size();
        if (tabSize == 0) return;

        int totalPages = (int) Math.ceil((double) tabSize / recipesPerPage);
        int startIdx   = pageIndex * recipesPerPage;
        int endIdx     = Math.min(startIdx + recipesPerPage, tabSize);

        int cardW = GUI_WIDTH - 8;
        int cardH = currentCardH;
        int slotH = cardH + 4;                          // card height + gap
        int cardX = guiLeft + 4;
        int singleOffset = 6;                           // fixed top padding — JEI top-aligns recipes

        for (int i = startIdx; i < endIdx; i++) {
            AmiRecipeHolder<?> recipe = tab.recipes().get(i);
            RecipeLayout layout = getLayoutForDisplay(i - startIdx, recipe);
            if (layout == null) continue;

            int cardY = guiTop + CONTENT_Y + (i - startIdx) * slotH + singleOffset;

            g.fill(cardX - 1, cardY - 1, cardX + cardW + 1, cardY + cardH + 1, COL_BORDER);
            g.fill(cardX, cardY, cardX + cardW, cardY + cardH, COL_PANEL);

            int rx = cardX + 24;

            int layoutHeight = layout.backgroundTexture() != null
                    ? layout.bgRenderY() + layout.bgH()
                    : 14 + layout.gridHeight() * 18;
            int yOffset = Math.max(4, (cardH - layoutHeight) / 2);
            int ry = cardY + yOffset;

            if (layout.backgroundTexture() != null) {
                int bx = rx + layout.bgRenderX();
                int by = ry + layout.bgRenderY();
                int bw = layout.bgW();
                int bh = layout.bgH();
                g.fill(bx - 1, by - 1, bx + bw + 1, by,          COL_SLOT_BORDER);
                g.fill(bx - 1, by + bh, bx + bw + 1, by + bh + 1, COL_SLOT_BORDER);
                g.fill(bx - 1, by,      bx,           by + bh,     COL_SLOT_BORDER);
                g.fill(bx + bw, by,     bx + bw + 1,  by + bh,     COL_SLOT_BORDER);
                g.blit(layout.backgroundTexture(),
                        bx, by, bw, bh,
                        layout.bgX(), layout.bgY(), bw, bh,
                        256, 256);
            }

            if (layout.shapeless()) {
                // Small "S" badge in top-right corner of the card (JEI-style shapeless indicator)
                String badge = "S";
                int bw = font.width(badge) + 4;
                int bx = cardX + cardW - bw - 3;
                int by = cardY + 3;
                AMITheme.fillRounded(g, bx, by, bw, font.lineHeight + 3, COL_SHAPELESS);
                g.drawString(font, badge, bx + 2, by + 2, 0xFFFFFFFF, false);
                if (isHovering(mouseX, mouseY, bx, by, bw, font.lineHeight + 3)) {
                    g.renderTooltip(font, Component.translatable("ami.recipe_viewer.shapeless"), mouseX, mouseY);
                }
            }

            // Favorite button — 9×9 star sprite centred in a 14×14 hit area
            if (!layout.output().isEmpty()) {
                int favX = cardX + 6;
                int favY = cardY + 4;
                boolean favorite = AmiFavoritesHandler.getInstance().isRecipeFavorite(recipe.id(), layout.output());
                boolean favHov   = isHovering(mouseX, mouseY, favX, favY, 14, 14);
                AMITheme.fillRounded(g, favX, favY, 14, 14,
                        favorite ? COL_TAB_ACTIVE : favHov ? COL_TAB_HOVER : COL_TAB_IDLE);
                String star = "★";
                g.drawString(font, star,
                        favX + (14 - font.width(star)) / 2,
                        favY + (14 - font.lineHeight) / 2,
                        favorite ? 0xFFFFFFFF : COL_TAB_TEXT_I, false);
                if (favHov) {
                    g.renderTooltip(font, Component.translatable(favorite
                            ? "ami.recipe_viewer.unfavorite"
                            : "ami.recipe_viewer.favorite"), mouseX, mouseY);
                }
            }

            // For mod recipe types that use the generic fallback, show the category name
            // as a small label so the user knows what machine handles it.
            RecipeType<?> rTypePre = recipe.value().getType();
            if (!RecipeDisplayHelper.hasDedicatedLayout(rTypePre) && layout.categoryName() != null
                    && !layout.categoryName().isEmpty()) {
                String catLabel = layout.categoryName();
                if (catLabel.length() > 20) catLabel = catLabel.substring(0, 18) + "…";
                g.drawString(font, catLabel, rx, cardY + 3, COL_TEXT_CAT, false);
            }

            // Input slots
            int slotIdx = 0;
            for (SlotPosition slot : layout.inputs()) {
                int sx = rx + slot.x();
                int sy = ry + slot.y();
                if (layout.drawSlotBackground()) {
                    drawSlot(g, sx, sy);
                }
                if (!slot.alternatives().isEmpty()) {
                    String slotKey = recipe.id().toString() + "_" + slotIdx;
                    int altIdx     = getSlotAltIndex(slotKey, slot.alternatives().size());
                    ItemStack stack = slot.alternatives().get(altIdx);
                    g.renderItem(stack, sx + 1, sy + 1);
                    g.renderItemDecorations(font, stack, sx + 1, sy + 1);
                    if (isHovering(mouseX, mouseY, sx, sy, 18, 18)) {
                        renderIngredientTooltip(g, slot, altIdx, mouseX, mouseY);
                    }
                }
                slotIdx++;
            }

            // Arrow / animation
            RecipeType<?> rType = recipe.value().getType();
            if (layout.backgroundTexture() == null) {
                boolean furnace = !layout.inputs().isEmpty() && RecipeDisplayHelper.isFurnaceType(rType);
                int arrowX = rx + layout.arrowX();
                int arrowY = ry + layout.arrowY();
                if (furnace) drawAnimatedArrow(g, arrowX, arrowY);
                else         drawArrow(g, arrowX, arrowY);
            } else {
                renderAnimatedBackground(g, rx, ry, layout, rType, recipe);
            }

            // Output slot — gold border to distinguish from input slots
            int outX = rx + layout.outputX();
            int outY = ry + layout.outputY();
            if (layout.drawSlotBackground()) {
                drawOutputSlot(g, outX, outY);
            }
            if (!layout.output().isEmpty()) {
                g.renderItem(layout.output(), outX + 1, outY + 1);
                g.renderItemDecorations(font, layout.output(), outX + 1, outY + 1);
                if (isHovering(mouseX, mouseY, outX, outY, 18, 18)) {
                    g.renderTooltip(font, layout.output(), mouseX, mouseY);
                }
            }

            // Transfer button
            boolean recipeCanTransfer = RecipeViewerBridge.canTransferRecipe(recipe, parentScreen);
            if (recipeCanTransfer) {
                int btnX    = cardX + cardW - 22;
                int btnY    = cardY + (cardH - 14) / 2;
                boolean bHov = isHovering(mouseX, mouseY, btnX, btnY, 18, 14);
                AMITheme.fillRounded(g, btnX, btnY, 18, 14, bHov ? COL_BTN_HOVER : COL_BTN_IDLE);
                drawCentered(g, Component.translatable("ami.recipe_viewer.transfer_icon"),
                        btnX + 9, btnY + 2, 0xFFFFFFFF);
                if (bHov) {
                    g.renderTooltip(font,
                            Component.translatable("ami.recipe_viewer.transfer"), mouseX, mouseY);
                }
            }
        }

    }

    private RecipeLayout getLayoutForDisplay(int displayIndex, AmiRecipeHolder<?> recipe) {
        if (displayIndex >= 0 && displayIndex < cachedLayouts.size()) {
            return cachedLayouts.get(displayIndex);
        }
        if (!loggedLayoutCacheDrift) {
            loggedLayoutCacheDrift = true;
            System.err.println("[AMI] RecipeViewerScreen cache drift detected: "
                    + "displayIndex=" + displayIndex
                    + ", cachedLayouts=" + cachedLayouts.size()
                    + ", recipesOnPage=" + recipesPerPage
                    + ", selectedTab=" + selectedTab
                    + ", pageIndex=" + pageIndex);
        }
        if (recipe == null || minecraft == null || minecraft.level == null) {
            return null;
        }
        return RecipeDisplayHelper.getLayout(recipe, minecraft.level.registryAccess());
    }

    private void renderIngredientTooltip(GuiGraphics g, SlotPosition slot, int altIdx, int mouseX, int mouseY) {
        ItemStack stack = slot.alternatives().get(altIdx);
        if (slot.alternatives().size() <= 1) {
            g.renderTooltip(font, stack, mouseX, mouseY);
            return;
        }
        List<Component> lines = new ArrayList<>(
                net.minecraft.client.gui.screens.Screen.getTooltipFromItem(minecraft, stack));
        lines.add(Component.empty());
        lines.add(Component.translatable("ami.recipe_viewer.ingredient_cycle",
                altIdx + 1, slot.alternatives().size())
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        g.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
    }

    private void renderAnimatedBackground(GuiGraphics g, int rx, int ry,
                                          RecipeLayout layout, RecipeType<?> rType,
                                          AmiRecipeHolder<?> recipe) {
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
            int srcX  = 176 + frame * 16;
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

    // ── Workstation / catalyst panel ─────────────────────────────────────

    private void drawWorkstationPanel(GuiGraphics g, int mouseX, int mouseY) {
        if (tabs.isEmpty() || selectedTab >= tabs.size()) return;
        List<ItemStack> workstations = tabs.get(selectedTab).workstations();
        if (workstations.isEmpty()) return;

        int slotOuter = 20;   // 18px slot + 1px border each side
        int padH      = 4;
        int padV      = 5;
        int panelW    = slotOuter + 2 * padH;  // 28px
        int panelH    = 2 * padV + workstations.size() * slotOuter;
        int overlap   = 4;
        int px        = guiLeft - panelW + overlap;
        int py        = guiTop + CONTENT_Y;

        g.fill(px - 1, py - 1, px + panelW + 1, py + panelH + 1, COL_BORDER);
        g.fill(px,     py,     px + panelW,     py + panelH,     COL_PANEL);

        for (int i = 0; i < workstations.size(); i++) {
            ItemStack ws = workstations.get(i);
            int sx = px + padH;
            int sy = py + padV + i * slotOuter;
            drawSlot(g, sx, sy);
            g.renderItem(ws, sx + 1, sy + 1);
            g.renderItemDecorations(font, ws, sx + 1, sy + 1);
            if (isHovering(mouseX, mouseY, sx, sy, 18, 18)) {
                g.renderTooltip(font, ws, mouseX, mouseY);
            }
        }
    }

    // ── Slot / arrow drawing ──────────────────────────────────────────────

    private void drawSlot(GuiGraphics g, int x, int y) {
        g.blit(widgets(), x, y, 18, 18, UV_SLOT_X, UV_SLOT_Y, 18, 18, 256, 256);
    }

    private void drawOutputSlot(GuiGraphics g, int x, int y) {
        // Output sprite is 26×26; inner 18×18 item area starts at (4,4) in sprite space
        g.blit(widgets(), x - 4, y - 4, 26, 26, UV_OUT_X, UV_OUT_Y, 26, 26, 256, 256);
    }

    private void drawArrow(GuiGraphics g, int x, int y) {
        g.blit(widgets(), x, y, 22, 16, UV_ARROW_X, UV_ARROW_Y, 22, 16, 256, 256);
    }

    private void drawAnimatedArrow(GuiGraphics g, int x, int y) {
        long elapsed  = System.currentTimeMillis() - animStart;
        float progress = (elapsed % 2500) / 2500f;
        int shaft     = (int) (20 * progress);
        if (shaft > 0) g.fill(x, y + 3, x + shaft, y + 6, COL_ARROW_ANIM);
        g.fill(x, y + 3, x + 20, y + 4, COL_ARROW);
        g.fill(x, y + 5, x + 20, y + 6, COL_ARROW);
        drawArrow(g, x + 20, y);
    }

    // ── Input handling ────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key pressedKey = InputConstants.getKey(keyCode, scanCode);
        if (AmiKeybinds.activeAndMatches(Services.PLATFORM.keyMappings().favorite(), pressedKey, modifiers)
                && tryToggleFavoriteUnderMouse()) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_E) {
            onClose();
            return true;
        }
        if (AmiKeybinds.activeAndMatches(Services.PLATFORM.keyMappings().recipeBack(), pressedKey)
                || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            goBack();
            return true;
        }
        if (tabs.isEmpty()) return super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (hasShiftDown()) prevPage(); else prevTab();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (hasShiftDown()) nextPage(); else nextTab();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && canTransfer) {
            doTransfer();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean tryToggleFavoriteUnderMouse() {
        if (tabs.isEmpty() || minecraft == null) {
            return false;
        }
        var window = minecraft.getWindow();
        double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / (double) window.getScreenHeight();
        return tryToggleFavoriteUnderMouse(mouseX, mouseY);
    }

    private boolean tryToggleFavoriteUnderMouse(double mouseX, double mouseY) {
        if (tabs.isEmpty()) {
            return false;
        }

        Tab tab     = tabs.get(selectedTab);
        int tabSize = tab.recipes().size();
        if (tabSize == 0) {
            return false;
        }

        int startIdx = pageIndex * recipesPerPage;
        int endIdx   = Math.min(startIdx + recipesPerPage, tabSize);
        int cardH    = currentCardH;
        int slotH    = cardH + 4;
        int cardX    = guiLeft + 4;
        int singleOffset = 6;

        for (int i = startIdx; i < endIdx; i++) {
            AmiRecipeHolder<?> recipe = tab.recipes().get(i);
            RecipeLayout layout = getLayoutForDisplay(i - startIdx, recipe);
            if (layout == null) continue;

            int cardY = guiTop + CONTENT_Y + (i - startIdx) * slotH + singleOffset;
            int rx = cardX + 24;
            int layoutHeight = layout.backgroundTexture() != null
                    ? layout.bgRenderY() + layout.bgH()
                    : 14 + layout.gridHeight() * 18;
            int ry = cardY + Math.max(4, (cardH - layoutHeight) / 2);

            if (!layout.output().isEmpty()) {
                int outX = rx + layout.outputX();
                int outY = ry + layout.outputY();
                if (isHovering(mouseX, mouseY, outX, outY, 18, 18)) {
                    AmiFavoritesHandler favorites = AmiFavoritesHandler.getInstance();
                    if (favorites.isRecipeFavorite(recipe.id(), layout.output())) {
                        favorites.removeRecipeFavorite(recipe.id(), layout.output());
                    } else {
                        favorites.addRecipeFavorite(recipe.id(), layout.output());
                    }
                    return true;
                }
            }

            int slotIdx = 0;
            for (SlotPosition slot : layout.inputs()) {
                if (!slot.alternatives().isEmpty()) {
                    int sx = rx + slot.x();
                    int sy = ry + slot.y();
                    if (isHovering(mouseX, mouseY, sx, sy, 18, 18)) {
                        String slotKey = recipe.id().toString() + "_" + slotIdx;
                        int altIdx = getSlotAltIndex(slotKey, slot.alternatives().size());
                        ItemStack stack = slot.alternatives().get(altIdx);
                        if (!stack.isEmpty()) {
                            AmiFavoritesHandler.getInstance().toggleFavorite(stack);
                            return true;
                        }
                    }
                }
                slotIdx++;
            }
        }

        return false;
    }

    // NeoForge 1.21.1+: 4-param signature
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return handleScroll(mouseX, mouseY, scrollY);
    }

    // Forge 1.20.1 compat
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return handleScroll(mouseX, mouseY, scrollY);
    }

    private boolean handleScroll(double mouseX, double mouseY, double scrollY) {
        if (tabs.isEmpty() || scrollY == 0) return false;

        // Scroll over tab bar → scroll tab strip if overflow
        int tabY = tabTopY();
        if (tabsOverflow() && isHovering(mouseX, mouseY, guiLeft, tabY, GUI_WIDTH, TAB_H)) {
            int vis    = visibleTabCount();
            int maxOff = Math.max(0, tabs.size() - vis);
            tabScrollOffset = Math.max(0, Math.min(tabScrollOffset + (scrollY > 0 ? -1 : 1), maxOff));
            return true;
        }

        Tab tab     = tabs.get(selectedTab);
        int tabSize = tab.recipes().size();
        if (tabSize == 0) return false;

        int startIdx = pageIndex * recipesPerPage;
        int endIdx   = Math.min(startIdx + recipesPerPage, tabSize);
        int cardH    = currentCardH;
        int slotH    = cardH + 4;
        int cardX        = guiLeft + 4;
        int singleOffset2 = 6;

        // Scroll over an ingredient slot → cycle alternatives
        for (int i = startIdx; i < endIdx; i++) {
            AmiRecipeHolder<?> recipe = tab.recipes().get(i);
            RecipeLayout layout = getLayoutForDisplay(i - startIdx, recipe);
            if (layout == null) continue;

            int cardY = guiTop + CONTENT_Y + (i - startIdx) * slotH + singleOffset2;
            int rx = cardX + 24;
            int layoutHeight = layout.backgroundTexture() != null
                    ? layout.bgRenderY() + layout.bgH()
                    : 14 + layout.gridHeight() * 18;
            int ry = cardY + Math.max(4, (cardH - layoutHeight) / 2);

            int slotIdx = 0;
            for (SlotPosition slot : layout.inputs()) {
                if (slot.alternatives().size() > 1) {
                    int sx = rx + slot.x();
                    int sy = ry + slot.y();
                    if (isHovering(mouseX, mouseY, sx, sy, 18, 18)) {
                        String slotKey = recipe.id().toString() + "_" + slotIdx;
                        int cur  = slotOffsets.getOrDefault(slotKey, 0);
                        int size = slot.alternatives().size();
                        slotOffsets.put(slotKey, ((cur + (scrollY > 0 ? 1 : -1)) % size + size) % size);
                        return true;
                    }
                }
                slotIdx++;
            }
        }

        // Scroll anywhere else → page
        int totalPages = (int) Math.ceil((double) tabSize / recipesPerPage);
        if (totalPages > 1) {
            if (scrollY > 0) prevPage(); else nextPage();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tabs.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);

        Tab tab     = tabs.get(selectedTab);
        int tabSize = tab.recipes().size();
        int tabY    = tabTopY();

        // Header nav buttons — row 1: category prev/next; row 2: page prev/next
        if (button == 0) {
            int titleH = font.lineHeight + BORDER_PAD;
            int btnY1  = guiTop + titleH - SMALL_BTN_H + NAV_PAD;
            int btnY2  = btnY1 + SMALL_BTN_H + NAV_PAD;
            int btnL   = guiLeft + BORDER_PAD;
            int btnR   = guiLeft + GUI_WIDTH - BORDER_PAD - SMALL_BTN_W;
            if (isHovering(mouseX, mouseY, btnL, btnY1, SMALL_BTN_W, SMALL_BTN_H)) { prevTab(); return true; }
            if (isHovering(mouseX, mouseY, btnR, btnY1, SMALL_BTN_W, SMALL_BTN_H)) { nextTab(); return true; }
            if (isHovering(mouseX, mouseY, btnL, btnY2, SMALL_BTN_W, SMALL_BTN_H)) { prevPage(); return true; }
            if (isHovering(mouseX, mouseY, btnR, btnY2, SMALL_BTN_W, SMALL_BTN_H)) { nextPage(); return true; }

            // Category title click → show all recipes of this type (JEI feature)
            if (focusType == null) {
                Component catTitle = tabs.get(selectedTab).label();
                int ty = guiTop + (titleH - font.lineHeight) / 2 + 1;
                int tw = font.width(catTitle);
                int tx = guiLeft + GUI_WIDTH / 2 - tw / 2;
                if (isHovering(mouseX, mouseY, tx, ty, tw, font.lineHeight)) {
                    navigateToType(tabs.get(selectedTab).type());
                    return true;
                }
            }
        }

        // Tab scroll arrows
        if (tabsOverflow() && mouseY >= tabY && mouseY <= tabY + TAB_H) {
            int arrowLX = guiLeft + 4;
            int arrowRX = guiLeft + GUI_WIDTH - 4 - TAB_ARROW_W;
            if (isHovering(mouseX, mouseY, arrowLX, tabY, TAB_ARROW_W, TAB_H) && tabScrollOffset > 0) {
                tabScrollOffset--;
                return true;
            }
            if (isHovering(mouseX, mouseY, arrowRX, tabY, TAB_ARROW_W, TAB_H)
                    && tabScrollOffset + visibleTabCount() < tabs.size()) {
                tabScrollOffset++;
                return true;
            }
        }

        // Tab selection
        if (mouseY >= tabY && mouseY <= tabY + TAB_H) {
            if (tabsOverflow()) {
                int tabAreaX = guiLeft + 4 + TAB_ARROW_W + 1;
                int vis      = visibleTabCount();
                for (int i = 0; i < vis; i++) {
                    int tabIdx = tabScrollOffset + i;
                    if (tabIdx >= tabs.size()) break;
                    int tx = tabAreaX + i * TAB_W;
                    if (isHovering(mouseX, mouseY, tx, tabY, TAB_W - 1, TAB_H)) {
                        selectedTab = tabIdx;
                        pageIndex   = 0;
                        refreshLayout();
                        minecraft.getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        return true;
                    }
                }
            } else {
                int startX = guiLeft + 4;
                for (int i = 0; i < tabs.size(); i++) {
                    int tx = startX + i * TAB_W;
                    if (isHovering(mouseX, mouseY, tx, tabY, TAB_W, TAB_H)) {
                        selectedTab = i;
                        pageIndex   = 0;
                        refreshLayout();
                        minecraft.getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        return true;
                    }
                }
            }
        }

        // Workstation panel click → navigate to the workstation item
        if (!tab.workstations().isEmpty() && (button == 0 || button == 1)) {
            int slotOuter = 20;
            int padH = 4, padV = 5;
            int panelW = slotOuter + 2 * padH;
            int overlap = 4;
            int px = guiLeft - panelW + overlap;
            int py = guiTop + CONTENT_Y;
            List<ItemStack> ws = tab.workstations();
            for (int i = 0; i < ws.size(); i++) {
                int sx = px + padH;
                int sy = py + padV + i * slotOuter;
                if (isHovering(mouseX, mouseY, sx, sy, 18, 18)) {
                    navigateTo(ws.get(i), button == 0);
                    return true;
                }
            }
        }

        int totalPages = (int) Math.ceil((double) tabSize / recipesPerPage);
        int startIdx   = pageIndex * recipesPerPage;
        int endIdx     = Math.min(startIdx + recipesPerPage, tabSize);
        int cardW      = GUI_WIDTH - 8;
        int cardH      = currentCardH;
        int slotH      = cardH + 4;
        int cardX      = guiLeft + 4;
        int singleOffset = 6;

        for (int i = startIdx; i < endIdx; i++) {
            AmiRecipeHolder<?> recipe = tab.recipes().get(i);
            RecipeLayout layout = getLayoutForDisplay(i - startIdx, recipe);
            if (layout == null) continue;

            int cardY = guiTop + CONTENT_Y + (i - startIdx) * slotH + singleOffset;
            int rx    = cardX + 24;
            int layoutHeight = layout.backgroundTexture() != null
                    ? layout.bgRenderY() + layout.bgH()
                    : 14 + layout.gridHeight() * 18;
            int ry = cardY + Math.max(4, (cardH - layoutHeight) / 2);

            boolean recipeCanTransfer = RecipeViewerBridge.canTransferRecipe(recipe, parentScreen);

            // Favorite button
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

            // Transfer button
            if (recipeCanTransfer && button == 0) {
                int btnX = cardX + cardW - 22;
                int btnY = cardY + (cardH - 14) / 2;
                if (isHovering(mouseX, mouseY, btnX, btnY, 18, 14)) {
                    doTransfer(recipe, hasShiftDown());
                    return true;
                }
            }

            // Input slots → navigate
            if (button == 0 || button == 1) {
                int slotIdx = 0;
                for (SlotPosition slot : layout.inputs()) {
                    int sx = rx + slot.x();
                    int sy = ry + slot.y();
                    if (isHovering(mouseX, mouseY, sx, sy, 18, 18) && !slot.alternatives().isEmpty()) {
                        String slotKey = recipe.id().toString() + "_" + slotIdx;
                        int altIdx     = getSlotAltIndex(slotKey, slot.alternatives().size());
                        ItemStack clicked = slot.alternatives().get(altIdx);
                        if (!clicked.isEmpty()) navigateTo(clicked, button == 0);
                        return true;
                    }
                    slotIdx++;
                }
            }

            // Output slot → navigate or shift-transfer
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

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Navigation helpers ────────────────────────────────────────────────

    private void prevTab() {
        selectedTab = (selectedTab - 1 + tabs.size()) % tabs.size();
        ensureSelectedTabVisible();
        pageIndex = 0;
        refreshLayout();
    }

    private void nextTab() {
        selectedTab = (selectedTab + 1) % tabs.size();
        ensureSelectedTabVisible();
        pageIndex = 0;
        refreshLayout();
    }

    private void prevPage() {
        int total = (int) Math.ceil((double) tabs.get(selectedTab).recipes().size() / recipesPerPage);
        if (total > 1) { pageIndex = (pageIndex - 1 + total) % total; refreshLayout(); }
    }

    private void nextPage() {
        int total = (int) Math.ceil((double) tabs.get(selectedTab).recipes().size() / recipesPerPage);
        if (total > 1) { pageIndex = (pageIndex + 1) % total; refreshLayout(); }
    }

    private void doTransfer() {
        if (minecraft == null) return;
        Tab tab = tabs.get(selectedTab);
        int firstIdx = pageIndex * recipesPerPage;
        if (firstIdx < tab.recipes().size()) doTransfer(tab.recipes().get(firstIdx), hasShiftDown());
    }

    private void doTransfer(AmiRecipeHolder<?> recipe, boolean maxTransfer) {
        if (minecraft == null || recipe == null) return;
        if (RecipeViewerBridge.transferRecipe(recipe, parentScreen, minecraft, maxTransfer, false)) {
            onClose();
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void onClose() {
        RecipeViewerBridge.clearRecipeView();
        InventoryOverlayHandler.setAmiEnabled(shouldRestoreAmiEnabled);
        if (minecraft != null) minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Utilities ─────────────────────────────────────────────────────────

    private int getSlotAltIndex(String slotKey, int size) {
        int timeIdx = (int) (System.currentTimeMillis() / 1000 % size);
        return ((timeIdx + slotOffsets.getOrDefault(slotKey, 0)) % size + size) % size;
    }

    private boolean isHovering(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean shadow() {
        return com.sanhiruzu.ami.config.AmiConfig.theme != com.sanhiruzu.ami.config.AmiConfig.Theme.VANILLA;
    }

    private void drawCentered(GuiGraphics g, Component text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, shadow());
    }

    private void drawCentered(GuiGraphics g, String text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, shadow());
    }

    // ── Records ───────────────────────────────────────────────────────────

    private record HistoryEntry(ItemStack target, boolean showRecipes, int selectedTab,
                                int pageIndex, RecipeType<?> focusType) {}

    private record Tab(RecipeType<?> type, Component label, String shortLabel,
                       List<AmiRecipeHolder<?>> recipes,
                       ItemStack icon, List<ItemStack> workstations) {}
}
