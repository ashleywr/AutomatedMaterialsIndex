package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.*;

public class ResultsToolbar implements SearchState.Listener {
    public enum ViewMode { GRID, LIST }

    public  static final int TOOLBAR_HEIGHT  = 20;
    public  static final int BUTTON_H        = 14;  // all buttons are this height
    private static final int MODE_BUTTON_W   = 28;  // "Grid"/"List"
    private static final int SORT_BUTTON_W   = 14;  // "▲"/"▼"
    private static final int RESET_BUTTON_W  = 18;  // reset icon
    private static final int COLLAPSE_BTN_W  = 18;  // "−"
    private static final int EXPAND_BTN_W    = 18;  // "+"
    private static final int DROPDOWN_W      = 80;
    private static final int MOD_FILTER_W    = 60;
    private static final int FIELDS_BTN_W    = 44;  // "Fields (3)"
    private static final int BUTTON_GAP      = 2;   // gap between buttons

    private int x, y, width;
    private final SearchState state;

    // Registered dropdowns - add or remove here to customize the toolbar
    private final List<Dropdown> dropdowns = new ArrayList<>();

    // Collapse/expand callbacks (only shown in list view with groups)
    private Runnable onCollapseAll = null;
    private Runnable onExpandAll = null;

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
        int startX = x + 2 + MODE_BUTTON_W + BUTTON_GAP + SORT_BUTTON_W + BUTTON_GAP + RESET_BUTTON_W + BUTTON_GAP;
        // Account for collapse/expand buttons in list view
        if (!gridMode && onCollapseAll != null) {
            startX += COLLAPSE_BTN_W + BUTTON_GAP + EXPAND_BTN_W + BUTTON_GAP;
        }
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
        boolean dropdownOpen = isAnyDropdownOpen();
        int effectiveMouseX = dropdownOpen ? -1 : mouseX;
        int buttonX = x + 2;
        int buttonY = y + 3;

        // View mode toggle button
        String modeLabel = state.getViewMode() == ViewMode.GRID ? "Grid" : "List";
        boolean modeHovered = Dropdown.contains(effectiveMouseX, mouseY, buttonX, buttonY, MODE_BUTTON_W, BUTTON_H);
        drawButton(g, buttonX, buttonY, MODE_BUTTON_W, BUTTON_H, modeHovered);
        g.drawCenteredString(font, modeLabel, buttonX + MODE_BUTTON_W / 2, buttonY + 3, AMITheme.TEXT_HEADER);
        buttonX += MODE_BUTTON_W + BUTTON_GAP;

        // Sort direction button (▲/▼)
        String dirLabel = state.isAscending() ? "▲" : "▼";
        boolean sortHovered = Dropdown.contains(effectiveMouseX, mouseY, buttonX, buttonY, SORT_BUTTON_W, BUTTON_H);
        drawButton(g, buttonX, buttonY, SORT_BUTTON_W, BUTTON_H, sortHovered);
        g.drawCenteredString(font, dirLabel, buttonX + SORT_BUTTON_W / 2, buttonY + 3, AMITheme.TEXT_HEADER);
        buttonX += SORT_BUTTON_W + BUTTON_GAP;

        // Reset button
        boolean resetHovered = Dropdown.contains(effectiveMouseX, mouseY, buttonX, buttonY, RESET_BUTTON_W, BUTTON_H);
        drawButton(g, buttonX, buttonY, RESET_BUTTON_W, BUTTON_H, resetHovered);
        int resetColor = resetHovered ? AMITheme.WHITE : AMITheme.TEXT_SUBTLE;
        com.sanhiruzu.ami.client.AmiGuiIcons.reset(g,
                buttonX + RESET_BUTTON_W / 2, buttonY + BUTTON_H / 2 + 1, resetColor);
        buttonX += RESET_BUTTON_W + BUTTON_GAP;

