package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiApi;
import com.sanhiruzu.ami.client.RecipeViewerSuppressionPolicy.VisibleLayer;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

import java.lang.reflect.Field;

/**
 * Fabric port of InventoryOverlayHandler.
 *
 * Manages the AMI overlay visibility state machine (NONE / AMI / EXTERNAL_RECIPE_VIEWER)
 * and provides the public API surface consumed by xplat code and Fabric hooks.
 *
 * Rendering and input events are wired by AmiFabricClientHooks via Fabric API screen callbacks.
 */
public class InventoryOverlayHandler {

    public static final boolean RECIPE_VIEWER_PRESENT =
            Services.PLATFORM.isModLoaded("emi") || Services.PLATFORM.isModLoaded("jei");

    private static final boolean EAGER_WORLD_JOIN_INDEXING =
            Boolean.getBoolean("ami.debug.indexOnWorldJoin");

    private static final OverlayWidgetManager manager = new OverlayWidgetManager();

    public enum RecipeBookIntercept { VANILLA, AMI_TOGGLE, PASS }

    // Single source of truth for what is visible. All transitions go through setLayer().
    private static VisibleLayer currentLayer = VisibleLayer.NONE;

    private static boolean pendingScreenReinit = false;
    private static Screen initializedScreen = null;
    private static boolean recipeTransitionRestoreQueued = false;
    private static VisibleLayer recipeTransitionRestoreLayer = VisibleLayer.AMI;
    private static boolean sessionInitialized = false;
    private static boolean indexingRequested = false;
    private static boolean statusEffectsHoverOwned = false;
    private static boolean wasMouseOverStatusEffects = false;
    private static Field containerLeftPosField = null;
    private static Field containerTopPosField = null;
    private static Field containerImageWidthField = null;

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

    public static boolean isContainerScreen(Screen screen) {
        return screen instanceof AbstractContainerScreen<?>;
    }

