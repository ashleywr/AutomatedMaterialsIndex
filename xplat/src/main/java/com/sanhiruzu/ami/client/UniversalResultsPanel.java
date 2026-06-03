package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.api.AmiGuideOpeners;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.client.results.*;
import com.sanhiruzu.ami.client.overlay.WidgetBounds;
import com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer;
import com.sanhiruzu.ami.compat.CompatRegistry;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.config.AmiDataFixes;
import com.sanhiruzu.ami.index.AmiIndexProgress;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

public class UniversalResultsPanel implements SearchState.Listener {

    private static final ResourceLocation PANEL_SPRITE =
            Services.PLATFORM.rl("recipe_book/overlay_recipe");

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
    private static final int SIDEBAR_RAIL_MAX_W = 64;
    private static final int SIDEBAR_RAIL_MAX_H = 44;
    private static final int SIDEBAR_SWAP_W = 18;
    private static final int SIDEBAR_SWAP_H = ResultsToolbar.BUTTON_H;
    private static final double DRAG_THRESHOLD = 5.0;
    private static final int GUIDE_HEADER_H = 12;
    private static final int GUIDE_ROW_H = 20;
    private static final int MAX_VISIBLE_GUIDE_ROWS = 3;
    private static final int MAX_VISIBLE_QUEST_ROWS = 3;
    private static final int EMBEDDED_SEARCH_H = 18;
    private static final int EMBEDDED_SEARCH_MIN_W = 76;
    private static final int EMBEDDED_SEARCH_MAX_W = 190;
    private static final int EMBEDDED_TOOLBAR_MIN_W = 170;
    private final SearchState state = new SearchState();
    private int x, y, width, height;
    // Toggle button position — recomputed on every layout update
    private int toggleX, toggleY;
    private int compactSortX, compactSortY;
    private int compactCollapseX, compactCollapseY;
    private ResultsToolbar toolbar;
    private ResultsTreeView treeView;
    private ItemGridView gridView;
    private final ResultContextMenu contextMenu = new ResultContextMenu();
    private final ResultContextMenuActionBuilder contextMenuActions = new ResultContextMenuActionBuilder();
    private List<SearchNode> currentResults = new ArrayList<>();
    private String currentQuery = "";
    private SearchService searchService;
    private ResultsViewProjector.Projection emptyQueryProjectionCache = null;
    private String emptyQueryProjectionCacheKey = "";
    private List<GuideResultRow> currentGuideRows = List.of();
    private List<QuestResultRow> currentQuestRows = List.of();
    private Runnable externalResetCallback;
    private java.util.function.Consumer<String> onTokenInject;
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
    // Displayed item count shown in the compact header (updated in refreshTree)
    private int displayedItemCount = 0;
    private SearchNode pressedNode = null;
    private double pressedX, pressedY;
    private int lastClickX, lastClickY;
    // State tracking to trigger auto-refreshes when player context changes
    private net.minecraft.world.level.GameType lastPlayerMode = null;
    private boolean lastDevMode = false;
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
        if (supportsEmbeddedSearch(new WidgetBounds(x, y, width, height))) return false;
        return !compactAutoBypass || compactAutoBypassW != width || compactAutoBypassH != height;
    }

    private boolean isForcedCompactByScreenSize() {
        return !compactMode && !isFavoritesPanel && !supportsEmbeddedSearch(new WidgetBounds(x, y, width, height));
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
            contentY = y + FAV_HEADER_H;
            contentH = height - FAV_HEADER_H - AMITheme.GLOBAL_PADDING;
        } else {
            contentY = y + AMITheme.GLOBAL_PADDING + headerH + AMITheme.ELEMENT_GAP;
            contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
        }

        this.treeView = new ResultsTreeView(innerX, contentY, innerW, contentH);
        this.gridView = new ItemGridView(innerX, contentY, innerW, contentH);
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
        this.currentResults = entries;
        invalidateProjectionCache();
        refreshAvailableListLenses();
        refreshTree();
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
        treeView.setRootNodes(normalized);
        gridView.setRootNodes(normalized);
        currentGuideRows = List.of();
        currentQuestRows = List.of();
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

    public void setSearchResults(Map<NodeType, List<SearchNode>> results, String query) {
        List<SearchNode> flat = new ArrayList<>();
        for (List<SearchNode> list : results.values()) flat.addAll(list);
        this.currentResults = flat;
        invalidateProjectionCache();
        refreshAvailableListLenses();
        String normalizedQuery = query == null ? "" : query.trim();
        if (state.getQuery().equals(normalizedQuery)) {
            refreshTree();
        } else {
            state.setQuery(normalizedQuery);
        }
    }

    public void setSearchService(SearchService service) {
        this.searchService = service;
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
        state.reset();
        if (externalResetCallback != null) externalResetCallback.run();
    }

    @Override
    public void onSearchStateChanged(SearchState state) {
        this.currentQuery = state.getQuery();
        refreshTree();
    }

    public void updateLayout(int x, int y, int width, int height) {
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
            treeView.updateLayout(innerX, contentY, innerW, contentH);
            gridView.updateLayout(innerX, contentY, innerW, contentH);
        } else if (isFavoritesPanel) {
            int contentY = y + FAV_HEADER_H;
            int contentH = height - FAV_HEADER_H - AMITheme.GLOBAL_PADDING;
            treeView.updateLayout(innerX, contentY, innerW, contentH);
            gridView.updateLayout(innerX, contentY, innerW, contentH);
        } else {
            int headerH = isCompactLayout() ? COMPACT_HEADER_H : HEADER_H;
            int contentY = y + AMITheme.GLOBAL_PADDING + headerH + AMITheme.ELEMENT_GAP;
            int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
            if (isCompactLayout()) {
                gridView.updateLayout(innerX, contentY, innerW, contentH);
            } else {
                boolean embeddedSearch = supportsEmbeddedSearch(new WidgetBounds(x, y, width, height));
                int toolbarX = embeddedSearch ? embeddedToolbarX(innerX, innerW) : narrowToolbarX(innerX);
                int toolbarW = embeddedSearch ? embeddedToolbarW(innerX, innerW) : narrowToolbarW(innerX, innerW);
                toolbar.updateLayout(toolbarX, y + AMITheme.GLOBAL_PADDING, toolbarW);
                updateResultViewLayouts(innerX, contentY, innerW, contentH);
            }
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("panel.render")) {
            checkPlayerStateChanged();
            refreshIndexProgressIfNeeded();

            if (AmiConfig.theme == AmiConfig.Theme.VANILLA) {
                g.blit(PANEL_SPRITE, x, y, 0, 0, width, height);
            } else {
                try (AmiRenderProfiler.Section chrome = AmiRenderProfiler.section("panel.chrome")) {
                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                    AMITheme.fillPanelChrome(g, x, y, width, height);

                    // Add subtle borders, only if defined by the theme.
                    if (AMITheme.BORDER_LIGHT != 0) {
                        g.fill(x + 2, y + 1, x + width - 2, y + 2, AMITheme.BORDER_LIGHT);
                        g.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, AMITheme.CONTROL_EDGE_DARK);
                    }
                    com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                }
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
                int titleRight = hasSidebarAlternate()
                        ? sidebarSwapX() - AMITheme.ELEMENT_GAP
                        : x + width - AMITheme.GLOBAL_PADDING;
                String titleText = truncate(font, title.getString(), Math.max(0, titleRight - x - AMITheme.GLOBAL_PADDING));
                g.drawString(font, titleText, x + AMITheme.GLOBAL_PADDING, y + (FAV_HEADER_H - font.lineHeight) / 2, AMITheme.TEXT_HEADER, false);

                if (hasSidebarAlternate()) {
                    renderSidebarToggle(g, mouseX, mouseY);
                }

                g.fill(x + 3, y + FAV_HEADER_H - 1, x + width - 3, y + FAV_HEADER_H, AMITheme.SECTION_SEP);

                if (displayedItemCount == 0) {
                    renderSidebarEmptyState(g);
                    return;
                }

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
                renderQuestRows(g, mouseX, mouseY);
                renderGuideRows(g, mouseX, mouseY);
                if (isGridActive()) {
                    gridView.render(g, mouseX, mouseY, dropdownOpen);
                } else {
                    treeView.render(g, mouseX, mouseY, dropdownOpen, null, state);
                }
            }

            if (!toolbar.isAnyDropdownOpen() && !contextMenu.isOpen()) {
                renderToggleTooltip(g, mouseX, mouseY);
                renderQuestTooltip(g, mouseX, mouseY);
                renderGuideTooltip(g, mouseX, mouseY);
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

    private void renderCompactIndexingProgress(GuiGraphics g, int contentY) {
        var font = Minecraft.getInstance().font;
        AmiIndexProgress progress = AmiIndexerService.getInstance().progress();
        String text = progress.message();
        if (text == null || text.isBlank() || "Ready".equals(text)) {
            text = Component.translatable("ami.gui.background_indexing").getString();
        }
        int maxW = Math.max(0, width - (AMITheme.GLOBAL_PADDING * 2));
        g.drawString(font, truncate(font, text, maxW), x + AMITheme.GLOBAL_PADDING, contentY, AMITheme.TEXT_SUBTLE, false);
    }

    private void renderIndexingProgress(GuiGraphics g, int contentY) {
        var font = Minecraft.getInstance().font;
        AmiIndexProgress progress = AmiIndexerService.getInstance().progress();
        String text = progress.message();
        if (text == null || text.isBlank() || "Ready".equals(text)) {
            text = Component.translatable("ami.gui.background_indexing").getString();
        }

        int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
        int textMaxWidth = Math.max(32, width - (AMITheme.GLOBAL_PADDING * 4));
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text).withStyle(net.minecraft.ChatFormatting.GOLD), textMaxWidth);
        int barH = progress.percent() >= 0 ? 8 : 0;
        int blockH = lines.size() * font.lineHeight + (barH > 0 ? barH + 7 : 0);
        int drawY = contentY + Math.max(0, (contentH - blockH) / 2);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            int lineW = font.width(line);
            g.drawString(font, line, x + (width - lineW) / 2, drawY, AMITheme.WHITE, false);
            drawY += font.lineHeight;
        }

        int percent = progress.percent();
        if (percent >= 0) {
            int barW = Math.min(Math.max(72, width - AMITheme.GLOBAL_PADDING * 6), 180);
            int barX = x + (width - barW) / 2;
            int barY = drawY + 5;
            int fillW = Math.max(0, Math.min(barW, Math.round(barW * (percent / 100.0F))));
            g.fill(barX, barY, barX + barW, barY + barH, AMITheme.DROPDOWN_BG);
            g.fill(barX, barY, barX + fillW, barY + barH, AMITheme.ACCENT_BLUE);
            g.fill(barX, barY, barX + barW, barY + 1, AMITheme.SECTION_SEP);
            g.fill(barX, barY + barH - 1, barX + barW, barY + barH, AMITheme.SECTION_SEP);
            g.fill(barX, barY, barX + 1, barY + barH, AMITheme.SECTION_SEP);
            g.fill(barX + barW - 1, barY, barX + barW, barY + barH, AMITheme.SECTION_SEP);
        }
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
    }

    private void renderSidebarToggle(GuiGraphics g, int mouseX, int mouseY) {
        int tx = sidebarSwapX();
        int ty = sidebarSwapY();
        boolean hovered = isOverSidebarSwap(mouseX, mouseY);

        int bgColor = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        AMITheme.fillControlChrome(g, tx, ty, SIDEBAR_SWAP_W, SIDEBAR_SWAP_H, bgColor, false);

        int color = hovered ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER;
        AmiGuiIcons.swap(g, tx + SIDEBAR_SWAP_W / 2, ty + SIDEBAR_SWAP_H / 2, color);
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
        drawCenteredTruncated(g, font, title.getString(), centerX, titleY, width - AMITheme.GLOBAL_PADDING * 2, AMITheme.TEXT_HEADER);
        drawCenteredTruncated(g, font, hint.getString(), centerX, titleY + font.lineHeight + 2, width - AMITheme.GLOBAL_PADDING * 2, AMITheme.TEXT_SUBTLE);
    }

    private static void drawCenteredTruncated(GuiGraphics g, net.minecraft.client.gui.Font font, String text,
                                              int centerX, int y, int maxWidth, int color) {
        String clipped = truncate(font, text, maxWidth);
        g.drawString(font, clipped, centerX - font.width(clipped) / 2, y, color, false);
    }

    private boolean hasSidebarAlternate() {
        return externalModeToggleCallback != null;
    }

    private int sidebarSwapX() {
        return x + width - AMITheme.GLOBAL_PADDING - SIDEBAR_SWAP_W;
    }

    private int sidebarSwapY() {
        return y + (FAV_HEADER_H - SIDEBAR_SWAP_H) / 2;
    }

    private boolean isOverSidebarSwap(double mouseX, double mouseY) {
        int tx = sidebarSwapX();
        int ty = sidebarSwapY();
        return mouseX >= tx && mouseX < tx + SIDEBAR_SWAP_W
                && mouseY >= ty && mouseY < ty + SIDEBAR_SWAP_H;
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
        int topY = contentY() + questSectionHeight();
        int rowCount = visibleGuideRowCount();

        g.fill(innerX, topY, innerX + innerW, topY + GUIDE_HEADER_H, AMITheme.GROUP_HEADER_BG);
        g.drawString(font, Component.translatable("ami.gui.guides").getString(), innerX + 4, topY + 2, AMITheme.TEXT_HEADER, false);

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
            ResourceLocation bookId = row.document().bookId();
            ItemStack bookStack = bookId != null ? new ItemStack(BuiltInRegistries.ITEM.get(bookId)) : ItemStack.EMPTY;
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
                g.drawString(font, "?", bx + 4, by + 2, AMITheme.ACCENT_BLUE, false);
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
    }

    private void renderQuestRows(GuiGraphics g, int mouseX, int mouseY) {
        if (!shouldShowQuestRows()) return;

        var font = Minecraft.getInstance().font;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int topY = contentY();
        int rowCount = visibleQuestRowCount();

        g.fill(innerX, topY, innerX + innerW, topY + GUIDE_HEADER_H, AMITheme.GROUP_HEADER_BG);
        g.drawString(font, Component.translatable("ami.gui.quest_results").getString(), innerX + 4, topY + 2, AMITheme.TEXT_HEADER, false);

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
            g.drawString(font, "Q", iconX + 3, iconY + 2, AMITheme.ACCENT_BLUE, false);

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
    }

    private void renderGuideTooltip(GuiGraphics g, int mouseX, int mouseY) {
        GuideResultRow row = guideRowAt(mouseX, mouseY);
        if (row == null) return;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(row.title()));
        lines.add(Component.literal(row.sourceLine()));
        lines.add(Component.literal(row.provenanceLine()));
        row.evidence().stream()
                .filter(evidence -> !evidence.snippet().isBlank())
                .findFirst()
                .ifPresent(evidence -> lines.add(Component.literal(evidence.snippet())));
        AmiTooltipRenderer.render(g, Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderQuestTooltip(GuiGraphics g, int mouseX, int mouseY) {
        QuestResultRow row = questRowAt(mouseX, mouseY);
        if (row == null) return;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(row.title()));
        lines.add(Component.literal(row.sourceLine()));
        lines.add(Component.literal(row.provenanceLine()));
        row.evidence().stream()
                .filter(evidence -> !evidence.snippet().isBlank())
                .findFirst()
                .ifPresent(evidence -> lines.add(Component.literal(evidence.snippet())));
        AmiTooltipRenderer.render(g, Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY);
    }

    // ── Tree refresh ──────────────────────────────────────────────────────────

    private void checkPlayerStateChanged() {
        var mc = Minecraft.getInstance();
        var gameMode = mc.gameMode != null ? mc.gameMode.getPlayerMode() : null;
        boolean devMode = AmiConfig.devMode;

        if (gameMode != lastPlayerMode || devMode != lastDevMode) {
            lastPlayerMode = gameMode;
            lastDevMode = devMode;
            refreshTree();
        }
    }

    private void refreshTree() {
        // Skip expensive tree rebuilds if we are hidden
        var manager = com.sanhiruzu.ami.client.InventoryOverlayHandler.getManager();
        if (manager != null && !manager.isPanelVisible() && !isFavoritesPanel) return;

        ResultsViewProjector.Projection projection = projectResults();
        displayedItemCount = projection.displayedItemCount();
        currentGuideRows = projection.guideRows();
        currentQuestRows = projection.questRows();
        updateResultViewLayouts();
        setViewRoots(projection.roots());
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
                AmiIndexerService.getInstance().getGuideSearchIndex(),
                AmiQuestsApi.getQuestSearchIndex(),
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
        return GlobalIndex.getInstance().revision()
                + "|source=" + System.identityHashCode(currentResults)
                + "|size=" + currentResults.size()
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
                + "|hidden=" + AmiConfig.showHiddenModItems;
    }

    private void invalidateProjectionCache() {
        emptyQueryProjectionCache = null;
        emptyQueryProjectionCacheKey = "";
    }

    private void refreshAvailableListLenses() {
        state.setAvailableListLenses(ListLens.availableFor(resolveSource()));
    }

    private void showDashboard() {
        List<TreeNode> dashboard = new ArrayList<>();

        // 1. Recent History (Most relevant first)
        var history = com.sanhiruzu.ami.client.favorites.AmiHistoryHandler.getInstance().getLookupHistory();
        if (!history.isEmpty()) {
            TreeNode historyGroup = new TreeNode("history", Component.translatable("ami.gui.sidebar.lookup_history"));
            historyGroup.setExpanded(true);
            for (int i = 0; i < Math.min(history.size(), 12); i++) {
                ItemStack stack = history.get(i);
                var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                com.sanhiruzu.ami.index.GlobalIndex.getInstance().getNode(id).ifPresent(node -> {
                    historyGroup.addChild(new TreeNode(Component.literal(node.displayName()), node));
                });
            }
            dashboard.add(historyGroup);
        }

        // 2. Browse by Category (Ontology) - category with subcategory placeholders
        List<com.sanhiruzu.ami.index.AmiOntology.Category> categories = new ArrayList<>(com.sanhiruzu.ami.index.AmiOntology.CATEGORIES);
        if (!state.isAscending()) {
            java.util.Collections.reverse(categories);
        }

        ResultsProcessor processor = state.createProcessor();
        List<TreeNode> categoryNodes = com.sanhiruzu.ami.client.results.DashboardBrowse.buildCategoryNodes(
                categories,
                categoryId -> {
                    List<SearchNode> raw = com.sanhiruzu.ami.index.GlobalIndex.getInstance().getNodesByCategory(categoryId);
                    return processor.processFlat(raw).stream()
                            .filter(TreeNode::isLeaf)
                            .map(TreeNode::getEntry)
                            .collect(java.util.stream.Collectors.toList());
                }
        );

        // Populate expanded placeholders eagerly
        for (TreeNode catNode : categoryNodes) {
            if (catNode.isExpanded()) {
                List<SearchNode> catRaw = com.sanhiruzu.ami.index.GlobalIndex.getInstance().getNodesByCategory(catNode.getKey());
                if (catRaw.isEmpty()) continue;

                Map<String, List<SearchNode>> subMap = new java.util.HashMap<>();
                for (SearchNode sn : catRaw) {
                    String subId = sn.meta(com.sanhiruzu.ami.index.SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
                    subMap.computeIfAbsent(subId, k -> new java.util.ArrayList<>()).add(sn);
                }

                for (TreeNode subNode : catNode.getChildren()) {
                    if (subNode.isExpanded() && subNode.getChildren().isEmpty()) {
                        String[] keys = com.sanhiruzu.ami.client.results.DashboardBrowse.splitSubcategoryKey(subNode.getKey());
                        if (keys != null) {
                            String subId = "none".equals(keys[1]) ? "" : keys[1];
                            List<SearchNode> members = subMap.getOrDefault(subId, java.util.Collections.emptyList());
                            if (!members.isEmpty()) {
                                SearchState tempState = new SearchState();
                                tempState.setSortField(state.getSortField());
                                tempState.setAscending(state.isAscending());
                                subNode.getChildren().addAll(createLeafNodesWithCollapseGrouping(members, tempState));
                                ResultsTreeNormalizer.normalizeChildren(subNode);
                            }
                        }
                    }
                }
            }
        }
        dashboard.addAll(categoryNodes);

        setViewRoots(dashboard);
    }

    private void setViewRoots(List<TreeNode> roots) {
        List<TreeNode> normalized = ResultsTreeNormalizer.normalize(roots);
        treeView.setRootNodes(normalized);
        gridView.setRootNodes(normalized);
        toolbar.resetCollapseState();
    }

    private void updateResultViewLayouts() {
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        if (isFavoritesPanel) {
            int contentY = y + FAV_HEADER_H;
            int contentH = height - FAV_HEADER_H - AMITheme.GLOBAL_PADDING;
            treeView.updateLayout(innerX, contentY, innerW, contentH);
            gridView.updateLayout(innerX, contentY, innerW, contentH);
            return;
        }

        int headerH = isCompactLayout() ? COMPACT_HEADER_H : HEADER_H;
        int contentY = y + AMITheme.GLOBAL_PADDING + headerH + AMITheme.ELEMENT_GAP;
        int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
        updateResultViewLayouts(innerX, contentY, innerW, contentH);
    }

    private void updateResultViewLayouts(int innerX, int contentY, int innerW, int contentH) {
        int sourceSectionsH = sourceSectionsHeight();
        int shiftedY = contentY + sourceSectionsH;
        int shiftedH = Math.max(0, contentH - sourceSectionsH);
        treeView.updateLayout(innerX, shiftedY, innerW, shiftedH);
        gridView.updateLayout(innerX, shiftedY, innerW, shiftedH);
    }

    private int contentY() {
        int headerH = isCompactLayout() ? COMPACT_HEADER_H : HEADER_H;
        return y + AMITheme.GLOBAL_PADDING + headerH + AMITheme.ELEMENT_GAP;
    }

    private boolean shouldShowGuideRows() {
        return !isCompactLayout()
                && !isFavoritesPanel
                && !currentQuery.isBlank()
                && !currentGuideRows.isEmpty();
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

    private int visibleQuestRowCount() {
        return Math.min(MAX_VISIBLE_QUEST_ROWS, currentQuestRows.size());
    }

    private int guideSectionHeight() {
        if (!shouldShowGuideRows()) return 0;
        return GUIDE_HEADER_H + visibleGuideRowCount() * GUIDE_ROW_H + AMITheme.ELEMENT_GAP;
    }

    private int questSectionHeight() {
        if (!shouldShowQuestRows()) return 0;
        return GUIDE_HEADER_H + visibleQuestRowCount() * GUIDE_ROW_H + AMITheme.ELEMENT_GAP;
    }

    private int sourceSectionsHeight() {
        return questSectionHeight() + guideSectionHeight();
    }

    private GuideResultRow guideRowAt(double mouseX, double mouseY) {
        if (!shouldShowGuideRows()) return null;
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);
        int rowY = contentY() + questSectionHeight() + GUIDE_HEADER_H;
        if (mouseX < innerX || mouseX >= innerX + innerW) return null;
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
        int rowY = contentY() + GUIDE_HEADER_H;
        if (mouseX < innerX || mouseX >= innerX + innerW) return null;
        int row = ((int) mouseY - rowY) / GUIDE_ROW_H;
        if (row < 0 || row >= visibleQuestRowCount()) return null;
        int y0 = rowY + row * GUIDE_ROW_H;
        if (mouseY < y0 || mouseY >= y0 + GUIDE_ROW_H) return null;
        return currentQuestRows.get(row);
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

    private static String truncate(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    // ── Item click (grid + list) ──────────────────────────────────────────────

    private List<SearchNode> resolveSource() {
        return currentResults;
    }

    private void onItemClicked(SearchNode node, int button) {
        if (button == 1) {
            openItemContextMenu(node, lastClickX, lastClickY);
            return;
        }

        if (CompatRegistry.handleResultClick(node, button)) {
            return;
        }

        ItemStack stack = com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node);
        boolean shiftDown = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        boolean controlDown = net.minecraft.client.gui.screens.Screen.hasControlDown();
        if (!stack.isEmpty() && shouldOpenLookup(node, stack, button, shiftDown, controlDown))
            RecipeViewerBridge.handleItemClick(stack, button, shiftDown, controlDown);
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
                        onTokenInject
                )
        ));
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

    private void refreshAfterDataFixApplied() {
        List<SearchNode> updated = new ArrayList<>(currentResults.size());
        for (SearchNode node : currentResults) {
            if (node == null) continue;
            Map<String, String> metadata = AmiDataFixes.apply(node.id(), node.type(), node.metadata());
            updated.add(metadata.equals(node.metadata()) ? node : node.withMetadata(metadata));
        }
        currentResults = updated;
        searchService = SearchService.buildFrom(GlobalIndex.getInstance(), true);
        invalidateProjectionCache();
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
        if (node == null || node.type() != NodeType.ENTITY || !Services.PLATFORM.isRecipeIndexBuilt()) {
            return true;
        }

        if ((shiftDown || controlDown) && button == 0) {
            return !Services.PLATFORM.getRecipesFor(stack).isEmpty();
        }

        if (button == 1) {
            return !Services.PLATFORM.getUsesFor(stack).isEmpty();
        }

        return switch (AmiConfig.itemClickAction) {
            case RECIPES -> !Services.PLATFORM.getRecipesFor(stack).isEmpty();
            case USES -> !Services.PLATFORM.getUsesFor(stack).isEmpty();
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
                    if (Services.PLATFORM.keyMappings().cheatGiveStack().isActiveAndMatches(mouseKey)) {
                        AMICheatMode.giveStack(stack);
                        return true;
                    }
                    if (Services.PLATFORM.keyMappings().cheatGiveOne().isActiveAndMatches(mouseKey)) {
                        AMICheatMode.giveItem(stack);
                        return true;
                    }
                } else if (hovered.type() == NodeType.ENTITY) {
                    boolean isPokemon = "pokemon_species".equals(
                            hovered.meta(com.sanhiruzu.ami.index.SearchNodeKeys.ENTITY_CATEGORY, ""));
                    if (isPokemon) {
                        if (Services.PLATFORM.keyMappings().cheatGiveStack().isActiveAndMatches(mouseKey)) {
                            AMICheatMode.pokemonToParty(hovered.id());
                            return true;
                        }
                        if (Services.PLATFORM.keyMappings().cheatGiveOne().isActiveAndMatches(mouseKey)) {
                            AMICheatMode.spawnPokemon(hovered.id());
                            return true;
                        }
                    } else {
                        if (Services.PLATFORM.keyMappings().cheatGiveStack().isActiveAndMatches(mouseKey)) {
                            AMICheatMode.giveEntityStackAsSpawnEgg(hovered.id());
                            return true;
                        }
                        if (Services.PLATFORM.keyMappings().cheatGiveOne().isActiveAndMatches(mouseKey)) {
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

        boolean isOntologyCategory = com.sanhiruzu.ami.index.AmiOntology.CATEGORIES.stream()
                .anyMatch(c -> c.id.equals(key));
        String[] subcategoryKey = com.sanhiruzu.ami.client.results.DashboardBrowse.splitSubcategoryKey(key);
        boolean isOntologySubcategory = subcategoryKey != null
                && com.sanhiruzu.ami.index.AmiOntology.CATEGORIES.stream().anyMatch(c -> c.id.equals(subcategoryKey[0]));

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
                ItemStack stack = com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(pressedNode);
                if (!stack.isEmpty()) {
                    com.sanhiruzu.ami.compat.RecipeViewerBridge.startDrag(stack);
                    pressedNode = null;
                }
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
            if (com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging()) {
                boolean handled = false;

                // Check if dropped into a favorites panel using inner grid bounds
                var manager = com.sanhiruzu.ami.client.InventoryOverlayHandler.getManager();
                var favPanel = manager.getFavoritesPanelAt(mouseX, mouseY);
                if (favPanel != null && favPanel.visible) {
                    var innerPanel = favPanel.getInnerPanel();
                    if (innerPanel.isMouseOver(mouseX, mouseY)) {
                        int dropIndex = innerPanel.getDropIndex(mouseX, mouseY);
                        ItemStack stack = com.sanhiruzu.ami.compat.RecipeViewerBridge.getDraggedStack();
                        if (!stack.isEmpty()) {
                            com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().addFavoriteAt(stack, dropIndex);
                            com.sanhiruzu.ami.compat.RecipeViewerBridge.stopDrag();
                            handled = true;
                        }
                    }
                }

                if (!handled) {
                    com.sanhiruzu.ami.compat.RecipeViewerBridge.handleDrop(mouseX, mouseY);
                }
            }
            pressedNode = null;
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
