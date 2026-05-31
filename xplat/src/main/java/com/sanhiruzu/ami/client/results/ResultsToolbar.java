package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

public class ResultsToolbar implements SearchState.Listener {
    public static final int TOOLBAR_HEIGHT = 20;
    public static final int BUTTON_H = 14;
    private static final int SORT_DIR_W = 18;
    private static final int COLLAPSE_TOGGLE_W = 18;
    private static final int MIN_DROPDOWN_W = 60;
    private static final int SCROLL_STEP = 18;
    private static final int ROW1_Y = 3;
    private static final List<ResultsProcessor.SortField> ITEM_SORT_FIELDS = List.of(
            ResultsProcessor.SortField.REGISTRY,
            ResultsProcessor.SortField.ALPHABETICAL,
            ResultsProcessor.SortField.COLOR,
            ResultsProcessor.SortField.MOD,
            ResultsProcessor.SortField.STORAGE_CAPACITY,
            ResultsProcessor.SortField.ENERGY_CAPACITY,
            ResultsProcessor.SortField.ENERGY_GENERATION,
            ResultsProcessor.SortField.FLUID_CAPACITY,
            ResultsProcessor.SortField.TOOL_SPEED,
            ResultsProcessor.SortField.TOOL_USES,
            ResultsProcessor.SortField.ARMOR_DEFENSE,
            ResultsProcessor.SortField.ARMOR_TOUGHNESS,
            ResultsProcessor.SortField.FOOD_NUTRITION,
            ResultsProcessor.SortField.FOOD_SATURATION,
            ResultsProcessor.SortField.DAMAGE,
            ResultsProcessor.SortField.HEALTH,
            ResultsProcessor.SortField.DPS
    );
    private static final EnumSet<ResultsProcessor.GroupBy> DEV_GROUPS = EnumSet.of(
            ResultsProcessor.GroupBy.TOPOLOGY,
            ResultsProcessor.GroupBy.SIMILARITY,
            ResultsProcessor.GroupBy.PROPERTIES
    );
    private final SearchState state;
    private final List<Dropdown> dropdowns = new ArrayList<>();
    private int x, y, width;
    private int contentWidth;
    private int scrollOffset;
    private Runnable onCollapseAll = null;
    private Runnable onExpandAll = null;
    private boolean collapseAllNext = true;
    private SingleSelectDropdown<ListLens> lensDropdown;
    private SingleSelectDropdown<ResultsProcessor.SortField> sortFieldDropdown;
    private SingleSelectDropdown<ResultsProcessor.GroupBy> groupByDropdown;
    private int sortDirX;
    private int sortDirY;
    private int collapseToggleX;
    private int collapseToggleY;
    public ResultsToolbar(int x, int y, int width, SearchState state) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.state = state;
        state.addListener(this);

        this.lensDropdown = new SingleSelectDropdown<>(
                Component.translatable("ami.gui.show"),
                state.getAvailableListLenses(),
                lens -> lens.displayName,
                state.getListLens(),
                state::setListLens
        );

        this.sortFieldDropdown = new SingleSelectDropdown<>(
                Component.translatable("ami.gui.sort"),
                sortOptions(),
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

        dropdowns.add(lensDropdown);
        dropdowns.add(sortFieldDropdown);
        dropdowns.add(groupByDropdown);

        updateDropdownPositions();
    }

    private static boolean isNumericSort(ResultsProcessor.SortField sortField) {
        return sortField == ResultsProcessor.SortField.STORAGE_CAPACITY
                || sortField == ResultsProcessor.SortField.ENERGY_CAPACITY
                || sortField == ResultsProcessor.SortField.ENERGY_GENERATION
                || sortField == ResultsProcessor.SortField.FLUID_CAPACITY
                || sortField == ResultsProcessor.SortField.TOOL_SPEED
                || sortField == ResultsProcessor.SortField.TOOL_USES
                || sortField == ResultsProcessor.SortField.ARMOR_DEFENSE
                || sortField == ResultsProcessor.SortField.ARMOR_TOUGHNESS
                || sortField == ResultsProcessor.SortField.FOOD_NUTRITION
                || sortField == ResultsProcessor.SortField.FOOD_SATURATION
                || sortField == ResultsProcessor.SortField.DAMAGE
                || sortField == ResultsProcessor.SortField.HEALTH
                || sortField == ResultsProcessor.SortField.DPS
                || sortField == ResultsProcessor.SortField.COUNT;
    }