    public static boolean isRecipeScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        if (screen instanceof com.sanhiruzu.ami.client.RecipeViewerScreen) return true;
        String name = screen.getClass().getName();
        return name.equals("dev.emi.emi.screen.RecipeScreen")
                || name.equals("mezz.jei.gui.recipes.RecipesGui");
    }

    public static boolean isAmiScreen(Screen screen) {
        return isContainerScreen(screen) || isRecipeScreen(screen);
    }

    // External recipe viewers (JEI/EMI) own their own screen and draw their own tooltips; AMI's
    // own RecipeViewerScreen does not need tooltip re-hosting because AMI controls its render order.
    private static boolean isExternalRecipeScreen(Screen screen) {
        return isRecipeScreen(screen) && !(screen instanceof com.sanhiruzu.ami.client.RecipeViewerScreen);
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
        Screen screen = Minecraft.getInstance().screen;
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

    private static boolean isVanillaRecipeBookVisible(Screen screen) {
        if (screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener listener) {
            return listener.getRecipeBookComponent().isVisible();
        }
        return false;
    }

    // Force-closes the vanilla recipe book if it opens outside of OPEN_VANILLA_BOOK mode.
    private static void syncVanillaRecipeBookVisibility(Screen screen) {
        if (!isAmiScreen(screen)) return;
        if (AmiConfig.recipeBookAction == AmiConfig.RecipeBookAction.OPEN_VANILLA_BOOK) return;
        boolean visible = isVanillaRecipeBookVisible(screen);
        if (visible && screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener listener) {
            listener.getRecipeBookComponent().toggleVisibility();
        }
    }

    /**
     * Called from AmiFabricClientHooks when a screen finishes initializing (AFTER_INIT).
     * Mirrors NeoForge's onScreenInit handler.
     */
    public static void onScreenInit(Screen screen) {
        if (!isAmiScreen(screen)) return;

        com.sanhiruzu.ami.compat.RecipeViewerBridge.clearRecipeView();

        Screen previousScreen = initializedScreen;
        boolean shouldStartHidden = AmiConfig.startHidden;
        boolean leavingRecipeScreen = previousScreen != null && isRecipeScreen(previousScreen);
        boolean enteringRecipeScreen = isRecipeScreen(screen);
        boolean enteringContainerScreen = isContainerScreen(screen);
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

        boolean newScreenInstance = screen != initializedScreen;
        if (newScreenInstance) {
            initializedScreen = screen;
            if (!restoredFromRecipeTransition) {
                if (shouldStartHidden) {
                    sessionInitialized = true;
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
        syncVanillaRecipeBookVisibility(screen);

        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            manager.computeLayouts(containerScreen, containerScreen.width, containerScreen.height);
            manager.getSearchBar().unfocus();
        }

        if (currentLayer == VisibleLayer.AMI && !manager.isPanelVisible() && !pendingScreenReinit) {
            manager.setPanelVisible(true);
        }
    }

    /**
     * Called on player login (world join). Triggers background indexing if configured.
     */
    public static void onPlayerLoggingIn() {
        if (!AmiConfig.enableAutoIndexing) return;
        Minecraft.getInstance().execute(InventoryOverlayHandler::ensureIndexingStarted);
    }

    // -------------------------------------------------------------------------
    // Render helpers — called by AmiFabricClientHooks
    // -------------------------------------------------------------------------

    /**
     * Called from the BEFORE_INIT hook to apply a pending screen reinit before the frame starts.
     * On Fabric we do this at the start of afterRender if pending.
     */
    public static boolean consumePendingScreenReinit() {
        if (!pendingScreenReinit) return false;
        pendingScreenReinit = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            mc.screen.init(mc, mc.screen.width, mc.screen.height);
        }
        return true;
    }

    /**
     * Prepare an overlay render frame. Returns false if the frame should be skipped.
     */
    public static boolean prepareOverlayFrame(Screen screen) {
        ensureIndexingStarted();
        syncVanillaRecipeBookVisibility(screen);
        if (pendingScreenReinit) {
            return false;
        }
        if (AmiApi.shouldSuppressAmi(screen)) {
            return false;
        }
        manager.refreshLayoutIfNeeded(screen);
        return true;
    }

    /**
     * Render both the base and top overlay layers in screen-space coordinates.
     * Used for all AMI screens on Fabric (container + recipe screens).
     */
    public static void renderOverlayFrame(Screen screen,
                                          net.minecraft.client.gui.GuiGraphics guiGraphics,
                                          int mouseX,
                                          int mouseY,
                                          float partialTick) {
        if (!prepareOverlayFrame(screen) || currentLayer != VisibleLayer.AMI) return;
        manager.tick();
        boolean statusEffectsHovered = updateStatusEffectsHoverOwnership(screen, mouseX, mouseY);
        int amiMouseX = statusEffectsHovered ? Integer.MIN_VALUE : mouseX;
        int amiMouseY = statusEffectsHovered ? Integer.MIN_VALUE : mouseY;
        manager.renderBase(guiGraphics, amiMouseX, amiMouseY, partialTick);
        manager.renderTopLayer(guiGraphics, amiMouseX, amiMouseY);
    }

    private static boolean updateStatusEffectsHoverOwnership(Screen screen, int mouseX, int mouseY) {
        boolean mouseOverStatusEffects = isMouseOverStatusEffects(screen, mouseX, mouseY);
        if (!mouseOverStatusEffects) {
            statusEffectsHoverOwned = false;
            wasMouseOverStatusEffects = false;
            return false;
        }

        if (!statusEffectsHoverOwned && !wasMouseOverStatusEffects && !isMouseOverAmiOverlay(mouseX, mouseY)) {
            statusEffectsHoverOwned = true;
        }
        wasMouseOverStatusEffects = true;
        return statusEffectsHoverOwned;
    }

    private static boolean isMouseOverStatusEffects(Screen screen, int mouseX, int mouseY) {
        if (!(screen instanceof EffectRenderingInventoryScreen<?> effectScreen)) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getActiveEffects().isEmpty()) return false;
        if (!effectScreen.canSeeEffects()) return false;

        try {
            int leftPos = reflectedContainerInt(effectScreen, "leftPos");
            int topPos = reflectedContainerInt(effectScreen, "topPos");
            int imageWidth = reflectedContainerInt(effectScreen, "imageWidth");
            int renderX = leftPos + imageWidth + 2;
            int availableWidth = screen.width - renderX;
            if (availableWidth < 32) return false;

            int effectCount = mc.player.getActiveEffects().size();
            int rowStep = effectCount > 5 ? 132 / Math.max(1, effectCount - 1) : 33;
            int stripWidth = availableWidth >= 120 ? 120 : 33;
            int stripHeight = 32 + Math.max(0, effectCount - 1) * rowStep;
            return mouseX >= renderX && mouseX <= renderX + stripWidth
                    && mouseY >= topPos && mouseY <= topPos + stripHeight;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static int reflectedContainerInt(AbstractContainerScreen<?> screen, String name)
            throws ReflectiveOperationException {
        Field field = switch (name) {
            case "leftPos" -> {
                if (containerLeftPosField == null) {
                    containerLeftPosField = containerField("leftPos");
                }
                yield containerLeftPosField;
            }
            case "topPos" -> {
                if (containerTopPosField == null) {
                    containerTopPosField = containerField("topPos");
                }
                yield containerTopPosField;
            }
            case "imageWidth" -> {
                if (containerImageWidthField == null) {
                    containerImageWidthField = containerField("imageWidth");
                }
                yield containerImageWidthField;
            }
            default -> throw new NoSuchFieldException(name);
        };
        return field.getInt(screen);
    }

    private static Field containerField(String name) throws NoSuchFieldException {
        Field field = AbstractContainerScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

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
     */
    public static void refreshOverlayResults() {
        if (currentLayer == VisibleLayer.AMI) {
            com.sanhiruzu.ami.client.overlay.DisplayStateManager.saveState(manager);
            try {
                manager.refreshEntriesForRuntimeIndexUpdate();
            } finally {
                com.sanhiruzu.ami.client.overlay.DisplayStateManager.restoreState(manager);
            }
        }
    }

    /**
     * Returns true when external recipe viewers (EMI, JEI) should not render their chrome.
     * True whenever AMI is active OR the NONE layer is in effect.
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
        AmiCore.LOGGER.debug("AMI: starting background index rebuild");
        AmiIndexerService.getInstance().rebuild(level);
    }
}
