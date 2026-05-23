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
    private static final int SORT_BUTTON_W   = 14;  // "▲"/"▼"
    private static final int RESET_BUTTON_W  = 18;  // reset icon
    private static final int COLLAPSE_BTN_W  = 18;  // "−"
    private static final int EXPAND_BTN_W    = 18;  // "+"
    private static final int DROPDOWN_W      = 80;
    private static final int MOD_FILTER_W    = 60;
    private static final int FIELDS_BTN_W    = 44;  // "Fields (3)"
    private static final int BUTTON_GAP      = 2;   // gap between buttons
    private static final int MIN_DROPDOWN_W  = 72;
    private static final int SCROLL_STEP     = 18;

    private int x, y, width;
    private final SearchState state;
    private int contentWidth;
    private int scrollOffset;

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
        int startX = x + 2 + SORT_BUTTON_W + BUTTON_GAP + RESET_BUTTON_W + BUTTON_GAP;
        // Account for collapse/expand buttons in list view
        if (!gridMode && onCollapseAll != null) {
            startX += COLLAPSE_BTN_W + BUTTON_GAP + EXPAND_BTN_W + BUTTON_GAP;
        }
        int headerControlsW = startX - x;
        int rightReserved = gridMode ? 0 : (FIELDS_BTN_W + 5);

        int n = dropdowns.size();
        if (n == 0) return;

        int gap = 3;
        int totalGaps = (n - 1) * gap;
        int minContentWidth = headerControlsW + rightReserved + (n * MIN_DROPDOWN_W) + totalGaps + 4;
        this.contentWidth = Math.max(width, minContentWidth);
        clampScrollOffset();

        int availableW = contentWidth - headerControlsW - rightReserved;
        int widthPerDropdown = (availableW - totalGaps) / n;
        int rightBound = x + contentWidth - rightReserved - 4;

        int currentX = startX;
        for (int i = 0; i < n; i++) {
            Dropdown dropdown = dropdowns.get(i);
            int w = (i == n - 1) ? (rightBound - currentX) : widthPerDropdown;
            dropdown.updatePosition(currentX, y + 3, Math.max(10, w));
            currentX += w + gap;
        }

        if (!gridMode) {
            // Position at the far right of virtual content space so it scrolls with the toolbar.
            // rightBound already reserves (FIELDS_BTN_W + 5) space, so align there.
            fieldsPicker.updatePosition(x + contentWidth - FIELDS_BTN_W - 2, y + 3, FIELDS_BTN_W);
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        boolean dropdownOpen = isAnyDropdownOpen();
        int effectiveMouseX = dropdownOpen ? -1 : mouseX;
        int buttonX = x + 2;
        int buttonY = y + 3;

        g.enableScissor(x, y, x + width, y + TOOLBAR_HEIGHT);
        g.pose().pushPose();
        g.pose().translate(-scrollOffset, 0, 0);

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

        // Render all registered dropdowns and fields picker (inside scroll transform)
        for (Dropdown dropdown : dropdowns) {
            dropdown.render(g, effectiveMouseX, mouseY);
        }
        if (state.getViewMode() != ViewMode.GRID) {
            fieldsPicker.render(g, effectiveMouseX, mouseY);
        }

        g.pose().popPose();
        g.disableScissor();
        renderScrollIndicators(g);
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
        int translatedMouseX = (int) mouseX + scrollOffset;
        int translatedMouseY = (int) mouseY;

        int buttonX = x + 2;
        int buttonY = y + 3;

        // Sort direction button
        if (Dropdown.contains(translatedMouseX, translatedMouseY, buttonX, buttonY, SORT_BUTTON_W, BUTTON_H)) {
            state.setAscending(!state.isAscending());
            closeAllDropdowns();
            return true;
        }
        buttonX += SORT_BUTTON_W + BUTTON_GAP;

        // Reset button
        if (Dropdown.contains(translatedMouseX, translatedMouseY, buttonX, buttonY, RESET_BUTTON_W, BUTTON_H)) {
            state.reset();
            RowFieldConfig.setSubtitleFields(List.of(RowField.MOD_NAME));
            closeAllDropdowns();
            return true;
        }
        buttonX += RESET_BUTTON_W + BUTTON_GAP;

        // Collapse/Expand all buttons (only in list view)
        if (state.getViewMode() != ViewMode.GRID && onCollapseAll != null) {
            // Collapse button
            if (Dropdown.contains(translatedMouseX, translatedMouseY, buttonX, buttonY, COLLAPSE_BTN_W, BUTTON_H)) {
                onCollapseAll.run();
                return true;
            }
            buttonX += COLLAPSE_BTN_W + BUTTON_GAP;

            // Expand button
            if (Dropdown.contains(translatedMouseX, translatedMouseY, buttonX, buttonY, EXPAND_BTN_W, BUTTON_H)) {
                onExpandAll.run();
                return true;
            }
        }

        // Fields picker — only in list mode
        if (state.getViewMode() != ViewMode.GRID && fieldsPicker.mouseClicked(mouseX + scrollOffset, mouseY, button)) {
            for (Dropdown d : dropdowns) d.close();
            return true;
        }

        // Handle dropdown clicks - close others when one is clicked
        for (Dropdown dropdown : dropdowns) {
            if (dropdown.mouseClicked(mouseX + scrollOffset, mouseY, button)) {
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
        g.pose().translate(-scrollOffset, 0, 0);
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

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!hasHorizontalOverflow() || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scrollOffset -= (int) Math.signum(scrollDelta) * SCROLL_STEP;
        clampScrollOffset();
        return true;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + TOOLBAR_HEIGHT;
    }

    private boolean hasHorizontalOverflow() {
        return contentWidth > width;
    }

    private void clampScrollOffset() {
        int maxOffset = Math.max(0, contentWidth - width);
        if (scrollOffset < 0) {
            scrollOffset = 0;
        } else if (scrollOffset > maxOffset) {
            scrollOffset = maxOffset;
        }
    }

    private void renderScrollIndicators(GuiGraphics g) {
        if (!hasHorizontalOverflow()) {
            return;
        }

        var font = Minecraft.getInstance().font;
        int indicatorW = 10;
        int top = y + 3;
        int bottom = top + BUTTON_H;

        if (scrollOffset > 0) {
            g.fill(x, top, x + indicatorW, bottom, AMITheme.SCROLL_INDICATOR_BG);
            g.drawCenteredString(font, "<", x + indicatorW / 2, top + 3, AMITheme.TEXT_SUBTLE);
        }
        if (scrollOffset < contentWidth - width) {
            g.fill(x + width - indicatorW, top, x + width, bottom, AMITheme.SCROLL_INDICATOR_BG);
            g.drawCenteredString(font, ">", x + width - indicatorW / 2, top + 3, AMITheme.TEXT_SUBTLE);
        }
    }
}
