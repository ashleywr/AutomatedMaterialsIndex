package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SingleSelectDropdown<T> implements Dropdown {
    private static final int HEIGHT = 14;
    private static final int ITEM_HEIGHT = 12;
    private final Component label;
    private final java.util.function.Function<T, Component> displayName;
    private final java.util.function.Consumer<T> onSelect;
    private List<T> options;
    private T selected;
    private List<Component> optionLabels = List.of();
    private int x, y, width;
    private int cachedListWidth;
    private int cachedBaseWidth = -1;
    private Font cachedFont;
    private boolean optionCacheDirty = true;
    private boolean open = false;

    public SingleSelectDropdown(Component label, List<T> options, java.util.function.Function<T, Component> displayName,
                                T selected, java.util.function.Consumer<T> onSelect) {
        this.label = label;
        this.options = copyOptions(options);
        this.displayName = displayName;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    public void updatePosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        if (this.width != width) {
            this.width = width;
            this.optionCacheDirty = true;
        }
    }

    public boolean isMouseOverButton(int mouseX, int mouseY) {
        return Dropdown.contains(mouseX, mouseY, x, y, width, HEIGHT);
    }

    public void setOptions(List<T> options) {
        List<T> next = copyOptions(options);
        if (!Objects.equals(this.options, next)) {
            this.options = next;
            this.optionCacheDirty = true;
        }
        if (selected != null && !this.options.contains(selected) && !this.options.isEmpty()) {
            selected = this.options.get(0);
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        boolean canOpen = options != null && options.size() > 1;
        boolean hovered = canOpen && Dropdown.contains(mouseX, mouseY, x, y, width, HEIGHT);
        int bgColor = (open || hovered) ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;

        AMITheme.fillControlChrome(g, x, y, width, HEIGHT, bgColor, open);
        Component textComp = selectedLabel();
        String text = textComp.getString();
        var font = Minecraft.getInstance().font;

        if (canOpen) {
            AmiGuiIcons.dropdownChevron(g, x + width - 7, y + HEIGHT / 2, AMITheme.TEXT_SUBTLE, open);
        }

        int maxTextW = Math.max(0, width - (canOpen ? 12 : 6));
        String displayText = text;
        if (maxTextW == 0) {
            displayText = "";
        } else if (font.width(text) > maxTextW) {
            String ellipsis = Component.translatable("ami.gui.dropdown_ellipsis").getString();
            displayText = font.plainSubstrByWidth(text, Math.max(0, maxTextW - font.width(ellipsis))) + ellipsis;
            if (font.width(displayText) > maxTextW) {
                displayText = font.plainSubstrByWidth(displayText, maxTextW);
            }
        }
        g.drawString(font, displayText, x + 3, y + 2, canOpen ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
    }

    public void renderList(GuiGraphics g, int mouseX, int mouseY) {
        if (open && options != null && options.size() > 1) renderDropdown(g, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        ensureOptionCache(font);
        int listWidth = cachedListWidth;

        int dropH = options.size() * ITEM_HEIGHT + 2;
        AMITheme.fillPixelPopup(g, x, y + HEIGHT + 2, listWidth, dropH,
                AMITheme.DROPDOWN_LIST_BG, AMITheme.SECTION_SEP, AMITheme.CONTROL_SHADOW, 0);

        int itemY = y + HEIGHT + 3;
        for (int i = 0; i < options.size(); i++) {
            T option = options.get(i);
            boolean hovered = Dropdown.contains(mouseX, mouseY, x, itemY, listWidth, ITEM_HEIGHT);
            if (hovered) g.fill(x + 1, itemY, x + listWidth - 1, itemY + ITEM_HEIGHT, AMITheme.DROPDOWN_BG);

            boolean isSelected = option.equals(selected);
            if (isSelected) {
                // Draw selection indicator (a small accent bar on the left)
                g.fill(x + 2, itemY + 2, x + 4, itemY + ITEM_HEIGHT - 2, com.sanhiruzu.ami.client.AMITheme.ACCENT_BLUE);
            }

            Component labelComp = optionLabels.get(i);
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

            ensureOptionCache(Minecraft.getInstance().font);
            int listWidth = cachedListWidth;

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

    private Component selectedLabel() {
        if (selected == null) return Component.empty();
        Component component = displayName.apply(selected);
        return component == null ? Component.empty() : component;
    }

    private void ensureOptionCache(Font font) {
        if (!optionCacheDirty && cachedBaseWidth == width && cachedFont == font) return;

        List<Component> labels = new ArrayList<>(options.size());
        int listWidth = width;
        for (T option : options) {
            Component component = displayName.apply(option);
            if (component == null) component = Component.empty();
            labels.add(component);
            listWidth = Math.max(listWidth, font.width(component.getString()) + 20);
        }
        this.optionLabels = List.copyOf(labels);
        this.cachedListWidth = listWidth;
        this.cachedBaseWidth = width;
        this.cachedFont = font;
        this.optionCacheDirty = false;
    }

    private static <T> List<T> copyOptions(List<T> options) {
        return options == null ? List.of() : List.copyOf(options);
    }

}
