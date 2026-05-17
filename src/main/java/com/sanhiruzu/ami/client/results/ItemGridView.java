package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Renders ITEM-type SearchNodes as a 3D item icon grid (18×18 cells).
 * Group nodes from the tree processor become collapsible section headers.
 * Non-ITEM leaf nodes are silently skipped.
 */
public class ItemGridView {
    private static final int CELL_SIZE  = 18;
    private static final int HEADER_H   = 12;
    private static final int SCROLLBAR_W = 5;

    private int x, y, width, height;
    private List<TreeNode> rootNodes = new ArrayList<>();
    private int pixelScrollOffset = 0;

    private boolean scrollbarDragging = false;
    private int scrollbarDragStartY;
    private int scrollbarDragStartOffset;

    /** Set by UniversalResultsPanel to route clicks to the recipe bridge. */
    private BiConsumer<SearchNode, Integer> onItemClick;
    private java.util.function.Consumer<String> onTokenInject;

    // Deferred tooltips — built during render, drawn after scissor is popped
    private ItemStack pendingTooltip = null;
    private List<Component> pendingTextTooltip = null;
    private Optional<TooltipComponent> pendingTooltipImage = Optional.empty();
    private SearchNode hoveredNode = null;

    // Virtual row cache — rebuilt whenever rootNodes changes or a group is toggled
    private List<VirtualRow> cachedRows = null;
    private int cachedCols = -1;
    private final Map<TreeNode, TreeNode> expandedGroupCache = new HashMap<>();

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

