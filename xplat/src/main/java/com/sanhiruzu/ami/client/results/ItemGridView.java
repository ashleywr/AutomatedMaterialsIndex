package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer;
import com.sanhiruzu.ami.index.SearchNode;
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
    private static final int CELL_SIZE = 18;
    private static final int HEADER_H = 12;
    private static final int STICKY_CONTEXT_H = HEADER_H + 3;
    private static final int SCROLLBAR_W = 5;
    private static final int HEADER_INDENT = 12;

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
    private final Map<TreeNode, TreeNode> expandedGroupCache = new HashMap<>();

    // Cached animation state per frame
    private float cachedWiggle = 0f;
    private float cachedRotation = 0f;
    private boolean cachedDragging = false;

    // =========================================================
    // Virtual row types
    // =========================================================

    private sealed interface VirtualRow permits HeaderRow, ItemRow {
        int height();
    }

    private record HeaderRow(TreeNode node, int depth, int itemCount, boolean toggleable, boolean alternateBand) implements VirtualRow {
        public int height() {
            return HEADER_H;
        }
    }

    private record ItemRow(List<TreeNode> items, int depth, boolean alternateBand) implements VirtualRow {
        public int height() {
            return CELL_SIZE;
        }
    }

    private record StickyContext(String label) {
    }

    private static final class BandSequence {
        private int index;

        boolean nextBand() {
            return (index++ & 1) == 1;
        }
    }

    // =========================================================
    // Public API
    // =========================================================

    public ItemGridView(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setRootNodes(List<TreeNode> nodes) {
        this.rootNodes = nodes;
        this.pixelScrollOffset = 0;
        this.cachedRows = null;
    }

    public List<TreeNode> getRootNodes() {
        return List.copyOf(rootNodes);
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

    public void setOnTokenInject(java.util.function.Consumer<String> callback) {
        this.onTokenInject = callback;
    }

    public void setTooltipLeftOfCursor(boolean ignored) {
    }

    public static void clearStackCache() {
        // Managed by ItemIconRenderer
    }

    // =========================================================
    // Rendering
    // =========================================================

    public void render(GuiGraphics g, int mouseX, int mouseY, boolean toolbarDropdownOpen) {
        pendingTooltip = null;
        pendingTextTooltip = null;
        pendingTooltipImage = Optional.empty();
        hoveredNode = null;
        hoveredTreeNode = null;

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
        int totalH = calcTotalHeight(rows);
        StickyContext stickyContext = stickyContext(rows);
        int contentY = contentY(stickyContext);
        int contentH = contentHeight(stickyContext);

        if (stickyContext != null) {
            renderStickyContext(g, stickyContext);
        }

        g.enableScissor(x, contentY, x + width, y + height);

        int drawY = contentY - pixelScrollOffset;
        for (VirtualRow row : rows) {
            int rowBottom = drawY + row.height();
            if (rowBottom > contentY && drawY < y + height) {
                if (row instanceof HeaderRow hr) {
                    renderHeader(g, hr, drawY, effectiveMouseX, mouseY);
                } else if (row instanceof ItemRow ir) {
                    renderItemRow(g, ir, drawY, effectiveMouseX, mouseY);
                }
            }
            drawY += row.height();
        }

        g.disableScissor();

        renderScrollbar(g, totalH, contentY, contentH, mouseX, mouseY);

        if (!toolbarDropdownOpen) {
            var font = Minecraft.getInstance().font;
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            if (pendingTextTooltip != null) {
                AmiTooltipRenderer.renderLeftOfCursor(g, font, pendingTextTooltip, pendingTooltipImage, mouseX, mouseY);
            } else if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
                AmiTooltipRenderer.renderLeftOfCursor(g, font, pendingTooltip, mouseX, mouseY);
            }
        }
    }

    private void renderHeader(GuiGraphics g, HeaderRow hr, int drawY, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width - SCROLLBAR_W
                && mouseY >= drawY && mouseY < drawY + HEADER_H;
        renderHeaderContext(g, hr.depth(), drawY, hr.alternateBand());
        if (hovered) {
            g.fill(x, drawY, x + width - SCROLLBAR_W, drawY + HEADER_H, com.sanhiruzu.ami.client.AMITheme.ENTRY_HOVER);
            hoveredTreeNode = hr.node();
        }
        int indent = hr.depth() * HEADER_INDENT;
        String marker = "";
        if (hr.toggleable()) {
            marker = hr.node().isExpanded() ? "▼ " : "▶ ";
        }
        String label = marker + hr.node().getLabel().getString() + " (" + hr.itemCount() + ")";
        g.drawString(Minecraft.getInstance().font, label, x + 4 + indent, drawY + 2, com.sanhiruzu.ami.client.AMITheme.TEXT_HEADER, false);
    }

    private void renderItemRow(GuiGraphics g, ItemRow ir, int drawY, int mouseX, int mouseY) {
        int cols = computeCols();
        renderGroupContext(g, ir.depth(), drawY, ir.alternateBand());
        for (int i = 0; i < ir.items().size(); i++) {
            int cellX = x + 1 + i * CELL_SIZE;
            int cellY = drawY;

            TreeNode node = ir.items().get(i);

            SearchNode entry = null;
            ItemStack overrideStack = null;

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
                        }
                    }
                }
            } else {
                continue; // Normal group headers are handled by renderHeader
            }

            if (entry == null) continue;

            boolean hovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= cellY && mouseY < cellY + CELL_SIZE;
            
            // Render default slot background if defined by theme
            if (AMITheme.SLOT_BG != 0) {
                g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, AMITheme.SLOT_BG);
            }

            if (hovered) {
                // Background tint
                g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, com.sanhiruzu.ami.client.AMITheme.ENTRY_HOVER);
                // Bright border for clarity
                g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + 1, com.sanhiruzu.ami.client.AMITheme.ACCENT_BLUE);
                g.fill(cellX, cellY + CELL_SIZE - 1, cellX + CELL_SIZE, cellY + CELL_SIZE, com.sanhiruzu.ami.client.AMITheme.ACCENT_BLUE);
                g.fill(cellX, cellY + 1, cellX + 1, cellY + CELL_SIZE - 1, com.sanhiruzu.ami.client.AMITheme.ACCENT_BLUE);
                g.fill(cellX + CELL_SIZE - 1, cellY + 1, cellX + CELL_SIZE, cellY + CELL_SIZE - 1, com.sanhiruzu.ami.client.AMITheme.ACCENT_BLUE);
                hoveredNode = entry;
                hoveredTreeNode = node;

                if (node.isHighCardinality()) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(node.getLabel().copy().append(" ").append(Component.translatable("ami.gui.items_count", node.getChildren().size())));
                    lines.add(Component.translatable(node.isExpanded() ? "ami.gui.collapse_hint" : "ami.gui.expand_hint")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                    lines.add(Component.translatable("ami.gui.group_filter_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                    pendingTextTooltip = lines;
                    pendingTooltipImage = Optional.empty();
                } else if (com.sanhiruzu.ami.client.AmiKeybindHandler.isDebugTooltipsActive()) {
                    pendingTextTooltip = com.sanhiruzu.ami.client.results.DebugTooltip.build(entry);
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
            if (node.isHighCardinality() && !node.isExpanded()) {
                // Gold border for collapsed groups
                g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + 1, AMITheme.GRID_GOLD_BORDER);
                g.fill(cellX, cellY + CELL_SIZE - 1, cellX + CELL_SIZE, cellY + CELL_SIZE, AMITheme.GRID_GOLD_BORDER);
                g.fill(cellX, cellY, cellX + 1, cellY + CELL_SIZE, AMITheme.GRID_GOLD_BORDER);
                g.fill(cellX + CELL_SIZE - 1, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, AMITheme.GRID_GOLD_BORDER);
            } else {
                TreeNode expandedGroup = expandedGroupCache.get(node);
                if (expandedGroup != null) {
                    // Union shape border for expanded groups
                    int idx = (node == expandedGroup) ? 0 : 1 + expandedGroup.getChildren().indexOf(node);
                    int totalSize = 1 + expandedGroup.getChildren().size();
                    int col = i;

                    boolean topEdge = idx < cols;
                    boolean bottomEdge = idx + cols >= totalSize;
                    boolean leftEdge = col == 0 || idx == 0;
                    boolean rightEdge = col == cols - 1 || idx == totalSize - 1;

                    int color = AMITheme.GRID_GOLD_BORDER; // Opaque gold border
                    int bgCol = AMITheme.GRID_GOLD_TINT; // Visible gold tint background

                    g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, bgCol);

                    if (topEdge) g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + 1, color);
                    if (bottomEdge) g.fill(cellX, cellY + CELL_SIZE - 1, cellX + CELL_SIZE, cellY + CELL_SIZE, color);
                    if (leftEdge) g.fill(cellX, cellY, cellX + 1, cellY + CELL_SIZE, color);
                    if (rightEdge) g.fill(cellX + CELL_SIZE - 1, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, color);

                    // If this is the header node of the expanded group, make it look like a "close" button
                    if (node == expandedGroup) {
                        g.fill(cellX + 1, cellY + 1, cellX + CELL_SIZE - 1, cellY + CELL_SIZE - 1, AMITheme.GRID_HEADER_DARKEN); // Darken the header icon
                        g.fill(cellX + 2, cellY + 2, cellX + 4, cellY + 4, AMITheme.GRID_HEADER_WHITE_DOT); // Small visual indicator
                    }
                }
            }

            if (overrideStack != null) {
                renderIconWithWiggle(g, overrideStack, cellX + 1, cellY + 1, hovered);
            } else if (entry.type() == com.sanhiruzu.ami.index.NodeType.ITEM) {
                ItemStack stack = resolveStack(entry);
                if (!stack.isEmpty()) renderIconWithWiggle(g, entry.id(), stack, cellX + 1, cellY + 1, hovered);
            } else {
                renderRendererWithWiggle(g, entry, cellX + 1, cellY + 1, hovered);
            }
        }
    }

    private void renderIconWithWiggle(GuiGraphics g, ItemStack stack, int x, int y, boolean hovered) {
        renderIconWithWiggle(g, null, stack, x, y, hovered);
    }

    private void renderIconWithWiggle(GuiGraphics g, ResourceLocation itemId, ItemStack stack, int x, int y, boolean hovered) {
        g.pose().pushPose();
        g.pose().translate(x + 8, y + 8, 150);
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
        GuiGraphics iconGraphics = new GuiGraphics(Minecraft.getInstance(), g.bufferSource());
        if (!hovered) {
            g.enableScissor(x, y, x + 16, y + 16);
        }
        iconGraphics.pose().pushPose();
        iconGraphics.pose().translate(0, 0, 100);
        try {
            com.sanhiruzu.ami.client.icon.RendererRegistry.get(entry.type()).render(iconGraphics, entry, x, y, 16, hovered);
        } finally {
            iconGraphics.pose().popPose();
            if (!hovered) {
                g.disableScissor();
            }
        }
    }

    private void primeIconCache(GuiGraphics g, List<VirtualRow> rows) {
        List<Map.Entry<ResourceLocation, ItemStack>> uncached = new ArrayList<>();
        int drawY = y - pixelScrollOffset;
        for (VirtualRow row : rows) {
            if (drawY + row.height() > y && drawY < y + height && row instanceof ItemRow ir) {
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
        if (!uncached.isEmpty()) com.sanhiruzu.ami.client.ItemIconCache.primeVisible(g, uncached);
    }

    // =========================================================
    // Virtual row construction
    // =========================================================

    private List<VirtualRow> getVirtualRows(int cols) {
        if (cachedRows == null || cachedCols != cols) {
            cachedRows = buildVirtualRows(cols);
            cachedCols = cols;
        }
        return cachedRows;
    }

    private List<VirtualRow> buildVirtualRows(int cols) {
        List<VirtualRow> rows = new ArrayList<>();
        expandedGroupCache.clear();

        List<TreeNode> linearItems = new ArrayList<>();
        BandSequence bands = new BandSequence();
        for (TreeNode root : rootNodes) {
            processNode(root, 0, cols, rows, linearItems, bands);
        }
        packIntoRows(linearItems, cols, rows, 0, false);

        return rows;
    }

    private void processNode(TreeNode node, int depth, int cols, List<VirtualRow> out, List<TreeNode> linearItems, BandSequence bands) {
        if (node.isLeaf()) {
            linearItems.add(node);
        } else {
            packIntoRows(linearItems, cols, out, depth, false);
            linearItems.clear();

            boolean nodeBand = bands.nextBand();
            // Calculate total item count in this group recursively for the header label
            int totalItems = countItemsRecursive(node);
            out.add(new HeaderRow(node, depth, totalItems, true, nodeBand));

            if (node.isExpanded()) {
                List<TreeNode> childGroups = node.getChildren().stream().filter(child -> !child.isLeaf()).toList();
                List<TreeNode> directItems = node.getChildren().stream().filter(TreeNode::isLeaf).toList();

                for (int i = 0; i < childGroups.size(); i++) {
                    processNode(childGroups.get(i), depth + 1, cols, out, linearItems, bands);
                }
                boolean directItemsAlternateBand = nodeBand;
                if (!directItems.isEmpty() && !childGroups.isEmpty()) {
                    directItemsAlternateBand = bands.nextBand();
                }
                if (!childGroups.isEmpty() && !directItems.isEmpty()) {
                    out.add(new HeaderRow(createLooseItemsNode(node), depth + 1, directItems.size(), false, directItemsAlternateBand));
                }
                for (TreeNode child : directItems) {
                    processNode(child, depth + 1, cols, out, linearItems, bands);
                }
                packIntoRows(linearItems, cols, out, depth + 1, directItemsAlternateBand);
                linearItems.clear();
            }
        }
    }

    private static TreeNode createLooseItemsNode(TreeNode parent) {
        String parentKey = parent.getKey() == null ? "anonymous" : parent.getKey();
        return new TreeNode(parentKey + ":grid_other", Component.translatable("ami.group.other"));
    }

    private void renderHeaderContext(GuiGraphics g, int depth, int drawY, boolean alternateBand) {
        int contentX = x;
        int contentRight = x + width - SCROLLBAR_W;
        g.fill(contentX, drawY, contentRight, drawY + HEADER_H, groupBandColor(alternateBand));
    }

    private void renderGroupContext(GuiGraphics g, int depth, int drawY, boolean alternateBand) {
        int contentX = x;
        int contentRight = x + width - SCROLLBAR_W;
        g.fill(contentX, drawY, contentRight, drawY + CELL_SIZE, groupBandColor(alternateBand));
    }

    private static int groupBandColor(boolean alternateBand) {
        return alternateBand ? AMITheme.GRID_GROUP_BAND_ALT : AMITheme.GRID_GROUP_BAND;
    }

    private void renderStickyContext(GuiGraphics g, StickyContext context) {
        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        try {
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
        } finally {
            g.pose().popPose();
            g.flush();
        }
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
        return Math.max(1, (width - SCROLLBAR_W - 2) / CELL_SIZE);
    }

    private static ItemStack resolveStack(SearchNode node) {
        if (node == null) return ItemStack.EMPTY;
        return ItemIconRenderer.resolveStack(node.id());
    }

    // =========================================================
    // Scrollbar
    // =========================================================

    private void renderScrollbar(GuiGraphics g, int totalH, int contentY, int contentH, int mouseX, int mouseY) {
        if (totalH <= contentH) return;
        boolean active = scrollbarDragging || isScrollbarHovered(mouseX, mouseY);
        int barW = active ? 6 : 4;
        int barX = x + width - 1 - barW;
        int thumbH = Math.max(12, (contentH * contentH) / totalH);
        int maxScroll = totalH - contentH;
        int thumbY = contentY + (pixelScrollOffset * (contentH - thumbH)) / maxScroll;

        // Use themed colors
        g.fill(x + width - SCROLLBAR_W, contentY, x + width, y + height, com.sanhiruzu.ami.client.AMITheme.SCROLL_TRACK);
        g.fill(barX, thumbY, barX + barW, thumbY + thumbH,
                active ? com.sanhiruzu.ami.client.AMITheme.SCROLL_THUMB_ACTIVE : com.sanhiruzu.ami.client.AMITheme.SCROLL_THUMB);
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

    // =========================================================
    // Input handlers
    // =========================================================

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
                    } else if (!hr.toggleable()) {
                        return false;
                    }
                    return true;
                } else if (row instanceof ItemRow ir) {
                    int col = ((int) mouseX - x - 1) / CELL_SIZE;
                    if (col >= 0 && col < ir.items().size()) {
                        TreeNode node = ir.items().get(col);
                        if (node.isHighCardinality()) {
                            if (button == 0) {
                                node.setExpanded(!node.isExpanded());
                                cachedRows = null;
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

    private static boolean isTokenInjectClick(int button) {
        return button == 1 && Screen.hasControlDown();
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

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!scrollbarDragging || button != 0) return false;
        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        StickyContext stickyContext = stickyContext(rows);
        int contentH = contentHeight(stickyContext);
        int totalH = calcTotalHeight(rows);
        if (totalH <= contentH) return true;
        int thumbH = Math.max(10, (contentH * contentH) / totalH);
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
                    int col = ((int) mouseX - x - 1) / CELL_SIZE;
                    return itemCounter + net.minecraft.util.Mth.clamp(col, 0, ir.items().size());
                }
            }
            if (row instanceof ItemRow ir) itemCounter += ir.items().size();
            drawY += row.height();
        }
        return itemCounter;
    }
}
