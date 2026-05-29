package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class AMIScreen extends Screen {
    private UniversalResultsPanel resultsPanel;

    public AMIScreen() {
        super(Component.translatable("ami.gui.registry_tree"));
    }

    @Override
    protected void init() {
        this.resultsPanel = new UniversalResultsPanel(10, 40, this.width - 20, this.height - 80);
        this.resultsPanel.setSearchService(com.sanhiruzu.ami.index.SearchService.buildFrom(com.sanhiruzu.ami.index.GlobalIndex.getInstance()));

        var all = new ArrayList<SearchNode>();
        for (NodeType t : NodeType.atlasValues()) {
            all.addAll(GlobalIndex.getInstance().getNodes(t));
        }
        resultsPanel.setEntries(all);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, 0, 0, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        if (resultsPanel != null) {
            guiGraphics.pose().pushPose();
            resultsPanel.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (resultsPanel != null) {
            return resultsPanel.mouseScrolled(mouseX, mouseY, scrollDeltaY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
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

    public UniversalResultsPanel getResultsPanel() {
        return resultsPanel;
    }
}