    public void setOnTokenInject(java.util.function.Consumer<String> callback) {
        this.onTokenInject = callback;
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

        if (rootNodes.isEmpty()) {
            g.drawString(Minecraft.getInstance().font,
                    "No results", x + 4, y + 4, 0xFFCCCCCC, false);
            return;
        }

        int effectiveMouseX = toolbarDropdownOpen ? -1 : mouseX;

        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        int totalH = calcTotalHeight(rows);

        g.enableScissor(x, y, x + width, y + height);

        int drawY = y - pixelScrollOffset;
        for (VirtualRow row : rows) {
            int rowBottom = drawY + row.height();
            if (rowBottom > y && drawY < y + height) {
                if (row instanceof HeaderRow hr) {
                    renderHeader(g, hr, drawY, effectiveMouseX, mouseY);
                } else if (row instanceof ItemRow ir) {
                    renderItemRow(g, ir, drawY, effectiveMouseX, mouseY);
                }
            }
            drawY += row.height();
        }

        g.disableScissor();

        renderScrollbar(g, totalH, mouseX, mouseY);

        if (!toolbarDropdownOpen) {
            var font = Minecraft.getInstance().font;
            if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
                g.renderTooltip(font, pendingTooltip, mouseX, mouseY);
            } else if (pendingTextTooltip != null) {
                g.renderTooltip(font, pendingTextTooltip, pendingTooltipImage, mouseX, mouseY);
            }
        }
    }

    private void renderHeader(GuiGraphics g, HeaderRow hr, int drawY, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width - SCROLLBAR_W
                && mouseY >= drawY && mouseY < drawY + HEADER_H;
        if (hovered) {
            g.fill(x, drawY, x + width - SCROLLBAR_W, drawY + HEADER_H, 0xFF282820);
        }
        String arrow = hr.node().isExpanded() ? "▼ " : "▶ ";
        String label = arrow + hr.node().getLabel().getString() + " (" + hr.itemCount() + ")";
        g.drawString(Minecraft.getInstance().font, label, x + 4, drawY + 2, 0xFFAAAA88, false);
    }

    private void renderItemRow(GuiGraphics g, ItemRow ir, int drawY, int mouseX, int mouseY) {
        int cols = computeCols();
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
            if (hovered) {
                g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, com.sanhiruzu.ami.client.AMITheme.SLOT_HOVER);
                hoveredNode = entry;
                
                if (node.isHighCardinality()) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(node.getLabel().copy().append(" ").append(Component.translatable("ami.gui.items_count", node.getChildren().size())));
                    lines.add(Component.translatable(node.isExpanded() ? "ami.gui.collapse_hint" : "ami.gui.expand_hint")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                    pendingTextTooltip = lines;
                    pendingTooltipImage = Optional.empty();
                } else if (com.sanhiruzu.ami.client.AMIKeyMappings.DEBUG_TOOLTIPS.isDown()) {
                    pendingTextTooltip = com.sanhiruzu.ami.client.results.DebugTooltip.build(entry);
                    pendingTooltipImage = Optional.empty();
                } else if (entry.type() == com.sanhiruzu.ami.index.NodeType.ITEM) {
                    pendingTooltip = resolveStack(entry);
                } else {
                    var renderer = com.sanhiruzu.ami.client.icon.RendererRegistry.get(entry.type());
                    List<Component> rendererLines = renderer.getTooltip(entry);
                    if (rendererLines != null) {
                        rendererLines = new ArrayList<>(rendererLines);
                        String keybindName = com.sanhiruzu.ami.client.AMIKeyMappings.DEBUG_TOOLTIPS.getTranslatedKeyMessage().getString();
                        Component hint = Component.translatable("ami.gui.debug_hint", keybindName).withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
                        rendererLines.add(hint);
                    }
                    pendingTextTooltip = rendererLines;
                    pendingTooltipImage = renderer.getTooltipImage(entry);
                }
            }

            // Group styling
            if (node.isHighCardinality() && !node.isExpanded()) {
                // Gold border for collapsed groups
                g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + 1, 0xFFAAAA00);
                g.fill(cellX, cellY + CELL_SIZE - 1, cellX + CELL_SIZE, cellY + CELL_SIZE, 0xFFAAAA00);
                g.fill(cellX, cellY, cellX + 1, cellY + CELL_SIZE, 0xFFAAAA00);
                g.fill(cellX + CELL_SIZE - 1, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, 0xFFAAAA00);
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
                    
                    int color = 0xFFAAAA00; // Opaque gold border
                    int bgCol = 0x44AAAA00; // Visible gold tint background
                    
                    g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, bgCol);
                    
                    if (topEdge) g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + 1, color);
                    if (bottomEdge) g.fill(cellX, cellY + CELL_SIZE - 1, cellX + CELL_SIZE, cellY + CELL_SIZE, color);
                    if (leftEdge) g.fill(cellX, cellY, cellX + 1, cellY + CELL_SIZE, color);
                    if (rightEdge) g.fill(cellX + CELL_SIZE - 1, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, color);

                    // If this is the header node of the expanded group, make it look like a "close" button
                    if (node == expandedGroup) {
                        g.fill(cellX + 1, cellY + 1, cellX + CELL_SIZE - 1, cellY + CELL_SIZE - 1, 0x66000000); // Darken the header icon
                        g.fill(cellX + 2, cellY + 2, cellX + 4, cellY + 4, 0xFFFFFFFF); // Small visual indicator
                    }
                }
            }

            if (overrideStack != null) {
                renderIconWithWiggle(g, overrideStack, cellX + 1, cellY + 1, hovered);
            } else if (entry.type() == com.sanhiruzu.ami.index.NodeType.ITEM) {
                ItemStack stack = resolveStack(entry);
                if (!stack.isEmpty()) renderIconWithWiggle(g, stack, cellX + 1, cellY + 1, hovered);
            } else {
                renderRendererWithWiggle(g, entry, cellX + 1, cellY + 1, hovered);
            }
        }
    }

    private void renderIconWithWiggle(GuiGraphics g, ItemStack stack, int x, int y, boolean hovered) {
        boolean dragging = com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging();
        g.pose().pushPose();
        g.pose().translate(x + 8, y + 8, 150);
        if (dragging || hovered) {
            float time = (System.currentTimeMillis() % 1000) / 1000f;
            float wiggle = (float) Math.sin(time * Math.PI * 2) * 0.05f;
            g.pose().scale(1.1f + wiggle, 1.1f + wiggle, 1.1f);
            if (dragging) {
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) Math.sin(time * Math.PI * 4) * 2f));
            }
        }
        g.renderItem(stack, -8, -8);
        g.pose().popPose();
    }

    private void renderRendererWithWiggle(GuiGraphics g, SearchNode entry, int x, int y, boolean hovered) {
        boolean dragging = com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging();
        g.pose().pushPose();
        g.pose().translate(x, y, 150);
        if (dragging || hovered) {
            float time = (System.currentTimeMillis() % 1000) / 1000f;
            float wiggle = (float) Math.sin(time * Math.PI * 2) * 0.05f;
            g.pose().translate(8, 8, 0);
            g.pose().scale(1.1f + wiggle, 1.1f + wiggle, 1.1f);
            if (dragging) {
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) Math.sin(time * Math.PI * 4) * 2f));
            }
            g.pose().translate(-8, -8, 0);
        }
        com.sanhiruzu.ami.client.icon.RendererRegistry.get(entry.type()).render(g, entry, 0, 0, 16);
        g.pose().popPose();
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

        // rootNodes now contains special HighCardinality groups
        List<TreeNode> linearItems = new ArrayList<>();
        
        for (TreeNode root : rootNodes) {
            if (root.isLeaf()) {
                linearItems.add(root);
            } else if (root.isHighCardinality()) {
                linearItems.add(root); // Add group header itself as a clickable grid item
                if (root.isExpanded()) {
                    linearItems.addAll(root.getChildren());
                    expandedGroupCache.put(root, root);
                    for (TreeNode child : root.getChildren()) {
                        expandedGroupCache.put(child, root);
                    }
                }
            } else {
                // Section header (mod, category, etc.)
                packIntoRows(linearItems, cols, rows);
                linearItems.clear();
                addGroupRows(root, cols, rows);
            }
        }
        packIntoRows(linearItems, cols, rows);

        return rows;
    }

    private void addGroupRows(TreeNode group, int cols, List<VirtualRow> out) {
        // This is for standard groups (section headers)
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
        } else if (node.isHighCardinality()) {
            out.add(node);
            if (node.isExpanded()) {
                out.addAll(node.getChildren());
                expandedGroupCache.put(node, node);
                for (TreeNode child : node.getChildren()) expandedGroupCache.put(child, node);
            }
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
        return ItemIconRenderer.resolveStack(node.id());
    }

    // =========================================================
    // Scrollbar
    // =========================================================

    private void renderScrollbar(GuiGraphics g, int totalH, int mouseX, int mouseY) {
        if (totalH <= height) return;
        boolean active = scrollbarDragging || isScrollbarHovered(mouseX, mouseY);
        int barW = active ? 6 : 4;
        int barX = x + width - 1 - barW;
        int thumbH = Math.max(12, (height * height) / totalH);
        int maxScroll = totalH - height;
        int thumbY = y + (pixelScrollOffset * (height - thumbH)) / maxScroll;

        // Higher contrast track background
        g.fill(x + width - SCROLLBAR_W, y, x + width, y + height, 0x44000000);
        g.fill(barX, thumbY, barX + barW, thumbY + thumbH,
                active ? 0xFFAAAA88 : 0xFF666666);
    }

    private boolean isScrollbarHovered(int mouseX, int mouseY) {
        int cols = computeCols();
        int totalH = calcTotalHeight(getVirtualRows(cols));
        if (totalH <= height) return false;
        // Widen hitbox to 10px
        return mouseX >= x + width - 10 && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
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

        int drawY = y - pixelScrollOffset;
        for (VirtualRow row : rows) {
            if (mouseY >= drawY && mouseY < drawY + row.height()) {
                if (row instanceof HeaderRow hr) {
                    if (button == 0) {
                        hr.node().setExpanded(!hr.node().isExpanded());
                        cachedRows = null; // rebuild
                    } else if (button == 1 && onTokenInject != null) {
                        // Right-click on group header: inject category token
                        onTokenInject.accept("$" + hr.node().getKey());
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
                            if (button == 1 && onTokenInject != null) {
                                // Right-click on item: inject mod name
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
        int totalH = calcTotalHeight(getVirtualRows(cols));
        int maxScroll = Math.max(0, totalH - height);

        // Adaptive scroll speed: 1/2 of visible height per tick
        // With 400 mods, this provides snappy scrolling like EMI
        int visibleRows = Math.max(1, height / CELL_SIZE);
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
            if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
                AmiClipboardHelper.copyItemTooltipToClipboard(pendingTooltip);
                return true;
            } else if (pendingTextTooltip != null && !pendingTextTooltip.isEmpty()) {
                AmiClipboardHelper.copyComponentsToClipboard(pendingTextTooltip);
                return true;
            }
        }

        if (keyCode == 266) { // Page Up
            pixelScrollOffset = Math.max(0, pixelScrollOffset - height);
            return true;
        } else if (keyCode == 267) { // Page Down
            int cols = computeCols();
            int totalH = calcTotalHeight(getVirtualRows(cols));
            int maxScroll = Math.max(0, totalH - height);
            pixelScrollOffset = Math.min(maxScroll, pixelScrollOffset + height);
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

    public SearchNode getHoveredNode() {
        return hoveredNode;
    }

    public TreeNode getHoveredTreeNode() {
        if (hoveredNode == null) return null;
        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        int drawY = y - pixelScrollOffset;
        for (VirtualRow row : rows) {
            if (row instanceof HeaderRow hr) {
                // Header rows are handled separately in mouseClicked, but we include it for completeness
            } else if (row instanceof ItemRow ir) {
                if (drawY + ir.height() > y && drawY < y + height) {
                    // Check if mouse is over any item in this row
                    // We already have hoveredNode, but we need the TreeNode
                    // Use a simple coordinate check
                }
            }
            drawY += row.height();
        }
        return null; // For grid, mouseClicked handles the expansion better
    }

    public int getDropIndex(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return -1;
        int cols = computeCols();
        List<VirtualRow> rows = getVirtualRows(cols);
        int itemCounter = 0;
        int drawY = y - pixelScrollOffset;
        for (VirtualRow row : rows) {
            if (mouseY >= drawY && mouseY < drawY + row.height()) {
                if (row instanceof HeaderRow) return itemCounter;
                if (row instanceof ItemRow ir) {
                    int col = ((int) mouseX - x - 1) / CELL_SIZE;
                    return itemCounter + Math.clamp(col, 0, ir.items().size());
                }
            }
            if (row instanceof ItemRow ir) itemCounter += ir.items().size();
            drawY += row.height();
        }
        return itemCounter;
    }
}
