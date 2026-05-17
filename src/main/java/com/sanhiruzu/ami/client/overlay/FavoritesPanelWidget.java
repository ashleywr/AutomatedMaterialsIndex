package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.UniversalResultsPanel;
import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A panel that displays favorited items, typically shown on the left side.
 * Reuses UniversalResultsPanel for its grid and list views.
 */
public class FavoritesPanelWidget extends AbstractWidget {
    private final UniversalResultsPanel panel;

    public FavoritesPanelWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("ami.gui.favorites"));
        this.panel = new UniversalResultsPanel(x, y, width, height);
        this.panel.setFavoritesPanel(true);
        // Favorites panel usually stays in grid mode by default
        refresh();
    }

    public void refresh() {
        List<SearchNode> favorites = AmiFavoritesHandler.getInstance().getFavorites();
        panel.setEntries(favorites);
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.width = width;
        this.height = height;
        panel.updateLayout(x, y, width, height);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        panel.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !panel.isMouseOver(mouseX, mouseY)) return false;
        panel.mouseClickedScrollbar(mouseX, mouseY, button);
        return panel.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;
        panel.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.visible) return false;
        return panel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.visible || !panel.isMouseOver(mouseX, mouseY)) return false;
        return panel.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.visible) return false;
        return panel.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    public UniversalResultsPanel getInnerPanel() {
        return panel;
    }
}
