package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.*;

public class ResultsToolbar implements SearchState.Listener {
    public enum ViewMode { GRID, LIST }

    private static final int TOOLBAR_HEIGHT  = 20;
    private static final int MODE_BUTTON_W  = 26; // wide enough for "Grid"/"List"
    private static final int BUTTON_W       = 14;
    private static final int DROPDOWN_W     = 80;
    private static final int MOD_FILTER_W   = 60;
    private static final int FIELDS_BTN_W   = 44; // wide enough for "Fields (3)"
    private static final int RESET_BUTTON_W = 32;

    private int x, y, width;
    private final SearchState state;

    // Registered dropdowns - add or remove here to customize the toolbar
    private final List<Dropdown> dropdowns = new ArrayList<>();

    // Specific dropdown references for getters
    private SingleSelectDropdown<ResultsProcessor.SortField> sortFieldDropdown;
    private SingleSelectDropdown<ResultsProcessor.GroupBy> groupByDropdown;

    // Field picker — pinned to right end of toolbar, not in the auto-sized list
    private final RowFieldPickerDropdown fieldsPicker = new RowFieldPickerDropdown();

    public ResultsToolbar(int x, int y, int width, SearchState state) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.state = state;
        state.addListener(this);

        // Create dropdowns
        List<ResultsProcessor.SortField> sortFields = Arrays.asList(ResultsProcessor.SortField.values());
        this.sortFieldDropdown = new SingleSelectDropdown<>(
                Component.translatable("ami.gui.sort"),
                sortFields,
                f -> f.displayName,
                state.getSortField(),
                selected -> state.setSortField(selected)
        );

        List<ResultsProcessor.GroupBy> groupByOptions = Arrays.asList(ResultsProcessor.GroupBy.values());
        this.groupByDropdown = new SingleSelectDropdown<>(
                Component.translatable("ami.gui.group"),
                groupByOptions,
                g -> g.displayName,
                state.getGroupBy(),
                selected -> state.setGroupBy(selected)
        );

        // Register dropdowns in order
        dropdowns.add(sortFieldDropdown);
        dropdowns.add(groupByDropdown);

        updateDropdownPositions();
    }

    @Override
    public void onSearchStateChanged(SearchState state) {
        this.sortFieldDropdown.setSelected(state.getSortField());
        this.groupByDropdown.setSelected(state.getGroupBy());
    }

    public void updateLayout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        updateDropdownPositions();
    }

    private void updateDropdownPositions() {
        boolean gridMode = state.getViewMode() == ViewMode.GRID;
        int startX = x + 2 + MODE_BUTTON_W + 3 + BUTTON_W + 3 + RESET_BUTTON_W + 3;
        // In grid mode the Fields picker is hidden, so give its space to Sort/Group dropdowns.
        int rightReserved = gridMode ? 0 : (FIELDS_BTN_W + 5);
        int availableW = width - (startX - x) - rightReserved;

        int n = dropdowns.size();
        if (n == 0) return;

        int gap = 3;
        int totalGaps = (n - 1) * gap;
        int widthPerDropdown = (availableW - totalGaps) / n;
        int rightBound = x + width - rightReserved - 4;

        int currentX = startX;
        for (int i = 0; i < n; i++) {
            Dropdown dropdown = dropdowns.get(i);
            int w = (i == n - 1) ? (rightBound - currentX) : widthPerDropdown;
            dropdown.updatePosition(currentX, y + 3, Math.max(10, w));
            currentX += w + gap;
        }

        if (!gridMode) {
            fieldsPicker.updatePosition(x + width - FIELDS_BTN_W - 2, y + 3, FIELDS_BTN_W);
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int buttonX = x + 2;

        // View mode toggle button
        String modeLabel = state.getViewMode() == ViewMode.GRID ? "Grid" : "List";
        g.drawString(font, modeLabel, buttonX + 2, y + 3, AMITheme.TEXT_HEADER, false);
        buttonX += MODE_BUTTON_W + 3;

        // Sort direction button (▲/▼)
        String dirLabel = state.isAscending() ? "▲" : "▼";
        g.drawString(font, dirLabel, buttonX + 2, y + 3, AMITheme.TEXT_HEADER, false);
        buttonX += BUTTON_W + 3;

        // Reset button (↺)
        boolean resetHovered = Dropdown.contains(mouseX, mouseY, buttonX, y + 3, RESET_BUTTON_W, 14);
        g.drawString(font, "↺", buttonX + 2, y + 3, resetHovered ? 0xFFFFFFFF : AMITheme.TEXT_SUBTLE, false);

        // Render all registered dropdowns
        for (Dropdown dropdown : dropdowns) {
            dropdown.render(g, mouseX, mouseY);
        }
        if (state.getViewMode() != ViewMode.GRID) {
            fieldsPicker.render(g, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int buttonX = x + 2;

        // View mode toggle
        if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, y + 3, MODE_BUTTON_W, 14)) {
            state.setViewMode(state.getViewMode() == ViewMode.GRID ? ViewMode.LIST : ViewMode.GRID);
            closeAllDropdowns();
            updateDropdownPositions(); // reclaim / restore Fields space
            return true;
        }
        buttonX += MODE_BUTTON_W + 3;

        // Sort direction button
        if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, y + 3, BUTTON_W, 14)) {
            state.setAscending(!state.isAscending());
            closeAllDropdowns();
            return true;
        }
        buttonX += BUTTON_W + 3;

        // Reset button
        if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, y + 3, RESET_BUTTON_W, 14)) {
            state.reset();
            RowFieldConfig.setSubtitleFields(List.of(RowField.MOD_NAME));
            closeAllDropdowns();
            return true;
        }

        // Fields picker — only in list mode
        if (state.getViewMode() != ViewMode.GRID && fieldsPicker.mouseClicked(mouseX, mouseY, button)) {
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
        g.pose().pushPose();
        g.pose().translate(0, 0, 400); // Lift above icons (Z=150) and other UI elements
        for (Dropdown dropdown : dropdowns) {
            dropdown.renderList(g, mouseX, mouseY);
        }
        if (state.getViewMode() != ViewMode.GRID) {
            fieldsPicker.renderList(g, mouseX, mouseY);
        }
        g.pose().popPose();
    }

    public void closeAllDropdowns() {
        for (Dropdown dropdown : dropdowns) dropdown.close();
        fieldsPicker.close();
    }


    public int getHeight() { return TOOLBAR_HEIGHT; }

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
