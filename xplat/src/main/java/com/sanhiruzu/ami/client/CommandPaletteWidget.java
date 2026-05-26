package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.providers.RegistryUtils;
import com.sanhiruzu.ami.util.AmiClipboardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spotlight/Cmd+P style command palette for global search across all NodeTypes.
 * Renders a centered search bar with rich result cards below.
 * Manages fade in/out animation, scrolling, and keyboard/mouse input.
 */
public class CommandPaletteWidget {
    // Layout constants
    private static final int SEARCH_BAR_HEIGHT = 20;
    private static final int SEARCH_BAR_WIDTH = 400;
    private static final int CARD_HEIGHT = 24;
    private static final int PADDING = 8;
    private static final int ICON_SIZE = 16;
    private static final int SCROLL_BAR_WIDTH = 6;

    private int screenWidth;
    private int screenHeight;

    // Search state
    private String searchQuery = "";
    private boolean focused = false;
    private float alpha = 0f;
    private static final float FADE_SPEED = 0.1f;  // alpha change per frame

    // Results
    private Map<NodeType, List<SearchNode>> currentResults = new LinkedHashMap<>();
    private final List<ResultGroup> resultGroups = new ArrayList<>();

    // Scrolling
    private int scrollOffset = 0;
    private boolean scrollbarDragging = false;

    // Deferred tooltip
    private List<Component> pendingTooltipLines = null;

    static final class ResultGroup {
        final String namespace;
        final String displayName;
        final NodeType type;
        final List<SearchNode> entries = new ArrayList<>();
        boolean expanded = true;

        ResultGroup(NodeType type, String displayName) {
            this.namespace = type.name().toLowerCase();
            this.displayName = displayName;
            this.type = type;
        }
    }

