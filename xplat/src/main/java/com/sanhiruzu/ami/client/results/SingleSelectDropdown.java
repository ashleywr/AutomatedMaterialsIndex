package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SingleSelectDropdown<T> implements Dropdown {
    private static final int HEIGHT = 14;
    private static final int ITEM_HEIGHT = 12;
    private final Component label;
    private final java.util.function.Function<T, Component> displayName;
    private final java.util.function.Consumer<T> onSelect;
    private List<T> options;
    private T selected;
    private int x, y, width;
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

    public boolean isMouseOverButton(int mouseX, int mouseY) {
        return Dropdown.contains(mouseX, mouseY, x, y, width, HEIGHT);
    }

    public void setOptions(List<T> options) {
        this.options = options;
        if (selected != null && options != null && !options.contains(selected) && !options.isEmpty()) {
            selected = options.get(0);
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        boolean canOpen = options != null && options.size() > 1;
        boolean hovered = canOpen && Dropdown.contains(mouseX, mouseY, x, y, width, HEIGHT);
        int bgColor = (open || hovered) ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;

        g.fill(x, y, x + width, y + HEIGHT, bgColor);
        Component textComp = displayName.apply(selected);
        String text = textComp.getString();
        var font = Minecraft.getInstance().font;

        if (canOpen) {
            AmiGuiIcons.dropdownChevron(g, x + width - 7, y + HEIGHT / 2, AMITheme.TEXT_SUBTLE, open);
        }

        int maxTextW = width - (canOpen ? 12 : 6);
        String displayText = text;
        if (font.width(text) > maxTextW) {
            displayText = font.plainSubstrByWidth(text, maxTextW - 6) + Component.translatable("ami.gui.dropdown_ellipsis").getString();
        }
        g.drawString(font, displayText, x + 3, y + 2, canOpen ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
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

        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Toggle on button click
        if (Dropdown.contains(mx, my, x, y, width, HEIGHT)) {
            if (options != null && options.size() > 1) {
                open = !open;
            }
            return true;
        }

        // Handle dropdown item clicks
        if (open) {
            if (options == null || options.size() <= 1) {
                return false;
            }

            var font = Minecraft.getInstance().font;
            int listWidth = width;
            for (T option : options) {
                listWidth = Math.max(listWidth, font.width(displayName.apply(option).getString()) + 20);
            }

            int listY = y + HEIGHT + 2;
            int dropH = options.size() * ITEM_HEIGHT + 2;
            if (!Dropdown.contains(mx, my, x, listY, listWidth, dropH)) {
                return false;
            }

            int itemY = y + HEIGHT + 3;
            for (T option : options) {
                if (Dropdown.contains(mx, my, x, itemY, listWidth, ITEM_HEIGHT)) {
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

    public void setSelected(T selected) {
        this.selected = selected;
    }

}
