package com.ashleyww.ami.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.ashleyww.ami.index.AMIIndex;

public class AMIScreen extends Screen {
    private static final int GRID_SIZE = 9;
    private int itemsPerRow = 9;
    private ItemGridWidget gridWidget;
    private int scrollOffset = 0;

    public AMIScreen() {
        super(Component.literal("Automated Materials Index"));
    }

    @Override
    protected void init() {
        this.gridWidget = new ItemGridWidget(this, this.width / 4, this.height / 4, this.width / 2, this.height / 2, AMIIndex.CLIENT_INSTANCE.getTotalItemsIndexed());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, 0, 0, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        String indexInfo = String.format("Indexed: %d items", AMIIndex.CLIENT_INSTANCE.getTotalItemsIndexed());
        guiGraphics.drawString(this.font, indexInfo, 20, 40, 0xAAAAAA);

        if (gridWidget != null) {
            gridWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == AMIKeyMappings.OPEN_AMI.getKey().getValue()) {
            this.onClose();
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
