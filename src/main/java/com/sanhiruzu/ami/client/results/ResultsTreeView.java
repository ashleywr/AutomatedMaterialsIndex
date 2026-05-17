package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.util.AmiColors;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
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
    
    private static final int TEXT_HIGHLIGHT = 0xFF55FFFF; // Aqua

    // Recomputed each frame — 0.75× when guiScale ≥ 3, otherwise 1.0×.
    private float currentLabelScale = 1.0f;

    private static float computeLabelScale() {
        return Minecraft.getInstance().getWindow().getGuiScale() >= 3.0 ? 0.75f : 1.0f;
    }
    
    // ── State ─────────────────────────────────────────────────────────────────
    private int x, y, width, height;
    private List<TreeNode> rootNodes = new ArrayList<>();

    /** Cached representative SearchNode per group TreeNode; cleared whenever rootNodes changes. */
    private final Map<TreeNode, SearchNode> representativeCache = new HashMap<>();

    /** Pixel scroll offset — increases as user scrolls down. */
    private int pixelScrollOffset = 0;

    /** Height of the scrollable content area (height minus any sticky header). Updated each render. */
    private int lastContentH = 0;

    private boolean scrollbarDragging = false;
    private int scrollbarDragStartY;
    private int scrollbarDragStartOffset;

    private List<Component> pendingTooltipLines = null;
    private Optional<TooltipComponent> pendingTooltipImage = Optional.empty();
    private ItemStack pendingItemStack = null;

    private java.util.function.Consumer<String> onModClick = null;

    // ── Construction ──────────────────────────────────────────────────────────

    public ResultsTreeView(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setOnModClick(java.util.function.Consumer<String> callback) {
        this.onModClick = callback;
    }

    public void setRootNodes(List<TreeNode> nodes) {
        this.rootNodes = nodes;
        this.pixelScrollOffset = 0;
        this.representativeCache.clear();
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
                       Component sectionLabel, SearchState state) {
        pendingTooltipLines = null;
        pendingTooltipImage = Optional.empty();
        pendingItemStack = null;
        currentLabelScale = computeLabelScale();

        String currentQuery = state.getQuery();
        Set<String> selectedMods = state.getSelectedMods();

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
                    effectiveMouseX, mouseY, y + topOffset, contentH, currentQuery, selectedMods);
        }

        g.disableScissor();

        renderScrollbar(g, totalH, contentH, y + topOffset, mouseX, mouseY);

        if (!toolbarDropdownOpen) {
            var font = Minecraft.getInstance().font;
            if (pendingItemStack != null && !pendingItemStack.isEmpty()) {
                g.renderTooltip(font, pendingItemStack, mouseX, mouseY);
            } else if (pendingTooltipLines != null) {
                g.renderTooltip(font, pendingTooltipLines, pendingTooltipImage, mouseX, mouseY);
            }
        }
    }

    /** Backwards-compatible overload used when no section label is needed. */
    public void render(GuiGraphics g, int mouseX, int mouseY, boolean toolbarDropdownOpen, SearchState state) {
        render(g, mouseX, mouseY, toolbarDropdownOpen, (Component) null, state);
    }

    /**
     * Renders one node and its children. Returns the next row index to use.
     * @param originY  top of the scrollable content region (y + topOffset)
     * @param contentH height of the scrollable content region
     */
    private int renderNode(GuiGraphics g, TreeNode node, int depth, int rowIdx,
                           int mouseX, int mouseY, int originY, int contentH, String currentQuery, Set<String> selectedMods) {
        int drawY = originY - pixelScrollOffset + rowIdx * AMITheme.ROW_HEIGHT;

        // Only draw if even partially visible
        if (drawY + AMITheme.ROW_HEIGHT > originY && drawY < originY + contentH) {
            boolean hovered = isRowHovered(mouseX, mouseY, drawY);

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
                renderLeaf(g, node, depth, drawY, hovered, currentQuery, selectedMods);
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
                rowIdx = renderNode(g, child, depth + 1, rowIdx, mouseX, mouseY, originY, contentH, currentQuery, selectedMods);
            }
        }

        return rowIdx;
    }

    // ── Leaf (rich card) ──────────────────────────────────────────────────────

    private void renderLeaf(GuiGraphics g, TreeNode node, int depth, int drawY, boolean hovered, String currentQuery, Set<String> selectedMods) {
        var font = Minecraft.getInstance().font;
        SearchNode entry = node.getEntry();

        int iconX = x + AMITheme.GLOBAL_PADDING + depth * INDENT;
        int iconY = drawY + (AMITheme.ROW_HEIGHT - AMITheme.ICON_SIZE) / 2;

        // Z-lift prevents dark-background clipping on 3D item models
        g.pose().pushPose();
        g.pose().translate(0, 0, 150);
        RendererRegistry.get(entry.type()).render(g, entry, iconX, iconY, AMITheme.ICON_SIZE);
        g.pose().popPose();

        int textX = iconX + AMITheme.ICON_SIZE + 4;
        int maxTextW = x + width - SCROLLBAR_W - 6 - textX;

        // Right-aligned badges
        int rightEdge = x + width - SCROLLBAR_W - 4;
        int badgeW = badgeWidth(font, entry);

        // Item name — truncated in font-pixel space (divide screen px by scale),
        // then drawn at reduced scale so long names fit.
        int availScreenPx = maxTextW - badgeW;
        String name = truncate(font, node.getLabel().getString(), (int)(availScreenPx / currentLabelScale));

        int screenTextY = drawY + (int)((AMITheme.ROW_HEIGHT - font.lineHeight * currentLabelScale) / 2);
        g.pose().pushPose();
        g.pose().scale(currentLabelScale, currentLabelScale, 1f);
        g.drawString(font, name,
                Math.round(textX / currentLabelScale), Math.round(screenTextY / currentLabelScale),
                AMITheme.TEXT_PRIMARY, currentLabelScale >= 1f);
        g.pose().popPose();

        // Mod name (on the second line, right aligned)
        // Check for @modid match in query or in selectedMods context object
        String modId = entry.id().getNamespace();
        boolean matched = selectedMods.contains(modId);
        if (!matched && currentQuery != null && !currentQuery.isEmpty()) {
            if (currentQuery.toLowerCase().contains("@" + modId.toLowerCase())) {
                matched = true;
            }
        }
        
        int modNameColor = matched ? TEXT_HIGHLIGHT : AmiColors.MOD_COLOR;
        
        renderBadges(g, font, entry, drawY, rightEdge, modNameColor, matched);

        if (hovered) {
            pendingTooltipLines = buildTooltip(entry);
            if (entry.type() != NodeType.ITEM) {
                var renderer = RendererRegistry.get(entry.type());
                pendingTooltipImage = renderer.getTooltipImage(entry);
            } else {
                pendingTooltipImage = Optional.empty();
            }
            pendingItemStack = null;
        }
    }

    // ── Badges ────────────────────────────────────────────────────────────────

    private void renderBadges(GuiGraphics g, net.minecraft.client.gui.Font font,
                              SearchNode entry, int drawY, int rightEdge, int modNameColor, boolean dropShadow) {
        int currentX = rightEdge;
        int textY = drawY + (AMITheme.ROW_HEIGHT - font.lineHeight) / 2;

        // Tool Requirement Badge — full 16×16, vertically centred
        String reqToolStr = entry.meta(SearchNodeKeys.REQUIRED_TOOL, "");
        if (!reqToolStr.isEmpty()) {
            ResourceLocation toolId = ResourceLocation.tryParse(reqToolStr);
            if (toolId != null) {
                Item toolItem = BuiltInRegistries.ITEM.get(toolId);
                if (toolItem != null && toolItem != net.minecraft.world.item.Items.AIR) {
                    ItemStack toolStack = new ItemStack(toolItem);
                    int iconX = currentX - 16;
                    int iconY = drawY + (AMITheme.ROW_HEIGHT - 16) / 2;
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 150);
                    g.renderItem(toolStack, iconX, iconY);
                    g.pose().popPose();
                }
            }
        }
        
        // Always skip the tool icon slot (16px + 4px gap) to keep mod names aligned
        currentX -= (16 + 4);

        // Subtitle fields (Mod, Storage, DPS, etc.) from RowFieldConfig
        List<RowField> active = RowFieldConfig.getSubtitleFields();
        if (active.isEmpty()) return;

        // Build list of parts that actually have data
        record BadgePart(RowField field, String text) {}
        List<BadgePart> parts = new ArrayList<>();
        for (RowField f : active) {
            String val = f.extract(entry);
            if (!val.isEmpty()) parts.add(new BadgePart(f, val));
        }

        if (parts.isEmpty()) return;

        // Calculate total width and handle truncation if needed
        int maxGroupW = (int)(width * 0.45);
        String fullJoined = RowFieldConfig.buildSubtitle(entry);
        if (font.width(fullJoined) > maxGroupW) {
            // If the whole thing is too long, we'll render a single truncated string in subtle color
            // (Simpler than per-part truncation while maintaining colors)
            String truncated = truncate(font, fullJoined, maxGroupW);
            int tw = font.width(truncated);
            g.drawString(font, truncated, currentX - tw, textY, AMITheme.TEXT_SUBTLE, false);
            return;
        }

        // Render from right to left to anchor against the tool slot
        for (int i = parts.size() - 1; i >= 0; i--) {
            BadgePart part = parts.get(i);
            int color = (part.field == RowField.MOD_NAME) ? modNameColor : AMITheme.TEXT_SUBTLE;
            boolean shadow = (part.field == RowField.MOD_NAME) ? dropShadow : false;
            
            int tw = font.width(part.text);
            g.drawString(font, part.text, currentX - tw, textY, color, shadow);
            currentX -= tw;

            if (i > 0) {
                String sep = " · ";
                int sw = font.width(sep);
                g.drawString(font, sep, currentX - sw, textY, AMITheme.TEXT_SUBTLE, false);
                currentX -= sw;
            }
        }
    }

    private int badgeWidth(net.minecraft.client.gui.Font font, SearchNode entry) {
        // Start with 20px (16px icon + 4px gap) reserved for the tool slot
        int w = 16 + 4;

        List<RowField> active = RowFieldConfig.getSubtitleFields();
        List<String> parts = new ArrayList<>();
        for (RowField f : active) {
            String val = f.extract(entry);
            if (!val.isEmpty()) parts.add(val);
        }

        if (!parts.isEmpty()) {
            String joined = String.join(" · ", parts);
            int maxGroupW = (int)(width * 0.45);
            w += font.width(truncate(font, joined, maxGroupW)) + 4;
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

        // Group icon
        ItemStack icon = resolveGroupIcon(node);
        if (!icon.isEmpty()) {
            int iconX = rowX + 12;
            int iconY = drawY + 1;
            g.pose().pushPose();
            g.pose().translate(0, 0, 150);
            g.renderItem(icon, iconX, iconY);
            g.pose().popPose();
        }

        // Count Badge
        String badge = "[" + node.getChildren().size() + " " + node.getLabel().getString() + "]";
        int badgeW = font.width(badge);
        int badgeX = x + width - SCROLLBAR_W - badgeW - 5;

        // Colour swatches — right-aligned, between label and badge
        List<Integer> swatchColors = collectSwatchColors(node, MAX_SWATCHES);
        int swatchBlockW = swatchColors.isEmpty() ? 0
                : swatchColors.size() * (SWATCH_SIZE + SWATCH_GAP);
        if (!swatchColors.isEmpty()) {
            int sx = badgeX - 4 - swatchBlockW;
            int sy = drawY + (AMITheme.ROW_HEIGHT - SWATCH_SIZE) / 2;
            for (int argb : swatchColors) {
                g.fill(sx, sy, sx + SWATCH_SIZE, sy + SWATCH_SIZE, argb);
                sx += SWATCH_SIZE + SWATCH_GAP;
            }
        }

        // Label (truncated to avoid overlap with swatches and badge)
        int labelRightBound = badgeX - (swatchBlockW > 0 ? swatchBlockW + 8 : 0);
        int labelMaxW = labelRightBound - (rowX + 32) - 4;
        String labelStr = node.getLabel().getString();
        String label = truncate(font, labelStr, Math.max(0, (int)(labelMaxW / currentLabelScale)));

        int screenLabelY = drawY + (int)((AMITheme.ROW_HEIGHT - font.lineHeight * currentLabelScale) / 2);
        g.pose().pushPose();
        g.pose().scale(currentLabelScale, currentLabelScale, 1f);

        int labelColor = node.isModGroup() ? AmiColors.MOD_COLOR : AMITheme.TEXT_HEADER;
        if (node.isHighCardinality()) {
            labelColor = 0xFFAAAA00; // Gold for high-cardinality groups
        }

        g.drawString(font, label,
                Math.round((rowX + 32) / currentLabelScale), Math.round(screenLabelY / currentLabelScale),
                labelColor, false);
        g.pose().popPose();

        int fullTextY = drawY + (AMITheme.ROW_HEIGHT - font.lineHeight) / 2;
        g.drawString(font, badge, badgeX, fullTextY, AMITheme.TEXT_SUBTLE, false);
    }

    // ── Representative icon helpers ───────────────────────────────────────────

    private ItemStack resolveGroupIcon(TreeNode node) {
        // Category-level nodes use the ontology's designated icon item.
        for (AmiOntology.Category cat : AmiOntology.CATEGORIES) {
            if (cat.id.equals(node.getKey())) {
                ResourceLocation iconId = ResourceLocation.tryParse(cat.iconItemId);
                if (iconId != null) {
                    Item item = BuiltInRegistries.ITEM.get(iconId);
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        return new ItemStack(item);
                    }
                }
                return ItemStack.EMPTY;
            }
        }
        // All other groups: use the alphabetically-first resolvable item in the subtree.
        SearchNode rep = getRepresentative(node);
        return rep != null ? ItemIconRenderer.resolveStack(rep.id()) : ItemStack.EMPTY;
    }

    private SearchNode getRepresentative(TreeNode node) {
        if (!representativeCache.containsKey(node)) {
            representativeCache.put(node, findRepresentative(node));
        }
        return representativeCache.get(node);
    }

    /**
     * Returns the alphabetically-first leaf in the subtree whose item registry
     * lookup is non-AIR. Prefers immediate children; falls back to child groups.
     */
    private SearchNode findRepresentative(TreeNode node) {
        List<SearchNode> weightedLeaves = node.getChildren().stream()
                .filter(TreeNode::isLeaf)
                .map(TreeNode::getEntry)
                .sorted(Comparator.comparingInt((SearchNode n) ->
                                com.sanhiruzu.ami.index.GroupingEngine.representativeWeight(ItemIconRenderer.resolveStack(n.id())))
                        .thenComparing(SearchNode::displayName))
                .toList();

        for (SearchNode candidate : weightedLeaves) {
            ItemStack stack = ItemIconRenderer.resolveStack(candidate.id());
            if (!stack.isEmpty()) {
                return candidate;
            }
        }

        for (TreeNode child : node.getChildren()) {
            if (!child.isLeaf()) {
                SearchNode rep = findRepresentative(child);
                if (rep != null) return rep;
            }
        }
        return null;
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

    private boolean isRowHovered(int mouseX, int mouseY, int drawY) {
        return mouseX >= x && mouseX < x + width - SCROLLBAR_W
                && mouseY >= drawY && mouseY < drawY + AMITheme.ROW_HEIGHT;
    }

    // ── Scrollbar ─────────────────────────────────────────────────────────────

    private void renderScrollbar(GuiGraphics g, int totalH, int contentH, int originY,
                                 int mouseX, int mouseY) {
        if (totalH <= contentH) return;

        boolean active = scrollbarDragging || isScrollbarHovered(mouseX, mouseY, totalH, contentH, originY);
        int barW = active ? 6 : 4;
        int barX = x + width - 1 - barW;
        int thumbH = Math.max(12, (contentH * contentH) / totalH);
        int maxScroll = totalH - contentH;
        int thumbY = originY + (pixelScrollOffset * (contentH - thumbH)) / maxScroll;

        // Higher contrast track background
        g.fill(x + width - SCROLLBAR_W, originY, x + width, originY + contentH, 0x44000000);
        g.fill(barX, thumbY, barX + barW, thumbY + thumbH,
                active ? AMITheme.SCROLL_THUMB_ACTIVE : AMITheme.SCROLL_THUMB);
    }

    private boolean isScrollbarHovered(int mouseX, int mouseY, int totalH, int contentH, int originY) {
        if (totalH <= contentH) return false;
        // Widen hitbox to 10px for easier clicking
        return mouseX >= x + width - 10 && mouseX < x + width
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
        if (font.width(text) <= maxW) return text;

        String ellipsis = "...";
        int ellipsisW = font.width(ellipsis);
        if (maxW <= ellipsisW) return ellipsis;

        return font.plainSubstrByWidth(text, maxW - ellipsisW) + ellipsis;
    }

    // ── Input handlers ────────────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (mouseX < x || mouseX >= x + width - SCROLLBAR_W) return false;

        // Which row is under the cursor?
        int targetRow = (int) (mouseY - y + pixelScrollOffset) / AMITheme.ROW_HEIGHT;
        if (targetRow < 0) return false;

        int[] counter = {0};
        for (TreeNode node : rootNodes) {
            if (handleNodeClick(node, targetRow, counter, mouseX)) return true;
        }
        return false;
    }

    /** DFS click handler. Returns true when the target row was found and handled. */
    private boolean handleNodeClick(TreeNode node, int targetRow, int[] counter, double mouseX) {
        if (counter[0] == targetRow) {
            if (node.isLeaf()) {
                // Check if mod badge was clicked (approximate area check)
                int rightEdge = x + width - SCROLLBAR_W - 4;
                int bWidth = badgeWidth(Minecraft.getInstance().font, node.getEntry());
                int badgeStartX = rightEdge - bWidth;

                if (onModClick != null && mouseX >= badgeStartX && mouseX <= rightEdge) {
                    onModClick.accept("@" + node.getEntry().id().getNamespace());
                }
            } else {
                node.setExpanded(!node.isExpanded());
            }
            return true;
        }
        counter[0]++;

        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                if (handleNodeClick(child, targetRow, counter, mouseX)) return true;
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

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            if (pendingItemStack != null && !pendingItemStack.isEmpty()) {
                AmiClipboardHelper.copyItemTooltipToClipboard(pendingItemStack);
                return true;
            } else if (pendingTooltipLines != null && !pendingTooltipLines.isEmpty()) {
                AmiClipboardHelper.copyComponentsToClipboard(pendingTooltipLines);
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            pixelScrollOffset = Math.max(0, pixelScrollOffset - (lastContentH > 0 ? lastContentH : height));
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            int contentH = lastContentH > 0 ? lastContentH : height;
            int totalH = countAllNodes() * AMITheme.ROW_HEIGHT;
            int maxScroll = Math.max(0, totalH - contentH);
            pixelScrollOffset = Math.min(maxScroll, pixelScrollOffset + contentH);
            return true;
        }
        return false;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int contentH = lastContentH > 0 ? lastContentH : height;
        int topOffset = height - contentH;
        int totalH = countAllNodes() * AMITheme.ROW_HEIGHT;
        if (!isScrollbarHovered((int) mouseX, (int) mouseY, totalH, contentH, y + topOffset)) return false;
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

    // ── Tooltip ───────────────────────────────────────────────────────────────

    private List<Component> buildTooltip(SearchNode entry) {
        if (Screen.hasControlDown()) {
            return DebugTooltip.build(entry);
        }

        List<Component> lines = new ArrayList<>();

        if (entry.type() == NodeType.ITEM) {
            ItemStack stack = ItemIconRenderer.resolveStack(entry.id());
            if (!stack.isEmpty()) {
                lines.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
            } else {
                lines.add(Component.literal(entry.displayName()));
            }
        } else {
            lines.add(Component.literal(entry.displayName()));
            var renderer = RendererRegistry.get(entry.type());
            List<Component> extra = renderer.getTooltip(entry);
            if (extra != null) lines.addAll(extra);
        }

        // Add ID
        lines.add(Component.literal(entry.id().toString()).withStyle(s -> s.withColor(0x666666)));

        // Unified Info: Required Tool
        String reqToolStr = entry.meta(SearchNodeKeys.REQUIRED_TOOL, "");
        if (!reqToolStr.isEmpty()) {
            ResourceLocation toolId = ResourceLocation.tryParse(reqToolStr);
            if (toolId != null) {
                Item toolItem = BuiltInRegistries.ITEM.get(toolId);
                if (toolItem != null && toolItem != net.minecraft.world.item.Items.AIR) {
                    lines.add(Component.empty());
                    lines.add(Component.translatable("ami.tooltip.required_tool", toolItem.getDescription())
                            .withStyle(s -> s.withColor(0x888888)));
                }
            }
        }

        lines.add(Component.literal("§8Hold Ctrl for AMI debug info"));
        return lines;
    }
}
