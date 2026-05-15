package com.sanhiruzu.ami.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.ProviderRegistry;
import com.sanhiruzu.ami.index.SearchService;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    // Checked once at class load — mod list is fixed after startup
    // Package-accessible so CommandPaletteWidget can check it
    static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");

    private static UniversalResultsPanel resultsPanel;

    // Always start in atlas mode - focus on World Atlas, not Items
    private static NodeType atlasType = NodeType.BIOME;
    private static SearchService searchService = null;

    private static volatile boolean indexingInProgress = false;
    private static boolean indexingDispatched = false;
    private static int retryCount = 0;
    private static final int MAX_RETRIES = 5;

    @SubscribeEvent
    static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        try {
            // Async lazy-load indexing on first inventory open
            if (!indexingDispatched && !indexingInProgress) {
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    indexingInProgress = true;
                    if (resultsPanel != null) resultsPanel.setIndexingInProgress(true);

                    GlobalIndexCache.loadOrIndexAsync(level, () -> {
                        // Runs on render thread after background indexing completes
                        searchService = SearchService.buildFrom(GlobalIndex.getInstance());
                        ProviderRegistry.indexStructuresDeferred(level);
                        searchService = SearchService.buildFrom(GlobalIndex.getInstance());
                        indexingInProgress = false;
                        indexingDispatched = true;
                        if (resultsPanel != null) resultsPanel.setIndexingInProgress(false);
                        refreshEntries();
                    });
                }
            } else if (indexingDispatched && retryCount < MAX_RETRIES) {
                // Retry if structures/dimensions are still empty
                var index = GlobalIndex.getInstance();
                int structures = index.getNodes(NodeType.STRUCTURE).size();
                int dimensions = index.getNodes(NodeType.DIMENSION).size();

                if (structures == 0 || dimensions == 0) {
                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        ProviderRegistry.indexStructuresDeferred(level);
                        searchService = SearchService.buildFrom(GlobalIndex.getInstance());
                    }
                    retryCount++;
                }
            }

            int panelY = 0;
            int panelHeight = event.getScreen().height;

            boolean goLeft = switch (AMIConfig.PANEL_SIDE.get()) {
                case LEFT  -> true;
                case RIGHT -> false;
                case AUTO  -> RECIPE_VIEWER_PRESENT;
            };

            int panelX, panelWidth;
            int widthOverride = AMIConfig.PANEL_WIDTH_OVERRIDE.get();
            if (goLeft) {
                int available = containerScreen.getGuiLeft() - 12;
                panelWidth = widthOverride > 0 ? widthOverride : available;
                panelX = containerScreen.getGuiLeft() - panelWidth - 6;
            } else {
                panelX = containerScreen.getGuiLeft() + containerScreen.getXSize() + 6;
                int available = event.getScreen().width - panelX - 6;
                panelWidth = widthOverride > 0 ? widthOverride : available;
            }

            if (panelWidth < 60) return;

            if (resultsPanel == null) {
                resultsPanel = new UniversalResultsPanel(panelX, panelY, panelWidth, panelHeight);
                refreshEntries();
            }

            checkAndRefreshIfStale();

            resultsPanel.updateLayout(panelX, panelY, panelWidth, panelHeight);

            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0, 0, 1000);
            resultsPanel.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            event.getGuiGraphics().pose().popPose();
        } catch (Exception e) {
            AMI.LOGGER.error("AMI overlay render failed", e);
        }
    }

    @SubscribeEvent
    static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        // Handle search bar keyboard input (Backspace, Escape)
        if (resultsPanel != null && resultsPanel.isSearchFocused()) {
            if (event.getKeyCode() == GLFW.GLFW_KEY_BACKSPACE) {
                resultsPanel.deleteSearchChar();
                triggerSearch();
                event.setCanceled(true);
                return;
            } else if (event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
                resultsPanel.clearSearch();
                refreshEntries();
                event.setCanceled(true);
                return;
            }
        }

        if (AMIKeyMappings.CYCLE_ATLAS.matches(event.getKeyCode(), event.getScanCode())) {
            if (resultsPanel != null) {
                resultsPanel.clearSearch();
            }
            atlasType = atlasType.next();
            refreshEntries();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get() || resultsPanel == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (!resultsPanel.isMouseOver(event.getMouseX(), event.getMouseY())) {
            // Click outside panel: unfocus search bar
            resultsPanel.setSearchFocused(false);
            return;
        }

        // Scrollbar takes priority over other clicks
        if (resultsPanel.mouseClickedScrollbar(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
        // Handle toolbar and tree clicks
        else if (resultsPanel.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            triggerSearch();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onScreenMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (resultsPanel == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (resultsPanel.mouseDragged(event.getMouseX(), event.getMouseY(),
                event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onScreenMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (resultsPanel == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        resultsPanel.stopScrollbarDrag();
    }

    @SubscribeEvent
    static void onScreenMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get() || resultsPanel == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        if (resultsPanel.isMouseOver(event.getMouseX(), event.getMouseY())) {
            resultsPanel.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onScreenCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (resultsPanel == null || !resultsPanel.isSearchFocused()) return;

        char c = (char) event.getCodePoint();
        resultsPanel.typeCharacter(c);
        triggerSearch();
        event.setCanceled(true);
    }

    private static void checkAndRefreshIfStale() {
        if (resultsPanel == null) return;
        // Refresh if empty (including when structures are loading) or when data is populated
        var index = GlobalIndex.getInstance();
        int currentCount = index.getNodes(atlasType).size();
        boolean isLoading = index.isLoading(atlasType);

        if ((resultsPanel.getEntryCount() == 0 && currentCount > 0) ||
            (isLoading && resultsPanel.getEntryCount() == 0)) {
            refreshEntries();
        }
    }

    private static void refreshEntries() {
        if (resultsPanel == null) return;

        var entries = GlobalIndex.getInstance().getNodes(atlasType);
        resultsPanel.setAtlasEntries(entries, atlasType.displayName(), atlasType);

        AMI.LOGGER.debug("AMI overlay refreshed: {} entries of type {}", resultsPanel.getEntryCount(), atlasType);
    }

    private static void triggerSearch() {
        if (searchService == null || resultsPanel == null) return;
        String query = resultsPanel.getSearchQuery();
        if (query.isEmpty()) {
            refreshEntries();
            return;
        }
        var results = searchService.query(query);
        resultsPanel.setSearchResults(results, query);
    }

    // -------------------------------------------------------------------------
    // Public accessors for exclusion areas
    // -------------------------------------------------------------------------

    public static UniversalResultsPanel getResultsPanel() {
        return resultsPanel;
    }
}
