package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MultiSelectDropdown<T> implements Dropdown {
    private static final int HEIGHT = 14;
    private static final int ITEM_HEIGHT = 12;
    private static final int MAX_DROPDOWN_HEIGHT = 150;
    private List<T> options;
    private final java.util.function.Function<T, String> displayName;
    private final Set<T> selected;
    private List<String> optionLabels = List.of();
    private int x, y, width;
    private int cachedListWidth;
    private int cachedBaseWidth = -1;
    private Font cachedFont;
    private boolean optionCacheDirty = true;
    private boolean open = false;

    public MultiSelectDropdown(List<T> options, java.util.function.Function<T, String> displayName) {
        this.options = copyOptions(options);
        this.displayName = displayName;
        this.selected = new HashSet<>(this.options); // Select all by default
    }

    public void updatePosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        if (this.width != width) {
            this.width = width;
            this.optionCacheDirty = true;
        }
    }

    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean canOpen = options != null && !options.isEmpty();
        boolean hovered = canOpen && Dropdown.contains(mouseX, mouseY, x, y, width, HEIGHT);
        AMITheme.fillControlChrome(g, x, y, width, HEIGHT,
                (open || hovered) ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG, open);

        var font = Minecraft.getInstance().font;
        if (canOpen) {
            AmiGuiIcons.dropdownChevron(g, x + width - 7, y + HEIGHT / 2, AMITheme.TEXT_SUBTLE, open);
        }

        String countLabel = Component.translatable("ami.gui.dropdown_count", selected.size(), options.size()).getString();
        int maxTextW = Math.max(0, width - (canOpen ? 12 : 6));
        if (maxTextW == 0) {
            countLabel = "";
        } else if (font.width(countLabel) > maxTextW) {
            String ellipsis = Component.translatable("ami.gui.dropdown_ellipsis").getString();
            countLabel = font.plainSubstrByWidth(countLabel, Math.max(0, maxTextW - font.width(ellipsis))) + ellipsis;
            if (font.width(countLabel) > maxTextW) {
                countLabel = font.plainSubstrByWidth(countLabel, maxTextW);
            }
        }
        g.text(font, countLabel, x + 3, y + 2, canOpen ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
    }

    public void renderList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (open && options != null && !options.isEmpty()) renderDropdown(g, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        ensureOptionCache(font);
        int listWidth = cachedListWidth;

        int dropH = Math.min(MAX_DROPDOWN_HEIGHT, options.size() * ITEM_HEIGHT + 2);
        AMITheme.fillPixelPopup(g, x, y + HEIGHT + 2, listWidth, dropH,
                AMITheme.DROPDOWN_LIST_BG, AMITheme.SECTION_SEP, AMITheme.CONTROL_SHADOW, 0);

        int itemY = y + HEIGHT + 3;
        for (int i = 0; i < options.size(); i++) {
            if (itemY >= y + HEIGHT + 2 + dropH - ITEM_HEIGHT) break;
            T option = options.get(i);

            boolean hovered = Dropdown.contains(mouseX, mouseY, x, itemY, listWidth, ITEM_HEIGHT);
            if (hovered) {
                g.fill(x + 1, itemY, x + listWidth - 1, itemY + ITEM_HEIGHT, AMITheme.DROPDOWN_BG);
            }

            boolean isSelected = selected.contains(option);
            if (isSelected) {
                // Small accent bar on the left
                g.fill(x + 2, itemY + 2, x + 4, itemY + ITEM_HEIGHT - 2, com.sanhiruzu.ami.client.AMITheme.ACCENT_BLUE);
            }

            g.text(font, optionLabels.get(i), x + 8, itemY + 1, isSelected ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
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

            ensureOptionCache(Minecraft.getInstance().font);
            int listWidth = cachedListWidth;

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
        List<T> next = copyOptions(newOptions);
        // Keep previously selected items that are still in the new list
        Set<T> kept = new HashSet<>();
        for (T item : selected) {
            if (next.contains(item)) {
                kept.add(item);
            }
        }
        // Add all new items by default
        kept.addAll(next);
        if (!Objects.equals(this.options, next)) {
            this.options = next;
            this.optionCacheDirty = true;
        }
        selected.clear();
        selected.addAll(kept);
    }

    private void ensureOptionCache(Font font) {
        if (!optionCacheDirty && cachedBaseWidth == width && cachedFont == font) return;

        List<String> labels = new ArrayList<>(options.size());
        int listWidth = width;
        for (T option : options) {
            String label = displayName.apply(option);
            if (label == null) label = "";
            labels.add(label);
            listWidth = Math.max(listWidth, font.width(label) + 20);
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
