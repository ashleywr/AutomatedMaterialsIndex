package com.sanhiruzu.ami.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import com.sanhiruzu.ami.index.WorldAtlasIndex;

public class AtlasGridWidget {
    private int x, y;
    private int width, height;
    private final List<Object> entries;
    private int scrollOffset = 0;
    private String modeLabel = "Items";
    private static final int ITEM_SIZE = 16;
    private static final int PADDING = 2;
    private static final int HEADER_HEIGHT = 12;
    private static final int ENTRY_ROW_HEIGHT = 10;

    public AtlasGridWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.entries = new ArrayList<>();
    }

    public void setEntries(List<?> newEntries) {
        this.entries.clear();
        this.entries.addAll(newEntries);
        this.scrollOffset = 0;
    }

    public void setModeLabel(String label) {
        this.modeLabel = label;
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Panel background
        guiGraphics.fill(x, y, x + width, y + height, 0xCC000000);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2A2A2A);

        // Header bar
        guiGraphics.fill(x, y, x + width, y + HEADER_HEIGHT + 2, 0xFF1A3A1A);
        guiGraphics.fill(x, y + HEADER_HEIGHT + 2, x + width, y + HEADER_HEIGHT + 3, 0xFF4A6A4A);

        String headerText = modeLabel + " (" + entries.size() + ")";
        guiGraphics.drawString(Minecraft.getInstance().font, headerText, x + 3, y + 2, 0xFF88FF88, false);

        if (isItemMode()) {
            renderItemGrid(guiGraphics, mouseX, mouseY);
        } else {
            renderAtlasList(guiGraphics, mouseX, mouseY);
        }

        renderScrollBar(guiGraphics);
    }

    private boolean isItemMode() {
        return entries.isEmpty() || entries.get(0) instanceof ItemStack;
    }

    private void renderItemGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int contentY = y + HEADER_HEIGHT + 4;
        int contentHeight = height - HEADER_HEIGHT - 4;
        int itemsPerRow = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
        int visibleRows = contentHeight / (ITEM_SIZE + PADDING);

        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleRows * itemsPerRow, entries.size()); i++) {
            int row = (i - scrollOffset) / itemsPerRow;
            int col = (i - scrollOffset) % itemsPerRow;
            int drawX = x + 4 + col * (ITEM_SIZE + PADDING);
            int drawY = contentY + row * (ITEM_SIZE + PADDING);

            if (!(entries.get(i) instanceof ItemStack stack)) continue;

            guiGraphics.fill(drawX - 1, drawY - 1, drawX + ITEM_SIZE + 1, drawY + ITEM_SIZE + 1, 0xFF555555);

            boolean hovered = mouseX >= drawX && mouseX < drawX + ITEM_SIZE && mouseY >= drawY && mouseY < drawY + ITEM_SIZE;
            if (hovered) {
                guiGraphics.fill(drawX - 1, drawY - 1, drawX + ITEM_SIZE + 1, drawY + ITEM_SIZE + 1, 0xFFAAAAAA);
            }

            guiGraphics.renderItem(stack, drawX, drawY);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, drawX, drawY);

            if (hovered) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
            }
        }
    }

    private void renderAtlasList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int contentY = y + HEADER_HEIGHT + 4;
        int contentHeight = height - HEADER_HEIGHT - 4;
        int visibleRows = contentHeight / (ENTRY_ROW_HEIGHT + 1);

        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleRows, entries.size()); i++) {
            if (!(entries.get(i) instanceof WorldAtlasIndex.AtlasEntry atlasEntry)) continue;

            int drawY = contentY + (i - scrollOffset) * (ENTRY_ROW_HEIGHT + 1);
            boolean hovered = mouseX >= x + 2 && mouseX < x + width - 6 && mouseY >= drawY && mouseY < drawY + ENTRY_ROW_HEIGHT;

            if (hovered) {
                guiGraphics.fill(x + 2, drawY, x + width - 6, drawY + ENTRY_ROW_HEIGHT, 0xFF3A5A3A);
            }

            String label = atlasEntry.name();
            // truncate to fit
            int maxChars = (width - 14) / 5;
            if (label.length() > maxChars) label = label.substring(0, maxChars - 1) + "…";
            guiGraphics.drawString(Minecraft.getInstance().font, label, x + 4, drawY + 1, 0xFFCCCCCC, false);

            if (hovered) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font,
                        net.minecraft.network.chat.Component.literal(atlasEntry.id().toString()),
                        mouseX, mouseY);
            }
        }
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        int totalEntries = entries.size();
        if (totalEntries == 0) return;

        int contentHeight = height - HEADER_HEIGHT - 4;
        int visibleCount = isItemMode()
                ? Math.max(1, (width - 12) / (ITEM_SIZE + PADDING)) * (contentHeight / (ITEM_SIZE + PADDING))
                : contentHeight / (ENTRY_ROW_HEIGHT + 1);

        if (totalEntries <= visibleCount) return;

        int barX = x + width - 4;
        int barAreaY = y + HEADER_HEIGHT + 4;
        int barHeight = Math.max(10, (visibleCount * contentHeight) / totalEntries);
        int barY = barAreaY + (scrollOffset * (contentHeight - barHeight)) / (totalEntries - visibleCount);

        guiGraphics.fill(barX, barAreaY, barX + 3, barAreaY + contentHeight, 0xFF333333);
        guiGraphics.fill(barX, barY, barX + 3, barY + barHeight, 0xFF88AA88);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int contentHeight = height - HEADER_HEIGHT - 4;
        int maxScroll;

        if (isItemMode()) {
            int itemsPerRow = Math.max(1, (width - 12) / (ITEM_SIZE + PADDING));
            int visibleRows = contentHeight / (ITEM_SIZE + PADDING);
            maxScroll = Math.max(0, (entries.size() + itemsPerRow - 1) / itemsPerRow - visibleRows);
        } else {
            int visibleRows = contentHeight / (ENTRY_ROW_HEIGHT + 1);
            maxScroll = Math.max(0, entries.size() - visibleRows);
        }

        scrollOffset = Math.max(0, Math.min(maxScroll, (int) (scrollOffset - scrollDelta)));
        return true;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getEntryCount() { return entries.size(); }
}
