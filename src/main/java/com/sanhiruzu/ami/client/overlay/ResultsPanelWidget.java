package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.UniversalResultsPanel;
import net.minecraft.client.gui.GuiGraphics;

public class ResultsPanelWidget implements AmiWidget {
    private UniversalResultsPanel panel;
    private WidgetBounds bounds;

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (panel == null || bounds == null) return;

        panel.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderOverlay(GuiGraphics g, int mouseX, int mouseY) {
        if (panel == null) return;
        panel.getToolbar().renderOpenDropdownLists(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (panel == null) return false;
        return panel.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panel == null) return false;
        return panel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (panel == null) return false;
        panel.stopScrollbarDrag();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (panel == null) return false;
        return panel.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return false;
    }

    @Override
    public WidgetBounds getBounds() {
        return bounds != null ? bounds : new WidgetBounds(0, 0, 0, 0);
    }

    public void updateBounds(WidgetBounds bounds) {
        this.bounds = bounds;

        // Lazy-create panel on first layout
        if (panel == null) {
            panel = new UniversalResultsPanel(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        } else {
            panel.updateLayout(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        }
    }

    public UniversalResultsPanel getInnerPanel() {
        return panel;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (panel == null) return false;
        return panel.mouseClickedScrollbar(mouseX, mouseY, button);
    }
}
