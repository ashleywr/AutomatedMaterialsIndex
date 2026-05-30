package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.api.AmiApi;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.gui.screens.Screen;
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
        return manager.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public static boolean mouseButtonPressed(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                             double mouseX, double mouseY, int button) {
        if (AmiApi.shouldSuppressAmi(screen)) return false;

        var searchBar = manager.getSearchBar();

        if (searchBar.isFocused() && !searchBar.isMouseOver(mouseX, mouseY)) {
            // AMI search is live-filtered; click-away records history and only changes focus.
            searchBar.submitAndUnfocus();
        }

        if (button == 0 && manager.getAmiButton().isMouseOver(mouseX, mouseY)) {
            manager.getAmiButton().mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (!amiEnabled || !manager.isPanelVisible()) return false;

        if (manager.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (searchBar.isMouseOver(mouseX, mouseY)) {
            searchBar.focusForInput();
            searchBar.mouseClicked(mouseX, mouseY, button);
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

        var searchBar = manager.getSearchBar();
        return searchBar.isFocused() && searchBar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public static boolean mouseButtonReleased(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                              double mouseX, double mouseY, int button) {
        if (!panelInputAllowed(screen, manager, amiEnabled)) return false;

        manager.mouseReleased(mouseX, mouseY, button);

        var searchBar = manager.getSearchBar();
        return searchBar.isFocused() && searchBar.mouseReleased(mouseX, mouseY, button);
    }

    public static boolean charTyped(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                    char codePoint, int modifiers) {
        if (!panelInputAllowed(screen, manager, amiEnabled)) return false;

        var searchBar = manager.getSearchBar();
        return searchBar.isFocused() && searchBar.charTyped(codePoint, modifiers);
    }

    public static boolean keyPressed(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                     int keyCode, int scanCode, int modifiers) {
        if (!AmiConfig.enableAutoIndexing) return false;
        if (AmiApi.shouldSuppressAmi(screen)) return false;

        var searchBar = manager.getSearchBar();

        if (amiEnabled && manager.isPanelVisible() && searchBar.isFocused()) {
            if (searchBar.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && RecipeViewerBridge.isRecipeViewActive()) {
            RecipeViewerBridge.clearRecipeView();
            return false;
        }

        if (amiEnabled && manager.isPanelVisible() && !searchBar.isFocused()
                && Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_F) {
            searchBar.focusForInput();
            return true;
        }

        if (AmiKeybindHandler.onKeyPressed(keyCode, scanCode, modifiers, GLFW.GLFW_PRESS)) {
            return true;
        }

        return amiEnabled && manager.isPanelVisible()
                && manager.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean panelInputAllowed(Screen screen, OverlayWidgetManager manager, boolean amiEnabled) {
        return AmiConfig.enableAutoIndexing
                && amiEnabled
                && manager.isPanelVisible()
                && !AmiApi.shouldSuppressAmi(screen);
    }
}
