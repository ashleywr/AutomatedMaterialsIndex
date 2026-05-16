package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.AMILayoutConfig;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

public class OverlayWidgetManager {
    private static final int BOTTOM_BAR_H = 32;
    private static final int SEARCH_H = 24;
    private static final int MIN_PANEL_WIDTH = 70;
    private static final int PANEL_MARGIN_V = 6;

    private final ResultsPanelWidget resultsPanel;
    private final SearchBarWidget searchBar;
    private final AmiButtonWidget amiButton;

    private SearchService searchService = null;
    private volatile boolean indexingInProgress = false;
    private boolean indexingDispatched = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 5;
    private boolean panelVisible = false;
    private String lastSyncedQuery = "";

    private WidgetBounds lastResultsBounds = null;
    private int lastScreenH = 0;
    private boolean onLeft = true;
    private boolean pendingEmiReinit = false;

    public OverlayWidgetManager() {
        this.resultsPanel = new ResultsPanelWidget();
        this.searchBar = new SearchBarWidget(this::triggerSearch);
        this.amiButton = new AmiButtonWidget(InventoryOverlayHandler::toggleAmi, () -> panelVisible);
    }

    /** Updates widget bounds for the current screen geometry. Called from both onScreenInit and renderAll. */
    public void computeLayouts(AbstractContainerScreen<?> containerScreen, int screenW, int screenH) {
        lastScreenH = screenH;

        boolean goLeft = switch (AMILayoutConfig.PANEL_SIDE.get()) {
            case LEFT  -> true;
            case RIGHT -> false;
            case AUTO  -> InventoryOverlayHandler.RECIPE_VIEWER_PRESENT;
        };
        this.onLeft = goLeft;

        int panelH = screenH - PANEL_MARGIN_V * 2;
        int panelX, panelW;
        int widthOverride = AMILayoutConfig.PANEL_WIDTH_OVERRIDE.get();
        if (goLeft) {
            int available = containerScreen.getGuiLeft() - 12;
            panelW = widthOverride > 0 ? widthOverride : Math.max(0, available);
            panelX = containerScreen.getGuiLeft() - panelW - 6;
        } else {
            panelX = containerScreen.getGuiLeft() + containerScreen.getXSize() + 6;
            int available = screenW - panelX - 6;
            panelW = widthOverride > 0 ? widthOverride : Math.max(0, available);
        }

        int btnY = screenH - BOTTOM_BAR_H + 2;
        amiButton.updateBounds(new WidgetBounds(2, btnY - 22, 22, 20));

        if (panelW < MIN_PANEL_WIDTH) {
            WidgetBounds zero = new WidgetBounds(0, 0, 0, 0);
            resultsPanel.updateBounds(zero);
            searchBar.updateBounds(zero);
            return;
        }

        resultsPanel.updateBounds(new WidgetBounds(panelX, PANEL_MARGIN_V, panelW, panelH));
        lastResultsBounds = resultsPanel.getBounds();

        int searchBarW = Math.min(AMILayoutConfig.SEARCH_BAR_WIDTH.get(), screenW - 8);
        int searchBarX = Math.max(4, (screenW - searchBarW) / 2);
        int searchBarY = screenH - BOTTOM_BAR_H + 2;
        searchBar.updateBounds(new WidgetBounds(searchBarX, searchBarY, searchBarW, SEARCH_H));
    }

    /** Drives indexing, search sync, and stale-refresh — only when the panel is visible. */
    public void tick(ScreenEvent.Render.Post event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!panelVisible) return;

        if (!indexingDispatched && !indexingInProgress) {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                indexingInProgress = true;
                GlobalIndexCache.loadOrIndexAsync(level, () -> {
                    searchService = SearchService.buildFrom(GlobalIndex.getInstance());
                    ProviderRegistry.indexStructuresDeferred(level);
                    searchService = SearchService.buildFrom(GlobalIndex.getInstance());
                    indexingInProgress = false;
                    indexingDispatched = true;
                    refreshEntries();
                });
            }
        } else if (indexingDispatched && retryCount < MAX_RETRIES) {
            var index = GlobalIndex.getInstance();
            int structures = index.getNodes(NodeType.STRUCTURE).size();
            int dimensions = index.getNodes(NodeType.DIMENSION).size();
            if (structures == 0 || dimensions == 0) {
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    ProviderRegistry.indexStructuresDeferred(level);
                    searchService = SearchService.buildFrom(GlobalIndex.getInstance());
                }
                retryCount++;
            }
        }

        syncFromRecipeViewer();
        checkAndRefreshIfStale();
    }

    /** Renders all AMI widgets. Button always; search bar and panel only when the panel is visible. */
    public void renderAll(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

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

    private void checkAndRefreshIfStale() {
        var panel = resultsPanel.getInnerPanel();
        if (panel == null) return;

        int totalCount = 0;
        for (NodeType t : NodeType.atlasValues()) totalCount += GlobalIndex.getInstance().getNodes(t).size();
        if (panel.getEntryCount() == 0 && totalCount > 0 && panel.getCurrentQuery().isEmpty()) {
            refreshEntries();
        }
    }

    private void refreshEntries() {
        var panel = resultsPanel.getInnerPanel();
        if (panel == null) return;

        List<SearchNode> all = new ArrayList<>();
        for (NodeType t : NodeType.atlasValues()) all.addAll(GlobalIndex.getInstance().getNodes(t));
        panel.setEntries(all);
        AMI.LOGGER.debug("AMI overlay refreshed: {} total entries across all types", all.size());
    }

    private void triggerSearch(String query) {
        var panel = resultsPanel.getInnerPanel();
        if (searchService == null || panel == null) return;

        if (query.isEmpty()) {
            refreshEntries();
        } else {
            var results = searchService.query(query);
            panel.setSearchResults(results, query);
        }

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
            if (panel == null || searchService == null) return;
            if (rvQuery.isEmpty()) {
                refreshEntries();
            } else {
                panel.setSearchResults(searchService.query(rvQuery), rvQuery);
            }
        }
    }

    private void togglePanelVisible() {
        boolean wasVisible = panelVisible;
        panelVisible = !panelVisible;

        if (wasVisible && !panelVisible) {
            indexingDispatched = false;
            retryCount = 0;
            searchBar.clear();
            lastSyncedQuery = "";
            pendingEmiReinit = true;
        } else if (!wasVisible && panelVisible) {
            pendingEmiReinit = true;
        }
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    public void setPanelVisible(boolean visible) {
        if (visible != panelVisible) togglePanelVisible();
    }

    public boolean isOnLeft() {
        return onLeft;
    }

    public WidgetBounds getResultsBounds() {
        return lastResultsBounds;
    }

    public AmiButtonWidget getAmiButton() {
        return amiButton;
    }

    public SearchBarWidget getSearchBar() {
        return searchBar;
    }

    public ResultsPanelWidget getResultsPanel() {
        return resultsPanel;
    }
}
