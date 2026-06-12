package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiRenderPhase;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import com.sanhiruzu.ami.util.AmiColors;
import com.sanhiruzu.ami.util.AmiTooltipComposer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * "Suginami" rich-card list view.
 * <p>
 * Each leaf row is 18 px tall:
 * [X_PAD][16×16 icon][4px][Item Name  (line 1, y+1)   |  badge right-aligned]
 * [mod.name   (line 2, y+10)                        ]
 * <p>
 * Group rows sit at the same 24 px row height and may show up to 3 colour-swatch
 * dots on the right when their children belong to a shared variant group.
 */
public class ResultsTreeView {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int INDENT = 12;
    private static final int SCROLLBAR_W = 6;
    private static final int HEADER_LABEL_H = 16; // height reserved for the optional pinned header (column row)
    private static final ResourceLocation VANILLA_SCROLLER =
            Services.PLATFORM.rl("minecraft", "widget/scroller");
    private static final ResourceLocation VANILLA_SCROLLER_BACKGROUND =
            Services.PLATFORM.rl("minecraft", "widget/scroller_background");

    // Swatch dots for variant collapsing
    private static final int SWATCH_SIZE = 5;
    private static final int SWATCH_GAP = 2;
    private static final int MAX_SWATCHES = 3;
    /**
     * Cached representative SearchNode per group TreeNode; cleared whenever rootNodes changes.
     */
    private final Map<TreeNode, SearchNode> representativeCache = new HashMap<>();
    // Recomputed each frame — 0.75× when guiScale ≥ 3, otherwise 1.0×.
    private float currentLabelScale = 1.0f;
    // ── State ─────────────────────────────────────────────────────────────────
    private int x, y, width, height;
    private List<TreeNode> rootNodes = new ArrayList<>();
    /**
     * Pixel scroll offset — increases as user scrolls down.
     */
    private int pixelScrollOffset = 0;
    /**
     * Height of the scrollable content area (height minus any sticky header). Updated each render.
     */
    private int lastContentH = 0;
    private boolean scrollbarDragging = false;
    private int scrollbarDragStartY;
    private int scrollbarDragStartOffset;
    private List<Component> pendingTooltipLines = null;
    private Optional<TooltipComponent> pendingTooltipImage = Optional.empty();
    private ItemStack pendingItemStack = null;
    private SearchNode hoveredNode = null;
    private java.util.function.Consumer<String> onModClick = null;
    private java.util.function.BiConsumer<SearchNode, Integer> onItemClick = null;
    private java.util.function.BiConsumer<TreeNode, Integer> onGroupClick = null;
    private java.util.function.Consumer<String> onTokenInject = null;
    public ResultsTreeView(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ── Construction ──────────────────────────────────────────────────────────

    private static float computeLabelScale() {
        return Minecraft.getInstance().getWindow().getGuiScale() >= 3.0 ? 0.75f : 1.0f;
    }

    private static int scaledBadgeWidth(net.minecraft.client.gui.Font font, String text, float scale) {
        return (int) Math.ceil(font.width(text) * scale);
    }

    private static void drawBadgeText(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int x, int y,
                                      float scale, int color, boolean shadow) {
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(font, text, Math.round(x / scale), Math.round(y / scale), color, shadow);
        g.pose().popPose();
    }

    private static String truncate(net.minecraft.client.gui.Font font, String text, int maxW) {
        if (maxW <= 0) return "";
        if (font.width(text) <= maxW) return text;

        String ellipsis = "...";
        int ellipsisW = font.width(ellipsis);
        if (maxW <= ellipsisW) return ellipsis;

        return font.plainSubstrByWidth(text, maxW - ellipsisW) + ellipsis;
    }

    private static boolean isTokenInjectClick(int button) {
        return ViewInputHelper.isTokenInjectClick(button);
    }

    private static boolean isEmiRecipeScreenActive() {
        return com.sanhiruzu.ami.compat.RecipeViewerBridge.isEmiRecipeScreenActive();
    }

    public void setOnModClick(java.util.function.Consumer<String> callback) {
        this.onModClick = callback;
    }

    public void setItemClickCallback(java.util.function.BiConsumer<SearchNode, Integer> callback) {
        this.onItemClick = callback;
    }

    public void setGroupClickCallback(java.util.function.BiConsumer<TreeNode, Integer> callback) {
        this.onGroupClick = callback;
    }

    public void setOnTokenInject(java.util.function.Consumer<String> callback) {
        this.onTokenInject = callback;
    }

    public List<TreeNode> getRootNodes() {
        return List.copyOf(rootNodes);
    }

    public void setRootNodes(List<TreeNode> nodes) {
        setRootNodes(nodes, true);
    }

    public void setRootNodes(List<TreeNode> nodes, boolean resetScroll) {
        this.rootNodes = nodes;
        if (resetScroll) this.pixelScrollOffset = 0;
        this.representativeCache.clear();
    }

    public void collapseAll() {
        for (TreeNode node : rootNodes) {
            collapseNode(node);
        }
        this.pixelScrollOffset = 0;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void expandAll() {
        for (TreeNode node : rootNodes) {
            expandNode(node);
        }
        this.pixelScrollOffset = 0;
    }

    private void collapseNode(TreeNode node) {
        node.setExpanded(false);
        for (TreeNode child : node.getChildren()) {
            collapseNode(child);
        }
    }

    private void expandNode(TreeNode node) {
        node.setExpanded(true);
        for (TreeNode child : node.getChildren()) {
            expandNode(child);
        }
    }

    // ── Leaf (rich card) ──────────────────────────────────────────────────────

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ── Badges ────────────────────────────────────────────────────────────────

    /**
     * @param sectionLabel Optional label drawn as a sticky header above the scroll region
     *                     (e.g. "Pinned & Discover"). Pass null to omit.
     */
    public void render(GuiGraphics g, int mouseX, int mouseY, boolean toolbarDropdownOpen,
                       Component sectionLabel, SearchState state) {
        AmiRenderPhase.requireBase("ResultsTreeView.render");
        pendingTooltipLines = null;
        pendingTooltipImage = Optional.empty();
        pendingItemStack = null;
        hoveredNode = null;
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
                    Component.translatable("ami.gui.no_results"), x + AMITheme.GLOBAL_PADDING, y + topOffset + AMITheme.GLOBAL_PADDING, AMITheme.TEXT_SUBTLE, false);
            return;
        }

        int contentH = height - topOffset;
        lastContentH = contentH;
        int totalH = countAllNodes() * AMITheme.ROW_HEIGHT;
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

    }

