package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * A compact dropdown pinned to the right end of the toolbar that lets players
 * choose which fields appear on the subtitle line of each list row.
 *
 * Not part of the auto-sized dropdown list — positioned explicitly by ResultsToolbar.
 */
public class RowFieldPickerDropdown {

    private static final int BTN_H  = 14;
    private static final int ITEM_H = 12;

    private int x, y, width;
    private boolean open = false;

    public void updatePosition(int x, int y, int width) {
        this.x     = x;
        this.y     = y;
        this.width = width;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        boolean hovered = Dropdown.contains(mouseX, mouseY, x, y, width, BTN_H);
        g.fill(x, y, x + width, y + BTN_H, open || hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG);

        var font = Minecraft.getInstance().font;
        int count = RowFieldConfig.getSubtitleFields().size();
        String label = count > 0 ? "Fields (" + count + ")" : "Fields";
        int textX = x + Math.max(2, (width - font.width(label)) / 2);
        g.drawString(font, label, textX, y + 2, AMITheme.TEXT_HEADER, false);
    }

    public void renderList(GuiGraphics g, int mouseX, int mouseY) {
        if (!open) return;

        RowField[] fields = RowField.values();
        List<RowField> active = RowFieldConfig.getSubtitleFields();

        int dropH = fields.length * ITEM_H + 4;
        int dy    = y + BTN_H + 2;

        // Background + top rule
        g.fill(x, dy, x + width, dy + dropH, AMITheme.DROPDOWN_LIST_BG);
        g.fill(x, dy, x + width, dy + 1,     AMITheme.SECTION_SEP);

        var font = Minecraft.getInstance().font;
        int iy = dy + 2;
        for (RowField field : fields) {
            if (Dropdown.contains(mouseX, mouseY, x, iy, width, ITEM_H)) {
                g.fill(x, iy, x + width, iy + ITEM_H, AMITheme.DROPDOWN_BG);
            }
            String check = active.contains(field) ? "✓ " : "  ";
            g.drawString(font, check + field.displayName, x + 3, iy + 1, AMITheme.TEXT_SUBTLE, false);
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
        int dropH = fields.length * ITEM_H + 4;
        int dy    = y + BTN_H + 2;

        if (Dropdown.contains((int) mouseX, (int) mouseY, x, dy, width, dropH)) {
            int iy = dy + 2;
            for (RowField field : fields) {
                if (Dropdown.contains((int) mouseX, (int) mouseY, x, iy, width, ITEM_H)) {
                    List<RowField> current = new ArrayList<>(RowFieldConfig.getSubtitleFields());
                    if (current.contains(field)) current.remove(field);
                    else current.add(field);
                    RowFieldConfig.setSubtitleFields(current);
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

    public void close()       { open = false; }
    public boolean isOpen()   { return open; }

}
