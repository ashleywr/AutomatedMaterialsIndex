package com.sanhiruzu.ami.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    public static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");

    private static final OverlayWidgetManager manager = new OverlayWidgetManager();

    @SubscribeEvent
    static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        manager.onRenderPost(event);
    }

    @SubscribeEvent
    static void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        manager.onMouseClick(event);
    }

    @SubscribeEvent
    static void onScreenMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        manager.onMouseDragged(event);
    }

    @SubscribeEvent
    static void onScreenMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        manager.onMouseRelease(event);
    }

    @SubscribeEvent
    static void onScreenMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        manager.onMouseScroll(event);
    }

    @SubscribeEvent
    static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        manager.onKeyPressed(event);
    }

    @SubscribeEvent
    static void onScreenCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        manager.onCharTyped(event);
    }

    public static OverlayWidgetManager getManager() {
        return manager;
    }
}
