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
        this.resultsPanel.setOnFacetInject(token -> searchBar.toggleToken(token));

        Runnable refreshSidebars = this::refreshSidebars;
        this.leftPanel.getInnerPanel().setOnReset(refreshSidebars);
        this.leftPanelSecondary.getInnerPanel().setOnReset(refreshSidebars);
        this.rightPanelPrimary.getInnerPanel().setOnReset(refreshSidebars);
        this.rightPanelSecondary.getInnerPanel().setOnReset(refreshSidebars);
        
        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().setOnChange(refreshSidebars);

        widgetsReady = true;
    }

    public void computeLayouts(AbstractContainerScreen<?> containerScreen, int screenW, int screenH) {
        ensureWidgets();
        lastScreenH = screenH;

        int btnY = screenH - BOTTOM_BAR_H + 2;
        amiButton.updateBounds(new WidgetBounds(2, btnY - 22, 22, 20));

        int containerLeftEdge = containerScreen.getGuiLeft();
        int containerRightEdge = containerScreen.getGuiLeft() + containerScreen.getXSize();
        
        // Left Side
        boolean hasLeft = isSidebarContent(AmiConfig.leftPanelContent);
        boolean hasLeftSec = isSidebarContent(AmiConfig.leftPanelSecondaryContent);
        
        if (hasLeft || hasLeftSec) {
            int leftW = Math.min(AmiConfig.leftPanelWidth, containerLeftEdge - (PANEL_MARGIN * 2));
            if (leftW >= 40) {
                int maxH = screenH - BOTTOM_BAR_H - 30;
                int panelH = Math.min(maxH, 600);
                int panelY = PANEL_MARGIN_V;
                
                if (hasLeft && hasLeftSec) {
                    int h1 = panelH / 2 - PANEL_MARGIN;
                    leftPanel.setContentType(AmiConfig.leftPanelContent);
                    leftPanel.updateLayout(PANEL_MARGIN, panelY, leftW, h1);
                    leftPanel.visible = true;
                    
                    leftPanelSecondary.setContentType(AmiConfig.leftPanelSecondaryContent);
                    leftPanelSecondary.updateLayout(PANEL_MARGIN, panelY + h1 + PANEL_MARGIN, leftW, panelH - h1 - PANEL_MARGIN);
                    leftPanelSecondary.visible = true;
                } else if (hasLeft) {
                    leftPanel.setContentType(AmiConfig.leftPanelContent);
                    leftPanel.updateLayout(PANEL_MARGIN, panelY, leftW, panelH);
                    leftPanel.visible = true;
                    leftPanelSecondary.visible = false;
                } else {
                    leftPanelSecondary.setContentType(AmiConfig.leftPanelSecondaryContent);
                    leftPanelSecondary.updateLayout(PANEL_MARGIN, panelY, leftW, panelH);
                    leftPanelSecondary.visible = true;
                    leftPanel.visible = false;
                }
            } else {
                leftPanel.visible = leftPanelSecondary.visible = false;
            }
        } else {
            leftPanel.visible = leftPanelSecondary.visible = false;
        }

        // Right Side
        int safeWidth = screenW - containerRightEdge - (PANEL_MARGIN * 2);
        int panelH = Math.min(screenH - 40, 600);
        int panelY = (screenH - panelH) / 2;
        int panelStartX = screenW;

        if (safeWidth >= MIN_PANEL_WIDTH) {
            int actualWidth = Math.clamp((int)(screenW * 0.35f), MIN_PANEL_WIDTH, Math.min(safeWidth, MAX_PANEL_WIDTH));
            panelStartX = screenW - actualWidth - PANEL_MARGIN;
            
            boolean hasRightPrimary = isSidebarContent(AmiConfig.rightPanelContent);
            boolean isSearch = isSearchContent(AmiConfig.rightPanelContent);
            boolean hasRightSec = isSidebarContent(AmiConfig.rightPanelSecondaryContent);
            
            if (isSearch) {
                if (hasRightSec) {
                    int h1 = panelH / 2 - PANEL_MARGIN;
                    resultsPanel.updateBounds(new WidgetBounds(panelStartX, panelY, actualWidth, h1));
                    resultsPanel.visible = true;
                    rightPanelPrimary.visible = false;
                    
                    rightPanelSecondary.setContentType(AmiConfig.rightPanelSecondaryContent);
                    rightPanelSecondary.updateLayout(panelStartX, panelY + h1 + PANEL_MARGIN, actualWidth, panelH - h1 - PANEL_MARGIN);
                    rightPanelSecondary.visible = true;
                } else {
                    resultsPanel.updateBounds(new WidgetBounds(panelStartX, panelY, actualWidth, panelH));
                    resultsPanel.visible = true;
                    rightPanelPrimary.visible = false;
                    rightPanelSecondary.visible = false;
                }
            } else if (hasRightPrimary || hasRightSec) {
                resultsPanel.visible = false;
                if (hasRightPrimary && hasRightSec) {
                    int h1 = panelH / 2 - PANEL_MARGIN;
                    rightPanelPrimary.setContentType(AmiConfig.rightPanelContent);
                    rightPanelPrimary.updateLayout(panelStartX, panelY, actualWidth, h1);
                    rightPanelPrimary.visible = true;
                    
                    rightPanelSecondary.setContentType(AmiConfig.rightPanelSecondaryContent);
                    rightPanelSecondary.updateLayout(panelStartX, panelY + h1 + PANEL_MARGIN, actualWidth, panelH - h1 - PANEL_MARGIN);
                    rightPanelSecondary.visible = true;
                } else if (hasRightPrimary) {
                    rightPanelPrimary.setContentType(AmiConfig.rightPanelContent);
                    rightPanelPrimary.updateLayout(panelStartX, panelY, actualWidth, panelH);
                    rightPanelPrimary.visible = true;
                    rightPanelSecondary.visible = false;
                } else {
                    rightPanelSecondary.setContentType(AmiConfig.rightPanelSecondaryContent);
                    rightPanelSecondary.updateLayout(panelStartX, panelY, actualWidth, panelH);
                    rightPanelSecondary.visible = true;
                    rightPanelPrimary.visible = false;
                }
            } else {
                resultsPanel.visible = rightPanelPrimary.visible = rightPanelSecondary.visible = false;
            }
            lastResultsBounds = new WidgetBounds(panelStartX, panelY, actualWidth, panelH);
        } else {
            resultsPanel.visible = rightPanelPrimary.visible = rightPanelSecondary.visible = false;
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

    private void computeLayoutsForCustomScreen(int screenW, int screenH) {
        // For non-vanilla container screens, use a centered results panel layout
        int leftEdge = screenW / 6;
        int rightEdge = screenW - screenW / 6;

        // Create a temporary simple layout - reuse the core logic with default positions
        ensureWidgets();
        lastScreenH = screenH;

        int btnY = screenH - BOTTOM_BAR_H + 2;
        amiButton.updateBounds(new WidgetBounds(2, btnY - 22, 22, 20));

        // Simple centered layout for custom screens
        leftPanel.visible = false;
        leftPanelSecondary.visible = false;
        rightPanelPrimary.visible = false;
        rightPanelSecondary.visible = false;
        resultsPanel.visible = true;

        int panelW = rightEdge - leftEdge;
        int panelH = screenH - BOTTOM_BAR_H - 40;
        resultsPanel.updateBounds(new WidgetBounds(leftEdge, 20, panelW, panelH));

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
