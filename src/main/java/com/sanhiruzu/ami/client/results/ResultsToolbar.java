package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

public class ResultsToolbar {
    public enum ViewMode { GRID, LIST }

    private static final int TOOLBAR_HEIGHT  = 20;
    private static final int MODE_BUTTON_W  = 26; // wide enough for "Grid"/"List"
    private static final int BUTTON_W       = 14;
    private static final int DROPDOWN_W     = 80;
    private static final int MOD_FILTER_W   = 60;
    private static final int FIELDS_BTN_W   = 44; // wide enough for "Fields (3)"

    private int x, y, width;
    private boolean ascending = true;
    private ViewMode viewMode = ViewMode.LIST;

    // Registered dropdowns - add or remove here to customize the toolbar
    private final List<Dropdown> dropdowns = new ArrayList<>();

    // Specific dropdown references for getters
    private SingleSelectDropdown<ResultsProcessor.SortField> sortFieldDropdown;
    private SingleSelectDropdown<ResultsProcessor.GroupBy> groupByDropdown;

    // Field picker — pinned to right end of toolbar, not in the auto-sized list
    private final RowFieldPickerDropdown fieldsPicker = new RowFieldPickerDropdown();

    public ResultsToolbar(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;

        // Create dropdowns
        List<ResultsProcessor.SortField> sortFields = Arrays.asList(ResultsProcessor.SortField.values());
        this.sortFieldDropdown = new SingleSelectDropdown<>(
                "Sort",
                sortFields,
                f -> f.displayName,
                ResultsProcessor.SortField.ALPHABETICAL,
                selected -> {}
        );

        List<ResultsProcessor.GroupBy> groupByOptions = Arrays.asList(ResultsProcessor.GroupBy.values());
        this.groupByDropdown = new SingleSelectDropdown<>(
                "Group",
                groupByOptions,
                g -> g.displayName,
                ResultsProcessor.GroupBy.MOD,
                selected -> {}
        );

        // Register dropdowns in order
        dropdowns.add(sortFieldDropdown);
        dropdowns.add(groupByDropdown);

        updateDropdownPositions();
    }

    public void updateLayout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        updateDropdownPositions();
    }

    private void updateDropdownPositions() {
        int startX = x + 2 + MODE_BUTTON_W + 3 + BUTTON_W + 3; // view-mode + sort-dir buttons
        int availableW = width - (startX - x) - FIELDS_BTN_W - 5; // reserve right edge for Fields picker

        int n = dropdowns.size();
        if (n == 0) return;

        int gap = 3;
        int totalGaps = (n - 1) * gap;
        int widthPerDropdown = (availableW - totalGaps) / n;

        int currentX = startX;
        for (int i = 0; i < n; i++) {
            Dropdown dropdown = dropdowns.get(i);
            int w = (i == n - 1)
                    ? (x + width - FIELDS_BTN_W - 4 - currentX)
                    : widthPerDropdown;
            dropdown.updatePosition(currentX, y + 3, Math.max(10, w));
            currentX += w + gap;
        }

        // Fields picker: fixed width, right-aligned
        fieldsPicker.updatePosition(x + width - FIELDS_BTN_W - 2, y + 3, FIELDS_BTN_W);
    }

    private int getDropdownWidth(Dropdown dropdown) {
        // Obsolete, widths are now calculated dynamically
        return 0;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int buttonX = x + 2;

        // View mode toggle button
        String modeLabel = viewMode == ViewMode.GRID ? "Grid" : "List";
        g.drawString(font, modeLabel, buttonX + 2, y + 3, AMITheme.TEXT_HEADER, false);
        buttonX += MODE_BUTTON_W + 3;

        // Sort direction button (▲/▼)
        String dirLabel = ascending ? "▲" : "▼";
        g.drawString(font, dirLabel, buttonX + 2, y + 3, AMITheme.TEXT_HEADER, false);

        // Render all registered dropdowns
        for (Dropdown dropdown : dropdowns) {
            dropdown.render(g, mouseX, mouseY);
        }
        fieldsPicker.render(g, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int buttonX = x + 2;

        // View mode toggle
        if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, y + 3, MODE_BUTTON_W, 14)) {
            viewMode = (viewMode == ViewMode.GRID) ? ViewMode.LIST : ViewMode.GRID;
            closeAllDropdowns();
            return true;
        }
        buttonX += MODE_BUTTON_W + 3;

        // Sort direction button
        if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, y + 3, BUTTON_W, 14)) {
            ascending = !ascending;
            closeAllDropdowns();
            return true;
        }

        // Fields picker
        if (fieldsPicker.mouseClicked(mouseX, mouseY, button)) {
            for (Dropdown d : dropdowns) d.close();
            return true;
        }

        // Handle dropdown clicks - close others when one is clicked
        for (Dropdown dropdown : dropdowns) {
            if (dropdown.mouseClicked(mouseX, mouseY, button)) {
                for (Dropdown other : dropdowns) {
                    if (other != dropdown) other.close();
                }
                fieldsPicker.close();
                return true;
            }
        }

        return false;
    }

    /** Renders only the open dropdown lists — call AFTER the tree view so they appear on top. */
    public void renderOpenDropdownLists(GuiGraphics g, int mouseX, int mouseY) {
        for (Dropdown dropdown : dropdowns) {
            dropdown.renderList(g, mouseX, mouseY);
        }
        fieldsPicker.renderList(g, mouseX, mouseY);
    }

    public void closeAllDropdowns() {
        for (Dropdown dropdown : dropdowns) dropdown.close();
        fieldsPicker.close();
    }


    public int getHeight() { return TOOLBAR_HEIGHT; }
    public ResultsProcessor.SortField getSortField() { return sortFieldDropdown.getSelected(); }
    public boolean isAscending() { return ascending; }
    public ResultsProcessor.GroupBy getGroupBy() { return groupByDropdown.getSelected(); }
    public Set<String> getSelectedMods() { return Set.of(); }
    public ViewMode getViewMode() { return viewMode; }

    public boolean isAnyDropdownOpen() {
        if (fieldsPicker.isOpen()) return true;
        for (Dropdown dropdown : dropdowns) {
            if (dropdown.isOpen()) return true;
        }
        return false;
    }

    public List<Dropdown> getRegisteredDropdowns() {
        return new ArrayList<>(dropdowns);
    }

    public void registerDropdown(Dropdown dropdown) {
        dropdowns.add(dropdown);
        updateDropdownPositions();
    }

}
