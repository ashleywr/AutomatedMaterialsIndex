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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
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
    private static boolean statusEffectsHoverOwned = false;
    private static boolean wasMouseOverStatusEffects = false;
    private static PendingGatheredTooltip pendingGatheredTooltip = null;
    private static PendingExternalTooltip pendingExternalTooltip = null;
    private static boolean renderingExternalTooltip = false;
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

    // External recipe viewers (JEI/EMI) own their own screen and draw their own tooltips; AMI's
    // own RecipeViewerScreen does not need tooltip re-hosting because AMI controls its render order.
    private static boolean isExternalRecipeScreen(net.minecraft.client.gui.screens.Screen screen) {
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
        return false;
    }

    // Force-closes the vanilla recipe book if it opens outside of OPEN_VANILLA_BOOK mode.
    // In OPEN_VANILLA_BOOK mode we let it coexist with AMI rather than suppressing either side.
    private static void syncVanillaRecipeBookVisibility(net.minecraft.client.gui.screens.Screen screen) {
        if (!isAmiScreen(screen)) return;
        if (AmiConfig.recipeBookAction == AmiConfig.RecipeBookAction.OPEN_VANILLA_BOOK) return;
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
        syncVanillaRecipeBookVisibility(event.getScreen());

        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            manager.computeLayouts(containerScreen, containerScreen.width, containerScreen.height);
            manager.getSearchBar().unfocus();
        }

        if (currentLayer == VisibleLayer.AMI && !manager.isPanelVisible() && !pendingScreenReinit) {
            manager.setPanelVisible(true);
        }
    }

    @SubscribeEvent
    static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!AmiConfig.enableAutoIndexing) return;
        Minecraft.getInstance().execute(InventoryOverlayHandler::ensureIndexingStarted);
    }

    // Render ownership split (container screens): AMI's durable body (panels, result icons,
    // search bar, buttons) renders in the container foreground, BEFORE vanilla/status tooltips,
    // so those tooltips win wherever they overlap AMI. AMI-owned transient UI (AMI tooltips,
    // dropdowns, context menus, hints) renders in Render.Post, AFTER vanilla tooltips, so it
    // wins over AMI's own body. AMI-owned recipe/custom screens render both layers together in
    // Post because AMI owns the whole screen.
    private static float lastContainerPartialTick = 0f;
    private static boolean frameStatusEffectsHovered = false;
    private static boolean frameBaseRendered = false;

    @SubscribeEvent
    static void onRenderPre(ScreenEvent.Render.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;
        // ContainerScreenEvent.Render.Foreground carries no partial tick; capture it here.
        if (isContainerScreen(event.getScreen())) {
            lastContainerPartialTick = event.getPartialTick();
        }
        frameBaseRendered = false;
        // Apply any scheduled reinit before vanilla renders this frame, so AMI never
        // reinitializes a screen from inside the container foreground (mid-render).
        if (pendingScreenReinit) {
            pendingScreenReinit = false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) {
                mc.screen.init(mc.screen.width, mc.screen.height);
            }
        }
    }

    @SubscribeEvent
    static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!isAmiScreen(screen) || !prepareOverlayFrame(screen) || currentLayer != VisibleLayer.AMI) {
            return;
        }

        net.minecraft.client.gui.GuiGraphicsExtractor g = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        manager.tick();
        frameStatusEffectsHovered = updateStatusEffectsHoverOwnership(screen, mouseX, mouseY);
        int amiMouseX = frameStatusEffectsHovered ? Integer.MIN_VALUE : mouseX;
        int amiMouseY = frameStatusEffectsHovered ? Integer.MIN_VALUE : mouseY;

        // Undo the container's leftPos/topPos translation so AMI renders in screen space,
        // matching the Render.Post coordinate space its layouts are computed in.
        g.pose().pushMatrix();
        int leftPos, topPos;
        try {
            leftPos = reflectedContainerInt(screen, "leftPos");
            topPos  = reflectedContainerInt(screen, "topPos");
        } catch (ReflectiveOperationException e) {
            leftPos = topPos = 0;
        }
        g.pose().translate(-leftPos, -topPos);
        manager.renderBase(g, amiMouseX, amiMouseY, lastContainerPartialTick);
        g.pose().popMatrix();
        frameBaseRendered = true;
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    static void onRenderPost(ScreenEvent.Render.Post event) {
        net.minecraft.client.gui.screens.Screen screen = event.getScreen();
        if (!isAmiScreen(screen)) return;

        net.minecraft.client.gui.GuiGraphicsExtractor graphics = event.getGuiGraphics();
        if (isContainerScreen(screen)) {
            // Durable body already drew in the foreground; only AMI-owned transient UI here.
            if (!frameBaseRendered || currentLayer != VisibleLayer.AMI) return;
            int amiMouseX = frameStatusEffectsHovered ? Integer.MIN_VALUE : event.getMouseX();
            int amiMouseY = frameStatusEffectsHovered ? Integer.MIN_VALUE : event.getMouseY();
            manager.renderTopLayer(graphics, amiMouseX, amiMouseY);
        } else if (isRecipeScreen(screen)) {
            // AMI owns recipe/custom screens: render base + top together. External recipe viewers
            // (JEI/EMI) draw their own tooltip during their render(), which AMI's base would cover,
            // so AMI re-hosts the captured external tooltip on top.
            renderOverlayFrame(screen, graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
            renderPendingExternalTooltip(graphics);
        } else {
            return;
        }

        // MC 26.1.2: Screen.extractRenderStateWithTooltipAndSubtitles() flushes deferred GUI
        // elements (tooltips) via GuiGraphics.extractDeferredElements() BEFORE ScreenEvent.Render.Post
        // fires (see ClientHooks.drawScreenInternal). AMI sets its tooltips here in Render.Post via
        // setTooltipForNextFrame, so they miss that flush and never draw. Flush again now so AMI-owned
        // tooltips are submitted this frame. Vanilla's deferred tooltip was already consumed (nulled)
        // by the first flush, so this only emits AMI's pending tooltip.
        graphics.extractDeferredElements(event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    @SubscribeEvent
    static void onRenderInventoryMobEffects(ScreenEvent.RenderInventoryMobEffects event) {
        if (currentLayer == VisibleLayer.AMI && manager.isPanelVisible() && !AmiApi.shouldSuppressAmi(event.getScreen())) {
            // Match JEI: compact potion indicators while the side overlay is visible.
            event.setCompact(true);
        }
    }

    // External recipe viewers render their tooltips inside their own screen render(), before AMI's
    // Render.Post overlay. With no container-foreground hook for a non-container Screen, AMI captures
    // the external tooltip as it is drawn, suppresses the external draw, and re-hosts it above AMI in
    // renderPendingExternalTooltip(). Scoped to external recipe screens only — container tooltips are
    // handled by the foreground/post split and are never replayed.
    @SubscribeEvent
    static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
        if (screen == null || !isExternalRecipeScreen(screen)) return;
        if (com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer.isRenderingAmiTooltip()) return;
        if (renderingExternalTooltip) return;
        if (currentLayer != VisibleLayer.AMI || !manager.isPanelVisible() || AmiApi.shouldSuppressAmi(screen)) return;
        pendingGatheredTooltip = new PendingGatheredTooltip(
                event.getItemStack().copy(),
                List.copyOf(event.getTooltipElements()));
    }

    @SubscribeEvent
    static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
        if (screen == null || !isExternalRecipeScreen(screen)) return;
        if (com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer.isRenderingAmiTooltip()) return;
        if (renderingExternalTooltip) return;
        if (currentLayer != VisibleLayer.AMI || !manager.isPanelVisible() || AmiApi.shouldSuppressAmi(screen)) return;

        PendingGatheredTooltip gathered = pendingGatheredTooltip;
        pendingGatheredTooltip = null;
        pendingExternalTooltip = new PendingExternalTooltip(
                event.getFont(),
                gathered != null ? gathered.stack() : event.getItemStack().copy(),
                gathered != null ? gathered.elements() : List.of(),
                event.getX(),
                event.getY());
        event.setCanceled(true);
    }

    private static void renderPendingExternalTooltip(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        PendingExternalTooltip tooltip = pendingExternalTooltip;
        pendingExternalTooltip = null;
        if (tooltip == null || (tooltip.stack().isEmpty() && tooltip.elements().isEmpty())) return;

        com.sanhiruzu.ami.client.RenderStateSnapshot state = com.sanhiruzu.ami.client.RenderStateSnapshot.capture();
        try {
            renderingExternalTooltip = true;
            graphics.pose().pushMatrix();
            if (tooltip.elements().isEmpty()) {
                graphics.setTooltipForNextFrame(tooltip.font(), tooltip.stack(), tooltip.x(), tooltip.y());
            } else {
                graphics.setComponentTooltipFromElementsForNextFrame(
                        tooltip.font(), tooltip.elements(), tooltip.x(), tooltip.y(), tooltip.stack());
            }
            graphics.pose().popMatrix();
        } finally {
            renderingExternalTooltip = false;
            state.restore();
        }
    }

    private record PendingGatheredTooltip(
            net.minecraft.world.item.ItemStack stack,
            List<Either<FormattedText, TooltipComponent>> elements) {
    }

    private record PendingExternalTooltip(
            net.minecraft.client.gui.Font font,
            net.minecraft.world.item.ItemStack stack,
            List<Either<FormattedText, TooltipComponent>> elements,
            int x,
            int y) {
    }

    private static void renderOverlayFrame(net.minecraft.client.gui.screens.Screen screen,
                                           net.minecraft.client.gui.GuiGraphicsExtractor GuiGraphicsExtractor,
                                           int mouseX,
                                           int mouseY,
                                           float partialTick) {
        if (!prepareOverlayFrame(screen) || currentLayer != VisibleLayer.AMI) return;
        manager.tick();
        boolean statusEffectsHovered = updateStatusEffectsHoverOwnership(screen, mouseX, mouseY);
        int amiMouseX = statusEffectsHovered ? Integer.MIN_VALUE : mouseX;
        int amiMouseY = statusEffectsHovered ? Integer.MIN_VALUE : mouseY;
        manager.renderBase(GuiGraphicsExtractor, amiMouseX, amiMouseY, partialTick);
        manager.renderTopLayer(GuiGraphicsExtractor, amiMouseX, amiMouseY);
    }

    private static boolean prepareOverlayFrame(net.minecraft.client.gui.screens.Screen screen) {
        ensureIndexingStarted();
        syncVanillaRecipeBookVisibility(screen);
        if (pendingScreenReinit) {
            // Reinit was scheduled after Render.Pre ran; defer AMI rendering until next frame.
            return false;
        }
        if (AmiApi.shouldSuppressAmi(screen)) {
            return false;
        }
        manager.refreshLayoutIfNeeded(screen);
        return true;
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
                event.getCharacterEvent())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isAmiScreen(event.getScreen())) return;

        if (OverlayInputController.keyPressed(event.getScreen(), manager, currentLayer == VisibleLayer.AMI,
                event.getKeyEvent())) {
            event.setCanceled(true);
        }
    }

    private static boolean updateStatusEffectsHoverOwnership(net.minecraft.client.gui.screens.Screen screen, int mouseX, int mouseY) {
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

    private static boolean isMouseOverStatusEffects(net.minecraft.client.gui.screens.Screen screen, int mouseX, int mouseY) {
        if (!(screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inventoryScreen)) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getActiveEffects().isEmpty()) return false;
        if (!inventoryScreen.showsActiveEffects()) return false;

        try {
            int leftPos = reflectedContainerInt(inventoryScreen, "leftPos");
            int topPos = reflectedContainerInt(inventoryScreen, "topPos");
            int imageWidth = reflectedContainerInt(inventoryScreen, "imageWidth");
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
