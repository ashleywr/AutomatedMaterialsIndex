package com.ashleyww.ami.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.ashleyww.ami.index.AMIIndex;
import com.ashleyww.ami.index.IndexCategory;
import com.ashleyww.ami.index.MaterialEntry;

public class ItemGridWidget {
    private final AMIScreen parent;
    private final int x, y, width, height;
    private final List<ItemStack> items;
    private int scrollOffset = 0;
    private static final int ITEM_SIZE = 18;
    private static final int PADDING = 2;

    public ItemGridWidget(AMIScreen parent, int x, int y, int width, int height, int totalItems) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.items = new ArrayList<>();

        loadItems();
    }

    private void loadItems() {
        items.clear();

        AMIIndex index = AMIIndex.CLIENT_INSTANCE;
        if (index.getTotalItemsIndexed() == 0) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != null && !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("air")) {
                    items.add(new ItemStack(item));
                    if (items.size() >= 100) break;
                }
            }
        } else {
            var categoryIndex = index.getCategoryIndex(IndexCategory.BY_MOD);
            for (List<MaterialEntry> entries : categoryIndex.values()) {
                for (MaterialEntry entry : entries) {
                    items.add(new ItemStack(entry.item()));
                    if (items.size() >= 200) break;
                }
                if (items.size() >= 200) break;
            }
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF3F3F3F);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "Items", x + width / 2, y + 5, 0xFFFFFF);

        int itemsPerRow = width / (ITEM_SIZE + PADDING);
        int visibleRows = height / (ITEM_SIZE + PADDING);

        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleRows * itemsPerRow, items.size()); i++) {
            int row = (i - scrollOffset) / itemsPerRow;
            int col = (i - scrollOffset) % itemsPerRow;
            int drawX = x + 10 + col * (ITEM_SIZE + PADDING);
            int drawY = y + 20 + row * (ITEM_SIZE + PADDING);

            guiGraphics.fill(drawX, drawY, drawX + ITEM_SIZE, drawY + ITEM_SIZE, 0xFF8B8B8B);
            if (i < items.size()) {
                guiGraphics.renderItem(items.get(i), drawX + 1, drawY + 1);
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int itemsPerRow = width / (ITEM_SIZE + PADDING);
        int maxScroll = Math.max(0, (items.size() - 1) / itemsPerRow - 3);
        scrollOffset = Math.max(0, Math.min(maxScroll, (int)(scrollOffset - scrollDelta)));
        return true;
    }
}
