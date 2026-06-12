package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiRenderProfiler;
import com.sanhiruzu.ami.client.AmiRenderPhase;
import com.sanhiruzu.ami.client.ItemIconBatchRenderer;
import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import com.sanhiruzu.ami.util.AmiTooltipComposer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Renders ITEM-type SearchNodes as a 3D item icon grid (18×18 cells).
 * Group nodes from the tree processor become collapsible section headers.
 * Non-ITEM leaf nodes are silently skipped.
 */
public class ItemGridView {
    private static final int CELL_SIZE = 20;
    private static final int HEADER_H = 13;
    private static final int ROOT_HEADER_H = 16;
    private static final int STICKY_CONTEXT_H = HEADER_H + 3;
    private static final int SCROLLBAR_W = 6;
    private static final int HEADER_INDENT = 12;
    private static final int GRID_LEFT_PAD = 1;
    private static final int ICON_CACHE_PRIME_BUDGET = 12;
    private static final ResourceLocation VANILLA_SCROLLER =
            Services.PLATFORM.rl("minecraft", "widget/scroller");
    private static final ResourceLocation VANILLA_SCROLLER_BACKGROUND =
            Services.PLATFORM.rl("minecraft", "widget/scroller_background");
    private static final boolean TEXTURE_ITEM_ICON_CACHE_ENABLED = Boolean.getBoolean("ami." + "itemIconCache");
    private Object itemIconBatchRenderer;
    private final List<PendingItemIcon> pendingDirectItemIcons = new ArrayList<>();
    private final List<PendingRendererIcon> pendingRendererIcons = new ArrayList<>();
    private final List<PendingIconOverlay> pendingIconOverlays = new ArrayList<>();
    private final List<PendingQuestMarker> pendingQuestMarkers = new ArrayList<>();
    private GridCellSpriteBatchRenderer gridCellSprites;
    private int pendingDirectItemIconCount;
    private int pendingRendererIconCount;
    private int pendingIconOverlayCount;
    private int pendingQuestMarkerCount;
    private int x, y, width, height;
    private List<TreeNode> rootNodes = new ArrayList<>();
    private int pixelScrollOffset = 0;
    private boolean scrollbarDragging = false;
    private int scrollbarDragStartY;
    private int scrollbarDragStartOffset;
    /**
     * Set by UniversalResultsPanel to route clicks to the recipe bridge.
     */
    private BiConsumer<SearchNode, Integer> onItemClick;
    private BiConsumer<TreeNode, Integer> onGroupClick;
    private java.util.function.Consumer<String> onTokenInject;
    // Deferred tooltips — built during render, drawn after scissor is popped
    private ItemStack pendingTooltip = null;
    private List<Component> pendingTextTooltip = null;
    private Optional<TooltipComponent> pendingTooltipImage = Optional.empty();
    private SearchNode hoveredNode = null;
    private TreeNode hoveredTreeNode = null;
    // Virtual row cache — rebuilt whenever rootNodes changes or a group is toggled
    private List<VirtualRow> cachedRows = null;
    private int cachedCols = -1;
    // Cached animation state per frame
    private float cachedWiggle = 0f;
    private float cachedRotation = 0f;
    private boolean cachedDragging = false;

    // =========================================================
    // Virtual row types
    // =========================================================

