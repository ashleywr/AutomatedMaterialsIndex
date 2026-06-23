package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.api.AmiAdvancementDocument;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiGuideOpeners;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.client.entitydetails.EntityDetailsListView;
import com.sanhiruzu.ami.client.entitydetails.EntityDetailsQuery;
import com.sanhiruzu.ami.client.entitydetails.EntityDetailsReport;
import com.sanhiruzu.ami.client.entitydetails.EntityDetailsResolver;
import com.sanhiruzu.ami.client.sources.ItemSourceQuery;
import com.sanhiruzu.ami.client.sources.ItemSourceListView;
import com.sanhiruzu.ami.client.sources.ItemSourceReport;
import com.sanhiruzu.ami.client.sources.ItemSourceResolver;
import com.sanhiruzu.ami.client.results.*;
import com.sanhiruzu.ami.index.AmiRegistryDocumentIndex;
import com.sanhiruzu.ami.client.overlay.WidgetBounds;
import com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer;
import com.sanhiruzu.ami.compat.CompatRegistry;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.config.AmiDataFixes;
import com.sanhiruzu.ami.index.AmiIndexProgress;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.RegistryDocumentKind;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchService;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.player.PlayerWaypointProviders;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

public class UniversalResultsPanel implements SearchState.Listener {

    // Fixed height of the top header area — full mode reserves room for the embedded search/control bar.
    private static final int HEADER_H = 24;
    private static final int COMPACT_HEADER_H = 20; // Minimal height for item count + toggle button
    // Compact toggle button dimensions — height matches toolbar buttons for visual consistency
    private static final int TOGGLE_W = 22;
    private static final int TOGGLE_H = ResultsToolbar.BUTTON_H;
    private static final int COMPACT_CONTROL_W = 18;
    private static final int COMPACT_CONTROL_H = ResultsToolbar.BUTTON_H;
    // Sidebar header height
    private static final int FAV_HEADER_H = 18;
    private static final int FAV_CONTENT_TOP_PAD = 3;
    private static final int SIDEBAR_HEADER_TEXT_PAD_X = 6;
    private static final int SIDEBAR_HEADER_TEXT_PAD_TOP = 5;
    private static final int SIDEBAR_RAIL_MAX_W = 64;
    private static final int SIDEBAR_RAIL_MAX_H = 44;
    private static final int SIDEBAR_HEADER_CONTROL_W = 20;
    private static final int SIDEBAR_HEADER_CONTROL_H = 14;
    private static final int SIDEBAR_HEADER_CONTROL_GAP = 2;
    private static final int SIDEBAR_HEADER_EDGE_PAD = 2;
    private static final double DRAG_THRESHOLD = 5.0;
    private static final int GUIDE_HEADER_H = 12;
    private static final int GUIDE_ROW_H = 20;
    private static final int MAX_VISIBLE_GUIDE_ROWS = 3;
    private static final int MAX_VISIBLE_QUEST_ROWS = 3;
    private static final int MAX_VISIBLE_ADVANCEMENT_ROWS = 3;
    private static final int EMBEDDED_SEARCH_H = 18;
    private static final int EMBEDDED_SEARCH_MIN_W = 76;
    private static final int EMBEDDED_SEARCH_MAX_W = 190;
    private static final int EMBEDDED_TOOLBAR_MIN_W = 170;
    private static final int MIN_FULL_HEADER_TOOLBAR_W = 64;
    private final SearchState state = new SearchState();
    private int x, y, width, height;
    // Toggle button position — recomputed on every layout update
    private int toggleX, toggleY;
    private int compactSortX, compactSortY;
    private int compactCollapseX, compactCollapseY;
    private ResultsToolbar toolbar;
    private ResultsTreeView treeView;
    private ItemGridView gridView;
    private ItemSourceListView sourceView;
    private EntityDetailsListView entityDetailsView;
    private final ResultContextMenu contextMenu = new ResultContextMenu();
    private final ResultContextMenuActionBuilder contextMenuActions = new ResultContextMenuActionBuilder();
    private static final long LENS_DEBOUNCE_MS = 2000;
    private List<SearchNode> currentResults = new ArrayList<>();
    private String currentQuery = "";
    private SearchService searchService;
    private long searchServiceRevision = Long.MIN_VALUE;
    private long runtimeSearchRevision = Long.MIN_VALUE;
    private ResultsViewProjector.Projection emptyQueryProjectionCache = null;
    private String emptyQueryProjectionCacheKey = "";
    private List<ListLens> cachedAvailableLenses = null;
    private long lensesLastComputedMs = 0;
    private boolean lensesDirty = true;
    private List<GuideResultRow> currentGuideRows = List.of();
    private List<QuestResultRow> currentQuestRows = List.of();
    private List<AdvancementResultRow> currentAdvancementRows = List.of();
    private List<RegistryDocumentRow> currentRegistryDocumentRows = List.of();
    private Runnable externalResetCallback;
    private java.util.function.Consumer<String> onTokenInject;
    private java.util.function.Consumer<String> onQueryReplace;
    private Runnable externalModeToggleCallback;
    private java.util.function.BooleanSupplier externalModeToggleActive;
    private boolean isFavoritesPanel = false;
    private boolean compactMode = false;
    private boolean compactAutoBypass = false;
    private int compactAutoBypassW = -1;
    private int compactAutoBypassH = -1;
    private boolean compactCollapseAllNext = true;
    private boolean chromeOnly = false;
    private Component panelTitle = null;
    private Runnable onCollapseSidebarCallback;
    private ItemSourceReport activeSourceReport = null;
    private String sourceReturnQuery = null;
    private long activeSourceRevision = Long.MIN_VALUE;
    private EntityDetailsReport activeEntityDetailsReport = null;
    private String entityDetailsReturnQuery = null;
    private long activeEntityDetailsRevision = Long.MIN_VALUE;
    // Displayed item count shown in the compact header (updated in refreshTree)
    private int displayedItemCount = 0;
    private SearchNode pressedNode = null;
    private SearchNode draggedFavoriteNode = null;
    private double pressedX, pressedY;
    private int lastClickX, lastClickY;
    // State tracking to trigger auto-refreshes when player context changes
    private net.minecraft.world.level.GameType lastPlayerMode = null;
    private boolean lastDevMode = false;
    private boolean lastShowCreativeItems = false;
    private boolean lastDiscoveryChecklist = false;
    private long lastDiscoveryRevision = Long.MIN_VALUE;
    private long lastIndexProgressRefreshMs = 0L;

