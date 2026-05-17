package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.config.AmiConfig;
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
        return AmiConfig.enableAutoIndexing;
    }

    /** Check if screen is a container screen. Matches EMI's check (HandledScreen equivalent). */
    private static boolean isContainerScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof AbstractContainerScreen<?>;
    }

    public static void toggleAmi() {
        Minecraft mc = Minecraft.getInstance();
        if (!isContainerScreen(mc.screen)) return;

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
        if (!isContainerScreen(event.getScreen())) return;
        var containerScreen = event.getScreen();

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
            if (manager.getLeftPanel() != null) event.addListener(manager.getLeftPanel());
            if (manager.getLeftPanelSecondary() != null) event.addListener(manager.getLeftPanelSecondary());
            if (manager.getRightPanelPrimary() != null) event.addListener(manager.getRightPanelPrimary());
            if (manager.getRightPanelSecondary() != null) event.addListener(manager.getRightPanelSecondary());
        }

        manager.getSearchBar().setFocused(false);
    }

    @SubscribeEvent
    static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!isContainerScreen(event.getScreen())) return;

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
        if (!AmiConfig.enableAutoIndexing) return;
        if (!isContainerScreen(event.getScreen())) return;
        
        if (manager.getResultsPanel().visible && manager.getResultsPanel().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true); return;
        }
        if (manager.getLeftPanel().visible && manager.getLeftPanel().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true); return;
        }
        if (manager.getLeftPanelSecondary().visible && manager.getLeftPanelSecondary().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true); return;
        }
        if (manager.getRightPanelPrimary().visible && manager.getRightPanelPrimary().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true); return;
        }
        if (manager.getRightPanelSecondary().visible && manager.getRightPanelSecondary().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isContainerScreen(event.getScreen())) return;
        var containerScreen = event.getScreen();

        if (event.getButton() == 0 && manager.getAmiButton().isMouseOver(event.getMouseX(), event.getMouseY())) {
            manager.getAmiButton().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
            return;
        }

        if (!amiEnabled || !manager.isPanelVisible()) return;

        if (manager.getResultsPanel().visible && manager.getResultsPanel().isMouseOver(event.getMouseX(), event.getMouseY())) {
            if (manager.getResultsPanel().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) { event.setCanceled(true); return; }
        }
        if (manager.getLeftPanel().visible && manager.getLeftPanel().isMouseOver(event.getMouseX(), event.getMouseY())) {
            if (manager.getLeftPanel().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) { event.setCanceled(true); return; }
        }
        if (manager.getLeftPanelSecondary().visible && manager.getLeftPanelSecondary().isMouseOver(event.getMouseX(), event.getMouseY())) {
            if (manager.getLeftPanelSecondary().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) { event.setCanceled(true); return; }
        }
        if (manager.getRightPanelPrimary().visible && manager.getRightPanelPrimary().isMouseOver(event.getMouseX(), event.getMouseY())) {
            if (manager.getRightPanelPrimary().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) { event.setCanceled(true); return; }
        }
        if (manager.getRightPanelSecondary().visible && manager.getRightPanelSecondary().isMouseOver(event.getMouseX(), event.getMouseY())) {
            if (manager.getRightPanelSecondary().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) { event.setCanceled(true); return; }
        }

        var searchBar = manager.getSearchBar();
        if (searchBar.isMouseOver(event.getMouseX(), event.getMouseY())) {
            searchBar.setFocused(true);
            containerScreen.setFocused(searchBar);
            searchBar.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
        } else if (searchBar.isFocused()) {
            searchBar.setFocused(false);
        }
    }

    @SubscribeEvent
    static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!amiEnabled || !manager.isPanelVisible()) return;
        if (!isContainerScreen(event.getScreen())) return;

        if (manager.getResultsPanel().visible && manager.getResultsPanel().mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true); return;
        }
        if (manager.getLeftPanel().visible && manager.getLeftPanel().mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true); return;
        }
        if (manager.getLeftPanelSecondary().visible && manager.getLeftPanelSecondary().mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true); return;
        }
        if (manager.getRightPanelPrimary().visible && manager.getRightPanelPrimary().mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true); return;
        }
        if (manager.getRightPanelSecondary().visible && manager.getRightPanelSecondary().mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true); return;
        }

        var searchBar = manager.getSearchBar();
        if (searchBar.isFocused() && searchBar.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!amiEnabled || !manager.isPanelVisible()) return;
        if (!isContainerScreen(event.getScreen())) return;

        manager.getResultsPanel().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
        manager.getLeftPanel().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
        manager.getLeftPanelSecondary().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
        manager.getRightPanelPrimary().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
        manager.getRightPanelSecondary().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());

        var searchBar = manager.getSearchBar();
        if (searchBar.isFocused() && searchBar.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!amiEnabled || !manager.isPanelVisible()) return;
        var searchBar = manager.getSearchBar();
        if (!searchBar.isFocused()) return;
        if (searchBar.charTyped(event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isAmiAvailable()) return;
        if (!isContainerScreen(event.getScreen())) return;
        var containerScreen = event.getScreen();

        if (AmiKeybindHandler.onKeyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers(), org.lwjgl.glfw.GLFW.GLFW_PRESS)) {
            event.setCanceled(true);
            return;
        }

        if (amiEnabled && manager.isPanelVisible()) {
            if (manager.getResultsPanel().visible && manager.getResultsPanel().keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                event.setCanceled(true); return;
            }
            if (manager.getLeftPanel().visible && manager.getLeftPanel().keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                event.setCanceled(true); return;
            }
            if (manager.getLeftPanelSecondary().visible && manager.getLeftPanelSecondary().keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                event.setCanceled(true); return;
            }
            if (manager.getRightPanelPrimary().visible && manager.getRightPanelPrimary().keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                event.setCanceled(true); return;
            }
            if (manager.getRightPanelSecondary().visible && manager.getRightPanelSecondary().keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
                event.setCanceled(true); return;
            }
        }

        var searchBar = manager.getSearchBar();
        if (!amiEnabled || !manager.isPanelVisible() || !searchBar.isFocused()) return;

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
