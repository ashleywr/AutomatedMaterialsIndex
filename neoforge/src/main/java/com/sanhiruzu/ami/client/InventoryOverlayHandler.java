package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.neoforge.AMI;
import com.sanhiruzu.ami.api.AmiApi;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiIndexerService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    public static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");

    private static final OverlayWidgetManager manager = new OverlayWidgetManager();
    private static boolean amiEnabled = false;
    private static boolean pendingScreenReinit = false;
    private static net.minecraft.client.gui.screens.Screen initializedScreen = null;
    private static boolean sessionInitialized = false;
    private static boolean indexingRequested = false;

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

        boolean newScreenInstance = event.getScreen() != initializedScreen;
        if (newScreenInstance) {
            initializedScreen = event.getScreen();
            if (AmiConfig.startHidden) {
                sessionInitialized = true;
                amiEnabled = false;
                manager.setPanelVisible(amiEnabled);
            } else if (!sessionInitialized) {
                sessionInitialized = true;
                amiEnabled = true;
                manager.setPanelVisible(true);
            }
        }

        ensureIndexingStarted();

        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            manager.computeLayouts(containerScreen, containerScreen.width, containerScreen.height);
            manager.getSearchBar().unfocus();
            manager.invalidateLayout();
        }
    }

    @SubscribeEvent
    static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!AmiConfig.enableAutoIndexing) return;
        Minecraft.getInstance().execute(InventoryOverlayHandler::ensureIndexingStarted);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!isAmiScreen(event.getScreen())) return;
        ensureIndexingStarted();

        if (pendingScreenReinit) {
            pendingScreenReinit = false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) {
                mc.screen.init(mc, mc.screen.width, mc.screen.height);
            }
            return;
        }

        if (AmiApi.shouldSuppressAmi(event.getScreen())) {
            return;
        }

        manager.refreshLayoutIfNeeded(event.getScreen());
        manager.renderAll(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());

        if (amiEnabled) {
            manager.tick(event);
        }
    }

    @SubscribeEvent
    static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.mouseScrolled(event.getScreen(), manager, amiEnabled,
                event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.mouseButtonPressed(event.getScreen(), manager, amiEnabled,
                event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.mouseDragged(event.getScreen(), manager, amiEnabled,
                event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.mouseButtonReleased(event.getScreen(), manager, amiEnabled,
                event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.charTyped(event.getScreen(), manager, amiEnabled,
                event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.keyPressed(event.getScreen(), manager, amiEnabled,
                event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        var screen = Minecraft.getInstance().screen;
        if (screen == null || !isAmiScreen(screen)) return;
        if (com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer.isRenderingAmiTooltip()) return;
        if (isMouseOverAmiOverlay(event.getX(), event.getY())) {
            event.setCanceled(true);
        }
    }

    public static OverlayWidgetManager getManager() {
        return manager;
    }

    public static boolean isAmiEnabled() {
        return amiEnabled;
    }

    public static boolean shouldSuppressRecipeViewerChrome() {
        Minecraft mc = Minecraft.getInstance();
        return amiEnabled && mc.screen != null && isAmiScreen(mc.screen);
    }

    public static boolean isMouseOverAmiOverlay(double mouseX, double mouseY) {
        if (!amiEnabled || !manager.isPanelVisible()) return false;

        var searchBar = manager.getSearchBar();
        if (searchBar != null && searchBar.visible
                && (searchBar.isMouseOver(mouseX, mouseY)
                || searchBar.isSearchOverlayMouseOver(mouseX, mouseY))) {
            return true;
        }

        if (manager.isMouseOverPanel(mouseX, mouseY)) return true;

        return manager.getAmiButton() != null && manager.getAmiButton().isMouseOver(mouseX, mouseY);
    }

    public static void resetSessionState() {
        amiEnabled = false;
        pendingScreenReinit = false;
        initializedScreen = null;
        sessionInitialized = false;
        indexingRequested = false;
    }

    private static void ensureIndexingStarted() {
        if (!AmiConfig.enableAutoIndexing) return;

        var level = Minecraft.getInstance().level;
        if (level == null) return;

        if (AmiIndexerService.getInstance().ensureCurrentLanguageIndex(level)) {
            indexingRequested = true;
            return;
        }
        if (indexingRequested) return;

        indexingRequested = true;
        AMI.LOGGER.debug("AMI: starting background index rebuild");
        AmiIndexerService.getInstance().rebuild(level);
    }
}
