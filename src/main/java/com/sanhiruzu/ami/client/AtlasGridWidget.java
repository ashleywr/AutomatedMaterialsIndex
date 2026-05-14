package com.sanhiruzu.ami.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.index.WorldAtlasIndex;

public class AtlasGridWidget {
    private int x, y;
    private int width, height;
    private final List<Object> entries;
    private int scrollOffset = 0;
    private static final int ENTRY_SIZE = 18;
    private static final int PADDING = 2;

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

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Outer dark background (semi-transparent)
        guiGraphics.fill(x, y, x + width, y + height, 0xAA000000);
        
        // Inner background with light border (vanilla style)
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF373737); // Inner
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF8B8B8B); // Top border
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B); // Bottom border
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF8B8B8B); // Left border
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B); // Right border

        int entriesPerRow = Math.max(1, (width - 10) / (ENTRY_SIZE + PADDING));
        int visibleRows = (height - 10) / (ENTRY_SIZE + PADDING);

        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleRows * entriesPerRow, entries.size()); i++) {
            int row = (i - scrollOffset) / entriesPerRow;
            int col = (i - scrollOffset) % entriesPerRow;
            int drawX = x + 5 + col * (ENTRY_SIZE + PADDING);
            int drawY = y + 5 + row * (ENTRY_SIZE + PADDING);

            Object entry = entries.get(i);
            if (entry instanceof ItemStack stack) {
                guiGraphics.renderItem(stack, drawX, drawY);
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, drawX, drawY);
                
                if (mouseX >= drawX && mouseX < drawX + ENTRY_SIZE && mouseY >= drawY && mouseY < drawY + ENTRY_SIZE) {
                    guiGraphics.fill(drawX, drawY, drawX + ENTRY_SIZE, drawY + ENTRY_SIZE, 0x80FFFFFF);
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
                }
            } else if (entry instanceof WorldAtlasIndex.AtlasEntry atlasEntry) {
                guiGraphics.fill(drawX, drawY, drawX + ENTRY_SIZE, drawY + ENTRY_SIZE, 0xFF555555);
                
                if (mouseX >= drawX && mouseX < drawX + ENTRY_SIZE && mouseY >= drawY && mouseY < drawY + ENTRY_SIZE) {
                    guiGraphics.fill(drawX, drawY, drawX + ENTRY_SIZE, drawY + ENTRY_SIZE, 0x80FFFFFF);
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, net.minecraft.network.chat.Component.literal(atlasEntry.name()), mouseX, mouseY);
                }
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int entriesPerRow = Math.max(1, (width - 10) / (ENTRY_SIZE + PADDING));
        int maxScroll = Math.max(0, (entries.size() - 1) / entriesPerRow - 5);
        scrollOffset = Math.max(0, Math.min(maxScroll, (int)(scrollOffset - scrollDelta)));
        return true;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