        // Collapse/Expand all buttons (only in list view)
        if (state.getViewMode() != ViewMode.GRID && onCollapseAll != null) {
            // Collapse button (−)
            boolean collapseHovered = Dropdown.contains(effectiveMouseX, mouseY, buttonX, buttonY, COLLAPSE_BTN_W, BUTTON_H);
            drawButton(g, buttonX, buttonY, COLLAPSE_BTN_W, BUTTON_H, collapseHovered);
            g.drawCenteredString(font, "−", buttonX + COLLAPSE_BTN_W / 2, buttonY + 3, AMITheme.TEXT_HEADER);
            buttonX += COLLAPSE_BTN_W + BUTTON_GAP;

            // Expand button (+)
            boolean expandHovered = Dropdown.contains(effectiveMouseX, mouseY, buttonX, buttonY, EXPAND_BTN_W, BUTTON_H);
            drawButton(g, buttonX, buttonY, EXPAND_BTN_W, BUTTON_H, expandHovered);
            g.drawCenteredString(font, "+", buttonX + EXPAND_BTN_W / 2, buttonY + 3, AMITheme.TEXT_HEADER);
            buttonX += EXPAND_BTN_W + BUTTON_GAP;
        }

        // Render all registered dropdowns
        for (Dropdown dropdown : dropdowns) {
            dropdown.render(g, effectiveMouseX, mouseY);
        }
        if (state.getViewMode() != ViewMode.GRID) {
            fieldsPicker.render(g, effectiveMouseX, mouseY);
        }
    }

    /** Draw a styled button background with border. */
    private void drawButton(GuiGraphics g, int bx, int by, int bw, int bh, boolean hovered) {
        int bgColor = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        // Background
        g.fill(bx, by, bx + bw, by + bh, bgColor);
        // Border
        g.fill(bx, by, bx + bw, by + 1, AMITheme.BORDER_DARK);           // top
        g.fill(bx, by + bh - 1, bx + bw, by + bh, AMITheme.BORDER_DARK); // bottom
        g.fill(bx, by, bx + 1, by + bh, AMITheme.BORDER_DARK);           // left
        g.fill(bx + bw - 1, by, bx + bw, by + bh, AMITheme.BORDER_DARK); // right
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int buttonX = x + 2;
        int buttonY = y + 3;

        // View mode toggle
        if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, buttonY, MODE_BUTTON_W, BUTTON_H)) {
            state.setViewMode(state.getViewMode() == ViewMode.GRID ? ViewMode.LIST : ViewMode.GRID);
            closeAllDropdowns();
            updateDropdownPositions(); // reclaim / restore Fields space
            return true;
        }
        buttonX += MODE_BUTTON_W + BUTTON_GAP;

        // Sort direction button
        if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, buttonY, SORT_BUTTON_W, BUTTON_H)) {
            state.setAscending(!state.isAscending());
            closeAllDropdowns();
            return true;
        }
        buttonX += SORT_BUTTON_W + BUTTON_GAP;

        // Reset button
        if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, buttonY, RESET_BUTTON_W, BUTTON_H)) {
            state.reset();
            RowFieldConfig.setSubtitleFields(List.of(RowField.MOD_NAME));
            closeAllDropdowns();
            return true;
        }
        buttonX += RESET_BUTTON_W + BUTTON_GAP;

        // Collapse/Expand all buttons (only in list view)
        if (state.getViewMode() != ViewMode.GRID && onCollapseAll != null) {
            // Collapse button
            if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, buttonY, COLLAPSE_BTN_W, BUTTON_H)) {
                onCollapseAll.run();
                return true;
            }
            buttonX += COLLAPSE_BTN_W + BUTTON_GAP;

            // Expand button
            if (Dropdown.contains((int) mouseX, (int) mouseY, buttonX, buttonY, EXPAND_BTN_W, BUTTON_H)) {
                onExpandAll.run();
                return true;
            }
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

    public void setCollapseExpandCallbacks(Runnable onCollapseAll, Runnable onExpandAll) {
        this.onCollapseAll = onCollapseAll;
        this.onExpandAll = onExpandAll;
    }
}
