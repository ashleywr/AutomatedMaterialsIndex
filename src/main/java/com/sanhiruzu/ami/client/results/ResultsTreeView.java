package com.sanhiruzu.ami.client.results;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

/**
 * "Suginami" rich-card list view.
 *
 * Each leaf row is 24 px tall:
 *   [X_PAD][16×16 icon][4px][Item Name  (line 1, y+2)   |  badge right-aligned]
 *                            [mod.name   (line 2, y+12)                        ]
 *
 * Group rows sit at the same 24 px row height and may show up to 3 colour-swatch
 * dots on the right when their children belong to a shared variant group.
 */
public class ResultsTreeView {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int ROW_HEIGHT   = 24;
    private static final int ICON_SIZE    = 16;
    private static final int X_PAD        = 6;
    private static final int INDENT       = 10;
    private static final int SCROLLBAR_W  = 5;
    private static final int HEADER_LABEL_H = 13; // height reserved for the optional pinned header

    // Cherry-blossom pink @ 30 % opacity
    private static final int HOVER_COLOR = 0x4DFFB7C5;

    // Swatch dots for variant collapsing
    private static final int SWATCH_SIZE = 5;
    private static final int SWATCH_GAP  = 2;
    private static final int MAX_SWATCHES = 3;

    // ── State ─────────────────────────────────────────────────────────────────
    private int x, y, width, height;
    private List<TreeNode> rootNodes = new ArrayList<>();

    /** Pixel scroll offset — increases as user scrolls down. */
    private int pixelScrollOffset = 0;

    /** Height of the scrollable content area (height minus any sticky header). Updated each render. */
    private int lastContentH = 0;

    private boolean scrollbarDragging = false;
    private int scrollbarDragStartY;
    private int scrollbarDragStartOffset;

    private List<Component> pendingTooltipLines = null;

    // ── Construction ──────────────────────────────────────────────────────────

    public ResultsTreeView(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setRootNodes(List<TreeNode> nodes) {
        this.rootNodes = nodes;
        this.pixelScrollOffset = 0;
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * @param sectionLabel Optional label drawn as a sticky header above the scroll region
     *                     (e.g. "Pinned & Discover"). Pass null to omit.
     */
    public void render(GuiGraphics g, int mouseX, int mouseY, boolean toolbarDropdownOpen,
                       String sectionLabel) {
        pendingTooltipLines = null;

        int topOffset = 0;
        if (sectionLabel != null) {
            g.drawString(Minecraft.getInstance().font,
                    sectionLabel, x + X_PAD, y + 3, 0xFF8888AA, false);
            topOffset = HEADER_LABEL_H;
        }

        if (rootNodes.isEmpty()) {
            g.drawString(Minecraft.getInstance().font,
                    Component.literal("No results"), x + X_PAD, y + topOffset + 6, 0xFFCCCCCC, false);
            return;
        }

        int contentH = height - topOffset;
        lastContentH = contentH;
        int totalH   = countAllNodes() * ROW_HEIGHT;
        clampScroll(totalH, contentH);

        g.enableScissor(x, y + topOffset, x + width, y + height);

        int[] rowCounter = {0};
        int effectiveMouseX = toolbarDropdownOpen ? -1 : mouseX;
        for (TreeNode node : rootNodes) {
            rowCounter[0] = renderNode(g, node, 0, rowCounter[0],
                    effectiveMouseX, mouseY, y + topOffset, contentH);
        }

        g.disableScissor();

        renderScrollbar(g, totalH, contentH, y + topOffset, mouseX, mouseY);

        if (!toolbarDropdownOpen && pendingTooltipLines != null) {
            g.renderComponentTooltip(Minecraft.getInstance().font, pendingTooltipLines, mouseX, mouseY);
        }
    }

    /** Backwards-compatible overload used when no section label is needed. */
    public void render(GuiGraphics g, int mouseX, int mouseY, boolean toolbarDropdownOpen) {
        render(g, mouseX, mouseY, toolbarDropdownOpen, null);
    }

    /**
     * Renders one node and its children. Returns the next row index to use.
     * @param originY  top of the scrollable content region (y + topOffset)
     * @param contentH height of the scrollable content region
     */
    private int renderNode(GuiGraphics g, TreeNode node, int depth, int rowIdx,
                           int mouseX, int mouseY, int originY, int contentH) {
        int drawY = originY - pixelScrollOffset + rowIdx * ROW_HEIGHT;

        // Only draw if even partially visible
        if (drawY + ROW_HEIGHT > originY && drawY < originY + contentH) {
            boolean hovered = !node.isLeaf()
                    ? isGroupRowHovered(mouseX, mouseY, drawY)
                    : isLeafRowHovered(mouseX, mouseY, drawY);

            if (hovered) {
                g.fill(x + 2, drawY, x + width - SCROLLBAR_W - 1, drawY + ROW_HEIGHT, HOVER_COLOR);
            }

            if (node.isLeaf()) {
                renderLeaf(g, node, depth, drawY, hovered);
            } else {
                renderGroup(g, node, depth, drawY, hovered);
            }
        }

        rowIdx++;

        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                rowIdx = renderNode(g, child, depth + 1, rowIdx, mouseX, mouseY, originY, contentH);
            }
        }

        return rowIdx;
    }