    public void renderPendingTooltip(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        if (pendingTooltipLines != null) {
            ItemStack stackContext = (pendingItemStack != null) ? pendingItemStack : ItemStack.EMPTY;
            AmiTooltipRenderer.render(g, font, stackContext, pendingTooltipLines, pendingTooltipImage, mouseX, mouseY);
        } else if (pendingItemStack != null && !pendingItemStack.isEmpty()) {
            AmiTooltipRenderer.render(g, font, pendingItemStack, mouseX, mouseY);
        }
    }

    /**
     * Backwards-compatible overload used when no section label is needed.
     */
    public void render(GuiGraphics g, int mouseX, int mouseY, boolean toolbarDropdownOpen, SearchState state) {
        render(g, mouseX, mouseY, toolbarDropdownOpen, (Component) null, state);
    }

    /**
     * Renders one node and its children. Returns the next row index to use.
     *
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
                boolean isModern = com.sanhiruzu.ami.config.AmiConfig.theme != com.sanhiruzu.ami.config.AmiConfig.Theme.VANILLA;
                int tint = isModern ? AMITheme.GRID_ROW_TINT_EVEN : AMITheme.GRID_ROW_TINT_ODD;
                g.fill(x, drawY, x + width - SCROLLBAR_W, drawY + AMITheme.ROW_HEIGHT, tint);
            }

            if (!node.isLeaf()) {
                int groupBg = depth == 0 ? AMITheme.GRID_GROUP_ROOT_BG : AMITheme.GROUP_HEADER_BG;
                g.fill(x, drawY, x + width - SCROLLBAR_W, drawY + AMITheme.ROW_HEIGHT, groupBg);
                if (depth == 0) {
                    g.fill(x, drawY, x + 2, drawY + AMITheme.ROW_HEIGHT, AMITheme.ACCENT_BLUE);
                    g.fill(x + 2, drawY + AMITheme.ROW_HEIGHT - 1,
                            x + width - SCROLLBAR_W - 2, drawY + AMITheme.ROW_HEIGHT, AMITheme.SECTION_SEP);
                } else {
                    int railX = x + 5 + (depth - 1) * INDENT;
                    g.fill(railX, drawY + 3, railX + 1, drawY + AMITheme.ROW_HEIGHT - 3, AMITheme.GRID_GROUP_RAIL);
                }
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

    private void renderLeaf(GuiGraphics g, TreeNode node, int depth, int drawY, boolean hovered, String currentQuery, Set<String> selectedMods) {
        var font = Minecraft.getInstance().font;
        SearchNode entry = node.getEntry();

        int iconX = x + AMITheme.GLOBAL_PADDING + depth * INDENT;
        int iconY = drawY + (AMITheme.ROW_HEIGHT - AMITheme.ICON_SIZE) / 2;

        // Z-lift prevents dark-background clipping on 3D item models
        g.pose().pushPose();
        g.pose().translate(iconX + 8, iconY + 8, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);

        boolean dragging = com.sanhiruzu.ami.compat.RecipeViewerBridge.isDragging();
        if (dragging || hovered) {
            float time = (System.currentTimeMillis() % 1000) / 1000f;
            float wiggle = (float) Math.sin(time * Math.PI * 2) * 0.05f;
            g.pose().scale(1.1f + wiggle, 1.1f + wiggle, 1.1f);
            if (dragging) {
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) Math.sin(time * Math.PI * 4) * 2f));
            }
        }

        var renderer = usesPlayerModelRenderer(entry) ? RendererRegistry.PLAYER_MODEL : RendererRegistry.get(entry.type());
        renderer.render(g, entry, -8, -8, AMITheme.ICON_SIZE, hovered);
        g.pose().popPose();
        DiscoveryVisuals.renderIconOverlay(g, entry, iconX, iconY, AMITheme.ICON_SIZE);
        AccessLevelOverlayRenderer.renderIconOverlay(g, entry, iconX, iconY, AMITheme.ICON_SIZE);

        int textX = iconX + AMITheme.ICON_SIZE + 4;
        int maxTextW = x + width - SCROLLBAR_W - 6 - textX;

        // Right-aligned badges
        int rightEdge = x + width - SCROLLBAR_W - 4;
        int badgeW = badgeWidth(font, entry);

        // Item name — truncated in font-pixel space (divide screen px by scale),
        // then drawn at reduced scale so long names fit.
        int availScreenPx = maxTextW - badgeW;
        String name = truncate(font, node.getLabel().getString(), (int) (availScreenPx / currentLabelScale));

        int screenTextY = drawY + (int) ((AMITheme.ROW_HEIGHT - font.lineHeight * currentLabelScale) / 2);
        g.pose().pushPose();
        g.pose().scale(currentLabelScale, currentLabelScale, 1f);
        g.drawString(font, name,
                Math.round(textX / currentLabelScale), Math.round(screenTextY / currentLabelScale),
                DiscoveryVisuals.primaryTextColor(entry, AMITheme.TEXT_PRIMARY), currentLabelScale >= 1f);
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

        int modNameColor = matched ? AMITheme.TEXT_HIGHLIGHT : AMITheme.MOD_NAME;
        modNameColor = DiscoveryVisuals.subtitleTextColor(entry, modNameColor);

        renderBadges(g, font, entry, drawY, rightEdge, modNameColor, matched);

        if (hovered) {
            hoveredNode = entry;
            pendingTooltipLines = AmiTooltipComposer.buildTooltip(entry);
            pendingTooltipImage = AmiTooltipComposer.getTooltipImage(entry);
            if (entry.type() == NodeType.ITEM) {
                pendingItemStack = ItemIconRenderer.resolveStack(entry.id());
            } else {
                pendingItemStack = null;
            }
        }
    }

    private static boolean usesPlayerModelRenderer(SearchNode entry) {
        return com.sanhiruzu.ami.config.AmiConfig.playerHeadShowFullModel
                && entry != null
                && entry.type() == com.sanhiruzu.ami.index.NodeType.ITEM
                && !entry.meta(SearchNodeKeys.PLAYER_HEAD_NAME, "").isBlank();
    }

    // ── Group header ──────────────────────────────────────────────────────────

    private void renderBadges(GuiGraphics g, net.minecraft.client.gui.Font font,
                              SearchNode entry, int drawY, int rightEdge, int modNameColor, boolean dropShadow) {
        int currentX = rightEdge;
        float badgeScale = currentLabelScale;
        int textY = drawY + Math.max(1, (int) ((AMITheme.ROW_HEIGHT - font.lineHeight * badgeScale) / 2));

        // Tool Requirement Badge — 12×12, vertically centred
        String reqToolStr = entry.meta(SearchNodeKeys.REQUIRED_TOOL, "");
        if (!reqToolStr.isEmpty()) {
            ResourceLocation toolId = ResourceLocation.tryParse(reqToolStr);
            if (toolId != null) {
                Item toolItem = BuiltInRegistries.ITEM.get(toolId);
                if (toolItem != null && toolItem != net.minecraft.world.item.Items.AIR) {
                    ItemStack toolStack = new ItemStack(toolItem);
                    int TOOL_ICON_SIZE = 12;
                    int iconX = currentX - TOOL_ICON_SIZE;
                    int iconY = drawY + (AMITheme.ROW_HEIGHT - TOOL_ICON_SIZE) / 2;
                    g.pose().pushPose();
                    g.pose().translate(iconX + TOOL_ICON_SIZE / 2.0, iconY + TOOL_ICON_SIZE / 2.0, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);
                    float toolScale = TOOL_ICON_SIZE / 16.0f;
                    g.pose().scale(toolScale, toolScale, 1.0f);
                    g.renderItem(toolStack, -8, -8);
                    g.pose().popPose();
                }
            }
        }

        // Always skip the tool icon slot (12px + 4px gap) to keep mod names aligned
        currentX -= (12 + 4);

        QuestItemEvidence questEvidence = QuestItemEvidenceProjector.project(entry);
        if (questEvidence.hasMatches()) {
            currentX = renderQuestBadge(g, font, questEvidence, currentX, drawY, badgeScale);
        }

        // Subtitle fields (Mod, Storage, DPS, etc.) from RowFieldConfig
        List<RowField> active = RowFieldConfig.getSubtitleFields();
        if (active.isEmpty()) return;

        // Build list of parts that actually have data
        record BadgePart(RowField field, String text) {
        }
        List<BadgePart> parts = new ArrayList<>();
        for (RowField f : active) {
            String val = f.extract(entry);
            if (!val.isEmpty()) parts.add(new BadgePart(f, val));
        }

        if (parts.isEmpty()) return;

        // Calculate total width and handle truncation if needed
        int maxGroupW = (int) (width * 0.55);
        String fullJoined = RowFieldConfig.buildSubtitle(entry);
        if (scaledBadgeWidth(font, fullJoined, badgeScale) > maxGroupW) {
            // If the whole thing is too long, we'll render a single truncated string in subtle color
            // (Simpler than per-part truncation while maintaining colors)
            String truncated = truncate(font, fullJoined, (int) (maxGroupW / badgeScale));
            int tw = scaledBadgeWidth(font, truncated, badgeScale);
            drawBadgeText(g, font, truncated, currentX - tw, textY, badgeScale, AMITheme.TEXT_SUBTLE, false);
            return;
        }

        // Render from right to left to anchor against the tool slot
        for (int i = parts.size() - 1; i >= 0; i--) {
            BadgePart part = parts.get(i);
            int color = (part.field == RowField.MOD_NAME) ? modNameColor : AMITheme.TEXT_SUBTLE;
            boolean shadow = (part.field == RowField.MOD_NAME) ? dropShadow : false;

            int tw = scaledBadgeWidth(font, part.text, badgeScale);
            drawBadgeText(g, font, part.text, currentX - tw, textY, badgeScale, color, shadow);
            currentX -= tw;

            if (i > 0) {
                String sep = " · ";
                int sw = scaledBadgeWidth(font, sep, badgeScale);
                drawBadgeText(g, font, sep, currentX - sw, textY, badgeScale, AMITheme.TEXT_SUBTLE, false);
                currentX -= sw;
            }
        }
    }

    private int renderQuestBadge(GuiGraphics g, net.minecraft.client.gui.Font font,
                                 QuestItemEvidence evidence, int currentX, int drawY, float scale) {
        String label = evidence.badgeLabel();
        int textW = scaledBadgeWidth(font, label, scale);
        int badgeW = textW + 6;
        int badgeH = Math.max(9, (int) Math.ceil(font.lineHeight * scale) + 1);
        int badgeX = currentX - badgeW;
        int badgeY = drawY + (AMITheme.ROW_HEIGHT - badgeH) / 2;
        int color = evidence.hasRequirement() ? AMITheme.ACCENT_BLUE : AMITheme.ACCENT_GOLD;

        g.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xAA000000);
        g.fill(badgeX, badgeY, badgeX + 1, badgeY + badgeH, color);
        drawBadgeText(g, font, label, badgeX + 3, badgeY + 1, scale, color, false);
        return badgeX - 4;
    }

    // ── Representative icon helpers ───────────────────────────────────────────

    private int badgeWidth(net.minecraft.client.gui.Font font, SearchNode entry) {
        // Start with 20px (16px icon + 4px gap) reserved for the tool slot
        int w = 16 + 4;

        QuestItemEvidence questEvidence = QuestItemEvidenceProjector.project(entry);
        if (questEvidence.hasMatches()) {
            w += scaledBadgeWidth(font, questEvidence.badgeLabel(), currentLabelScale) + 10;
        }

        List<RowField> active = RowFieldConfig.getSubtitleFields();
        List<String> parts = new ArrayList<>();
        for (RowField f : active) {
            String val = f.extract(entry);
            if (!val.isEmpty()) parts.add(val);
        }

        if (!parts.isEmpty()) {
            String joined = String.join(" · ", parts);
            int maxGroupW = (int) (width * 0.55);
            float badgeScale = currentLabelScale;
            w += scaledBadgeWidth(font, truncate(font, joined, (int) (maxGroupW / badgeScale)), badgeScale) + 4;
        }

        return w;
    }

    private void renderGroup(GuiGraphics g, TreeNode node, int depth, int drawY, boolean hovered) {
        var font = Minecraft.getInstance().font;
        int indent = depth * INDENT;
        int rowX = x + AMITheme.GLOBAL_PADDING + indent;

        // Expansion Toggle — brighter than text to show importance
        String arrow = node.isExpanded() ? "▼" : "▶";
        int caretY = drawY + (AMITheme.ROW_HEIGHT - font.lineHeight) / 2;
        g.drawString(font, arrow, rowX, caretY, AMITheme.TEXT_PRIMARY, false);

        // Group icon
        ItemStack icon = resolveGroupIcon(node);
        if (!icon.isEmpty()) {
            int iconX = rowX + 12;
            int iconY = drawY + 1;
            g.pose().pushPose();
            g.pose().translate(0, 0, com.sanhiruzu.ami.client.overlay.OverlayLayers.SCREEN);
            g.renderItem(icon, iconX, iconY);
            g.pose().popPose();
        }

        // Count Badge — just the number, no repeated label
        String labelStr = node.getLabel().getString();
        int count = node.getItemCountOverride() != -1 ? node.getItemCountOverride() : node.getChildren().size();
        String badge = Component.translatable("ami.gui.badge_count", count).getString();
        int badgeW = (int) (font.width(badge) * currentLabelScale);
        int badgeX = x + width - SCROLLBAR_W - badgeW - 5;

        // Color swatches hidden for now — unclear to players, feature not ready
        int swatchBlockW = 0;

        // Label (truncated to avoid overlap with swatches and badge)
        int labelRightBound = badgeX - (swatchBlockW > 0 ? swatchBlockW + 8 : 0);
        int labelMaxW = labelRightBound - (rowX + 32) - 4;
        String label = truncate(font, labelStr, Math.max(0, (int) (labelMaxW / currentLabelScale)));

        int screenLabelY = drawY + (int) ((AMITheme.ROW_HEIGHT - font.lineHeight * currentLabelScale) / 2);
        g.pose().pushPose();
        g.pose().scale(currentLabelScale, currentLabelScale, 1f);

        int labelColor = depth == 0 ? AMITheme.TEXT_PRIMARY : node.isModGroup() ? AmiColors.MOD_COLOR : AMITheme.TEXT_HEADER;
        if (node.isHighCardinality()) {
            labelColor = AMITheme.GRID_GOLD_BORDER; // Gold for high-cardinality groups
        }

        g.drawString(font, label,
                Math.round((rowX + 32) / currentLabelScale), Math.round(screenLabelY / currentLabelScale),
                labelColor, depth == 0 && currentLabelScale >= 1f);
        g.pose().popPose();

        // Render badge at the same scale as the group label — higher contrast for visibility
        int badgeY = drawY + (int) ((AMITheme.ROW_HEIGHT - font.lineHeight * currentLabelScale) / 2);
        g.pose().pushPose();
        g.pose().scale(currentLabelScale, currentLabelScale, 1f);
        g.drawString(font, badge,
                Math.round(badgeX / currentLabelScale),
                Math.round(badgeY / currentLabelScale),
                AMITheme.TEXT_PRIMARY, false);
        g.pose().popPose();
    }

    private ItemStack resolveGroupIcon(TreeNode node) {
        if (node.isHighCardinality()) {
            String key = node.getKey();
            if (key.startsWith("cardinality:")) {
                String baseIdStr = key.substring(12);
                ResourceLocation baseLoc = ResourceLocation.tryParse(baseIdStr);
                if (baseLoc != null) {
                    Item item = BuiltInRegistries.ITEM.get(baseLoc);
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        return new ItemStack(item);
                    }
                }
            }
        }

        // Category-level nodes use the ontology's designated icon item.
        if (AmiOntology.isDefinedCategoryId(node.getKey())) {
            AmiOntology.Category cat = AmiOntology.categoryForId(node.getKey());
            ResourceLocation iconId = ResourceLocation.tryParse(cat.iconItemId);
            if (iconId != null) {
                Item item = BuiltInRegistries.ITEM.get(iconId);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    return new ItemStack(item);
                }
            }
            return ItemStack.EMPTY;
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

    // ── Hit-testing ───────────────────────────────────────────────────────────

    private List<Integer> collectSwatchColors(TreeNode node, int max) {
        Set<String> seen = new LinkedHashSet<>();
        collectBuckets(node, seen, max);

        List<Integer> result = new ArrayList<>();
        for (String bucket : seen) {
            result.add(AMITheme.getSwatchColor(bucket));
        }
        return result;
    }

    // ── Scrollbar ─────────────────────────────────────────────────────────────

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

    private boolean isRowHovered(int mouseX, int mouseY, int drawY) {
        return mouseX >= x && mouseX < x + width - SCROLLBAR_W
                && mouseY >= drawY && mouseY < drawY + AMITheme.ROW_HEIGHT;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void renderScrollbar(GuiGraphics g, int totalH, int contentH, int originY,
                                 int mouseX, int mouseY) {
        if (totalH <= contentH) return;

        boolean active = scrollbarDragging || isScrollbarHovered(mouseX, mouseY, totalH, contentH, originY);
        boolean vanilla = AmiConfig.theme == AmiConfig.Theme.VANILLA;
        int thumbH = vanilla ? vanillaThumbHeight(totalH, contentH) : Math.max(12, (contentH * contentH) / totalH);
        int maxScroll = totalH - contentH;
        int thumbY = originY + (pixelScrollOffset * (contentH - thumbH)) / maxScroll;

        if (vanilla) {
            int barX = x + width - SCROLLBAR_W;
            Services.PLATFORM.renderVanillaScrollbar(g, VANILLA_SCROLLER, VANILLA_SCROLLER_BACKGROUND,
                    barX, originY, SCROLLBAR_W, contentH, thumbY, thumbH);
        } else {
            int barW = active ? 6 : 4;
            int barX = x + width - 1 - barW;
            ScrollbarSpriteRenderer.renderTrack(g, x + width - SCROLLBAR_W, originY, SCROLLBAR_W, contentH);
            ScrollbarSpriteRenderer.renderThumb(g, barX, thumbY, barW, thumbH, active);
        }
    }

    private static int vanillaThumbHeight(int totalH, int contentH) {
        int max = Math.max(1, contentH - 8);
        int min = Math.min(32, max);
        return net.minecraft.util.Mth.clamp((contentH * contentH) / totalH, min, max);
    }

    private boolean isScrollbarHovered(int mouseX, int mouseY, int totalH, int contentH, int originY) {
        if (totalH <= contentH) return false;
        // Widen hitbox to 10px for easier clicking
        return mouseX >= x + width - 10 && mouseX < x + width
                && mouseY >= originY && mouseY < originY + contentH;
    }

    private void clampScroll(int totalH, int contentH) {
        int maxScroll = Math.max(0, totalH - contentH);
        pixelScrollOffset = Math.max(0, Math.min(maxScroll, pixelScrollOffset));
    }

    private int countAllNodes() {
        int count = 0;
        for (TreeNode n : rootNodes) count += countNode(n);
        return count;
    }


    // ── Input handlers ────────────────────────────────────────────────────────

    private int countNode(TreeNode node) {
        int count = 1;
        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) count += countNode(child);
        }
        return count;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;

        if (mouseX < x || mouseX >= x + width - SCROLLBAR_W) return false;

        int targetRow = (int) (mouseY - y + pixelScrollOffset) / AMITheme.ROW_HEIGHT;
        if (targetRow < 0) return false;

        int[] counter = {0};
        for (TreeNode node : rootNodes) {
            if (handleNodeClick(node, targetRow, counter, mouseX, button)) return true;
        }
        return false;
    }

    /**
     * DFS click handler. Returns true when the target row was found and handled.
     */
    private boolean handleNodeClick(TreeNode node, int targetRow, int[] counter, double mouseX, int button) {
        if (counter[0] == targetRow) {
            if (node.isLeaf()) {
                SearchNode entry = node.getEntry();
                int rightEdge = x + width - SCROLLBAR_W - 4;
                int bWidth = badgeWidth(Minecraft.getInstance().font, entry);
                int badgeStartX = rightEdge - bWidth;

                if (isTokenInjectClick(button) && onTokenInject != null) {
                    // Ctrl+right-click: inject mod name as token
                    onTokenInject.accept("@" + entry.id().getNamespace());
                } else if (onModClick != null && button == 0 && mouseX >= badgeStartX && mouseX <= rightEdge) {
                    onModClick.accept("@" + entry.id().getNamespace());
                } else if (onItemClick != null) {
                    onItemClick.accept(entry, button);
                }
            } else {
                // Group header: left-click to expand/collapse, Ctrl+right-click to inject category token
                if (button == 0) {
                    node.setExpanded(!node.isExpanded());
                } else if (isTokenInjectClick(button) && onTokenInject != null) {
                    onTokenInject.accept("$" + node.getKey());
                } else if (onGroupClick != null) {
                    onGroupClick.accept(node, button);
                }
            }
            return true;
        }
        counter[0]++;

        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                if (handleNodeClick(child, targetRow, counter, mouseX, button)) return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int contentH = lastContentH > 0 ? lastContentH : height;
        int totalH = countAllNodes() * AMITheme.ROW_HEIGHT;
        int maxScroll = Math.max(0, totalH - contentH);
        int rowsPerWheel = Math.max(1, Math.min(50, AmiConfig.listScrollRows));
        pixelScrollOffset = Math.max(0, Math.min(maxScroll,
                (int) (pixelScrollOffset - delta * AMITheme.ROW_HEIGHT * rowsPerWheel)));
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Services.PLATFORM.keyMappings().favorite().isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode))) {
            if (hoveredNode != null) {
                com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().toggleFavorite(hoveredNode);
                return true;
            }
        }
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

        int thumbH = AmiConfig.theme == AmiConfig.Theme.VANILLA
                ? vanillaThumbHeight(totalH, contentH)
                : Math.max(12, (contentH * contentH) / totalH);
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

    public SearchNode getHoveredNode() {
        return hoveredNode;
    }

    public TreeNode getHoveredTreeNode() {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        double mx = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double my = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / (double) window.getScreenHeight();

        if (!isMouseOver(mx, my)) return null;

        int topOffset = height - (lastContentH > 0 ? lastContentH : height);
        int targetRow = (int) (my - y - topOffset + pixelScrollOffset) / AMITheme.ROW_HEIGHT;
        if (targetRow < 0) return null;

        int[] counter = {0};
        for (TreeNode node : rootNodes) {
            TreeNode found = findNodeAtRow(node, targetRow, counter);
            if (found != null) return found;
        }
        return null;
    }

    private TreeNode findNodeAtRow(TreeNode node, int targetRow, int[] counter) {
        if (counter[0] == targetRow) return node;
        counter[0]++;
        if (!node.isLeaf() && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                TreeNode found = findNodeAtRow(child, targetRow, counter);
                if (found != null) return found;
            }
        }
        return null;
    }

    public int getDropIndex(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return -1;
        int contentH = lastContentH > 0 ? lastContentH : height;
        int topOffset = height - contentH;
        int relativeY = (int) mouseY - y - topOffset + pixelScrollOffset;
        int rowIndex = relativeY / AMITheme.ROW_HEIGHT;
        return Math.max(0, rowIndex);
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    private List<Component> buildTooltip(SearchNode entry) {
        return AmiTooltipComposer.buildTooltip(entry);
    }
}
