package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

public class OverlayWidgetManager {
    private static final int BOTTOM_BAR_H   = 32;
    private static final int SEARCH_H       = 24;
    private static final int MIN_PANEL_WIDTH = 100;
    private static final int MAX_PANEL_WIDTH = 280;
    private static final int PANEL_MARGIN    = 6;
    private static final int PANEL_MARGIN_V  = 6;

    // AMI button sits at (AMI_BTN_X, screenH - AMI_BTN_H - AMI_BTN_MARGIN), matching the row
    // EMI uses for its own button (emi.x=2, emi.y=screenH-22).  When EMI is active alongside
    // AMI, EmiScreenManagerMixin shifts EMI's button to AMI_BTN_NEXT_X so they sit side-by-side.
    public  static final int AMI_BTN_X       = 2;
    public  static final int AMI_BTN_W       = 22;
    private static final int AMI_BTN_H       = 20;
    private static final int AMI_BTN_MARGIN  = 2;  // 2 px gap used both below and beside the button
    /** X where EMI's button should start when AMI is present. */
    public  static final int AMI_BTN_NEXT_X  = AMI_BTN_X + AMI_BTN_W + AMI_BTN_MARGIN;

    private ResultsPanelWidget resultsPanel;
    private SidebarPanelWidget leftPanel;
    private SidebarPanelWidget leftPanelSecondary;
    private SidebarPanelWidget rightPanelPrimary;
    private SidebarPanelWidget rightPanelSecondary;
    private SearchBarWidget    searchBar;
    private AmiButtonWidget    amiButton;
    private boolean widgetsReady = false;

    private boolean indexingStarted = false;
    private boolean panelVisible = false;
    private String lastSyncedQuery = "";

    private WidgetBounds lastResultsBounds = null;
    private int lastScreenH = 0;
    private boolean onLeft = true;
    private boolean pendingEmiReinit = false;

    public OverlayWidgetManager() {
        // Widgets are created on first computeLayouts() call so Minecraft.font is available.
    }

    private void ensureWidgets() {
        if (widgetsReady) return;
        this.resultsPanel      = new ResultsPanelWidget();
        this.leftPanel         = new SidebarPanelWidget(0, 0, 0, 0, AmiConfig.leftPanelContent);
        this.leftPanelSecondary = new SidebarPanelWidget(0, 0, 0, 0, AmiConfig.leftPanelSecondaryContent);
        this.rightPanelPrimary  = new SidebarPanelWidget(0, 0, 0, 0, AmiConfig.rightPanelContent);
        this.rightPanelSecondary = new SidebarPanelWidget(0, 0, 0, 0, AmiConfig.rightPanelSecondaryContent);
        this.searchBar         = new SearchBarWidget(this::triggerSearch);
        this.amiButton         = new AmiButtonWidget(() -> {
            var mc = Minecraft.getInstance();
            mc.setScreen(new com.sanhiruzu.ami.client.screen.AmiConfigScreen(mc.screen));
        }, InventoryOverlayHandler::toggleAmi, () -> panelVisible);

        this.resultsPanel.setOnModClick(token -> {
            searchBar.toggleToken(token);
            String modId = token.startsWith("@") ? token.substring(1) : token;
            var inner = resultsPanel.getInnerPanel();
            if (inner != null) inner.getState().toggleMod(modId);
        });
        this.resultsPanel.setOnReset(searchBar::clear);
        this.resultsPanel.setOnTokenInject(token -> searchBar.toggleToken(token));

        Runnable refreshSidebars = this::refreshSidebars;
        this.leftPanel.getInnerPanel().setOnReset(refreshSidebars);
        this.leftPanelSecondary.getInnerPanel().setOnReset(refreshSidebars);
        this.rightPanelPrimary.getInnerPanel().setOnReset(refreshSidebars);
        this.rightPanelSecondary.getInnerPanel().setOnReset(refreshSidebars);
        
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().setOnChange(refreshSidebars);

        widgetsReady = true;
    }