    public ItemGridView(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static void clearStackCache() {
        // The default item path batches only within the current frame.
    }

    private ItemIconBatchRenderer itemIconBatchRenderer() {
        if (itemIconBatchRenderer == null) {
            itemIconBatchRenderer = new ItemIconBatchRenderer();
        }
        return (ItemIconBatchRenderer) itemIconBatchRenderer;
    }

    private static int groupBandColor(boolean alternateBand) {
        return alternateBand ? AMITheme.GRID_GROUP_BAND_ALT : AMITheme.GRID_GROUP_BAND;
    }

    private static ItemStack resolveStack(SearchNode node) {
        if (node == null) return ItemStack.EMPTY;
        return ItemIconRenderer.resolveStack(node.id());
    }

    private static boolean isTokenInjectClick(int button) {
        return ViewInputHelper.isTokenInjectClick(button);
    }

    private static boolean isEmiRecipeScreenActive() {
        return com.sanhiruzu.ami.compat.RecipeViewerBridge.isEmiRecipeScreenActive();
    }

    // =========================================================
    // Public API
    // =========================================================

    public List<TreeNode> getRootNodes() {
        return List.copyOf(rootNodes);
    }

    public void setRootNodes(List<TreeNode> nodes) {
        setRootNodes(nodes, true);
    }

    public void setRootNodes(List<TreeNode> nodes, boolean resetScroll) {
        this.rootNodes = copyNodesForGrid(nodes);
        if (resetScroll) this.pixelScrollOffset = 0;
        this.cachedRows = null;
    }

    public void collapseAll() {
        for (TreeNode node : rootNodes) {
            setNodeExpanded(node, false);
        }
        this.pixelScrollOffset = 0;
        this.cachedRows = null;
    }

    public void expandAll() {
        for (TreeNode node : rootNodes) {
            setNodeExpanded(node, true);
        }
        this.pixelScrollOffset = 0;
        this.cachedRows = null;
    }

    public void invalidateCache() {
        this.cachedRows = null;
    }

    private void setNodeExpanded(TreeNode node, boolean expanded) {
        node.setExpanded(expanded);
        for (TreeNode child : node.getChildren()) {
            setNodeExpanded(child, expanded);
        }
    }

    private static List<TreeNode> copyNodesForGrid(List<TreeNode> nodes) {
        List<TreeNode> copies = new ArrayList<>();
        for (TreeNode node : nodes) {
            copies.add(copyNodeForGrid(node));
        }
        return copies;
    }

    private static TreeNode copyNodeForGrid(TreeNode node) {
        if (node.isLeaf()) {
            return new TreeNode(node.getLabel(), node.getEntry());
        }
        TreeNode copy = new TreeNode(node.getKey(), node.getLabel());
        copy.setExpanded(node.isHighCardinality() ? false : node.isExpanded());
        copy.setModGroup(node.isModGroup());
        copy.setHighCardinality(node.isHighCardinality());
        copy.setItemCountOverride(node.getItemCountOverride());
        for (TreeNode child : node.getChildren()) {
            copy.addChild(copyNodeForGrid(child));
        }
        return copy;
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.cachedRows = null; // cols may have changed
    }

    public void setItemClickCallback(BiConsumer<SearchNode, Integer> callback) {
        this.onItemClick = callback;
    }

    public void setGroupClickCallback(BiConsumer<TreeNode, Integer> callback) {
        this.onGroupClick = callback;
    }

    public void setOnTokenInject(java.util.function.Consumer<String> callback) {
        this.onTokenInject = callback;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, boolean toolbarDropdownOpen) {
        AmiRenderPhase.requireBase("ItemGridView.render");
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("grid.render")) {
        pendingTooltip = null;
        pendingTextTooltip = null;
        pendingTooltipImage = Optional.empty();
        hoveredNode = null;
        hoveredTreeNode = null;
        clearItemIconBatchRenderer();
        clearPendingQueues();
        clearGridCellSpriteRenderer();

        // Cache animation state once per frame
        cachedDragging = com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging();
        if (cachedDragging || mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            float time = (System.currentTimeMillis() % 1000) / 1000f;
            cachedWiggle = (float) Math.sin(time * Math.PI * 2) * 0.05f;
            cachedRotation = (float) Math.sin(time * Math.PI * 4) * 2f;
        } else {
            cachedWiggle = 0f;
            cachedRotation = 0f;
        }

        if (rootNodes.isEmpty()) {
            g.drawString(Minecraft.getInstance().font,
                    Component.translatable("ami.gui.no_results"), x + AMITheme.GLOBAL_PADDING, y + AMITheme.GLOBAL_PADDING, AMITheme.GRID_NO_RESULTS_TEXT, false);
            return;
        }

        int effectiveMouseX = toolbarDropdownOpen ? -1 : mouseX;

        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        AmiRenderProfiler.add("grid.virtualRows", rows.size());
        int totalH = calcTotalHeight(rows);
        StickyContext stickyContext = stickyContext(rows);
        int contentY = contentY(stickyContext);
        int contentH = contentHeight(stickyContext);

        if (stickyContext != null) {
            renderStickyContext(g, stickyContext);
        }

        g.enableScissor(x, contentY, x + width, y + height);
        try {
            if (TEXTURE_ITEM_ICON_CACHE_ENABLED && !cachedDragging) {
                try (AmiRenderProfiler.Section icons = AmiRenderProfiler.section("grid.iconCachePrime")) {
                    primeIconCache(g, rows, contentY);
                }
            }

            try (AmiRenderProfiler.Section rowsSection = AmiRenderProfiler.section("grid.rows")) {
                int drawY = contentY - pixelScrollOffset;
                for (VirtualRow row : rows) {
                    int rowBottom = drawY + row.height();
                    if (rowBottom > contentY && drawY < y + height) {
                        if (row instanceof HeaderRow hr) {
                            AmiRenderProfiler.count("grid.visibleHeaders");
                            renderHeader(g, hr, drawY, effectiveMouseX, mouseY);
                        } else if (row instanceof ItemRow ir) {
                            AmiRenderProfiler.count("grid.visibleItemRows");
                            AmiRenderProfiler.add("grid.visibleCells", ir.items().size());
                            renderItemRow(g, ir, drawY, effectiveMouseX, mouseY);
                        }
                    }
                    drawY += row.height();
                }
            }

            gridCellSpriteRenderer().flush(g);
            renderItemIconBatch(g);
            renderPendingDirectIcons(g);
            renderPendingIconOverlays(g);
            renderPendingQuestMarkers(g);
        } finally {
            g.disableScissor();
            clearGridCellSpriteRenderer();
            clearItemIconBatchRenderer();
            clearPendingQueues();
        }

        renderScrollbar(g, totalH, contentY, contentH, mouseX, mouseY);

        }
    }

