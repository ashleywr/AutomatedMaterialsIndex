package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class RecipeViewerScreen extends Screen {
    private final ItemStack item;
    private final Screen parentScreen;

    public RecipeViewerScreen(ItemStack item, Screen parentScreen) {
        super(Component.translatable("ami.recipe_viewer.title", item.getHoverName().getString()));
        this.item = item;
        this.parentScreen = parentScreen;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, 0, 0, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, AMITheme.CONFIG_TEXT_PRIMARY);

        guiGraphics.drawString(this.font, Component.translatable("ami.recipe_viewer.item_label", item.getHoverName().getString()), 20, 50, AMITheme.CONFIG_TEXT_PRIMARY);

        guiGraphics.renderItem(item, 20, 70);

        guiGraphics.drawString(this.font, Component.translatable("ami.recipe_viewer.placeholder"), 20, 100, AMITheme.CONFIG_TEXT_SECONDARY);
        guiGraphics.drawString(this.font, Component.translatable("ami.recipe_viewer.go_back"), 20, this.height - 30, AMITheme.CONFIG_TEXT_SECONDARY);

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
