package com.sanhiruzu.ami.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;

import java.util.ArrayList;
import java.util.List;

public class AMIScreen extends Screen {
    private UniversalResultsPanel resultsPanel;

    public AMIScreen() {
        super(Component.translatable("ami.gui.registry_tree"));
        AMI.LOGGER.debug("AMI screen created");
    }

    @Override
    protected void init() {
        AMI.LOGGER.debug("AMI screen initialized - size: {}x{}", this.width, this.height);
        this.resultsPanel = new UniversalResultsPanel(10, 40, this.width - 20, this.height - 80);

        var biomeEntries = GlobalIndex.getInstance().getNodes(NodeType.BIOME);
        resultsPanel.setAtlasEntries(biomeEntries, Component.literal("Biomes"), NodeType.BIOME);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, 0, 0, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        Component cycleHint = Component.translatable("ami.gui.cycle_hint", AMIKeyMappings.CYCLE_ATLAS.getTranslatedKeyMessage());
        Component closeHint = Component.translatable("ami.gui.close_hint", AMIKeyMappings.OPEN_AMI.getTranslatedKeyMessage());

        String info = String.format("%s | %s", cycleHint.getString(), closeHint.getString());
        guiGraphics.drawString(this.font, info, 10, 25, 0xAAAAAA);

        if (resultsPanel != null) {
            guiGraphics.pose().pushPose();
            resultsPanel.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AMIKeyMappings.OPEN_AMI.isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (resultsPanel != null) {
            return resultsPanel.mouseScrolled(mouseX, mouseY, scrollDeltaY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
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
