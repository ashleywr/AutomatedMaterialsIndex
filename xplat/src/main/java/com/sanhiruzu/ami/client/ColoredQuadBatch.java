package com.sanhiruzu.ami.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Batches flat GUI rectangles into one draw call. Falls back to GuiGraphicsExtractor.fill()
 * in MC 26.x where the vertex-buffer plumbing was removed.
 */
public final class ColoredQuadBatch {
    private final List<Rect> rects = new ArrayList<>();
    private int count;

    public void clear() {
        count = 0;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1 || (color >>> 24) == 0) return;
        Rect rect;
        if (count < rects.size()) {
            rect = rects.get(count);
        } else {
            rect = new Rect();
            rects.add(rect);
        }
        count++;
        rect.set(x1, y1, x2, y2, color);
    }

    public void border(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        fill(x, y, x + width, y + 1, color);
        fill(x, y + height - 1, x + width, y + height, color);
        fill(x, y + 1, x + 1, y + height - 1, color);
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public void flush(GuiGraphicsExtractor graphics) {
        if (count == 0) return;
        for (int i = 0; i < count; i++) {
            Rect rect = rects.get(i);
            graphics.fill(rect.x1, rect.y1, rect.x2, rect.y2, rect.color);
        }
        clear();
    }

    private static final class Rect {
        private int x1;
        private int y1;
        private int x2;
        private int y2;
        private int color;

        private void set(int x1, int y1, int x2, int y2, int color) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
        }
    }
}
