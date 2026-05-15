package com.sanhiruzu.ami.client.overlay;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.AMILayoutConfig;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.ProviderRegistry;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;

public class OverlayWidgetManager {
    private static final int BOTTOM_BAR_H = 24;
    private static final int SEARCH_H = 20;
    private static final int MIN_PANEL_WIDTH = 60;

    private final ResultsPanelWidget resultsPanel;
    private final SearchBarWidget searchBar;
    private final AmiButtonWidget amiButton;
    private final List<AmiWidget> widgets;

    private SearchService searchService = null;
    private volatile boolean indexingInProgress = false;
    private boolean indexingDispatched = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 5;

    private WidgetBounds lastResultsBounds = null;
    private int lastScreenH = 0;

    public OverlayWidgetManager() {
        this.resultsPanel = new ResultsPanelWidget();
        this.searchBar = new SearchBarWidget(this::triggerSearch);
        this.amiButton = new AmiButtonWidget(this::openAmiScreen);
        this.widgets = List.of(resultsPanel, searchBar, amiButton);
    }

    public void onRenderPost(ScreenEvent.Render.Post event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        try {
            // Indexing state machine
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

            // Compute layouts
            int screenW = event.getScreen().width;
            int screenH = event.getScreen().height;
            lastScreenH = screenH;

            computeLayouts(containerScreen, screenW, screenH);

            // Check if index has become stale
            checkAndRefreshIfStale();

            // Render all widgets
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0, 0, 1000);

            // First pass: render all widgets
            for (AmiWidget widget : widgets) {
                widget.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            }

            // Second pass: render overlays (dropdowns, etc.)
            for (AmiWidget widget : widgets) {
                widget.renderOverlay(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
            }

            event.getGuiGraphics().pose().popPose();

        } catch (Exception e) {
            AMI.LOGGER.error("AMI overlay render failed", e);
        }
    }

    private void computeLayouts(AbstractContainerScreen<?> containerScreen, int screenW, int screenH) {
        int panelH = screenH - BOTTOM_BAR_H;

        // Resolve panel side
        boolean goLeft = switch (AMILayoutConfig.PANEL_SIDE.get()) {
            case LEFT  -> true;
            case RIGHT -> false;
            case AUTO  -> InventoryOverlayHandler.RECIPE_VIEWER_PRESENT;
        };

        // Compute panel bounds
        int panelX, panelW;
        int widthOverride = AMILayoutConfig.PANEL_WIDTH_OVERRIDE.get();
        if (goLeft) {
            int available = containerScreen.getGuiLeft() - 12;
            panelW = widthOverride > 0 ? widthOverride : available;
            panelX = containerScreen.getGuiLeft() - panelW - 6;
        } else {
            panelX = containerScreen.getGuiLeft() + containerScreen.getXSize() + 6;
            int available = screenW - panelX - 6;
            panelW = widthOverride > 0 ? widthOverride : available;
        }

        if (panelW < MIN_PANEL_WIDTH) {
            resultsPanel.updateBounds(new WidgetBounds(0, 0, 0, 0));
            return;
        }

        // Layout results panel
        WidgetBounds panelBounds = new WidgetBounds(panelX, 0, panelW, panelH);
        resultsPanel.updateBounds(panelBounds);
        lastResultsBounds = panelBounds;

        // Layout search bar (centered on screen)
        int searchBarW = AMILayoutConfig.SEARCH_BAR_WIDTH.get();
        int searchBarX = (screenW - searchBarW) / 2;
        int searchBarY = screenH - BOTTOM_BAR_H + 2;
        WidgetBounds searchBarBounds = new WidgetBounds(searchBarX, searchBarY, searchBarW, SEARCH_H);
        searchBar.updateBounds(searchBarBounds);

        // Layout AMI button (screen lower-left)
        int btnX = 2;
        int btnY = screenH - BOTTOM_BAR_H + 2;
        WidgetBounds amiButtonBounds = new WidgetBounds(btnX, btnY, 22, SEARCH_H);
        amiButton.updateBounds(amiButtonBounds);
    }

    public void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        double mx = event.getMouseX(), my = event.getMouseY();

        // Priority: button, then searchBar (when focused), then panel
        if (amiButton.mouseClicked(mx, my, event.getButton())) {
            event.setCanceled(true);
            return;
        }

        if (searchBar.mouseClicked(mx, my, event.getButton())) {
            event.setCanceled(true);
            return;
        }

        // Check scrollbar priority
        if (resultsPanel.mouseClickedScrollbar(mx, my, event.getButton())) {
            event.setCanceled(true);
            return;
        }

        if (resultsPanel.mouseClicked(mx, my, event.getButton())) {
            event.setCanceled(true);
            return;
        }

        // Click outside all widgets: unfocus search
        if (!searchBar.isMouseOver(mx, my)) {
            searchBar.setFocused(false);
        }
    }

    public void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        if (resultsPanel.mouseDragged(event.getMouseX(), event.getMouseY(),
                event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    public void onMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        resultsPanel.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
    }

    public void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        if (resultsPanel.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    public void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        if (searchBar.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    public void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        if (searchBar.charTyped((char) event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    private void checkAndRefreshIfStale() {
        var panel = resultsPanel.getInnerPanel();
        if (panel == null) return;

        int totalCount = 0;
        for (NodeType t : NodeType.atlasValues()) {
            totalCount += GlobalIndex.getInstance().getNodes(t).size();
        }
        if (panel.getEntryCount() == 0 && totalCount > 0) {
            refreshEntries();
        }
    }

    private void refreshEntries() {
        var panel = resultsPanel.getInnerPanel();
        if (panel == null) return;

        List<SearchNode> all = new ArrayList<>();
        for (NodeType t : NodeType.atlasValues()) {
            all.addAll(GlobalIndex.getInstance().getNodes(t));
        }

        panel.setEntries(all);
        AMI.LOGGER.debug("AMI overlay refreshed: {} total entries across all types", all.size());
    }

    private void triggerSearch(String query) {
        var panel = resultsPanel.getInnerPanel();
        if (searchService == null || panel == null) return;

        if (query.isEmpty()) {
            refreshEntries();
            return;
        }
        var results = searchService.query(query);
        panel.setSearchResults(results, query);
    }

    private void openAmiScreen() {
        Minecraft.getInstance().setScreen(new com.sanhiruzu.ami.client.AMIScreen());
    }

    public WidgetBounds getResultsBounds() {
        return lastResultsBounds;
    }
}
