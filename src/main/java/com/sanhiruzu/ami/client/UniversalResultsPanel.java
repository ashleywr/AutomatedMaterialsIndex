package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AMIConfig;
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

    // Compact toggle button dimensions
    private static final int TOGGLE_W = 18;
    private static final int TOGGLE_H = 18;

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

        int contentY = toolbarY + toolbar.getHeight() + AMITheme.ELEMENT_GAP;
        int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;

        this.treeView = new ResultsTreeView(innerX, contentY, innerW, contentH);
        this.gridView = new ItemGridView(innerX, contentY, innerW, contentH);
        this.gridView.setItemClickCallback(this::onItemClicked);
        this.treeView.setItemClickCallback(this::onItemClicked);
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

        if (AMIConfig.COMPACT_MODE.get()) {
            // Compact: thin header row for toggle button only, grid fills rest
            int contentY = y + AMITheme.GLOBAL_PADDING + TOGGLE_H + AMITheme.ELEMENT_GAP;
            int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
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

        g.blitSprite(PANEL_SPRITE, x, y, width, height);

        boolean compact = AMIConfig.COMPACT_MODE.get();

        if (compact) {
            renderToggleBtn(g, mouseX, mouseY);
            if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
                int loadY = toggleY + TOGGLE_H + AMITheme.ELEMENT_GAP;
                g.drawString(Minecraft.getInstance().font, "...",
                        x + AMITheme.GLOBAL_PADDING, loadY, 0xFFAAAAAA, false);
            } else {
                gridView.render(g, mouseX, mouseY);
            }
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
            if (isGridActive()) {
                gridView.render(g, mouseX, mouseY);
            } else {
                treeView.render(g, mouseX, mouseY, toolbar.isAnyDropdownOpen(), null, state);
            }
        }

        toolbar.renderOpenDropdownLists(g, mouseX, mouseY);
        facetBar.renderTooltip(g, mouseX, mouseY);
    }

    private void renderToggleBtn(GuiGraphics g, int mouseX, int mouseY) {
        boolean compact = AMIConfig.COMPACT_MODE.get();
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

        var font = Minecraft.getInstance().font;
        String label = compact ? "≡" : "⊡"; // ≡ / ⊡
        int textColor = compact ? 0xFFAADDFF : (hovered ? 0xFFFFFFA0 : 0xFFAAAAAA);
        g.drawCenteredString(font, label, toggleX + TOGGLE_W / 2,
                toggleY + (TOGGLE_H - font.lineHeight) / 2, textColor);
    }

    private void checkPlayerStateChanged() {
        var mc = Minecraft.getInstance();
        var gameMode = mc.gameMode != null ? mc.gameMode.getPlayerMode() : null;
        boolean devMode = AMIConfig.DEV_MODE.get();

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
        ItemStack stack = com.sanhiruzu.ami.client.icon.ItemIconRenderer.resolveStack(node.id());
        if (!stack.isEmpty()) RecipeViewerBridge.handleItemClick(stack, button);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** True when the grid view should be active — either explicit grid mode or compact mode. */
    private boolean isGridActive() {
        return AMIConfig.COMPACT_MODE.get() || state.getViewMode() == ResultsToolbar.ViewMode.GRID;
    }

    private boolean isOverToggle(double mouseX, double mouseY) {
        return mouseX >= toggleX && mouseX < toggleX + TOGGLE_W
                && mouseY >= toggleY && mouseY < toggleY + TOGGLE_H;
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;

        // Compact toggle — always first, regardless of mode
        if (button == 0 && isOverToggle(mouseX, mouseY)) {
            AMIConfig.COMPACT_MODE.set(!AMIConfig.COMPACT_MODE.get());
            AMIConfig.SPEC.save();
            updateLayout(x, y, width, height);
            return true;
        }

        boolean compact = AMIConfig.COMPACT_MODE.get();
        if (compact) {
            return gridView.mouseClicked(mouseX, mouseY, button);
        }

        // Full mode
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

        if (isGridActive()) {
            return gridView.mouseClicked(mouseX, mouseY, button);
        }
        if (button != 0) return false;
        return treeView.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!isMouseOver(mouseX, mouseY)) return false;

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

        if (!AMIConfig.COMPACT_MODE.get() && facetBar.keyPressed(keyCode, scanCode, modifiers)) return true;

        if (isGridActive()) return gridView.keyPressed(keyCode, scanCode, modifiers);
        return treeView.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (isGridActive()) return gridView.mouseClickedScrollbar(mouseX, mouseY, button);
        return treeView.mouseClickedScrollbar(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isGridActive()) return gridView.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
