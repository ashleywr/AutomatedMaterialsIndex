package com.sanhiruzu.ami.client;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.client.results.ItemGridView;
import com.sanhiruzu.ami.client.results.ResultsToolbar;
import com.sanhiruzu.ami.client.results.ResultsTreeView;
import com.sanhiruzu.ami.client.results.ResultsProcessor;

public class UniversalResultsPanel {
    private int x, y, width, height;

    private ResultsToolbar toolbar;
    private ResultsTreeView treeView;
    private ItemGridView gridView;

    private List<SearchNode> currentResults = new ArrayList<>();

    public UniversalResultsPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.toolbar = new ResultsToolbar(x + 2, y + 2, width - 4);
        int contentY = y + toolbar.getHeight() + 4;
        int contentH = height - toolbar.getHeight() - 6;
        this.treeView = new ResultsTreeView(x + 2, contentY, width - 4, contentH);
        this.gridView = new ItemGridView(x + 2, contentY, width - 4, contentH);
        this.gridView.setItemClickCallback(this::onGridItemClicked);
    }

    public void setEntries(List<SearchNode> entries) {
        this.currentResults = entries;
        refreshTree();
    }

    public void setSearchResults(Map<NodeType, List<SearchNode>> results, String query) {
        List<SearchNode> flat = new ArrayList<>();
        for (List<SearchNode> list : results.values()) {
            flat.addAll(list);
        }
        this.currentResults = flat;
        refreshTree();
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        toolbar.updateLayout(x + 2, y + 2, width - 4);
        int contentY = y + toolbar.getHeight() + 4;
        int contentH = height - toolbar.getHeight() - 6;
        treeView.updateLayout(x + 2, contentY, width - 4, contentH);
        gridView.updateLayout(x + 2, contentY, width - 4, contentH);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Panel background
        g.fill(x, y, x + width, y + height, 0xFF0A0A0A);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF1A1A1A);

        toolbar.setAvailableMods(toolbar.getAllMods(currentResults));
        toolbar.render(g, mouseX, mouseY);

        boolean gridMode = toolbar.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) {
            // Grid mode: item nodes in grid, non-item nodes hidden
            // (non-item nodes are best explored via the atlas / list mode)
            gridView.render(g, mouseX, mouseY);
        } else {
            treeView.render(g, mouseX, mouseY, toolbar.isAnyDropdownOpen());
        }

        // Dropdown lists rendered last so they appear on top of everything
        toolbar.renderOpenDropdownLists(g, mouseX, mouseY);
    }

    private void refreshTree() {
        ResultsProcessor processor = new ResultsProcessor(
                toolbar.getSortField(),
                toolbar.isAscending(),
                toolbar.getGroupBy(),
                toolbar.getSelectedMods()
        );
        var treeNodes = processor.process(currentResults);
        treeView.setRootNodes(treeNodes);

        // Grid view only shows ITEM nodes
        List<SearchNode> itemsOnly = currentResults.stream()
                .filter(n -> n.type() == NodeType.ITEM)
                .collect(Collectors.toList());
        ResultsProcessor gridProcessor = new ResultsProcessor(
                toolbar.getSortField(),
                toolbar.isAscending(),
                toolbar.getGroupBy(),
                toolbar.getSelectedMods()
        );
        gridView.setRootNodes(gridProcessor.process(itemsOnly));
    }

    private void onGridItemClicked(SearchNode node, int button) {
        ItemStack stack = BuiltInRegistries.ITEM.getOptional(node.id())
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        RecipeViewerBridge.handleItemClick(stack, button);
    }

    // =========================================================================
    // Input Handlers
    // =========================================================================

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Allow right-click through to grid even though default routing only sends button==0
        if (button != 0 && button != 1) return false;

        // If any dropdown is open and the click is outside all dropdown hit areas,
        // close them and consume the click.
        if (toolbar.isAnyDropdownOpen()) {
            boolean handledByToolbar = toolbar.mouseClicked(mouseX, mouseY, button);
            if (handledByToolbar) {
                refreshTree();
            } else {
                toolbar.closeAllDropdowns();
            }
            return true;
        }

        // Toolbar (no dropdown open) — only left-click
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
        if (gridMode) {
            return gridView.mouseClickedScrollbar(mouseX, mouseY, button);
        }
        return treeView.mouseClickedScrollbar(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean gridMode = toolbar.getViewMode() == ResultsToolbar.ViewMode.GRID;
        if (gridMode) {
            return gridView.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return treeView.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public void stopScrollbarDrag() {
        treeView.stopScrollbarDrag();
        gridView.stopScrollbarDrag();
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public void setIndexingInProgress(boolean inProgress) { /* reserved */ }
    public int getEntryCount() { return currentResults.size(); }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public ResultsToolbar getToolbar() { return toolbar; }
}
