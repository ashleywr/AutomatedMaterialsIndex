package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
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
    private static boolean amiEnabled = false;
    private static boolean pendingScreenReinit = false;

    private static boolean isAmiAvailable() {
        return AMIConfig.ENABLE_AUTO_INDEXING.get();
    }

    public static void toggleAmi() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?>)) return;

        amiEnabled = !amiEnabled;

        if (amiEnabled && !manager.isPanelVisible()) {
            manager.setPanelVisible(true);
        } else if (!amiEnabled && manager.isPanelVisible()) {
            manager.setPanelVisible(false);
        }

        pendingScreenReinit = true;
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        // Compute widget positions from current screen geometry.
        manager.computeLayouts(containerScreen, containerScreen.width, containerScreen.height);

        // AMI button is always a screen child so it receives clicks even when the panel is closed.
        event.addListener(manager.getAmiButton());

        if (amiEnabled) {
            event.addListener(manager.getSearchBar());
            event.addListener(manager.getResultsPanel());
            // Clear any pre-existing focus (e.g. EMI auto-focused its search bar).
            event.getScreen().setFocused(null);
        }
    }

    @SubscribeEvent
    static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        // Process deferred screen reinit before any rendering this frame.
        if (pendingScreenReinit) {
            pendingScreenReinit = false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) mc.screen.init(mc, mc.screen.width, mc.screen.height);
            return;
        }

        if (amiEnabled) {
            manager.tick(event);
        }

        manager.renderAll(event);

        if (amiEnabled) {
            // Prevent EMI or any other widget from stealing keyboard focus from our search bar.
            GuiEventListener focused = event.getScreen().getFocused();
            if (focused != null && focused != manager.getSearchBar()) {
                event.getScreen().setFocused(null);
            }
        }
    }

    @SubscribeEvent
    static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!amiEnabled) return;
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        // ContainerEventHandler.mouseScrolled routes only to getFocused(), not the hovered widget.
        // We must handle the results panel scroll ourselves.
        if (manager.getResultsPanel().mouseScrolled(
                event.getMouseX(), event.getMouseY(),
                event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isAmiAvailable()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        InputConstants.Key pressed = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        if (AMIKeyMappings.TOGGLE_AMI.isActiveAndMatches(pressed)) {
            toggleAmi();
        }
    }

    public static OverlayWidgetManager getManager() {
        return manager;
    }

    public static boolean isAmiEnabled() {
        return amiEnabled;
    }
}
