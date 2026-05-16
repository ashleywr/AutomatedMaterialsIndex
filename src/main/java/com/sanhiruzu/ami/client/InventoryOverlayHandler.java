package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.InputConstants;
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
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    public static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");

    private static final OverlayWidgetManager manager = new OverlayWidgetManager();
    private static boolean amiEnabled = false;
    private static boolean pendingScreenReinit = false;
    /** True when the user has clicked the search bar and it should receive keyboard input. */
    private static boolean searchBarInputActive = false;

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
            event.addListener(manager.getResultsPanel());
        }

        // Reset our own focus tracking so the user must explicitly re-click the search bar.
        searchBarInputActive = false;
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

        // Re-assert the widget's own focused flag each frame so the cursor/border render correctly.
        // We route input directly via event handlers, so we don't need to fight for screen focus.
        if (amiEnabled && manager.isPanelVisible() && searchBarInputActive) {
            var searchBar = manager.getSearchBar();
            if (!searchBar.isFocused()) searchBar.setFocused(true);
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
    static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!amiEnabled || !manager.isPanelVisible()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        var searchBar = manager.getSearchBar();
        if (searchBar.isMouseOver(event.getMouseX(), event.getMouseY())) {
            searchBarInputActive = true;
            searchBar.setFocused(true);
            // Handle the click ourselves and cancel so the container screen cannot reset focus.
            searchBar.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
        } else if (searchBarInputActive) {
            searchBarInputActive = false;
            searchBar.setFocused(false);
        }
    }

    @SubscribeEvent
    static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!amiEnabled || !searchBarInputActive || !manager.isPanelVisible()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        var searchBar = manager.getSearchBar();
        // Re-assert focus in case another mod cleared it since our last mouse click.
        if (!searchBar.isFocused()) searchBar.setFocused(true);
        if (searchBar.charTyped(event.getCodePoint(), event.getModifiers())) {
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
            event.setCanceled(true);
            return;
        }

        if (!amiEnabled || !searchBarInputActive || !manager.isPanelVisible()) return;

        var searchBar = manager.getSearchBar();
        if (!searchBar.isFocused()) searchBar.setFocused(true);
        boolean handled = searchBar.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        // Cancel the event for every key except Escape while the search bar is active.
        // Without this, letter keys that match mod-configured keybinds (e.g. "I") fire
        // their actions even though the user is just typing into the search field.
        // Escape is the only exception — we let it propagate so the screen can close.
        if (handled || event.getKeyCode() != GLFW.GLFW_KEY_ESCAPE) {
            event.setCanceled(true);
        }
        // If the search bar released its own focus (e.g. on Escape), mirror that here.
        if (!searchBar.isFocused()) {
            searchBarInputActive = false;
        }
    }

    public static OverlayWidgetManager getManager() {
        return manager;
    }

    public static boolean isAmiEnabled() {
        return amiEnabled;
    }
}