    public void renderPendingTooltip(GuiGraphics g, int mouseX, int mouseY) {
        try (AmiRenderProfiler.Section tooltipSection = AmiRenderProfiler.section("grid.tooltip")) {
            var font = Minecraft.getInstance().font;
            if (pendingTextTooltip != null) {
                ItemStack stackContext = (pendingTooltip != null) ? pendingTooltip : ItemStack.EMPTY;
                AmiTooltipRenderer.render(g, font, stackContext, pendingTextTooltip, pendingTooltipImage, mouseX, mouseY);
            } else if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
                AmiTooltipRenderer.render(g, font, pendingTooltip, mouseX, mouseY);
            }
        }
    }

    private void renderHeader(GuiGraphics g, HeaderRow hr, int drawY, int mouseX, int mouseY) {
        int rowH = hr.height();
        boolean hovered = mouseX >= x && mouseX < x + width - SCROLLBAR_W
                && mouseY >= drawY && mouseY < drawY + rowH;
        renderHeaderContext(g, hr.depth(), drawY, rowH, hr.alternateBand());
        if (hovered) {
            g.fill(x, drawY, x + width - SCROLLBAR_W, drawY + rowH, com.sanhiruzu.ami.client.AMITheme.ENTRY_HOVER);
            hoveredTreeNode = hr.node();
        }

        int indent = hr.depth() * HEADER_INDENT;
        int contentRight = x + width - SCROLLBAR_W;
        int rowX = x + 4 + indent;
        var font = Minecraft.getInstance().font;
        int textY = drawY + Math.max(1, (rowH - font.lineHeight) / 2);

        if (hr.depth() == 0) {
            g.fill(x, drawY, contentRight, drawY + 1, AMITheme.SECTION_SEP);
            g.fill(x, drawY, x + 2, drawY + rowH, AMITheme.ACCENT_GOLD);
            g.fill(x + 2, drawY + rowH - 1, contentRight - 2, drawY + rowH, AMITheme.SECTION_SEP);
            rowX += 2;
        }

        if (hr.depth() > 0) {
            int railX = x + 5 + (hr.depth() - 1) * HEADER_INDENT;
            g.fill(railX, drawY + 2, railX + 1, drawY + rowH - 2, AMITheme.GRID_GROUP_RAIL);
        }

        if (hr.toggleable()) {
            int caretColor = hovered ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_SUBTLE;
            String marker = hr.node().isExpanded() ? "▼" : "▶";
            g.drawString(font, marker, rowX, textY, caretColor, false);
            rowX += 10;
        }

        String count = Component.translatable("ami.gui.badge_count", hr.itemCount()).getString();
        int countW = font.width(count);
        int countX = contentRight - countW - 5;
        int labelMaxW = Math.max(0, countX - rowX - 6);
        String label = truncate(font, hr.node().getLabel().getString(), labelMaxW);
        int labelColor = hr.depth() == 0 ? AMITheme.ACCENT_GOLD : AMITheme.TEXT_HEADER;
        g.drawString(font, label, rowX, textY, labelColor, false);
        g.drawString(font, count, countX, textY, AMITheme.TEXT_SUBTLE, false);
    }

    // =========================================================
    // Rendering
    // =========================================================

    private void renderItemRow(GuiGraphics g, ItemRow ir, int drawY, int mouseX, int mouseY) {
        renderGroupContext(g, ir.depth(), drawY, ir.alternateBand());
        renderRowSlotBackgrounds(g, drawY, ir.items().size());
        for (int i = 0; i < ir.items().size(); i++) {
            int cellX = gridLeftX() + i * CELL_SIZE;
            int cellY = drawY;

            TreeNode node = ir.items().get(i);

            SearchNode entry = null;
            ItemStack overrideStack = null;
            ResourceLocation overrideId = null;

            if (node.isLeaf()) {
                entry = node.getEntry();
            } else if (node.isHighCardinality()) {
                entry = node.getChildren().get(0).getEntry(); // For tooltips
                String key = node.getKey();
                if (key.startsWith("cardinality:")) {
                    String baseIdStr = key.substring(12);
                    ResourceLocation baseLoc = ResourceLocation.tryParse(baseIdStr);
                    if (baseLoc != null) {
                        net.minecraft.world.item.Item baseItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(baseLoc);
                        if (baseItem != null && baseItem != net.minecraft.world.item.Items.AIR) {
                            overrideStack = new ItemStack(baseItem);
                            overrideId = baseLoc;
                        }
                    }
                }
            } else {
                continue; // Normal group headers are handled by renderHeader
            }

            if (entry == null) continue;

            boolean hovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= cellY && mouseY < cellY + CELL_SIZE;

            if (hovered) {
                gridCellSpriteRenderer().hover(cellX, cellY);
                hoveredNode = entry;
                hoveredTreeNode = node;

                if (node.isHighCardinality()) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(node.getLabel().copy().append(" ").append(Component.translatable("ami.gui.items_count", node.getChildren().size())));
                    lines.add(Component.translatable(node.isExpanded() ? "ami.gui.collapse_hint" : "ami.gui.expand_hint")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                    lines.add(Component.translatable("ami.gui.group_filter_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                    pendingTextTooltip = AmiTooltipComposer.normalizeTooltipLines(lines);
                    pendingTooltipImage = Optional.empty();
                } else if (com.sanhiruzu.ami.client.AmiKeybindHandler.isDebugTooltipsActive()) {
                    pendingTextTooltip = AmiTooltipComposer.normalizeTooltipLines(DebugTooltip.build(entry));
                    pendingTooltipImage = Optional.empty();
                } else {
                    pendingTextTooltip = AmiTooltipComposer.buildTooltip(entry);
                    pendingTooltipImage = AmiTooltipComposer.getTooltipImage(entry);
                    if (entry.type() == com.sanhiruzu.ami.index.NodeType.ITEM) {
                        pendingTooltip = resolveStack(entry);
                    } else {
                        pendingTooltip = null;
                    }
                }
            }

            // Group styling
            if (node.isHighCardinality()) {
                // Gold border for family cards, including the expanded collapse target.
                gridCellSpriteRenderer().goldBorder(cellX, cellY);
            }

            if (overrideStack != null) {
                queueItemIcon(entry, overrideId, overrideStack, cellX + 2, cellY + 2, hovered);
            } else if (entry.type() == com.sanhiruzu.ami.index.NodeType.ITEM) {
                if (usesPlayerModelRenderer(entry)) {
                    AmiRenderProfiler.count("grid.queuedRendererIcons");
                    AmiRenderProfiler.count("grid.rendererIcon.PLAYER_MODEL");
                    queueRendererIcon(entry, cellX + 2, cellY + 2, hovered);
                    queueIconOverlay(entry, cellX + 2, cellY + 2);
                } else {
                    ItemStack stack = resolveStack(entry);
                    if (!stack.isEmpty()) queueItemIcon(entry, entry.id(), stack, cellX + 2, cellY + 2, hovered);
                }
            } else {
                AmiRenderProfiler.count("grid.queuedRendererIcons");
                AmiRenderProfiler.count("grid.rendererIcon." + entry.type().name());
                queueRendererIcon(entry, cellX + 2, cellY + 2, hovered);
                queueIconOverlay(entry, cellX + 2, cellY + 2);
            }

            if (node.isLeaf()) {
                queueQuestMarker(entry, cellX, cellY);
            }
        }
    }

    private void queueItemIcon(SearchNode entry, ResourceLocation itemId, ItemStack stack, int x, int y, boolean hovered) {
        if (TEXTURE_ITEM_ICON_CACHE_ENABLED
                || hovered
                || cachedDragging) {
            AmiRenderProfiler.count("grid.queuedDirectIcons");
            queueDirectItemIcon(entry, itemId, stack, x, y, hovered);
        } else {
            AmiRenderProfiler.count("grid.queuedBatchedIcons");
            itemIconBatchRenderer().add(stack, x, y);
        }
        queueIconOverlay(entry, x, y);
    }

    private void clearPendingQueues() {
        pendingDirectItemIconCount = 0;
        pendingRendererIconCount = 0;
        pendingIconOverlayCount = 0;
        pendingQuestMarkerCount = 0;
    }

    private void queueDirectItemIcon(SearchNode entry, ResourceLocation itemId, ItemStack stack, int x, int y, boolean hovered) {
        PendingItemIcon icon;
        if (pendingDirectItemIconCount < pendingDirectItemIcons.size()) {
            icon = pendingDirectItemIcons.get(pendingDirectItemIconCount);
        } else {
            icon = new PendingItemIcon();
            pendingDirectItemIcons.add(icon);
        }
        pendingDirectItemIconCount++;
        icon.set(entry, itemId, stack, x, y, hovered);
    }

    private void queueRendererIcon(SearchNode entry, int x, int y, boolean hovered) {
        PendingRendererIcon icon;
        if (pendingRendererIconCount < pendingRendererIcons.size()) {
            icon = pendingRendererIcons.get(pendingRendererIconCount);
        } else {
            icon = new PendingRendererIcon();
            pendingRendererIcons.add(icon);
        }
        pendingRendererIconCount++;
        icon.set(entry, x, y, hovered);
    }

    private void queueIconOverlay(SearchNode entry, int x, int y) {
        if (!DiscoveryVisuals.hasDiscoveryState(entry) && !AccessLevelVisuals.hasDevOnlyMarker(entry)) {
            return;
        }
        PendingIconOverlay overlay;
        if (pendingIconOverlayCount < pendingIconOverlays.size()) {
            overlay = pendingIconOverlays.get(pendingIconOverlayCount);
        } else {
            overlay = new PendingIconOverlay();
            pendingIconOverlays.add(overlay);
        }
        pendingIconOverlayCount++;
        overlay.set(entry, x, y);
    }

    private void queueQuestMarker(SearchNode entry, int cellX, int cellY) {
        PendingQuestMarker marker;
        if (pendingQuestMarkerCount < pendingQuestMarkers.size()) {
            marker = pendingQuestMarkers.get(pendingQuestMarkerCount);
        } else {
            marker = new PendingQuestMarker();
            pendingQuestMarkers.add(marker);
        }
        pendingQuestMarkerCount++;
        marker.set(entry, cellX, cellY);
    }

    private void renderItemIconBatch(GuiGraphics g) {
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("grid.iconBatch")) {
            if (itemIconBatchRenderer instanceof ItemIconBatchRenderer renderer) {
                renderer.render(g);
            }
        }
    }

    private void clearItemIconBatchRenderer() {
        if (itemIconBatchRenderer instanceof ItemIconBatchRenderer renderer) {
            renderer.clear();
        }
    }

    private GridCellSpriteBatchRenderer gridCellSpriteRenderer() {
        if (gridCellSprites == null) {
            gridCellSprites = new GridCellSpriteBatchRenderer();
        }
        return gridCellSprites;
    }

    private void clearGridCellSpriteRenderer() {
        if (gridCellSprites != null) {
            gridCellSprites.clear();
        }
    }

    private void renderPendingDirectIcons(GuiGraphics g) {
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("grid.directIcons")) {
            AmiRenderProfiler.add("grid.renderedDirectIcons", pendingDirectItemIconCount);
            AmiRenderProfiler.add("grid.renderedRendererIcons", pendingRendererIconCount);
            for (int i = 0; i < pendingDirectItemIconCount; i++) {
                PendingItemIcon icon = pendingDirectItemIcons.get(i);
                renderIconWithWiggle(g, icon.itemId, icon.stack, icon.x, icon.y, icon.hovered);
            }
            for (int i = 0; i < pendingRendererIconCount; i++) {
                PendingRendererIcon icon = pendingRendererIcons.get(i);
                renderRendererWithWiggle(g, icon.entry, icon.x, icon.y, icon.hovered);
            }
        }
    }

    private void renderPendingIconOverlays(GuiGraphics g) {
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("grid.iconOverlays")) {
            AmiRenderProfiler.add("grid.iconOverlays", pendingIconOverlayCount);
            for (int i = 0; i < pendingIconOverlayCount; i++) {
                PendingIconOverlay overlay = pendingIconOverlays.get(i);
                DiscoveryVisuals.renderIconOverlay(g, overlay.entry, overlay.x, overlay.y, 16);
            }
            for (int i = 0; i < pendingIconOverlayCount; i++) {
                PendingIconOverlay overlay = pendingIconOverlays.get(i);
                AccessLevelOverlayRenderer.renderIconOverlay(g, overlay.entry, overlay.x, overlay.y, 16);
            }
        }
    }

    private void renderPendingQuestMarkers(GuiGraphics g) {
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("grid.questMarkers")) {
            AmiRenderProfiler.add("grid.questMarkers", pendingQuestMarkerCount);
            for (int i = 0; i < pendingQuestMarkerCount; i++) {
                PendingQuestMarker marker = pendingQuestMarkers.get(i);
                renderQuestMarker(g, marker.entry, marker.cellX, marker.cellY);
            }
        }
    }

    private void renderQuestMarker(GuiGraphics g, SearchNode entry, int cellX, int cellY) {
        QuestItemEvidence evidence = QuestItemEvidenceProjector.project(entry);
        if (!evidence.hasMatches()) {
            return;
        }
        int markerX = cellX + CELL_SIZE - 7;
        int markerY = cellY + 1;
        QuestMarkerSpriteRenderer.render(g, markerX, markerY, evidence.hasRequirement(), evidence.totalCount() > 1);
    }

    private void renderIconWithWiggle(GuiGraphics g, ItemStack stack, int x, int y, boolean hovered) {
        renderIconWithWiggle(g, null, stack, x, y, hovered);
    }

    private void renderIconWithWiggle(GuiGraphics g, ResourceLocation itemId, ItemStack stack, int x, int y, boolean hovered) {
        if (TEXTURE_ITEM_ICON_CACHE_ENABLED && !hovered && !cachedDragging && itemId != null && ItemIconCache.isCached(itemId)) {
            ItemIconCache.blit(g, itemId, x, y);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x + 8, y + 8, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);
        if (cachedDragging || hovered) {
            g.pose().scale(1.1f + cachedWiggle, 1.1f + cachedWiggle, 1.1f);
            if (cachedDragging) {
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(cachedRotation));
            }
        }
        g.renderItem(stack, -8, -8);
        g.pose().popPose();
    }

    private void renderRendererWithWiggle(GuiGraphics g, SearchNode entry, int x, int y, boolean hovered) {
        boolean playerModelRenderer = usesPlayerModelRenderer(entry);
        boolean needsCellClip = !hovered && (playerModelRenderer || needsRendererCellClip(entry.type()));
        if (needsCellClip) {
            g.enableScissor(x, y, x + 16, y + 16);
        }
        g.pose().pushPose();
        g.pose().translate(x + 8, y + 8, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);
        if (cachedDragging || hovered) {
            g.pose().scale(1.1f + cachedWiggle, 1.1f + cachedWiggle, 1.1f);
            if (cachedDragging) {
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(cachedRotation));
            }
        }
        try {
            var renderer = playerModelRenderer
                    ? com.sanhiruzu.ami.client.icon.RendererRegistry.PLAYER_MODEL
                    : com.sanhiruzu.ami.client.icon.RendererRegistry.get(entry.type());
            renderer.render(g, entry, -8, -8, 16, hovered);
        } finally {
            g.pose().popPose();
            if (needsCellClip) {
                g.disableScissor();
            }
        }
    }

    private static boolean needsRendererCellClip(NodeType type) {
        return type == NodeType.ENTITY || type == NodeType.PLAYER;
    }

    private static boolean usesPlayerModelRenderer(SearchNode entry) {
        return com.sanhiruzu.ami.config.AmiConfig.playerHeadShowFullModel
                && entry != null
                && entry.type() == NodeType.ITEM
                && !entry.meta(com.sanhiruzu.ami.index.SearchNodeKeys.PLAYER_HEAD_NAME, "").isBlank();
    }

    private void primeIconCache(GuiGraphics g, List<VirtualRow> rows, int contentY) {
        List<Map.Entry<ResourceLocation, ItemStack>> uncached = new ArrayList<>();
        int drawY = contentY - pixelScrollOffset;
        for (VirtualRow row : rows) {
            if (drawY + row.height() > contentY && drawY < y + height && row instanceof ItemRow ir) {
                for (TreeNode node : ir.items()) {
                    ResourceLocation overrideId = null;
                    ItemStack overrideStack = null;
                    SearchNode entry = null;

                    if (node.isLeaf()) {
                        entry = node.getEntry();
                    } else if (node.isHighCardinality()) {
                        entry = node.getChildren().get(0).getEntry();
                        String key = node.getKey();
                        if (key.startsWith("cardinality:")) {
                            String baseIdStr = key.substring(12);
                            ResourceLocation baseLoc = ResourceLocation.tryParse(baseIdStr);
                            if (baseLoc != null) {
                                net.minecraft.world.item.Item baseItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(baseLoc);
                                if (baseItem != null && baseItem != net.minecraft.world.item.Items.AIR) {
                                    overrideStack = new ItemStack(baseItem);
                                    overrideId = baseLoc;
                                }
                            }
                        }
                    }

                    if (overrideStack != null && overrideId != null && !com.sanhiruzu.ami.client.ItemIconCache.isCached(overrideId)) {
                        uncached.add(Map.entry(overrideId, overrideStack));
                    } else if (entry != null && entry.type() == com.sanhiruzu.ami.index.NodeType.ITEM && !com.sanhiruzu.ami.client.ItemIconCache.isCached(entry.id())) {
                        ItemStack stack = resolveStack(entry);
                        if (!stack.isEmpty()) uncached.add(Map.entry(entry.id(), stack));
                    }
                }
            }
            drawY += row.height();
        }
        if (!uncached.isEmpty()) ItemIconCache.primeVisible(g, uncached, ICON_CACHE_PRIME_BUDGET);
    }

    private List<VirtualRow> getVirtualRows(int cols) {
        if (cachedRows == null || cachedCols != cols) {
            cachedRows = buildVirtualRows(cols);
            cachedCols = cols;
        }
        return cachedRows;
    }

    private List<VirtualRow> buildVirtualRows(int cols) {
        List<VirtualRow> rows = new ArrayList<>();

        List<TreeNode> linearItems = new ArrayList<>();
        BandSequence bands = new BandSequence();
        for (TreeNode root : rootNodes) {
            processNode(root, 0, cols, rows, linearItems, bands, false);
        }
        packIntoRows(linearItems, cols, rows, 0, false);

        return rows;
    }

    // =========================================================
    // Virtual row construction
    // =========================================================

    private void processNode(TreeNode node, int depth, int cols, List<VirtualRow> out, List<TreeNode> linearItems,
                             BandSequence bands, boolean alternateBand) {
        if (node.isLeaf()) {
            linearItems.add(node);
        } else if (node.isHighCardinality()) {
            linearItems.add(node);
            if (node.isExpanded()) {
                for (TreeNode child : node.getChildren()) {
                    if (child.isLeaf() || child.isHighCardinality()) {
                        linearItems.add(child);
                    } else {
                        packIntoRows(linearItems, cols, out, depth, alternateBand);
                        linearItems.clear();
                        processNode(child, depth + 1, cols, out, linearItems, bands, alternateBand);
                    }
                }
            }
        } else {
            packIntoRows(linearItems, cols, out, depth, alternateBand);
            linearItems.clear();

            boolean nodeBand = bands.nextBand();
            // Calculate total item count in this group recursively for the header label
            int totalItems = countItemsRecursive(node);
            out.add(new HeaderRow(node, depth, totalItems, true, nodeBand));

            if (node.isExpanded()) {
                List<TreeNode> inlineItems = node.getChildren().stream()
                        .filter(child -> child.isLeaf() || child.isHighCardinality())
                        .toList();
                List<TreeNode> childGroups = node.getChildren().stream()
                        .filter(child -> !child.isLeaf() && !child.isHighCardinality())
                        .toList();

                for (TreeNode child : inlineItems) {
                    processNode(child, depth + 1, cols, out, linearItems, bands, nodeBand);
                }
                packIntoRows(linearItems, cols, out, depth + 1, nodeBand);
                linearItems.clear();

                for (TreeNode childGroup : childGroups) {
                    processNode(childGroup, depth + 1, cols, out, linearItems, bands, nodeBand);
                }
            }
        }
    }

    private void renderHeaderContext(GuiGraphics g, int depth, int drawY, int rowH, boolean alternateBand) {
        int contentX = x;
        int contentRight = x + width - SCROLLBAR_W;
        int color = depth == 0 ? AMITheme.GRID_GROUP_ROOT_BG : AMITheme.GRID_GROUP_CHILD_BG;
        g.fill(contentX, drawY, contentRight, drawY + rowH, color);
        if (depth == 0) {
            g.fill(contentX, drawY, contentRight, drawY + rowH, AMITheme.GRID_HEADER_DARKEN);
        } else {
            g.fill(contentX, drawY, contentRight, drawY + rowH, groupBandColor(alternateBand));
        }
    }

    private void renderGroupContext(GuiGraphics g, int depth, int drawY, boolean alternateBand) {
        int contentX = x;
        int contentRight = x + width - SCROLLBAR_W;
        g.fill(contentX, drawY, contentRight, drawY + CELL_SIZE, groupBandColor(alternateBand));
    }

    private void renderRowSlotBackgrounds(GuiGraphics g, int rowY, int itemCount) {
        if ((AMITheme.SLOT_BG >>> 24) == 0) return;
        if (itemCount <= 0) return;
        AmiRenderProfiler.add("grid.slotBackgrounds", itemCount);
        int rowX = gridLeftX();
        for (int i = 0; i < itemCount; i++) {
            gridCellSpriteRenderer().slot(rowX + i * CELL_SIZE, rowY);
        }
    }

    private void renderStickyContext(GuiGraphics g, StickyContext context) {
        int contentRight = x + width - SCROLLBAR_W;
        int headerBottom = y + STICKY_CONTEXT_H;
        g.fill(x, y, contentRight, headerBottom, AMITheme.GRID_HEADER_DARKEN);
        g.fill(x, headerBottom - 1, contentRight, headerBottom, AMITheme.GRID_GROUP_BAND);

        var font = Minecraft.getInstance().font;
        String label = context.label();
        int maxWidth = Math.max(0, contentRight - x - 8);
        if (font.width(label) > maxWidth) {
            label = font.plainSubstrByWidth(label, Math.max(0, maxWidth - font.width("..."))) + "...";
        }
        g.drawString(font, label, x + 4, y + 3, AMITheme.TEXT_HEADER, false);
    }

    private static String truncate(net.minecraft.client.gui.Font font, String text, int maxW) {
        if (text == null || text.isEmpty() || maxW <= 0) return "";
        if (font.width(text) <= maxW) return text;

        String ellipsis = "...";
        int ellipsisW = font.width(ellipsis);
        if (maxW <= ellipsisW) return ellipsis;

        return font.plainSubstrByWidth(text, maxW - ellipsisW) + ellipsis;
    }

    private int contentY(StickyContext context) {
        return y + (context == null ? 0 : STICKY_CONTEXT_H);
    }

    private int contentHeight(StickyContext context) {
        return Math.max(1, height - (context == null ? 0 : STICKY_CONTEXT_H));
    }

    private StickyContext stickyContext(List<VirtualRow> rows) {
        if (pixelScrollOffset <= 0) {
            return null;
        }

        List<HeaderRow> stack = new ArrayList<>();
        int rowTop = 0;
        for (VirtualRow row : rows) {
            int rowBottom = rowTop + row.height();
            if (rowTop >= pixelScrollOffset) {
                break;
            }
            if (row instanceof HeaderRow header) {
                while (stack.size() > header.depth()) {
                    stack.remove(stack.size() - 1);
                }
                if (stack.size() == header.depth()) {
                    stack.add(header);
                } else if (stack.size() > header.depth()) {
                    stack.set(header.depth(), header);
                }
            }
            if (rowBottom > pixelScrollOffset) {
                break;
            }
            rowTop = rowBottom;
        }

        if (stack.isEmpty()) {
            return null;
        }
        return new StickyContext(stack.stream()
                .map(header -> header.node().getLabel().getString())
                .filter(label -> !label.isBlank())
                .reduce((left, right) -> left + " / " + right)
                .orElse(""));
    }

    private int countItemsRecursive(TreeNode node) {
        if (node.getItemCountOverride() != -1) return node.getItemCountOverride();
        if (node.isLeaf()) return 1;
        int sum = 0;
        for (TreeNode child : node.getChildren()) {
            sum += countItemsRecursive(child);
        }
        return sum;
    }

    private void packIntoRows(List<TreeNode> items, int cols, List<VirtualRow> out, int depth, boolean alternateBand) {
        for (int i = 0; i < items.size(); i += cols) {
            out.add(new ItemRow(new ArrayList<>(items.subList(i, Math.min(i + cols, items.size()))), depth, alternateBand));
        }
    }

    private int calcTotalHeight(List<VirtualRow> rows) {
        int h = 0;
        for (VirtualRow r : rows) h += r.height();
        return h;
    }

    private int computeCols() {
        if (com.sanhiruzu.ami.config.AmiConfig.gridColumns > 0) {
            return Math.max(1, Math.min(com.sanhiruzu.ami.config.AmiConfig.gridColumns, 16));
        }
        return Math.max(1, (x + width - SCROLLBAR_W - gridLeftX() - 1) / CELL_SIZE);
    }

    private int gridLeftX() {
        return x + GRID_LEFT_PAD;
    }

    private void renderScrollbar(GuiGraphics g, int totalH, int contentY, int contentH, int mouseX, int mouseY) {
        if (totalH <= contentH) return;
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("grid.scrollbar")) {
            AmiRenderProfiler.count("grid.scrollbars");
            boolean active = scrollbarDragging || isScrollbarHovered(mouseX, mouseY);
            boolean vanilla = AmiConfig.theme == AmiConfig.Theme.VANILLA;
            int thumbH = vanilla ? vanillaThumbHeight(totalH, contentH) : Math.max(12, (contentH * contentH) / totalH);
            int maxScroll = totalH - contentH;
            int thumbY = contentY + (pixelScrollOffset * (contentH - thumbH)) / maxScroll;

            if (vanilla) {
                int barX = x + width - SCROLLBAR_W;
                Services.PLATFORM.renderVanillaScrollbar(g, VANILLA_SCROLLER, VANILLA_SCROLLER_BACKGROUND,
                        barX, contentY, SCROLLBAR_W, contentH, thumbY, thumbH);
            } else {
                int barW = active ? 6 : 4;
                int barX = x + width - 1 - barW;
                ScrollbarSpriteRenderer.renderTrack(g, x + width - SCROLLBAR_W, contentY, SCROLLBAR_W, y + height - contentY);
                ScrollbarSpriteRenderer.renderThumb(g, barX, thumbY, barW, thumbH, active);
            }
        }
    }

    private static int vanillaThumbHeight(int totalH, int contentH) {
        int max = Math.max(1, contentH - 8);
        int min = Math.min(32, max);
        return net.minecraft.util.Mth.clamp((contentH * contentH) / totalH, min, max);
    }

    private boolean isScrollbarHovered(int mouseX, int mouseY) {
        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        StickyContext stickyContext = stickyContext(rows);
        int totalH = calcTotalHeight(rows);
        int contentY = contentY(stickyContext);
        int contentH = contentHeight(stickyContext);
        if (totalH <= contentH) return false;
        // Widen hitbox to 10px
        return mouseX >= x + width - 10 && mouseX < x + width
                && mouseY >= contentY && mouseY < y + height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;

        if (!isMouseOver(mouseX, mouseY)) return false;
        if (mouseX >= x + width - SCROLLBAR_W) return false;

        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        StickyContext stickyContext = stickyContext(rows);
        int contentY = contentY(stickyContext);
        if (mouseY < contentY) {
            return false;
        }

        int drawY = contentY - pixelScrollOffset;
        for (VirtualRow row : rows) {
            if (mouseY >= drawY && mouseY < drawY + row.height()) {
                if (row instanceof HeaderRow hr) {
                    if (button == 0 && hr.toggleable()) {
                        hr.node().setExpanded(!hr.node().isExpanded());
                        cachedRows = null; // rebuild
                    } else if (isTokenInjectClick(button) && hr.toggleable() && onTokenInject != null) {
                        // Ctrl+right-click on group header: inject category token
                        onTokenInject.accept("$" + hr.node().getKey());
                    } else if (button == 1 && onGroupClick != null) {
                        onGroupClick.accept(hr.node(), button);
                    } else if (!hr.toggleable()) {
                        return false;
                    }
                    return true;
                } else if (row instanceof ItemRow ir) {
                    int col = ((int) mouseX - gridLeftX()) / CELL_SIZE;
                    if (col >= 0 && col < ir.items().size()) {
                        TreeNode node = ir.items().get(col);
                        if (node.isHighCardinality()) {
                            if (button == 0) {
                                node.setExpanded(!node.isExpanded());
                                cachedRows = null;
                            } else if (button == 1 && onGroupClick != null) {
                                onGroupClick.accept(node, button);
                            }
                            return true;
                        }

                        SearchNode entry = node.getEntry();
                        if (entry != null) {
                            if (isTokenInjectClick(button) && onTokenInject != null) {
                                // Ctrl+right-click on item: inject mod name
                                onTokenInject.accept("@" + entry.id().getNamespace());
                            } else if (onItemClick != null) {
                                onItemClick.accept(entry, button);
                            }
                            return true;
                        }
                    }
                }
                return false;
            }
            drawY += row.height();
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        StickyContext stickyContext = stickyContext(rows);
        int totalH = calcTotalHeight(rows);
        int contentH = contentHeight(stickyContext);
        int maxScroll = Math.max(0, totalH - contentH);

        // Adaptive scroll speed: 1/2 of visible height per tick
        // With 400 mods, this provides snappy scrolling like EMI
        int visibleRows = Math.max(1, contentH / CELL_SIZE);
        int scrollAmount = (visibleRows * CELL_SIZE) / 2;

        pixelScrollOffset = Math.max(0, Math.min(maxScroll,
                (int) (pixelScrollOffset - delta * scrollAmount)));
        return true;
    }

    // =========================================================
    // Scrollbar
    // =========================================================

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_A) {
            if (hoveredNode != null) {
                com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().toggleFavorite(hoveredNode);
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            if (pendingTextTooltip != null && !pendingTextTooltip.isEmpty()) {
                AmiClipboardHelper.copyComponentsToClipboard(pendingTextTooltip);
                return true;
            } else if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
                AmiClipboardHelper.copyItemTooltipToClipboard(pendingTooltip);
                return true;
            }
        }

        if (keyCode == 266) { // Page Up
            int cols = computeCols();
            StickyContext stickyContext = stickyContext(getVirtualRows(cols));
            pixelScrollOffset = Math.max(0, pixelScrollOffset - contentHeight(stickyContext));
            return true;
        } else if (keyCode == 267) { // Page Down
            int cols = computeCols();
            List<VirtualRow> rows = getVirtualRows(cols);
            StickyContext stickyContext = stickyContext(rows);
            int contentH = contentHeight(stickyContext);
            int totalH = calcTotalHeight(rows);
            int maxScroll = Math.max(0, totalH - contentH);
            pixelScrollOffset = Math.min(maxScroll, pixelScrollOffset + contentH);
            return true;
        }
        return false;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (button != 0 || !isScrollbarHovered((int) mouseX, (int) mouseY)) return false;
        scrollbarDragging = true;
        scrollbarDragStartY = (int) mouseY;
        scrollbarDragStartOffset = pixelScrollOffset;
        return true;
    }

    // =========================================================
    // Input handlers
    // =========================================================

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!scrollbarDragging || button != 0) return false;
        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        StickyContext stickyContext = stickyContext(rows);
        int contentH = contentHeight(stickyContext);
        int totalH = calcTotalHeight(rows);
        if (totalH <= contentH) return true;
        int thumbH = AmiConfig.theme == AmiConfig.Theme.VANILLA
                ? vanillaThumbHeight(totalH, contentH)
                : Math.max(12, (contentH * contentH) / totalH);
        int dragRange = contentH - thumbH;
        if (dragRange <= 0) return true;
        int dyPx = (int) mouseY - scrollbarDragStartY;
        int offsetDelta = (int) Math.round((double) dyPx * (totalH - contentH) / dragRange);
        pixelScrollOffset = Math.max(0, Math.min(totalH - contentH, scrollbarDragStartOffset + offsetDelta));
        return true;
    }

    public void stopScrollbarDrag() {
        scrollbarDragging = false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public SearchNode getHoveredNode() {
        return hoveredNode;
    }

    public TreeNode getHoveredTreeNode() {
        return hoveredTreeNode;
    }

    public int getDropIndex(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return -1;
        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        StickyContext stickyContext = stickyContext(rows);
        int contentY = contentY(stickyContext);
        if (mouseY < contentY) {
            return 0;
        }
        int itemCounter = 0;
        int drawY = contentY - pixelScrollOffset;
        for (VirtualRow row : rows) {
            if (mouseY >= drawY && mouseY < drawY + row.height()) {
                if (row instanceof HeaderRow) return itemCounter;
                if (row instanceof ItemRow ir) {
                    int col = ((int) mouseX - gridLeftX()) / CELL_SIZE;
                    return itemCounter + net.minecraft.util.Mth.clamp(col, 0, ir.items().size());
                }
            }
            if (row instanceof ItemRow ir) itemCounter += ir.items().size();
            drawY += row.height();
        }
        return itemCounter;
    }

    private sealed interface VirtualRow permits HeaderRow, ItemRow {
        int height();
    }

    private record HeaderRow(TreeNode node, int depth, int itemCount, boolean toggleable,
                             boolean alternateBand) implements VirtualRow {
        public int height() {
            return depth == 0 ? ROOT_HEADER_H : HEADER_H;
        }
    }

    private record ItemRow(List<TreeNode> items, int depth, boolean alternateBand) implements VirtualRow {
        public int height() {
            return CELL_SIZE;
        }
    }

    private record StickyContext(String label) {
    }

    private static final class PendingItemIcon {
        private SearchNode entry;
        private ResourceLocation itemId;
        private ItemStack stack = ItemStack.EMPTY;
        private int x;
        private int y;
        private boolean hovered;

        private void set(SearchNode entry, ResourceLocation itemId, ItemStack stack, int x, int y, boolean hovered) {
            this.entry = entry;
            this.itemId = itemId;
            this.stack = stack;
            this.x = x;
            this.y = y;
            this.hovered = hovered;
        }
    }

    private static final class PendingRendererIcon {
        private SearchNode entry;
        private int x;
        private int y;
        private boolean hovered;

        private void set(SearchNode entry, int x, int y, boolean hovered) {
            this.entry = entry;
            this.x = x;
            this.y = y;
            this.hovered = hovered;
        }
    }

    private static final class PendingIconOverlay {
        private SearchNode entry;
        private int x;
        private int y;

        private void set(SearchNode entry, int x, int y) {
            this.entry = entry;
            this.x = x;
            this.y = y;
        }
    }

    private static final class PendingQuestMarker {
        private SearchNode entry;
        private int cellX;
        private int cellY;

        private void set(SearchNode entry, int cellX, int cellY) {
            this.entry = entry;
            this.cellX = cellX;
            this.cellY = cellY;
        }
    }

    private static final class BandSequence {
        private int index;

        boolean nextBand() {
            return (index++ & 1) == 1;
        }
    }
}
