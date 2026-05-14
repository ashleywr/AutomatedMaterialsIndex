package com.sanhiruzu.ami.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.sanhiruzu.ami.index.WorldAtlasIndex;
import com.sanhiruzu.ami.index.WorldAtlasIndexer;

public class AtlasGridWidget {
    public enum Mode { ITEMS, ATLAS }

    private static final int ITEM_SIZE     = 16;
    private static final int PADDING       = 2;
    private static final int HEADER_HEIGHT = 12;
    private static final int ROW_HEIGHT    = 11;
    private static final int SWATCH_SIZE   = 6;
    private static final int SWATCH_GAP    = 3;
    private static final int DIM_BADGE     = 4;

    // Dimension badge colours
    private static final int COL_NETHER = 0xFFCC4444;
    private static final int COL_END    = 0xFF9944CC;

    private int x, y, width, height;
    private Mode mode = Mode.ITEMS;
    private String modeLabel = "Items";

    private final List<ItemStack> itemEntries = new ArrayList<>();

    /** One group per namespace in the atlas list. */
    static final class AtlasGroup {
        final String namespace;
        final String displayName;
        final List<WorldAtlasIndex.AtlasEntry> entries = new ArrayList<>();
        boolean expanded = true;

        AtlasGroup(String namespace) {
            this.namespace = namespace;
            this.displayName = WorldAtlasIndexer.modDisplayName(namespace);
        }
    }

    private final List<AtlasGroup> atlasGroups = new ArrayList<>();

    private int scrollOffset = 0;

    // Deferred tooltips — collected during render, drawn last
    private ItemStack pendingItemTooltip = null;
    private Component pendingTextTooltip = null;

    public AtlasGridWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // -------------------------------------------------------------------------
    // Data setters
    // -------------------------------------------------------------------------

    public void setItemEntries(List<ItemStack> items) {
        itemEntries.clear();
        itemEntries.addAll(items);
        mode = Mode.ITEMS;
        scrollOffset = 0;
    }

    public void setAtlasEntries(List<WorldAtlasIndex.AtlasEntry> entries, String label) {
        // Preserve which groups the user has already collapsed
        Set<String> collapsed = new HashSet<>();
        for (AtlasGroup g : atlasGroups) {
            if (!g.expanded) collapsed.add(g.namespace);
        }

        atlasGroups.clear();
        Map<String, AtlasGroup> byNamespace = new LinkedHashMap<>();
        for (WorldAtlasIndex.AtlasEntry entry : entries) {
            byNamespace.computeIfAbsent(entry.id().getNamespace(), AtlasGroup::new).entries.add(entry);
        }
        atlasGroups.addAll(byNamespace.values());

        for (AtlasGroup g : atlasGroups) {
            if (collapsed.contains(g.namespace)) g.expanded = false;
        }

        mode = Mode.ATLAS;
        modeLabel = label;
        scrollOffset = 0;
    }

    public void setItemModeLabel(String label) {
        modeLabel = label;
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        pendingItemTooltip = null;
        pendingTextTooltip = null;

        // Panel background
        g.fill(x, y, x + width, y + height, 0xCC000000);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2A2A2A);

        // Header bar
        g.fill(x, y, x + width, y + HEADER_HEIGHT + 2, 0xFF1A3A1A);
        g.fill(x, y + HEADER_HEIGHT + 2, x + width, y + HEADER_HEIGHT + 3, 0xFF4A6A4A);
        g.drawString(Minecraft.getInstance().font,
                modeLabel + " (" + entryCount() + ")", x + 3, y + 2, 0xFF88FF88, false);

        if (mode == Mode.ITEMS) {
            renderItemGrid(g, mouseX, mouseY);
        } else {
            renderAtlasList(g, mouseX, mouseY);
        }

        renderScrollBar(g);

