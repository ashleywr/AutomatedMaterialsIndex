package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.client.results.*;
import com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import com.sanhiruzu.ami.platform.Services;
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

public class UniversalResultsPanel implements SearchState.Listener {

    private static final ResourceLocation PANEL_SPRITE =
            Services.PLATFORM.rl("recipe_book/overlay_recipe");

    // Fixed height of the top header area — full mode uses toolbar height, compact uses minimal space
    private static final int HEADER_H = ResultsToolbar.TOOLBAR_HEIGHT;
    private static final int COMPACT_HEADER_H = 20; // Minimal height for item count + toggle button
    // Compact toggle button dimensions — height matches toolbar buttons for visual consistency
    private static final int TOGGLE_W = 22;
    private static final int TOGGLE_H = ResultsToolbar.BUTTON_H;
    // Favorites panel heart header height
    private static final int FAV_HEADER_H = 16;
    private static final int SIDEBAR_SWAP_W = 18;
    private static final int SIDEBAR_SWAP_H = ResultsToolbar.BUTTON_H;
    private static final double DRAG_THRESHOLD = 5.0;
    private final SearchState state = new SearchState();
    private int x, y, width, height;
    // Toggle button position — recomputed on every layout update
    private int toggleX, toggleY;
    private ResultsToolbar toolbar;
    private ResultsTreeView treeView;
    private ItemGridView gridView;
    private List<SearchNode> currentResults = new ArrayList<>();
    private String currentQuery = "";
    private SearchService searchService;
    private Runnable externalResetCallback;
    private java.util.function.Consumer<String> onTokenInject;
    private Runnable externalModeToggleCallback;
    private java.util.function.BooleanSupplier externalModeToggleActive;
    private boolean isFavoritesPanel = false;
    private boolean compactMode = false;
    private boolean chromeOnly = false;
    private boolean tooltipLeftOfCursor = false;
    private Component panelTitle = null;
    // Displayed item count shown in the compact header (updated in refreshTree)
    private int displayedItemCount = 0;
    private SearchNode pressedNode = null;
    private double pressedX, pressedY;
    // State tracking to trigger auto-refreshes when player context changes
    private net.minecraft.world.level.GameType lastPlayerMode = null;
    private boolean lastDevMode = false;

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

    private void initChildren() {
        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);

        // View switch sits at the left edge of the header, before sort/group controls.
        this.toggleX = innerX;
        this.toggleY = y + AMITheme.GLOBAL_PADDING + (HEADER_H - TOGGLE_H) / 2;

        int toolbarW = innerW - TOGGLE_W - AMITheme.ELEMENT_GAP;
        int toolbarY = y + AMITheme.GLOBAL_PADDING;
        this.toolbar = new ResultsToolbar(innerX + TOGGLE_W + AMITheme.ELEMENT_GAP, toolbarY, toolbarW, state);

        int contentY, contentH;
        if (isFavoritesPanel) {
            contentY = y + FAV_HEADER_H;
            contentH = height - FAV_HEADER_H - AMITheme.GLOBAL_PADDING;
        } else {
            contentY = y + AMITheme.GLOBAL_PADDING + HEADER_H + AMITheme.ELEMENT_GAP;
            contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
        }

