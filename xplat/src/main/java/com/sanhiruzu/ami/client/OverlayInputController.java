package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.api.AmiApi;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * Shared AMI overlay input policy. Loader event handlers should only adapt event
 * fields and cancellation to these methods.
 */
public final class OverlayInputController {
    private OverlayInputController() {
    }

    // Tracks whether AMI consumed the press for each mouse button, so the matching release can
    // mirror that decision instead of guessing from the release position (a drag that starts on
    // a real vanilla slot can end up over the AMI panel before the button comes up).
    private static final boolean[] pressConsumedByButton = new boolean[8];

    private static void recordPressConsumed(int button, boolean consumed) {
        if (button >= 0 && button < pressConsumedByButton.length) {
            pressConsumedByButton[button] = consumed;
        }
    }

    private static boolean consumePressConsumed(int button) {
        if (button < 0 || button >= pressConsumedByButton.length) return false;
        boolean value = pressConsumedByButton[button];
        pressConsumedByButton[button] = false;
        return value;
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
        boolean consumed = computeMouseButtonPressed(screen, manager, amiEnabled, mouseX, mouseY, button);
        recordPressConsumed(button, consumed);
        return consumed;
    }

    private static boolean computeMouseButtonPressed(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                                      double mouseX, double mouseY, int button) {
        if (AmiApi.shouldSuppressAmi(screen)) return false;

        var searchBar = manager.getSearchBar();
        boolean inLayoutMode = manager.isInLayoutMode();

        if (!inLayoutMode && searchBar.isFocused() && searchBar.isSearchOverlayMouseOver(mouseX, mouseY)) {
            return searchBar.mouseClicked(mouseX, mouseY, button);
        }

        if (!inLayoutMode && searchBar.isFocused() && !searchBar.isMouseOver(mouseX, mouseY)) {
            // AMI search is live-filtered; click-away records history and only changes focus.
            searchBar.submitAndUnfocus();
        }

        if (button == 0 && manager.getAmiButton().isMouseOver(mouseX, mouseY)) {
            manager.getAmiButton().mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (!amiEnabled || !manager.isPanelVisible()) return false;

        if (!inLayoutMode && searchBar.isMouseOver(mouseX, mouseY)) {
            return searchBar.mouseClicked(mouseX, mouseY, button);
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

        var searchBar = manager.getSearchBar();
        return searchBar.isFocused() && searchBar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public static boolean mouseButtonReleased(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                              double mouseX, double mouseY, int button) {
        boolean pressWasConsumed = consumePressConsumed(button);

        if (!panelInputAllowed(screen, manager, amiEnabled)) return false;
        boolean inLayoutMode = manager.isInLayoutMode();

        manager.mouseReleased(mouseX, mouseY, button);

        if (inLayoutMode) {
            return false;
        }

        var searchBar = manager.getSearchBar();
        if (searchBar.isFocused() && searchBar.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }

        // Only consume the release if AMI actually owned the matching press for this button (or
        // a context menu is open right now, which only AMI can do) — checking release *position*
        // would swallow releases for drags that started on a real vanilla slot and happened to
        // end up over the AMI panel by the time the button comes up.
        return manager.hasOpenContextMenu() || pressWasConsumed;
    }

    public static boolean charTyped(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                    char codePoint, int modifiers) {
        if (!panelInputAllowed(screen, manager, amiEnabled)) return false;
        if (manager.isInLayoutMode()) return false;

        if (manager.hasOpenContextMenu()) {
            return manager.charTyped(codePoint, modifiers);
        }

        var searchBar = manager.getSearchBar();
        return searchBar.isFocused() && searchBar.charTyped(codePoint, modifiers);
    }

    public static boolean keyPressed(Screen screen, OverlayWidgetManager manager, boolean amiEnabled,
                                     int keyCode, int scanCode, int modifiers) {
        if (AmiApi.shouldSuppressAmi(screen)) return false;

        var searchBar = manager.getSearchBar();

        if (amiEnabled && manager.isPanelVisible() && manager.hasOpenContextMenu()) {
            return manager.keyPressed(keyCode, scanCode, modifiers);
        }

        if (amiEnabled && manager.isPanelVisible() && manager.isInLayoutMode()) {
            return manager.keyPressed(keyCode, scanCode, modifiers);
        }

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

        boolean amiPanelVisible = amiEnabled && manager.isPanelVisible();
        boolean toggleViewerKey = AmiKeybindHandler.isToggleViewerKey(keyCode, scanCode);
        if (OverlayInputPolicy.shouldDispatchGlobalAmiKeybinds(amiPanelVisible, toggleViewerKey)) {
            boolean allowRecipeSlotFallback = amiPanelVisible || RecipeViewerBridge.shouldUseNativeViewer();
            if (AmiKeybindHandler.onKeyPressed(keyCode, scanCode, modifiers, GLFW.GLFW_PRESS,
                    amiPanelVisible, allowRecipeSlotFallback)) {
                return true;
            }
        }

        return amiPanelVisible
                && manager.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean panelInputAllowed(Screen screen, OverlayWidgetManager manager, boolean amiEnabled) {
        return amiEnabled
                && manager.isPanelVisible()
                && !AmiApi.shouldSuppressAmi(screen);
    }
}