    /**
     * Computes pixel-accurate bounds for every overlay widget.
     *
     * <p>Screen layout (typical inventory, not to scale):
     * <pre>
     *  0                                          screenW
     *  ┌──────────────────────────────────────────────┐  0
     *  │  left    │ inventory UI │       right         │
     *  │  panels  │             │  (results / sidebar) │  panelY
     *  │  (same   │             │   (same panelY,      │
     *  │  panelY, │             │    same panelH)      │
     *  │  panelH) │             │                      │  panelY + panelH
     *  ├──────────┴─────────────┴──────────────────────┤  screenH - BOTTOM_BAR_H
     *  │ [B]   [search bar──────────────────────]      │
     *  └──────────────────────────────────────────────┘  screenH
     * </pre>
     *
     * <p>Key invariants:
     * <ul>
     *   <li>Left and right panel slots share the same {@code panelY} and {@code panelH},
     *       so they are always vertically symmetric regardless of screen size.
     *   <li>Both slots are centred within the usable height above the bottom bar,
     *       capped at 600 px so they never become unwieldy on large monitors.
     *   <li>When two panels are configured on one side they split the slot into
     *       equal halves with a {@code PANEL_MARGIN} gap between them.
     * </ul>
     */
    public void computeLayouts(AbstractContainerScreen<?> containerScreen, int screenW, int screenH) {
        ensureWidgets();
        lastScreenH = screenH;

        // ── AMI button ────────────────────────────────────────────────────────
        // Pinned to top-left of the bottom-bar area (bottom edge = screenH - BOTTOM_BAR_H).
        amiButton.updateBounds(new WidgetBounds(AMI_BTN_X, screenH - AMI_BTN_H - AMI_BTN_MARGIN, AMI_BTN_W, AMI_BTN_H));

        // ── Shared panel slot geometry ────────────────────────────────────────
        // Both left and right sides derive their Y and height from this so they
        // are always visually symmetric. Panels are centred in the usable height
        // above the bottom bar, then capped at 600 px.
        int usableH = screenH - BOTTOM_BAR_H - PANEL_MARGIN_V * 2;
        int panelH  = Math.min(usableH, 600);
        int panelY  = PANEL_MARGIN_V + (usableH - panelH) / 2;

        // ── Left panels ───────────────────────────────────────────────────────
        int containerLeftEdge  = containerScreen.getGuiLeft();
        int containerRightEdge = containerScreen.getGuiLeft() + containerScreen.getXSize();

        boolean hasLeft    = isSidebarContent(AmiConfig.leftPanelContent);
        boolean hasLeftSec = isSidebarContent(AmiConfig.leftPanelSecondaryContent);

        if (hasLeft || hasLeftSec) {
            // Width is user-configured, but capped so we don't overlap the inventory.
            int leftW = Math.min(AmiConfig.leftPanelWidth, containerLeftEdge - PANEL_MARGIN * 2);
            if (leftW >= 40) {
                Rect leftSlot = Rect.of(PANEL_MARGIN, panelY, leftW, panelH);
                placeSidePanels(leftSlot, hasLeft, hasLeftSec,
                        leftPanel,          AmiConfig.leftPanelContent,
                        leftPanelSecondary, AmiConfig.leftPanelSecondaryContent);
            } else {
                leftPanel.visible = leftPanelSecondary.visible = false;
            }
        } else {
            leftPanel.visible = leftPanelSecondary.visible = false;
        }

        // ── Right panels ──────────────────────────────────────────────────────
        // safeWidth = pixels between the right edge of the inventory and the screen edge.
        int safeWidth   = screenW - containerRightEdge - PANEL_MARGIN * 2;
        int panelStartX = screenW; // default: off-screen (used for search-bar clamping below)

        if (safeWidth >= MIN_PANEL_WIDTH) {
            // Width: 35 % of screen, clamped to [MIN_PANEL_WIDTH, min(safeWidth, MAX_PANEL_WIDTH)].
            int rw = Math.clamp((int)(screenW * 0.35f), MIN_PANEL_WIDTH, Math.min(safeWidth, MAX_PANEL_WIDTH));
            panelStartX = screenW - rw - PANEL_MARGIN;
            Rect rightSlot = Rect.of(panelStartX, panelY, rw, panelH);

            boolean isSearch        = isSearchContent(AmiConfig.rightPanelContent);
            boolean hasRightPrimary = isSidebarContent(AmiConfig.rightPanelContent);
            boolean hasRightSec     = isSidebarContent(AmiConfig.rightPanelSecondaryContent);

            if (isSearch) {
                // Search/results panel fills the right slot (or the top half when a
                // secondary sidebar is also configured on the right).
                if (hasRightSec) {
                    Rect[] halves = rightSlot.halves(PANEL_MARGIN);
                    resultsPanel.updateBounds(halves[0].toWidgetBounds());
                    resultsPanel.visible = true;
                    rightPanelPrimary.visible = false;
                    rightPanelSecondary.setContentType(AmiConfig.rightPanelSecondaryContent);
                    rightPanelSecondary.updateLayout(halves[1]);
                    rightPanelSecondary.visible = true;
                } else {
                    resultsPanel.updateBounds(rightSlot.toWidgetBounds());
                    resultsPanel.visible = true;
                    rightPanelPrimary.visible = false;
                    rightPanelSecondary.visible = false;
                }
            } else if (hasRightPrimary || hasRightSec) {
                resultsPanel.visible = false;
                placeSidePanels(rightSlot, hasRightPrimary, hasRightSec,
                        rightPanelPrimary, AmiConfig.rightPanelContent,
                        rightPanelSecondary, AmiConfig.rightPanelSecondaryContent);
            } else {
                resultsPanel.visible = rightPanelPrimary.visible = rightPanelSecondary.visible = false;
            }

            lastResultsBounds = rightSlot.toWidgetBounds();
        } else {
            resultsPanel.visible = rightPanelPrimary.visible = rightPanelSecondary.visible = false;
        }

        // ── Search bar ────────────────────────────────────────────────────────
        // Centred horizontally, then nudged left if it would overlap the right panel.
        int maxBarRight = (safeWidth >= MIN_PANEL_WIDTH) ? (panelStartX - PANEL_MARGIN) : (screenW - 4);
        int barW = Math.min(AmiConfig.searchBarWidth, screenW - 8);
        int barX = Math.max(4, (screenW - barW) / 2);
        if (barX + barW > maxBarRight) {
            barX = Math.max(4, maxBarRight - barW);
            barW = Math.max(60, Math.min(barW, maxBarRight - barX));
        }
        searchBar.updateBounds(new WidgetBounds(barX, screenH - BOTTOM_BAR_H + 2, barW, SEARCH_H));
    }

