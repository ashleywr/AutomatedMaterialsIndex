package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.api.AmiApi;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

/**
 * Shared AMI overlay input policy. Loader event handlers should only adapt event
 * fields and cancellation to these methods.
 */
public final class OverlayInputController {
    private OverlayInputController() {
    }

    public static boolean mouseScrolled(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                        double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!panelInputAllowed(screen, manager, amiEnabled)) return false;
        if (manager.isInLayoutMode()) {
            return false;
        }
        var searchBar = manager.getSearchBar();
        if (searchBar.isFocused() && searchBar.isSearchOverlayMouseOver(mouseX, mouseY)) {
            searchBar.handleSuggestionScroll(scrollY);
            return true;
        }
        return manager.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public static boolean mouseButtonPressed(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                             double mouseX, double mouseY, int button) {
        if (AmiApi.shouldSuppressAmi(screen)) return false;

        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
        var searchBar = manager.getSearchBar();
        boolean inLayoutMode = manager.isInLayoutMode();

        if (!inLayoutMode && searchBar.isFocused() && searchBar.isSearchOverlayMouseOver(mouseX, mouseY)) {
            return searchBar.mouseClicked(event, false);
        }

        if (!inLayoutMode && searchBar.isFocused() && !searchBar.isMouseOver(mouseX, mouseY)) {
            // AMI search is live-filtered; click-away records history and only changes focus.
            searchBar.submitAndUnfocus();
        }

        if (button == 0 && manager.getAmiButton().isMouseOver(mouseX, mouseY)) {
            manager.getAmiButton().mouseClicked(event, false);
            return true;
        }

        if (!amiEnabled || !manager.isPanelVisible()) return false;

        if (!inLayoutMode && searchBar.isMouseOver(mouseX, mouseY)) {
            return searchBar.mouseClicked(event, false);
        }

        if (manager.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return false;
    }

    public static boolean mouseDragged(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                       double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!panelInputAllowed(screen, manager, amiEnabled)) return false;

        if (manager.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }

        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
        var searchBar = manager.getSearchBar();
        return searchBar.isFocused() && searchBar.mouseDragged(event, dragX, dragY);
    }

    public static boolean mouseButtonReleased(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                              double mouseX, double mouseY, int button) {
        if (!panelInputAllowed(screen, manager, amiEnabled)) return false;
        boolean inLayoutMode = manager.isInLayoutMode();

        manager.mouseReleased(mouseX, mouseY, button);

        if (inLayoutMode) {
            return false;
        }

        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
        var searchBar = manager.getSearchBar();
        return searchBar.isFocused() && searchBar.mouseReleased(event);
    }

    public static boolean charTyped(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                    CharacterEvent event) {
        if (!panelInputAllowed(screen, manager, amiEnabled)) return false;
        if (manager.isInLayoutMode()) return false;

        if (manager.hasOpenContextMenu()) {
            return manager.charTyped(event);
        }

        var searchBar = manager.getSearchBar();
        return searchBar.isFocused() && searchBar.charTyped(event);
    }

    public static boolean keyPressed(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                     KeyEvent event) {
        if (AmiApi.shouldSuppressAmi(screen)) return false;

        int keyCode = event.key();
        var searchBar = manager.getSearchBar();

        if (amiEnabled && manager.isPanelVisible() && manager.hasOpenContextMenu()) {
            return manager.keyPressed(event);
        }

        if (amiEnabled && manager.isPanelVisible() && manager.isInLayoutMode()) {
            return manager.keyPressed(event);
        }

        if (amiEnabled && manager.isPanelVisible() && searchBar.isFocused()) {
            if (searchBar.keyPressed(event)) {
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && RecipeViewerBridge.isRecipeViewActive()) {
            RecipeViewerBridge.clearRecipeView();
            return false;
        }

        if (amiEnabled && manager.isPanelVisible() && !searchBar.isFocused()
                && Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_F) {
            searchBar.focusForInput();
            return true;
        }

        boolean amiPanelVisible = amiEnabled && manager.isPanelVisible();
        boolean allowRecipeSlotFallback = amiPanelVisible || RecipeViewerBridge.shouldUseNativeViewer();
        if (AmiKeybindHandler.onKeyPressed(keyCode, event.scancode(), event.modifiers(), GLFW.GLFW_PRESS,
                amiPanelVisible, allowRecipeSlotFallback)) {
            return true;
        }

        return amiPanelVisible && manager.keyPressed(event);
    }

    private static boolean panelInputAllowed(Screen screen, OverlayWidgetManager manager, boolean amiEnabled) {
        return amiEnabled
                && manager.isPanelVisible()
                && !AmiApi.shouldSuppressAmi(screen);
    }
}
