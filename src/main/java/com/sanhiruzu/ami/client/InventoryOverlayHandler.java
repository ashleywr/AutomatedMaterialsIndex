package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    public static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");

    private static final OverlayWidgetManager manager = new OverlayWidgetManager();

    @SubscribeEvent
    static void onRenderPost(ScreenEvent.Render.Post event) {
        manager.onRenderPost(event);
    }

    @SubscribeEvent
    static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        manager.onMouseClick(event);
    }

    @SubscribeEvent
    static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        manager.onMouseDragged(event);
    }

    @SubscribeEvent
    static void onMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        manager.onMouseRelease(event);
    }

    @SubscribeEvent
    static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        manager.onMouseScroll(event);
    }

    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        manager.onKeyPressed(event);
    }

    @SubscribeEvent
    static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        manager.onCharTyped(event);
    }

    public static OverlayWidgetManager getManager() {
        return manager;
    }
}
