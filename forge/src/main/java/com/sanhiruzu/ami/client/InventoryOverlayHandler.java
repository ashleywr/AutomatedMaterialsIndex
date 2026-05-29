package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.api.AmiApi;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiIndexerService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;

@Mod.EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    public static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");

    private static final OverlayWidgetManager manager = new OverlayWidgetManager();
    private static boolean amiEnabled = false;
    private static boolean pendingScreenReinit = false;
    private static boolean sessionInitialized = false;
    private static boolean indexingRequested = false;

    private static boolean isAmiAvailable() {
        return AmiConfig.enableAutoIndexing;
    }

    /**
     * Check if screen is a container screen. Matches EMI's check (HandledScreen equivalent).
     */
    private static boolean isContainerScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof AbstractContainerScreen<?>;
    }

    private static boolean isRecipeScreen(net.minecraft.client.gui.screens.Screen screen) {
        if (screen instanceof com.sanhiruzu.ami.client.RecipeViewerScreen) return true;
        String name = screen.getClass().getName();
        return name.equals("dev.emi.emi.screen.RecipeScreen")
                || name.equals("mezz.jei.gui.recipes.RecipesGui");
    }

    /**
     * Screens where AMI renders and handles input.
     */
    private static boolean isAmiScreen(net.minecraft.client.gui.screens.Screen screen) {
        return isContainerScreen(screen) || isRecipeScreen(screen);
    }

    public static void toggleAmi() {
        Minecraft mc = Minecraft.getInstance();
        if (!isAmiScreen(mc.screen)) return;

        amiEnabled = !amiEnabled;

        // Toggling AMI dismisses any active recipe view
        com.sanhiruzu.ami.compat.RecipeViewerBridge.clearRecipeView();

        if (amiEnabled && !manager.isPanelVisible()) {
            manager.setPanelVisible(true);
        } else if (!amiEnabled && manager.isPanelVisible()) {
            manager.setPanelVisible(false);
            AmiKeybindHandler.resetDebugTooltips();
        }

        pendingScreenReinit = true;
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!isAmiScreen(event.getScreen())) return;

        // Screen reinit dismisses any active recipe view
        com.sanhiruzu.ami.compat.RecipeViewerBridge.clearRecipeView();

        if (!sessionInitialized) {
            sessionInitialized = true;
            amiEnabled = true;
            manager.setPanelVisible(true);
        }

        ensureIndexingStarted();

        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            manager.computeLayouts(containerScreen, containerScreen.width, containerScreen.height);
            event.addListener(manager.getAmiButton());
            if (amiEnabled) {
                event.addListener(manager.getSearchBar());
            }
            manager.getSearchBar().setFocused(false);
        }
        // For RecipeScreen, layout is computed by renderAll each frame.
        // Don't add children — RecipeScreen manages its own widget lifecycle.
    }

    @SubscribeEvent
    static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!AmiConfig.enableAutoIndexing) return;
        Minecraft.getInstance().execute(InventoryOverlayHandler::ensureIndexingStarted);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!isAmiScreen(event.getScreen())) return;

        // Process deferred screen reinit before any rendering this frame.
        if (pendingScreenReinit) {
            pendingScreenReinit = false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) mc.screen.init(mc, mc.screen.width, mc.screen.height);
            return;
        }

        // Check if any registered suppressors want to hide AMI
        if (AmiApi.shouldSuppressAmi(event.getScreen())) {
            return;
        }

        // renderAll must be called before tick to ensure widgets are initialized
        manager.renderAll(event);

        if (amiEnabled) {
            manager.tick(event);
        }
    }

    @SubscribeEvent
    static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;
        if (AmiApi.shouldSuppressAmi(event.getScreen())) return;
        if (!amiEnabled) return;
        if (!AmiConfig.enableAutoIndexing) return;

        if (manager.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        // Check if any registered suppressors want to hide AMI
        if (AmiApi.shouldSuppressAmi(event.getScreen())) {
            return;
        }

        var screen = event.getScreen();
        var searchBar = manager.getSearchBar();

        // Unfocus search bar if clicking anywhere else
        if (searchBar.isFocused() && !searchBar.isMouseOver(event.getMouseX(), event.getMouseY())) {
            searchBar.setFocused(false);
        }

        if (event.getButton() == 0 && manager.getAmiButton().isMouseOver(event.getMouseX(), event.getMouseY())) {
            manager.getAmiButton().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
            return;
        }

        if (!amiEnabled || !manager.isPanelVisible()) return;

        if (manager.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }

        if (searchBar.isMouseOver(event.getMouseX(), event.getMouseY())) {
            searchBar.setFocused(true);
            screen.setFocused(searchBar);
            searchBar.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!amiEnabled || !manager.isPanelVisible()) return;
        if (!isAmiScreen(event.getScreen())) return;

        if (manager.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
            return;
        }

        var searchBar = manager.getSearchBar();
        if (searchBar.isFocused() && searchBar.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!amiEnabled || !manager.isPanelVisible()) return;
        if (!isAmiScreen(event.getScreen())) return;

        manager.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());

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
        if (!isAmiScreen(event.getScreen())) return;

        int key = event.getKeyCode();
        var searchBar = manager.getSearchBar();

        // If the search bar is focused, let it handle the keystroke first.
        // SearchBarWidget consumes ESC (to unfocus) and Backspace (as text deletion),
        // so those keys never reach the handlers below while the bar is active.
        if (amiEnabled && manager.isPanelVisible() && searchBar.isFocused()) {
            if (searchBar.keyPressed(key, event.getScanCode(), event.getModifiers())) {
                event.setCanceled(true);
                return;
            }
        }

        // For EMI/JEI recipe views, ESC should clear the recipe-view focus so the
        // external viewer can dismiss itself.  Do not cancel — let the event
        // propagate so EMI/JEI also sees the ESC.
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
                && com.sanhiruzu.ami.compat.RecipeViewerBridge.isRecipeViewActive()) {
            com.sanhiruzu.ami.compat.RecipeViewerBridge.clearRecipeView();
            return;
        }

        if (AmiKeybindHandler.onKeyPressed(key, event.getScanCode(), event.getModifiers(), org.lwjgl.glfw.GLFW.GLFW_PRESS)) {
            event.setCanceled(true);
            return;
        }

        if (amiEnabled && manager.isPanelVisible()) {
            if (manager.keyPressed(key, event.getScanCode(), event.getModifiers())) {
                event.setCanceled(true);
            }
        }
    }

    public static OverlayWidgetManager getManager() {
        return manager;
    }

    public static boolean isAmiEnabled() {
        return amiEnabled;
    }

    public static boolean shouldSuppressRecipeViewerChrome() {
        if (amiEnabled) return true;
        if (sessionInitialized) return false;
        Minecraft mc = Minecraft.getInstance();
        return isAmiAvailable() && mc.screen != null && isAmiScreen(mc.screen);
    }

    public static boolean isMouseOverAmiOverlay(double mouseX, double mouseY) {
        if (!amiEnabled || !manager.isPanelVisible()) return false;

        var searchBar = manager.getSearchBar();
        if (searchBar != null && searchBar.visible && searchBar.isMouseOver(mouseX, mouseY)) {
            return true;
        }

        if (manager.isMouseOverPanel(mouseX, mouseY)) return true;

        return manager.getAmiButton() != null && manager.getAmiButton().isMouseOver(mouseX, mouseY);
    }

    public static void resetSessionState() {
        amiEnabled = false;
        pendingScreenReinit = false;
        sessionInitialized = false;
        indexingRequested = false;
    }

    private static void ensureIndexingStarted() {
        if (!AmiConfig.enableAutoIndexing) return;
        if (indexingRequested) return;

        var level = Minecraft.getInstance().level;
        if (level == null) return;

        indexingRequested = true;
        AMI.LOGGER.info("AMI: starting background index rebuild");
        AmiIndexerService.getInstance().rebuild(level);
    }
}
