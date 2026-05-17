package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.client.results.*;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UniversalResultsPanel implements SearchState.Listener {

    private static final ResourceLocation PANEL_SPRITE =
            ResourceLocation.withDefaultNamespace("recipe_book/overlay_recipe");

    // Compact toggle button dimensions
    private static final int TOGGLE_W = 18;
    private static final int TOGGLE_H = 18;
    // Favorites panel heart header height
    private static final int FAV_HEADER_H = 16;

    private int x, y, width, height;

    // Toggle button position — recomputed on every layout update
    private int toggleX, toggleY;

    private FacetBar       facetBar;
    private ResultsToolbar toolbar;
    private ResultsTreeView treeView;
    private ItemGridView    gridView;

    private final SearchState state = new SearchState();
    private List<SearchNode> currentResults = new ArrayList<>();
    private String currentQuery = "";
    private SearchService searchService;
    private Runnable externalResetCallback;
    private boolean isFavoritesPanel = false;

    private SearchNode pressedNode = null;
    private double pressedX, pressedY;
    private static final double DRAG_THRESHOLD = 5.0;

    // State tracking to trigger auto-refreshes when player context changes
    private net.minecraft.world.level.GameType lastPlayerMode = null;
    private boolean lastDevMode = false;

    public UniversalResultsPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        state.addListener(this);
        initChildren();
    }

    private void initChildren() {
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);

        // Toggle button always sits at top-right of the panel
        this.toggleX = x + width - AMITheme.GLOBAL_PADDING - TOGGLE_W;
        this.toggleY = y + AMITheme.GLOBAL_PADDING;

        // Facet bar leaves room for the toggle button on its right
        int facetBarW = innerW - TOGGLE_W - AMITheme.ELEMENT_GAP;
        this.facetBar = new FacetBar(innerX, y + AMITheme.GLOBAL_PADDING, facetBarW, state);

        int toolbarY = y + AMITheme.GLOBAL_PADDING + FacetBar.HEIGHT + AMITheme.ELEMENT_GAP;
        this.toolbar = new ResultsToolbar(innerX, toolbarY, innerW, state);

        int contentY, contentH;
        if (isFavoritesPanel) {
            contentY = y + FAV_HEADER_H;
            contentH = height - FAV_HEADER_H - AMITheme.GLOBAL_PADDING;
        } else if (AmiConfig.compactMode) {
            contentY = y + AMITheme.GLOBAL_PADDING;
            contentH = height - AMITheme.GLOBAL_PADDING * 2;
        } else {
            contentY = toolbarY + toolbar.getHeight() + AMITheme.ELEMENT_GAP;
            contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
        }

        this.treeView = new ResultsTreeView(innerX, contentY, innerW, contentH);
        this.gridView = new ItemGridView(innerX, contentY, innerW, contentH);
        this.gridView.setItemClickCallback(this::onItemClicked);
        this.treeView.setItemClickCallback(this::onItemClicked);

        toolbar.setCollapseExpandCallbacks(
            () -> treeView.collapseAll(),
            () -> treeView.expandAll()
        );
    }

    public void setEntries(List<SearchNode> entries) {
        this.currentResults = entries;
        refreshTree();
    }

    public void setOnModClick(java.util.function.Consumer<String> callback) {
        this.treeView.setOnModClick(callback);
    }

    public void setOnFacetInject(java.util.function.Consumer<String> callback) {
        this.facetBar.setOnTokenInject(callback);
    }

    public void setSearchResults(Map<NodeType, List<SearchNode>> results, String query) {
        List<SearchNode> flat = new ArrayList<>();
        for (List<SearchNode> list : results.values()) flat.addAll(list);
        this.currentResults = flat;
        state.setQuery(query == null ? "" : query.trim());
    }

    public void setSearchService(SearchService service) {
        this.searchService = service;
        refreshTree();
    }

    public void setOnReset(Runnable callback) {
        this.externalResetCallback = callback;
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
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);

        // Toggle button always top-right
        this.toggleX = x + width - AMITheme.GLOBAL_PADDING - TOGGLE_W;
        this.toggleY = y + AMITheme.GLOBAL_PADDING;

        if (isFavoritesPanel) {
            int contentY = y + FAV_HEADER_H;
            int contentH = height - FAV_HEADER_H - AMITheme.GLOBAL_PADDING;
            gridView.updateLayout(innerX, contentY, innerW, contentH);
        } else if (AmiConfig.compactMode) {
            // Compact: grid fills the full panel area; toggle button is overlaid in top-right corner
            int contentY = y + AMITheme.GLOBAL_PADDING;
            int contentH = height - AMITheme.GLOBAL_PADDING * 2;
            gridView.updateLayout(innerX, contentY, innerW, contentH);
        } else {
            int facetBarW = innerW - TOGGLE_W - AMITheme.ELEMENT_GAP;
            facetBar.updateLayout(innerX, y + AMITheme.GLOBAL_PADDING, facetBarW);

            int toolbarY = y + AMITheme.GLOBAL_PADDING + FacetBar.HEIGHT + AMITheme.ELEMENT_GAP;
            toolbar.updateLayout(innerX, toolbarY, innerW);

            int contentY = toolbarY + toolbar.getHeight() + AMITheme.ELEMENT_GAP;
            int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
            treeView.updateLayout(innerX, contentY, innerW, contentH);
            gridView.updateLayout(innerX, contentY, innerW, contentH);
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        AMITheme.sync();
        checkPlayerStateChanged();

        if (AmiConfig.useTransparentTheme) {
            AMITheme.fillRounded(g, x, y, width, height, AmiConfig.overlayBg);
            // Add a subtle border
            int border = 0x33FFFFFF;
            g.fill(x, y, x + width, y + 1, border);
            g.fill(x, y + height - 1, x + width, y + height, border);
            g.fill(x, y, x + 1, y + height, border);
            g.fill(x + width - 1, y, x + width, y + height, border);
        } else {
            g.blitSprite(PANEL_SPRITE, x, y, width, height);
        }

        boolean compact = AmiConfig.compactMode;

        if (isFavoritesPanel) {
            var font = Minecraft.getInstance().font;
            Component title = Component.translatable("ami.gui.favorites");
            g.drawString(font, "♥ " + title.getString(), x + AMITheme.GLOBAL_PADDING, y + (FAV_HEADER_H - font.lineHeight) / 2, AMITheme.TEXT_HEADER, false);
            g.fill(x + 3, y + FAV_HEADER_H - 1, x + width - 3, y + FAV_HEADER_H, AMITheme.SECTION_SEP);
            gridView.render(g, mouseX, mouseY, false);
            return;
        }

        if (compact) {
            if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
                g.drawString(Minecraft.getInstance().font, "...",
                        x + AMITheme.GLOBAL_PADDING, y + AMITheme.GLOBAL_PADDING, 0xFFAAAAAA, false);
            } else {
                gridView.render(g, mouseX, mouseY, false);
            }
            renderToggleBtn(g, mouseX, mouseY); // overlaid on top of grid
            return;
        }

        // Full mode
        facetBar.render(g, mouseX, mouseY);
        renderToggleBtn(g, mouseX, mouseY); // drawn after facetBar so it appears on top

        int sep1Y = y + AMITheme.GLOBAL_PADDING + FacetBar.HEIGHT;
        g.fill(x + 3, sep1Y, x + width - 3, sep1Y + 1, AMITheme.SECTION_SEP);

        toolbar.render(g, mouseX, mouseY);

        int toolbarY = y + AMITheme.GLOBAL_PADDING + FacetBar.HEIGHT + AMITheme.ELEMENT_GAP;
        int sep2Y = toolbarY + toolbar.getHeight();
        g.fill(x + 3, sep2Y, x + width - 3, sep2Y + 1, AMITheme.SECTION_SEP);

        if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
            net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.translatable("ami.gui.background_indexing")
                    .withStyle(net.minecraft.ChatFormatting.GOLD);
            int msgW = Minecraft.getInstance().font.width(msg);
            int contentY = toolbarY + toolbar.getHeight() + AMITheme.ELEMENT_GAP;
            int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
            g.drawString(Minecraft.getInstance().font, msg, x + (width - msgW) / 2, contentY + (contentH / 2) - 4, 0xFFFFFF, false);
        } else {
            boolean dropdownOpen = toolbar.isAnyDropdownOpen();
            if (isGridActive()) {
                gridView.render(g, mouseX, mouseY, dropdownOpen);
            } else {
                treeView.render(g, mouseX, mouseY, dropdownOpen, null, state);
            }
        }

        toolbar.renderOpenDropdownLists(g, mouseX, mouseY);
        facetBar.renderTooltip(g, mouseX, mouseY);
    }

    private void renderToggleBtn(GuiGraphics g, int mouseX, int mouseY) {
        boolean compact = AmiConfig.compactMode;
        boolean hovered = mouseX >= toggleX && mouseX < toggleX + TOGGLE_W
                && mouseY >= toggleY && mouseY < toggleY + TOGGLE_H;

        int bgColor = compact ? 0x55AADDFF : (hovered ? 0x33FFFFFF : 0x11FFFFFF);
        g.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H, bgColor);

        if (hovered || compact) {
            int border = compact ? 0xFFAADDFF : 0x88FFFFFF;
            g.fill(toggleX,              toggleY,              toggleX + TOGGLE_W, toggleY + 1,              border); // top
            g.fill(toggleX,              toggleY + TOGGLE_H - 1, toggleX + TOGGLE_W, toggleY + TOGGLE_H,     border); // bottom
            g.fill(toggleX,              toggleY,              toggleX + 1,         toggleY + TOGGLE_H,      border); // left
            g.fill(toggleX + TOGGLE_W - 1, toggleY,           toggleX + TOGGLE_W, toggleY + TOGGLE_H,       border); // right
        }

        int iconColor = compact ? 0xFFAADDFF : (hovered ? 0xFFFFFFA0 : 0xFFAAAAAA);
        int cx = toggleX + TOGGLE_W / 2;
        int cy = toggleY + TOGGLE_H / 2;
        if (compact) {
            AmiGuiIcons.expand(g, cx, cy, iconColor);
        } else {
            AmiGuiIcons.compact(g, cx, cy, iconColor);
        }
    }

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

    // ── Tree refresh ──────────────────────────────────────────────────────────

    private void refreshTree() {
        List<SearchNode> source = resolveSource();
        String query = state.getQuery();

        if (!query.isEmpty() && searchService != null) {
            Map<NodeType, List<SearchNode>> results = searchService.query(query);
            source = new ArrayList<>();
            for (List<SearchNode> list : results.values()) source.addAll(list);
        }

        ResultsProcessor processor = state.createProcessor();
        List<TreeNode> processed = processor.process(source);
        treeView.setRootNodes(processed);
        gridView.setRootNodes(processed);
    }

    private List<SearchNode> resolveSource() {
        return currentResults;
    }

    // ── Item click (grid + list) ──────────────────────────────────────────────

    private void onItemClicked(SearchNode node, int button) {
        ItemStack stack = com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node);
        if (!stack.isEmpty()) RecipeViewerBridge.handleItemClick(stack, button, net.minecraft.client.gui.screens.Screen.hasShiftDown());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** True when the grid view should be active — favorites panel, compact mode, or explicit grid mode. */
    private boolean isGridActive() {
        return isFavoritesPanel || AmiConfig.compactMode || state.getViewMode() == ResultsToolbar.ViewMode.GRID;
    }

    private boolean isOverToggle(double mouseX, double mouseY) {
        return mouseX >= toggleX && mouseX < toggleX + TOGGLE_W
                && mouseY >= toggleY && mouseY < toggleY + TOGGLE_H;
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

        // Favorites panel — only grid clicks, no toggle/toolbar/facetbar
        if (isFavoritesPanel) {
            return gridView.mouseClicked(mouseX, mouseY, button);
        }

        // Compact toggle — always first, regardless of mode
        if (button == 0 && isOverToggle(mouseX, mouseY)) {
            AmiConfig.compactMode = !AmiConfig.compactMode;
            updateLayout(x, y, width, height);
            return true;
        }

        boolean compact = AmiConfig.compactMode;
        if (compact) {
            return gridView.mouseClicked(mouseX, mouseY, button);
        }

        // Full mode — facet bar handles both left-click (filter) and right-click (inject token)
        if (facetBar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (toolbar.isAnyDropdownOpen()) {
            boolean handled = toolbar.mouseClicked(mouseX, mouseY, button);
            if (!handled) {
                toolbar.closeAllDropdowns();
            }
            return true;
        }

        if (button == 0 && toolbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (isGridActive()) {
            return gridView.mouseClicked(mouseX, mouseY, button);
        }
        return treeView.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (toolbar.isAnyDropdownOpen()) return true;

        // In full mode, the facet bar scrolls horizontally when the mouse is over it
        if (!AmiConfig.compactMode && facetBar.isMouseOver(mouseX, mouseY)) {
            return facetBar.mouseScrolled(mouseX, mouseY, scrollDelta);
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

        if (!isMouseOver(mx, my)) return false;

        if (!AmiConfig.compactMode && facetBar.keyPressed(keyCode, scanCode, modifiers)) return true;

        if (isGridActive()) return gridView.keyPressed(keyCode, scanCode, modifiers);
        return treeView.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (isGridActive()) return gridView.mouseClickedScrollbar(mouseX, mouseY, button);
        return treeView.mouseClickedScrollbar(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
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
        initChildren(); // re-layout now that mode is known
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging()) {
                boolean handled = false;

                // Check if dropped into a favorites panel using inner grid bounds
                var manager = com.sanhiruzu.ami.client.InventoryOverlayHandler.getManager();
                var favPanel = manager.getLeftPanel();
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
    public int getEntryCount() { return currentResults.size(); }
    public String getCurrentQuery() { return currentQuery; }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getX()       { return x; }
    public int getY()       { return y; }
    public int getWidth()   { return width; }
    public int getHeight()  { return height; }
    public ResultsToolbar getToolbar() { return toolbar; }
    public SearchState getState() { return state; }

    public int getDropIndex(double mouseX, double mouseY) {
        if (isGridActive()) return gridView.getDropIndex(mouseX, mouseY);
        return treeView.getDropIndex(mouseX, mouseY);
    }

    public SearchNode getHoveredNode() {
        if (isGridActive()) return gridView.getHoveredNode();
        return treeView.getHoveredNode();
    }
}
