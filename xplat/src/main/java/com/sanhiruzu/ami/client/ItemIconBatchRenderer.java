package com.sanhiruzu.ami.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ItemIconBatchRenderer {
    private final List<Entry> entries = new ArrayList<>();
    private int count;

    public void clear() {
        count = 0;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void add(ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        Entry entry;
        if (count < entries.size()) {
            entry = entries.get(count);
        } else {
            entry = new Entry();
            entries.add(entry);
        }
        entry.set(stack, x, y);
        count++;
    }

    public void render(GuiGraphicsExtractor g) {
        if (isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < count; i++) {
            Entry e = entries.get(i);
            g.item(e.stack, e.x, e.y);
        }
        for (int i = 0; i < count; i++) {
            Entry e = entries.get(i);
            g.itemDecorations(font, e.stack, e.x, e.y);
        }
        clear();
    }

    private static final class Entry {
        private ItemStack stack = ItemStack.EMPTY;
        private int x;
        private int y;

        private void set(ItemStack stack, int x, int y) {
            this.stack = stack;
            this.x = x;
            this.y = y;
        }
    }
}
