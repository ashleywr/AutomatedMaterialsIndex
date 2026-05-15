package com.sanhiruzu.ami.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.client.results.ResultsToolbar;
import com.sanhiruzu.ami.client.results.ResultsTreeView;
import com.sanhiruzu.ami.client.results.ResultsProcessor;

public class UniversalResultsPanel {
    private static final int HEADER_HEIGHT = 14;
    private static final int SEARCH_BAR_HEIGHT = 12;
    private static final int PADDING = 4;

    private int x, y, width, height;
    private Component modeLabel = Component.literal("Atlas");
    private NodeType currentAtlasType = null;

    private ResultsToolbar toolbar;
    private ResultsTreeView treeView;

    // Search state
    private String searchQuery = "";
    private boolean searchFocused = false;

    // Indexing state
    private boolean indexingInProgress = false;

    // Current data for processing
    private List<SearchNode> currentResults = new ArrayList<>();

    public UniversalResultsPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.toolbar = new ResultsToolbar(x + 2, y + HEADER_HEIGHT + 2, width - 4);
        int treeY = y + HEADER_HEIGHT + toolbar.getHeight() + 4;
        int treeH = height - HEADER_HEIGHT - toolbar.getHeight() - SEARCH_BAR_HEIGHT - 12;
        this.treeView = new ResultsTreeView(x + 2, treeY, width - 4, treeH);
    }

    public void setAtlasEntries(List<SearchNode> entries, Component label, NodeType type) {
        this.currentResults = entries;
        this.currentAtlasType = type;
        this.modeLabel = label;
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
        this.currentAtlasType = null;
        this.modeLabel = Component.literal("Search Results");
        refreshTree();
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        toolbar.updateLayout(x + 2, y + HEADER_HEIGHT + 2, width - 4);
        int treeY = y + HEADER_HEIGHT + toolbar.getHeight() + 4;
        int treeH = height - HEADER_HEIGHT - toolbar.getHeight() - SEARCH_BAR_HEIGHT - 12;
        treeView.updateLayout(x + 2, treeY, width - 4, treeH);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Panel background
        g.fill(x, y, x + width, y + height, 0xFF0A0A0A);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF1A1A1A);

        // Header bar
        g.fill(x, y, x + width, y + HEADER_HEIGHT + 2, 0xFF2A2A2A);
        g.fill(x, y + HEADER_HEIGHT + 2, x + width, y + HEADER_HEIGHT + 3, 0xFF444444);

        var font = Minecraft.getInstance().font;

        // Center text: mode label + count
        MutableComponent centerText = modeLabel.copy()
                .append(Component.literal(" (" + currentResults.size() + ")"));

        int textWidth = font.width(centerText);
        int centerX = x + (width - textWidth) / 2;
        g.drawString(font, centerText, centerX, y + 2, 0xFFCCCCCC, false);

        // Render toolbar
        toolbar.render(g, mouseX, mouseY);

        // Update toolbar's available mods
        Set<String> allMods = toolbar.getAllMods(currentResults);
        toolbar.setAvailableMods(allMods);

        // Render tree view (pass dropdown state to prevent mouseover interaction)
        treeView.render(g, mouseX, mouseY, toolbar.isAnyDropdownOpen());

        // Search bar
        renderSearchBar(g, mouseX, mouseY);
    }

    private void renderSearchBar(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int searchBarY = y + height - SEARCH_BAR_HEIGHT - 3;
        int searchBarX = x + 2;
        int searchBarW = width - 4;

        // Background
        g.fill(searchBarX, searchBarY, searchBarX + searchBarW, searchBarY + SEARCH_BAR_HEIGHT,
                searchFocused ? 0xFF3A3A3A : 0xFF2A2A2A);
        g.fill(searchBarX + 1, searchBarY + 1, searchBarX + searchBarW - 1, searchBarY + SEARCH_BAR_HEIGHT - 1,
                0xFF1A1A1A);

        // Border
        int borderColor = searchFocused ? 0xFFAAAA44 : 0xFF555555;
        g.fill(searchBarX, searchBarY, searchBarX + searchBarW, searchBarY + 1, borderColor);
        g.fill(searchBarX, searchBarY + SEARCH_BAR_HEIGHT - 1, searchBarX + searchBarW, searchBarY + SEARCH_BAR_HEIGHT, borderColor);
        g.fill(searchBarX, searchBarY, searchBarX + 1, searchBarY + SEARCH_BAR_HEIGHT, borderColor);
        g.fill(searchBarX + searchBarW - 1, searchBarY, searchBarX + searchBarW, searchBarY + SEARCH_BAR_HEIGHT, borderColor);

        // Text
        int textX = searchBarX + 3;
        String displayText = searchQuery;
        if (searchQuery.isEmpty() && !searchFocused) {
            displayText = "Filter...";
            g.drawString(font, displayText, textX, searchBarY + 2, 0xFF666666, false);
        } else if (!searchQuery.isEmpty()) {
            g.drawString(font, displayText, textX, searchBarY + 2, 0xFFCCCCCC, false);
        }

        // Cursor blink
        if (searchFocused && (System.currentTimeMillis() % 1000) < 500) {
            int cursorX = textX + font.width(displayText) + 1;
            g.fill(cursorX, searchBarY + 2, cursorX + 1, searchBarY + SEARCH_BAR_HEIGHT - 2, 0xFFCCCCCC);
        }
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

        // Toolbar first
        if (toolbar.mouseClicked(mouseX, mouseY, button)) {
            refreshTree();
            return true;
        }

        // Search bar
        if (isSearchBarHovered(mouseX, mouseY)) {
            setSearchFocused(true);
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
    public void setIndexingInProgress(boolean inProgress) { this.indexingInProgress = inProgress; }
    public int getEntryCount() { return currentResults.size(); }
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isSearchBarHovered(double mouseX, double mouseY) {
        int searchBarY = y + height - SEARCH_BAR_HEIGHT - 3;
        int searchBarH = SEARCH_BAR_HEIGHT + 1;
        return mouseX >= x + 1 && mouseX < x + width - 1
                && mouseY >= searchBarY && mouseY < searchBarY + searchBarH;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
