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
import net.minecraft.resources.ResourceLocation;
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
    private static final int HEADER_H         = 28;
    private static final int TAB_BAR_Y        = HEADER_H + 2;               // 30
    private static final int TAB_H            = 18;
    private static final int TAB_W            = 60;                          // fixed width per tab slot
    private static final int TAB_ARROW_W      = 14;                          // scroll arrow button width
    private static final int WORKSTATION_BAR_Y = TAB_BAR_Y + TAB_H + 4;    // 52
    private static final int WORKSTATION_H    = 22;
    private static final int CONTENT_Y        = WORKSTATION_BAR_Y + WORKSTATION_H + 4; // 78
    private static final int FOOTER_H         = 18;
    // Non-recipe vertical overhead (CONTENT_Y + padding + FOOTER_H)
    private static final int CHROME_OVERHEAD  = 112;

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

    // ── Layout broadcast (read by OverlayWidgetManager to avoid panel overlap) ──
    /** Set while this screen is open; -1 when closed. X of the left edge of the panel. */
    public static int openLeft  = -1;
    /** Set while this screen is open; -1 when closed. X of the right edge of the panel. */
    public static int openRight = -1;

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
        openLeft  = guiLeft;
        openRight = guiLeft + GUI_WIDTH;
        animStart = System.currentTimeMillis();

        if (tabs.isEmpty()) {
            rebuildTabs();   // calls recomputePanelSize() → sets guiTop
        } else {
            recomputePanelSize();
            refreshLayout();
        }
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

    // Recomputes recipesPerPage, guiHeight, guiTop from currentCardH and screen size.
    private void recomputePanelSize() {
        int slotH  = currentCardH + 4;                     // card + gap between cards
        int maxH   = (minecraft != null ? minecraft.getWindow().getGuiScaledHeight() : height) - 30;
        int avail  = maxH - CHROME_OVERHEAD - 16;           // 16 = top+bottom card padding
        int N      = Math.max(1, Math.min(3, avail / slotH));
        this.recipesPerPage = N;
        this.guiHeight = N == 1
                ? CHROME_OVERHEAD + currentCardH + 32     // 16 top + 16 bottom content padding
                : CHROME_OVERHEAD + N * slotH + 12;       // small margin at top/bottom
        this.guiTop = ((minecraft != null ? minecraft.getWindow().getGuiScaledHeight() : height) - guiHeight) / 2;
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
                        entry.getValue()));
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

        // Update panel size if the recipe type changed card height
        int newCardH = getCardHeight(currentLayout);
        if (newCardH != currentCardH) {
            currentCardH = newCardH;
            recomputePanelSize();
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
        drawHeader(g);
        drawFavoritesButton(g, mouseX, mouseY);

        if (tabs.isEmpty()) {
            drawNoRecipes(g);
        } else {
            drawTabBar(g, mouseX, mouseY);
            drawWorkstationPanel(g, mouseX, mouseY);
            drawContent(g, mouseX, mouseY);
        }

        g.drawCenteredString(font,
                Component.translatable("ami.recipe_viewer.go_back"),
                guiLeft + GUI_WIDTH / 2,
                guiTop + guiHeight - FOOTER_H + 4,
                COL_TEXT_FOOTER);
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

    // Returns the bounding box of the favorites button in the header.
    private int favBtnX() { return guiLeft + GUI_WIDTH - 18; }
    private int favBtnY() { return guiTop + 4; }

    private void drawFavoritesButton(GuiGraphics g, int mouseX, int mouseY) {
        int bx = favBtnX(), by = favBtnY();
        boolean hov = isHovering(mouseX, mouseY, bx, by, 14, 14);
        AMITheme.fillRounded(g, bx, by, 14, 14, hov ? COL_TAB_HOVER : COL_TAB_IDLE);
        g.drawCenteredString(font, "★", bx + 7, by + 3,
                hov ? COL_TAB_ACTIVE : COL_TAB_TEXT_I);
        if (hov) {
            g.renderTooltip(font, Component.translatable("ami.favorites.open"), mouseX, mouseY);
        }
    }

    // ── Tab bar ───────────────────────────────────────────────────────────

    private void drawTabBar(GuiGraphics g, int mouseX, int mouseY) {
        int barY = guiTop + TAB_BAR_Y;

        if (tabsOverflow()) {
            int arrowLX  = guiLeft + 4;
            int arrowRX  = guiLeft + GUI_WIDTH - 4 - TAB_ARROW_W;
            boolean canL = tabScrollOffset > 0;
            boolean canR = tabScrollOffset + visibleTabCount() < tabs.size();
            boolean hovL = isHovering(mouseX, mouseY, arrowLX, barY, TAB_ARROW_W, TAB_H);
            boolean hovR = isHovering(mouseX, mouseY, arrowRX, barY, TAB_ARROW_W, TAB_H);

            // 9×9 arrow glyphs centred in the button area; greyed text fallback when disabled
            int btnMidY = barY + (TAB_H - 9) / 2;
            if (canL) {
                g.blit(widgets(), arrowLX + (TAB_ARROW_W - 9) / 2, btnMidY, 9, 9, UV_BTN_PREV_X, UV_BTN_PREV_Y, 9, 9, 256, 256);
            } else {
                g.drawCenteredString(font, "❮", arrowLX + TAB_ARROW_W / 2, barY + (TAB_H - font.lineHeight) / 2, COL_TAB_TEXT_I);
            }
            if (canR) {
                g.blit(widgets(), arrowRX + (TAB_ARROW_W - 9) / 2, btnMidY, 9, 9, UV_BTN_NEXT_X, UV_BTN_NEXT_Y, 9, 9, 256, 256);
            } else {
                g.drawCenteredString(font, "❯", arrowRX + TAB_ARROW_W / 2, barY + (TAB_H - font.lineHeight) / 2, COL_TAB_TEXT_I);
            }

            int tabAreaX = arrowLX + TAB_ARROW_W + 1;
            int vis = visibleTabCount();
            for (int i = 0; i < vis; i++) {
                int tabIdx = tabScrollOffset + i;
                if (tabIdx >= tabs.size()) break;
                drawSingleTab(g, mouseX, mouseY, tabIdx, tabAreaX + i * TAB_W, TAB_W - 1, barY);
            }
        } else {
            int tabW   = Math.min((GUI_WIDTH - 8) / tabs.size(), 72);
            int totalW = tabW * tabs.size();
            int startX = guiLeft + (GUI_WIDTH - totalW) / 2;
            for (int i = 0; i < tabs.size(); i++) {
                drawSingleTab(g, mouseX, mouseY, i, startX + i * tabW, tabW - 1, barY);
            }
        }
    }

    private void drawSingleTab(GuiGraphics g, int mouseX, int mouseY,
                               int tabIdx, int tx, int tw, int barY) {
        boolean active  = (tabIdx == selectedTab);
        boolean hovered = isHovering(mouseX, mouseY, tx, barY, tw, TAB_H);

        int bg = active ? COL_TAB_ACTIVE : hovered ? COL_TAB_HOVER : COL_TAB_IDLE;
        AMITheme.fillRounded(g, tx, barY, tw, TAB_H, bg);
        if (active) {
            g.fill(tx + 1, barY + TAB_H - 2, tx + tw - 1, barY + TAB_H, COL_TAB_ACTIVE);
        }

        ItemStack icon = RecipeDisplayHelper.getRepresentativeWorkstation(tabs.get(tabIdx).type());
        if (icon != null && !icon.isEmpty() && tw >= 18) {
            int iconX = tx + (tw - 16) / 2;
            int iconY = barY + (TAB_H - 16) / 2;
            g.renderItem(icon, iconX, iconY);
        } else {
            String label    = tabs.get(tabIdx).shortLabel();
            int textColor   = active ? COL_TAB_TEXT_A : COL_TAB_TEXT_I;
            int textX       = tx + (tw - font.width(label)) / 2;
            int textY       = barY + (TAB_H - font.lineHeight) / 2;
            g.drawString(font, label, textX, textY, textColor, false);
        }

        // Recipe count badge (top-right corner of the tab)
        int count = tabs.get(tabIdx).recipes().size();
        if (count > 0 && tw >= 26) {
            String countStr = count > 99 ? "99+" : String.valueOf(count);
            int badgeW = font.width(countStr) + 3;
            int badgeX = tx + tw - badgeW - 1;
            int badgeY = barY + 1;
            if ((COL_COUNT_BADGE >>> 24) != 0) {
                g.fill(badgeX, badgeY, badgeX + badgeW, badgeY + font.lineHeight + 1, COL_COUNT_BADGE);
            }
            g.drawString(font, countStr, badgeX + 1, badgeY + 1, COL_COUNT_BADGE_TEXT, false);
        }

        if (hovered) {
            g.renderTooltip(font, tabs.get(tabIdx).label(), mouseX, mouseY);
        }
    }

    // ── Workstation panel ─────────────────────────────────────────────────

    private void drawWorkstationPanel(GuiGraphics g, int mouseX, int mouseY) {
        if (tabs.isEmpty()) return;
        Tab tab = tabs.get(selectedTab);

        int stripY = guiTop + WORKSTATION_BAR_Y;
        int stripX = guiLeft + 4;
        int stripW = GUI_WIDTH - 8;

        if ((COL_WORKSTATION_BG >>> 24) != 0) {
            g.fill(stripX, stripY, stripX + stripW, stripY + WORKSTATION_H, COL_WORKSTATION_BG);
        }
        g.fill(stripX, stripY,                   stripX + stripW, stripY + 1,            COL_BORDER);
        g.fill(stripX, stripY + WORKSTATION_H - 1, stripX + stripW, stripY + WORKSTATION_H, COL_BORDER);

        Component label = Component.translatable("ami.recipe_viewer.made_in");
        int labelX = stripX + 4;
        int labelY = stripY + (WORKSTATION_H - font.lineHeight) / 2;
        g.drawString(font, label, labelX, labelY, COL_WORKSTATION_TEXT, false);

        List<ItemStack> workstations = RecipeDisplayHelper.getWorkstations(tab.type());
        int iconX = labelX + font.width(label) + 5;
        int iconY = stripY + (WORKSTATION_H - 16) / 2;
        int shown = 0;
        for (ItemStack ws : workstations) {
            if (ws.isEmpty()) continue;
            if (shown >= 6) {
                // More indicator
                g.drawString(font, "…", iconX + 2, iconY + (16 - font.lineHeight) / 2,
                        COL_TEXT_CAT, false);
                break;
            }
            g.renderItem(ws, iconX, iconY);
            if (isHovering(mouseX, mouseY, iconX, iconY, 16, 16)) {
                List<Component> wsTip = new ArrayList<>(
                        net.minecraft.client.gui.screens.Screen.getTooltipFromItem(minecraft, ws));
                wsTip.add(Component.translatable("ami.recipe_viewer.browse_type")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                g.renderTooltip(font, wsTip, java.util.Optional.empty(), mouseX, mouseY);
            }
            iconX += 18;
            shown++;
        }
    }

    // ── Content ───────────────────────────────────────────────────────────

    private void drawNoRecipes(GuiGraphics g) {
        g.drawCenteredString(font,
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
        // Centre the recipe(s) inside the content area when only one is shown
        int contentAreaH = guiHeight - CONTENT_Y - FOOTER_H;
        int singleOffset = recipesPerPage == 1 ? Math.max(0, (contentAreaH - cardH) / 2) : 6;

        for (int i = startIdx; i < endIdx; i++) {
            AmiRecipeHolder<?> recipe = tab.recipes().get(i);
            RecipeLayout layout = RecipeDisplayHelper.getLayout(recipe, minecraft.level.registryAccess());
            if (layout == null) continue;

            int cardY = guiTop + CONTENT_Y + (i - startIdx) * slotH + singleOffset;

            g.fill(cardX - 1, cardY - 1, cardX + cardW + 1, cardY + cardH + 1, COL_BORDER);
            g.fill(cardX, cardY, cardX + cardW, cardY + cardH, COL_PANEL);

            int rx = cardX + 24;

            int layoutHeight = layout.backgroundTexture() != null
                    ? layout.bgRenderY() + layout.bgH()
                    : 14 + layout.gridHeight() * 18;
            int yOffset = Math.max(0, (cardH - layoutHeight) / 2);
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

            // Favorite button
            if (!layout.output().isEmpty()) {
                int favX = cardX + 6;
                int favY = cardY + 4;
                boolean favorite = AmiFavoritesHandler.getInstance().isRecipeFavorite(recipe.id(), layout.output());
                boolean favHov   = isHovering(mouseX, mouseY, favX, favY, 14, 14);
                AMITheme.fillRounded(g, favX, favY, 14, 14,
                        favorite ? COL_TAB_ACTIVE : favHov ? COL_TAB_HOVER : COL_TAB_IDLE);
                g.drawCenteredString(font, Component.translatable("ami.recipe_viewer.favorite_icon"),
                        favX + 7, favY + 3, favorite ? 0xFFFFFFFF : COL_TAB_TEXT_I);
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
                g.drawCenteredString(font, Component.translatable("ami.recipe_viewer.transfer_icon"),
                        btnX + 9, btnY + 2, 0xFFFFFFFF);
                if (bHov) {
                    g.renderTooltip(font,
                            Component.translatable("ami.recipe_viewer.transfer"), mouseX, mouseY);
                }
            }
        }

        if (totalPages > 1) {
            drawPageNav(g, mouseX, mouseY, pageIndex + 1, totalPages);
        }
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

    // ── Page nav ──────────────────────────────────────────────────────────

    private void drawPageNav(GuiGraphics g, int mouseX, int mouseY, int current, int total) {
        int navY  = guiTop + guiHeight - FOOTER_H - 8;
        int navCX = guiLeft + GUI_WIDTH / 2;

        boolean prevHov = isHovering(mouseX, mouseY, navCX - 40, navY - 2, 14, 12);
        boolean nextHov = isHovering(mouseX, mouseY, navCX + 26, navY - 2, 14, 12);

        g.drawString(font, Component.translatable("ami.recipe_viewer.prev"),
                navCX - 36, navY, prevHov ? 0xFFFFFFFF : COL_TEXT_NAV, false);
        g.drawString(font, Component.translatable("ami.recipe_viewer.next"),
                navCX + 28, navY, nextHov ? 0xFFFFFFFF : COL_TEXT_NAV, false);
        g.drawCenteredString(font,
                Component.translatable("ami.recipe_viewer.page", current, total),
                navCX, navY, COL_TEXT_NAV);
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
        int barY = guiTop + TAB_BAR_Y;
        if (tabsOverflow() && isHovering(mouseX, mouseY, guiLeft, barY, GUI_WIDTH, TAB_H)) {
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
        int cardX    = guiLeft + 4;
        int contentAreaH2 = guiHeight - CONTENT_Y - FOOTER_H;
        int singleOffset2 = recipesPerPage == 1 ? Math.max(0, (contentAreaH2 - cardH) / 2) : 6;

        // Scroll over an ingredient slot → cycle alternatives
        for (int i = startIdx; i < endIdx; i++) {
            AmiRecipeHolder<?> recipe = tab.recipes().get(i);
            RecipeLayout layout = RecipeDisplayHelper.getLayout(recipe, minecraft.level.registryAccess());
            if (layout == null) continue;

            int cardY = guiTop + CONTENT_Y + (i - startIdx) * slotH + singleOffset2;
            int rx = cardX + 24;
            int layoutHeight = layout.backgroundTexture() != null
                    ? layout.bgRenderY() + layout.bgH()
                    : 14 + layout.gridHeight() * 18;
            int ry = cardY + Math.max(0, (cardH - layoutHeight) / 2);

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
        int barY    = guiTop + TAB_BAR_Y;

        // Favorites button (★ in header top-right)
        if (button == 0 && isHovering(mouseX, mouseY, favBtnX(), favBtnY(), 14, 14)) {
            if (minecraft != null) {
                minecraft.setScreen(new com.sanhiruzu.ami.client.favorites.AmiFavoritesScreen(parentScreen));
            }
            return true;
        }

        // Workstation panel — click any workstation icon to browse all recipes of that type
        int wsY = guiTop + WORKSTATION_BAR_Y;
        if (button == 0 && mouseY >= wsY && mouseY < wsY + WORKSTATION_H && !tabs.isEmpty()) {
            Tab wTab = tabs.get(selectedTab);
            List<ItemStack> workstations = RecipeDisplayHelper.getWorkstations(wTab.type());
            int wsLabelX = guiLeft + 4 + 4;
            int wsIconX  = wsLabelX + font.width(Component.translatable("ami.recipe_viewer.made_in")) + 5;
            int wsIconY  = wsY + (WORKSTATION_H - 16) / 2;
            int shown    = 0;
            for (ItemStack ws : workstations) {
                if (ws.isEmpty()) continue;
                if (shown >= 6) break;
                if (isHovering(mouseX, mouseY, wsIconX, wsIconY, 16, 16)) {
                    navigateToType(wTab.type());
                    return true;
                }
                wsIconX += 18;
                shown++;
            }
        }

        // Tab scroll arrows
        if (tabsOverflow() && mouseY >= barY && mouseY <= barY + TAB_H) {
            int arrowLX = guiLeft + 4;
            int arrowRX = guiLeft + GUI_WIDTH - 4 - TAB_ARROW_W;
            if (isHovering(mouseX, mouseY, arrowLX, barY, TAB_ARROW_W, TAB_H) && tabScrollOffset > 0) {
                tabScrollOffset--;
                return true;
            }
            if (isHovering(mouseX, mouseY, arrowRX, barY, TAB_ARROW_W, TAB_H)
                    && tabScrollOffset + visibleTabCount() < tabs.size()) {
                tabScrollOffset++;
                return true;
            }
        }

        // Tab selection
        if (mouseY >= barY && mouseY <= barY + TAB_H) {
            if (tabsOverflow()) {
                int tabAreaX = guiLeft + 4 + TAB_ARROW_W + 1;
                int vis      = visibleTabCount();
                for (int i = 0; i < vis; i++) {
                    int tabIdx = tabScrollOffset + i;
                    if (tabIdx >= tabs.size()) break;
                    int tx = tabAreaX + i * TAB_W;
                    if (isHovering(mouseX, mouseY, tx, barY, TAB_W - 1, TAB_H)) {
                        selectedTab = tabIdx;
                        pageIndex   = 0;
                        refreshLayout();
                        return true;
                    }
                }
            } else {
                int tabW   = Math.min((GUI_WIDTH - 8) / tabs.size(), 72);
                int totalW = tabW * tabs.size();
                int startX = guiLeft + (GUI_WIDTH - totalW) / 2;
                int idx    = (int) ((mouseX - startX) / tabW);
                if (idx >= 0 && idx < tabs.size()) {
                    selectedTab = idx;
                    pageIndex   = 0;
                    refreshLayout();
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
        int contentAreaH = guiHeight - CONTENT_Y - FOOTER_H;
        int singleOffset = recipesPerPage == 1 ? Math.max(0, (contentAreaH - cardH) / 2) : 6;

        for (int i = startIdx; i < endIdx; i++) {
            AmiRecipeHolder<?> recipe = tab.recipes().get(i);
            RecipeLayout layout = RecipeDisplayHelper.getLayout(recipe, minecraft.level.registryAccess());
            if (layout == null) continue;

            int cardY = guiTop + CONTENT_Y + (i - startIdx) * slotH + singleOffset;
            int rx    = cardX + 24;
            int layoutHeight = layout.backgroundTexture() != null
                    ? layout.bgRenderY() + layout.bgH()
                    : 14 + layout.gridHeight() * 18;
            int ry = cardY + Math.max(0, (cardH - layoutHeight) / 2);

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

        // Page nav
        if (totalPages > 1) {
            int navY  = guiTop + guiHeight - FOOTER_H - 8;
            int navCX = guiLeft + GUI_WIDTH / 2;
            if (mouseY >= navY - 2 && mouseY <= navY + 12) {
                if (mouseX >= navCX - 40 && mouseX <= navCX - 26) { prevPage(); return true; }
                if (mouseX >= navCX + 26 && mouseX <= navCX + 40) { nextPage(); return true; }
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
        openLeft  = -1;
        openRight = -1;
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

    // ── Records ───────────────────────────────────────────────────────────

    private record HistoryEntry(ItemStack target, boolean showRecipes, int selectedTab,
                                int pageIndex, RecipeType<?> focusType) {}

    private record Tab(RecipeType<?> type, Component label, String shortLabel,
                       List<AmiRecipeHolder<?>> recipes) {}
}
