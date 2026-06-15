package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.UniversalResultsPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Fabric stub for SidebarPanelWidget.
 * Full sidebar panel implementation is deferred to a later milestone.
 */
public class SidebarPanelWidget extends AbstractWidget {

    private final UniversalResultsPanel panel;

    public SidebarPanelWidget() {
        super(0, 0, 0, 0, Component.empty());
        this.panel = new UniversalResultsPanel(0, 0, 0, 0);
    }

    public UniversalResultsPanel getInnerPanel() {
        return panel;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
