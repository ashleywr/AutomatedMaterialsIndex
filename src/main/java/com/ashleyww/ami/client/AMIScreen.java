package com.ashleyww.ami.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.ashleyww.ami.AMI;
import com.ashleyww.ami.index.AMIIndex;

public class AMIScreen extends Screen {
    private ItemGridWidget gridWidget;

    public AMIScreen() {
        super(Component.literal("Automated Materials Index"));
        AMI.LOGGER.debug("AMI screen created");
    }

    @Override
    protected void init() {
        AMI.LOGGER.debug("AMI screen initialized - size: {}x{}", this.width, this.height);
        this.gridWidget = new ItemGridWidget(this, 10, 40, this.width - 20, this.height - 80, AMIIndex.getInstance().getTotalItemsIndexed());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, 0, 0, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        String indexInfo = String.format("Indexed: %d items | Press I to close", AMIIndex.getInstance().getTotalItemsIndexed());
        guiGraphics.drawString(this.font, indexInfo, 10, 25, 0xAAAAAA);

        if (gridWidget != null) {
            guiGraphics.pose().pushPose();
            gridWidget.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.pose().popPose();
        }
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (gridWidget != null) {
            return gridWidget.mouseScrolled(mouseX, mouseY, scrollDeltaY);
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