        this.treeView = new ResultsTreeView(innerX, contentY, innerW, contentH);
        this.gridView = new ItemGridView(innerX, contentY, innerW, contentH);
        this.treeView.setTooltipLeftOfCursor(tooltipLeftOfCursor);
        this.gridView.setTooltipLeftOfCursor(tooltipLeftOfCursor);
        this.gridView.setItemClickCallback(this::onItemClicked);
        this.treeView.setItemClickCallback(this::onItemClicked);
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
        refreshTree();
    }

    /**
     * Directly set pre-built TreeNode roots, bypassing refreshTree().
     * Used by sidebar panels that need grouped display (e.g. quests).
     */
    public void setGroupedEntries(List<TreeNode> roots) {
        List<TreeNode> normalized = ResultsTreeNormalizer.normalize(roots);
        treeView.setRootNodes(normalized);
        gridView.setRootNodes(normalized);
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

    public void setTooltipLeftOfCursor(boolean tooltipLeftOfCursor) {
        this.tooltipLeftOfCursor = tooltipLeftOfCursor;
        if (treeView != null) treeView.setTooltipLeftOfCursor(tooltipLeftOfCursor);
        if (gridView != null) gridView.setTooltipLeftOfCursor(tooltipLeftOfCursor);
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
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int innerX = x + AMITheme.GLOBAL_PADDING;
        int innerW = width - (AMITheme.GLOBAL_PADDING * 2);

        // View switch stays at the left edge of the header, before sort/group controls.
        this.toggleX = innerX;
        this.toggleY = y + AMITheme.GLOBAL_PADDING;

        if (isFavoritesPanel) {
            int contentY = y + FAV_HEADER_H;
            int contentH = height - FAV_HEADER_H - AMITheme.GLOBAL_PADDING;
            treeView.updateLayout(innerX, contentY, innerW, contentH);
            gridView.updateLayout(innerX, contentY, innerW, contentH);
        } else {
            int headerH = compactMode ? COMPACT_HEADER_H : HEADER_H;
            int contentY = y + AMITheme.GLOBAL_PADDING + headerH + AMITheme.ELEMENT_GAP;
            int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
            if (compactMode) {
                gridView.updateLayout(innerX, contentY, innerW, contentH);
            } else {
                int toolbarW = innerW - TOGGLE_W - AMITheme.ELEMENT_GAP;
                toolbar.updateLayout(innerX + TOGGLE_W + AMITheme.ELEMENT_GAP, y + AMITheme.GLOBAL_PADDING, toolbarW);
                treeView.updateLayout(innerX, contentY, innerW, contentH);
                gridView.updateLayout(innerX, contentY, innerW, contentH);
            }
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        AMITheme.sync();
        checkPlayerStateChanged();

        if (AmiConfig.theme == AmiConfig.Theme.VANILLA) {
            g.blit(PANEL_SPRITE, x, y, 0, 0, width, height);
        } else {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            AMITheme.fillRounded(g, x, y, width, height, AMITheme.PANEL_BG);

            // Add subtle borders and accent line, only if defined by the theme
            if (AMITheme.BORDER_LIGHT != 0) {
                int border = AMITheme.BORDER_LIGHT;
                g.fill(x, y, x + width, y + 2, AMITheme.ACCENT_BLUE);
                g.fill(x, y + height - 1, x + width, y + height, border);
                g.fill(x, y, x + 1, y + height, border);
                g.fill(x + width - 1, y, x + width, y + height, border);
            }
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }

        boolean compact = compactMode;

        if (isFavoritesPanel) {
            if (chromeOnly) {
                return;
            }
            var font = Minecraft.getInstance().font;
            Component title = panelTitle != null ? panelTitle : Component.translatable("ami.gui.favorites");
            g.drawString(font, title.getString(), x + AMITheme.GLOBAL_PADDING, y + (FAV_HEADER_H - font.lineHeight) / 2, AMITheme.TEXT_HEADER, false);

            if (hasSidebarAlternate()) {
                renderSidebarToggle(g, mouseX, mouseY);
            }

            g.fill(x + 3, y + FAV_HEADER_H - 1, x + width - 3, y + FAV_HEADER_H, AMITheme.SECTION_SEP);

            if (isGridActive()) {
                gridView.render(g, mouseX, mouseY, false);
            } else {
                treeView.render(g, mouseX, mouseY, false, null, state);
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
            g.drawString(font, countStr, toggleX + TOGGLE_W + AMITheme.ELEMENT_GAP, textY, AMITheme.TEXT_SUBTLE, false);

            renderToggleBtn(g, mouseX, mouseY);

            g.fill(x + 3, sepY, x + width - 3, sepY + 1, AMITheme.SECTION_SEP);

            if (!com.sanhiruzu.ami.index.GlobalIndex.getInstance().isIndexReady()) {
                g.drawString(font, Component.translatable("ami.gui.loading_dots"), x + AMITheme.GLOBAL_PADDING, contentY, AMITheme.TEXT_SUBTLE, false);
            } else {
                gridView.render(g, mouseX, mouseY, false);
            }
            renderToggleTooltip(g, mouseX, mouseY);
            return;
        }

        // Full mode
        var font = Minecraft.getInstance().font;
        toolbar.render(g, mouseX, mouseY);
        renderToggleBtn(g, mouseX, mouseY);

        g.fill(x + 3, sepY, x + width - 3, sepY + 1, AMITheme.SECTION_SEP);

        if (!com.sanhiruzu.ami.index.AmiIndexerService.getInstance().isReady() && currentResults.isEmpty() && currentQuery.isEmpty()) {
            net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.translatable("ami.gui.background_indexing")
                    .withStyle(net.minecraft.ChatFormatting.GOLD);
            int contentH = height - (contentY - y) - AMITheme.GLOBAL_PADDING;
            int textMaxWidth = Math.max(32, width - (AMITheme.GLOBAL_PADDING * 4));
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(msg, textMaxWidth);
            int blockH = lines.size() * font.lineHeight;
            int drawY = contentY + Math.max(0, (contentH - blockH) / 2);
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                int lineW = font.width(line);
                g.drawString(font, line, x + (width - lineW) / 2, drawY, com.sanhiruzu.ami.client.AMITheme.WHITE, false);
                drawY += font.lineHeight;
            }
        } else {
            boolean dropdownOpen = toolbar.isAnyDropdownOpen();
            if (isGridActive()) {
                gridView.render(g, mouseX, mouseY, dropdownOpen);
            } else {
                treeView.render(g, mouseX, mouseY, dropdownOpen, null, state);
            }
        }

        toolbar.renderOpenDropdownLists(g, mouseX, mouseY);
        if (!toolbar.isAnyDropdownOpen()) {
            renderToggleTooltip(g, mouseX, mouseY);
        }
    }

    private void renderSidebarToggle(GuiGraphics g, int mouseX, int mouseY) {
        int tx = sidebarSwapX();
        int ty = sidebarSwapY();
        boolean hovered = isOverSidebarSwap(mouseX, mouseY);

        int bgColor = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        int border = AMITheme.SECTION_SEP;
        g.fill(tx, ty, tx + SIDEBAR_SWAP_W, ty + SIDEBAR_SWAP_H, bgColor);
        g.fill(tx, ty, tx + SIDEBAR_SWAP_W, ty + 1, border);
        g.fill(tx, ty + SIDEBAR_SWAP_H - 1, tx + SIDEBAR_SWAP_W, ty + SIDEBAR_SWAP_H, border);
        g.fill(tx, ty, tx + 1, ty + SIDEBAR_SWAP_H, border);
        g.fill(tx + SIDEBAR_SWAP_W - 1, ty, tx + SIDEBAR_SWAP_W, ty + SIDEBAR_SWAP_H, border);

        int color = hovered ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER;
        AmiGuiIcons.swap(g, tx + SIDEBAR_SWAP_W / 2, ty + SIDEBAR_SWAP_H / 2, color);
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

    private void renderToggleBtn(GuiGraphics g, int mouseX, int mouseY) {
        boolean compact = compactMode;
        boolean alternateActive = externalModeToggleActive != null && externalModeToggleActive.getAsBoolean();
        boolean hovered = mouseX >= toggleX && mouseX < toggleX + TOGGLE_W
                && mouseY >= toggleY && mouseY < toggleY + TOGGLE_H;

        int bgColor = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        g.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H, bgColor);

        // Borders
        int border = AMITheme.SECTION_SEP;
        g.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + 1, border);
        g.fill(toggleX, toggleY + TOGGLE_H - 1, toggleX + TOGGLE_W, toggleY + TOGGLE_H, border);
        g.fill(toggleX, toggleY, toggleX + 1, toggleY + TOGGLE_H, border);
        g.fill(toggleX + TOGGLE_W - 1, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H, border);

        int contentColor = hovered ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER;
        int cx = toggleX + TOGGLE_W / 2;
        int cy = toggleY + TOGGLE_H / 2;
        if (isGridActive()) {
            AmiGuiIcons.expand(g, cx, cy, contentColor);
        } else {
            AmiGuiIcons.compact(g, cx, cy, contentColor);
        }
    }

    private void renderToggleTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (!isOverToggle(mouseX, mouseY)) return;
        AmiTooltipRenderer.renderLeftOfCursor(g, Minecraft.getInstance().font, List.of(
                Component.translatable(isGridActive() ? "ami.gui.tooltip.view_switch_to_list" : "ami.gui.tooltip.view_switch_to_grid"),
                Component.translatable("ami.gui.tooltip.view_switch_detail")
        ), Optional.empty(), mouseX, mouseY);
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

        state.setAvailableListLenses(ListLens.availableFor(resolveSource()));
        ResultsViewProjector.Projection projection = ResultsViewProjector.project(
                resolveSource(),
                state,
                searchService,
                compactMode && !isFavoritesPanel,
                isFavoritesPanel
        );
        displayedItemCount = projection.displayedItemCount();
        setViewRoots(projection.roots());
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
    }

    // ── Item click (grid + list) ──────────────────────────────────────────────

    private List<SearchNode> resolveSource() {
        return currentResults;
    }

    private void onItemClicked(SearchNode node, int button) {
        ItemStack stack = com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node);
        boolean shiftDown = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        boolean controlDown = net.minecraft.client.gui.screens.Screen.hasControlDown();
        if (!stack.isEmpty() && shouldOpenLookup(node, stack, button, shiftDown, controlDown))
            RecipeViewerBridge.handleItemClick(stack, button, shiftDown, controlDown);
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
        return compactMode || state.getViewMode() == ResultsToolbar.ViewMode.GRID;
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    private boolean isOverToggle(double mouseX, double mouseY) {
        return mouseX >= toggleX && mouseX < toggleX + TOGGLE_W
                && mouseY >= toggleY && mouseY < toggleY + TOGGLE_H;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                    if (Services.PLATFORM.keyMappings().cheatGiveStack().isActiveAndMatches(mouseKey)) {
                        AMICheatMode.giveEntityStackAsSpawnEgg(hovered.id());
                        return true;
                    }
                    if (Services.PLATFORM.keyMappings().cheatGiveOne().isActiveAndMatches(mouseKey)) {
                        AMICheatMode.giveEntityAsSpawnEgg(hovered.id());
                        return true;
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

        // Compact toggle — always first, regardless of mode
        if (button == 0 && isOverToggle(mouseX, mouseY)) {
            if (externalModeToggleCallback != null) {
                externalModeToggleCallback.run();
            } else {
                compactMode = !compactMode;
                if (compactMode) {
                    resetSearchStateForCompact();
                }
                saveMainPanelViewPreference();
                updateLayout(x, y, width, height);
                refreshTree();
            }
            return true;
        }

        boolean compact = compactMode;
        if (compact) {
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

        if (toolbar.isAnyDropdownOpen()) return true;
        if (!compactMode && !isFavoritesPanel && toolbar.mouseScrolled(mouseX, mouseY, scrollDelta)) {
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

        if (!isMouseOver(mx, my)) return false;

        if (isShiftKey(keyCode) && getHoveredNode() != null) {
            return true;
        }

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

    public TreeNode getHoveredTreeNode() {
        if (isGridActive()) return gridView.getHoveredTreeNode();
        return treeView.getHoveredTreeNode();
    }
}
