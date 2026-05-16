package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

import java.util.*;

/**
 * "Suginami" rich-card list view.
 *
 * Each leaf row is 18 px tall:
 *   [X_PAD][16×16 icon][4px][Item Name  (line 1, y+1)   |  badge right-aligned]
 *                            [mod.name   (line 2, y+10)                        ]
 *
 * Group rows sit at the same 24 px row height and may show up to 3 colour-swatch
 * dots on the right when their children belong to a shared variant group.
 */
public class ResultsTreeView {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int INDENT       = 12;
    private static final int SCROLLBAR_W  = 5;
    private static final int HEADER_LABEL_H = 13; // height reserved for the optional pinned header

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
                       Component sectionLabel) {
        pendingTooltipLines = null;

        int topOffset = 0;
        if (sectionLabel != null) {
            g.drawString(Minecraft.getInstance().font,
                    sectionLabel, x + AMITheme.GLOBAL_PADDING, y + 3, AMITheme.TEXT_HEADER, false);
            topOffset = HEADER_LABEL_H;
        }

        if (rootNodes.isEmpty()) {
            g.drawString(Minecraft.getInstance().font,
                    Component.translatable("ami.gui.search.empty"), x + AMITheme.GLOBAL_PADDING, y + topOffset + 6, AMITheme.TEXT_SUBTLE, false);
            return;
        }

        int contentH = height - topOffset;
        lastContentH = contentH;
        int totalH   = countAllNodes() * AMITheme.ROW_HEIGHT;
        clampScroll(totalH, contentH);

        g.enableScissor(x + 2, y + topOffset, x + width - 2, y + height - 2);

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
        render(g, mouseX, mouseY, toolbarDropdownOpen, (Component) null);
    }

    /**
     * Renders one node and its children. Returns the next row index to use.
     * @param originY  top of the scrollable content region (y + topOffset)
     * @param contentH height of the scrollable content region
     */
    private int renderNode(GuiGraphics g, TreeNode node, int depth, int rowIdx,
                           int mouseX, int mouseY, int originY, int contentH) {
        int drawY = originY - pixelScrollOffset + rowIdx * AMITheme.ROW_HEIGHT;

        // Only draw if even partially visible
        if (drawY + AMITheme.ROW_HEIGHT > originY && drawY < originY + contentH) {
            boolean hovered = !node.isLeaf()
                    ? isGroupRowHovered(mouseX, mouseY, drawY)
                    : isLeafRowHovered(mouseX, mouseY, drawY);

            if (rowIdx % 2 == 0) {
                g.fill(x, drawY, x + width - SCROLLBAR_W, drawY + AMITheme.ROW_HEIGHT, 0x15000000);
            }

            if (!node.isLeaf()) {
                g.fill(x, drawY, x + width - SCROLLBAR_W, drawY + AMITheme.ROW_HEIGHT, AMITheme.GROUP_HEADER_BG);
            }

            if (hovered) {
                g.fill(x, drawY, x + width - SCROLLBAR_W, drawY + AMITheme.ROW_HEIGHT, AMITheme.ENTRY_HOVER);
            }

            if (node.isLeaf()) {
                renderLeaf(g, node, depth, drawY, hovered);
            } else {
                renderGroup(g, node, depth, drawY, hovered);
            }

            // 1px separator at the bottom of every row
            g.fill(x + 3, drawY + AMITheme.ROW_HEIGHT - 1,
                   x + width - SCROLLBAR_W - 3, drawY + AMITheme.ROW_HEIGHT,
                   AMITheme.ROW_SEPARATOR);
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

        int iconX = x + AMITheme.GLOBAL_PADDING + depth * INDENT;
        int iconY = drawY + (AMITheme.ROW_HEIGHT - 16) / 2;

        // Z-lift prevents dark-background clipping on 3D item models
        g.pose().pushPose();
        g.pose().translate(0, 0, 150);
        RendererRegistry.get(entry.type()).render(g, entry, iconX, iconY, AMITheme.ICON_SIZE);
        g.pose().popPose();

        int textX = iconX + AMITheme.ICON_SIZE + 4;
        int maxTextW = x + width - SCROLLBAR_W - 6 - textX;

        // Right-aligned badges
        int rightEdge = x + width - SCROLLBAR_W - 5;
        int badgeW = badgeWidth(font, entry);

        // Subtitle text (computed early so we know whether it's a 1- or 2-line row)
        String subtitle = RowFieldConfig.buildSubtitle(entry);
        boolean hasSubtitle = !subtitle.isEmpty();

        // Vertically centre the text block in the row
        int lineH = font.lineHeight;
        int textY1 = drawY + (AMITheme.ROW_HEIGHT - lineH) / 2;
        int textY2 = textY1 + lineH + 1;

        // Line 1 — item name
        String name = truncate(font, node.getLabel(), maxTextW - badgeW);
        g.drawString(font, name, textX, textY1, AMITheme.TEXT_PRIMARY, true);

        // Line 2 — subtitle right-aligned against the scrollbar edge
        if (hasSubtitle) {
            String subtitleTrunc = truncate(font, subtitle, maxTextW);
            int subtitleX = Math.max(textX, rightEdge - font.width(subtitleTrunc));
            g.drawString(font, subtitleTrunc, subtitleX, textY2, AMITheme.TEXT_SUBTLE, false);
        }

        renderBadges(g, font, entry, drawY, rightEdge);

        if (hovered) {
            pendingTooltipLines = buildTooltip(entry);
        }
    }

    // ── Badges ────────────────────────────────────────────────────────────────

    private void renderBadges(GuiGraphics g, net.minecraft.client.gui.Font font,
                              SearchNode entry, int drawY, int rightEdge) {
        int currentX = rightEdge;

        // Render Tool Requirement Badge
        String reqToolStr = entry.meta(SearchNodeKeys.REQUIRED_TOOL, "");
        if (!reqToolStr.isEmpty()) {
            ResourceLocation toolId = ResourceLocation.tryParse(reqToolStr);
            if (toolId != null) {
                Item toolItem = BuiltInRegistries.ITEM.get(toolId);
                if (toolItem != null && toolItem != net.minecraft.world.item.Items.AIR) {
                    ItemStack toolStack = new ItemStack(toolItem);
                    int scaledIconSize = 8;
                    currentX -= scaledIconSize;
                    
                    int badgeY = drawY + 1; // Align with top text line
                    
                    g.pose().pushPose();
                    g.pose().translate(currentX, badgeY, 150);
                    g.pose().scale(0.5f, 0.5f, 1.0f);
                    g.renderItem(toolStack, 0, 0);
                    g.pose().popPose();
                    
                    currentX -= 5; // Padding before next badge
                }
            }
        }

        // Render ESM Capacity Badge
        String cap  = entry.meta(SearchNodeKeys.ESM_CAPACITY, "");
        if (!cap.isEmpty()) {
            Component badge = Component.translatable("ami.gui.badge.storage", cap);
            int bw = font.width(badge);
            currentX -= bw;
            g.drawString(font, badge, currentX, drawY + 5, AMITheme.TEXT_SUBTLE, false);
        }
    }

    private int badgeWidth(net.minecraft.client.gui.Font font, SearchNode entry) {
        int w = 0;
        String reqToolStr = entry.meta(SearchNodeKeys.REQUIRED_TOOL, "");
        if (!reqToolStr.isEmpty()) {
            w += 8 + 5; // 8px for scaled icon, 5px padding
        }
        
        String cap  = entry.meta(SearchNodeKeys.ESM_CAPACITY, "");
        if (!cap.isEmpty()) {
            w += font.width(Component.translatable("ami.gui.badge.storage", cap)) + 5;
        }
        return w;
    }

    // ── Group header ──────────────────────────────────────────────────────────

    private void renderGroup(GuiGraphics g, TreeNode node, int depth, int drawY, boolean hovered) {
        var font = Minecraft.getInstance().font;
        int indent = depth * INDENT;
        int rowX = x + AMITheme.GLOBAL_PADDING + indent;

        // Expansion Toggle
        String arrow = node.isExpanded() ? "▼" : "▶";
        int caretY = drawY + (AMITheme.ROW_HEIGHT - font.lineHeight) / 2;
        g.drawString(font, arrow, rowX, caretY, AMITheme.TEXT_HEADER, false);

        // Stacked Icon Effect (Representative Item)
        if (!node.getChildren().isEmpty()) {
            // Find first leaf child to use for the icon
            SearchNode representative = null;
            for (TreeNode child : node.getChildren()) {
                if (child.isLeaf()) {
                    representative = child.getEntry();
                    break;
                }
            }

            if (representative != null) {
                net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(representative.id());
                ItemStack icon = new ItemStack(item);
                int iconX = rowX + 12;
                int iconY = drawY + 1;
                
                // Base Icon
                g.renderItem(icon, iconX, iconY);
                
                // Stacked Icon (Darkened & Offset)
                g.pose().pushPose();
                g.pose().translate(2, -2, 100);
                g.fill(iconX, iconY, iconX + 16, iconY + 16, 0x44000000);
                g.renderItem(icon, iconX, iconY);
                g.pose().popPose();
            }
        }

        // Count Badge (computed first so we know the available label width)
        String badge = "[" + node.getChildren().size() + " " + node.getLabel() + "]";
        int badgeW = font.width(badge);
        int badgeX = x + width - SCROLLBAR_W - badgeW - 5;

        // Label (truncated to prevent overlap with the badge)
        int labelMaxW = badgeX - (rowX + 32) - 4;
        String label = truncate(font, node.getLabel(), Math.max(0, labelMaxW));
        g.drawString(font, label, rowX + 32, drawY + 5, AMITheme.TEXT_HEADER, false);

        g.drawString(font, badge, badgeX, drawY + 5, AMITheme.TEXT_SUBTLE, false);
    }

    /**
     * Draws up to MAX_SWATCHES coloured dots as a subtitle for group rows.
     */
    private void renderSwatches(GuiGraphics g, TreeNode group, int drawY, int depth) {
        List<Integer> swatchColors = collectSwatchColors(group, MAX_SWATCHES);
        if (swatchColors.isEmpty()) return;

        int bx = x + AMITheme.GLOBAL_PADDING + depth * INDENT + 10; // offset from arrow
        int swatchY = drawY + 2;

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
        return mouseX >= x && mouseX < x + width - SCROLLBAR_W
                && mouseY >= drawY && mouseY < drawY + AMITheme.ROW_HEIGHT;
    }

    private boolean isGroupRowHovered(int mouseX, int mouseY, int drawY) {
        return mouseX >= x && mouseX < x + width - SCROLLBAR_W
                && mouseY >= drawY && mouseY < drawY + AMITheme.ROW_HEIGHT;
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    private List<Component> buildTooltip(SearchNode entry) {
        if (Screen.hasControlDown()) {
            return DebugTooltip.build(entry);
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.displayName()));
        lines.add(Component.literal(entry.id().toString())
                .withStyle(s -> s.withColor(0x666666)));
        lines.add(Component.literal("§8Hold Ctrl for AMI debug info"));
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

        g.fill(barX, originY, barX + barW, originY + contentH, AMITheme.SCROLL_TRACK);
        g.fill(barX, thumbY, barX + barW, thumbY + thumbH,
                active ? AMITheme.SCROLL_THUMB_ACTIVE : AMITheme.SCROLL_THUMB);
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
        return font.plainSubstrByWidth(text, maxW);
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Which row is under the cursor?
        int targetRow = (int) (mouseY - y + pixelScrollOffset) / AMITheme.ROW_HEIGHT;
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
        int totalH = countAllNodes() * AMITheme.ROW_HEIGHT;
        int maxScroll = Math.max(0, totalH - contentH);
        pixelScrollOffset = Math.max(0, Math.min(maxScroll,
                (int) (pixelScrollOffset - delta * AMITheme.ROW_HEIGHT)));
        return true;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int contentH = lastContentH > 0 ? lastContentH : height;
        int totalH = countAllNodes() * AMITheme.ROW_HEIGHT;
        if (!isScrollbarHovered((int) mouseX, (int) mouseY, totalH, contentH, y)) return false;
        scrollbarDragging = true;
        scrollbarDragStartY = (int) mouseY;
        scrollbarDragStartOffset = pixelScrollOffset;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!scrollbarDragging || button != 0) return false;

        int contentH = lastContentH > 0 ? lastContentH : height;
        int totalH = countAllNodes() * AMITheme.ROW_HEIGHT;
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
