package com.sanhiruzu.ami.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class RecipeViewerScreen extends Screen {
    private final ItemStack item;
    private final Screen parentScreen;

    public RecipeViewerScreen(ItemStack item, Screen parentScreen) {
        super(Component.literal("Recipe: " + item.getHoverName().getString()));
        this.item = item;
        this.parentScreen = parentScreen;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, 0, 0, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        guiGraphics.drawString(this.font, "Item: " + item.getHoverName().getString(), 20, 50, 0xFFFFFF);
        guiGraphics.renderItem(item, 20, 70);

        guiGraphics.drawString(this.font, "[Recipes for this item would appear here]", 20, 100, 0xAAAAAA);
        guiGraphics.drawString(this.font, "Press ESC to go back", 20, this.height - 30, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
