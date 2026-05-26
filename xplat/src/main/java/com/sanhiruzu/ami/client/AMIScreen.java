package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

import com.sanhiruzu.ami.forge.AMI;
public class AMIScreen extends Screen {
    private UniversalResultsPanel resultsPanel;

    public AMIScreen() {
        super(Component.translatable("ami.gui.registry_tree"));
    }

    @Override
    protected void init() {
        // Optimized padding and centered width for 856x482
        int panelW = Math.min(width - 20, 600); // Max width for readability on ultra-wide
        int panelH = height - 60;
        int panelX = (width - panelW) / 2;
        int panelY = 40;

        this.resultsPanel = new UniversalResultsPanel(panelX, panelY, panelW, panelH);
        this.resultsPanel.setSearchService(com.sanhiruzu.ami.index.SearchService.buildFrom(GlobalIndex.getInstance()));

        var all = new ArrayList<SearchNode>();
        for (NodeType t : NodeType.atlasValues()) {
            all.addAll(GlobalIndex.getInstance().getNodes(t));
        }
        resultsPanel.setEntries(all);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // AMI Dimmer
        g.fill(0, 0, width, height, 0x66000000);
        this.renderBackground(g);

        g.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        if (resultsPanel != null) {
            resultsPanel.render(g, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (resultsPanel != null) {
            return resultsPanel.mouseScrolled(mouseX, mouseY, scrollDelta);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (resultsPanel != null && resultsPanel.isMouseOver(mouseX, mouseY)) {
            if (resultsPanel.mouseClickedScrollbar(mouseX, mouseY, button)) return true;
            return resultsPanel.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (resultsPanel != null && resultsPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (resultsPanel != null) {
            resultsPanel.stopScrollbarDrag();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (resultsPanel != null && resultsPanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
