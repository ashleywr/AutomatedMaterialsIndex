package com.sanhiruzu.ami.client.results;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SingleSelectDropdown<T> implements Dropdown {
    private final String label;
    private final List<T> options;
    private final java.util.function.Function<T, String> displayName;
    private final java.util.function.Consumer<T> onSelect;
    private T selected;

    private int x, y, width;
    private static final int HEIGHT = 14;
    private static final int ITEM_HEIGHT = 12;

    private boolean open = false;

    public SingleSelectDropdown(String label, List<T> options, java.util.function.Function<T, String> displayName,
                                 T selected, java.util.function.Consumer<T> onSelect) {
        this.label = label;
        this.options = options;
        this.displayName = displayName;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    public void updatePosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        int bgColor = open ? 0xFF3A3A3A : 0xFF2A2A2A;
        g.fill(x, y, x + width, y + HEIGHT, bgColor);
        g.drawString(Minecraft.getInstance().font, displayName.apply(selected), x + 3, y + 2, 0xFFCCCCCC, false);

        if (open) {
            renderDropdown(g, mouseX, mouseY);
        }
    }

    private void renderDropdown(GuiGraphics g, int mouseX, int mouseY) {
        int dropH = options.size() * ITEM_HEIGHT + 2;
        g.fill(x, y + HEIGHT + 2, x + width, y + HEIGHT + 2 + dropH, 0xFF1A1A1A);
        g.fill(x, y + HEIGHT + 2, x + width, y + HEIGHT + 3, 0xFF555555);

        int itemY = y + HEIGHT + 3;
        for (T option : options) {
            boolean hovered = isPointInRect(mouseX, mouseY, x, itemY, width, ITEM_HEIGHT);
            if (hovered) g.fill(x, itemY, x + width, itemY + ITEM_HEIGHT, 0xFF333333);

            if (option.equals(selected)) {
                g.drawString(Minecraft.getInstance().font, "✓ " + displayName.apply(option), x + 2, itemY + 1, 0xFFAAAA44, false);
            } else {
                g.drawString(Minecraft.getInstance().font, displayName.apply(option), x + 2, itemY + 1, 0xFFCCCCCC, false);
            }
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
            for (T option : options) {
                if (isPointInRect((int) mouseX, (int) mouseY, x, itemY, width, ITEM_HEIGHT)) {
                    selected = option;
                    onSelect.accept(option);
                    open = false;
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

    public T getSelected() {
        return selected;
    }

    private boolean isPointInRect(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }
}
