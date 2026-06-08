package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.api.AmiApi;
import com.sanhiruzu.ami.client.RecipeViewerSuppressionPolicy.VisibleLayer;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.index.AmiIndexerService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    public static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");
    private static final boolean EAGER_WORLD_JOIN_INDEXING =
            Boolean.getBoolean("ami.debug.indexOnWorldJoin");

    private static final OverlayWidgetManager manager = new OverlayWidgetManager();

    public enum RecipeBookIntercept { VANILLA, AMI_TOGGLE, PASS }

    // Single source of truth for what is visible. All transitions go through setLayer().
    private static VisibleLayer currentLayer = VisibleLayer.NONE;

    private static boolean pendingScreenReinit = false;
    private static net.minecraft.client.gui.screens.Screen initializedScreen = null;
    private static boolean recipeTransitionRestoreQueued = false;
    private static VisibleLayer recipeTransitionRestoreLayer = VisibleLayer.AMI;
    private static boolean sessionInitialized = false;
    private static boolean indexingRequested = false;

    /**
     * The single choke point for all visibility state changes. Updates the panel and schedules
     * a screen reinit so widget layouts (AMI's and external viewers') stay consistent with the new layer.
     */
    private static void setLayer(VisibleLayer layer) {
        if (currentLayer == layer) return;
        currentLayer = layer;
        com.sanhiruzu.ami.compat.RecipeViewerBridge.clearRecipeView();
        if (layer == VisibleLayer.AMI && !manager.isPanelVisible()) {
            manager.setPanelVisible(true);
        } else if (layer != VisibleLayer.AMI && manager.isPanelVisible()) {
            manager.setPanelVisible(false);
            AmiKeybindHandler.resetDebugTooltips();
        }
        pendingScreenReinit = true;
    }

    private static boolean isContainerScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof AbstractContainerScreen<?>;
    }

    private static boolean isRecipeScreen(net.minecraft.client.gui.screens.Screen screen) {
        if (screen instanceof com.sanhiruzu.ami.client.RecipeViewerScreen) return true;
        String name = screen.getClass().getName();
        return name.equals("dev.emi.emi.screen.RecipeScreen")
                || name.equals("mezz.jei.gui.recipes.RecipesGui");
    }

    private static boolean isAmiScreen(net.minecraft.client.gui.screens.Screen screen) {
        return isContainerScreen(screen) || isRecipeScreen(screen);
    }

    /**
     * Keybind toggle (Alt-V): cycle between AMI and external viewers. If no external viewer is
     * present, AMI off means NONE. Does not suppress external viewers when AMI is off.
     */
    public static void toggleAmi() {
        Minecraft mc = Minecraft.getInstance();
        if (!isAmiScreen(mc.screen)) return;
        VisibleLayer next = currentLayer == VisibleLayer.AMI
                ? (RECIPE_VIEWER_PRESENT ? VisibleLayer.EXTERNAL_RECIPE_VIEWER : VisibleLayer.NONE)
                : VisibleLayer.AMI;
        setLayer(next);
    }

    /**
     * Recipe-book TOGGLE_AMI mode: toggle between AMI and NONE. Suppresses external viewers
     * even when AMI is off — the recipe book acts as "hide everything".
     */
    public static void toggleAmiSuppressAll() {
        Minecraft mc = Minecraft.getInstance();
        if (!isAmiScreen(mc.screen)) return;
        setLayer(currentLayer == VisibleLayer.AMI ? VisibleLayer.NONE : VisibleLayer.AMI);
    }

    public static RecipeBookIntercept recipeBookIntercept() {
        if (AmiConfig.recipeBookAction == AmiConfig.RecipeBookAction.OPEN_VANILLA_BOOK) return RecipeBookIntercept.VANILLA;
        if (!AmiConfig.enableAutoIndexing) return RecipeBookIntercept.PASS;
        if (ModList.get().isLoaded("nerb")) return RecipeBookIntercept.PASS;
        net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof AbstractContainerScreen<?>)
                || !(screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener)) return RecipeBookIntercept.PASS;
        return RecipeBookIntercept.AMI_TOGGLE;
    }

    public static void handleRecipeBookToggle() {
        switch (AmiConfig.recipeBookAction) {
            case TOGGLE_AMI -> toggleAmiSuppressAll();
            case TOGGLE_EXTERNAL_VIEWER -> toggleAmi();
        }
    }

    private static boolean isVanillaRecipeBookVisible(net.minecraft.client.gui.screens.Screen screen) {
        if (screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener listener) {
            return listener.getRecipeBookComponent().isVisible();
        }
        return false;
    }

    // Force-closes the vanilla recipe book if it opens outside of OPEN_VANILLA_BOOK mode.
    // In OPEN_VANILLA_BOOK mode we let it coexist with AMI rather than suppressing either side.
    private static void syncVanillaRecipeBookVisibility(net.minecraft.client.gui.screens.Screen screen) {
        if (!isAmiScreen(screen)) return;
        if (AmiConfig.recipeBookAction == AmiConfig.RecipeBookAction.OPEN_VANILLA_BOOK) return;
        boolean visible = isVanillaRecipeBookVisible(screen);
        if (visible && screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener listener) {
            listener.getRecipeBookComponent().toggleVisibility();
        }
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!isAmiScreen(event.getScreen())) return;

        com.sanhiruzu.ami.compat.RecipeViewerBridge.clearRecipeView();

        net.minecraft.client.gui.screens.Screen previousScreen = initializedScreen;
        boolean shouldStartHidden = AmiConfig.startHidden;
        boolean leavingRecipeScreen = previousScreen != null && isRecipeScreen(previousScreen);
        boolean enteringRecipeScreen = isRecipeScreen(event.getScreen());
        boolean enteringContainerScreen = isContainerScreen(event.getScreen());
        boolean restoredFromRecipeTransition = recipeTransitionRestoreQueued
                && leavingRecipeScreen
                && enteringContainerScreen
                && !enteringRecipeScreen;

        if (restoredFromRecipeTransition) {
            setLayer(recipeTransitionRestoreLayer);
            recipeTransitionRestoreQueued = false;
        }

        if (!enteringRecipeScreen) {
            recipeTransitionRestoreQueued = false;
        } else if (!leavingRecipeScreen) {
            recipeTransitionRestoreLayer = currentLayer;
            recipeTransitionRestoreQueued = true;
        }

        boolean newScreenInstance = event.getScreen() != initializedScreen;
        if (newScreenInstance) {
            initializedScreen = event.getScreen();
            if (!restoredFromRecipeTransition) {
                if (shouldStartHidden) {
                    sessionInitialized = true;
                    // Direct assignment during init — setLayer() would schedule a redundant reinit.
                    currentLayer = VisibleLayer.NONE;
                    manager.setPanelVisible(false);
                } else if (!sessionInitialized) {
                    sessionInitialized = true;
                    currentLayer = VisibleLayer.AMI;
                    manager.setPanelVisible(true);
                }
            }
        }

        ensureIndexingStarted();

        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            manager.computeLayouts(containerScreen, containerScreen.width, containerScreen.height);
            manager.getSearchBar().unfocus();
            manager.invalidateLayout();
        }

        syncVanillaRecipeBookVisibility(event.getScreen());
        if (currentLayer == VisibleLayer.AMI && !manager.isPanelVisible() && !pendingScreenReinit) {
            manager.setPanelVisible(true);
        }
    }

    @SubscribeEvent
    static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!AmiConfig.enableAutoIndexing) return;
        Minecraft.getInstance().execute(InventoryOverlayHandler::ensureIndexingStarted);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!isAmiScreen(event.getScreen())) return;
        ensureIndexingStarted();
        syncVanillaRecipeBookVisibility(event.getScreen());

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

        if (currentLayer == VisibleLayer.AMI) {
            manager.tick(event);
            manager.renderAll(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.mouseScrolled(event.getScreen(), manager, currentLayer == VisibleLayer.AMI,
                event.getMouseX(), event.getMouseY(), 0, event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.mouseButtonPressed(event.getScreen(), manager, currentLayer == VisibleLayer.AMI,
                event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.mouseDragged(event.getScreen(), manager, currentLayer == VisibleLayer.AMI,
                event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.mouseButtonReleased(event.getScreen(), manager, currentLayer == VisibleLayer.AMI,
                event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.charTyped(event.getScreen(), manager, currentLayer == VisibleLayer.AMI,
                event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.keyPressed(event.getScreen(), manager, currentLayer == VisibleLayer.AMI,
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
        return currentLayer == VisibleLayer.AMI;
    }

    public static void setAmiEnabled(boolean enabled) {
        setLayer(enabled ? VisibleLayer.AMI
                : (RECIPE_VIEWER_PRESENT ? VisibleLayer.EXTERNAL_RECIPE_VIEWER : VisibleLayer.NONE));
    }

    /**
     * Refresh the overlay results display (search results, favorites, sidebars).
     * Called when data changes (e.g., waypoint deletion, runtime index updates).
     * Safe to call from any context; only refreshes if the overlay is visible.
     */
    public static void refreshOverlayResults() {
        if (currentLayer == VisibleLayer.AMI) {
            manager.refreshEntriesForRuntimeIndexUpdate();
        }
    }

    /**
     * Returns true when external recipe viewers (EMI, JEI) should not render their chrome
     * (search bar, item list, buttons). True whenever AMI is active OR the NONE layer is in
     * effect — only EXTERNAL_RECIPE_VIEWER lets external viewers show.
     */
    public static boolean shouldSuppressRecipeViewerChrome() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || !isAmiScreen(mc.screen)) return false;
        return currentLayer != VisibleLayer.EXTERNAL_RECIPE_VIEWER;
    }

    public static boolean isMouseOverAmiOverlay(double mouseX, double mouseY) {
        if (currentLayer != VisibleLayer.AMI || !manager.isPanelVisible()) return false;

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
        currentLayer = VisibleLayer.NONE;
        pendingScreenReinit = false;
        initializedScreen = null;
        recipeTransitionRestoreQueued = false;
        recipeTransitionRestoreLayer = VisibleLayer.AMI;
        sessionInitialized = false;
        indexingRequested = false;
    }

    public static void tickAutoIndexBootstrap() {
        if (!EAGER_WORLD_JOIN_INDEXING || !AmiConfig.enableAutoIndexing || indexingRequested) {
            return;
        }
        ensureIndexingStarted();
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
