package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResultsToolbar implements SearchState.Listener {
    public enum ViewMode {GRID, LIST}

    public static final int TOOLBAR_HEIGHT = 36;
    public static final int BUTTON_H = 14;
    private static final int SORT_DIR_W = 14;
    private static final int COLLAPSE_TOGGLE_W = 18;
    private static final int DROPDOWN_W = 75;
    private static final int FIELDS_BTN_W = 50;
    private static final int BUTTON_GAP = 2;
    private static final int MIN_DROPDOWN_W = 60;
    private static final int SCROLL_STEP = 18;
    private static final int ROW1_Y = 3;
    private static final int ROW2_Y = 20;
    private static final List<ResultsProcessor.SortField> ITEM_SORT_FIELDS = List.of(
            ResultsProcessor.SortField.REGISTRY,
            ResultsProcessor.SortField.ALPHABETICAL,
            ResultsProcessor.SortField.COLOR,
            ResultsProcessor.SortField.MOD,
            ResultsProcessor.SortField.STORAGE_CAPACITY,
            ResultsProcessor.SortField.DPS
    );
    private static final EnumSet<ResultsProcessor.GroupBy> DEV_GROUPS = EnumSet.of(
            ResultsProcessor.GroupBy.TOPOLOGY,
            ResultsProcessor.GroupBy.SIMILARITY,
            ResultsProcessor.GroupBy.PROPERTIES
    );

    private int x, y, width;
    private final SearchState state;
    private int contentWidth;
    private int scrollOffset;

    private final List<Dropdown> dropdowns = new ArrayList<>();
    private Runnable onCollapseAll = null;
    private Runnable onExpandAll = null;

    private boolean collapseAllNext = true;

    private SingleSelectDropdown<ResultsProcessor.SortField> sortFieldDropdown;
    private SingleSelectDropdown<ResultsProcessor.GroupBy> groupByDropdown;
    private final RowFieldPickerDropdown fieldsPicker = new RowFieldPickerDropdown();

    public ResultsToolbar(int x, int y, int width, SearchState state) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.state = state;
        state.addListener(this);

        this.sortFieldDropdown = new SingleSelectDropdown<>(
                Component.translatable("ami.gui.sort"),
                ITEM_SORT_FIELDS,
                f -> f.displayName,
                state.getSortField(),
                state::setSortField
        );

        this.groupByDropdown = new SingleSelectDropdown<>(
                Component.translatable("ami.gui.group"),
                groupOptions(),
                g -> g.displayName,
                state.getGroupBy(),
                state::setGroupBy
        );

        dropdowns.add(sortFieldDropdown);
        dropdowns.add(groupByDropdown);

        updateDropdownPositions();
    }

    @Override
    public void onSearchStateChanged(SearchState state) {
        updateGroupOptions();
        this.sortFieldDropdown.setSelected(state.getSortField());
        this.groupByDropdown.setSelected(state.getGroupBy());
        updateDropdownPositions();
    }

    public void updateLayout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        updateDropdownPositions();
    }

    private List<Dropdown> getActiveDropdowns() {
        List<Dropdown> active = new ArrayList<>();
        active.add(groupByDropdown);
        active.add(sortFieldDropdown);
        return active;
    }

    private void updateDropdownPositions() {
        boolean gridMode = state.getViewMode() == ViewMode.GRID;

        // Row 1 left-side buttons: SortDir, CollapseToggle
        int row1LeftW = SORT_DIR_W + BUTTON_GAP + COLLAPSE_TOGGLE_W + BUTTON_GAP;

        int rightReserved = gridMode ? 5 : (FIELDS_BTN_W + 5);

        List<Dropdown> active = getActiveDropdowns();
        int gap = 3;
        int n = active.size();
        int totalGaps = Math.max(0, n - 1) * gap;

        int minContentWidth = row1LeftW + rightReserved + (n * MIN_DROPDOWN_W) + totalGaps + 4;
        this.contentWidth = Math.max(width, minContentWidth);
        clampScrollOffset();

        int availableW = contentWidth - row1LeftW - rightReserved;
        int widthPerDropdown = n > 0 ? (availableW - totalGaps) / n : 0;
        int rightBound = x + contentWidth - rightReserved;

        int currentX = x + 2 + row1LeftW;
        for (int i = 0; i < n; i++) {
            Dropdown dropdown = active.get(i);
            int w = (i == n - 1) ? (rightBound - currentX) : widthPerDropdown;
            dropdown.updatePosition(currentX, y + ROW1_Y, Math.max(10, w));
            currentX += w + gap;
        }

        if (!gridMode) {
            fieldsPicker.updatePosition(x + contentWidth - FIELDS_BTN_W - 2, y + ROW2_Y, FIELDS_BTN_W);
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        updateGroupOptions();
        var font = Minecraft.getInstance().font;
        boolean anyOpen = isAnyDropdownOpen();
        int effectiveMouseX = anyOpen ? -1 : mouseX;

        g.enableScissor(x, y, x + width, y + TOOLBAR_HEIGHT);
        g.pose().pushPose();
        g.pose().translate(-scrollOffset, 0, 0);

        // ── Row 1 ──────────────────────────────────────────────────────────
        int curX = x + 2;
        int row1Y = y + ROW1_Y;

        // 1. Sort Direction
        boolean sortDirHov = Dropdown.contains(effectiveMouseX, mouseY, curX, row1Y, SORT_DIR_W, BUTTON_H);
        drawButton(g, curX, row1Y, SORT_DIR_W, BUTTON_H, sortDirHov);
        String dirChar = state.isAscending() ? "↑" : "↓";
        g.drawCenteredString(font, dirChar, curX + SORT_DIR_W / 2, row1Y + 3, sortDirHov ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER);
        curX += SORT_DIR_W + BUTTON_GAP;

        // 2. Collapse/Expand Toggle
        {
        boolean colHov = Dropdown.contains(effectiveMouseX, mouseY, curX, row1Y, COLLAPSE_TOGGLE_W, BUTTON_H);
        drawButton(g, curX, row1Y, COLLAPSE_TOGGLE_W, BUTTON_H, colHov);
        String arrow = collapseAllNext ? "▼" : "▶";
        g.drawCenteredString(font, arrow, curX + COLLAPSE_TOGGLE_W / 2, row1Y + 2, colHov ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER);
        curX += COLLAPSE_TOGGLE_W + BUTTON_GAP;
        }

        // 3. Dropdowns
        for (Dropdown d : getActiveDropdowns()) {
            d.render(g, effectiveMouseX, mouseY);
        }

        // ── Row 2 ──────────────────────────────────────────────────────────
        if (state.getViewMode() == ViewMode.LIST) {
            fieldsPicker.render(g, effectiveMouseX, mouseY);
        }

        g.pose().popPose();
        g.disableScissor();
        renderScrollIndicators(g);
        renderHoveredTooltip(g, effectiveMouseX, mouseY);
    }

    private void renderHoveredTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (mouseX < 0 || isAnyDropdownOpen()) return;

        List<Component> tooltip = null;
        int curX = x + 2 - scrollOffset;
        int row1Y = y + ROW1_Y;
        if (Dropdown.contains(mouseX, mouseY, curX, row1Y, SORT_DIR_W, BUTTON_H)) {
            tooltip = List.of(
                    Component.translatable(state.isAscending() ? "ami.gui.tooltip.sort_ascending" : "ami.gui.tooltip.sort_descending"),
                    Component.translatable("ami.gui.tooltip.sort_scope")
            );
        } else if (sortFieldDropdown.isMouseOverButton(mouseX + scrollOffset, mouseY)) {
            tooltip = List.of(
                    Component.translatable("ami.gui.tooltip.sort"),
                    Component.translatable("ami.gui.tooltip.sort_scope")
            );
        } else if (groupByDropdown.isMouseOverButton(mouseX + scrollOffset, mouseY)) {
            tooltip = List.of(
                    Component.translatable("ami.gui.tooltip.group"),
                    Component.translatable("ami.gui.tooltip.group_scope")
            );
        }

        if (tooltip != null) {
            AmiTooltipRenderer.renderLeftOfCursor(g, Minecraft.getInstance().font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    private void drawButton(GuiGraphics g, int bx, int by, int bw, int bh, boolean hovered) {
        int bgColor = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        g.fill(bx, by, bx + bw, by + bh, bgColor);
        
        int borderColor = AMITheme.SECTION_SEP;
        g.fill(bx, by, bx + bw, by + 1, borderColor);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, borderColor);
        g.fill(bx, by, bx + 1, by + bh, borderColor);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, borderColor);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int tx = (int) mouseX + scrollOffset;
        int ty = (int) mouseY;

        int curX = x + 2;
        int row1Y = y + ROW1_Y;

        // Sort Dir
        if (Dropdown.contains(tx, ty, curX, row1Y, SORT_DIR_W, BUTTON_H)) {
            state.setAscending(!state.isAscending());
            closeAllDropdowns();
            return true;
        }
        curX += SORT_DIR_W + BUTTON_GAP;

        // Collapse/Expand Toggle
        if (Dropdown.contains(tx, ty, curX, row1Y, COLLAPSE_TOGGLE_W, BUTTON_H)) {
            if (collapseAllNext) {
                onCollapseAll.run();
            } else {
                onExpandAll.run();
            }
            collapseAllNext = !collapseAllNext;
            closeAllDropdowns();
            return true;
        }
        curX += COLLAPSE_TOGGLE_W + BUTTON_GAP;

        // Fields picker (Row 2)
        if (state.getViewMode() == ViewMode.LIST && fieldsPicker.mouseClicked(mouseX + scrollOffset, mouseY, button)) {
            closeAllDropdowns();
            return true;
        }

        // Row 1 dropdowns
        List<Dropdown> active = getActiveDropdowns();
        for (Dropdown d : active) {
            if (d.mouseClicked(mouseX + scrollOffset, mouseY, button)) {
                for (Dropdown other : active) if (other != d) other.close();
                fieldsPicker.close();
                return true;
            }
        }

        return false;
    }

    public void renderOpenDropdownLists(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.pose().translate(-scrollOffset, 0, 0);
        for (Dropdown d : getActiveDropdowns()) d.renderList(g, mouseX, mouseY);
        if (state.getViewMode() == ViewMode.LIST) fieldsPicker.renderList(g, mouseX, mouseY);
        g.pose().popPose();
    }

    public void closeAllDropdowns() {
        for (Dropdown d : dropdowns) d.close();
        fieldsPicker.close();
    }

    public boolean isAnyDropdownOpen() {
        if (fieldsPicker.isOpen()) return true;
        for (Dropdown d : dropdowns) if (d.isOpen()) return true;
        return false;
    }

    public void setCollapseExpandCallbacks(Runnable onCollapseAll, Runnable onExpandAll) {
        this.onCollapseAll = onCollapseAll;
        this.onExpandAll = onExpandAll;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (contentWidth <= width || !isMouseOver(mouseX, mouseY)) return false;
        scrollOffset -= (int) Math.signum(scrollDelta) * SCROLL_STEP;
        clampScrollOffset();
        return true;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + TOOLBAR_HEIGHT;
    }

    private void clampScrollOffset() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, contentWidth - width));
    }

    private void updateGroupOptions() {
        groupByDropdown.setOptions(groupOptions());
        if (!AmiConfig.devMode && DEV_GROUPS.contains(state.getGroupBy())) {
            state.setGroupBy(ResultsProcessor.GroupBy.CATEGORY);
        }
    }

    private static List<ResultsProcessor.GroupBy> groupOptions() {
        return Arrays.stream(ResultsProcessor.GroupBy.values())
                .filter(group -> AmiConfig.devMode || !DEV_GROUPS.contains(group))
                .collect(Collectors.toList());
    }

    private void renderScrollIndicators(GuiGraphics g) {
        if (contentWidth <= width) return;
        var font = Minecraft.getInstance().font;
        int indicatorW = 10;
        int top = y + ROW1_Y;
        if (scrollOffset > 0) {
            g.fill(x, top, x + indicatorW, top + BUTTON_H, AMITheme.SCROLL_INDICATOR_BG);
            g.drawCenteredString(font, "<", x + indicatorW / 2, top + 3, AMITheme.TEXT_SUBTLE);
        }
        if (scrollOffset < contentWidth - width) {
            g.fill(x + width - indicatorW, top, x + width, top + BUTTON_H, AMITheme.SCROLL_INDICATOR_BG);
            g.drawCenteredString(font, ">", x + width - indicatorW / 2, top + 3, AMITheme.TEXT_SUBTLE);
        }
    }
}
