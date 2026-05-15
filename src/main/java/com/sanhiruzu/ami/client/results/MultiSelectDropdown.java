package com.sanhiruzu.ami.client.results;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class MultiSelectDropdown<T> implements Dropdown {
    private final List<T> options;
    private final java.util.function.Function<T, String> displayName;
    private final Set<T> selected;

    private int x, y, width;
    private static final int HEIGHT = 14;
    private static final int ITEM_HEIGHT = 12;
    private static final int MAX_DROPDOWN_HEIGHT = 150;

    private boolean open = false;

    public MultiSelectDropdown(List<T> options, java.util.function.Function<T, String> displayName) {
        this.options = options;
        this.displayName = displayName;
        this.selected = new HashSet<>(options); // Select all by default
    }

    public void updatePosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        int bgColor = open ? 0xFF3A3A3A : 0xFF2A2A2A;
        g.fill(x, y, x + width, y + HEIGHT, bgColor);
        String countLabel = selected.size() + "/" + options.size();
        g.drawString(Minecraft.getInstance().font, countLabel, x + 3, y + 2, 0xFFCCCCCC, false);
    }

    public void renderList(GuiGraphics g, int mouseX, int mouseY) {
        if (open) renderDropdown(g, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics g, int mouseX, int mouseY) {
        int dropH = Math.min(MAX_DROPDOWN_HEIGHT, options.size() * ITEM_HEIGHT + 2);
        g.fill(x, y + HEIGHT + 2, x + width, y + HEIGHT + 2 + dropH, 0xFF1A1A1A);
        g.fill(x, y + HEIGHT + 2, x + width, y + HEIGHT + 3, 0xFF555555);

        int itemY = y + HEIGHT + 3;
        for (T option : options) {
            if (itemY >= y + HEIGHT + 2 + dropH - ITEM_HEIGHT) break;

            boolean hovered = isPointInRect(mouseX, mouseY, x, itemY, width, ITEM_HEIGHT);
            if (hovered) g.fill(x, itemY, x + width, itemY + ITEM_HEIGHT, 0xFF333333);

            String checkmark = selected.contains(option) ? "✓ " : "  ";
            g.drawString(Minecraft.getInstance().font, checkmark + displayName.apply(option), x + 2, itemY + 1, 0xFFCCCCCC, false);
            itemY += ITEM_HEIGHT;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Toggle on button click
        if (isPointInRect((int) mouseX, (int) mouseY, x, y, width, HEIGHT)) {
            open = !open;
            return true;
        }

        // Handle dropdown item clicks
        if (open) {
            int itemY = y + HEIGHT + 3;
            int dropH = Math.min(MAX_DROPDOWN_HEIGHT, options.size() * ITEM_HEIGHT + 2);
            for (T option : options) {
                if (itemY >= y + HEIGHT + 2 + dropH - ITEM_HEIGHT) break;

                if (isPointInRect((int) mouseX, (int) mouseY, x, itemY, width, ITEM_HEIGHT)) {
                    if (selected.contains(option)) {
                        selected.remove(option);
                    } else {
                        selected.add(option);
                    }
                    return true;
                }
                itemY += ITEM_HEIGHT;
            }
            // Click inside dropdown but not on item: keep open
            return true;
        }

        return false;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public Set<T> getSelected() {
        return new HashSet<>(selected);
    }

    public void setOptions(List<T> newOptions) {
        // Keep previously selected items that are still in the new list
        Set<T> kept = new HashSet<>();
        for (T item : selected) {
            if (newOptions.contains(item)) {
                kept.add(item);
            }
        }
        // Add all new items by default
        kept.addAll(newOptions);
        selected.clear();
        selected.addAll(kept);
    }

    private boolean isPointInRect(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }
}
