package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.results.*;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UniversalResultsPanel {

    // Hardcoded IDs shown in the "pinned" zero-query view
    private static final List<ResourceLocation> PINNED_IDS = List.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "chest"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "furnace"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "diamond"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "oak_log"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "torch"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "stone")
    );

    private int x, y, width, height;

    private FacetBar       facetBar;
    private ResultsToolbar toolbar;
    private ResultsTreeView treeView;
    private ItemGridView    gridView;

    private List<SearchNode> currentResults = new ArrayList<>();
    private String currentQuery = "";

    public UniversalResultsPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        initChildren();
    }

    private void initChildren() {
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);

        this.facetBar = new FacetBar(innerX, y + AMITheme.GLOBAL_PADDING, innerW);

        int toolbarY = y + AMITheme.GLOBAL_PADDING + FacetBar.HEIGHT + AMITheme.ELEMENT_GAP;
        this.toolbar = new ResultsToolbar(innerX, toolbarY, innerW);

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

    public void setSearchResults(Map<NodeType, List<SearchNode>> results, String query) {
        this.currentQuery = query == null ? "" : query.trim();
        List<SearchNode> flat = new ArrayList<>();
        for (List<SearchNode> list : results.values()) flat.addAll(list);
        this.currentResults = flat;
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

        // Panel background
        g.fill(x, y, x + width, y + height, AMITheme.PANEL_BG);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, AMITheme.PANEL_INNER);

        facetBar.render(g, mouseX, mouseY);

        toolbar.setAvailableMods(toolbar.getAllMods(currentResults));
        toolbar.render(g, mouseX, mouseY);

        boolean gridMode = toolbar.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) {
            gridView.render(g, mouseX, mouseY);
        } else {
            treeView.render(g, mouseX, mouseY, toolbar.isAnyDropdownOpen(),
                    currentQuery.isEmpty() ? Component.translatable("ami.gui.pinned_discover") : null);
        }

        toolbar.renderOpenDropdownLists(g, mouseX, mouseY);
    }

    // ── Tree refresh ──────────────────────────────────────────────────────────

    private void refreshTree() {
        List<SearchNode> source = resolveSource();

        ResultsProcessor processor = new ResultsProcessor(
                toolbar.getSortField(),
                toolbar.isAscending(),
                toolbar.getGroupBy(),
                toolbar.getSelectedMods()
        );
        treeView.setRootNodes(processor.process(source));

        ResultsProcessor gridProcessor = new ResultsProcessor(
                toolbar.getSortField(),
                toolbar.isAscending(),
                toolbar.getGroupBy(),
                toolbar.getSelectedMods()
        );
        gridView.setRootNodes(gridProcessor.process(source));
    }

    /**
     * Returns the node list to display: pinned items when query is empty,
     * or the current search results otherwise.
     */
    private List<SearchNode> resolveSource() {
        if (!currentQuery.isEmpty()) {
            return currentResults;
        }

        // Zero-query: resolve pinned IDs from GlobalIndex
        GlobalIndex gi = GlobalIndex.getInstance();
        if (!gi.isIndexReady()) {
            return currentResults; // fall back to whatever we have
        }

        List<SearchNode> pinned = new ArrayList<>();
        for (ResourceLocation id : PINNED_IDS) {
            gi.getNode(id).ifPresent(pinned::add);
        }
        return pinned.isEmpty() ? currentResults : pinned;
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
            refreshTree();
            return true;
        }

        if (toolbar.isAnyDropdownOpen()) {
            boolean handled = toolbar.mouseClicked(mouseX, mouseY, button);
            if (handled) {
                refreshTree();
            } else {
                toolbar.closeAllDropdowns();
            }
            return true;
        }

        if (button == 0 && toolbar.mouseClicked(mouseX, mouseY, button)) {
            refreshTree();
            return true;
        }

        boolean gridMode = toolbar.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) {
            return gridView.mouseClicked(mouseX, mouseY, button);
        }
        if (button != 0) return false;
        return treeView.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        boolean gridMode = toolbar.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) {
            return gridView.mouseScrolled(mouseX, mouseY, scrollDelta);
        }
        if (treeView.isMouseOver(mouseX, mouseY)) {
            return treeView.mouseScrolled(mouseX, mouseY, scrollDelta);
        }
        return false;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        boolean gridMode = toolbar.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) return gridView.mouseClickedScrollbar(mouseX, mouseY, button);
        return treeView.mouseClickedScrollbar(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean gridMode = toolbar.getViewMode() == ResultsToolbar.ViewMode.GRID;
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

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getX()       { return x; }
    public int getY()       { return y; }
    public int getWidth()   { return width; }
    public int getHeight()  { return height; }
    public ResultsToolbar getToolbar() { return toolbar; }
}
