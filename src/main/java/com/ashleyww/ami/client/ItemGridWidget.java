package com.ashleyww.ami.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.ashleyww.ami.AMI;
import com.ashleyww.ami.index.AMIIndex;
import com.ashleyww.ami.index.IndexCategory;
import com.ashleyww.ami.index.MaterialEntry;

public class ItemGridWidget {
    private int x, y;
    private final int width, height;
    private final List<ItemStack> items;
    private int scrollOffset = 0;
    private static final int ITEM_SIZE = 18;
    private static final int PADDING = 2;

    public ItemGridWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.items = new ArrayList<>();

        loadItems();
        AMI.LOGGER.debug("ItemGridWidget created with {} items", items.size());
    }

    private void loadItems() {
        items.clear();

        AMIIndex index = AMIIndex.getInstance();
        if (index.getTotalItemsIndexed() == 0) {
            AMI.LOGGER.debug("Loading items from registry (no index data available)");
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != null && !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("air")) {
                    items.add(new ItemStack(item));
                    if (items.size() >= 100) break;
                }
            }
        } else {
            AMI.LOGGER.debug("Loading items from AMI index");
            var categoryIndex = index.getCategoryIndex(IndexCategory.BY_MOD);
            for (List<MaterialEntry> entries : categoryIndex.values()) {
                for (MaterialEntry entry : entries) {
                    items.add(new ItemStack(entry.item()));
                    if (items.size() >= 200) break;
                }
                if (items.size() >= 200) break;
            }
        }
        AMI.LOGGER.debug("Loaded {} items into grid", items.size());
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Background
        guiGraphics.fill(x, y, x + width, y + height, 0xFF1F1F1F);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF3F3F3F);

        // Header
        guiGraphics.drawString(Minecraft.getInstance().font, "Items (" + items.size() + ")", x + 5, y + 5, 0xFFFFFF);

        int itemsPerRow = Math.max(1, (width - 20) / (ITEM_SIZE + PADDING));
        int visibleRows = (height - 30) / (ITEM_SIZE + PADDING);

        // Render items
        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleRows * itemsPerRow, items.size()); i++) {
            int row = (i - scrollOffset) / itemsPerRow;
            int col = (i - scrollOffset) % itemsPerRow;
            int drawX = x + 5 + col * (ITEM_SIZE + PADDING);
            int drawY = y + 20 + row * (ITEM_SIZE + PADDING);

            // Slot background
            guiGraphics.fill(drawX, drawY, drawX + ITEM_SIZE, drawY + ITEM_SIZE, 0xFF8B8B8B);

            if (i < items.size()) {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    guiGraphics.renderItem(stack, drawX + 1, drawY + 1);
                    guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, drawX + 1, drawY + 1);
                }
            }
        }

        // Scroll bar
        int totalRows = (items.size() - 1) / itemsPerRow + 1;
        if (totalRows > visibleRows) {
            int scrollBarHeight = Math.max(10, (visibleRows * height) / totalRows);
            int scrollBarY = y + 20 + (scrollOffset * (height - 30 - scrollBarHeight)) / (totalRows - visibleRows);
            guiGraphics.fill(x + width - 5, scrollBarY, x + width - 2, scrollBarY + scrollBarHeight, 0xFFAAAAAA);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int itemsPerRow = width / (ITEM_SIZE + PADDING);
        int maxScroll = Math.max(0, (items.size() - 1) / itemsPerRow - 3);
        scrollOffset = Math.max(0, Math.min(maxScroll, (int)(scrollOffset - scrollDelta)));
        return true;
    }

    public void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