    // ── Leaf (rich card) ──────────────────────────────────────────────────────

    private void renderLeaf(GuiGraphics g, TreeNode node, int depth, int drawY, boolean hovered) {
        var font = Minecraft.getInstance().font;
        SearchNode entry = node.getEntry();

        int iconX = x + X_PAD + depth * INDENT;
        int iconY = drawY + (ROW_HEIGHT - ICON_SIZE) / 2;

        // Z-lift prevents dark-background clipping on 3D item models
        g.pose().pushPose();
        g.pose().translate(0, 0, 150);
        RendererRegistry.get(entry.type()).render(g, entry, iconX, iconY, ICON_SIZE);
        g.pose().popPose();

        int textX = iconX + ICON_SIZE + 4;
        int maxTextW = x + width - SCROLLBAR_W - 6 - textX;

        // Line 1 — item name
        String name = truncate(font, node.getLabel(), maxTextW - badgeWidth(font, entry));
        g.drawString(font, name, textX, drawY + 2, 0xFFDDDDDD, false);

        // Line 2 — mod namespace (subtitle)
        String modId = entry.id().getNamespace();
        g.drawString(font, modId, textX, drawY + 13, 0xFF888888, false);

        // Right-aligned JRPG badge
        renderBadge(g, font, entry, drawY);

        if (hovered) {
            pendingTooltipLines = buildTooltip(entry);
        }
    }

    // ── Badge ─────────────────────────────────────────────────────────────────

    private void renderBadge(GuiGraphics g, net.minecraft.client.gui.Font font,
                              SearchNode entry, int drawY) {
        String tier = entry.meta(SearchNodeKeys.TIER, "");
        String cap  = entry.meta(SearchNodeKeys.ESM_CAPACITY, "");

        String badge;
        int color;
        if (!tier.isEmpty()) {
            badge = "[" + tier + "]";
            color = tierColor(tier);
        } else if (!cap.isEmpty()) {
            badge = "[x" + cap + "]";
            color = 0xFF88DDFF;
        } else {
            return;
        }

        int bw = font.width(badge);
        g.drawString(font, badge, x + width - SCROLLBAR_W - 5 - bw, drawY + 7, color, false);
    }

    private int badgeWidth(net.minecraft.client.gui.Font font, SearchNode entry) {
        String tier = entry.meta(SearchNodeKeys.TIER, "");
        String cap  = entry.meta(SearchNodeKeys.ESM_CAPACITY, "");
        if (!tier.isEmpty()) return font.width("[" + tier + "]") + 5;
        if (!cap.isEmpty())  return font.width("[x" + cap + "]") + 5;
        return 0;
    }

