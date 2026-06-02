package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.UniversalResultsPanel;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OverlayWidgetManager {
    public static final int AMI_BTN_X = 2;
    public static final int AMI_BTN_W = 32;
    private static final int BOTTOM_BAR_H = 32;
    private static final int SEARCH_H = 24;
    private static final int MIN_PANEL_WIDTH = 64;
    private static final int MAX_PANEL_WIDTH = 420;
    private static final int PANEL_MARGIN = 6;
    private static final int PANEL_MARGIN_V = 6;
    private static final int AMI_BTN_H = 22;
    private static final int AMI_BTN_MARGIN = 4;
    public static final int AMI_BTN_NEXT_X = AMI_BTN_X + AMI_BTN_W + AMI_BTN_MARGIN;

    private final List<PanelSlot> leftSlotPool = new ArrayList<>();
    private final List<PanelSlot> rightSlotPool = new ArrayList<>();
    private final List<PanelSlot> activeSlots = new ArrayList<>();
    private SearchBarWidget searchBar;
    private AmiButtonWidget amiButton;
    private boolean widgetsReady = false;

    private boolean panelVisible = false;
    private String lastSyncedQuery = "";
    private WidgetBounds lastResultsBounds = null;
    private int lastScreenH = 0;
    private boolean pendingEmiReinit = false;
    private boolean leftAlternateActive = false;
    private boolean rightAlternateActive = false;
    private WidgetBounds leftStripBounds = null;
    private WidgetBounds rightStripBounds = null;
    private boolean searchBarEmbedded = false;
    private int lastLayoutSignature = Integer.MIN_VALUE;
    private boolean layoutDirty = true;

    public OverlayWidgetManager() {
    }

    private void ensureWidgets() {
        if (widgetsReady) return;
        this.searchBar = new SearchBarWidget(this::triggerSearch);
        this.amiButton = new AmiButtonWidget(() -> {
            var mc = Minecraft.getInstance();
            mc.setScreen(new com.sanhiruzu.ami.client.screen.AmiConfigScreen(mc.screen));
        }, InventoryOverlayHandler::toggleAmi, () -> panelVisible);

        com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.getInstance().setOnChange(this::refreshSidebarsAndLayout);

        widgetsReady = true;
    }

    private PanelSlot getSlot(List<PanelSlot> pool, int index, boolean leftSide) {
        while (pool.size() <= index) {
            pool.add(createPanelSlot(leftSide));
        }
        return pool.get(index);
    }

    private PanelSlot createPanelSlot(boolean leftSide) {
        PanelSlot slot = new PanelSlot();
        configureResultsCallbacks(slot.results);
        Runnable refreshSidebars = this::refreshSidebars;
        slot.sidebar.getInnerPanel().setOnReset(refreshSidebars);
        if (hasAlternateContent(leftSide)) {
            slot.setOnModeToggle(leftSide ? this::toggleLeftAlternate : this::toggleRightAlternate,
                    leftSide ? () -> leftAlternateActive : () -> rightAlternateActive);
        }
        return slot;
    }

    private boolean hasAlternateContent(boolean leftSide) {
        List<AmiConfig.PanelContent> alt;
        if (leftSide) {
            alt = configuredContents(AmiConfig.leftPanelAlternateSlots,
                    AmiConfig.leftPanelAlternateContent, AmiConfig.leftPanelAlternateSecondaryContent);
        } else {
            alt = configuredContents(AmiConfig.rightPanelAlternateSlots,
                    AmiConfig.rightPanelAlternateContent, AmiConfig.rightPanelAlternateSecondaryContent);
        }
        return !alt.isEmpty();
    }

    private void configureResultsCallbacks(ResultsPanelWidget panel) {
        panel.setOnReset(searchBar::clear);
        panel.setOnTokenInject(token -> searchBar.toggleToken(token));
        panel.setOnModClick(token -> {
            searchBar.toggleToken(token);
            String modId = token.startsWith("@") ? token.substring(1) : token;
            var inner = panel.getInnerPanel();
            if (inner != null) inner.getState().toggleMod(modId);
        });
    }

    public void computeLayouts(AbstractContainerScreen<?> containerScreen, int screenW, int screenH) {
        ensureWidgets();
        activeSlots.clear();
        hideAllSlots();
        searchBarEmbedded = false;
        lastScreenH = screenH;

        amiButton.updateBounds(new WidgetBounds(AMI_BTN_X, screenH - AMI_BTN_H - AMI_BTN_MARGIN, AMI_BTN_W, AMI_BTN_H));

        int usableH = screenH - BOTTOM_BAR_H - PANEL_MARGIN_V * 2;
        int panelH = Math.min(usableH, 600);
        int panelY = PANEL_MARGIN_V + (usableH - panelH) / 2;

        int leftMaxH = 360; // Sidebars don't need to be as tall as the main results grid
        int leftH = Math.min(usableH, leftMaxH);
        int leftY = PANEL_MARGIN_V + (usableH - leftH) / 2;

        int containerLeftEdge = containerScreen.getGuiLeft();
        int containerRightEdge = containerScreen.getGuiLeft() + containerScreen.getXSize();

        List<AmiConfig.PanelContent> leftContents = leftContents();
        if (!leftContents.isEmpty()) {
            int leftW = Math.min(AmiConfig.leftPanelWidth, containerLeftEdge - PANEL_MARGIN * 2);
            if (leftW >= 40) {
                Rect leftSlot = Rect.of(PANEL_MARGIN, leftY, leftW, leftH);
                placeSideSlots(leftSlot, leftContents, leftSlotPool);
                // Claim full left vertical strip to screen bottom so JEI can't use the corner
                leftStripBounds = new WidgetBounds(0, 0, leftSlot.x() + leftSlot.w(), screenH - BOTTOM_BAR_H);
            }
        } else {
            leftStripBounds = null;
        }

        int safeWidth = screenW - containerRightEdge - PANEL_MARGIN * 2;
        int panelStartX = screenW;
        if (safeWidth >= MIN_PANEL_WIDTH) {
            int configuredRightWidth = AmiConfig.rightPanelWidth > 0
                    ? AmiConfig.rightPanelWidth
                    : net.minecraft.util.Mth.clamp((int) (screenW * 0.35f), MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
            int rw = net.minecraft.util.Mth.clamp(configuredRightWidth, MIN_PANEL_WIDTH, Math.min(safeWidth, MAX_PANEL_WIDTH));
            panelStartX = screenW - rw - PANEL_MARGIN;
            // Extend right panel to screen bottom so JEI's config button has no
            // empty space to draw in below the panel.
            int rightH = screenH - BOTTOM_BAR_H - PANEL_MARGIN_V - panelY;
            Rect rightSlot = Rect.of(panelStartX, panelY, rw, rightH);

            List<AmiConfig.PanelContent> rightContents = rightContents();
            placeSideSlots(rightSlot, rightContents, rightSlotPool);
            lastResultsBounds = rightSlot.toWidgetBounds();
            if (shouldEmbedSearchBar(rightContents, lastResultsBounds)) {
                searchBar.updateBounds(UniversalResultsPanel.embeddedSearchBounds(lastResultsBounds));
                searchBarEmbedded = true;
            }
            // Claim full right vertical strip to screen bottom so JEI can't use the corner
            rightStripBounds = new WidgetBounds(panelStartX, 0, screenW - panelStartX, screenH - BOTTOM_BAR_H);
        } else {
            lastResultsBounds = null;
            rightStripBounds = null;
        }

        if (!searchBarEmbedded) {
            int maxBarRight = (safeWidth >= MIN_PANEL_WIDTH) ? (panelStartX - PANEL_MARGIN) : (screenW - 4);
            int barW = Math.min(AmiConfig.searchBarWidth, screenW - 8);
            int barX = Math.max(4, (screenW - barW) / 2);
            if (barX + barW > maxBarRight) {
                barX = Math.max(4, maxBarRight - barW);
                barW = Math.max(60, Math.min(barW, maxBarRight - barX));
            }
            searchBar.updateBounds(new WidgetBounds(barX, screenH - BOTTOM_BAR_H + 2, barW, SEARCH_H));
        }

        rememberLayout(containerScreen, screenW, screenH);
    }

    private void placeSideSlots(Rect sideSlot, List<AmiConfig.PanelContent> contents, List<PanelSlot> pool) {
        List<AmiConfig.PanelContent> renderable = contents.stream()
                .filter(this::isRenderableContent)
                .toList();
        if (renderable.isEmpty()) return;

        List<Rect> rows = splitRows(sideSlot, renderable.size(), PANEL_MARGIN);
        for (int i = 0; i < renderable.size(); i++) {
            PanelSlot slot = getSlot(pool, i, pool == leftSlotPool);
            slot.place(rows.get(i), renderable.get(i));
            activeSlots.add(slot);
        }
    }

    private List<Rect> splitRows(Rect slot, int count, int gap) {
        List<Rect> rows = new ArrayList<>();
        if (count <= 0) return rows;
        int totalGap = gap * (count - 1);
        int usable = Math.max(0, slot.h() - totalGap);
        int base = usable / count;
        int remainder = usable % count;
        int y = slot.y();
        for (int i = 0; i < count; i++) {
            int h = base + (i < remainder ? 1 : 0);
            rows.add(Rect.of(slot.x(), y, slot.w(), h));
            y += h + gap;
        }
        return rows;
    }

    private void hideAllSlots() {
        leftSlotPool.forEach(PanelSlot::hide);
        rightSlotPool.forEach(PanelSlot::hide);
    }

    public void computeLayouts(Screen screen, int screenW, int screenH) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            computeLayouts(containerScreen, screenW, screenH);
        } else {
            computeLayoutsForCustomScreen(screen, screenW, screenH);
        }
    }

    /**
     * Layout for screens that are not inventory containers (e.g. EMI's RecipeScreen).
     * Panels are kept in the left/right margins, away from the center of the screen
     * where recipe content and other modal UI typically renders.
     */
    private void computeLayoutsForCustomScreen(Screen screen, int screenW, int screenH) {
        ensureWidgets();
        activeSlots.clear();
        hideAllSlots();
        searchBarEmbedded = false;
        lastScreenH = screenH;

        amiButton.updateBounds(new WidgetBounds(AMI_BTN_X, screenH - AMI_BTN_H - AMI_BTN_MARGIN, AMI_BTN_W, AMI_BTN_H));

        int usableH = screenH - BOTTOM_BAR_H - PANEL_MARGIN_V * 2;
        int panelH = Math.min(usableH, 600);
        int panelY = PANEL_MARGIN_V + (usableH - panelH) / 2;

        int leftMaxH = 360;
        int leftH = Math.min(usableH, leftMaxH);
        int leftY = PANEL_MARGIN_V + (usableH - leftH) / 2;

        // Reserve the middle of the screen for recipe content / modal UI.
        // This avoids overlapping the recipe view without needing to know
        // exact recipe bounds via reflection.
        int centerReserve = Math.max(screenW / 3, 200);
        int centerLeft = (screenW - centerReserve) / 2;
        int centerRight = centerLeft + centerReserve;

        List<AmiConfig.PanelContent> leftContents = leftContents();
        if (!leftContents.isEmpty()) {
            int leftW = Math.min(AmiConfig.leftPanelWidth, centerLeft - PANEL_MARGIN * 2);
            if (leftW >= 40) {
                Rect leftSlot = Rect.of(PANEL_MARGIN, leftY, leftW, leftH);
                placeSideSlots(leftSlot, leftContents, leftSlotPool);
                leftStripBounds = new WidgetBounds(0, 0, leftSlot.x() + leftSlot.w(), screenH - BOTTOM_BAR_H);
            }
        } else {
            leftStripBounds = null;
        }

        int safeWidth = screenW - centerRight - PANEL_MARGIN * 2;
        int panelStartX = screenW;
        if (safeWidth >= MIN_PANEL_WIDTH) {
            int configuredRightWidth = AmiConfig.rightPanelWidth > 0
                    ? AmiConfig.rightPanelWidth
                    : net.minecraft.util.Mth.clamp((int) (screenW * 0.35f), MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
            int rw = net.minecraft.util.Mth.clamp(configuredRightWidth, MIN_PANEL_WIDTH, Math.min(safeWidth, MAX_PANEL_WIDTH));
            panelStartX = screenW - rw - PANEL_MARGIN;
            int rightH = screenH - BOTTOM_BAR_H - PANEL_MARGIN_V - panelY;
            Rect rightSlot = Rect.of(panelStartX, panelY, rw, rightH);

            List<AmiConfig.PanelContent> rightContents = rightContents();
            placeSideSlots(rightSlot, rightContents, rightSlotPool);
            lastResultsBounds = rightSlot.toWidgetBounds();
            if (shouldEmbedSearchBar(rightContents, lastResultsBounds)) {
                searchBar.updateBounds(UniversalResultsPanel.embeddedSearchBounds(lastResultsBounds));
                searchBarEmbedded = true;
            }
            rightStripBounds = new WidgetBounds(panelStartX, 0, screenW - panelStartX, screenH - BOTTOM_BAR_H);
        } else {
            lastResultsBounds = null;
            rightStripBounds = null;
        }

        if (!searchBarEmbedded) {
            int maxBarRight = (safeWidth >= MIN_PANEL_WIDTH) ? (panelStartX - PANEL_MARGIN) : (screenW - 4);
            int barW = Math.min(AmiConfig.searchBarWidth, screenW - 8);
            int barX = Math.max(4, (screenW - barW) / 2);
            if (barX + barW > maxBarRight) {
                barX = Math.max(4, maxBarRight - barW);
                barW = Math.max(60, Math.min(barW, maxBarRight - barX));
            }
            searchBar.updateBounds(new WidgetBounds(barX, screenH - BOTTOM_BAR_H + 2, barW, SEARCH_H));
        }

        rememberLayout(screen, screenW, screenH);
    }

    public void invalidateLayout() {
        layoutDirty = true;
    }

    public void refreshLayoutIfNeeded(Screen screen) {
        if (screen == null) return;
        int signature = layoutSignature(screen, screen.width, screen.height);
        if (layoutDirty || signature != lastLayoutSignature) {
            computeLayouts(screen, screen.width, screen.height);
        }
    }

    private void rememberLayout(Screen screen, int screenW, int screenH) {
        lastLayoutSignature = layoutSignature(screen, screenW, screenH);
        layoutDirty = false;
    }

    private int layoutSignature(Screen screen, int screenW, int screenH) {
        int guiLeft = -1;
        int guiTop = -1;
        int guiWidth = -1;
        int guiHeight = -1;
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            guiLeft = containerScreen.getGuiLeft();
            guiTop = containerScreen.getGuiTop();
            guiWidth = containerScreen.getXSize();
            guiHeight = containerScreen.getYSize();
        }
        return Objects.hash(
                System.identityHashCode(screen),
                screenW,
                screenH,
                guiLeft,
                guiTop,
                guiWidth,
                guiHeight,
                AmiConfig.leftPanelWidth,
                AmiConfig.rightPanelWidth,
                AmiConfig.leftPanelSlots,
                AmiConfig.leftPanelAlternateSlots,
                AmiConfig.rightPanelSlots,
                AmiConfig.rightPanelAlternateSlots,
                leftAlternateActive,
                rightAlternateActive,
                panelVisible
        );
    }

    private boolean shouldEmbedSearchBar(List<AmiConfig.PanelContent> contents, WidgetBounds panelBounds) {
        if (contents == null || contents.isEmpty() || panelBounds == null) return false;
        AmiConfig.PanelContent first = contents.get(0);
        return (first == AmiConfig.PanelContent.GRID || first == AmiConfig.PanelContent.LIST)
                && UniversalResultsPanel.supportsEmbeddedSearch(panelBounds);
    }

    private boolean isSearchContent(AmiConfig.PanelContent content) {
        return content == AmiConfig.PanelContent.GRID || content == AmiConfig.PanelContent.LIST || content == AmiConfig.PanelContent.COMPACT;
    }

    private boolean isRenderableContent(AmiConfig.PanelContent content) {
        return isSearchContent(content) || isSidebarContent(content);
    }

    private boolean isSidebarContent(AmiConfig.PanelContent content) {
        return content == AmiConfig.PanelContent.FAVORITES || content == AmiConfig.PanelContent.LOOKUP_HISTORY
                || content == AmiConfig.PanelContent.CRAFTING_HISTORY || content == AmiConfig.PanelContent.CRAFTABLE
                || content == AmiConfig.PanelContent.EMPTY || content == AmiConfig.PanelContent.QUESTS;
    }

    private List<AmiConfig.PanelContent> leftContents() {
        return configuredContents(leftAlternateActive ? AmiConfig.leftPanelAlternateSlots : AmiConfig.leftPanelSlots,
                leftAlternateActive ? AmiConfig.leftPanelAlternateContent : AmiConfig.leftPanelContent,
                leftAlternateActive ? AmiConfig.leftPanelAlternateSecondaryContent : AmiConfig.leftPanelSecondaryContent);
    }

    private List<AmiConfig.PanelContent> rightContents() {
        return configuredContents(rightAlternateActive ? AmiConfig.rightPanelAlternateSlots : AmiConfig.rightPanelSlots,
                rightAlternateActive ? AmiConfig.rightPanelAlternateContent : AmiConfig.rightPanelContent,
                rightAlternateActive ? AmiConfig.rightPanelAlternateSecondaryContent : AmiConfig.rightPanelSecondaryContent);
    }

    private List<AmiConfig.PanelContent> configuredContents(String rawSlots, AmiConfig.PanelContent primary, AmiConfig.PanelContent secondary) {
        List<AmiConfig.PanelContent> parsed = AmiConfig.parsePanelSlots(rawSlots);
        if (!parsed.isEmpty() || rawSlots != null && !rawSlots.isBlank()) {
            return parsed;
        }
        List<AmiConfig.PanelContent> legacy = new ArrayList<>();
        if (primary != null && primary != AmiConfig.PanelContent.NONE) legacy.add(primary);
        if (secondary != null && secondary != AmiConfig.PanelContent.NONE) legacy.add(secondary);
        return legacy;
    }

    private void toggleLeftAlternate() {
        leftAlternateActive = !leftAlternateActive;
        refreshCurrentLayout();
    }

    private void toggleRightAlternate() {
        rightAlternateActive = !rightAlternateActive;
        refreshCurrentLayout();
    }

    private void refreshCurrentLayout() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen != null) {
            computeLayouts(screen, screen.width, screen.height);
        }
    }

    private void refreshSidebarsAndLayout() {
        refreshCurrentLayout();
        refreshSidebars();
    }

    public void tick(ScreenEvent.Render.Post event) {
        if (!AmiConfig.enableAutoIndexing) return;
        if (!panelVisible) return;

        ensureWidgets();
        var indexer = AmiIndexerService.getInstance();
        if (indexer.isReady()) {
            var service = indexer.getOrBuildSearchService();
            boolean needsRefresh = false;
            for (ResultsPanelWidget panel : getResultPanels()) {
                var inner = panel.getInnerPanel();
                if (inner != null && inner.getEntryCount() == 0 && indexer.indexedItemCount() > 0) {
                    inner.setSearchService(service);
                    needsRefresh = true;
                }
            }
            if (needsRefresh) refreshEntries();
        }

        syncFromRecipeViewer();

        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() % 20 == 0) {
            refreshSidebars();
        }
    }

    public void renderAll(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        try {
            g.flush();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            g.pose().pushPose();
            g.pose().translate(0, 0, 0);

            amiButton.render(g, mx, my, pt);

            if (panelVisible) {
                renderPanels(g, mx, my, pt);
                renderSearchBar(g, mx, my, pt);
            }

            g.pose().popPose();
            g.flush();

        } catch (Exception e) {
            AMI.LOGGER.error("AMI overlay render failed", e);
        }

        if (pendingEmiReinit) {
            pendingEmiReinit = false;
            var mc = Minecraft.getInstance();
            if (mc.screen != null) mc.screen.init(mc, mc.screen.width, mc.screen.height);
        }
    }

    public void renderPanels(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        if (!panelVisible) return;
        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        g.pose().pushPose();
        g.pose().translate(0, 0, OverlayLayers.PANEL);

        for (PanelSlot slot : activeSlots) {
            slot.render(g, mx, my, pt);
        }
        for (PanelSlot slot : activeSlots) {
            slot.renderOverlay(g, mx, my);
        }
        renderCheatDeleteHint(g, mx, my);

        if (AmiConfig.highlightExclusionAreas) {
            g.pose().pushPose();
            g.pose().translate(0, 0, OverlayLayers.DEBUG);
            renderExclusionHighlights(g);
            g.pose().popPose();
        }

        g.pose().popPose();
        g.flush();
    }

    public void renderSearchBar(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        if (!panelVisible || searchBar == null) return;
        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        g.pose().pushPose();
        g.pose().translate(0, 0, OverlayLayers.PANEL + 1);
        searchBar.render(g, mx, my, pt);
        g.pose().popPose();
        g.flush();
    }


    private void renderCheatDeleteHint(net.minecraft.client.gui.GuiGraphics g, int mx, int my) {
        if (!com.sanhiruzu.ami.client.AMICheatMode.isEnabled()) return;
        if (!com.sanhiruzu.ami.client.AMICheatMode.hasCarriedItem()) return;
        for (PanelSlot slot : activeSlots) {
            if (slot.results.visible && slot.results.isMouseOver(mx, my)) {
                var font = net.minecraft.client.Minecraft.getInstance().font;
                var msg = net.minecraft.network.chat.Component.translatable("ami.cheat.drop_to_delete");
                g.pose().pushPose();
                g.pose().translate(0, 0, OverlayLayers.TRANSIENT_TOOLTIP);
                g.renderTooltip(font, List.of(msg), java.util.Optional.empty(), mx, my);
                g.pose().popPose();
                break;
            }
        }
    }

    private void renderExclusionHighlights(net.minecraft.client.gui.GuiGraphics g) {
        // Render panel bounds in blue (matching EMI's debug style)
        for (PanelSlot slot : activeSlots) {
            if (slot.results.visible) {
                WidgetBounds b = slot.results.getBounds();
                g.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), 0x440000ff);
            }
            if (slot.sidebar.visible) {
                WidgetBounds b = slot.sidebar.getBounds();
                g.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), 0x440000ff);
            }
        }

        // Render exclusion bounds: first in green, rest in red (matching EMI's convention)
        List<WidgetBounds> exclusions = getExclusionBounds();
        for (int i = 0; i < exclusions.size(); i++) {
            WidgetBounds b = exclusions.get(i);
            int color = i == 0 ? 0x4400ff00 : 0x44ff0000;
            g.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), color);
        }
    }

    private void refreshEntries() {
        List<SearchNode> all = new ArrayList<>();
        for (NodeType t : NodeType.atlasValues()) all.addAll(GlobalIndex.getInstance().getNodes(t));
        for (ResultsPanelWidget panel : getResultPanels()) {
            if (panel.getInnerPanel() != null) panel.getInnerPanel().setEntries(all);
        }
        refreshSidebars();
    }

    public void refreshSidebars() {
        for (SidebarPanelWidget panel : getSidebarPanels()) {
            panel.refresh();
        }
    }

    private void triggerSearch(String query) {
        for (ResultsPanelWidget panel : getResultPanels()) {
            if (panel.getInnerPanel() != null) panel.getInnerPanel().getState().setQuery(query);
        }
        if (RecipeViewerBridge.supportsSearchSync() && !query.equals(lastSyncedQuery)) {
            lastSyncedQuery = query;
            RecipeViewerBridge.setSearchText(query);
        }
    }

    private void syncFromRecipeViewer() {
        if (searchBar == null) return;
        if (!RecipeViewerBridge.supportsSearchSync()) return;
        if (searchBar.isFocused()) return;

        String rvQuery = RecipeViewerBridge.getSearchText();
        if (!rvQuery.equals(lastSyncedQuery)) {
            lastSyncedQuery = rvQuery;
            searchBar.setQuery(rvQuery);
            for (ResultsPanelWidget panel : getResultPanels()) {
                if (panel.getInnerPanel() != null) panel.getInnerPanel().getState().setQuery(rvQuery);
            }
        }
    }


    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        for (PanelSlot slot : activeSlots) {
            if (slot.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return mouseScrolled(mouseX, mouseY, scrollY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (PanelSlot slot : activeSlots) {
            if (slot.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (PanelSlot slot : activeSlots) {
            if (slot.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (PanelSlot slot : activeSlots) {
            slot.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (PanelSlot slot : activeSlots) {
            if (slot.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        for (PanelSlot slot : activeSlots) {
            if (slot.charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }

    public boolean hasOpenContextMenu() {
        for (PanelSlot slot : activeSlots) {
            if (slot.hasOpenContextMenu()) return true;
        }
        return false;
    }

    public boolean isMouseOverPanel(double mouseX, double mouseY) {
        for (PanelSlot slot : activeSlots) {
            if (slot.isMouseOver(mouseX, mouseY)) return true;
        }
        return false;
    }

    public SearchNode getHoveredNode() {
        for (PanelSlot slot : activeSlots) {
            SearchNode node = slot.getHoveredNode();
            if (node != null) return node;
        }
        return null;
    }

    public SidebarPanelWidget getFavoritesPanelAt(double mouseX, double mouseY) {
        for (PanelSlot slot : activeSlots) {
            if (slot.sidebar.visible
                    && slot.sidebar.getContentType() == AmiConfig.PanelContent.FAVORITES
                    && slot.sidebar.isMouseOver(mouseX, mouseY)) {
                return slot.sidebar;
            }
        }
        return null;
    }

    private List<PanelSlot> allSlots() {
        List<PanelSlot> slots = new ArrayList<>(leftSlotPool.size() + rightSlotPool.size());
        slots.addAll(leftSlotPool);
        slots.addAll(rightSlotPool);
        return slots;
    }

    public List<GuiEventListener> getPanelListeners() {
        ensureWidgets();
        List<GuiEventListener> listeners = new ArrayList<>();
        for (PanelSlot slot : allSlots()) {
            listeners.add(slot.results);
            listeners.add(slot.sidebar);
        }
        return listeners;
    }

    private List<ResultsPanelWidget> getResultPanels() {
        List<ResultsPanelWidget> panels = new ArrayList<>();
        for (PanelSlot slot : allSlots()) panels.add(slot.results);
        return panels;
    }

    public List<UniversalResultsPanel> getDebugVisibleResultPanels() {
        List<UniversalResultsPanel> panels = new ArrayList<>();
        for (PanelSlot slot : activeSlots) {
            if (slot.results.visible && slot.results.getInnerPanel() != null) {
                panels.add(slot.results.getInnerPanel());
            }
            if (slot.sidebar.visible && slot.sidebar.getInnerPanel() != null) {
                panels.add(slot.sidebar.getInnerPanel());
            }
        }
        return panels;
    }

    private List<SidebarPanelWidget> getSidebarPanels() {
        List<SidebarPanelWidget> panels = new ArrayList<>();
        for (PanelSlot slot : allSlots()) panels.add(slot.sidebar);
        return panels;
    }

    private void togglePanelVisible() {
        panelVisible = !panelVisible;
        if (!panelVisible) {
            if (searchBar != null) searchBar.clear();
            lastSyncedQuery = "";
        }
        pendingEmiReinit = true;
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    public void setPanelVisible(boolean visible) {
        if (visible != panelVisible) togglePanelVisible();
    }

    public WidgetBounds getResultsBounds() {
        return lastResultsBounds;
    }

    /**
     * Returns bounds of all visible AMI panels and widgets so recipe viewers can avoid overlapping them.
     */
    public List<WidgetBounds> getExclusionBounds() {
        List<WidgetBounds> bounds = new ArrayList<>();
        if (amiButton != null) bounds.add(amiButton.getBounds());
        if (searchBar != null) bounds.addAll(searchBar.getPredictiveBounds());
        for (PanelSlot slot : activeSlots) {
            if (slot.results.visible) bounds.add(slot.results.getBounds());
            if (slot.sidebar.visible) bounds.add(slot.sidebar.getBounds());
        }
        // Filler strips claim the full vertical column on sides with panels,
        // preventing JEI from drawing its config button in the bottom corners.
        if (leftStripBounds != null) bounds.add(leftStripBounds);
        if (rightStripBounds != null) bounds.add(rightStripBounds);
        return bounds;
    }

    public AmiButtonWidget getAmiButton() {
        ensureWidgets();
        return amiButton;
    }

    public SearchBarWidget getSearchBar() {
        ensureWidgets();
        return searchBar;
    }

    public ResultsPanelWidget getResultsPanel() {
        ensureWidgets();
        return getSlot(rightSlotPool, 0, false).results;
    }

    public ResultsPanelWidget getLeftResultsPanel() {
        ensureWidgets();
        return getSlot(leftSlotPool, 0, true).results;
    }

    public ResultsPanelWidget getLeftResultsPanelSecondary() {
        ensureWidgets();
        return getSlot(leftSlotPool, 1, true).results;
    }

    public ResultsPanelWidget getRightResultsPanelSecondary() {
        ensureWidgets();
        return getSlot(rightSlotPool, 1, false).results;
    }

    public ResultsPanelWidget getRightResultsPanel() {
        return getResultsPanel();
    }

    public SidebarPanelWidget getLeftPanel() {
        ensureWidgets();
        return getSlot(leftSlotPool, 0, true).sidebar;
    }

    public SidebarPanelWidget getLeftPanelSecondary() {
        ensureWidgets();
        return getSlot(leftSlotPool, 1, true).sidebar;
    }

    public SidebarPanelWidget getRightPanelPrimary() {
        ensureWidgets();
        return getSlot(rightSlotPool, 0, false).sidebar;
    }

    public SidebarPanelWidget getRightPanelSecondary() {
        ensureWidgets();
        return getSlot(rightSlotPool, 1, false).sidebar;
    }

    private static final class PanelSlot {
        final ResultsPanelWidget results = new ResultsPanelWidget();
        final SidebarPanelWidget sidebar = new SidebarPanelWidget(0, 0, 0, 0, AmiConfig.PanelContent.EMPTY);

        void setOnModeToggle(Runnable callback, java.util.function.BooleanSupplier activeSupplier) {
            results.setOnModeToggle(callback, activeSupplier);
            sidebar.setOnModeToggle(callback, activeSupplier);
        }

        void place(Rect rect, AmiConfig.PanelContent content) {
            if (content == AmiConfig.PanelContent.GRID || content == AmiConfig.PanelContent.LIST || content == AmiConfig.PanelContent.COMPACT) {
                results.setContentType(content);
                results.updateBounds(rect.toWidgetBounds());
                results.visible = true;
                sidebar.visible = false;
            } else {
                sidebar.updateLayout(rect);
                sidebar.visible = true;
                results.visible = false;
                sidebar.setContentType(content);
                sidebar.refresh();
            }
        }

        void hide() {
            results.visible = false;
            sidebar.visible = false;
        }

        void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
            if (results.visible) results.render(g, mx, my, pt);
            if (sidebar.visible) sidebar.render(g, mx, my, pt);
        }

        void renderOverlay(net.minecraft.client.gui.GuiGraphics g, int mx, int my) {
            if (results.visible) results.renderOverlay(g, mx, my);
            if (sidebar.visible) sidebar.renderOverlay(g, mx, my);
        }

        boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
            AbstractWidget widget = activeWidget();
            return widget != null && widget.visible && widget.mouseScrolled(mouseX, mouseY, scrollY);
        }

        boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return mouseScrolled(mouseX, mouseY, scrollY);
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            AbstractWidget widget = activeWidget();
            return widget != null && widget.visible && (widget.isMouseOver(mouseX, mouseY) || hasOpenContextMenu(widget))
                    && widget.mouseClicked(mouseX, mouseY, button);
        }

        private boolean hasOpenContextMenu(AbstractWidget widget) {
            if (widget instanceof ResultsPanelWidget results) {
                return results.isContextMenuOpen();
            }
            if (widget instanceof SidebarPanelWidget sidebar) {
                return sidebar.isContextMenuOpen();
            }
            return false;
        }

        boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            AbstractWidget widget = activeWidget();
            return widget != null && widget.visible && widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        void mouseReleased(double mouseX, double mouseY, int button) {
            if (results.visible) results.mouseReleased(mouseX, mouseY, button);
            if (sidebar.visible) sidebar.mouseReleased(mouseX, mouseY, button);
        }

        boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            AbstractWidget widget = activeWidget();
            return widget != null && widget.visible && widget.keyPressed(keyCode, scanCode, modifiers);
        }

        boolean charTyped(char codePoint, int modifiers) {
            AbstractWidget widget = activeWidget();
            return widget != null && widget.visible && widget.charTyped(codePoint, modifiers);
        }

        boolean hasOpenContextMenu() {
            AbstractWidget widget = activeWidget();
            return widget != null && widget.visible && hasOpenContextMenu(widget);
        }

        boolean isMouseOver(double mouseX, double mouseY) {
            AbstractWidget widget = activeWidget();
            return widget != null && widget.visible && widget.isMouseOver(mouseX, mouseY);
        }

        SearchNode getHoveredNode() {
            if (results.visible && results.getInnerPanel() != null) {
                SearchNode node = results.getInnerPanel().getHoveredNode();
                if (node != null) return node;
            }
            if (sidebar.visible && sidebar.getInnerPanel() != null) {
                return sidebar.getInnerPanel().getHoveredNode();
            }
            return null;
        }

        private AbstractWidget activeWidget() {
            if (results.visible) return results;
            if (sidebar.visible) return sidebar;
            return null;
        }
    }
}
