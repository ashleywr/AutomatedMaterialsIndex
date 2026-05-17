package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.*;
import net.minecraft.client.Minecraft;
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

    private ResultsPanelWidget resultsPanel;
    private FavoritesPanelWidget favoritesPanel;
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
        this.resultsPanel   = new ResultsPanelWidget();
        this.favoritesPanel = new FavoritesPanelWidget(0, 0, 0, 0);
        this.searchBar      = new SearchBarWidget(this::triggerSearch);
        this.amiButton      = new AmiButtonWidget(() -> {
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
        this.resultsPanel.setOnFacetInject(token -> searchBar.toggleToken(token));

        this.favoritesPanel.getInnerPanel().setOnReset(this::refreshFavorites);
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().setOnChange(this::refreshFavorites);

        widgetsReady = true;
    }

    /** Updates widget bounds for the current screen geometry. Called from both onScreenInit and renderAll. */
    public void computeLayouts(AbstractContainerScreen<?> containerScreen, int screenW, int screenH) {
        ensureWidgets();
        lastScreenH = screenH;
        this.onLeft = false;

        int btnY = screenH - BOTTOM_BAR_H + 2;
        amiButton.updateBounds(new WidgetBounds(2, btnY - 22, 22, 20));

        int containerLeftEdge = containerScreen.getGuiLeft();
        int containerRightEdge = containerScreen.getGuiLeft() + containerScreen.getXSize();
        
        // Favorites Panel (Left)
        if (AmiConfig.leftPanelContent == AmiConfig.PanelContent.FAVORITES) {
            int favW = Math.min(AmiConfig.leftPanelWidth, containerLeftEdge - (PANEL_MARGIN * 2));
            if (favW >= 40) {
                // Constrain height to not overlap with the AMI button at the bottom
                int maxH = screenH - BOTTOM_BAR_H - 30; // Leave space for button
                int panelH = Math.min(maxH, 600);
                int panelY = PANEL_MARGIN_V; // Start from top margin
                favoritesPanel.updateLayout(PANEL_MARGIN, panelY, favW, panelH);
                favoritesPanel.visible = true;
            } else {
                favoritesPanel.visible = false;
            }
        } else {
            favoritesPanel.visible = false;
        }

        // Results Panel (Right)
        int safeWidth = screenW - containerRightEdge - (PANEL_MARGIN * 2);
        int panelH = Math.min(screenH - 40, 600);
        int panelY = (screenH - panelH) / 2;
        int panelStartX = screenW; // sentinel: panel off-screen when hidden

        if (safeWidth >= MIN_PANEL_WIDTH) {
            // Same width in compact and full modes — only the panel content changes.
            int actualWidth = Math.clamp((int)(screenW * 0.35f), MIN_PANEL_WIDTH, Math.min(safeWidth, MAX_PANEL_WIDTH));
            panelStartX = screenW - actualWidth - PANEL_MARGIN;
            resultsPanel.updateBounds(new WidgetBounds(panelStartX, panelY, actualWidth, panelH));
            lastResultsBounds = resultsPanel.getBounds();
        } else {
            resultsPanel.updateBounds(new WidgetBounds(0, 0, 0, 0));
        }

        // Search bar
        int maxBarRight = (safeWidth >= MIN_PANEL_WIDTH) ? (panelStartX - PANEL_MARGIN) : (screenW - 4);
        int barW   = Math.min(AmiConfig.searchBarWidth, screenW - 8);
        int barX   = Math.max(4, (screenW - barW) / 2);
        if (barX + barW > maxBarRight) {
            barX = Math.max(4, maxBarRight - barW);
            barW = Math.min(barW, maxBarRight - barX);
            barW = Math.max(60, barW); 
        }
        int searchBarY = screenH - BOTTOM_BAR_H + 2;
        searchBar.updateBounds(new WidgetBounds(barX, searchBarY, barW, SEARCH_H));
    }

    /** Drives indexing, search sync, and stale-refresh — only when the panel is visible. */
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
    }

    /** Renders all AMI widgets. Button always; search bar and panel only when the panel is visible. */
    public void renderAll(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        if (AmiConfig.devMode) {
            for (var plugin : com.sanhiruzu.ami.api.AmiPluginRegistry.getPlugins()) {
                for (var zone : plugin.getExclusionZones(event.getScreen())) {
                    event.getGuiGraphics().fill(zone.getX(), zone.getY(), zone.getX() + zone.getWidth(), zone.getY() + zone.getHeight(), 0x55FF0000);
                }
            }
        }

        try {
            computeLayouts(containerScreen, event.getScreen().width, event.getScreen().height);

            var g = event.getGuiGraphics();
            int mx = event.getMouseX(), my = event.getMouseY();
            float pt = event.getPartialTick();

            g.pose().pushPose();
            g.pose().translate(0, 0, 0);

            amiButton.render(g, mx, my, pt);

            if (panelVisible) {
                searchBar.render(g, mx, my, pt);
                resultsPanel.render(g, mx, my, pt);
                resultsPanel.renderOverlay(g, mx, my);
                if (favoritesPanel.visible) {
                    favoritesPanel.render(g, mx, my, pt);
                }
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
        AMI.LOGGER.debug("AMI overlay refreshed: {} total entries across all types", all.size());
        refreshFavorites();
    }

    public void refreshFavorites() {
        if (favoritesPanel != null) favoritesPanel.refresh();
    }

    private void triggerSearch(String query) {
        var panel = resultsPanel.getInnerPanel();
        if (panel == null) return;

        panel.getState().setQuery(query);

        if (!query.equals(lastSyncedQuery)) {
            lastSyncedQuery = query;
            RecipeViewerBridge.setSearchText(query);
        }
    }

    private void syncFromRecipeViewer() {
        if (!RecipeViewerBridge.isAvailable()) return;
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
        boolean wasVisible = panelVisible;
        panelVisible = !panelVisible;

        if (wasVisible && !panelVisible) {
            indexingStarted = false;
            searchBar.clear();
            lastSyncedQuery = "";
            pendingEmiReinit = true;
        } else if (!wasVisible && panelVisible) {
            pendingEmiReinit = true;
        }
    }

    public boolean isPanelVisible() { return panelVisible; }

    public void setPanelVisible(boolean visible) {
        if (visible != panelVisible) togglePanelVisible();
    }

    public boolean isOnLeft() { return onLeft; }

    public WidgetBounds getResultsBounds() { return lastResultsBounds; }

    public AmiButtonWidget getAmiButton() { return amiButton; }

    public SearchBarWidget getSearchBar() { return searchBar; }

    public ResultsPanelWidget getResultsPanel() { return resultsPanel; }

    public FavoritesPanelWidget getFavoritesPanel() { return favoritesPanel; }
}
