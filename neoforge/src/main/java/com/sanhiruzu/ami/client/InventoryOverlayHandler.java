package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.api.AmiApi;
import com.sanhiruzu.ami.client.RecipeViewerSuppressionPolicy.VisibleLayer;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.neoforge.AMI;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Method;
import java.util.List;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
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
    private static PendingGatheredTooltip pendingGatheredTooltip = null;
    private static PendingExternalTooltip pendingExternalTooltip = null;
    private static boolean renderingExternalTooltip = false;
    private static boolean renderingStatusEffectsAboveAmi = false;
    private static int statusEffectsOnTopFrames = 0;
    private static Method renderEffectsMethod = null;

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
        if (screen == null) {
            return false;
        }
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

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!isAmiScreen(event.getScreen())) return;
        renderOverlayFrame(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    private static void renderOverlayFrame(net.minecraft.client.gui.screens.Screen screen,
                                           net.minecraft.client.gui.GuiGraphics guiGraphics,
                                           int mouseX,
                                           int mouseY,
                                           float partialTick) {
        ensureIndexingStarted();
        syncVanillaRecipeBookVisibility(screen);

        if (pendingScreenReinit) {
            pendingScreenReinit = false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) {
                mc.screen.init(mc, mc.screen.width, mc.screen.height);
            }
            return;
        }

        if (AmiApi.shouldSuppressAmi(screen)) {
            return;
        }

        manager.refreshLayoutIfNeeded(screen);

        if (currentLayer == VisibleLayer.AMI) {
            manager.tick();
            manager.renderAll(guiGraphics, mouseX, mouseY, partialTick);
            if (statusEffectsOnTopFrames > 0 && renderStatusEffectsAboveAmi(screen, guiGraphics, mouseX, mouseY)) {
                statusEffectsOnTopFrames--;
                pendingExternalTooltip = null;
            } else {
                renderPendingExternalTooltip(guiGraphics);
            }
        }
    }

    @SubscribeEvent
    static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.mouseScrolled(event.getScreen(), manager, currentLayer == VisibleLayer.AMI,
                event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
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
    static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        var screen = Minecraft.getInstance().screen;
        if (screen == null || !isAmiScreen(screen)) return;
        if (com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer.isRenderingAmiTooltip()) return;
        if (renderingExternalTooltip) return;
        if (currentLayer != VisibleLayer.AMI || !manager.isPanelVisible()) return;

        pendingGatheredTooltip = new PendingGatheredTooltip(
                event.getItemStack().copy(),
                List.copyOf(event.getTooltipElements())
        );
        if (event.getItemStack().isEmpty() && !event.getTooltipElements().isEmpty()) {
            statusEffectsOnTopFrames = 2;
        }
    }

    @SubscribeEvent
    static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        var screen = Minecraft.getInstance().screen;
        if (screen == null || !isAmiScreen(screen)) return;
        if (com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer.isRenderingAmiTooltip()) return;
        if (renderingExternalTooltip) return;
        if (renderingStatusEffectsAboveAmi) return;
        if (statusEffectsOnTopFrames <= 0 && isMouseOverAmiOverlay(event.getX(), event.getY())) {
            event.setCanceled(true);
            return;
        }
        if (currentLayer == VisibleLayer.AMI && manager.isPanelVisible()) {
            PendingGatheredTooltip gathered = pendingGatheredTooltip;
            pendingGatheredTooltip = null;
            pendingExternalTooltip = new PendingExternalTooltip(
                    event.getFont(),
                    gathered != null ? gathered.stack() : event.getItemStack().copy(),
                    gathered != null ? gathered.elements() : List.of(),
                    event.getX(),
                    event.getY()
            );
            event.setCanceled(true);
        }
    }

    private static boolean renderStatusEffectsAboveAmi(net.minecraft.client.gui.screens.Screen screen,
                                                       net.minecraft.client.gui.GuiGraphics graphics,
                                                       int mouseX,
                                                       int mouseY) {
        if (!(screen instanceof EffectRenderingInventoryScreen<?> effectScreen)) return false;

        try {
            Method method = renderEffectsMethod;
            if (method == null) {
                method = EffectRenderingInventoryScreen.class.getDeclaredMethod(
                        "renderEffects", net.minecraft.client.gui.GuiGraphics.class, int.class, int.class);
                method.setAccessible(true);
                renderEffectsMethod = method;
            }

            var state = com.sanhiruzu.ami.client.RenderStateSnapshot.capture();
            try {
                renderingStatusEffectsAboveAmi = true;
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, com.sanhiruzu.ami.client.overlay.OverlayLayers.TRANSIENT_TOOLTIP);
                method.invoke(effectScreen, graphics, mouseX, mouseY);
                graphics.pose().popPose();
                graphics.flush();
            } finally {
                renderingStatusEffectsAboveAmi = false;
                state.restore();
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            renderEffectsMethod = null;
            return false;
        }
    }

    private static void renderPendingExternalTooltip(net.minecraft.client.gui.GuiGraphics graphics) {
        PendingExternalTooltip tooltip = pendingExternalTooltip;
        pendingExternalTooltip = null;
        if (tooltip == null || (tooltip.stack().isEmpty() && tooltip.elements().isEmpty())) return;

        var state = com.sanhiruzu.ami.client.RenderStateSnapshot.capture();
        try {
            renderingExternalTooltip = true;
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, com.sanhiruzu.ami.client.overlay.OverlayLayers.TRANSIENT_TOOLTIP);
            if (tooltip.elements().isEmpty()) {
                graphics.renderTooltip(tooltip.font(), tooltip.stack(), tooltip.x(), tooltip.y());
            } else {
                graphics.renderComponentTooltipFromElements(
                        tooltip.font(), tooltip.elements(), tooltip.x(), tooltip.y(), tooltip.stack());
            }
            graphics.pose().popPose();
            graphics.flush();
        } finally {
            renderingExternalTooltip = false;
            state.restore();
        }
    }

    private record PendingGatheredTooltip(
            net.minecraft.world.item.ItemStack stack,
            List<Either<FormattedText, TooltipComponent>> elements
    ) {
    }

    private record PendingExternalTooltip(
            net.minecraft.client.gui.Font font,
            net.minecraft.world.item.ItemStack stack,
            List<Either<FormattedText, TooltipComponent>> elements,
            int x,
            int y
    ) {
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
     * Saves and restores display state to preserve UI (expansion, selection, scroll, mode, filters).
     * Safe to call from any context; only refreshes if the overlay is visible.
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
