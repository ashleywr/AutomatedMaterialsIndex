package com.sanhiruzu.ami.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.sanhiruzu.ami.index.WorldAtlasIndex;

public class AtlasGridWidget {
    public enum Mode { ITEMS, ATLAS }

    private int x, y;
    private int width, height;
    private Mode mode = Mode.ITEMS;
    private String modeLabel = "Items";
    private final List<ItemStack> itemEntries = new ArrayList<>();
    private final List<WorldAtlasIndex.AtlasEntry> atlasEntries = new ArrayList<>();
    private int scrollOffset = 0;

    // Deferred tooltip — collected during render pass, drawn last
    private ItemStack pendingItemTooltip = null;
    private Component pendingTextTooltip = null;

    private static final int ITEM_SIZE = 16;
    private static final int PADDING = 2;
    private static final int HEADER_HEIGHT = 12;
    private static final int ENTRY_ROW_HEIGHT = 10;

    public AtlasGridWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setItemEntries(List<ItemStack> items) {
        this.itemEntries.clear();
        this.itemEntries.addAll(items);
        this.mode = Mode.ITEMS;
        this.scrollOffset = 0;
    }

    public void setAtlasEntries(List<WorldAtlasIndex.AtlasEntry> entries, String label) {
        this.atlasEntries.clear();
        this.atlasEntries.addAll(entries);
        this.mode = Mode.ATLAS;
        this.modeLabel = label;
        this.scrollOffset = 0;
    }

    public void setItemModeLabel(String label) {
        this.modeLabel = label;
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        pendingItemTooltip = null;
        pendingTextTooltip = null;

        guiGraphics.fill(x, y, x + width, y + height, 0xCC000000);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2A2A2A);

        guiGraphics.fill(x, y, x + width, y + HEADER_HEIGHT + 2, 0xFF1A3A1A);
        guiGraphics.fill(x, y + HEADER_HEIGHT + 2, x + width, y + HEADER_HEIGHT + 3, 0xFF4A6A4A);
        guiGraphics.drawString(Minecraft.getInstance().font,
                modeLabel + " (" + entryCount() + ")", x + 3, y + 2, 0xFF88FF88, false);

        if (mode == Mode.ITEMS) {
            renderItemGrid(guiGraphics, mouseX, mouseY);
        } else {
            renderAtlasList(guiGraphics, mouseX, mouseY);
        }

        renderScrollBar(guiGraphics);

        if (pendingItemTooltip != null) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, pendingItemTooltip, mouseX, mouseY);
        } else if (pendingTextTooltip != null) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, pendingTextTooltip, mouseX, mouseY);
        }
    }

    private void renderItemGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int contentY = y + HEADER_HEIGHT + 4;
        int contentHeight = height - HEADER_HEIGHT - 4;
        int itemsPerRow = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
        int visibleRows = contentHeight / (ITEM_SIZE + PADDING);

        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleRows * itemsPerRow, itemEntries.size()); i++) {
            int row = (i - scrollOffset) / itemsPerRow;
            int col = (i - scrollOffset) % itemsPerRow;
            int drawX = x + 4 + col * (ITEM_SIZE + PADDING);
            int drawY = contentY + row * (ITEM_SIZE + PADDING);
            ItemStack stack = itemEntries.get(i);

            guiGraphics.fill(drawX - 1, drawY - 1, drawX + ITEM_SIZE + 1, drawY + ITEM_SIZE + 1, 0xFF555555);

            boolean hovered = mouseX >= drawX && mouseX < drawX + ITEM_SIZE
                    && mouseY >= drawY && mouseY < drawY + ITEM_SIZE;
            if (hovered) {
                guiGraphics.fill(drawX - 1, drawY - 1, drawX + ITEM_SIZE + 1, drawY + ITEM_SIZE + 1, 0xFFAAAAAA);
                pendingItemTooltip = stack;
            }

            guiGraphics.renderItem(stack, drawX, drawY);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, drawX, drawY);
        }
    }

    private void renderAtlasList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int contentY = y + HEADER_HEIGHT + 4;
        int contentHeight = height - HEADER_HEIGHT - 4;
        int visibleRows = contentHeight / (ENTRY_ROW_HEIGHT + 1);

        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleRows, atlasEntries.size()); i++) {
            WorldAtlasIndex.AtlasEntry entry = atlasEntries.get(i);
            int drawY = contentY + (i - scrollOffset) * (ENTRY_ROW_HEIGHT + 1);
            boolean hovered = mouseX >= x + 2 && mouseX < x + width - 6
                    && mouseY >= drawY && mouseY < drawY + ENTRY_ROW_HEIGHT;

            if (hovered) {
                guiGraphics.fill(x + 2, drawY, x + width - 6, drawY + ENTRY_ROW_HEIGHT, 0xFF3A5A3A);
                pendingTextTooltip = Component.literal(entry.id().toString());
            }

            String label = entry.name();
            int maxChars = (width - 14) / 5;
            if (label.length() > maxChars) label = label.substring(0, maxChars - 1) + "…";
            guiGraphics.drawString(Minecraft.getInstance().font, label, x + 4, drawY + 1, 0xFFCCCCCC, false);
        }
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        int total = entryCount();
        if (total == 0) return;

        int contentHeight = height - HEADER_HEIGHT - 4;
        int visible = visibleCount(contentHeight);
        if (total <= visible) return;

        int barX = x + width - 4;
        int barAreaY = y + HEADER_HEIGHT + 4;
        int barHeight = Math.max(10, (visible * contentHeight) / total);
        int barY = barAreaY + (scrollOffset * (contentHeight - barHeight)) / (total - visible);

        guiGraphics.fill(barX, barAreaY, barX + 3, barAreaY + contentHeight, 0xFF333333);
        guiGraphics.fill(barX, barY, barX + 3, barY + barHeight, 0xFF88AA88);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int contentHeight = height - HEADER_HEIGHT - 4;
        int maxScroll = Math.max(0, entryCount() - visibleCount(contentHeight));
        scrollOffset = Math.max(0, Math.min(maxScroll, (int) (scrollOffset - scrollDelta)));
        return true;
    }

    private int entryCount() {
        return mode == Mode.ITEMS ? itemEntries.size() : atlasEntries.size();
    }

    private int visibleCount(int contentHeight) {
        if (mode == Mode.ITEMS) {
            int itemsPerRow = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
            return itemsPerRow * (contentHeight / (ITEM_SIZE + PADDING));
        }
        return contentHeight / (ENTRY_ROW_HEIGHT + 1);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getEntryCount() { return entryCount(); }
}
