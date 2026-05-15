package com.sanhiruzu.ami.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.ProviderRegistry;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");

    // Bottom bar dimensions (search bar + AMI button rendered BELOW the panel)
    private static final int BOTTOM_BAR_H = 18;
    private static final int SEARCH_H     = 14;
    private static final int SEARCH_W     = 160;  // Centered search bar width (EMI pattern)
    private static final int AMI_BTN_W    = 22;

    private static UniversalResultsPanel resultsPanel;

    // Panel geometry remembered between render and click frames
    private static int lastPanelX    = 0;
    private static int lastPanelW    = 0;
    private static int lastScreenH   = 0;

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
            // Async lazy-load on first open
            if (!indexingDispatched && !indexingInProgress) {
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    indexingInProgress = true;
                    if (resultsPanel != null) resultsPanel.setIndexingInProgress(true);

                    GlobalIndexCache.loadOrIndexAsync(level, () -> {
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

            // Panel occupies everything ABOVE the bottom bar
            int screenW = event.getScreen().width;
            int screenH = event.getScreen().height;
            int panelH  = screenH - BOTTOM_BAR_H;

            boolean goLeft = switch (AMIConfig.PANEL_SIDE.get()) {
                case LEFT  -> true;
                case RIGHT -> false;
                case AUTO  -> RECIPE_VIEWER_PRESENT;
            };

            int panelX, panelW;
            int widthOverride = AMIConfig.PANEL_WIDTH_OVERRIDE.get();
            if (goLeft) {
                int available = containerScreen.getGuiLeft() - 12;
                panelW = widthOverride > 0 ? widthOverride : available;
                panelX = containerScreen.getGuiLeft() - panelW - 6;
            } else {
                panelX = containerScreen.getGuiLeft() + containerScreen.getXSize() + 6;
                int available = screenW - panelX - 6;
                panelW = widthOverride > 0 ? widthOverride : available;
            }

            if (panelW < 60) return;

            lastPanelX  = panelX;
            lastPanelW  = panelW;
            lastScreenH = screenH;

            if (resultsPanel == null) {
                resultsPanel = new UniversalResultsPanel(panelX, 0, panelW, panelH);
                refreshEntries();
            }

            checkAndRefreshIfStale();
            resultsPanel.updateLayout(panelX, 0, panelW, panelH);

            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0, 0, 1000);
            resultsPanel.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            // Bottom bar (search + AMI button) rendered after panel, same Z-layer
            renderBottomBar(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
            event.getGuiGraphics().pose().popPose();

        } catch (Exception e) {
            AMI.LOGGER.error("AMI overlay render failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // Bottom bar rendering (external to the panel widget)
    // -------------------------------------------------------------------------

    private static void renderBottomBar(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;

        // EMI-style: search bar centered on screen, AMI button at screen's absolute left
        int screenW = g.guiWidth();
        int barY     = lastScreenH - BOTTOM_BAR_H;
        int btnX     = 2;  // Screen's absolute left edge (like EMI's settings button)
        int btnY     = barY + 2;
        int searchX  = (screenW - SEARCH_W) / 2;  // Centered on screen
        int searchW  = SEARCH_W;

        // AMI button — at screen's lower-left corner
        boolean btnHovered = mouseX >= btnX && mouseX < btnX + AMI_BTN_W
                && mouseY >= btnY && mouseY < btnY + SEARCH_H;
        int btnBorder    = btnHovered ? 0xFFFFAA00 : 0xFF555555;
        int btnTextColor = btnHovered ? 0xFFFFDD44 : 0xFFFFAA00;
        g.fill(btnX, btnY, btnX + AMI_BTN_W, btnY + SEARCH_H, 0xFF0A0A0A);
        g.fill(btnX,              btnY,              btnX + AMI_BTN_W, btnY + 1,              btnBorder);
        g.fill(btnX,              btnY + SEARCH_H - 1, btnX + AMI_BTN_W, btnY + SEARCH_H,      btnBorder);
        g.fill(btnX,              btnY,              btnX + 1,           btnY + SEARCH_H,      btnBorder);
        g.fill(btnX + AMI_BTN_W - 1, btnY,          btnX + AMI_BTN_W, btnY + SEARCH_H,        btnBorder);
        int labelW = font.width("AMI");
        g.drawString(font, "AMI", btnX + (AMI_BTN_W - labelW) / 2, btnY + 3, btnTextColor, false);

        // Search bar — in the panel's column
        boolean focused = resultsPanel != null && resultsPanel.isSearchFocused();
        String query    = resultsPanel != null ? resultsPanel.getSearchQuery() : "";

        g.fill(searchX, btnY, searchX + searchW, btnY + SEARCH_H,
                focused ? 0xFF2E2E2E : 0xFF1A1A1A);
        int border = focused ? 0xFFAAAA44 : 0xFF555555;
        g.fill(searchX,              btnY,              searchX + searchW, btnY + 1,         border);
        g.fill(searchX,              btnY + SEARCH_H - 1, searchX + searchW, btnY + SEARCH_H, border);
        g.fill(searchX,              btnY,              searchX + 1,        btnY + SEARCH_H, border);
        g.fill(searchX + searchW - 1, btnY,            searchX + searchW, btnY + SEARCH_H,  border);

        int textX = searchX + 3;
        int textY = btnY + 3;
        if (query.isEmpty() && !focused) {
            g.drawString(font, Component.translatable("ami.gui.search.placeholder"),
                    textX, textY, 0xFF666666, false);
        } else {
            g.drawString(font, query, textX, textY, 0xFFCCCCCC, false);
        }

        if (focused && (System.currentTimeMillis() % 1000) < 500) {
            int cursorX = textX + font.width(query) + 1;
            g.fill(cursorX, textY, cursorX + 1, textY + font.lineHeight, 0xFFCCCCCC);
        }
    }

    // -------------------------------------------------------------------------
    // Hit tests for bottom bar elements
    // -------------------------------------------------------------------------

    private static boolean isAmiBtnHit(double mx, double my) {
        int btnX = 2;
        int btnY = lastScreenH - BOTTOM_BAR_H + 2;
        return mx >= btnX && mx < btnX + AMI_BTN_W && my >= btnY && my < btnY + SEARCH_H;
    }

    private static boolean isSearchBarHit(double mx, double my) {
        int screenW = Minecraft.getInstance().getWindow().getWidth();
        int searchX = (screenW - SEARCH_W) / 2;
        int searchY = lastScreenH - BOTTOM_BAR_H + 2;
        return mx >= searchX && mx < searchX + SEARCH_W && my >= searchY && my < searchY + SEARCH_H;
    }

    // -------------------------------------------------------------------------
    // Events
    // -------------------------------------------------------------------------

    @SubscribeEvent
    static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (resultsPanel == null || !resultsPanel.isSearchFocused()) return;

        // When search is focused, handle search keys and block everything else
        // to prevent inventory close key (E) and other screen controls from firing
        int keyCode = event.getKeyCode();

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            resultsPanel.deleteSearchChar();
            triggerSearch();
            event.setCanceled(true);
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            resultsPanel.clearSearch();
            refreshEntries();
            event.setCanceled(true);
        } else {
            // Block all other keys (including inventory close key) while searching
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get() || resultsPanel == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        double mx = event.getMouseX(), my = event.getMouseY();

        // AMI button → open full-screen AMI
        if (isAmiBtnHit(mx, my)) {
            Minecraft.getInstance().setScreen(new AMIScreen());
            event.setCanceled(true);
            return;
        }

        // Search bar → focus it (no search trigger — typing does that)
        if (isSearchBarHit(mx, my)) {
            resultsPanel.setSearchFocused(true);
            event.setCanceled(true);
            return;
        }

        // Click outside panel AND bottom bar → unfocus search
        if (!resultsPanel.isMouseOver(mx, my)) {
            resultsPanel.setSearchFocused(false);
            return;
        }

        // Panel clicks: scrollbar takes priority
        if (resultsPanel.mouseClickedScrollbar(mx, my, event.getButton())) {
            event.setCanceled(true);
        } else if (resultsPanel.mouseClicked(mx, my, event.getButton())) {
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

        resultsPanel.typeCharacter((char) event.getCodePoint());
        triggerSearch();
        event.setCanceled(true);
    }

    // -------------------------------------------------------------------------
    // Data management
    // -------------------------------------------------------------------------

    private static void checkAndRefreshIfStale() {
        if (resultsPanel == null) return;
        int totalCount = 0;
        for (NodeType t : NodeType.atlasValues()) {
            totalCount += GlobalIndex.getInstance().getNodes(t).size();
        }
        if (resultsPanel.getEntryCount() == 0 && totalCount > 0) {
            refreshEntries();
        }
    }

    static void refreshEntries() {
        if (resultsPanel == null) return;

        List<SearchNode> all = new ArrayList<>();
        for (NodeType t : NodeType.atlasValues()) {
            all.addAll(GlobalIndex.getInstance().getNodes(t));
        }

        resultsPanel.setEntries(all);
        AMI.LOGGER.debug("AMI overlay refreshed: {} total entries across all types", all.size());
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
    // Exclusion area accessor for EMI plugin
    // -------------------------------------------------------------------------

    public static UniversalResultsPanel getResultsPanel() {
        return resultsPanel;
    }
}
