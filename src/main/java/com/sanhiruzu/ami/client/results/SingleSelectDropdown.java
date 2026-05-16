package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

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
        int bgColor = open ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        g.fill(x, y, x + width, y + HEIGHT, bgColor);
        String text = displayName.apply(selected);
        var font = Minecraft.getInstance().font;
        if (font.width(text) > width - 6) {
            text = font.plainSubstrByWidth(text, width - 6);
        }
        g.drawString(font, text, x + 3, y + 2, AMITheme.TEXT_HEADER, false);
    }

    public void renderList(GuiGraphics g, int mouseX, int mouseY) {
        if (open) renderDropdown(g, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int dropH = options.size() * ITEM_HEIGHT + 2;
        g.fill(x, y + HEIGHT + 2, x + width, y + HEIGHT + 2 + dropH, AMITheme.DROPDOWN_LIST_BG);
        g.fill(x, y + HEIGHT + 2, x + width, y + HEIGHT + 3, AMITheme.SECTION_SEP);

        int itemY = y + HEIGHT + 3;
        for (T option : options) {
            boolean hovered = Dropdown.contains(mouseX, mouseY, x, itemY, width, ITEM_HEIGHT);
            if (hovered) g.fill(x, itemY, x + width, itemY + ITEM_HEIGHT, AMITheme.DROPDOWN_BG);

            boolean isSelected = option.equals(selected);
            String text = (isSelected ? "✓ " : "") + displayName.apply(option);
            g.drawString(font, text, x + 2, itemY + 1, isSelected ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
            itemY += ITEM_HEIGHT;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Toggle on button click
        if (Dropdown.contains((int) mouseX, (int) mouseY, x, y, width, HEIGHT)) {
            open = !open;
            return true;
        }

        // Handle dropdown item clicks
        if (open) {
            int itemY = y + HEIGHT + 3;
            for (T option : options) {
                if (Dropdown.contains((int) mouseX, (int) mouseY, x, itemY, width, ITEM_HEIGHT)) {
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

}