    /**
     * Places one or two sidebar panels within {@code slot}.
     *
     * <p>When both panels are active the slot is split into equal halves
     * ({@link Rect#halves}). This logic is identical on both sides of the screen
     * so it lives here rather than being duplicated in the left/right branches.
     */
    private void placeSidePanels(
            Rect slot,
            boolean hasPrimary,   boolean hasSecondary,
            SidebarPanelWidget primary,   AmiConfig.PanelContent primaryContent,
            SidebarPanelWidget secondary, AmiConfig.PanelContent secondaryContent) {
        if (hasPrimary && hasSecondary) {
            Rect[] halves = slot.halves(PANEL_MARGIN);
            primary.setContentType(primaryContent);
            primary.updateLayout(halves[0]);
            primary.visible = true;
            secondary.setContentType(secondaryContent);
            secondary.updateLayout(halves[1]);
            secondary.visible = true;
        } else if (hasPrimary) {
            primary.setContentType(primaryContent);
            primary.updateLayout(slot);
            primary.visible = true;
            secondary.visible = false;
        } else {
            secondary.setContentType(secondaryContent);
            secondary.updateLayout(slot);
            secondary.visible = true;
            primary.visible = false;
        }
    }

    /** Overload for any Screen type (including non-AbstractContainerScreen mod containers) */
    public void computeLayouts(Screen screen, int screenW, int screenH) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            computeLayouts(containerScreen, screenW, screenH);
        } else {
            // For non-AbstractContainerScreen containers, assume full screen layout
            // and use default container bounds
            computeLayoutsForCustomScreen(screenW, screenH);
        }
    }

    /**
     * Fallback layout for non-{@link AbstractContainerScreen} screens.
     * Results panel is centred horizontally in the middle third of the screen.
     */
    private void computeLayoutsForCustomScreen(int screenW, int screenH) {
        ensureWidgets();
        lastScreenH = screenH;

        amiButton.updateBounds(new WidgetBounds(AMI_BTN_X, screenH - AMI_BTN_H - AMI_BTN_MARGIN, AMI_BTN_W, AMI_BTN_H));

        leftPanel.visible = leftPanelSecondary.visible = false;
        rightPanelPrimary.visible = rightPanelSecondary.visible = false;

        // Centred panel: middle third of the screen width, full usable height.
        Rect panel = Rect.of(screenW / 6, 20, screenW * 2 / 3, screenH - BOTTOM_BAR_H - 40);
        resultsPanel.updateBounds(panel.toWidgetBounds());
        resultsPanel.visible = true;

        int barW = Math.min(AmiConfig.searchBarWidth, screenW - 8);
        int barX = (screenW - barW) / 2;
        searchBar.updateBounds(new WidgetBounds(barX, screenH - BOTTOM_BAR_H + 2, barW, SEARCH_H));
    }

    private boolean isSearchContent(AmiConfig.PanelContent content) {
        return content == AmiConfig.PanelContent.GRID || content == AmiConfig.PanelContent.LIST || content == AmiConfig.PanelContent.COMPACT;
    }

    private boolean isSidebarContent(AmiConfig.PanelContent content) {
        return content == AmiConfig.PanelContent.FAVORITES || content == AmiConfig.PanelContent.LOOKUP_HISTORY || 
               content == AmiConfig.PanelContent.CRAFTING_HISTORY || content == AmiConfig.PanelContent.CRAFTABLE;
    }

    public void tick(ScreenEvent.Render.Post event) {
        if (!AmiConfig.enableAutoIndexing) return;
        if (!panelVisible) return;

        var indexer = AmiIndexerService.getInstance();
        if (!indexingStarted) {
            indexingStarted = true;
            indexer.rebuild();
        }

        if (indexer.isReady()) {
            var panel = resultsPanel.getInnerPanel();
            if (panel != null && panel.getEntryCount() == 0 && indexer.indexedItemCount() > 0) {
                panel.setSearchService(indexer.getOrBuildSearchService());
                refreshEntries();
            }
        }

        syncFromRecipeViewer();
        
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() % 20 == 0) {
            refreshSidebars();
        }
    }

    public void renderAll(ScreenEvent.Render.Post event) {
        if (event.getScreen() == null) return;

        try {
            computeLayouts(event.getScreen(), event.getScreen().width, event.getScreen().height);

            var g = event.getGuiGraphics();
            int mx = event.getMouseX(), my = event.getMouseY();
            float pt = event.getPartialTick();

            g.pose().pushPose();
            g.pose().translate(0, 0, 0);

            amiButton.render(g, mx, my, pt);

            if (panelVisible) {
                searchBar.render(g, mx, my, pt);
                if (resultsPanel.visible) {
                    resultsPanel.render(g, mx, my, pt);
                    resultsPanel.renderOverlay(g, mx, my);
                }
                if (leftPanel.visible) leftPanel.render(g, mx, my, pt);
                if (leftPanelSecondary.visible) leftPanelSecondary.render(g, mx, my, pt);
                if (rightPanelPrimary.visible) rightPanelPrimary.render(g, mx, my, pt);
                if (rightPanelSecondary.visible) rightPanelSecondary.render(g, mx, my, pt);
            }

            g.pose().popPose();

        } catch (Exception e) {
            AMI.LOGGER.error("AMI overlay render failed", e);
        }

        if (pendingEmiReinit) {
            pendingEmiReinit = false;
            var mc = Minecraft.getInstance();
            if (mc.screen != null) mc.screen.init(mc, mc.screen.width, mc.screen.height);
        }
    }

    private void refreshEntries() {
        var panel = resultsPanel.getInnerPanel();
        if (panel == null) return;

        List<SearchNode> all = new ArrayList<>();
        for (NodeType t : NodeType.atlasValues()) all.addAll(GlobalIndex.getInstance().getNodes(t));
        panel.setEntries(all);
        refreshSidebars();
    }

    public void refreshSidebars() {
        if (leftPanel != null) leftPanel.refresh();
        if (leftPanelSecondary != null) leftPanelSecondary.refresh();
        if (rightPanelPrimary != null) rightPanelPrimary.refresh();
        if (rightPanelSecondary != null) rightPanelSecondary.refresh();
    }

    private void triggerSearch(String query) {
        var panel = resultsPanel.getInnerPanel();
        if (panel == null) return;
        panel.getState().setQuery(query);
        if (RecipeViewerBridge.supportsSearchSync() && !query.equals(lastSyncedQuery)) {
            lastSyncedQuery = query;
            RecipeViewerBridge.setSearchText(query);
        }
    }

    private void syncFromRecipeViewer() {
        if (!RecipeViewerBridge.supportsSearchSync() || searchBar.isFocused()) return;
        String rvQuery = RecipeViewerBridge.getSearchText();
        if (!rvQuery.equals(lastSyncedQuery)) {
            lastSyncedQuery = rvQuery;
            searchBar.setQuery(rvQuery);
            var panel = resultsPanel.getInnerPanel();
            if (panel == null) return;
            panel.getState().setQuery(rvQuery);
        }
    }

    private void togglePanelVisible() {
        panelVisible = !panelVisible;
        if (!panelVisible) {
            searchBar.clear();
            lastSyncedQuery = "";
        }
        pendingEmiReinit = true;
    }

    public boolean isPanelVisible() { return panelVisible; }
    public void setPanelVisible(boolean visible) { if (visible != panelVisible) togglePanelVisible(); }
    public WidgetBounds getResultsBounds() { return lastResultsBounds; }
    public AmiButtonWidget getAmiButton() { return amiButton; }
    public SearchBarWidget getSearchBar() { return searchBar; }
    public ResultsPanelWidget getResultsPanel() { return resultsPanel; }
    public SidebarPanelWidget getLeftPanel() { return leftPanel; }
    public SidebarPanelWidget getLeftPanelSecondary() { return leftPanelSecondary; }
    public SidebarPanelWidget getRightPanelPrimary() { return rightPanelPrimary; }
    public SidebarPanelWidget getRightPanelSecondary() { return rightPanelSecondary; }
}
