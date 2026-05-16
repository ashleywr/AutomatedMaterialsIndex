package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Renders ITEM-type SearchNodes as a 3D item icon grid (18×18 cells).
 * Group nodes from the tree processor become collapsible section headers.
 * Non-ITEM leaf nodes are silently skipped.
 */
public class ItemGridView {
    private static final int CELL_SIZE  = 18;
    private static final int HEADER_H   = 12;
    private static final int SCROLLBAR_W = 5;

    private static final Map<ResourceLocation, ItemStack> stackCache = new HashMap<>();

    private int x, y, width, height;
    private List<TreeNode> rootNodes = new ArrayList<>();
    private int pixelScrollOffset = 0;

    private boolean scrollbarDragging = false;
    private int scrollbarDragStartY;
    private int scrollbarDragStartOffset;

    /** Set by UniversalResultsPanel to route clicks to the recipe bridge. */
    private BiConsumer<SearchNode, Integer> onItemClick;

    // Deferred tooltips — built during render, drawn after scissor is popped
    private ItemStack pendingTooltip = null;
    private List<Component> pendingTextTooltip = null;
    private Optional<TooltipComponent> pendingTooltipImage = Optional.empty();

    // Virtual row cache — rebuilt whenever rootNodes changes or a group is toggled
    private List<VirtualRow> cachedRows = null;
    private int cachedCols = -1;

    // =========================================================
    // Virtual row types
    // =========================================================

    private sealed interface VirtualRow permits HeaderRow, ItemRow {
        int height();
    }

    private record HeaderRow(TreeNode node, int itemCount) implements VirtualRow {
        public int height() { return HEADER_H; }
    }

    private record ItemRow(List<TreeNode> items) implements VirtualRow {
        public int height() { return CELL_SIZE; }
    }

    // =========================================================
    // Public API
    // =========================================================