    private static List<ResultsProcessor.GroupBy> groupOptions() {
        return Arrays.stream(ResultsProcessor.GroupBy.values())
                .filter(group -> AmiConfig.devMode || !DEV_GROUPS.contains(group))
                .collect(Collectors.toList());
    }

    @Override
    public void onSearchStateChanged(SearchState state) {
        updateLensOptions();
        updateGroupOptions();
        this.lensDropdown.setSelected(state.getListLens());
        this.sortFieldDropdown.setOptions(sortOptions());
        this.sortFieldDropdown.setSelected(state.getSortField());
        this.groupByDropdown.setSelected(state.getGroupBy());
        if (state.getViewMode() == ViewMode.LIST) {
            this.groupByDropdown.close();
        }
        updateDropdownPositions();
    }

    public void updateLayout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        updateDropdownPositions();
    }

    private void updateDropdownPositions() {
        int gap = 3;
        int fixedControlsW = SORT_DIR_W + COLLAPSE_TOGGLE_W;
        boolean listMode = state.getViewMode() == ViewMode.LIST;
        int dropdownCount = 2;
        int totalGaps = gap * (dropdownCount + 1);

        int minContentWidth = fixedControlsW + (dropdownCount * MIN_DROPDOWN_W) + totalGaps + 4;
        this.contentWidth = Math.max(width, minContentWidth);
        clampScrollOffset();

        int availableDropdownW = contentWidth - fixedControlsW - totalGaps - 4;
        int lensW = listMode ? Math.max(MIN_DROPDOWN_W, availableDropdownW / 2) : 0;
        int remainingDropdownW = availableDropdownW - lensW;
        int sortW = listMode
                ? Math.max(MIN_DROPDOWN_W, remainingDropdownW)
                : Math.max(MIN_DROPDOWN_W, remainingDropdownW / 2);
        int groupW = listMode ? 0 : Math.max(MIN_DROPDOWN_W, remainingDropdownW - sortW);

        int currentX = x + 2;
        // List: [Lens] [Sort] [SortDir] [Collapse]
        // Grid: [Group] [Sort] [SortDir] [Collapse]
        if (listMode) {
            lensDropdown.updatePosition(currentX, y + ROW1_Y, lensW);
            currentX += lensW + gap;
        } else {
            groupByDropdown.updatePosition(currentX, y + ROW1_Y, groupW);
            currentX += groupW + gap;
        }

        sortFieldDropdown.updatePosition(currentX, y + ROW1_Y, sortW);
        currentX += sortW + gap;

        sortDirX = currentX;
        sortDirY = y + ROW1_Y;
        currentX += SORT_DIR_W + gap;

        collapseToggleX = currentX;
        collapseToggleY = y + ROW1_Y;
    }

    public void resetCollapseState() {
        this.collapseAllNext = true;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        updateLensOptions();
        updateGroupOptions();
        var font = Minecraft.getInstance().font;
        boolean anyOpen = isAnyDropdownOpen();
        int effectiveMouseX = anyOpen ? -1 : mouseX;

        g.enableScissor(x, y, x + width, y + TOOLBAR_HEIGHT);
        g.pose().pushPose();
        g.pose().translate(-scrollOffset, 0, 0);

        // ── Row 1 ──────────────────────────────────────────────────────────
        if (state.getViewMode() == ViewMode.LIST) {
            lensDropdown.render(g, effectiveMouseX, mouseY);
        }
        sortFieldDropdown.render(g, effectiveMouseX, mouseY);

        // Sort Direction
        boolean sortDirHov = Dropdown.contains(effectiveMouseX, mouseY, sortDirX, sortDirY, SORT_DIR_W, BUTTON_H);
        drawButton(g, sortDirX, sortDirY, SORT_DIR_W, BUTTON_H, sortDirHov);
        int sortIconColor = sortDirHov ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER;
        AmiGuiIcons.sortDirection(g, sortDirX + SORT_DIR_W / 2, sortDirY + BUTTON_H / 2, sortIconColor, state.isAscending());

        if (state.getViewMode() != ViewMode.LIST) {
            groupByDropdown.render(g, effectiveMouseX, mouseY);
        }

        // Collapse/Expand Toggle
        boolean colHov = Dropdown.contains(effectiveMouseX, mouseY, collapseToggleX, collapseToggleY, COLLAPSE_TOGGLE_W, BUTTON_H);
        drawButton(g, collapseToggleX, collapseToggleY, COLLAPSE_TOGGLE_W, BUTTON_H, colHov);
        int collapseIconColor = colHov ? AMITheme.ACCENT_BLUE : AMITheme.TEXT_HEADER;
        int collapseCx = collapseToggleX + COLLAPSE_TOGGLE_W / 2;
        int collapseCy = collapseToggleY + BUTTON_H / 2;
        if (collapseAllNext) {
            AmiGuiIcons.collapseAll(g, collapseCx, collapseCy, collapseIconColor);
        } else {
            AmiGuiIcons.expandAll(g, collapseCx, collapseCy, collapseIconColor);
        }

        g.pose().popPose();
        g.disableScissor();
        renderScrollIndicators(g);
        renderHoveredTooltip(g, effectiveMouseX, mouseY);
    }

    private void renderHoveredTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (mouseX < 0 || isAnyDropdownOpen()) return;

        List<Component> tooltip = null;
        if (Dropdown.contains(mouseX + scrollOffset, mouseY, sortDirX, sortDirY, SORT_DIR_W, BUTTON_H)) {
            tooltip = List.of(
                    Component.translatable("ami.gui.tooltip.sort_direction", sortDirectionLabel()),
                    Component.translatable("ami.gui.tooltip.sort_scope")
            );
        } else if (state.getViewMode() == ViewMode.LIST && lensDropdown.isMouseOverButton(mouseX + scrollOffset, mouseY)) {
            tooltip = List.of(
                    Component.translatable("ami.gui.tooltip.show"),
                    Component.translatable("ami.gui.tooltip.show_scope")
            );
        } else if (sortFieldDropdown.isMouseOverButton(mouseX + scrollOffset, mouseY)) {
            tooltip = List.of(
                    Component.translatable("ami.gui.tooltip.sort"),
                    Component.translatable("ami.gui.tooltip.sort_scope")
            );
        } else if (state.getViewMode() != ViewMode.LIST && groupByDropdown.isMouseOverButton(mouseX + scrollOffset, mouseY)) {
            tooltip = List.of(
                    Component.translatable("ami.gui.tooltip.group"),
                    Component.translatable("ami.gui.tooltip.group_scope")
            );
        }

        if (tooltip != null) {
            AmiTooltipRenderer.render(g, Minecraft.getInstance().font, tooltip, Optional.empty(), mouseX, mouseY, true);
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

    private String sortDirectionLabel() {
        return isNumericSort(state.getSortField())
                ? (state.isAscending() ? "Low" : "High")
                : (state.isAscending() ? "A-Z" : "Z-A");
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int tx = (int) mouseX + scrollOffset;
        int ty = (int) mouseY;

        // Sort Dir
        if (Dropdown.contains(tx, ty, sortDirX, sortDirY, SORT_DIR_W, BUTTON_H)) {
            state.setAscending(!state.isAscending());
            closeAllDropdowns();
            return true;
        }

        // Collapse/Expand Toggle
        if (Dropdown.contains(tx, ty, collapseToggleX, collapseToggleY, COLLAPSE_TOGGLE_W, BUTTON_H)) {
            if (collapseAllNext) {
                onCollapseAll.run();
            } else {
                onExpandAll.run();
            }
            collapseAllNext = !collapseAllNext;
            closeAllDropdowns();
            return true;
        }
        // Row 1 dropdowns
        List<Dropdown> active = state.getViewMode() == ViewMode.LIST
                ? List.of(lensDropdown, sortFieldDropdown)
                : List.of(sortFieldDropdown, groupByDropdown);
        for (Dropdown d : active) {
            if (d.mouseClicked(mouseX + scrollOffset, mouseY, button)) {
                for (Dropdown other : active) if (other != d) other.close();
                return true;
            }
        }

        return false;
    }

    public void renderOpenDropdownLists(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 50);
        g.pose().translate(-scrollOffset, 0, 0);
        if (state.getViewMode() == ViewMode.LIST) lensDropdown.renderList(g, mouseX, mouseY);
        sortFieldDropdown.renderList(g, mouseX, mouseY);
        if (state.getViewMode() != ViewMode.LIST) groupByDropdown.renderList(g, mouseX, mouseY);
        g.pose().popPose();
    }

    public void closeAllDropdowns() {
        for (Dropdown d : dropdowns) d.close();
    }

    public boolean isAnyDropdownOpen() {
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

    private void updateLensOptions() {
        lensDropdown.setOptions(state.getAvailableListLenses());
    }

    private List<ResultsProcessor.SortField> sortOptions() {
        return state.getViewMode() == ViewMode.LIST
                ? state.getListLens().sortFields()
                : ITEM_SORT_FIELDS;
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

    public enum ViewMode {GRID, LIST}
}
