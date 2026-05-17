package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SingleSelectDropdown<T> implements Dropdown {
    private final Component label;
    private final List<T> options;
    private final java.util.function.Function<T, Component> displayName;
    private final java.util.function.Consumer<T> onSelect;
    private T selected;

    private int x, y, width;
    private static final int HEIGHT = 14;
    private static final int ITEM_HEIGHT = 12;

    private boolean open = false;

    public SingleSelectDropdown(Component label, List<T> options, java.util.function.Function<T, Component> displayName,
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
        boolean canOpen = options != null && options.size() > 1;
        boolean hovered = canOpen && Dropdown.contains(mouseX, mouseY, x, y, width, HEIGHT);
        int bgColor = (open || hovered) ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        
        g.fill(x, y, x + width, y + HEIGHT, bgColor);
        Component textComp = displayName.apply(selected);
        String text = textComp.getString();
        var font = Minecraft.getInstance().font;
        
        // Add a small arrow if it can be opened
        if (canOpen) {
            String arrow = open ? "▲" : "▼";
            g.drawString(font, arrow, x + width - 9, y + 2, AMITheme.TEXT_SUBTLE, false);
        }
        
        int maxTextW = width - (canOpen ? 12 : 6);
        if (font.width(text) > maxTextW) {
            text = font.plainSubstrByWidth(text, maxTextW);
        }
        g.drawString(font, text, x + 3, y + 2, canOpen ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
    }

    public void renderList(GuiGraphics g, int mouseX, int mouseY) {
        if (open && options != null && options.size() > 1) renderDropdown(g, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        
        // Calculate required width to fit all options
        int listWidth = width;
        for (T option : options) {
            listWidth = Math.max(listWidth, font.width(displayName.apply(option).getString()) + 20);
        }
        
        int dropH = options.size() * ITEM_HEIGHT + 2;
        g.fill(x, y + HEIGHT + 2, x + listWidth, y + HEIGHT + 2 + dropH, AMITheme.DROPDOWN_LIST_BG);
        g.fill(x, y + HEIGHT + 2, x + listWidth, y + HEIGHT + 3, AMITheme.SECTION_SEP);

        int itemY = y + HEIGHT + 3;
        for (T option : options) {
            boolean hovered = Dropdown.contains(mouseX, mouseY, x, itemY, listWidth, ITEM_HEIGHT);
            if (hovered) g.fill(x, itemY, x + listWidth, itemY + ITEM_HEIGHT, AMITheme.DROPDOWN_BG);

            boolean isSelected = option.equals(selected);
            if (isSelected) {
                // Draw selection indicator (a small accent bar on the left)
                g.fill(x + 2, itemY + 2, x + 4, itemY + ITEM_HEIGHT - 2, com.sanhiruzu.ami.client.AMITheme.ACCENT_BLUE);
            }
            
            Component labelComp = displayName.apply(option);
            g.drawString(font, labelComp, x + 8, itemY + 1, isSelected ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
            itemY += ITEM_HEIGHT;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Toggle on button click
        if (Dropdown.contains((int) mouseX, (int) mouseY, x, y, width, HEIGHT)) {
            if (options != null && options.size() > 1) {
                open = !open;
            }
            return true;
        }

        // Handle dropdown item clicks
        if (open) {
            var font = Minecraft.getInstance().font;
            int listWidth = width;
            for (T option : options) {
                listWidth = Math.max(listWidth, font.width(displayName.apply(option).getString()) + 20);
            }

            int itemY = y + HEIGHT + 3;
            for (T option : options) {
                if (Dropdown.contains((int) mouseX, (int) mouseY, x, itemY, listWidth, ITEM_HEIGHT)) {
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

    public void setSelected(T selected) {
        this.selected = selected;
    }

    public T getSelected() {
        return selected;
    }

}