    public CommandPaletteWidget(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    public void setSearchResults(Map<NodeType, List<SearchNode>> results, String query) {
        this.currentResults = new LinkedHashMap<>(results);
        this.searchQuery = query;
        rebuildGroups();
        this.scrollOffset = 0;
    }

    public void clearSearch() {
        this.searchQuery = "";
        this.currentResults.clear();
        this.resultGroups.clear();
        this.scrollOffset = 0;
    }

    public void typeCharacter(char c) {
        if (c >= 32 && c < 127) {
            searchQuery += c;
        }
    }

    public void deleteSearchChar() {
        if (!searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void updateScreenSize(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Update alpha based on focused state
        float targetAlpha = focused ? 1f : 0f;
        if (alpha < targetAlpha) {
            alpha = Math.min(alpha + FADE_SPEED, targetAlpha);
        } else if (alpha > targetAlpha) {
            alpha = Math.max(alpha - FADE_SPEED, targetAlpha);
        }

        // If fully faded out, don't render anything
        if (alpha <= 0.01f) {
            return;
        }

        pendingTooltipLines = null;

        // Draw overlay
        drawOverlay(g);

        // Draw search bar
        int searchBarX = (screenWidth - SEARCH_BAR_WIDTH) / 2;
        int searchBarY = screenHeight / 5;
        drawSearchBar(g, searchBarX, searchBarY);

        // Draw results
        int resultsY = searchBarY + SEARCH_BAR_HEIGHT + PADDING;
        int resultsH = screenHeight - resultsY - PADDING;
        drawResults(g, mouseX, mouseY, searchBarX, resultsY, resultsH);

        // Draw deferred tooltip
        if (pendingTooltipLines != null && !pendingTooltipLines.isEmpty()) {
            var font = Minecraft.getInstance().font;
            int tooltipY = mouseY;
            for (Component line : pendingTooltipLines) {
                g.drawString(font, line, mouseX + 10, tooltipY, com.sanhiruzu.ami.client.AMITheme.WHITE, true);
                tooltipY += 10;
            }
        }
    }

    private void drawOverlay(GuiGraphics g) {
        int overlayColor = AmiConfig.overlayBg;
        int a = (int) (((overlayColor >> 24) & 0xFF) * alpha);
        int rgb = overlayColor & com.sanhiruzu.ami.client.AMITheme.WHITE;
        int color = (a << 24) | rgb;
        g.fill(0, 0, screenWidth, screenHeight, color);
    }

    private void drawSearchBar(GuiGraphics g, int x, int y) {
        int w = SEARCH_BAR_WIDTH;
        int h = SEARCH_BAR_HEIGHT;

        // Draw elegant blue accent line right above the search bar
        g.fill(x, y - 4, x + w, y - 2, com.sanhiruzu.ami.client.AMITheme.ACCENT_BLUE);

        // Rounded search bar background and border
        com.sanhiruzu.ami.client.AMITheme.drawRoundedBorder(g, x, y, w, h, AmiConfig.searchBarBg, AmiConfig.searchBarBorder);

        var font = Minecraft.getInstance().font;
        Component displayText = searchQuery.isEmpty()
                ? Component.translatable("ami.gui.search.placeholder")
                : Component.literal(searchQuery);

        int textColor = searchQuery.isEmpty() ? AmiConfig.searchPlaceholder : AmiConfig.searchText;

        g.drawString(font, displayText, x + 6, y + 5, textColor, false);

        // Blinking cursor
        if (!searchQuery.isEmpty() && (System.currentTimeMillis() % 1000) < 500) {
            int cursorX = x + 6 + font.width(displayText);
            g.fill(cursorX, y + 4, cursorX + 1, y + 16, AmiConfig.searchText);
        }

        // Clear button [x]
        if (!searchQuery.isEmpty()) {
            int xX = x + w - 12;
            int xY = y + 6;
            g.drawString(font, Component.translatable("ami.gui.search.clear"), xX, xY, AmiConfig.searchText, false);
        }
    }

    private void drawResults(GuiGraphics g, int mouseX, int mouseY, int x, int y, int h) {
        if (resultGroups.isEmpty()) {
            var font = Minecraft.getInstance().font;
            Component msg = searchQuery.isEmpty()
                    ? Component.translatable("ami.gui.search.typing")
                    : Component.translatable("ami.gui.no_results");
            g.drawString(font, msg, x + PADDING, y + PADDING, com.sanhiruzu.ami.client.AMITheme.ACCENT_GOLD, false);
            return;
        }

        int cardW = SEARCH_BAR_WIDTH;
        int visibleCards = Math.max(1, h / CARD_HEIGHT);

        int row = 0;
        for (ResultGroup group : resultGroups) {
            // Group header
            if (row >= scrollOffset && row < scrollOffset + visibleCards) {
                int drawY = y + (row - scrollOffset) * CARD_HEIGHT;
                boolean hovered = isCardHovered(mouseX, mouseY, x, drawY, cardW);
                if (hovered) {
                    com.sanhiruzu.ami.client.AMITheme.fillRounded(g, x + 2, drawY + 2, cardW - 4, CARD_HEIGHT - 4, AmiConfig.cardBgHover);
                }
                String arrow = group.expanded ? "▼ " : "▶ ";
                // Assuming palette had group header color, using a default or adding to AmiConfig if needed
                // Using card name color for now as a fallback
                g.drawString(Minecraft.getInstance().font, arrow + group.displayName + " (" + group.entries.size() + ")",
                        x + PADDING, drawY + 6, com.sanhiruzu.ami.client.AMITheme.GROUP_HEADER_TEXT, false);
            }
            row++;

            if (!group.expanded) continue;

            // Group entries
            for (SearchNode entry : group.entries) {
                if (row >= scrollOffset && row < scrollOffset + visibleCards) {
                    int drawY = y + (row - scrollOffset) * CARD_HEIGHT;
                    drawResultCard(g, x, drawY, cardW, entry, mouseX, mouseY);
                }
                row++;
            }
        }

        // Draw scrollbar if needed
        int totalRows = countTotalRows();
        if (totalRows > visibleCards) {
            drawScrollbar(g, x + SEARCH_BAR_WIDTH - SCROLL_BAR_WIDTH, y, SCROLL_BAR_WIDTH, h, totalRows, visibleCards);
        }
    }

    private void drawResultCard(GuiGraphics g, int x, int y, int w, SearchNode entry, int mouseX, int mouseY) {
        boolean hovered = isCardHovered(mouseX, mouseY, x, y, w);

        if (hovered) {
            com.sanhiruzu.ami.client.AMITheme.fillRounded(g, x + 2, y + 2, w - 4, CARD_HEIGHT - 4, AmiConfig.cardBgHover);
            pendingTooltipLines = buildTooltip(entry);
        } else {
            com.sanhiruzu.ami.client.AMITheme.fillRounded(g, x + 2, y + 2, w - 4, CARD_HEIGHT - 4, AmiConfig.cardBg);
        }

        // Icon
        int iconX = x + PADDING;
        int iconY = y + (CARD_HEIGHT - ICON_SIZE) / 2;
        if (entry.type() == NodeType.ITEM) {
            BuiltInRegistries.ITEM.getOptional(entry.id())
                    .ifPresent(item -> {
                        ItemStack stack = new ItemStack(item);
                        g.renderItem(stack, iconX, iconY);
                    });
        } else {
            g.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, entry.color());
        }

        // Name
        int textX = iconX + ICON_SIZE + PADDING;
        g.drawString(Minecraft.getInstance().font, entry.displayName(), textX, y + 4, AmiConfig.cardTextName, false);

        // Subtitle (mod name or dimension)
        String subtitle = entry.id().getNamespace();
        if (entry.type() == NodeType.BIOME || entry.type() == NodeType.ENTITY) {
            subtitle = RegistryUtils.modDisplayName(subtitle);
        }
        g.drawString(Minecraft.getInstance().font, subtitle, textX, y + 14, AmiConfig.cardTextSubtitle, false);

        // Action hint (right-aligned)
        if (entry.type() == NodeType.ITEM && InventoryOverlayHandler.RECIPE_VIEWER_PRESENT) {
            Component hint = Component.translatable("ami.gui.search.recipe");
            int hintW = Minecraft.getInstance().font.width(hint);
            // Fallback action hint color
            g.drawString(Minecraft.getInstance().font, hint, x + w - hintW - PADDING, y + 6, com.sanhiruzu.ami.client.AMITheme.TEXT_SUBTLE, false);
        }
    }

    private void drawScrollbar(GuiGraphics g, int x, int y, int w, int h, int totalRows, int visibleRows) {
        // Fallback scrollbar colors
        g.fill(x, y, x + w, y + h, com.sanhiruzu.ami.client.AMITheme.SCROLL_TRACK);

        // Thumb
        int thumbH = Math.max(10, (h * visibleRows) / totalRows);
        int thumbY = y + (h * scrollOffset) / totalRows;
        boolean thumbHovered = scrollbarDragging || (true);  // TODO: hover detection
        int thumbColor = thumbHovered ? com.sanhiruzu.ami.client.AMITheme.SCROLL_THUMB_ACTIVE : com.sanhiruzu.ami.client.AMITheme.SCROLL_THUMB;
        g.fill(x, thumbY, x + w, thumbY + thumbH, thumbColor);
    }

    private List<Component> buildTooltip(SearchNode entry) {
        if (entry.type() == NodeType.ITEM) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(entry.displayName()).withStyle(s -> s.withColor(com.sanhiruzu.ami.client.AMITheme.WHITE)));
            lines.add(Component.literal(entry.id().toString()).withStyle(s -> s.withColor(com.sanhiruzu.ami.client.AMITheme.TEXT_SUBTLE)));
            return lines;
        }
        return RendererRegistry.get(entry.type()).getTooltip(entry);
    }

