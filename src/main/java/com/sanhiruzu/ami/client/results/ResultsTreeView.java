package com.sanhiruzu.ami.client.results;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ResultsTreeView {
    private static final int ROW_HEIGHT = 11;
    private static final int SWATCH_SIZE = 6;
    private static final int SWATCH_GAP = 3;
    private static final int INDENT = 10;
    private static final int DIM_BADGE = 4;

    private int x, y, width, height;
    private List<TreeNode> rootNodes = new ArrayList<>();
    private int scrollOffset = 0;

    // Scrollbar drag state
    private boolean scrollbarDragging = false;
    private int scrollbarDragStartY;
    private int scrollbarDragStartOffset;

    // Deferred tooltip
    private List<Component> pendingTooltipLines = null;

    public ResultsTreeView(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setRootNodes(List<TreeNode> nodes) {
        this.rootNodes = nodes;
        this.scrollOffset = 0;
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, boolean toolbarDropdownOpen) {
        pendingTooltipLines = null;

        if (rootNodes.isEmpty()) {
            g.drawString(Minecraft.getInstance().font, Component.literal("No results"),
                    x + 4, y + 4, 0xFFCCCCCC, false);
            return;
        }

        var font = Minecraft.getInstance().font;
        int contentH = height;
        int visRows = Math.max(1, contentH / ROW_HEIGHT);

        int row = 0;
        int visibleRow = 0;

        for (TreeNode node : rootNodes) {
            visibleRow = renderNode(g, font, node, 0, row, scrollOffset, visRows,
                    toolbarDropdownOpen ? -1 : mouseX, mouseY);
            row = visibleRow;
        }

        renderScrollBar(g, mouseX, mouseY);

        // Only show tooltips if no dropdown is open
        if (!toolbarDropdownOpen && pendingTooltipLines != null) {
            g.renderComponentTooltip(Minecraft.getInstance().font, pendingTooltipLines, mouseX, mouseY);
        }
    }

    private int renderNode(GuiGraphics g, net.minecraft.client.gui.Font font, TreeNode node, int depth,
                           int row, int scrollOffset, int visRows, int mouseX, int mouseY) {
        int contentY = y;
        int indent = depth * INDENT;

        // Render this node if visible
        if (row >= scrollOffset && row < scrollOffset + visRows) {
            int drawY = contentY + (row - scrollOffset) * ROW_HEIGHT;
            boolean hovered = isRowHovered(mouseX, mouseY, drawY);

            if (hovered) {
                g.fill(x + 2, drawY, x + width - 6, drawY + ROW_HEIGHT, 0xFF333333);
            }

            if (node.isLeaf()) {
                // Leaf entry: draw swatch + name
                int swatchY = drawY + (ROW_HEIGHT - SWATCH_SIZE) / 2;
                g.fill(x + SWATCH_GAP + indent, swatchY,
                        x + SWATCH_GAP + indent + SWATCH_SIZE, swatchY + SWATCH_SIZE, node.getEntry().color());

                String name = node.getLabel();
                int maxW = width - indent - SWATCH_GAP - SWATCH_SIZE - SWATCH_GAP - DIM_BADGE - 6;
                while (font.width(name) > maxW && name.length() > 1) {
                    name = name.substring(0, name.length() - 1);
                }

                g.drawString(font, name, x + SWATCH_GAP + indent + SWATCH_SIZE + SWATCH_GAP, drawY + 2,
                        0xFFCCCCCC, false);

                // Dimension badge for non-overworld biomes
                if (node.getEntry().type().name().equals("BIOME")) {
                    String dim = node.getEntry().meta(com.sanhiruzu.ami.index.SearchNodeKeys.DIMENSION, "overworld");
                    if (!"overworld".equals(dim)) {
                        int badgeColor = "nether".equals(dim) ? 0xFFB87333 : 0xFF7CB9FF;
                        int badgeX = x + width - DIM_BADGE - 6;
                        int badgeY = drawY + (ROW_HEIGHT - DIM_BADGE) / 2;
                        g.fill(badgeX, badgeY, badgeX + DIM_BADGE, badgeY + DIM_BADGE, badgeColor);
                    }
                }

                if (hovered) {
                    pendingTooltipLines = buildTooltip(node.getEntry());
                }
            } else {
                // Group node: draw collapse arrow + label
                String arrow = node.isExpanded() ? "▼ " : "▶ ";
                String label = arrow + node.getLabel() + " (" + node.getChildCount() + ")";
                g.drawString(font, label, x + 4 + indent, drawY + 2, 0xFFAAAA88, false);
            }
        }
        row++;

        // Render children if expanded
        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                row = renderNode(g, font, child, depth + 1, row, scrollOffset, visRows, mouseX, mouseY);
            }
        }

        return row;
    }

    private boolean isRowHovered(int mouseX, int mouseY, int drawY) {
        return mouseX >= x + 2 && mouseX < x + width - 5
                && mouseY >= drawY && mouseY < drawY + ROW_HEIGHT;
    }

    private List<Component> buildTooltip(com.sanhiruzu.ami.index.SearchNode entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.displayName()));
        lines.add(Component.literal(entry.id().toString()).withStyle(s -> s.withColor(0x666666)));
        lines.add(Component.literal("Shift for details").withStyle(s -> s.withColor(0x555555)));
        return lines;
    }

    private void renderScrollBar(GuiGraphics g, int mouseX, int mouseY) {
        int total = countAllNodes();
        int contentH = height;
        int visible = visibleRowCount(contentH);
        if (total <= visible) return;

        boolean active = scrollbarDragging || isScrollbarHovered(mouseX, mouseY);
        int barW = active ? 5 : 3;
        int barX = x + width - 1 - barW;
        int thumbH = Math.max(10, (visible * contentH) / total);
        int thumbY = y + (scrollOffset * (contentH - thumbH)) / (total - visible);

        g.fill(barX, y, barX + barW, y + contentH, 0xFF2A2A2A);
        g.fill(barX, thumbY, barX + barW, thumbY + thumbH,
                active ? 0xFFAAAA88 : 0xFF666666);
    }

    private boolean isScrollbarHovered(int mouseX, int mouseY) {
        int contentH = height;
        if (countAllNodes() <= visibleRowCount(contentH)) return false;
        return mouseX >= x + width - 6 && mouseX < x + width - 1
                && mouseY >= y && mouseY < y + contentH;
    }

    private int countAllNodes() {
        int count = 0;
        for (TreeNode node : rootNodes) {
            count += countNode(node);
        }
        return count;
    }

    private int countNode(TreeNode node) {
        int count = 1; // this node
        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                count += countNode(child);
            }
        }
        return count;
    }

    private int visibleRowCount(int contentH) {
        return Math.max(1, contentH / ROW_HEIGHT);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int visRows = Math.max(1, height / ROW_HEIGHT);
        int row = 0;
        for (TreeNode node : rootNodes) {
            int result = handleNodeClick(node, 0, row, (int) mouseX, (int) mouseY, scrollOffset, visRows, y);
            if (result == Integer.MIN_VALUE) return true;
            row = result;
        }
        return false;
    }

    private int handleNodeClick(TreeNode node, int depth, int row, int mouseX, int mouseY, int scrollOffset, int visRows, int contentY) {
        if (row >= scrollOffset && row < scrollOffset + visRows) {
            int drawY = contentY + (row - scrollOffset) * ROW_HEIGHT;
            if (isRowHovered(mouseX, mouseY, drawY)) {
                if (!node.isLeaf()) {
                    node.setExpanded(!node.isExpanded());
                }
                return Integer.MIN_VALUE;
            }
        }
        row++;

        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                int result = handleNodeClick(child, depth + 1, row, mouseX, mouseY, scrollOffset, visRows, contentY);
                if (result == Integer.MIN_VALUE) return Integer.MIN_VALUE;
                row = result;
            }
        }
        return row;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int contentH = height;
        int maxScroll = Math.max(0, countAllNodes() - visibleRowCount(contentH));
        scrollOffset = Math.max(0, Math.min(maxScroll, (int) (scrollOffset - scrollDelta)));
        return true;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (button != 0 || !isScrollbarHovered((int) mouseX, (int) mouseY)) return false;
        scrollbarDragging = true;
        scrollbarDragStartY = (int) mouseY;
        scrollbarDragStartOffset = scrollOffset;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!scrollbarDragging || button != 0) return false;

        int contentH = height;
        int total = countAllNodes();
        int visible = visibleRowCount(contentH);
        if (total <= visible) return true;

        int thumbH = Math.max(10, (visible * contentH) / total);
        int dragRange = contentH - thumbH;
        if (dragRange <= 0) return true;

        int dy = (int) mouseY - scrollbarDragStartY;
        int offsetDelta = (int) Math.round((double) dy * (total - visible) / dragRange);
        scrollOffset = Math.max(0, Math.min(total - visible, scrollbarDragStartOffset + offsetDelta));
        return true;
    }

    public void stopScrollbarDrag() {
        scrollbarDragging = false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