    public ItemGridView(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public void setRootNodes(List<TreeNode> nodes) {
        this.rootNodes = nodes;
        this.pixelScrollOffset = 0;
        this.cachedRows = null;
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        this.cachedRows = null; // cols may have changed
    }

    public void setItemClickCallback(BiConsumer<SearchNode, Integer> callback) {
        this.onItemClick = callback;
    }

    public static void clearStackCache() {
        stackCache.clear();
    }

    // =========================================================
    // Rendering
    // =========================================================

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        pendingTooltip = null;
        pendingTextTooltip = null;
        pendingTooltipImage = Optional.empty();

        if (rootNodes.isEmpty()) {
            g.drawString(Minecraft.getInstance().font,
                    "No results", x + 4, y + 4, 0xFFCCCCCC, false);
            return;
        }

        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        int totalH = calcTotalHeight(rows);

        g.enableScissor(x, y, x + width, y + height);

        int drawY = y - pixelScrollOffset;
        for (VirtualRow row : rows) {
            int rowBottom = drawY + row.height();
            if (rowBottom > y && drawY < y + height) {
                if (row instanceof HeaderRow hr) {
                    renderHeader(g, hr, drawY, mouseX, mouseY);
                } else if (row instanceof ItemRow ir) {
                    renderItemRow(g, ir, drawY, mouseX, mouseY);
                }
            }
            drawY += row.height();
        }

        g.disableScissor();

        renderScrollbar(g, totalH, mouseX, mouseY);

        var font = Minecraft.getInstance().font;
        if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
            g.renderTooltip(font, pendingTooltip, mouseX, mouseY);
        } else if (pendingTextTooltip != null) {
            g.renderTooltip(font, pendingTextTooltip, pendingTooltipImage, mouseX, mouseY);
        }
    }

    private void renderHeader(GuiGraphics g, HeaderRow hr, int drawY, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width - SCROLLBAR_W
                && mouseY >= drawY && mouseY < drawY + HEADER_H;
        if (hovered) {
            g.fill(x, drawY, x + width - SCROLLBAR_W, drawY + HEADER_H, 0xFF282820);
        }
        String arrow = hr.node().isExpanded() ? "▼ " : "▶ ";
        String label = arrow + hr.node().getLabel() + " (" + hr.itemCount() + ")";
        g.drawString(Minecraft.getInstance().font, label, x + 4, drawY + 2, 0xFFAAAA88, false);
    }

    private void renderItemRow(GuiGraphics g, ItemRow ir, int drawY, int mouseX, int mouseY) {
        for (int i = 0; i < ir.items().size(); i++) {
            int cellX = x + 1 + i * CELL_SIZE;
            int cellY = drawY;

            TreeNode node = ir.items().get(i);
            SearchNode entry = node.getEntry();
            if (entry == null) continue;

            boolean hovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= cellY && mouseY < cellY + CELL_SIZE;
            if (hovered) {
                g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, 0xFF3A3A3A);
                if (Screen.hasControlDown()) {
                    pendingTextTooltip = DebugTooltip.build(entry);
                    pendingTooltipImage = Optional.empty();
                } else if (entry.type() == NodeType.ITEM) {
                    pendingTooltip = resolveStack(entry);
                } else {
                    var renderer = RendererRegistry.get(entry.type());
                    pendingTextTooltip = renderer.getTooltip(entry);
                    pendingTooltipImage = renderer.getTooltipImage(entry);
                }
            }

            if (entry.type() == NodeType.ITEM) {
                ItemStack stack = resolveStack(entry);
                if (!stack.isEmpty()) g.renderItem(stack, cellX + 1, cellY + 1);
            } else {
                RendererRegistry.get(entry.type()).render(g, entry, cellX + 1, cellY + 1, 16);
            }
        }
    }

    private void primeIconCache(GuiGraphics g, List<VirtualRow> rows) {
        List<Map.Entry<ResourceLocation, ItemStack>> uncached = new ArrayList<>();
        int drawY = y - pixelScrollOffset;
        for (VirtualRow row : rows) {
            if (drawY + row.height() > y && drawY < y + height && row instanceof ItemRow ir) {
                for (TreeNode node : ir.items()) {
                    SearchNode entry = node.getEntry();
                    if (entry != null && entry.type() == NodeType.ITEM
                            && !ItemIconCache.isCached(entry.id())) {
                        ItemStack stack = resolveStack(entry);
                        if (!stack.isEmpty()) uncached.add(Map.entry(entry.id(), stack));
                    }
                }
            }
            drawY += row.height();
        }
        if (!uncached.isEmpty()) ItemIconCache.primeVisible(g, uncached);
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

        boolean hasGroups = rootNodes.stream().anyMatch(n -> !n.isLeaf());
        if (hasGroups) {
            for (TreeNode root : rootNodes) {
                if (!root.isLeaf()) {
                    addGroupRows(root, cols, rows);
                } else {
                    rows.add(new ItemRow(List.of(root)));
                }
            }
        } else {
            // Flat list of leaves — pack directly into item rows
            List<TreeNode> items = rootNodes.stream()
                    .filter(TreeNode::isLeaf)
                    .collect(Collectors.toList());
            packIntoRows(items, cols, rows);
        }

        return rows;
    }

    private void addGroupRows(TreeNode group, int cols, List<VirtualRow> out) {
        List<TreeNode> items = new ArrayList<>();
        collectItemLeaves(group, items);
        if (items.isEmpty()) return;

        out.add(new HeaderRow(group, items.size()));
        if (!group.isExpanded()) return;

        packIntoRows(items, cols, out);
    }

    private void packIntoRows(List<TreeNode> items, int cols, List<VirtualRow> out) {
        for (int i = 0; i < items.size(); i += cols) {
            out.add(new ItemRow(new ArrayList<>(items.subList(i, Math.min(i + cols, items.size())))));
        }
    }

    private void collectItemLeaves(TreeNode node, List<TreeNode> out) {
        if (node.isLeaf()) {
            out.add(node);
        } else {
            for (TreeNode child : node.getChildren()) {
                collectItemLeaves(child, out);
            }
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

    // =========================================================
    // ItemStack resolution
    // =========================================================

    private static ItemStack resolveStack(SearchNode node) {
        if (node == null) return ItemStack.EMPTY;
        return stackCache.computeIfAbsent(node.id(),
                id -> BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY));
    }

    // =========================================================
    // Scrollbar
    // =========================================================

    private void renderScrollbar(GuiGraphics g, int totalH, int mouseX, int mouseY) {
        if (totalH <= height) return;
        boolean active = scrollbarDragging || isScrollbarHovered(mouseX, mouseY);
        int barW = active ? 5 : 3;
        int barX = x + width - 1 - barW;
        int thumbH = Math.max(10, (height * height) / totalH);
        int maxScroll = totalH - height;
        int thumbY = y + (pixelScrollOffset * (height - thumbH)) / maxScroll;

        g.fill(barX, y, barX + barW, y + height, 0xFF2A2A2A);
        g.fill(barX, thumbY, barX + barW, thumbY + thumbH,
                active ? 0xFFAAAA88 : 0xFF666666);
    }

    private boolean isScrollbarHovered(int mouseX, int mouseY) {
        int cols = computeCols();
        int totalH = calcTotalHeight(getVirtualRows(cols));
        if (totalH <= height) return false;
        return mouseX >= x + width - 6 && mouseX < x + width - 1
                && mouseY >= y && mouseY < y + height;
    }

    // =========================================================
    // Input handlers
    // =========================================================

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;

        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);

        int drawY = y - pixelScrollOffset;
        for (VirtualRow row : rows) {
            if (mouseY >= drawY && mouseY < drawY + row.height()) {
                if (row instanceof HeaderRow hr) {
                    hr.node().setExpanded(!hr.node().isExpanded());
                    cachedRows = null; // rebuild
                    return true;
                } else if (row instanceof ItemRow ir) {
                    int col = ((int) mouseX - x - 1) / CELL_SIZE;
                    if (col >= 0 && col < ir.items().size()) {
                        SearchNode node = ir.items().get(col).getEntry();
                        if (node != null && onItemClick != null) {
                            onItemClick.accept(node, button);
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
        int totalH = calcTotalHeight(getVirtualRows(cols));
        int maxScroll = Math.max(0, totalH - height);
        pixelScrollOffset = Math.max(0, Math.min(maxScroll,
                (int) (pixelScrollOffset - delta * CELL_SIZE)));
        return true;
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
        int totalH = calcTotalHeight(getVirtualRows(cols));
        if (totalH <= height) return true;
        int thumbH = Math.max(10, (height * height) / totalH);
        int dragRange = height - thumbH;
        if (dragRange <= 0) return true;
        int dyPx = (int) mouseY - scrollbarDragStartY;
        int offsetDelta = (int) Math.round((double) dyPx * (totalH - height) / dragRange);
        pixelScrollOffset = Math.max(0, Math.min(totalH - height, scrollbarDragStartOffset + offsetDelta));
        return true;
    }

    public void stopScrollbarDrag() {
        scrollbarDragging = false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
