package com.ashleyww.ami.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.InputEvent;

import com.ashleyww.ami.AMI;
import com.ashleyww.ami.AMIConfig;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    private static ItemGridWidget gridWidget;
    private static AbstractContainerScreen<?> lastContainerScreen;

    @SubscribeEvent
    static void onContainerScreenRender(ContainerScreenEvent.Render.Foreground event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;

        var screen = event.getContainerScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

        lastContainerScreen = containerScreen;

        int panelWidth = 100;
        int panelHeight = containerScreen.getYSize();
        int panelX = containerScreen.getGuiLeft() + containerScreen.getXSize() + 5;
        int panelY = containerScreen.getGuiTop();

        if (gridWidget == null || gridWidget.getWidth() != panelWidth || gridWidget.getHeight() != panelHeight) {
            gridWidget = new ItemGridWidget(panelX, panelY, panelWidth, panelHeight);
        }

        gridWidget.setPosition(panelX, panelY);
        gridWidget.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), 0f);
    }

    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get() || gridWidget == null || lastContainerScreen == null) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        int panelX = lastContainerScreen.getGuiLeft() + lastContainerScreen.getXSize() + 5;
        int panelY = lastContainerScreen.getGuiTop();
        int panelWidth = 100;
        int panelHeight = lastContainerScreen.getYSize();

        if (mouseX >= panelX && mouseX < panelX + panelWidth && mouseY >= panelY && mouseY < panelY + panelHeight) {
            if (gridWidget.mouseScrolled(mouseX, mouseY, event.getScrollDeltaY())) {
                event.setCanceled(true);
            }
        }
    }
}