    public UniversalResultsPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        AmiViewPreferences.StoredView preferredView = AmiViewPreferences.loadMainPanelPreference();
        compactMode = preferredView == AmiViewPreferences.StoredView.COMPACT;
        state.addListener(this);
        initChildren();
        state.setViewMode(preferredView == AmiViewPreferences.StoredView.GRID
                ? ResultsToolbar.ViewMode.GRID
                : ResultsToolbar.ViewMode.LIST);
    }

    private static int countLeaves(List<TreeNode> nodes) {
        int count = 0;
        for (TreeNode node : nodes) {
            if (node.isLeaf()) count++;
            else count += countLeaves(node.getChildren());
        }
        return count;
    }

    private static boolean isShiftKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT;
    }

    public static WidgetBounds embeddedSearchBounds(WidgetBounds panelBounds) {
        int innerX = panelBounds.x() + AMITheme.GLOBAL_PADDING;
        int innerW = panelBounds.width() - AMITheme.GLOBAL_PADDING * 2;
        int searchX = innerX + TOGGLE_W + AMITheme.ELEMENT_GAP;
        return new WidgetBounds(searchX, panelBounds.y() + AMITheme.GLOBAL_PADDING + 1,
                embeddedSearchW(innerW), EMBEDDED_SEARCH_H);
    }

    public static boolean supportsEmbeddedSearch(WidgetBounds panelBounds) {
        int innerW = panelBounds.width() - AMITheme.GLOBAL_PADDING * 2;
        return innerW >= embeddedHeaderMinW();
    }

    private static boolean supportsMinimumFullHeader(WidgetBounds panelBounds) {
        int innerW = panelBounds.width() - AMITheme.GLOBAL_PADDING * 2;
        return innerW >= TOGGLE_W + AMITheme.ELEMENT_GAP + MIN_FULL_HEADER_TOOLBAR_W;
    }

    private static int embeddedHeaderMinW() {
        return TOGGLE_W + AMITheme.ELEMENT_GAP + EMBEDDED_SEARCH_MIN_W
                + AMITheme.ELEMENT_GAP + EMBEDDED_TOOLBAR_MIN_W;
    }

    private static int embeddedSearchW(int innerW) {
        int available = innerW - TOGGLE_W - AMITheme.ELEMENT_GAP * 2 - EMBEDDED_TOOLBAR_MIN_W;
        return net.minecraft.util.Mth.clamp(available, EMBEDDED_SEARCH_MIN_W, EMBEDDED_SEARCH_MAX_W);
    }

    private static int embeddedToolbarX(int innerX, int innerW) {
        return innerX + TOGGLE_W + AMITheme.ELEMENT_GAP + embeddedSearchW(innerW) + AMITheme.ELEMENT_GAP;
    }

    private static int embeddedToolbarW(int innerX, int innerW) {
        return Math.max(64, innerX + innerW - embeddedToolbarX(innerX, innerW));
    }

    private static int narrowToolbarX(int innerX) {
        return innerX + TOGGLE_W + AMITheme.ELEMENT_GAP;
    }

    private static int narrowToolbarW(int innerX, int innerW) {
        return Math.max(64, innerX + innerW - narrowToolbarX(innerX));
    }

    private boolean isCompactLayout() {
        if (compactMode) return true;
        if (isFavoritesPanel) return false;
        if (AmiConfig.disableAutoCompact) return false;
        if (supportsMinimumFullHeader(new WidgetBounds(x, y, width, height))) return false;
        return !compactAutoBypass || compactAutoBypassW != width || compactAutoBypassH != height;
    }

    private boolean isForcedCompactByScreenSize() {
        return !compactMode && !isFavoritesPanel && !AmiConfig.disableAutoCompact
                && !supportsMinimumFullHeader(new WidgetBounds(x, y, width, height));
    }

    private boolean isForcedCompactActive() {
        return isForcedCompactByScreenSize() && isCompactLayout();
    }

    private boolean isSidebarRailLayout() {
        return isFavoritesPanel && (width <= SIDEBAR_RAIL_MAX_W || height <= SIDEBAR_RAIL_MAX_H);
    }

    private int sidebarPadding() {
        return isSidebarRailLayout() ? 1 : AMITheme.GLOBAL_PADDING;
    }

    private void initChildren() {
        int innerX = x + sidebarPadding();
        int innerW = width - (sidebarPadding() * 2);

        boolean compact = isCompactLayout();
        int headerH = compact ? COMPACT_HEADER_H : HEADER_H;

        // View switch sits at the left edge of the header, before sort/group controls.
        this.toggleX = innerX;
        this.toggleY = y + AMITheme.GLOBAL_PADDING + (headerH - TOGGLE_H) / 2;
        updateCompactControlPositions(innerX, innerW);

        boolean embeddedSearch = supportsEmbeddedSearch(new WidgetBounds(x, y, width, height));
        int toolbarW = embeddedSearch ? embeddedToolbarW(innerX, innerW) : narrowToolbarW(innerX, innerW);
        int toolbarY = y + AMITheme.GLOBAL_PADDING;
        int toolbarX = embeddedSearch ? embeddedToolbarX(innerX, innerW) : narrowToolbarX(innerX);
        this.toolbar = new ResultsToolbar(toolbarX, toolbarY, toolbarW, state);

        int contentY, contentH;
        if (isSidebarRailLayout()) {
            contentY = y + 3;
            contentH = height - 6;
        } else if (isFavoritesPanel) {
            contentY = y + FAV_HEADER_H + FAV_CONTENT_TOP_PAD;
            contentH = height - FAV_HEADER_H - FAV_CONTENT_TOP_PAD - AMITheme.GLOBAL_PADDING;
        } else {
            contentY = y + AMITheme.GLOBAL_PADDING + headerH + AMITheme.ELEMENT_GAP;
            contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
        }

        this.treeView = new ResultsTreeView(innerX, contentY, innerW, contentH);
        this.gridView = new ItemGridView(innerX, contentY, innerW, contentH);
        this.sourceView = new ItemSourceListView(innerX, contentY, innerW, contentH);
        this.sourceView.setActionHandler(this::handleSourceAction);
        this.sourceView.setEntityInfoAvailable(RecipeViewerBridge::hasEntityInfo);
        this.entityDetailsView = new EntityDetailsListView(innerX, contentY, innerW, contentH);
        this.entityDetailsView.setActionHandler(this::handleEntityDetailsAction);
        this.gridView.setItemClickCallback(this::onItemClicked);
        this.treeView.setItemClickCallback(this::onItemClicked);
        this.gridView.setGroupClickCallback(this::onGroupClicked);
        this.treeView.setGroupClickCallback(this::onGroupClicked);
        this.gridView.setOnTokenInject(token -> {
            if (onTokenInject != null) onTokenInject.accept(token);
        });
        this.treeView.setOnTokenInject(token -> {
            if (onTokenInject != null) onTokenInject.accept(token);
        });

        toolbar.setCollapseExpandCallbacks(
                () -> {
                    treeView.collapseAll();
                    gridView.collapseAll();
                },
                () -> {
                    treeView.expandAll();
                    gridView.expandAll();
                }
        );
    }

    public void setEntries(List<SearchNode> entries) {
        setEntries(entries, false);
    }

    public void setEntries(List<SearchNode> entries, boolean incrementalUpdate) {
        closeSourceView();
        this.currentResults = entries;
        invalidateProjectionCache();
        if (incrementalUpdate) {
            lensesDirty = true;
        } else {
            forceLensRecompute();
        }
        refreshAvailableListLenses();
        refreshTree(incrementalUpdate);
    }

    /**
     * Directly set pre-built TreeNode roots, bypassing refreshTree().
     * Used by sidebar panels that need grouped display (e.g. quests).
     */
    public void setGroupedEntries(List<TreeNode> roots) {
        List<TreeNode> normalized = ResultsTreeNormalizer.normalize(roots);
        if (TreeNodeShape.sameVisibleContent(treeView.getRootNodes(), normalized)) {
            return;
        }
        ResultsExpansionDefaults.apply(normalized, AmiConfig.resultsExpandedByDefault);
        treeView.setRootNodes(normalized);
        gridView.setRootNodes(normalized);
        compactCollapseAllNext = AmiConfig.resultsExpandedByDefault;
        currentGuideRows = List.of();
        currentQuestRows = List.of();
        currentAdvancementRows = List.of();
        currentRegistryDocumentRows = List.of();
        this.displayedItemCount = countLeaves(normalized);
    }

    public void setOnModClick(java.util.function.Consumer<String> callback) {
        this.treeView.setOnModClick(callback);
    }

    public void setOnTreeTokenInject(java.util.function.Consumer<String> callback) {
        this.treeView.setOnTokenInject(callback);
    }

    public void setOnTokenInject(java.util.function.Consumer<String> callback) {
        this.onTokenInject = callback;
    }

    public void setOnQueryReplace(java.util.function.Consumer<String> callback) {
        this.onQueryReplace = callback;
    }

    public void setSearchResults(Map<NodeType, List<SearchNode>> results, String query) {
        closeSourceView();
        List<SearchNode> flat = new ArrayList<>();
        for (List<SearchNode> list : results.values()) flat.addAll(list);
        this.currentResults = flat;
        invalidateProjectionCache();
        forceLensRecompute();
        refreshAvailableListLenses();
        String normalizedQuery = query == null ? "" : query.trim();
        if (state.getQuery().equals(normalizedQuery)) {
            refreshTree();
        } else {
            state.setQuery(normalizedQuery);
        }
    }

    public void setSearchService(SearchService service) {
        setSearchService(service, Long.MIN_VALUE);
    }

    public boolean setSearchServiceIfChanged(SearchService service, long revision) {
        if (service == null) {
            return false;
        }
        if (this.searchService == service && this.searchServiceRevision == revision) {
            return false;
        }
        setSearchService(service, revision);
        return true;
    }

    public boolean setRuntimeSearchRevisionIfChanged(long revision) {
        if (this.runtimeSearchRevision == revision) {
            return false;
        }
        this.runtimeSearchRevision = revision;
        refreshTree(true);
        return true;
    }

    private void setSearchService(SearchService service, long revision) {
        this.searchService = service;
        this.searchServiceRevision = revision;
        invalidateProjectionCache();
        refreshTree();
    }

    public void setOnReset(Runnable callback) {
        this.externalResetCallback = callback;
    }

    public void setOnModeToggle(Runnable callback, java.util.function.BooleanSupplier activeSupplier) {
        this.externalModeToggleCallback = callback;
        this.externalModeToggleActive = activeSupplier;
    }

    public void configureView(AmiConfig.PanelContent content) {
        boolean oldCompact = compactMode;
        compactMode = content == AmiConfig.PanelContent.COMPACT;

        if (!oldCompact && compactMode) {
            resetSearchStateForCompact();
        } else if (content == AmiConfig.PanelContent.GRID || content == AmiConfig.PanelContent.COMPACT) {
            state.setViewMode(ResultsToolbar.ViewMode.GRID);
        } else if (content == AmiConfig.PanelContent.LIST) {
            state.setViewMode(ResultsToolbar.ViewMode.LIST);
        }

        if (oldCompact != compactMode) {
            // Delaying updateLayout if width/height aren't initialized yet
            if (width > 0 && height > 0) {
                updateLayout(x, y, width, height);
                refreshTree();
            }
        }
    }

    public void reset() {
        closeSourceView();
        state.reset();
        if (externalResetCallback != null) externalResetCallback.run();
    }

    @Override
    public void onSearchStateChanged(SearchState state) {
        this.currentQuery = state.getQuery();
        refreshTree();
    }

    public void updateLayout(int x, int y, int width, int height) {
        boolean oldCompactLayout = isCompactLayout();
        if (compactAutoBypass && (width != compactAutoBypassW || height != compactAutoBypassH)) {
            compactAutoBypass = false;
        }

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int innerX = x + sidebarPadding();
        int innerW = width - (sidebarPadding() * 2);

        // View switch stays at the left edge of the header, before sort/group controls.
        this.toggleX = innerX;
        this.toggleY = y + AMITheme.GLOBAL_PADDING;
        updateCompactControlPositions(innerX, innerW);

        if (isSidebarRailLayout()) {
            int contentY = y + 3;
            int contentH = height - 6;
            treeView.setTopContentHeight(0);
            gridView.setTopContentHeight(0);
            treeView.updateLayout(innerX, contentY, innerW, contentH);
            gridView.updateLayout(innerX, contentY, innerW, contentH);
            sourceView.updateLayout(innerX, contentY, innerW, contentH);
            entityDetailsView.updateLayout(innerX, contentY, innerW, contentH);
        } else if (isFavoritesPanel) {
            int contentY = y + FAV_HEADER_H + FAV_CONTENT_TOP_PAD;
            int contentH = height - FAV_HEADER_H - FAV_CONTENT_TOP_PAD - AMITheme.GLOBAL_PADDING;
            treeView.setTopContentHeight(0);
            gridView.setTopContentHeight(0);
            treeView.updateLayout(innerX, contentY, innerW, contentH);
            gridView.updateLayout(innerX, contentY, innerW, contentH);
            sourceView.updateLayout(innerX, contentY, innerW, contentH);
            entityDetailsView.updateLayout(innerX, contentY, innerW, contentH);
        } else {
            int headerH = isCompactLayout() ? COMPACT_HEADER_H : HEADER_H;
            int contentY = y + AMITheme.GLOBAL_PADDING + headerH + AMITheme.ELEMENT_GAP;
            int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
            if (isCompactLayout()) {
                treeView.setTopContentHeight(0);
                gridView.setTopContentHeight(0);
                gridView.updateLayout(innerX, contentY, innerW, contentH);
                sourceView.updateLayout(innerX, contentY, innerW, contentH);
                entityDetailsView.updateLayout(innerX, contentY, innerW, contentH);
            } else {
                boolean embeddedSearch = supportsEmbeddedSearch(new WidgetBounds(x, y, width, height));
                int toolbarX = embeddedSearch ? embeddedToolbarX(innerX, innerW) : narrowToolbarX(innerX);
                int toolbarW = embeddedSearch ? embeddedToolbarW(innerX, innerW) : narrowToolbarW(innerX, innerW);
                toolbar.updateLayout(toolbarX, y + AMITheme.GLOBAL_PADDING, toolbarW);
                updateResultViewLayouts(innerX, contentY, innerW, contentH);
            }
        }

        if (oldCompactLayout != isCompactLayout()) {
            refreshTree();
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        AmiRenderPhase.requireBase("UniversalResultsPanel.render");
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("panel.render")) {
            checkPlayerStateChanged();
            refreshIndexProgressIfNeeded();
            refreshSourceRouteIfNeeded();
            refreshEntityDetailsRouteIfNeeded();

            try (AmiRenderProfiler.Section chrome = AmiRenderProfiler.section("panel.chrome")) {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                if (isFavoritesPanel && (AMITheme.PANEL_BG & 0xFF000000) != 0xFF000000) {
                    g.fill(x, y, x + width, y + height, (AMITheme.PANEL_BG & 0x00FFFFFF) | 0xFF000000);
                }
                AMITheme.fillPanelChrome(g, x, y, width, height);

                // Add subtle borders, only if defined by the theme.
                if (AMITheme.BORDER_LIGHT != 0) {
                    g.fill(x + 2, y + 1, x + width - 2, y + 2, AMITheme.BORDER_LIGHT);
                    g.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, AMITheme.CONTROL_EDGE_DARK);
                }
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            }

            boolean compact = isCompactLayout();

            if (!isFavoritesPanel) {
                int headerY = y + AMITheme.GLOBAL_PADDING;
                int headerH = compact ? COMPACT_HEADER_H : HEADER_H;
                int sepY = headerY + headerH;
                int contentY = sepY + AMITheme.ELEMENT_GAP;
                renderPanelSurfaces(g, headerY, headerH, contentY);
            } else if (isSidebarRailLayout()) {
                renderSidebarRailSurfaces(g);
            } else if (!chromeOnly) {
                renderFavoritesSurfaces(g);
            }

            if (isSidebarRailLayout()) {
                if (displayedItemCount == 0) {
                    return;
                }
                gridView.render(g, mouseX, mouseY, contextMenu.isOpen());
                return;
            }

            if (isFavoritesPanel) {
                if (chromeOnly) {
                    return;
                }
                var font = Minecraft.getInstance().font;
                Component title = panelTitle != null ? panelTitle : Component.translatable("ami.gui.favorites");
                int titleX = x + 3 + SIDEBAR_HEADER_TEXT_PAD_X;
                int titleY = y + SIDEBAR_HEADER_TEXT_PAD_TOP;
                int titleRight = x + width - 3 - SIDEBAR_HEADER_TEXT_PAD_X;
                if (hasCollapseButton()) titleRight = Math.min(titleRight, collapseBtnX() - AMITheme.ELEMENT_GAP);
                if (hasSidebarAlternate()) titleRight = Math.min(titleRight, sidebarSwapX() - AMITheme.ELEMENT_GAP);
                String titleText = truncate(font, title.getString(), Math.max(0, titleRight - titleX));
                g.drawString(font, titleText, titleX, titleY, AMITheme.TEXT_HEADER, false);

                if (hasSidebarAlternate()) {
                    renderSidebarToggle(g, mouseX, mouseY);
                }
                if (hasCollapseButton()) {
                    renderSidebarCollapseBtn(g, mouseX, mouseY);
                }
                renderSidebarHeaderDivider(g);

                g.fill(x + 3, y + FAV_HEADER_H - 1, x + width - 3, y + FAV_HEADER_H, AMITheme.SECTION_SEP);

                if (isGridActive()) {
                    gridView.render(g, mouseX, mouseY, contextMenu.isOpen());
                } else {
                    treeView.render(g, mouseX, mouseY, contextMenu.isOpen(), null, state);
                }
                return;
            }

            // Shared header geometry — compact mode uses smaller header height
            int headerY = y + AMITheme.GLOBAL_PADDING;
            int headerH = compact ? COMPACT_HEADER_H : HEADER_H;
            int sepY = headerY + headerH;
            int contentY = sepY + AMITheme.ELEMENT_GAP;

            if (activeSourceReport != null) {
                renderSourceView(g, mouseX, mouseY, headerY, headerH, sepY);
                return;
            }
            if (activeEntityDetailsReport != null) {
                renderEntityDetailsView(g, mouseX, mouseY, headerY, headerH, sepY);
                return;
            }

            if (compact) {
                var font = Minecraft.getInstance().font;

                // Item count centered vertically in the header strip
                String countStr = Component.translatable("ami.gui.items_label", displayedItemCount).getString();
                int textY = headerY + (headerH - font.lineHeight) / 2;
                int countX = toggleX + TOGGLE_W + AMITheme.ELEMENT_GAP;
                int countRight = compactControlsFit() ? compactSortX - AMITheme.ELEMENT_GAP : x + width - AMITheme.GLOBAL_PADDING;
                String clippedCount = truncate(font, countStr, Math.max(0, countRight - countX));
                g.drawString(font, clippedCount, countX, textY, AMITheme.TEXT_SUBTLE, false);

                renderToggleBtn(g, mouseX, mouseY);
                renderCompactControls(g, mouseX, mouseY);

                g.fill(x + 3, sepY, x + width - 3, sepY + 1, AMITheme.SECTION_SEP);

                if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
                    renderCompactIndexingProgress(g, contentY);
                } else {
                    gridView.render(g, mouseX, mouseY, contextMenu.isOpen());
                }
                if (!contextMenu.isOpen()) {
                    renderToggleTooltip(g, mouseX, mouseY);
                    renderCompactControlTooltips(g, mouseX, mouseY);
                }
                return;
            }

            // Full mode
            var font = Minecraft.getInstance().font;
            toolbar.render(g, mouseX, mouseY);
            renderToggleBtn(g, mouseX, mouseY);

            g.fill(x + 3, sepY, x + width - 3, sepY + 1, AMITheme.SECTION_SEP);

            if (!com.sanhiruzu.ami.index.AmiIndexerService.getInstance().isReady() && currentResults.isEmpty() && currentQuery.isEmpty()) {
                renderIndexingProgress(g, contentY);
            } else {
                boolean dropdownOpen = toolbar.isAnyDropdownOpen() || contextMenu.isOpen();
                renderAdvancementRows(g, mouseX, mouseY);
                renderQuestRows(g, mouseX, mouseY);
                renderGuideRows(g, mouseX, mouseY);
                renderRegistryDocumentRows(g, mouseX, mouseY);
                if (isGridActive()) {
                    gridView.render(g, mouseX, mouseY, dropdownOpen);
                } else {
                    treeView.render(g, mouseX, mouseY, dropdownOpen, null, state);
                }
            }

            if (!toolbar.isAnyDropdownOpen() && !contextMenu.isOpen()) {
                renderToggleTooltip(g, mouseX, mouseY);
                renderAdvancementTooltip(g, mouseX, mouseY);
                renderQuestTooltip(g, mouseX, mouseY);
                renderGuideTooltip(g, mouseX, mouseY);
                renderRegistryDocumentTooltip(g, mouseX, mouseY);
            }
        }
    }

    private void renderPanelSurfaces(GuiGraphics g, int headerY, int headerH, int contentY) {
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("panel.surfaces")) {
            int panelX = x + 3;
            int panelW = width - 6;
            if (panelW <= 0) return;

            int headerSurfaceY = y + 3;
            int headerSurfaceH = Math.max(0, headerY + headerH - headerSurfaceY);
            AMITheme.fillPanelHeaderChrome(g, panelX, headerSurfaceY, panelW, headerSurfaceH);

            int contentSurfaceY = Math.max(headerY + headerH + 2, contentY - 2);
            int contentSurfaceH = y + height - 3 - contentSurfaceY;
            AMITheme.fillContentChrome(g, panelX, contentSurfaceY, panelW, contentSurfaceH);
        }
    }

    private void renderSourceView(GuiGraphics g, int mouseX, int mouseY, int headerY, int headerH, int sepY) {
        var font = Minecraft.getInstance().font;
        renderSourceBackButton(g, mouseX, mouseY);

        int titleX = toggleX + TOGGLE_W + AMITheme.ELEMENT_GAP;
        int titleRight = x + width - AMITheme.GLOBAL_PADDING;
        String title = activeSourceReport == null ? "" : activeSourceReport.title().getString();
        int textY = headerY + (headerH - font.lineHeight) / 2;
        g.drawString(font, truncate(font, title, Math.max(0, titleRight - titleX)), titleX, textY,
                AMITheme.TEXT_HEADER, false);
        g.fill(x + 3, sepY, x + width - 3, sepY + 1, AMITheme.SECTION_SEP);
        sourceView.render(g, mouseX, mouseY);
    }

    private void renderEntityDetailsView(GuiGraphics g, int mouseX, int mouseY, int headerY, int headerH, int sepY) {
        var font = Minecraft.getInstance().font;
        renderSourceBackButton(g, mouseX, mouseY);

        int titleX = toggleX + TOGGLE_W + AMITheme.ELEMENT_GAP;
        int titleRight = x + width - AMITheme.GLOBAL_PADDING;
        String title = activeEntityDetailsReport == null ? "" : activeEntityDetailsReport.title().getString();
        int textY = headerY + (headerH - font.lineHeight) / 2;
        g.drawString(font, truncate(font, title, Math.max(0, titleRight - titleX)), titleX, textY,
                AMITheme.TEXT_HEADER, false);
        g.fill(x + 3, sepY, x + width - 3, sepY + 1, AMITheme.SECTION_SEP);
        entityDetailsView.render(g, mouseX, mouseY);
    }

    private void renderSourceBackButton(GuiGraphics g, int mouseX, int mouseY) {
        boolean hovered = isOverToggle(mouseX, mouseY);
        int bg = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        g.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H, bg);
        g.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + 1, AMITheme.CONTROL_EDGE_LIGHT);
        g.fill(toggleX, toggleY + TOGGLE_H - 1, toggleX + TOGGLE_W, toggleY + TOGGLE_H, AMITheme.CONTROL_EDGE_DARK);
        var font = Minecraft.getInstance().font;
        String label = "<";
        g.drawString(font, label, toggleX + (TOGGLE_W - font.width(label)) / 2,
                toggleY + (TOGGLE_H - font.lineHeight) / 2, AMITheme.TEXT_HEADER, false);
    }

    private void refreshIndexProgressIfNeeded() {
        if (AmiIndexerService.getInstance().isReady()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastIndexProgressRefreshMs < 250L) {
            return;
        }
        lastIndexProgressRefreshMs = now;
        refreshTree();
    }

    private void refreshSourceRouteIfNeeded() {
        if (activeSourceReport == null || isFavoritesPanel || !ItemSourceQuery.isRoute(state.getQuery())) {
            return;
        }
        long indexRevision = GlobalIndex.getInstance().revision();
        boolean loadingChanged = activeSourceReport.loading() != AmiIndexerService.getInstance().isSourceIndexingPending();
        if (indexRevision != activeSourceRevision || loadingChanged) {
            refreshTree(true);
        }
    }

    private void refreshEntityDetailsRouteIfNeeded() {
        if (activeEntityDetailsReport == null || isFavoritesPanel || !EntityDetailsQuery.isRoute(state.getQuery())) {
            return;
        }
        long indexRevision = GlobalIndex.getInstance().revision();
        boolean loadingChanged = activeEntityDetailsReport.loading() != AmiIndexerService.getInstance().isSourceIndexingPending();
        if (indexRevision != activeEntityDetailsRevision || loadingChanged) {
            refreshTree(true);
        }
    }

    private void renderCompactIndexingProgress(GuiGraphics g, int contentY) {
        var font = Minecraft.getInstance().font;
        AmiIndexProgress progress = AmiIndexerService.getInstance().progress();
        int maxW = Math.max(0, width - (AMITheme.GLOBAL_PADDING * 2));
        String text = progressTitle(progress);
        String stats = progressStats(progress);
        if (!stats.isBlank()) {
            text = text + "  " + stats;
        }
        g.drawString(font, truncate(font, text, maxW), x + AMITheme.GLOBAL_PADDING, contentY, AMITheme.TEXT_SUBTLE, false);
        renderIndexProgressBar(g, x + AMITheme.GLOBAL_PADDING, contentY + font.lineHeight + 3, maxW, 6, progress.percent());
    }

    private void renderIndexingProgress(GuiGraphics g, int contentY) {
        var font = Minecraft.getInstance().font;
        AmiIndexProgress progress = AmiIndexerService.getInstance().progress();
        int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
        int textMaxWidth = Math.max(32, width - (AMITheme.GLOBAL_PADDING * 4));
        String title = progressTitle(progress);
        String detail = progress.detail();
        String stats = progressStats(progress);
        int barH = progress.percent() >= 0 ? 10 : 0;
        int detailH = detail == null || detail.isBlank() ? 0 : font.lineHeight + 4;
        int statsH = stats.isBlank() ? 0 : font.lineHeight + 5;
        int blockH = font.lineHeight + detailH + statsH + (barH > 0 ? barH + 8 : 0);
        int drawY = contentY + Math.max(0, (contentH - blockH) / 2);
        drawCenteredTruncated(g, font, title, x + width / 2, drawY, textMaxWidth, AMITheme.WHITE);
        drawY += font.lineHeight + 4;

        if (detail != null && !detail.isBlank()) {
            drawCenteredTruncated(g, font, detail, x + width / 2, drawY, textMaxWidth, AMITheme.TEXT_SUBTLE);
            drawY += font.lineHeight + 4;
        }

        if (!stats.isBlank()) {
            drawCenteredTruncated(g, font, stats, x + width / 2, drawY, textMaxWidth, AMITheme.TEXT_SUBTLE);
            drawY += font.lineHeight + 5;
        }

        int percent = progress.percent();
        if (percent >= 0) {
            int barW = Math.min(Math.max(72, width - AMITheme.GLOBAL_PADDING * 6), 180);
            int barX = x + (width - barW) / 2;
            renderIndexProgressBar(g, barX, drawY, barW, barH, percent);
        }
    }

    private static String progressTitle(AmiIndexProgress progress) {
        String phase = progress.phase();
        if (phase == null || phase.isBlank() || "Ready".equals(phase)) {
            String key = com.sanhiruzu.ami.config.AmiConfig.enableAutoIndexing
                    ? "ami.gui.background_indexing"
                    : "ami.gui.indexing_disabled";
            return Component.translatable(key).getString();
        }
        return phase;
    }

    private static String progressStats(AmiIndexProgress progress) {
        int percent = progress.percent();
        long elapsed = progress.elapsedMs();
        String elapsedText = formatElapsed(elapsed);
        if (percent >= 0) {
            String count = progress.total() > 0
                    ? " (" + Math.max(0, Math.min(progress.current(), progress.total())) + "/" + progress.total() + ")"
                    : "";
            return percent + "%" + count + (elapsedText.isBlank() ? "" : " - " + elapsedText);
        }
        return elapsedText;
    }

    private static String formatElapsed(long elapsedMs) {
        if (elapsedMs < 1000L) {
            return "";
        }
        long seconds = elapsedMs / 1000L;
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        if (minutes <= 0L) {
            return seconds + "s";
        }
        return minutes + "m " + remainingSeconds + "s";
    }

    private static void renderIndexProgressBar(GuiGraphics g, int barX, int barY, int barW, int barH, int percent) {
        if (percent < 0 || barW <= 0 || barH <= 0) {
            return;
        }
        int fillW = Math.max(0, Math.min(barW - 2, Math.round((barW - 2) * (percent / 100.0F))));
        int fillColor = progressFillColor();
        g.fill(barX, barY, barX + barW, barY + barH, AMITheme.SECTION_SEP);
        g.fill(barX + 1, barY + 1, barX + barW - 1, barY + barH - 1, AMITheme.DROPDOWN_BG);
        if (fillW > 0) {
            g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, fillColor);
        }
        if (barH >= 5 && fillW > 0) {
            g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + 2, 0x55FFFFFF);
            g.fill(barX + 1, barY + barH - 2, barX + 1 + fillW, barY + barH - 1, 0x33000000);
        }
    }

    private static int progressFillColor() {
        return (AMITheme.ACCENT_BLUE & 0xFF000000) == 0 ? 0xFF4FA3FF : AMITheme.ACCENT_BLUE;
    }

    private void renderFavoritesSurfaces(GuiGraphics g) {
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("panel.surfaces")) {
            int panelX = x + 3;
            int panelW = width - 6;
            if (panelW <= 0) return;

            int headerSurfaceY = y + 3;
            int headerSurfaceH = Math.max(0, FAV_HEADER_H - 3);
            AMITheme.fillPanelHeaderChrome(g, panelX, headerSurfaceY, panelW, headerSurfaceH);

            int contentSurfaceY = y + FAV_HEADER_H;
            int contentSurfaceH = y + height - 3 - contentSurfaceY;
            AMITheme.fillContentChrome(g, panelX, contentSurfaceY, panelW, contentSurfaceH);
        }
    }

    private void renderSidebarRailSurfaces(GuiGraphics g) {
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("panel.surfaces")) {
            int panelX = x + 2;
            int panelW = width - 4;
            if (panelW <= 0) return;
            AMITheme.fillContentChrome(g, panelX, y + 2, panelW, height - 4);
        }
    }

    public void renderOverlay(GuiGraphics g, int mouseX, int mouseY) {
        toolbar.renderOpenDropdownLists(g, mouseX, mouseY);
        contextMenu.render(g, mouseX, mouseY);
        renderResultTooltipOverlay(g, mouseX, mouseY);
    }

    private void renderResultTooltipOverlay(GuiGraphics g, int mouseX, int mouseY) {
        if (toolbar.isAnyDropdownOpen() || contextMenu.isOpen()) {
            return;
        }

        if (isGridActive()) {
            gridView.renderPendingTooltip(g, mouseX, mouseY);
        } else {
            treeView.renderPendingTooltip(g, mouseX, mouseY);
        }
    }

    private void renderSidebarToggle(GuiGraphics g, int mouseX, int mouseY) {
        int tx = sidebarSwapX();
        int ty = sidebarSwapY();
        boolean hovered = isOverSidebarSwap(mouseX, mouseY);

        int bgColor = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        AMITheme.fillControlChrome(g, tx, ty, SIDEBAR_HEADER_CONTROL_W, SIDEBAR_HEADER_CONTROL_H, bgColor, false);

        int color = hovered ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER;
        AmiGuiIcons.swap(g, tx + SIDEBAR_HEADER_CONTROL_W / 2, ty + SIDEBAR_HEADER_CONTROL_H / 2, color);
    }

    private void renderSidebarEmptyState(GuiGraphics g) {
        var font = Minecraft.getInstance().font;
        int contentTop = y + FAV_HEADER_H;
        int contentBottom = y + height - AMITheme.GLOBAL_PADDING;
        int centerY = contentTop + Math.max(0, contentBottom - contentTop) / 2;
        int centerX = x + width / 2;

        int ghostW = Math.min(86, Math.max(36, width - AMITheme.GLOBAL_PADDING * 4));
        int ghostH = 36;
        int ghostX = centerX - ghostW / 2;
        int ghostY = centerY - ghostH / 2 - 8;
        AMITheme.fillRounded(g, ghostX, ghostY, ghostW, ghostH, AMITheme.GRID_GROUP_BAND);
        g.fill(ghostX + 8, ghostY + 9, ghostX + ghostW - 8, ghostY + 10, AMITheme.SECTION_SEP);
        g.fill(ghostX + 8, ghostY + 18, ghostX + ghostW - 18, ghostY + 19, AMITheme.SECTION_SEP);
        g.fill(ghostX + 8, ghostY + 27, ghostX + ghostW - 28, ghostY + 28, AMITheme.SECTION_SEP);

        Component title = Component.translatable("ami.gui.sidebar.empty_state.title");
        Component hint = Component.translatable("ami.gui.sidebar.empty_state.hint");
        int titleY = ghostY + ghostH + 8;
        int textMaxW = Math.max(20, width - AMITheme.GLOBAL_PADDING * 2);
        int nextY = drawCenteredWrapped(g, font, title.getString(), centerX, titleY, textMaxW, AMITheme.TEXT_HEADER, 2);
        int remainingHintLines = Math.max(1, (contentBottom - nextY) / (font.lineHeight + 2));
        drawCenteredWrapped(g, font, hint.getString(), centerX, nextY + 2, textMaxW, AMITheme.TEXT_SUBTLE,
                Math.min(3, remainingHintLines));
    }

    private static void drawCenteredTruncated(GuiGraphics g, net.minecraft.client.gui.Font font, String text,
                                              int centerX, int y, int maxWidth, int color) {
        String clipped = truncate(font, text, maxWidth);
        g.drawString(font, clipped, centerX - font.width(clipped) / 2, y, color, false);
    }

    private static int drawCenteredWrapped(GuiGraphics g, net.minecraft.client.gui.Font font, String text,
                                           int centerX, int y, int maxWidth, int color, int maxLines) {
        int lineY = y;
        for (String line : wrapText(font, text, maxWidth, maxLines)) {
            g.drawString(font, line, centerX - font.width(line) / 2, lineY, color, false);
            lineY += font.lineHeight + 2;
        }
        return lineY;
    }

    private static List<String> wrapText(net.minecraft.client.gui.Font font, String text, int maxWidth, int maxLines) {
        if (text == null || text.isBlank() || maxWidth <= 0 || maxLines <= 0) return List.of();

        List<String> lines = new ArrayList<>();
        String remaining = text.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            String line = font.plainSubstrByWidth(remaining, maxWidth);
            if (line.length() < remaining.length()) {
                int wordBreak = line.lastIndexOf(' ');
                if (wordBreak > 0) {
                    line = line.substring(0, wordBreak);
                }
            }
            line = line.trim();
            if (line.isEmpty()) {
                line = font.plainSubstrByWidth(remaining, maxWidth).trim();
            }
            if (line.isEmpty()) break;

            remaining = remaining.substring(Math.min(line.length(), remaining.length())).trim();
            if (!remaining.isEmpty() && lines.size() == maxLines - 1) {
                line = truncate(font, line + "...", maxWidth);
            }
            lines.add(line);
        }
        return List.copyOf(lines);
    }

    private boolean hasSidebarAlternate() {
        return externalModeToggleCallback != null;
    }

    private boolean hasCollapseButton() {
        return onCollapseSidebarCallback != null;
    }

    private int sidebarSwapX() {
        return sidebarHeaderControlRightEdge() - sidebarHeaderControlGroupWidth();
    }

    private int sidebarSwapY() {
        return sidebarHeaderControlY();
    }

    private int collapseBtnX() {
        return sidebarHeaderControlRightEdge() - SIDEBAR_HEADER_CONTROL_W;
    }

    private int sidebarHeaderControlRightEdge() {
        return x + width - 3 - SIDEBAR_HEADER_EDGE_PAD;
    }

    private int sidebarHeaderControlY() {
        int headerSurfaceY = y + 3;
        int headerSurfaceH = Math.max(0, FAV_HEADER_H - 3);
        return headerSurfaceY + Math.max(0, (headerSurfaceH - SIDEBAR_HEADER_CONTROL_H) / 2);
    }

    private int sidebarHeaderControlGroupWidth() {
        int width = hasCollapseButton() ? SIDEBAR_HEADER_CONTROL_W : 0;
        if (hasSidebarAlternate()) {
            if (width > 0) {
                width += SIDEBAR_HEADER_CONTROL_GAP;
            }
            width += SIDEBAR_HEADER_CONTROL_W;
        }
        return width;
    }

    private void renderSidebarHeaderDivider(GuiGraphics g) {
        int controlWidth = sidebarHeaderControlGroupWidth();
        if (controlWidth <= 0) {
            return;
        }
        int dividerX = sidebarHeaderControlRightEdge() - controlWidth - SIDEBAR_HEADER_CONTROL_GAP;
        int dividerTop = y + 5;
        int dividerBottom = y + FAV_HEADER_H - 3;
        if (dividerBottom > dividerTop) {
            g.fill(dividerX, dividerTop, dividerX + 1, dividerBottom, AMITheme.SECTION_SEP);
        }
    }

    private boolean isOverSidebarSwap(double mouseX, double mouseY) {
        int tx = sidebarSwapX();
        int ty = sidebarSwapY();
        return mouseX >= tx && mouseX < tx + SIDEBAR_HEADER_CONTROL_W
                && mouseY >= ty && mouseY < ty + SIDEBAR_HEADER_CONTROL_H;
    }

    private boolean isOverCollapseBtn(double mouseX, double mouseY) {
        if (!hasCollapseButton()) return false;
        int bx = collapseBtnX();
        int by = sidebarSwapY();
        return mouseX >= bx && mouseX < bx + SIDEBAR_HEADER_CONTROL_W
                && mouseY >= by && mouseY < by + SIDEBAR_HEADER_CONTROL_H;
    }

    private void renderSidebarCollapseBtn(GuiGraphics g, int mouseX, int mouseY) {
        int bx = collapseBtnX();
        int by = sidebarSwapY();
        boolean hovered = isOverCollapseBtn(mouseX, mouseY);
        int bgColor = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        AMITheme.fillControlChrome(g, bx, by, SIDEBAR_HEADER_CONTROL_W, SIDEBAR_HEADER_CONTROL_H, bgColor, false);
        int iconColor = hovered ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE;
        AmiGuiIcons.sidebarCollapse(g, bx + SIDEBAR_HEADER_CONTROL_W / 2, by + SIDEBAR_HEADER_CONTROL_H / 2, iconColor);
    }

    public void setOnCollapseSidebar(Runnable callback) {
        this.onCollapseSidebarCallback = callback;
    }

    private void updateCompactControlPositions(int innerX, int innerW) {
        int right = innerX + innerW;
        compactCollapseX = right - COMPACT_CONTROL_W;
        compactSortX = compactCollapseX - AMITheme.ELEMENT_GAP - COMPACT_CONTROL_W;
        compactSortY = y + AMITheme.GLOBAL_PADDING + (COMPACT_HEADER_H - COMPACT_CONTROL_H) / 2;
        compactCollapseY = compactSortY;
    }

    private boolean compactControlsFit() {
        int countMinW = 34;
        return compactSortX - AMITheme.ELEMENT_GAP >= toggleX + TOGGLE_W + AMITheme.ELEMENT_GAP + countMinW;
    }

    private boolean isOverCompactSort(double mouseX, double mouseY) {
        return compactControlsFit()
                && mouseX >= compactSortX && mouseX < compactSortX + COMPACT_CONTROL_W
                && mouseY >= compactSortY && mouseY < compactSortY + COMPACT_CONTROL_H;
    }

    private boolean isOverCompactCollapse(double mouseX, double mouseY) {
        return compactControlsFit()
                && mouseX >= compactCollapseX && mouseX < compactCollapseX + COMPACT_CONTROL_W
                && mouseY >= compactCollapseY && mouseY < compactCollapseY + COMPACT_CONTROL_H;
    }

    private void renderCompactControls(GuiGraphics g, int mouseX, int mouseY) {
        if (!compactControlsFit()) return;

        renderCompactIconButton(g, compactSortX, compactSortY, COMPACT_CONTROL_W, COMPACT_CONTROL_H,
                isOverCompactSort(mouseX, mouseY),
                (cx, cy, color) -> AmiGuiIcons.sortDirection(g, cx, cy, color, state.isAscending()));
        renderCompactIconButton(g, compactCollapseX, compactCollapseY, COMPACT_CONTROL_W, COMPACT_CONTROL_H,
                isOverCompactCollapse(mouseX, mouseY),
                (cx, cy, color) -> {
                    if (compactCollapseAllNext) {
                        AmiGuiIcons.collapseAll(g, cx, cy, color);
                    } else {
                        AmiGuiIcons.expandAll(g, cx, cy, color);
                    }
                });
    }

    private void renderCompactIconButton(GuiGraphics g, int bx, int by, int bw, int bh, boolean hovered,
                                         IconPainter icon) {
        int bgColor = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        AMITheme.fillControlChrome(g, bx, by, bw, bh, bgColor, false);
        int color = hovered ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER;
        icon.paint(bx + bw / 2, by + bh / 2, color);
    }

    private void renderToggleBtn(GuiGraphics g, int mouseX, int mouseY) {
        boolean compact = isCompactLayout();
        boolean alternateActive = externalModeToggleActive != null && externalModeToggleActive.getAsBoolean();
        boolean hovered = mouseX >= toggleX && mouseX < toggleX + TOGGLE_W
                && mouseY >= toggleY && mouseY < toggleY + TOGGLE_H;

        int bgColor = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        AMITheme.fillControlChrome(g, toggleX, toggleY, TOGGLE_W, TOGGLE_H, bgColor, false);

        int contentColor = hovered ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER;
        int cx = toggleX + TOGGLE_W / 2;
        int cy = toggleY + TOGGLE_H / 2;
        AmiGuiIcons.resultBook(g, cx, cy, contentColor, isGridActive());
    }

    private void renderToggleTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (!isOverToggle(mouseX, mouseY)) return;
        List<Component> lines;
        if (isForcedCompactActive()) {
            lines = List.of(
                    Component.translatable("ami.gui.tooltip.compact_forced"),
                    Component.translatable("ami.gui.tooltip.compact_forced_action")
            );
        } else {
            lines = List.of(
                    Component.translatable(isGridActive() ? "ami.gui.tooltip.view_switch_to_list" : "ami.gui.tooltip.view_switch_to_grid"),
                    Component.translatable("ami.gui.tooltip.view_switch_detail")
            );
        }
        AmiTooltipRenderer.render(g, Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderCompactControlTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (isOverCompactSort(mouseX, mouseY)) {
            AmiTooltipRenderer.render(g, Minecraft.getInstance().font, List.of(
                    Component.translatable("ami.gui.tooltip.sort_direction", sortDirectionLabel()),
                    Component.translatable("ami.gui.tooltip.grid_reverse_scope")
            ), Optional.empty(), mouseX, mouseY);
        } else if (isOverCompactCollapse(mouseX, mouseY)) {
            AmiTooltipRenderer.render(g, Minecraft.getInstance().font, List.of(
                    Component.translatable(compactCollapseAllNext
                            ? "ami.gui.tooltip.collapse_all"
                            : "ami.gui.tooltip.expand_all")
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    private String sortDirectionLabel() {
        if (isGridActive()) {
            return state.isAscending() ? "Default" : "Reverse";
        }
        return state.getSortField().isNumeric()
                ? (state.isAscending() ? "Low" : "High")
                : (state.isAscending() ? "A-Z" : "Z-A");
    }

    private void renderGuideRows(GuiGraphics g, int mouseX, int mouseY) {
        if (!shouldShowGuideRows()) return;

        var font = Minecraft.getInstance().font;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int topY = sourceRowsTopY() + advancementSectionHeight() + questSectionHeight();
        int rowCount = visibleGuideRowCount();

        if (!intersectsContent(topY, guideSectionHeight())) return;
        g.enableScissor(innerX, contentY(), innerX + innerW, contentY() + contentHeight());
        try {
            g.fill(innerX, topY, innerX + innerW, topY + GUIDE_HEADER_H, AMITheme.GROUP_HEADER_BG);
            DocumentRowIconSprites.guide(g, innerX + 2, topY + 1, true);
            g.drawString(font, Component.translatable("ami.gui.guides").getString(), innerX + 18, topY + 2, AMITheme.TEXT_HEADER, false);

            int rowY = topY + GUIDE_HEADER_H;
            for (int i = 0; i < rowCount; i++) {
                GuideResultRow row = currentGuideRows.get(i);
                boolean hovered = guideRowAt(mouseX, mouseY) == row;
                int bg = i % 2 == 0 ? AMITheme.GRID_ROW_TINT_EVEN : AMITheme.GRID_ROW_TINT_ODD;
                g.fill(innerX, rowY, innerX + innerW, rowY + GUIDE_ROW_H, bg);
                if (hovered) {
                    g.fill(innerX, rowY, innerX + innerW, rowY + GUIDE_ROW_H, AMITheme.ENTRY_HOVER);
                }

                int iconX = innerX + 2;
                int iconY = rowY + 2;
                ItemStack bookStack = guideBookStack(row.document());
                if (!bookStack.isEmpty()) {
                    g.pose().pushPose();
                    g.renderItem(bookStack, iconX, iconY);
                    g.pose().popPose();
                } else {
                    int bx = iconX + 2;
                    int by = iconY + 2;
                    g.fill(bx, by, bx + 12, by + 12, AMITheme.DROPDOWN_BG);
                    g.fill(bx, by, bx + 12, by + 1, AMITheme.SECTION_SEP);
                    g.fill(bx, by + 11, bx + 12, by + 12, AMITheme.SECTION_SEP);
                    g.fill(bx, by, bx + 1, by + 12, AMITheme.SECTION_SEP);
                    g.fill(bx + 11, by, bx + 12, by + 12, AMITheme.SECTION_SEP);
                    DocumentRowIconSprites.guide(g, bx, by, false);
                }

                int textX = iconX + 18;
                int maxTextW = innerX + innerW - textX - 4;
                String title = truncate(font, row.title(), maxTextW);
                String subtitle = truncate(font, row.sourceLine() + " - " + row.provenanceLine(), maxTextW);
                g.drawString(font, title, textX, rowY + 2, AMITheme.TEXT_PRIMARY, false);
                g.drawString(font, subtitle, textX, rowY + 11, AMITheme.TEXT_SUBTLE, false);
                rowY += GUIDE_ROW_H;
            }

            int sepY = topY + guideSectionHeight() - AMITheme.ELEMENT_GAP;
            g.fill(innerX + 3, sepY, innerX + innerW - 3, sepY + 1, AMITheme.SECTION_SEP);
        } finally {
            g.disableScissor();
        }
    }

    private void renderAdvancementRows(GuiGraphics g, int mouseX, int mouseY) {
        if (!shouldShowAdvancementRows()) return;

        var font = Minecraft.getInstance().font;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int topY = sourceRowsTopY();
        int rowCount = visibleAdvancementRowCount();

        if (!intersectsContent(topY, advancementSectionHeight())) return;
        g.enableScissor(innerX, contentY(), innerX + innerW, contentY() + contentHeight());
        try {
            g.fill(innerX, topY, innerX + innerW, topY + GUIDE_HEADER_H, AMITheme.GROUP_HEADER_BG);
            DocumentRowIconSprites.advancement(g, innerX + 2, topY + 1, true);
            g.drawString(font, Component.translatable("ami.gui.advancement_results").getString(), innerX + 18, topY + 2, AMITheme.TEXT_HEADER, false);

            int rowY = topY + GUIDE_HEADER_H;
            for (int i = 0; i < rowCount; i++) {
                AdvancementResultRow row = currentAdvancementRows.get(i);
                boolean hovered = advancementRowAt(mouseX, mouseY) == row;
                int bg = i % 2 == 0 ? AMITheme.GRID_ROW_TINT_EVEN : AMITheme.GRID_ROW_TINT_ODD;
                g.fill(innerX, rowY, innerX + innerW, rowY + GUIDE_ROW_H, bg);
                if (hovered) {
                    g.fill(innerX, rowY, innerX + innerW, rowY + GUIDE_ROW_H, AMITheme.ENTRY_HOVER);
                }

                int iconX = innerX + 2;
                int iconY = rowY + 2;
                ItemStack iconStack = advancementIconStack(row.document());
                if (!iconStack.isEmpty()) {
                    g.pose().pushPose();
                    g.renderItem(iconStack, iconX, iconY);
                    g.pose().popPose();
                } else {
                    int bx = iconX + 2;
                    int by = iconY + 2;
                    g.fill(bx, by, bx + 12, by + 12, AMITheme.DROPDOWN_BG);
                    g.fill(bx, by, bx + 12, by + 1, AMITheme.SECTION_SEP);
                    g.fill(bx, by + 11, bx + 12, by + 12, AMITheme.SECTION_SEP);
                    g.fill(bx, by, bx + 1, by + 12, AMITheme.SECTION_SEP);
                    g.fill(bx + 11, by, bx + 12, by + 12, AMITheme.SECTION_SEP);
                    DocumentRowIconSprites.advancement(g, bx, by, false);
                }
                renderAdvancementStatusIcon(g, row.document(), iconX + 8, iconY + 8);

                int textX = iconX + 18;
                int maxTextW = innerX + innerW - textX - 4;
                String title = truncate(font, row.title(), maxTextW);
                String subtitle = truncate(font, row.sourceLine() + " - " + row.provenanceLine(), maxTextW);
                g.drawString(font, title, textX, rowY + 2, AMITheme.TEXT_PRIMARY, false);
                g.drawString(font, subtitle, textX, rowY + 11, AMITheme.TEXT_SUBTLE, false);
                rowY += GUIDE_ROW_H;
            }

            int sepY = topY + advancementSectionHeight() - AMITheme.ELEMENT_GAP;
            g.fill(innerX + 3, sepY, innerX + innerW - 3, sepY + 1, AMITheme.SECTION_SEP);
        } finally {
            g.disableScissor();
        }
    }

    private void renderAdvancementStatusIcon(GuiGraphics g, AmiAdvancementDocument document, int x, int y) {
        DocumentRowIconSprites.advancementStatus(g, document.progressStatus(), x, y);
    }

    private void renderQuestRows(GuiGraphics g, int mouseX, int mouseY) {
        if (!shouldShowQuestRows()) return;

        var font = Minecraft.getInstance().font;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int topY = sourceRowsTopY() + advancementSectionHeight();
        int rowCount = visibleQuestRowCount();

        if (!intersectsContent(topY, questSectionHeight())) return;
        g.enableScissor(innerX, contentY(), innerX + innerW, contentY() + contentHeight());
        try {
            g.fill(innerX, topY, innerX + innerW, topY + GUIDE_HEADER_H, AMITheme.GROUP_HEADER_BG);
            DocumentRowIconSprites.quest(g, innerX + 2, topY + 1, true);
            g.drawString(font, Component.translatable("ami.gui.quest_results").getString(), innerX + 18, topY + 2, AMITheme.TEXT_HEADER, false);

            int rowY = topY + GUIDE_HEADER_H;
            for (int i = 0; i < rowCount; i++) {
                QuestResultRow row = currentQuestRows.get(i);
                boolean hovered = questRowAt(mouseX, mouseY) == row;
                int bg = i % 2 == 0 ? AMITheme.GRID_ROW_TINT_EVEN : AMITheme.GRID_ROW_TINT_ODD;
                g.fill(innerX, rowY, innerX + innerW, rowY + GUIDE_ROW_H, bg);
                if (hovered) {
                    g.fill(innerX, rowY, innerX + innerW, rowY + GUIDE_ROW_H, AMITheme.ENTRY_HOVER);
                }

                int iconX = innerX + 4;
                int iconY = rowY + 4;
                g.fill(iconX, iconY, iconX + 12, iconY + 12, AMITheme.DROPDOWN_BG);
                g.fill(iconX, iconY, iconX + 12, iconY + 1, AMITheme.SECTION_SEP);
                g.fill(iconX, iconY + 11, iconX + 12, iconY + 12, AMITheme.SECTION_SEP);
                g.fill(iconX, iconY, iconX + 1, iconY + 12, AMITheme.SECTION_SEP);
                g.fill(iconX + 11, iconY, iconX + 12, iconY + 12, AMITheme.SECTION_SEP);
                DocumentRowIconSprites.quest(g, iconX, iconY, false);

                int textX = iconX + 16;
                int maxTextW = innerX + innerW - textX - 4;
                String title = truncate(font, row.title(), maxTextW);
                String subtitle = truncate(font, row.sourceLine() + " - " + row.provenanceLine(), maxTextW);
                g.drawString(font, title, textX, rowY + 2, AMITheme.TEXT_PRIMARY, false);
                g.drawString(font, subtitle, textX, rowY + 11, AMITheme.TEXT_SUBTLE, false);
                rowY += GUIDE_ROW_H;
            }

            int sepY = topY + questSectionHeight() - AMITheme.ELEMENT_GAP;
            g.fill(innerX + 3, sepY, innerX + innerW - 3, sepY + 1, AMITheme.SECTION_SEP);
        } finally {
            g.disableScissor();
        }
    }

    private void renderGuideTooltip(GuiGraphics g, int mouseX, int mouseY) {
        GuideResultRow row = guideRowAt(mouseX, mouseY);
        if (row == null) return;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(sanitizeTooltipText(row.title())).withStyle(ChatFormatting.WHITE));
        lines.add(Component.literal(sanitizeTooltipText(row.sourceLine())).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(sanitizeTooltipText(row.provenanceLine())).withStyle(ChatFormatting.DARK_GRAY));
        if ("silentgear_materials".equals(row.document().sourceType())) {
            for (MaterialTooltipLine line : materialSummaryTooltipLines(row.document().summaryText())) {
                lines.add(Component.literal(line.text()).withStyle(line.color()));
            }
        } else {
            row.evidence().stream()
                    .filter(evidence -> !evidence.snippet().isBlank())
                    .findFirst()
                    .ifPresent(evidence -> lines.add(Component.literal(sanitizeTooltipText(evidence.snippet()))
                            .withStyle(ChatFormatting.GRAY)));
        }
        AmiTooltipRenderer.render(g, Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderRegistryDocumentRows(GuiGraphics g, int mouseX, int mouseY) {
        if (!shouldShowRegistryDocumentRows()) return;

        var font = Minecraft.getInstance().font;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int topY = sourceRowsTopY() + advancementSectionHeight() + questSectionHeight() + guideSectionHeight();
        int rowCount = visibleRegistryDocumentRowCount();

        if (!intersectsContent(topY, registryDocumentSectionHeight())) return;
        g.enableScissor(innerX, contentY(), innerX + innerW, contentY() + contentHeight());
        try {
            g.fill(innerX, topY, innerX + innerW, topY + GUIDE_HEADER_H, AMITheme.GROUP_HEADER_BG);
            DocumentRowIconSprites.guide(g, innerX + 2, topY + 1, true);
            g.drawString(font, Component.translatable("ami.gui.registry_results").getString(), innerX + 18, topY + 2, AMITheme.TEXT_HEADER, false);

            int rowY = topY + GUIDE_HEADER_H;
            for (int i = 0; i < rowCount; i++) {
                RegistryDocumentRow row = currentRegistryDocumentRows.get(i);
                boolean hovered = registryDocumentRowAt(mouseX, mouseY) == row;
                int bg = i % 2 == 0 ? AMITheme.GRID_ROW_TINT_EVEN : AMITheme.GRID_ROW_TINT_ODD;
                g.fill(innerX, rowY, innerX + innerW, rowY + GUIDE_ROW_H, bg);
                if (hovered) {
                    g.fill(innerX, rowY, innerX + innerW, rowY + GUIDE_ROW_H, AMITheme.ENTRY_HOVER);
                }

                int iconX = innerX + 4;
                int iconY = rowY + 4;
                g.fill(iconX, iconY, iconX + 12, iconY + 12, AMITheme.DROPDOWN_BG);
                g.fill(iconX, iconY, iconX + 12, iconY + 1, AMITheme.SECTION_SEP);
                g.fill(iconX, iconY + 11, iconX + 12, iconY + 12, AMITheme.SECTION_SEP);
                g.fill(iconX, iconY, iconX + 1, iconY + 12, AMITheme.SECTION_SEP);
                g.fill(iconX + 11, iconY, iconX + 12, iconY + 12, AMITheme.SECTION_SEP);
                DocumentRowIconSprites.guide(g, iconX, iconY, false);

                int textX = iconX + 16;
                int maxTextW = innerX + innerW - textX - 4;
                String title = truncate(font, row.title(), maxTextW);
                String subtitle = truncate(font, row.subtitleLine(), maxTextW);
                g.drawString(font, title, textX, rowY + 2, AMITheme.TEXT_PRIMARY, false);
                g.drawString(font, subtitle, textX, rowY + 11, AMITheme.TEXT_SUBTLE, false);
                rowY += GUIDE_ROW_H;
            }

            int sepY = topY + registryDocumentSectionHeight() - AMITheme.ELEMENT_GAP;
            g.fill(innerX + 3, sepY, innerX + innerW - 3, sepY + 1, AMITheme.SECTION_SEP);
        } finally {
            g.disableScissor();
        }
    }

    private void renderRegistryDocumentTooltip(GuiGraphics g, int mouseX, int mouseY) {
        RegistryDocumentRow row = registryDocumentRowAt(mouseX, mouseY);
        if (row == null) return;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(sanitizeTooltipText(row.title())));
        lines.add(Component.literal(sanitizeTooltipText(row.subtitleLine())));
        AmiTooltipRenderer.render(g, Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderQuestTooltip(GuiGraphics g, int mouseX, int mouseY) {
        QuestResultRow row = questRowAt(mouseX, mouseY);
        if (row == null) return;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(sanitizeTooltipText(row.title())));
        lines.add(Component.literal(sanitizeTooltipText(row.sourceLine())));
        lines.add(Component.literal(sanitizeTooltipText(row.provenanceLine())));
        row.evidence().stream()
                .filter(evidence -> !evidence.snippet().isBlank())
                .findFirst()
                .ifPresent(evidence -> lines.add(Component.literal(sanitizeTooltipText(evidence.snippet()))));
        AmiTooltipRenderer.render(g, Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderAdvancementTooltip(GuiGraphics g, int mouseX, int mouseY) {
        AdvancementResultRow row = advancementRowAt(mouseX, mouseY);
        if (row == null) return;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(sanitizeTooltipText(row.title())));
        lines.add(Component.literal(sanitizeTooltipText(row.sourceLine())));
        lines.add(Component.literal(row.document().progressStatus().label()));
        lines.add(Component.literal(sanitizeTooltipText(row.provenanceLine())));
        row.evidence().stream()
                .filter(evidence -> !evidence.snippet().isBlank())
                .findFirst()
                .ifPresent(evidence -> lines.add(Component.literal(sanitizeTooltipText(evidence.snippet()))));
        AmiTooltipRenderer.render(g, Minecraft.getInstance().font, advancementIconStack(row.document()), lines, Optional.empty(), mouseX, mouseY);
    }

    private static String sanitizeTooltipText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String cleaned = value
                .replace("\r\n", "\n")
                .replace("\\r", " ")
                .replace("\\n", " ")
                .replace("\\t", " ")
                .replace("\u00A0", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ');
        StringBuilder out = new StringBuilder(cleaned.length());
        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            int type = Character.getType(ch);
            if (Character.isISOControl(ch)
                    || type == Character.LINE_SEPARATOR
                    || type == Character.PARAGRAPH_SEPARATOR
                    || type == Character.FORMAT
                    || ch == 0x00AD
                    || ch == 0x200B
                    || ch == 0xFEFF) {
                out.append(' ');
            } else {
                out.append(ch);
            }
        }
        return out.toString().trim();
    }

    private static List<MaterialTooltipLine> materialSummaryTooltipLines(String summary) {
        if (summary == null || summary.isBlank()) {
            return List.of();
        }
        List<MaterialTooltipLine> lines = new ArrayList<>();
        for (String paragraph : summary.replace('\r', '\n').split("\\n+")) {
            String clean = sanitizeTooltipText(paragraph);
            if (clean.isBlank()) {
                continue;
            }
            for (String part : splitMaterialSummaryParagraph(clean)) {
                String text = sanitizeTooltipText(part);
                if (!text.isBlank()) {
                    lines.add(new MaterialTooltipLine(text, materialTooltipColor(text)));
                }
                if (lines.size() >= 8) {
                    return lines;
                }
            }
        }
        return lines;
    }

    private static List<String> splitMaterialSummaryParagraph(String paragraph) {
        String withBreaks = paragraph
                .replaceAll("\\s+(Categories:)", "\n$1")
                .replaceAll("\\s+(Durability|Armor toughness|Armor|Attack damage|Harvest speed|Enchantment)\\s+([-+]?\\d)", "\n$1 $2")
                .replaceAll("\\s+(Traits:)\\s+", "\n$1 ");
        List<String> lines = new ArrayList<>();
        for (String line : withBreaks.split("\\n+|;\\s*")) {
            String clean = sanitizeTooltipText(line);
            if (!clean.isBlank()) {
                lines.add(clean);
            }
        }
        return lines;
    }

    private static ChatFormatting materialTooltipColor(String line) {
        String lower = line.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("categories:")) {
            return ChatFormatting.AQUA;
        }
        if (lower.startsWith("traits:")) {
            return ChatFormatting.LIGHT_PURPLE;
        }
        if (lower.startsWith("durability") || lower.startsWith("armor") || lower.startsWith("attack damage")
                || lower.startsWith("harvest speed") || lower.startsWith("enchantment")) {
            return ChatFormatting.GREEN;
        }
        return ChatFormatting.GRAY;
    }

    private record MaterialTooltipLine(String text, ChatFormatting color) {
    }

    // ── Tree refresh ──────────────────────────────────────────────────────────

    private void checkPlayerStateChanged() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.level.GameType mode = mc.gameMode == null ? null : mc.gameMode.getPlayerMode();
        boolean devMode = AmiConfig.devMode;
        boolean showCreativeItems = AmiConfig.shouldShowCreativeItems(mode);
        boolean discoveryChecklist = AmiConfig.enableDiscoveryChecklist;
        long discoveryRevision = com.sanhiruzu.ami.client.discovery.AmiDiscoveryState.getInstance().revision();
        boolean playerContextChanged = mode != lastPlayerMode
                || devMode != lastDevMode
                || showCreativeItems != lastShowCreativeItems;
        boolean discoveryChanged = discoveryChecklist != lastDiscoveryChecklist
                || discoveryRevision != lastDiscoveryRevision;
        if (playerContextChanged || discoveryChanged) {
            lastPlayerMode = mode;
            lastDevMode = devMode;
            lastShowCreativeItems = showCreativeItems;
            lastDiscoveryChecklist = discoveryChecklist;
            lastDiscoveryRevision = discoveryRevision;
            invalidateProjectionCache();
            forceLensRecompute();
            refreshAvailableListLenses();
            refreshTree(!playerContextChanged);
        }
    }







    private void refreshTree() {
        refreshTree(false);
    }

    private void refreshTree(boolean incrementalUpdate) {
        // Skip expensive tree rebuilds if we are hidden
        var manager = com.sanhiruzu.ami.client.InventoryOverlayHandler.getManager();
        if (manager != null && !manager.isPanelVisible() && !isFavoritesPanel) return;

        if (!isFavoritesPanel && openSourceRoute(state.getQuery())) {
            displayedItemCount = 0;
            currentAdvancementRows = List.of();
            currentGuideRows = List.of();
            currentQuestRows = List.of();
            currentRegistryDocumentRows = List.of();
            updateResultViewLayouts();
            setViewRoots(List.of(), incrementalUpdate);
            return;
        }
        if (!isFavoritesPanel && openEntityDetailsRoute(state.getQuery())) {
            displayedItemCount = 0;
            currentAdvancementRows = List.of();
            currentGuideRows = List.of();
            currentQuestRows = List.of();
            currentRegistryDocumentRows = List.of();
            updateResultViewLayouts();
            setViewRoots(List.of(), incrementalUpdate);
            return;
        }
        if (activeSourceReport != null) {
            closeSourceView();
        }
        if (activeEntityDetailsReport != null) {
            closeEntityDetailsView();
        }

        ResultsViewProjector.Projection projection = projectResults();
        displayedItemCount = projection.displayedItemCount();
        currentAdvancementRows = projection.advancementRows();
        currentGuideRows = projection.guideRows();
        currentQuestRows = projection.questRows();
        currentRegistryDocumentRows = projection.registryDocumentRows();
        updateResultViewLayouts();
        setViewRoots(projection.roots(), incrementalUpdate);
    }

    private ResultsViewProjector.Projection projectResults() {
        String cacheKey = emptyQueryProjectionCacheKey();
        if (cacheKey != null && cacheKey.equals(emptyQueryProjectionCacheKey) && emptyQueryProjectionCache != null) {
            return emptyQueryProjectionCache;
        }

        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                resolveSource(),
                state,
                searchService,
                AmiConfig.searchIncludeGuides ? AmiIndexerService.getInstance().getGuideSearchIndex() : null,
                AmiConfig.searchIncludeQuests ? AmiQuestsApi.getQuestSearchIndex() : null,
                AmiConfig.searchIncludeAdvancements ? AdvancementRuntimeDocuments.searchIndex() : null,
                AmiIndexerService.getInstance().getRegistryDocumentIndex(),
                isCompactLayout() && !isFavoritesPanel,
                isFavoritesPanel
        );

        if (cacheKey != null) {
            emptyQueryProjectionCacheKey = cacheKey;
            emptyQueryProjectionCache = projection;
        }
        return projection;
    }

    private String emptyQueryProjectionCacheKey() {
        if (!state.getQuery().isBlank()) {
            return null;
        }
        List<SearchNode> source = resolveSource();
        return GlobalIndex.getInstance().revision() + "|searchRevision=" + searchServiceRevision
                + "|sourceSignature=" + sourceSignature(source)
                + "|view=" + state.getViewMode()
                + "|lens=" + state.getListLens()
                + "|sort=" + state.getSortField()
                + "|asc=" + state.isAscending()
                + "|group=" + state.getGroupBy()
                + "|mods=" + new TreeSet<>(state.getSelectedMods())
                + "|facets=" + new TreeSet<>(state.getActiveFacets())
                + "|compact=" + (isCompactLayout() && !isFavoritesPanel)
                + "|favorites=" + isFavoritesPanel
                + "|dev=" + AmiConfig.devMode
                + "|cheat=" + AmiConfig.cheatMode
                + "|hidden=" + AmiConfig.showHiddenModItems
                + "|creative=" + AmiConfig.shouldShowCreativeItems(lastPlayerMode)
                + "|searchGuides=" + AmiConfig.searchIncludeGuides
                + "|searchQuests=" + AmiConfig.searchIncludeQuests
                + "|searchAdvancements=" + AmiConfig.searchIncludeAdvancements
                + "|searchEntities=" + AmiConfig.searchIncludeEntities
                + "|searchPlayers=" + AmiConfig.searchIncludePlayers
                + "|searchWaypoints=" + AmiConfig.searchIncludeWaypoints
                + "|searchEnchantments=" + AmiConfig.searchIncludeEnchantments
                + "|searchEffects=" + AmiConfig.searchIncludeMobEffects
                + "|searchTags=" + AmiConfig.searchIncludeTags
                + "|searchPaintings=" + AmiConfig.searchIncludePaintings
                + "|searchGameRules=" + AmiConfig.searchIncludeGameRules
                + "|discoveryEnabled=" + AmiConfig.enableDiscoveryChecklist
                + "|discoveryRevision=" + com.sanhiruzu.ami.client.discovery.AmiDiscoveryState.getInstance().revision();
    }

    private static long sourceSignature(List<SearchNode> source) {
        long signature = 1125899906842597L;
        for (SearchNode node : source) {
            signature = 31 * signature + node.type().hashCode();
            signature = 31 * signature + node.id().hashCode();
            signature = 31 * signature + node.displayName().hashCode();
            signature = 31 * signature + node.metadata().hashCode();
        }
        return signature;
    }

    private void invalidateProjectionCache() {
        emptyQueryProjectionCache = null;
        emptyQueryProjectionCacheKey = "";
    }

    private void refreshAvailableListLenses() {
        if (!lensesDirty) return;
        long now = System.currentTimeMillis();
        if (cachedAvailableLenses != null && now - lensesLastComputedMs < LENS_DEBOUNCE_MS) return;
        cachedAvailableLenses = ListLens.availableFor(ResultsViewProjector.applyRuntimeMetadataForLens(resolveSource()));
        lensesLastComputedMs = now;
        lensesDirty = false;
        state.setAvailableListLenses(cachedAvailableLenses);
    }

    private void forceLensRecompute() {
        lensesLastComputedMs = 0;
        lensesDirty = true;
    }

    private void setViewRoots(List<TreeNode> roots) {
        setViewRoots(roots, false);
    }

    private void setViewRoots(List<TreeNode> roots, boolean incrementalUpdate) {
        List<TreeNode> normalized = ResultsTreeNormalizer.normalize(roots);
        List<TreeNode> currentRoots = treeView.getRootNodes();
        if (TreeNodeShape.sameVisibleContent(currentRoots, normalized)) {
            return;
        }
        boolean resetScroll = !incrementalUpdate;
        if (incrementalUpdate && !currentRoots.isEmpty()) {
            ResultsExpansionDefaults.transferExpansionState(currentRoots, normalized);
        } else {
            ResultsExpansionDefaults.apply(normalized, AmiConfig.resultsExpandedByDefault);
        }
        treeView.setRootNodes(normalized, resetScroll);
        gridView.setRootNodes(normalized, resetScroll);
        if (resetScroll) {
            toolbar.resetCollapseState(AmiConfig.resultsExpandedByDefault);
            compactCollapseAllNext = AmiConfig.resultsExpandedByDefault;
        }
    }

    private void updateResultViewLayouts() {
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        if (isFavoritesPanel) {
            int contentY = y + FAV_HEADER_H + FAV_CONTENT_TOP_PAD;
            int contentH = height - FAV_HEADER_H - FAV_CONTENT_TOP_PAD - AMITheme.GLOBAL_PADDING;
            treeView.setTopContentHeight(0);
            gridView.setTopContentHeight(0);
            treeView.updateLayout(innerX, contentY, innerW, contentH);
            gridView.updateLayout(innerX, contentY, innerW, contentH);
            sourceView.updateLayout(innerX, contentY, innerW, contentH);
            entityDetailsView.updateLayout(innerX, contentY, innerW, contentH);
            return;
        }

        int headerH = isCompactLayout() ? COMPACT_HEADER_H : HEADER_H;
        int contentY = y + AMITheme.GLOBAL_PADDING + headerH + AMITheme.ELEMENT_GAP;
        int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
        updateResultViewLayouts(innerX, contentY, innerW, contentH);
    }

    private void updateResultViewLayouts(int innerX, int contentY, int innerW, int contentH) {
        int sourceSectionsH = sourceSectionsHeight();
        treeView.setTopContentHeight(sourceSectionsH);
        gridView.setTopContentHeight(sourceSectionsH);
        treeView.updateLayout(innerX, contentY, innerW, contentH);
        gridView.updateLayout(innerX, contentY, innerW, contentH);
        sourceView.updateLayout(innerX, contentY, innerW, contentH);
        entityDetailsView.updateLayout(innerX, contentY, innerW, contentH);
    }

    private int contentY() {
        int headerH = isCompactLayout() ? COMPACT_HEADER_H : HEADER_H;
        return y + AMITheme.GLOBAL_PADDING + headerH + AMITheme.ELEMENT_GAP;
    }

    private int contentHeight() {
        return height - (contentY() - y) - AMITheme.GLOBAL_PADDING;
    }

    private int activeResultScrollOffset() {
        return isGridActive() ? gridView.getPixelScrollOffset() : treeView.getPixelScrollOffset();
    }

    private int sourceRowsTopY() {
        return contentY() - activeResultScrollOffset();
    }

    private boolean intersectsContent(int topY, int sectionHeight) {
        if (sectionHeight <= 0) return false;
        int contentTop = contentY();
        int contentBottom = contentTop + contentHeight();
        return topY + sectionHeight > contentTop && topY < contentBottom;
    }

    private boolean shouldShowGuideRows() {
        return !isCompactLayout()
                && !isFavoritesPanel
                && !currentQuery.isBlank()
                && !currentGuideRows.isEmpty();
    }

    private boolean shouldShowAdvancementRows() {
        return !isCompactLayout()
                && !isFavoritesPanel
                && !currentQuery.isBlank()
                && !currentAdvancementRows.isEmpty();
    }

    private boolean shouldShowQuestRows() {
        return !isCompactLayout()
                && !isFavoritesPanel
                && !currentQuery.isBlank()
                && !currentQuestRows.isEmpty();
    }

    private int visibleGuideRowCount() {
        return Math.min(MAX_VISIBLE_GUIDE_ROWS, currentGuideRows.size());
    }

    private int visibleAdvancementRowCount() {
        return Math.min(MAX_VISIBLE_ADVANCEMENT_ROWS, currentAdvancementRows.size());
    }

    private int visibleQuestRowCount() {
        return Math.min(MAX_VISIBLE_QUEST_ROWS, currentQuestRows.size());
    }

    private int guideSectionHeight() {
        if (!shouldShowGuideRows()) return 0;
        return GUIDE_HEADER_H + visibleGuideRowCount() * GUIDE_ROW_H + AMITheme.ELEMENT_GAP;
    }

    private int advancementSectionHeight() {
        if (!shouldShowAdvancementRows()) return 0;
        return GUIDE_HEADER_H + visibleAdvancementRowCount() * GUIDE_ROW_H + AMITheme.ELEMENT_GAP;
    }

    private int questSectionHeight() {
        if (!shouldShowQuestRows()) return 0;
        return GUIDE_HEADER_H + visibleQuestRowCount() * GUIDE_ROW_H + AMITheme.ELEMENT_GAP;
    }

    private boolean shouldShowRegistryDocumentRows() {
        return !isCompactLayout()
                && !isFavoritesPanel
                && !currentQuery.isBlank()
                && !currentRegistryDocumentRows.isEmpty();
    }

    private int visibleRegistryDocumentRowCount() {
        return Math.min(MAX_VISIBLE_GUIDE_ROWS, currentRegistryDocumentRows.size());
    }

    private int registryDocumentSectionHeight() {
        if (!shouldShowRegistryDocumentRows()) return 0;
        return GUIDE_HEADER_H + visibleRegistryDocumentRowCount() * GUIDE_ROW_H + AMITheme.ELEMENT_GAP;
    }

    private int sourceSectionsHeight() {
        return advancementSectionHeight() + questSectionHeight() + guideSectionHeight() + registryDocumentSectionHeight();
    }

    private GuideResultRow guideRowAt(double mouseX, double mouseY) {
        if (!shouldShowGuideRows()) return null;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int rowY = sourceRowsTopY() + advancementSectionHeight() + questSectionHeight() + GUIDE_HEADER_H;
        if (mouseX < innerX || mouseX >= innerX + innerW) return null;
        if (mouseY < contentY() || mouseY >= contentY() + contentHeight()) return null;
        int row = ((int) mouseY - rowY) / GUIDE_ROW_H;
        if (row < 0 || row >= visibleGuideRowCount()) return null;
        int y0 = rowY + row * GUIDE_ROW_H;
        if (mouseY < y0 || mouseY >= y0 + GUIDE_ROW_H) return null;
        return currentGuideRows.get(row);
    }

    private QuestResultRow questRowAt(double mouseX, double mouseY) {
        if (!shouldShowQuestRows()) return null;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int rowY = sourceRowsTopY() + advancementSectionHeight() + GUIDE_HEADER_H;
        if (mouseX < innerX || mouseX >= innerX + innerW) return null;
        if (mouseY < contentY() || mouseY >= contentY() + contentHeight()) return null;
        int row = ((int) mouseY - rowY) / GUIDE_ROW_H;
        if (row < 0 || row >= visibleQuestRowCount()) return null;
        int y0 = rowY + row * GUIDE_ROW_H;
        if (mouseY < y0 || mouseY >= y0 + GUIDE_ROW_H) return null;
        return currentQuestRows.get(row);
    }

    private AdvancementResultRow advancementRowAt(double mouseX, double mouseY) {
        if (!shouldShowAdvancementRows()) return null;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int rowY = sourceRowsTopY() + GUIDE_HEADER_H;
        if (mouseX < innerX || mouseX >= innerX + innerW) return null;
        if (mouseY < contentY() || mouseY >= contentY() + contentHeight()) return null;
        int row = ((int) mouseY - rowY) / GUIDE_ROW_H;
        if (row < 0 || row >= visibleAdvancementRowCount()) return null;
        int y0 = rowY + row * GUIDE_ROW_H;
        if (mouseY < y0 || mouseY >= y0 + GUIDE_ROW_H) return null;
        return currentAdvancementRows.get(row);
    }

    private RegistryDocumentRow registryDocumentRowAt(double mouseX, double mouseY) {
        if (!shouldShowRegistryDocumentRows()) return null;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int rowY = sourceRowsTopY() + advancementSectionHeight() + questSectionHeight() + guideSectionHeight() + GUIDE_HEADER_H;
        if (mouseX < innerX || mouseX >= innerX + innerW) return null;
        if (mouseY < contentY() || mouseY >= contentY() + contentHeight()) return null;
        int row = ((int) mouseY - rowY) / GUIDE_ROW_H;
        if (row < 0 || row >= visibleRegistryDocumentRowCount()) return null;
        int y0 = rowY + row * GUIDE_ROW_H;
        if (mouseY < y0 || mouseY >= y0 + GUIDE_ROW_H) return null;
        return currentRegistryDocumentRows.get(row);
    }

    private static void tryOpenGuide(AmiGuideDocument document) {
        if (document.canOpen()) {
            try { document.open(); } catch (RuntimeException ignored) {}
            return;
        }
        ResourceLocation bookId = document.bookId();
        if (bookId != null) {
            AmiGuideOpeners.patchouli(bookId, document.pageId()).run();
        }
    }

    private static ItemStack guideBookStack(AmiGuideDocument document) {
        if (document == null || document.bookId() == null) {
            return ItemStack.EMPTY;
        }

        ItemStack iconStack = guideIconStack(document);
        if (!iconStack.isEmpty()) {
            return iconStack;
        }

        ItemStack directBook = new ItemStack(BuiltInRegistries.ITEM.get(document.bookId()));
        if (!directBook.isEmpty()) {
            return directBook;
        }

        for (SearchNode node : GlobalIndex.getInstance().getNodes(NodeType.ITEM)) {
            if (!document.bookId().toString().equals(node.meta(SearchNodeKeys.GUIDE_BOOK_ID, ""))) {
                continue;
            }
            try {
                ItemStack stack = com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node);
                if (!stack.isEmpty()) {
                    return stack;
                }
            } catch (RuntimeException ignored) {
                // Keep trying other guidebook candidates.
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack guideIconStack(AmiGuideDocument document) {
        ResourceLocation iconItemId = document.iconItemId();
        if (iconItemId == null) {
            return ItemStack.EMPTY;
        }
        for (SearchNode node : GlobalIndex.getInstance().getNodes(NodeType.ITEM)) {
            if (!iconItemId.equals(node.id())) {
                continue;
            }
            if (!document.bookId().toString().equals(node.meta(SearchNodeKeys.GUIDE_BOOK_ID, ""))) {
                return ItemStack.EMPTY;
            }
            try {
                ItemStack stack = com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node);
                return stack.isEmpty() ? new ItemStack(BuiltInRegistries.ITEM.get(iconItemId)) : stack;
            } catch (RuntimeException ignored) {
                return new ItemStack(BuiltInRegistries.ITEM.get(iconItemId));
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack advancementIconStack(AmiAdvancementDocument document) {
        if (document == null || document.iconItemId() == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(document.iconItemId()));
    }

    private static String truncate(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    // ── Item click (grid + list) ──────────────────────────────────────────────

    private List<SearchNode> resolveSource() {
        if (isFavoritesPanel || searchService == null) {
            return currentResults;
        }
        LinkedHashSet<SearchNode> combined = new LinkedHashSet<>(currentResults);
        combined.addAll(searchService.defaultResults());
        return new ArrayList<>(combined);
    }

    private void onItemClicked(SearchNode node, int button) {
        if (node == null) {
            return;
        }
        if (button == 1) {
            openItemContextMenu(node, lastClickX, lastClickY);
            return;
        }

        if (CompatRegistry.handleResultClick(node, button)) {
            return;
        }

        if (node != null && node.type() == NodeType.WAYPOINT) {
            PlayerWaypointProviders.openLiveWaypointAction(node)
                    .ifPresent(action -> action.action().run());
            return;
        }

        ItemStack stack = com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node);
        boolean shiftDown = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        boolean controlDown = net.minecraft.client.gui.screens.Screen.hasControlDown();
        if (!stack.isEmpty() && shouldOpenLookup(node, stack, button, shiftDown, controlDown)) {
            RecipeViewerBridge.handleItemClick(stack, button, shiftDown, controlDown);
            return;
        }
        if (stack.isEmpty() && shouldOpenLookup(node, stack, button, shiftDown, controlDown)) {
            if (button == 1) {
                RecipeViewerBridge.openUses(node);
            } else {
                switch (AmiConfig.itemClickAction) {
                    case RECIPES -> RecipeViewerBridge.openRecipes(node);
                    case USES -> RecipeViewerBridge.openUses(node);
                    case NONE -> {
                    }
                }
            }
        }
    }

    private void onGroupClicked(TreeNode node, int button) {
        if (button == 1) {
            openGroupContextMenu(node, lastClickX, lastClickY);
        }
    }

    private void openItemContextMenu(SearchNode node, int mouseX, int mouseY) {
        if (node == null) return;

        contextMenu.open(mouseX, mouseY, x, y, width, height, contextMenuActions.forItem(
                new ResultContextMenuActionBuilder.ItemContext(
                        node,
                        resolveStackForContextMenu(node),
                        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance(),
                        onTokenInject,
                        this::refreshAfterDataFixApplied,
                        this::openSourceView,
                        this::openEntityDetailsView
                )
        ));
    }

    private void openSourceView(SearchNode node) {
        if (node == null || node.type() != NodeType.ITEM) return;
        String route = ItemSourceQuery.queryFor(node);
        if (!route.equals(state.getQuery())) {
            sourceReturnQuery = state.getQuery();
            replaceQuery(route);
            return;
        }
        openSourceRoute(route);
    }

    private void openEntityDetailsView(SearchNode node) {
        if (node == null || node.type() != NodeType.ENTITY) return;
        String route = EntityDetailsQuery.queryFor(node);
        if (!route.equals(state.getQuery())) {
            entityDetailsReturnQuery = state.getQuery();
            replaceQuery(route);
            return;
        }
        openEntityDetailsRoute(route);
    }

    private boolean openSourceRoute(String query) {
        var targetText = ItemSourceQuery.parseTarget(query);
        if (targetText.isEmpty()) {
            return false;
        }
        closeEntityDetailsView();
        activeSourceReport = ItemSourceQuery.resolveTarget(query, searchService)
                .map(node -> {
                    boolean loading = AmiIndexerService.getInstance().ensureSourcesForItem(node.id());
                    ItemSourceReport report = ItemSourceResolver.fromGlobalIndex().resolve(node);
                    List<Component> diagnostics = !loading ? sourceViewDiagnostics(node.id(), report) : List.of();
                    return report.withState(loading, diagnostics);
                })
                .orElseGet(() -> new ItemSourceReport(
                        Component.literal("Sources: " + targetText.get()),
                        List.of(),
                        AmiIndexerService.getInstance().isSourceIndexingPending(),
                        List.of(Component.translatable("ami.sources.diagnostic.target_not_found", targetText.get()))
                ));
        activeSourceRevision = GlobalIndex.getInstance().revision();
        sourceView.setReport(activeSourceReport);
        return true;
    }

    private boolean openEntityDetailsRoute(String query) {
        var targetText = EntityDetailsQuery.parseTarget(query);
        if (targetText.isEmpty()) {
            return false;
        }
        closeSourceView();
        activeEntityDetailsReport = EntityDetailsQuery.resolveTarget(query, searchService)
                .map(node -> {
                    boolean loading = AmiIndexerService.getInstance().ensureSourcesForItem(node.id());
                    EntityDetailsReport report = EntityDetailsResolver.fromGlobalIndex().resolve(node);
                    List<Component> diagnostics = !loading ? entityDetailsDiagnostics(report) : List.of();
                    return report.withState(loading, diagnostics);
                })
                .orElseGet(() -> new EntityDetailsReport(
                        Component.literal("Mob: " + targetText.get()),
                        List.of(),
                        AmiIndexerService.getInstance().isSourceIndexingPending(),
                        List.of(Component.translatable("ami.entity_details.diagnostic.target_not_found", targetText.get()))
                ));
        activeEntityDetailsRevision = GlobalIndex.getInstance().revision();
        entityDetailsView.setReport(activeEntityDetailsReport);
        return true;
    }

    private List<Component> entityDetailsDiagnostics(EntityDetailsReport report) {
        List<Component> diagnostics = new ArrayList<>();
        if (report == null || report.groupOrder().isEmpty()) {
            diagnostics.add(Component.translatable("ami.entity_details.diagnostic.no_rows"));
        }
        boolean missingSpawnRows = report != null && report.rows(com.sanhiruzu.ami.client.entitydetails.EntityDetailsSection.SPAWNS).isEmpty();
        boolean missingDropRows = report != null && report.rows(com.sanhiruzu.ami.client.entitydetails.EntityDetailsSection.DROPS).isEmpty();
        if (missingSpawnRows) {
            diagnostics.add(Component.translatable(AmiConfig.sourceIndexSpawnBiomes
                    ? "ami.entity_details.diagnostic.spawn.no_biomes"
                    : "ami.sources.diagnostic.spawn.disabled"));
        }
        if (missingDropRows) {
            diagnostics.add(Component.translatable(AmiConfig.sourceIndexLootDrops
                    ? "ami.entity_details.diagnostic.loot.no_drops"
                    : "ami.sources.diagnostic.loot.disabled"));
        }
        return diagnostics;
    }

    private List<Component> sourceViewDiagnostics(ResourceLocation itemId, ItemSourceReport report) {
        if (report == null) {
            return AmiIndexerService.getInstance().sourceDiagnostics(itemId);
        }
        List<Component> diagnostics = new ArrayList<>();
        if (report.groupOrder().isEmpty()) {
            diagnostics.addAll(AmiIndexerService.getInstance().sourceDiagnostics(itemId));
        }
        if (mobDropRowsMissingBiomeLinks(report)) {
            diagnostics.add(Component.translatable(AmiConfig.sourceIndexSpawnBiomes
                    ? "ami.sources.diagnostic.spawn.no_biomes"
                    : "ami.sources.diagnostic.spawn.disabled"));
        }
        return diagnostics;
    }

    private static boolean mobDropRowsMissingBiomeLinks(ItemSourceReport report) {
        boolean hasMobDropRows = !report.rows(com.sanhiruzu.ami.client.sources.ItemSourceType.MOB_DROP).isEmpty();
        if (!hasMobDropRows) {
            return false;
        }
        for (var row : report.rows(com.sanhiruzu.ami.client.sources.ItemSourceType.MOB_DROP)) {
            if (!row.biomeLinks().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void closeSourceView() {
        activeSourceReport = null;
        activeSourceRevision = Long.MIN_VALUE;
        if (sourceView != null) {
            sourceView.setReport(null);
        }
    }

    private void closeEntityDetailsView() {
        activeEntityDetailsReport = null;
        activeEntityDetailsRevision = Long.MIN_VALUE;
        if (entityDetailsView != null) {
            entityDetailsView.setReport(null);
        }
    }

    private void exitSourceView() {
        String returnQuery = sourceReturnQuery == null ? "" : sourceReturnQuery;
        sourceReturnQuery = null;
        closeSourceView();
        replaceQuery(returnQuery);
    }

    private void exitEntityDetailsView() {
        String returnQuery = entityDetailsReturnQuery == null ? "" : entityDetailsReturnQuery;
        entityDetailsReturnQuery = null;
        closeEntityDetailsView();
        replaceQuery(returnQuery);
    }

    private void replaceQuery(String query) {
        if (onQueryReplace != null) {
            onQueryReplace.accept(query == null ? "" : query);
        } else {
            state.setQuery(query == null ? "" : query);
        }
    }

    private void openSourceLink(SearchNode node) {
        if (node == null) return;
        sourceReturnQuery = null;
        closeSourceView();
        if (onQueryReplace != null) {
            onQueryReplace.accept(node.id().toString());
        } else if (onTokenInject != null) {
            onTokenInject.accept(node.id().toString());
        }
    }

    private boolean handleSourceAction(SearchNode node, ItemSourceListView.SourceAction action, int mouseX, int mouseY) {
        if (node == null || action == null) {
            return false;
        }
        return switch (action) {
            case OPEN_LINK -> {
                openSourceLink(node);
                yield true;
            }
            case OPEN_OUTPUT_RECIPES -> {
                RecipeViewerBridge.openRecipes(node);
                yield true;
            }
            case OPEN_OUTPUT_CONTEXT -> {
                openItemContextMenu(node, mouseX, mouseY);
                yield true;
            }
            case LOCATE_BIOME -> locateSourceBiome(node);
            case OPEN_BIOME_CONTEXT -> {
                openItemContextMenu(node, mouseX, mouseY);
                yield true;
            }
            case OPEN_ENTITY_INFO -> RecipeViewerBridge.openEntityInfo(node);
        };
    }

    private boolean locateSourceBiome(SearchNode node) {
        if (node == null || node.type() != NodeType.BIOME) {
            return false;
        }
        if (AMICheatMode.isAllowed()) {
            AMICheatMode.locateBiome(node.id());
        }
        return true;
    }

    private boolean handleEntityDetailsAction(SearchNode node, EntityDetailsListView.EntityAction action, int mouseX, int mouseY) {
        if (node == null || action == null) {
            return false;
        }
        return switch (action) {
            case LOCATE_BIOME -> locateSourceBiome(node);
            case OPEN_CONTEXT -> {
                openItemContextMenu(node, mouseX, mouseY);
                yield true;
            }
            case OPEN_ITEM_RECIPES -> {
                RecipeViewerBridge.openRecipes(node);
                yield true;
            }
            case OPEN_EXTERNAL_INFO -> RecipeViewerBridge.openEntityInfo(node);
        };
    }

    private void openGroupContextMenu(TreeNode node, int mouseX, int mouseY) {
        if (node == null || node.isLeaf()) return;

        contextMenu.open(mouseX, mouseY, x, y, width, height, contextMenuActions.forGroup(
                new ResultContextMenuActionBuilder.GroupContext(
                        node,
                        onTokenInject,
                        gridView::invalidateCache,
                        this::refreshAfterDataFixApplied
                )
        ));
    }

    private void openAdvancementContextMenu(AdvancementResultRow row, int mouseX, int mouseY) {
        if (row == null || row.document() == null) return;

        List<ResultContextMenu.Action> actions = new ArrayList<>();
        if (row.document().canOpen()) {
            actions.add(ResultContextMenu.Action.enabled(
                    "ami:open_advancement",
                    Component.translatable("ami.context.open_advancement"),
                    'o',
                    row.document()::open
            ));
        } else {
            actions.add(ResultContextMenu.Action.disabled(
                    "ami:open_advancement",
                    Component.translatable("ami.context.open_advancement"),
                    'o'
            ));
        }
        actions.add(ResultContextMenu.Action.enabled(
                "ami:copy_advancement_title",
                Component.translatable("ami.context.copy_advancement_title"),
                't',
                () -> AmiClipboardHelper.copyToClipboard(row.title())
        ));
        actions.add(ResultContextMenu.Action.enabled(
                "ami:copy_advancement_id",
                Component.translatable("ami.context.copy_advancement_id"),
                'i',
                () -> AmiClipboardHelper.copyToClipboard(row.document().id().toString())
        ));
        if (onTokenInject != null && !row.document().sourceId().isBlank()) {
            actions.add(ResultContextMenu.Action.enabled(
                    "ami:filter_advancement_source",
                    Component.translatable("ami.context.filter_mod"),
                    'm',
                    () -> onTokenInject.accept("@" + row.document().sourceId())
            ));
        }

        contextMenu.open(mouseX, mouseY, x, y, width, height, actions);
    }

    private void refreshAfterDataFixApplied() {
        List<SearchNode> updated = new ArrayList<>(currentResults.size());
        for (SearchNode node : currentResults) {
            if (node == null) continue;
            Map<String, String> metadata = AmiDataFixes.apply(node.id(), node.type(), node.metadata());
            updated.add(metadata.equals(node.metadata()) ? node : node.withMetadata(metadata));
        }
        currentResults = updated;
        invalidateProjectionCache();
        forceLensRecompute();
        refreshAvailableListLenses();
        refreshTree();
        gridView.invalidateCache();
    }

    private void openGuideContextMenu(GuideResultRow row, int mouseX, int mouseY) {
        if (row == null || row.document() == null) return;

        List<ResultContextMenu.Action> actions = new ArrayList<>();
        if (row.document().canOpen()) {
            actions.add(ResultContextMenu.Action.enabled(
                    "ami:open_guide",
                    Component.translatable("ami.context.open_guide_page"),
                    'o',
                    row.document()::open
            ));
        } else {
            actions.add(ResultContextMenu.Action.disabled(
                    "ami:open_guide",
                    Component.translatable("ami.context.open_guide_page"),
                    'o'
            ));
        }
        actions.add(ResultContextMenu.Action.enabled(
                "ami:copy_guide_title",
                Component.translatable("ami.context.copy_guide_title"),
                't',
                () -> AmiClipboardHelper.copyToClipboard(row.title())
        ));
        actions.add(ResultContextMenu.Action.enabled(
                "ami:copy_guide_id",
                Component.translatable("ami.context.copy_guide_id"),
                'i',
                () -> AmiClipboardHelper.copyToClipboard(row.document().id().toString())
        ));
        if (onTokenInject != null) {
            actions.add(ResultContextMenu.Action.enabled(
                    "ami:filter_guide_book",
                    Component.translatable("ami.context.filter_guide_book"),
                    'b',
                    () -> onTokenInject.accept(AmiGuideSearchIndex.GUIDEBOOKS_FILTER_QUERY)
            ));
        }
        if (onTokenInject != null && !row.document().modId().isBlank()) {
            actions.add(ResultContextMenu.Action.enabled(
                    "ami:filter_guide_mod",
                    Component.translatable("ami.context.filter_mod"),
                    'm',
                    () -> onTokenInject.accept("@" + row.document().modId())
            ));
        }

        contextMenu.open(mouseX, mouseY, x, y, width, height, actions);
    }

    private void openQuestContextMenu(QuestResultRow row, int mouseX, int mouseY) {
        if (row == null || row.document() == null) return;

        List<ResultContextMenu.Action> actions = new ArrayList<>();
        if (row.document().canOpen()) {
            actions.add(ResultContextMenu.Action.enabled(
                    "ami:open_quest",
                    Component.translatable("ami.context.open_quest"),
                    'o',
                    row.document()::open
            ));
        } else {
            actions.add(ResultContextMenu.Action.disabled(
                    "ami:open_quest",
                    Component.translatable("ami.context.open_quest"),
                    'o'
            ));
        }
        actions.add(ResultContextMenu.Action.enabled(
                "ami:copy_quest_title",
                Component.translatable("ami.context.copy_quest_title"),
                't',
                () -> AmiClipboardHelper.copyToClipboard(row.title())
        ));
        actions.add(ResultContextMenu.Action.enabled(
                "ami:copy_quest_id",
                Component.translatable("ami.context.copy_quest_id"),
                'i',
                () -> AmiClipboardHelper.copyToClipboard(row.document().id())
        ));
        if (onTokenInject != null && !row.document().sourceId().isBlank()) {
            actions.add(ResultContextMenu.Action.enabled(
                    "ami:filter_quest_source",
                    Component.translatable("ami.context.filter_mod"),
                    'm',
                    () -> onTokenInject.accept("@" + row.document().sourceId())
            ));
        }

        contextMenu.open(mouseX, mouseY, x, y, width, height, actions);
    }

    private ItemStack resolveStackForContextMenu(SearchNode node) {
        if (node == null || node.id() == null) return ItemStack.EMPTY;

        try {
            return com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node);
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean shouldOpenLookup(SearchNode node, ItemStack stack, int button, boolean shiftDown, boolean controlDown) {
        if (node != null && node.type() != NodeType.ITEM) {
            if ((shiftDown || controlDown) && button == 0) {
                return RecipeViewerBridge.hasRecipes(node);
            }
            if (button == 1) {
                return RecipeViewerBridge.hasUses(node);
            }
            return switch (AmiConfig.itemClickAction) {
                case RECIPES -> RecipeViewerBridge.hasRecipes(node);
                case USES -> RecipeViewerBridge.hasUses(node);
                case NONE -> false;
            };
        }

        if (node == null || node.type() != NodeType.ENTITY || !Services.PLATFORM.isRecipeIndexBuilt()) {
            return true;
        }

        if ((shiftDown || controlDown) && button == 0) {
            return Services.PLATFORM.hasRecipesFor(stack);
        }

        if (button == 1) {
            return Services.PLATFORM.hasUsesFor(stack);
        }

        return switch (AmiConfig.itemClickAction) {
            case RECIPES -> Services.PLATFORM.hasRecipesFor(stack);
            case USES -> Services.PLATFORM.hasUsesFor(stack);
            case NONE -> false;
        };
    }

    private boolean isGridActive() {
        if (isFavoritesPanel) return state.getViewMode() == ResultsToolbar.ViewMode.GRID;
        return isCompactLayout() || state.getViewMode() == ResultsToolbar.ViewMode.GRID;
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    private boolean isOverToggle(double mouseX, double mouseY) {
        return mouseX >= toggleX && mouseX < toggleX + TOGGLE_W
                && mouseY >= toggleY && mouseY < toggleY + TOGGLE_H;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastClickX = (int) mouseX;
        lastClickY = (int) mouseY;

        if (contextMenu.isOpen()) {
            if (contextMenu.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            contextMenu.close();
            return true;
        }

        if (AMICheatMode.isEnabled()) {
            // Drop-to-delete: left-clicking the panel while holding an item on the cursor deletes it.
            if (button == 0 && AmiConfig.cheatDropToDelete && AMICheatMode.hasCarriedItem()) {
                AMICheatMode.deleteCarried();
                return true;
            }

            SearchNode hovered = isGridActive() ? gridView.getHoveredNode() : treeView.getHoveredNode();
            if (hovered != null) {
                InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(button);
                if (hovered.type() == NodeType.ITEM) {
                    ItemStack stack = com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(hovered);
                    if (stack.isEmpty()) {
                        stack = new ItemStack(BuiltInRegistries.ITEM.get(hovered.id()));
                    }
                    if (AmiKeybinds.activeAndMatches(Services.PLATFORM.keyMappings().cheatGiveStack(), mouseKey)) {
                        AMICheatMode.giveStack(stack);
                        return true;
                    }
                    if (AmiKeybinds.activeAndMatches(Services.PLATFORM.keyMappings().cheatGiveOne(), mouseKey)) {
                        AMICheatMode.giveItem(stack);
                        return true;
                    }
                } else if (hovered.type() == NodeType.ENTITY) {
                    boolean isPokemon = "pokemon_species".equals(
                            hovered.meta(com.sanhiruzu.ami.index.SearchNodeKeys.ENTITY_CATEGORY, ""));
                    if (isPokemon) {
                        if (AmiKeybinds.activeAndMatches(Services.PLATFORM.keyMappings().cheatGiveStack(), mouseKey)) {
                            AMICheatMode.pokemonToParty(hovered.id());
                            return true;
                        }
                        if (AmiKeybinds.activeAndMatches(Services.PLATFORM.keyMappings().cheatGiveOne(), mouseKey)) {
                            AMICheatMode.spawnPokemon(hovered.id());
                            return true;
                        }
                    } else {
                        if (AmiKeybinds.activeAndMatches(Services.PLATFORM.keyMappings().cheatGiveStack(), mouseKey)) {
                            AMICheatMode.giveEntityStackAsSpawnEgg(hovered.id());
                            return true;
                        }
                        if (AmiKeybinds.activeAndMatches(Services.PLATFORM.keyMappings().cheatGiveOne(), mouseKey)) {
                            AMICheatMode.giveEntityAsSpawnEgg(hovered.id());
                            return true;
                        }
                    }
                } else if (hovered.type() == NodeType.BIOME && button == 0) {
                    AMICheatMode.locateBiome(hovered.id());
                    return true;
                } else if (hovered.type() == NodeType.STRUCTURE && button == 0) {
                    AMICheatMode.locateStructure(hovered.id());
                    return true;
                }
            }
        }

        if (button != 0 && button != 1) return false;

        if (activeSourceReport != null) {
            if (button == 0 && isOverToggle(mouseX, mouseY)) {
                exitSourceView();
                return true;
            }
            return sourceView.mouseClicked(mouseX, mouseY, button);
        }
        if (activeEntityDetailsReport != null) {
            if (button == 0 && isOverToggle(mouseX, mouseY)) {
                exitEntityDetailsView();
                return true;
            }
            return entityDetailsView.mouseClicked(mouseX, mouseY, button);
        }

        pressedNode = null;
        if (button == 0) {
            if (isGridActive()) {
                pressedNode = gridView.getHoveredNode();
            } else {
                pressedNode = treeView.getHoveredNode();
            }
            if (pressedNode != null) {
                pressedX = mouseX;
                pressedY = mouseY;
            }
        }

        // Sidebar panel clicks and optional content swap.
        if (isFavoritesPanel) {
            if (hasCollapseButton() && button == 0 && isOverCollapseBtn(mouseX, mouseY)) {
                onCollapseSidebarCallback.run();
                return true;
            }
            if (hasSidebarAlternate() && button == 0 && isOverSidebarSwap(mouseX, mouseY)) {
                externalModeToggleCallback.run();
                updateLayout(x, y, width, height);
                return true;
            }

            if (isGridActive()) return gridView.mouseClicked(mouseX, mouseY, button);
            return treeView.mouseClicked(mouseX, mouseY, button);
        }

        // View switch — toggles between list and grid view
        if (button == 0 && isOverToggle(mouseX, mouseY)) {
            if (isForcedCompactActive()) {
                compactAutoBypass = true;
                compactAutoBypassW = width;
                compactAutoBypassH = height;
                updateLayout(x, y, width, height);
                refreshTree();
            } else if (externalModeToggleCallback != null) {
                externalModeToggleCallback.run();
            } else {
                ResultsToolbar.ViewMode next = state.getViewMode() == ResultsToolbar.ViewMode.GRID
                        ? ResultsToolbar.ViewMode.LIST
                        : ResultsToolbar.ViewMode.GRID;
                state.setViewMode(next);
                saveMainPanelViewPreference();
            }
            return true;
        }

        boolean compact = isCompactLayout();
        if (compact) {
            if (button == 0 && isOverCompactSort(mouseX, mouseY)) {
                state.setAscending(!state.isAscending());
                return true;
            }
            if (button == 0 && isOverCompactCollapse(mouseX, mouseY)) {
                if (compactCollapseAllNext) {
                    gridView.collapseAll();
                } else {
                    gridView.expandAll();
                }
                compactCollapseAllNext = !compactCollapseAllNext;
                return true;
            }
            boolean handled = gridView.mouseClicked(mouseX, mouseY, button);
            if (handled && currentQuery.isEmpty()) {
                tryLazyLoad(gridView.getHoveredTreeNode());
            }
            return handled;
        }

        // Full mode
        if (toolbar.isAnyDropdownOpen()) {
            boolean handled = toolbar.mouseClicked(mouseX, mouseY, button);
            if (!handled) {
                toolbar.closeAllDropdowns();
            }
            return true;
        }

        if (button == 0 && toolbar.mouseClicked(mouseX, mouseY, button)) {
            saveMainPanelViewPreference();
            return true;
        }

        AdvancementResultRow advancementRow = advancementRowAt(mouseX, mouseY);
        if (advancementRow != null) {
            if (button == 1) {
                openAdvancementContextMenu(advancementRow, (int) mouseX, (int) mouseY);
                return true;
            }
            if (button == 0 && advancementRow.document().canOpen()) {
                try {
                    advancementRow.document().open();
                } catch (RuntimeException ignored) {
                }
                return true;
            }
        }

        QuestResultRow questRow = questRowAt(mouseX, mouseY);
        if (questRow != null) {
            if (button == 1) {
                openQuestContextMenu(questRow, (int) mouseX, (int) mouseY);
                return true;
            }
            if (button == 0 && questRow.document().canOpen()) {
                try {
                    questRow.document().open();
                } catch (RuntimeException ignored) {
                }
                return true;
            }
        }

        GuideResultRow guideRow = guideRowAt(mouseX, mouseY);
        if (guideRow != null) {
            if (button == 1) {
                openGuideContextMenu(guideRow, (int) mouseX, (int) mouseY);
                return true;
            }
            if (button == 0) {
                tryOpenGuide(guideRow.document());
                return true;
            }
        }

        RegistryDocumentRow registryDocumentRow = registryDocumentRowAt(mouseX, mouseY);
        if (registryDocumentRow != null) {
            if (button == 0 && registryDocumentRow.document().kind() == RegistryDocumentKind.TAG) {
                replaceQuery("#" + registryDocumentRow.document().id());
            }
            return true;
        }

        // Handle Dashboard Atlas lazy loading
        if (currentQuery.isEmpty() && !isFavoritesPanel) {
            boolean handled = isGridActive() ? gridView.mouseClicked(mouseX, mouseY, button) : treeView.mouseClicked(mouseX, mouseY, button);
            if (handled) {
                tryLazyLoad(getHoveredTreeNode());
                return true;
            }
        }

        if (isGridActive()) {
            return gridView.mouseClicked(mouseX, mouseY, button);
        }
        return treeView.mouseClicked(mouseX, mouseY, button);
    }

    private void tryLazyLoad(TreeNode candidate) {
        if (candidate != null && candidate.isExpanded()) {
            populateLazyNode(candidate);
            if (isGridActive()) {
                gridView.invalidateCache();
            }
        }
    }

    private void populateLazyNode(TreeNode node) {
        if (node == null || !node.getChildren().isEmpty() || node.isLeaf()) return;

        String key = node.getKey();
        if (key == null) return;

        boolean isOntologyCategory = com.sanhiruzu.ami.index.AmiOntology.isDefinedCategoryId(key);
        String[] subcategoryKey = com.sanhiruzu.ami.client.results.DashboardBrowse.splitSubcategoryKey(key);
        boolean isOntologySubcategory = subcategoryKey != null
                && com.sanhiruzu.ami.index.AmiOntology.isDefinedCategoryId(subcategoryKey[0]);

        List<SearchNode> nodes;
        if (isOntologySubcategory) {
            List<SearchNode> categoryNodes = com.sanhiruzu.ami.index.GlobalIndex.getInstance().getNodesByCategory(subcategoryKey[0]);
            String subId = "none".equals(subcategoryKey[1]) ? "" : subcategoryKey[1];
            nodes = com.sanhiruzu.ami.client.results.DashboardBrowse.filterSubcategoryNodes(categoryNodes, subId);
        } else {
            nodes = com.sanhiruzu.ami.index.GlobalIndex.getInstance().getNodesByCategory(key);
        }

        // Fallback for NodeType keys
        if (nodes.isEmpty()) {
            try {
                NodeType type = NodeType.valueOf(key);
                nodes = com.sanhiruzu.ami.index.GlobalIndex.getInstance().getNodes(type);
            } catch (Exception ignored) {
            }
        }

        if (nodes.isEmpty()) return;

        SearchState tempState = new SearchState();
        tempState.setViewMode(state.getViewMode());
        tempState.setSortField(state.getSortField());
        tempState.setAscending(state.isAscending());

        if ((isOntologyCategory || isOntologySubcategory) && state.getGroupBy() == ResultsProcessor.GroupBy.CATEGORY) {
            node.getChildren().addAll(createLeafNodesWithCollapseGrouping(nodes, tempState));
        } else {
            tempState.setGroupBy(state.getGroupBy());
            ResultsProcessor processor = tempState.createProcessor();
            node.getChildren().addAll(applySmartGrouping(nodes, processor));
        }
        ResultsTreeNormalizer.normalizeChildren(node);
    }

    private List<TreeNode> createLeafNodesWithCollapseGrouping(List<SearchNode> nodes, SearchState tempState) {
        tempState.setGroupBy(ResultsProcessor.GroupBy.MOD);
        ResultsProcessor processor = tempState.createProcessor();
        return processor.processFlatWithCardGrouping(nodes);
    }

    private void resetSearchStateForCompact() {
        state.reset();
        state.setViewMode(ResultsToolbar.ViewMode.GRID);
    }

    private List<TreeNode> applySmartGrouping(List<SearchNode> nodes, ResultsProcessor processor) {
        return processor.process(nodes);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (contextMenu.isOpen()) {
            return true;
        }

        if (toolbar.isAnyDropdownOpen()) return true;
        if (!isCompactLayout() && !isFavoritesPanel && toolbar.mouseScrolled(mouseX, mouseY, scrollDelta)) {
            return true;
        }

        if (isGridActive()) {
            gridView.mouseScrolled(mouseX, mouseY, scrollDelta);
            return true;
        }
        if (treeView.isMouseOver(mouseX, mouseY)) {
            treeView.mouseScrolled(mouseX, mouseY, scrollDelta);
            return true;
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        double mx = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double my = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / (double) window.getScreenHeight();

        if (contextMenu.isOpen()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                contextMenu.close();
                return true;
            }
            if (contextMenu.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        }

        if (!isMouseOver(mx, my)) return false;

        if (isShiftKey(keyCode) && getHoveredNode() != null) {
            return true;
        }

        if (isGridActive()) return gridView.keyPressed(keyCode, scanCode, modifiers);
        return treeView.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (contextMenu.isOpen()) {
            return contextMenu.charTyped(codePoint, modifiers);
        }
        return false;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (contextMenu.isOpen()) return true;
        if (isGridActive()) return gridView.mouseClickedScrollbar(mouseX, mouseY, button);
        return treeView.mouseClickedScrollbar(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (contextMenu.isOpen()) return true;

        if (pressedNode != null && button == 0 && !com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging()) {
            double dx = mouseX - pressedX;
            double dy = mouseY - pressedY;
            if (dx * dx + dy * dy > DRAG_THRESHOLD * DRAG_THRESHOLD) {
                if (isFavoritesPanel
                        && com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().isFavorite(pressedNode)) {
                    draggedFavoriteNode = pressedNode;
                }
                ItemStack stack = com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(pressedNode);
                if (!stack.isEmpty() && com.sanhiruzu.ami.compat.RecipeViewerBridge.canStartDrag(stack)) {
                    com.sanhiruzu.ami.compat.RecipeViewerBridge.startDrag(stack);
                }
                pressedNode = null;
            }
        }

        if (isGridActive()) return gridView.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return treeView.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public void setFavoritesPanel(boolean favoritesPanel) {
        isFavoritesPanel = favoritesPanel;
        if (isFavoritesPanel) {
            state.setViewMode(ResultsToolbar.ViewMode.GRID); // Default sidebars to grid
        }
        initChildren(); // re-layout now that mode is known
    }

    public void setPanelTitle(Component title) {
        this.panelTitle = title;
    }

    public void setChromeOnly(boolean chromeOnly) {
        this.chromeOnly = chromeOnly;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (draggedFavoriteNode != null || com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging()) {
                boolean handled = false;

                // Check if dropped into a favorites panel using inner grid bounds
                var manager = com.sanhiruzu.ami.client.InventoryOverlayHandler.getManager();
                var favPanel = manager.getFavoritesPanelAt(mouseX, mouseY);
                if (favPanel != null && favPanel.visible) {
                    var innerPanel = favPanel.getInnerPanel();
                    if (innerPanel.isMouseOver(mouseX, mouseY)) {
                        int dropIndex = innerPanel.getDropIndex(mouseX, mouseY);
                        if (draggedFavoriteNode != null) {
                            com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance()
                                    .moveFavorite(draggedFavoriteNode, dropIndex);
                            if (com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging()) {
                                com.sanhiruzu.ami.compat.RecipeViewerBridge.stopDrag();
                            }
                            handled = true;
                        } else {
                            ItemStack stack = com.sanhiruzu.ami.compat.RecipeViewerBridge.getDraggedStack();
                            if (!stack.isEmpty()) {
                                com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().addFavoriteAt(stack, dropIndex);
                                com.sanhiruzu.ami.compat.RecipeViewerBridge.stopDrag();
                                handled = true;
                            }
                        }
                    }
                }

                if (!handled && com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging()) {
                    com.sanhiruzu.ami.compat.RecipeViewerBridge.handleDrop(mouseX, mouseY);
                }
            }
            pressedNode = null;
            draggedFavoriteNode = null;
        }
        stopScrollbarDrag();
    }

    public void stopScrollbarDrag() {
        treeView.stopScrollbarDrag();
        gridView.stopScrollbarDrag();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public void setIndexingInProgress(boolean inProgress) { /* reserved */ }

    public int getEntryCount() {
        return currentResults.size();
    }

    public String getCurrentQuery() {
        return currentQuery;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ResultsToolbar getToolbar() {
        return toolbar;
    }

    public SearchState getState() {
        return state;
    }

    public List<TreeNode> getDebugRootNodes() {
        return isGridActive() ? gridView.getRootNodes() : treeView.getRootNodes();
    }

    public String getDebugSummary() {
        return "query=\"" + currentQuery + "\""
                + " entries=" + currentResults.size()
                + " displayed=" + displayedItemCount
                + " view=" + state.getViewMode()
                + " sort=" + state.getSortField()
                + " ascending=" + state.isAscending()
                + " group=" + state.getGroupBy()
                + " gridActive=" + isGridActive()
                + " compact=" + compactMode
                + " favorites=" + isFavoritesPanel;
    }

    private void saveMainPanelViewPreference() {
        if (isFavoritesPanel) return;
        AmiViewPreferences.saveMainPanelPreference(compactMode, state.getViewMode());
    }

    public int getDropIndex(double mouseX, double mouseY) {
        if (isGridActive()) return gridView.getDropIndex(mouseX, mouseY);
        return treeView.getDropIndex(mouseX, mouseY);
    }

    public SearchNode getHoveredNode() {
        if (isGridActive()) return gridView.getHoveredNode();
        return treeView.getHoveredNode();
    }

    public boolean isContextMenuOpen() {
        return contextMenu.isOpen();
    }

    public TreeNode getHoveredTreeNode() {
        if (isGridActive()) return gridView.getHoveredTreeNode();
        return treeView.getHoveredTreeNode();
    }

    private interface IconPainter {
        void paint(int cx, int cy, int color);
    }
}
