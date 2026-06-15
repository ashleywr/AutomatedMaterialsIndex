package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.UniversalResultsPanel;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

/**
 * Fabric stub for OverlayWidgetManager.
 * Full overlay widget management is deferred to a later milestone.
 * Provides the minimum API surface required by xplat code so the project compiles.
 */
public class OverlayWidgetManager {

    // TODO(Milestone C+): implement Fabric overlay widget manager with Fabric event hooks

    private final SearchBarWidget searchBar;

    public OverlayWidgetManager() {
        this.searchBar = new SearchBarWidget(new AbstractSearchBarWidget.Listener() {
            @Override public void onQueryChanged(String query) {}
            @Override public void onSearchBarDoubleClicked(String query) {}
            @Override public void onSearchBarCleared() {}
        });
    }

    public SearchBarWidget getSearchBar() {
        return searchBar;
    }

    public com.sanhiruzu.ami.client.overlay.AmiButtonWidget getAmiButton() {
        return null;
    }

    public boolean isPanelVisible() {
        return false;
    }

    public void setPanelVisible(boolean visible) {
    }

    public boolean isInLayoutMode() {
        return false;
    }

    public boolean hasOpenContextMenu() {
        return false;
    }

    public boolean isMouseOverPanel(double mouseX, double mouseY) {
        return false;
    }

    public SearchNode getHoveredNode() {
        return null;
    }

    public SidebarPanelWidget getFavoritesPanelAt(double mouseX, double mouseY) {
        return null;
    }

    public boolean hasVisibleFavoritesPanel() {
        return false;
    }

    public List<WidgetBounds> getExclusionBounds() {
        return List.of();
    }

    public WidgetBounds getResultsBounds() {
        return null;
    }

    public List<GuiEventListener> getPanelListeners() {
        return List.of();
    }

    public List<UniversalResultsPanel> getDebugVisibleResultPanels() {
        return List.of();
    }

    public void refreshEntriesForRuntimeIndexUpdate() {
    }

    public void refreshSidebars() {
    }

    public void refreshLayoutIfNeeded(Screen screen) {
    }

    public void computeLayouts(AbstractContainerScreen<?> containerScreen, int screenW, int screenH) {
    }

    public void computeLayouts(Screen screen, int screenW, int screenH) {
    }

    public void invalidateLayout() {
    }

    public void tick() {
    }

    public void renderBase(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
    }

    public void renderTopLayer(net.minecraft.client.gui.GuiGraphics g, int mx, int my) {
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }
}