    private boolean isCardHovered(int mouseX, int mouseY, int x, int y, int w) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + CARD_HEIGHT;
    }

    private int countTotalRows() {
        int count = 0;
        for (ResultGroup group : resultGroups) {
            count++;  // header
            if (group.expanded) {
                count += group.entries.size();
            }
        }
        return count;
    }

    private void rebuildGroups() {
        resultGroups.clear();
        for (var entry : currentResults.entrySet()) {
            NodeType type = entry.getKey();
            List<SearchNode> nodes = entry.getValue();
            if (nodes.isEmpty()) continue;

            ResultGroup group = new ResultGroup(type, type.displayName().getString());
            group.entries.addAll(nodes);
            resultGroups.add(group);
        }
    }

    // =========================================================================
    // Input Handling (keyboard/mouse hooks from InventoryOverlayHandler)
    // =========================================================================

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!focused || alpha <= 0.01f) return false;

        int searchBarX = (screenWidth - SEARCH_BAR_WIDTH) / 2;
        int searchBarY = screenHeight / 5;

        // Check search bar click
        if (mouseX >= searchBarX && mouseX < searchBarX + SEARCH_BAR_WIDTH &&
                mouseY >= searchBarY && mouseY < searchBarY + SEARCH_BAR_HEIGHT) {
            // Click on search bar itself (already focused)
            // Check for [x] clear button
            if (mouseX >= searchBarX + SEARCH_BAR_WIDTH - 15 && !searchQuery.isEmpty()) {
                searchQuery = "";
                currentResults.clear();
                resultGroups.clear();
                return true;
            }
            return true;
        }

        // Check result card clicks
        int resultsY = searchBarY + SEARCH_BAR_HEIGHT + PADDING;
        int cardW = SEARCH_BAR_WIDTH;
        int visibleCards = Math.max(1, (screenHeight - resultsY - PADDING) / CARD_HEIGHT);

        int row = 0;
        for (ResultGroup group : resultGroups) {
            if (row >= scrollOffset && row < scrollOffset + visibleCards) {
                int drawY = resultsY + (row - scrollOffset) * CARD_HEIGHT;
                if (isCardHovered((int) mouseX, (int) mouseY, searchBarX, drawY, cardW)) {
                    // Click on group header
                    group.expanded = !group.expanded;
                    return true;
                }
            }
            row++;

            if (!group.expanded) continue;

            for (SearchNode entry : group.entries) {
                if (row >= scrollOffset && row < scrollOffset + visibleCards) {
                    int drawY = resultsY + (row - scrollOffset) * CARD_HEIGHT;
                    if (isCardHovered((int) mouseX, (int) mouseY, searchBarX, drawY, cardW)) {
                        // Click on result card - check for recipe hint
                        if (entry.type() == NodeType.ITEM && InventoryOverlayHandler.RECIPE_VIEWER_PRESENT) {
                            int hintX = searchBarX + cardW - 60;
                            if (mouseX >= hintX) {
                                // Clicked recipe hint
                                BuiltInRegistries.ITEM.getOptional(entry.id())
                                        .ifPresent(item -> {
                                            // TODO: Call RecipeViewerBridge.showRecipes(new ItemStack(item))
                                        });
                                return true;
                            }
                        }
                        return true;
                    }
                }
                row++;
            }
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!focused || resultGroups.isEmpty()) return false;

        int totalRows = countTotalRows();
        int visibleCards = Math.max(1, (screenHeight - (screenHeight / 5 + SEARCH_BAR_HEIGHT + PADDING * 2)) / CARD_HEIGHT);

        if (totalRows <= visibleCards) return false;

        scrollOffset -= (int) scrollDelta;
        scrollOffset = Math.max(0, Math.min(scrollOffset, totalRows - visibleCards));
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused || alpha <= 0.01f) return false;

        if (keyCode == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            if (pendingTooltipLines != null && !pendingTooltipLines.isEmpty()) {
                AmiClipboardHelper.copyComponentsToClipboard(pendingTooltipLines);
                return true;
            }
        }
        return false;
    }
}
