package com.sanhiruzu.ami.client.results;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.sanhiruzu.ami.index.SearchNode;

public class ResultsToolbar {
    private static final int TOOLBAR_HEIGHT = 20;
    private static final int BUTTON_W = 14;
    private static final int DROPDOWN_W = 80;
    private static final int MOD_FILTER_W = 60;

    private int x, y, width;
    private boolean ascending = true;

    // Registered dropdowns - add or remove here to customize the toolbar
    private final List<Dropdown> dropdowns = new ArrayList<>();

    // Specific dropdown references for getters
    private SingleSelectDropdown<ResultsProcessor.SortField> sortFieldDropdown;
    private SingleSelectDropdown<ResultsProcessor.GroupBy> groupByDropdown;
    private MultiSelectDropdown<String> modFilterDropdown;

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
                ResultsProcessor.GroupBy.DIMENSION,
                selected -> {}
        );

        this.modFilterDropdown = new MultiSelectDropdown<>(
                new ArrayList<>(),
                s -> s
        );

        // Register dropdowns in order
        dropdowns.add(sortFieldDropdown);
        dropdowns.add(groupByDropdown);
        dropdowns.add(modFilterDropdown);

        updateDropdownPositions();
    }

    public void updateLayout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        updateDropdownPositions();
    }

    private void updateDropdownPositions() {
        int buttonX = x + 2 + BUTTON_W + 3; // Account for sort direction button
        for (Dropdown dropdown : dropdowns) {
            dropdown.updatePosition(buttonX, y + 3, getDropdownWidth(dropdown));
            buttonX += getDropdownWidth(dropdown) + 3;
        }
    }

    private int getDropdownWidth(Dropdown dropdown) {
        if (dropdown == modFilterDropdown) return MOD_FILTER_W;
        return DROPDOWN_W;
    }

    public void setAvailableMods(Set<String> mods) {
        List<String> modList = new ArrayList<>(mods);
        Collections.sort(modList);
        modFilterDropdown.setOptions(modList);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        int buttonX = x + 2;

        // Sort direction button (⬆/⬇)
        String dirLabel = ascending ? "▲" : "▼";
        boolean dirHovered = isPointInRect(mouseX, mouseY, buttonX, y + 3, BUTTON_W, 14);
        int dirColor = dirHovered ? 0xFFAAAA44 : 0xFF888888;
        g.drawString(Minecraft.getInstance().font, dirLabel, buttonX + 2, y + 3, dirColor, false);

        // Render all registered dropdowns
        for (Dropdown dropdown : dropdowns) {
            dropdown.render(g, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int buttonX = x + 2;

        // Sort direction button
        if (isPointInRect((int) mouseX, (int) mouseY, buttonX, y + 3, BUTTON_W, 14)) {
            ascending = !ascending;
            closeAllDropdowns();
            return true;
        }

        // Handle dropdown clicks - close others when one is clicked
        for (Dropdown dropdown : dropdowns) {
            if (dropdown.mouseClicked(mouseX, mouseY, button)) {
                for (Dropdown other : dropdowns) {
                    if (other != dropdown) {
                        other.close();
                    }
                }
                return true;
            }
        }

        return false;
    }

    private void closeAllDropdowns() {
        for (Dropdown dropdown : dropdowns) {
            dropdown.close();
        }
    }

    private boolean isPointInRect(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }

    public int getHeight() { return TOOLBAR_HEIGHT; }
    public ResultsProcessor.SortField getSortField() { return sortFieldDropdown.getSelected(); }
    public boolean isAscending() { return ascending; }
    public ResultsProcessor.GroupBy getGroupBy() { return groupByDropdown.getSelected(); }
    public Set<String> getSelectedMods() { return modFilterDropdown.getSelected(); }

    public boolean isAnyDropdownOpen() {
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

    public Set<String> getAllMods(java.util.List<SearchNode> results) {
        return results.stream()
                .map(n -> n.id().getNamespace())
                .collect(Collectors.toSet());
    }
}
