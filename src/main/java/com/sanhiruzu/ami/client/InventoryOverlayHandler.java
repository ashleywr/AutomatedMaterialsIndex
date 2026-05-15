package com.sanhiruzu.ami.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    public static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");

    private static final OverlayWidgetManager manager = new OverlayWidgetManager();
    private static boolean handlersRegistered = false;

    @SubscribeEvent
    static void onScreenOpen(ScreenEvent.Init.Post event) {
        // Register handlers when a container screen opens
        if (event.getScreen() instanceof AbstractContainerScreen<?> && !handlersRegistered) {
            registerHandlers();
            handlersRegistered = true;
            AMI.LOGGER.debug("AMI overlay handlers registered");
        }
    }

    @SubscribeEvent
    static void onScreenOpenPre(ScreenEvent.Init.Pre event) {
        // Mark handlers for unregistration when switching away from a container screen
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>) && handlersRegistered) {
            handlersRegistered = false;
            AMI.LOGGER.debug("AMI overlay handlers deactivated");
        }
    }

    private static void registerHandlers() {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ScreenEvent.Render.Post.class, manager::onRenderPost);
        // Use HIGH priority for input events to ensure we process clicks before EMI
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ScreenEvent.MouseButtonPressed.Pre.class, manager::onMouseClick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ScreenEvent.MouseDragged.Pre.class, manager::onMouseDragged);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ScreenEvent.MouseButtonReleased.Pre.class, manager::onMouseRelease);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ScreenEvent.MouseScrolled.Pre.class, manager::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ScreenEvent.KeyPressed.Pre.class, manager::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, ScreenEvent.CharacterTyped.Pre.class, manager::onCharTyped);
    }

    public static OverlayWidgetManager getManager() {
        return manager;
    }
}
