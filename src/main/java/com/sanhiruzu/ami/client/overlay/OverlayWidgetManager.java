package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
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
    private static final int BOTTOM_BAR_H = 32;
    private static final int SEARCH_H = 24;
    private static final int MIN_PANEL_WIDTH = 120;
    private static final int MAX_PANEL_WIDTH = 280;
    private static final int PANEL_MARGIN = 6;
    private static final int PANEL_MARGIN_V = 6;

    private ResultsPanelWidget resultsPanel;
    private SearchBarWidget searchBar;
    private AmiButtonWidget amiButton;
    private boolean widgetsReady = false;

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
        // Widgets are created on first computeLayouts() call so Minecraft.font is available.
    }

    private void ensureWidgets() {
        if (widgetsReady) return;
        this.resultsPanel = new ResultsPanelWidget();
        this.searchBar = new SearchBarWidget(this::triggerSearch);
        this.amiButton = new AmiButtonWidget(InventoryOverlayHandler::toggleAmi, () -> panelVisible);

        // Connect mod-badge clicking to search bar filtering
        this.resultsPanel.setOnModClick(token -> {
            searchBar.toggleToken(token);
            String modId = token.startsWith("@") ? token.substring(1) : token;
            var inner = resultsPanel.getInnerPanel();
            if (inner != null) inner.getState().toggleMod(modId);
        });
        this.resultsPanel.setOnReset(searchBar::clear);

        widgetsReady = true;
    }

    /** Updates widget bounds for the current screen geometry. Called from both onScreenInit and renderAll. */
    public void computeLayouts(AbstractContainerScreen<?> containerScreen, int screenW, int screenH) {
        ensureWidgets();
        lastScreenH = screenH;
        this.onLeft = false;

        // Button is always positioned regardless of panel state.
        int btnY = screenH - BOTTOM_BAR_H + 2;
        amiButton.updateBounds(new WidgetBounds(2, btnY - 22, 22, 20));

        // Task 1: right edge of the container GUI in screen-pixel space.
        int containerRightEdge = containerScreen.getGuiLeft() + containerScreen.getXSize();

        // Task 2: usable width between the container's right edge and the screen edge.
        int safeWidth = screenW - containerRightEdge - (PANEL_MARGIN * 2);

        // Task 4: no room for even a minimal panel — hide rather than draw an unusable sliver.
        if (safeWidth < MIN_PANEL_WIDTH) {
            WidgetBounds zero = new WidgetBounds(0, 0, 0, 0);
            resultsPanel.updateBounds(zero);
            searchBar.updateBounds(zero);
            return;
        }

        // Task 3: preferred width is 35% of screen, then clamped so it neither exceeds the
        // safe region nor falls below the minimum readable size.
        int preferredWidth = (int)(screenW * 0.35f);
        int actualWidth = Math.clamp(preferredWidth, MIN_PANEL_WIDTH, Math.min(safeWidth, MAX_PANEL_WIDTH));

        // Screen height minus 40px headroom, hard-capped at 600 scaled pixels.
        int panelH = Math.min(screenH - 40, 600);
        // Right-anchor: PANEL_MARGIN from the right screen edge.
        int startX = screenW - actualWidth - PANEL_MARGIN;
        // Vertically centred on the screen.
        int panelY = (screenH - panelH) / 2;

        resultsPanel.updateBounds(new WidgetBounds(startX, panelY, actualWidth, panelH));
        lastResultsBounds = resultsPanel.getBounds();

        int searchBarW = Math.min(AMIConfig.SEARCH_BAR_WIDTH.get(), screenW - 8);
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
                    var panel = resultsPanel.getInnerPanel();
                    if (panel != null) panel.setSearchService(searchService);
                    indexingInProgress = false;
                    indexingDispatched = true;
                    if (AMIConfig.DEV_MODE.get()) ItemIconRenderer.auditMissingIcons();
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
                    var panel = resultsPanel.getInnerPanel();
                    if (panel != null) panel.setSearchService(searchService);
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

        // Debug visualizer
        if (AMIConfig.DEV_MODE.get()) {
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
            if (panel == null || searchService == null) return;
            panel.getState().setQuery(rvQuery);
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
