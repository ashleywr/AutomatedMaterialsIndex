package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.results.*;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UniversalResultsPanel implements SearchState.Listener {

    private static final ResourceLocation PANEL_SPRITE =
            ResourceLocation.withDefaultNamespace("recipe_book/overlay_recipe");

    private int x, y, width, height;

    private FacetBar       facetBar;
    private ResultsToolbar toolbar;
    private ResultsTreeView treeView;
    private ItemGridView    gridView;

    private final SearchState state = new SearchState();
    private List<SearchNode> currentResults = new ArrayList<>();
    private String currentQuery = "";
    private SearchService searchService;
    private Runnable externalResetCallback;

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

        this.facetBar = new FacetBar(innerX, y + AMITheme.GLOBAL_PADDING, innerW, state);

        int toolbarY = y + AMITheme.GLOBAL_PADDING + FacetBar.HEIGHT + AMITheme.ELEMENT_GAP;
        this.toolbar = new ResultsToolbar(innerX, toolbarY, innerW, state);

        int contentY = toolbarY + toolbar.getHeight() + AMITheme.ELEMENT_GAP;
        int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;

        this.treeView = new ResultsTreeView(innerX, contentY, innerW, contentH);
        this.gridView = new ItemGridView(innerX, contentY, innerW, contentH);
        this.gridView.setItemClickCallback(this::onGridItemClicked);
    }

    public void setEntries(List<SearchNode> entries) {
        this.currentResults = entries;
        refreshTree();
    }

    public void setOnModClick(java.util.function.Consumer<String> callback) {
        this.treeView.setOnModClick(callback);
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

        facetBar.updateLayout(innerX, y + AMITheme.GLOBAL_PADDING, innerW);

        int toolbarY = y + AMITheme.GLOBAL_PADDING + FacetBar.HEIGHT + AMITheme.ELEMENT_GAP;
        toolbar.updateLayout(innerX, toolbarY, innerW);

        int contentY = toolbarY + toolbar.getHeight() + AMITheme.ELEMENT_GAP;
        int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
        treeView.updateLayout(innerX, contentY, innerW, contentH);
        gridView.updateLayout(innerX, contentY, innerW, contentH);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        AMITheme.sync(); // CSS-like hot-reloading of config values

        // Vanilla 9-slice panel background
        g.blitSprite(PANEL_SPRITE, x, y, width, height);

        facetBar.render(g, mouseX, mouseY);

        // Section separator: FacetBar → Toolbar
        int sep1Y = y + AMITheme.GLOBAL_PADDING + FacetBar.HEIGHT;
        g.fill(x + 3, sep1Y, x + width - 3, sep1Y + 1, AMITheme.SECTION_SEP);

        toolbar.render(g, mouseX, mouseY);

        // Section separator: Toolbar → Results
        int toolbarY = y + AMITheme.GLOBAL_PADDING + FacetBar.HEIGHT + AMITheme.ELEMENT_GAP;
        int sep2Y = toolbarY + toolbar.getHeight();
        g.fill(x + 3, sep2Y, x + width - 3, sep2Y + 1, AMITheme.SECTION_SEP);

        boolean gridMode = state.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) {
            gridView.render(g, mouseX, mouseY);
        } else {
            treeView.render(g, mouseX, mouseY, toolbar.isAnyDropdownOpen(), null, currentQuery);
        }

        toolbar.renderOpenDropdownLists(g, mouseX, mouseY);

        // FacetBar tooltip drawn last so it layers above everything else
        facetBar.renderTooltip(g, mouseX, mouseY);
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

    // ── Item click (grid) ─────────────────────────────────────────────────────

    private void onGridItemClicked(SearchNode node, int button) {
        ItemStack stack = BuiltInRegistries.ITEM.getOptional(node.id())
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        RecipeViewerBridge.handleItemClick(stack, button);
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;

        // Facet bar intercepts left-clicks on its badges
        if (button == 0 && facetBar.mouseClicked(mouseX, mouseY, button)) {
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

        boolean gridMode = state.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) {
            return gridView.mouseClicked(mouseX, mouseY, button);
        }
        if (button != 0) return false;
        return treeView.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        boolean gridMode = state.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) {
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

        if (facetBar.keyPressed(keyCode, scanCode, modifiers)) return true;

        boolean gridMode = state.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) return gridView.keyPressed(keyCode, scanCode, modifiers);
        return treeView.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        boolean gridMode = state.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) return gridView.mouseClickedScrollbar(mouseX, mouseY, button);
        return treeView.mouseClickedScrollbar(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean gridMode = state.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) return gridView.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return treeView.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
}
