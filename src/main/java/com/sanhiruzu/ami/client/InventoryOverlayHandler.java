package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import net.minecraft.client.Minecraft;
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
    private static boolean sessionInitialized = false;

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

        if (!sessionInitialized) {
            sessionInitialized = true;
            amiEnabled = true;
            manager.setPanelVisible(true);
        }

        // Compute widget positions from current screen geometry.
        manager.computeLayouts(containerScreen, containerScreen.width, containerScreen.height);

        // AMI button is always a screen child so clicks work even when the panel is closed.
        event.addListener(manager.getAmiButton());

        if (amiEnabled) {
            event.addListener(manager.getSearchBar());
            event.addListener(manager.getResultsPanel());
        }

        manager.getSearchBar().setFocused(false);
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
    static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        // Handle the AMI button directly — some container screens override mouseClicked without
        // calling super, so we can't rely on the screen routing clicks to our registered child.
        if (event.getButton() == 0 && manager.getAmiButton().isMouseOver(event.getMouseX(), event.getMouseY())) {
            toggleAmi();
            event.setCanceled(true);
            return;
        }

        if (!amiEnabled || !manager.isPanelVisible()) return;

        var searchBar = manager.getSearchBar();
        var resultsPanel = manager.getResultsPanel();
        if (resultsPanel.isMouseOver(event.getMouseX(), event.getMouseY())) {
            resultsPanel.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
            return;
        }

        if (searchBar.isMouseOver(event.getMouseX(), event.getMouseY())) {
            searchBar.setFocused(true);
            containerScreen.setFocused(searchBar);
            // Handle the click ourselves and cancel so the container screen cannot reset focus.
            searchBar.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
        } else if (searchBar.isFocused()) {
            searchBar.setFocused(false);
        }
    }

    @SubscribeEvent
    static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!amiEnabled || !manager.isPanelVisible()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        var resultsPanel = manager.getResultsPanel();
        if (resultsPanel.mouseDragged(
                event.getMouseX(), event.getMouseY(),
                event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
            return;
        }

        var searchBar = manager.getSearchBar();
        if (!searchBar.isFocused()) return;

        if (searchBar.mouseDragged(
                event.getMouseX(), event.getMouseY(),
                event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!amiEnabled || !manager.isPanelVisible()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        manager.getResultsPanel().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());

        var searchBar = manager.getSearchBar();
        if (!searchBar.isFocused()) return;

        if (searchBar.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!amiEnabled || !manager.isPanelVisible()) return;
        var searchBar = manager.getSearchBar();
        if (!searchBar.isFocused()) return;
        // Route the char ourselves and cancel so container screens that override charTyped
        // directly (e.g. CreativeModeInventoryScreen → its search box) don't also receive it.
        if (searchBar.charTyped(event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isAmiAvailable()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        var resultsPanel = manager.getResultsPanel();
        if (amiEnabled && manager.isPanelVisible()
                && resultsPanel.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
            return;
        }

        var searchBar = manager.getSearchBar();
        if (!amiEnabled || !manager.isPanelVisible() || !searchBar.isFocused()) return;

        // Route the key directly and cancel if consumed — same pattern as onCharTyped.
        // searchBar.keyPressed returns false for Escape and Tab so those propagate normally.
        if (searchBar.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    public static OverlayWidgetManager getManager() {
        return manager;
    }

    public static boolean isAmiEnabled() {
        return amiEnabled;
    }
}
