package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiSelectDropdown<T> implements Dropdown {
    private static final int HEIGHT = 14;
    private static final int ITEM_HEIGHT = 12;
    private static final int MAX_DROPDOWN_HEIGHT = 150;
    private final List<T> options;
    private final java.util.function.Function<T, String> displayName;
    private final Set<T> selected;
    private int x, y, width;
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
        boolean canOpen = options != null && !options.isEmpty();
        boolean hovered = canOpen && Dropdown.contains(mouseX, mouseY, x, y, width, HEIGHT);
        AMITheme.fillControlChrome(g, x, y, width, HEIGHT,
                (open || hovered) ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG, open);

        var font = Minecraft.getInstance().font;
        if (canOpen) {
            AmiGuiIcons.dropdownChevron(g, x + width - 7, y + HEIGHT / 2, AMITheme.TEXT_SUBTLE, open);
        }

        String countLabel = Component.translatable("ami.gui.dropdown_count", selected.size(), options.size()).getString();
        g.drawString(font, countLabel, x + 3, y + 2, canOpen ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
    }

    public void renderList(GuiGraphics g, int mouseX, int mouseY) {
        if (open && options != null && !options.isEmpty()) renderDropdown(g, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;

        // Calculate required width
        int listWidth = width;
        for (T option : options) {
            listWidth = Math.max(listWidth, font.width(displayName.apply(option)) + 20);
        }

        int dropH = Math.min(MAX_DROPDOWN_HEIGHT, options.size() * ITEM_HEIGHT + 2);
        AMITheme.fillInsetRect(g, x, y + HEIGHT + 2, listWidth, dropH, AMITheme.DROPDOWN_LIST_BG, false);

        int itemY = y + HEIGHT + 3;
        for (T option : options) {
            if (itemY >= y + HEIGHT + 2 + dropH - ITEM_HEIGHT) break;

            boolean hovered = Dropdown.contains(mouseX, mouseY, x, itemY, listWidth, ITEM_HEIGHT);
            if (hovered) {
                g.fill(x, itemY, x + listWidth, itemY + ITEM_HEIGHT, AMITheme.DROPDOWN_BG);
            }

            boolean isSelected = selected.contains(option);
            if (isSelected) {
                // Small accent bar on the left
                g.fill(x + 2, itemY + 2, x + 4, itemY + ITEM_HEIGHT - 2, com.sanhiruzu.ami.client.AMITheme.ACCENT_BLUE);
            }

            g.drawString(font, displayName.apply(option), x + 8, itemY + 1, isSelected ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
            itemY += ITEM_HEIGHT;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Toggle on button click
        if (Dropdown.contains(mx, my, x, y, width, HEIGHT)) {
            if (options != null && !options.isEmpty()) {
                open = !open;
            }
            return true;
        }

        // Handle dropdown item clicks
        if (open) {
            if (options == null || options.isEmpty()) {
                return false;
            }

            var font = Minecraft.getInstance().font;
            int listWidth = width;
            for (T option : options) {
                listWidth = Math.max(listWidth, font.width(displayName.apply(option)) + 20);
            }

            int itemY = y + HEIGHT + 3;
            int dropH = Math.min(MAX_DROPDOWN_HEIGHT, options.size() * ITEM_HEIGHT + 2);
            if (!Dropdown.contains(mx, my, x, y + HEIGHT + 2, listWidth, dropH)) {
                return false;
            }

            for (T option : options) {
                if (itemY >= y + HEIGHT + 2 + dropH - ITEM_HEIGHT) break;

                if (Dropdown.contains(mx, my, x, itemY, listWidth, ITEM_HEIGHT)) {
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

}
