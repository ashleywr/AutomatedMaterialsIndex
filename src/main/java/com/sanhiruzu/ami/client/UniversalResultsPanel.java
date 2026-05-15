package com.sanhiruzu.ami.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.client.results.ResultsToolbar;
import com.sanhiruzu.ami.client.results.ResultsTreeView;
import com.sanhiruzu.ami.client.results.ResultsProcessor;

public class UniversalResultsPanel {
    private int x, y, width, height;

    private ResultsToolbar toolbar;
    private ResultsTreeView treeView;

    // Search state owned here, rendered externally by InventoryOverlayHandler
    private String searchQuery = "";
    private boolean searchFocused = false;

    private List<SearchNode> currentResults = new ArrayList<>();

    public UniversalResultsPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.toolbar = new ResultsToolbar(x + 2, y + 2, width - 4);
        int treeY = y + toolbar.getHeight() + 4;
        int treeH = height - toolbar.getHeight() - 6;
        this.treeView = new ResultsTreeView(x + 2, treeY, width - 4, treeH);
    }

    public void setEntries(List<SearchNode> entries) {
        this.currentResults = entries;
        this.searchQuery = "";
        this.searchFocused = false;
        refreshTree();
    }

    public void setSearchResults(Map<NodeType, List<SearchNode>> results, String query) {
        this.searchQuery = query;
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
        int treeY = y + toolbar.getHeight() + 4;
        int treeH = height - toolbar.getHeight() - 6;
        treeView.updateLayout(x + 2, treeY, width - 4, treeH);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Panel background
        g.fill(x, y, x + width, y + height, 0xFF0A0A0A);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF1A1A1A);

        // Toolbar buttons only (no dropdown lists yet)
        toolbar.setAvailableMods(toolbar.getAllMods(currentResults));
        toolbar.render(g, mouseX, mouseY);

        // Tree view
        treeView.render(g, mouseX, mouseY, toolbar.isAnyDropdownOpen());

        // Dropdown lists rendered last so they appear on top of the tree
        toolbar.renderOpenDropdownLists(g, mouseX, mouseY);

        // Tooltips deferred by tree view
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
    }

    // =========================================================================
    // Input Handlers
    // =========================================================================

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

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

        // Toolbar (no dropdown open)
        if (toolbar.mouseClicked(mouseX, mouseY, button)) {
            refreshTree();
            return true;
        }

        // Tree view
        return treeView.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (treeView.isMouseOver(mouseX, mouseY)) {
            return treeView.mouseScrolled(mouseX, mouseY, scrollDelta);
        }
        return false;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        return treeView.mouseClickedScrollbar(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return treeView.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public void stopScrollbarDrag() {
        treeView.stopScrollbarDrag();
    }

    public void typeCharacter(char c) {
        if (c >= 32 && c < 127) {
            searchQuery += c;
        }
    }

    public void deleteSearchChar() {
        if (!searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
        }
    }

    public void clearSearch() {
        searchQuery = "";
        searchFocused = false;
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public boolean isSearchFocused() { return searchFocused; }
    public void setSearchFocused(boolean focused) { this.searchFocused = focused; }
    public String getSearchQuery() { return searchQuery; }
    public void setIndexingInProgress(boolean inProgress) { /* reserved */ }
    public int getEntryCount() { return currentResults.size(); }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
