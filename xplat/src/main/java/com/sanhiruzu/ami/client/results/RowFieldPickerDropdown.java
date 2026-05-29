package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.List;

/**
 * A compact dropdown pinned to the right end of the toolbar that lets players
 * choose which fields appear on the subtitle line of each list row.
 * <p>
 * Not part of the auto-sized dropdown list — positioned explicitly by ResultsToolbar.
 */
public class RowFieldPickerDropdown {

    private static final int BTN_H = 14;
    private static final int ITEM_H = 12;
    private static final int CHECK_SIZE = 6;

    private int x, y, width;
    private boolean open = false;

    public void updatePosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        boolean hovered = Dropdown.contains(mouseX, mouseY, x, y, width, BTN_H);
        g.fill(x, y, x + width, y + BTN_H, open || hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG);

        var font = Minecraft.getInstance().font;
        String label = Component.translatable("ami.gui.fields_button").getString();
        int labelW = font.width(label);
        int textX = x + Math.max(2, (width - labelW - 12) / 2);
        g.drawString(font, label, textX, y + 2, AMITheme.TEXT_HEADER, false);
        AmiGuiIcons.dropdownChevron(g, x + width - 7, y + BTN_H / 2, open || hovered ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_SUBTLE, open);
    }

    public void renderList(GuiGraphics g, int mouseX, int mouseY) {
        if (!open) return;

        RowField[] fields = RowField.values();
        List<RowField> active = RowFieldConfig.getSubtitleFields();
        var font = Minecraft.getInstance().font;

        int listWidth = listWidth(font, fields);

        int dropH = fields.length * ITEM_H + 4;
        int dy = y + BTN_H + 2;

        // Background + top rule
        g.fill(x, dy, x + listWidth, dy + dropH, AMITheme.DROPDOWN_LIST_BG);
        g.fill(x, dy, x + listWidth, dy + 1, AMITheme.SECTION_SEP);

        int iy = dy + 2;
        for (RowField field : fields) {
            boolean hovered = Dropdown.contains(mouseX, mouseY, x, iy, listWidth, ITEM_H);
            if (hovered) {
                g.fill(x, iy, x + listWidth, iy + ITEM_H, AMITheme.DROPDOWN_BG);
            }

            boolean isSelected = active.contains(field);
            int checkX = x + 3;
            int checkY = iy + 2;
            g.fill(checkX, checkY, checkX + CHECK_SIZE, checkY + CHECK_SIZE, AMITheme.SECTION_SEP);
            if (isSelected) {
                g.fill(checkX + 1, checkY + 1, checkX + CHECK_SIZE - 1, checkY + CHECK_SIZE - 1, AMITheme.ACCENT_BLUE);
                g.drawString(font, "✓", checkX + 1, iy, AMITheme.WHITE, false);
            } else {
                g.fill(checkX + 1, checkY + 1, checkX + CHECK_SIZE - 1, checkY + CHECK_SIZE - 1, AMITheme.DROPDOWN_LIST_BG);
            }

            g.drawString(font, field.displayName, x + 12, iy + 2, isSelected ? AMITheme.TEXT_HEADER : AMITheme.TEXT_SUBTLE, false);
            iy += ITEM_H;
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Header button — toggle open/closed
        if (Dropdown.contains((int) mouseX, (int) mouseY, x, y, width, BTN_H)) {
            open = !open;
            return true;
        }

        if (!open) return false;

        RowField[] fields = RowField.values();
        var font = Minecraft.getInstance().font;
        int listWidth = listWidth(font, fields);

        int dropH = fields.length * ITEM_H + 4;
        int dy = y + BTN_H + 2;

        if (Dropdown.contains((int) mouseX, (int) mouseY, x, dy, listWidth, dropH)) {
            int iy = dy + 2;
            for (RowField field : fields) {
                if (Dropdown.contains((int) mouseX, (int) mouseY, x, iy, listWidth, ITEM_H)) {
                    List<RowField> current = RowFieldConfig.getSubtitleFields();
                    EnumSet<RowField> next = current.isEmpty()
                            ? EnumSet.noneOf(RowField.class)
                            : EnumSet.copyOf(current);
                    if (next.contains(field)) {
                        next.remove(field);
                    } else {
                        next.add(field);
                    }
                    RowFieldConfig.setSubtitleFields(next);
                    return true;
                }
                iy += ITEM_H;
            }
            return true; // click inside list but not on an item — keep open
        }

        // Click outside — close
        open = false;
        return false;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    private int listWidth(Font font, RowField[] fields) {
        int listWidth = width;
        for (RowField field : fields) {
            listWidth = Math.max(listWidth, font.width(field.displayName.getString()) + 20);
        }
        return listWidth;
    }
}