        if (pendingItemTooltip != null) {
            g.renderTooltip(Minecraft.getInstance().font, pendingItemTooltip, mouseX, mouseY);
        } else if (pendingTextTooltip != null) {
            g.renderTooltip(Minecraft.getInstance().font, pendingTextTooltip, mouseX, mouseY);
        }
    }

    private void renderItemGrid(GuiGraphics g, int mouseX, int mouseY) {
        int contentY = y + HEADER_HEIGHT + 4;
        int contentH = height - HEADER_HEIGHT - 4;
        int perRow   = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
        int visRows  = contentH / (ITEM_SIZE + PADDING);

        for (int i = scrollOffset; i < Math.min(scrollOffset + visRows * perRow, itemEntries.size()); i++) {
            int row  = (i - scrollOffset) / perRow;
            int col  = (i - scrollOffset) % perRow;
            int drawX = x + 4 + col * (ITEM_SIZE + PADDING);
            int drawY = contentY + row * (ITEM_SIZE + PADDING);
            ItemStack stack = itemEntries.get(i);

            g.fill(drawX - 1, drawY - 1, drawX + ITEM_SIZE + 1, drawY + ITEM_SIZE + 1, 0xFF555555);
            boolean hovered = mouseX >= drawX && mouseX < drawX + ITEM_SIZE
                    && mouseY >= drawY && mouseY < drawY + ITEM_SIZE;
            if (hovered) {
                g.fill(drawX - 1, drawY - 1, drawX + ITEM_SIZE + 1, drawY + ITEM_SIZE + 1, 0xFFAAAAAA);
                pendingItemTooltip = stack;
            }
            g.renderItem(stack, drawX, drawY);
            g.renderItemDecorations(Minecraft.getInstance().font, stack, drawX, drawY);
        }
    }

    private void renderAtlasList(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int contentY = y + HEADER_HEIGHT + 4;
        int contentH = height - HEADER_HEIGHT - 4;
        int visRows  = contentH / ROW_HEIGHT;

        int textStartX = x + SWATCH_GAP + SWATCH_SIZE + SWATCH_GAP;
        int maxTextW   = width - (textStartX - x) - DIM_BADGE - 6;

        int row = 0;
        for (AtlasGroup group : atlasGroups) {
            // --- group header ---
            if (row >= scrollOffset && row < scrollOffset + visRows) {
                int drawY = contentY + (row - scrollOffset) * ROW_HEIGHT;
                boolean hovered = isRowHovered(mouseX, mouseY, drawY);

                g.fill(x + 1, drawY, x + width - 5, drawY + ROW_HEIGHT - 1,
                        hovered ? 0xFF2A4A2A : 0xFF1E3A1E);

                String arrow = group.expanded ? "▼ " : "▶ ";
                String label = arrow + group.displayName + " (" + group.entries.size() + ")";
                g.drawString(font, label, x + 4, drawY + 2, 0xFF99DD99, false);
            }
            row++;

            if (!group.expanded) continue;

            // --- group entries ---
            for (WorldAtlasIndex.AtlasEntry entry : group.entries) {
                if (row >= scrollOffset && row < scrollOffset + visRows) {
                    int drawY = contentY + (row - scrollOffset) * ROW_HEIGHT;
                    boolean hovered = isRowHovered(mouseX, mouseY, drawY);

                    if (hovered) {
                        g.fill(x + 2, drawY, x + width - 6, drawY + ROW_HEIGHT - 1, 0xFF3A5A3A);
                        pendingTextTooltip = buildTooltip(entry);
                    }

                    // Water-color swatch
                    int swatchY = drawY + (ROW_HEIGHT - SWATCH_SIZE) / 2;
                    g.fill(x + SWATCH_GAP, swatchY,
                            x + SWATCH_GAP + SWATCH_SIZE, swatchY + SWATCH_SIZE, entry.color());

                    // Dimension badge (top-right of the row, only for non-overworld)
                    if (entry.dimension() != WorldAtlasIndex.Dimension.OVERWORLD) {
                        int badgeColor = entry.dimension() == WorldAtlasIndex.Dimension.NETHER
                                ? COL_NETHER : COL_END;
                        int badgeX = x + width - DIM_BADGE - 6;
                        int badgeY = drawY + (ROW_HEIGHT - DIM_BADGE) / 2;
                        g.fill(badgeX, badgeY, badgeX + DIM_BADGE, badgeY + DIM_BADGE, badgeColor);
                    }

                    // Name — truncate to fit between swatch and badge
                    String name = entry.name();
                    while (font.width(name) > maxTextW && name.length() > 1) {
                        name = name.substring(0, name.length() - 1);
                    }
                    if (font.width(entry.name()) > maxTextW) name += "…";
                    g.drawString(font, name, textStartX, drawY + 2, 0xFFCCCCCC, false);
                }
                row++;
            }
        }
    }

    private boolean isRowHovered(int mouseX, int mouseY, int drawY) {
        return mouseX >= x + 2 && mouseX < x + width - 5
                && mouseY >= drawY && mouseY < drawY + ROW_HEIGHT - 1;
    }

    private Component buildTooltip(WorldAtlasIndex.AtlasEntry entry) {
        String dimLabel = switch (entry.dimension()) {
            case NETHER -> " §c[Nether]§r";
            case END    -> " §5[End]§r";
            default     -> "";
        };
        return Component.literal(entry.id().toString() + dimLabel)
                .append(Component.literal("\n" + WorldAtlasIndexer.modDisplayName(entry.id().getNamespace()))
                        .withStyle(s -> s.withColor(0x888888)));
    }

    private void renderScrollBar(GuiGraphics g) {
        int total = totalRows();
        if (total == 0) return;

        int contentH = height - HEADER_HEIGHT - 4;
        int visible  = visibleRowCount(contentH);
        if (total <= visible) return;

        int barX      = x + width - 4;
        int barAreaY  = y + HEADER_HEIGHT + 4;
        int barHeight = Math.max(10, (visible * contentH) / total);
        int barY      = barAreaY + (scrollOffset * (contentH - barHeight)) / (total - visible);

        g.fill(barX, barAreaY, barX + 3, barAreaY + contentH, 0xFF333333);
        g.fill(barX, barY,     barX + 3, barY + barHeight,    0xFF88AA88);
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || mode != Mode.ATLAS) return false;

        int contentY = y + HEADER_HEIGHT + 4;
        int contentH = height - HEADER_HEIGHT - 4;
        int visRows  = contentH / ROW_HEIGHT;

        // Find which logical row was clicked
        int clickedLogicalRow = scrollOffset + (int) ((mouseY - contentY) / ROW_HEIGHT);

        int row = 0;
        for (AtlasGroup group : atlasGroups) {
            if (row == clickedLogicalRow) {
                // Clicked a group header — toggle
                group.expanded = !group.expanded;
                // Clamp scroll so we don't end up past the end
                int maxScroll = Math.max(0, totalRows() - visRows);
                scrollOffset = Math.min(scrollOffset, maxScroll);
                return true;
            }
            row++;
            if (group.expanded) row += group.entries.size();
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int contentH = height - HEADER_HEIGHT - 4;
        int maxScroll = Math.max(0, totalRows() - visibleRowCount(contentH));
        scrollOffset = Math.max(0, Math.min(maxScroll, (int) (scrollOffset - scrollDelta)));
        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Total logical rows including group headers and expanded entries. */
    private int totalRows() {
        if (mode == Mode.ITEMS) {
            int perRow = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
            return (itemEntries.size() + perRow - 1) / perRow;
        }
        int rows = 0;
        for (AtlasGroup g : atlasGroups) {
            rows++; // header
            if (g.expanded) rows += g.entries.size();
        }
        return rows;
    }

    private int visibleRowCount(int contentH) {
        if (mode == Mode.ITEMS) {
            int perRow = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
            return perRow * (contentH / (ITEM_SIZE + PADDING));
        }
        return contentH / ROW_HEIGHT;
    }

    /** Total flat entry count (for the header label). */
    private int entryCount() {
        if (mode == Mode.ITEMS) return itemEntries.size();
        return atlasGroups.stream().mapToInt(g -> g.entries.size()).sum();
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getWidth()      { return width; }
    public int getHeight()     { return height; }
    public int getEntryCount() { return entryCount(); }
}