    private static int tierColor(String tier) {
        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "wood"    -> 0xFFCCAA55;
            case "stone"   -> 0xFF999999;
            case "iron"    -> 0xFFCCCCCC;
            case "gold"    -> 0xFFFFDD44;
            case "diamond" -> 0xFF44EEFF;
            case "netherite" -> 0xFFAA77AA;
            default        -> 0xFFAAAA88;
        };
    }

    // ── Group header ──────────────────────────────────────────────────────────

    private void renderGroup(GuiGraphics g, TreeNode node, int depth, int drawY, boolean hovered) {
        var font = Minecraft.getInstance().font;
        int indent = depth * INDENT;

        String arrow = node.isExpanded() ? "▼ " : "▶ ";
        String label = arrow + node.getLabel() + " (" + node.getChildCount() + ")";
        g.drawString(font, label, x + X_PAD + indent, drawY + 7, 0xFFAAAA88, false);

        // Colour swatches from variant-grouped children
        renderSwatches(g, node, drawY);
    }

    /**
     * Draws up to MAX_SWATCHES coloured dots on the right side of a group row.
     * Dots are derived from the COLOR_BUCKET metadata of leaf descendants.
     */
    private void renderSwatches(GuiGraphics g, TreeNode group, int drawY) {
        List<Integer> swatchColors = collectSwatchColors(group, MAX_SWATCHES);
        if (swatchColors.isEmpty()) return;

        int rightEdge = x + width - SCROLLBAR_W - 6;
        int swatchY   = drawY + (ROW_HEIGHT - SWATCH_SIZE) / 2;
        int bx = rightEdge - swatchColors.size() * (SWATCH_SIZE + SWATCH_GAP) + SWATCH_GAP;

        for (int argb : swatchColors) {
            g.fill(bx, swatchY, bx + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | argb);
            bx += SWATCH_SIZE + SWATCH_GAP;
        }
    }

    private List<Integer> collectSwatchColors(TreeNode node, int max) {
        Set<String> seen = new LinkedHashSet<>();
        collectBuckets(node, seen, max);

        List<Integer> result = new ArrayList<>();
        for (String bucket : seen) {
            result.add(bucketToArgb(bucket));
        }
        return result;
    }

    private void collectBuckets(TreeNode node, Set<String> out, int max) {
        if (out.size() >= max) return;
        if (node.isLeaf()) {
            String bucket = node.getEntry().meta(SearchNodeKeys.COLOR_BUCKET, "");
            if (!bucket.isEmpty()) out.add(bucket);
        } else {
            for (TreeNode child : node.getChildren()) {
                collectBuckets(child, out, max);
                if (out.size() >= max) return;
            }
        }
    }

    /**
     * Maps a named colour bucket to a display ARGB int.
     * Fallback is mid-gray for unknown buckets.
     */
    private static int bucketToArgb(String bucket) {
        return switch (bucket.toLowerCase(Locale.ROOT)) {
            case "red"    -> 0xFFCC3333;
            case "orange" -> 0xFFDD7722;
            case "yellow" -> 0xFFDDCC22;
            case "lime",
                 "green"  -> 0xFF44AA44;
            case "cyan"   -> 0xFF22AACC;
            case "blue",
                 "light_blue" -> 0xFF3355DD;
            case "purple",
                 "magenta" -> 0xFF9933CC;
            case "pink"   -> 0xFFFFB7C5;
            case "white"  -> 0xFFEEEEEE;
            case "light_gray",
                 "silver" -> 0xFFAAAAAA;
            case "gray"   -> 0xFF666666;
            case "black"  -> 0xFF222222;
            case "brown"  -> 0xFF885533;
            default       -> 0xFF888888;
        };
    }

    // ── Hit-testing ───────────────────────────────────────────────────────────

    private boolean isLeafRowHovered(int mouseX, int mouseY, int drawY) {
        return mouseX >= x + 2 && mouseX < x + width - SCROLLBAR_W - 1
                && mouseY >= drawY && mouseY < drawY + ROW_HEIGHT;
    }

    private boolean isGroupRowHovered(int mouseX, int mouseY, int drawY) {
        return mouseX >= x + 2 && mouseX < x + width - SCROLLBAR_W - 1
                && mouseY >= drawY && mouseY < drawY + ROW_HEIGHT;
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    private List<Component> buildTooltip(SearchNode entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.displayName()));
        lines.add(Component.literal(entry.id().toString())
                .withStyle(s -> s.withColor(0x666666)));
        String tier = entry.meta(SearchNodeKeys.TIER, "");
        if (!tier.isEmpty()) {
            lines.add(Component.literal("Tier: " + tier)
                    .withStyle(s -> s.withColor(0xAAAA44)));
        }
        lines.add(Component.literal("Shift for details")
                .withStyle(s -> s.withColor(0x555555)));
        return lines;
    }

    // ── Scrollbar ─────────────────────────────────────────────────────────────

    private void renderScrollbar(GuiGraphics g, int totalH, int contentH, int originY,
                                 int mouseX, int mouseY) {
        if (totalH <= contentH) return;

        boolean active = scrollbarDragging || isScrollbarHovered(mouseX, mouseY, totalH, contentH, originY);
        int barW = active ? 5 : 3;
        int barX = x + width - 1 - barW;
        int thumbH = Math.max(10, (contentH * contentH) / totalH);
        int maxScroll = totalH - contentH;
        int thumbY = originY + (pixelScrollOffset * (contentH - thumbH)) / maxScroll;

        g.fill(barX, originY, barX + barW, originY + contentH, 0xFF2A2A2A);
        g.fill(barX, thumbY, barX + barW, thumbY + thumbH,
                active ? 0xFFAAAA88 : 0xFF666666);
    }

    private boolean isScrollbarHovered(int mouseX, int mouseY, int totalH, int contentH, int originY) {
        if (totalH <= contentH) return false;
        return mouseX >= x + width - 6 && mouseX < x + width - 1
                && mouseY >= originY && mouseY < originY + contentH;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clampScroll(int totalH, int contentH) {
        int maxScroll = Math.max(0, totalH - contentH);
        pixelScrollOffset = Math.max(0, Math.min(maxScroll, pixelScrollOffset));
    }

    private int countAllNodes() {
        int count = 0;
        for (TreeNode n : rootNodes) count += countNode(n);
        return count;
    }

    private int countNode(TreeNode node) {
        int count = 1;
        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) count += countNode(child);
        }
        return count;
    }

    private static String truncate(net.minecraft.client.gui.Font font, String text, int maxW) {
        if (maxW <= 0) return "";
        while (font.width(text) > maxW && text.length() > 1) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Which row is under the cursor?
        int targetRow = (int) (mouseY - y + pixelScrollOffset) / ROW_HEIGHT;
        if (targetRow < 0) return false;

        int[] counter = {0};
        for (TreeNode node : rootNodes) {
            if (handleNodeClick(node, targetRow, counter)) return true;
        }
        return false;
    }

    /** DFS click handler. Returns true when the target row was found and handled. */
    private boolean handleNodeClick(TreeNode node, int targetRow, int[] counter) {
        if (counter[0] == targetRow) {
            if (!node.isLeaf()) {
                node.setExpanded(!node.isExpanded());
            }
            return true;
        }
        counter[0]++;

        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                if (handleNodeClick(child, targetRow, counter)) return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int contentH = lastContentH > 0 ? lastContentH : height;
        int totalH = countAllNodes() * ROW_HEIGHT;
        int maxScroll = Math.max(0, totalH - contentH);
        pixelScrollOffset = Math.max(0, Math.min(maxScroll,
                (int) (pixelScrollOffset - delta * ROW_HEIGHT)));
        return true;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int contentH = lastContentH > 0 ? lastContentH : height;
        int totalH = countAllNodes() * ROW_HEIGHT;
        if (!isScrollbarHovered((int) mouseX, (int) mouseY, totalH, contentH, y)) return false;
        scrollbarDragging = true;
        scrollbarDragStartY = (int) mouseY;
        scrollbarDragStartOffset = pixelScrollOffset;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!scrollbarDragging || button != 0) return false;

        int contentH = lastContentH > 0 ? lastContentH : height;
        int totalH = countAllNodes() * ROW_HEIGHT;
        if (totalH <= contentH) return true;

        int thumbH = Math.max(10, (contentH * contentH) / totalH);
        int dragRange = contentH - thumbH;
        if (dragRange <= 0) return true;

        int dy = (int) mouseY - scrollbarDragStartY;
        int delta = (int) Math.round((double) dy * (totalH - contentH) / dragRange);
        pixelScrollOffset = Math.max(0, Math.min(totalH - contentH, scrollbarDragStartOffset + delta));
        return true;
    }

    public void stopScrollbarDrag() {
        scrollbarDragging = false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
