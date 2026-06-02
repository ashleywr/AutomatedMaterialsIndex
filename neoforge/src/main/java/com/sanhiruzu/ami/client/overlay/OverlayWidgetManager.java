package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.AmiRenderProfiler;
import com.sanhiruzu.ami.client.UniversalResultsPanel;
import com.sanhiruzu.ami.api.AmiPluginRegistry;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.compat.FtbLibrarySidebarCompat;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.neoforge.AMI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OverlayWidgetManager {
    public static final int AMI_BTN_X = 2;
    public static final int AMI_BTN_W = 22;
    private static final int BOTTOM_BAR_H = 32;
    private static final int SEARCH_H = 24;
    private static final int MIN_PANEL_WIDTH = 64;
    private static final int MAX_PANEL_WIDTH = 420;
    private static final int PANEL_MARGIN = 6;
    private static final int PANEL_MARGIN_V = 6;
    private static final int AMI_BTN_H = 20;
    private static final int AMI_BTN_MARGIN = 2;
    private static final int MAX_MARGIN_CONTROL_H = 32;
    private static final int TOP_MARGIN_CONTROL_MAX_Y = 96;
    private static final int MIN_SIDE_PANEL_HEIGHT = 24;
    private static final int MIN_SIDE_PANEL_WIDTH = 24;
    private static final int MIN_SEARCH_PANEL_HEIGHT = 80;
    private static final int WIDTH_SHRINK_STEP_PERCENT = 10;
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
    private int lastThirdPartyMarginSignature = 0;
    private final List<WidgetBounds> lastRejectedPanelBounds = new ArrayList<>();
    private int lastAdaptiveLeftPanelWidth = -1;
    private int lastAdaptiveRightPanelWidth = -1;

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

    /**
     * Returns the maximum bottom-Y of top-anchored third-party controls whose horizontal
     * extent overlaps the given screen strip [stripX1, stripX2). Bottom margin controls
     * such as FTB's "Dark Mode" button remain exclusions, but must not define where a
     * panel can start below top sidebar buttons.
     */
    private int thirdPartyWidgetBottomInStrip(Screen screen, int stripX1, int stripX2, int panelY, int panelBottom) {
        int maxBottom = 0;
        for (WidgetBounds bounds : topSideBlockers(screen, stripX1, stripX2, panelY, panelBottom)) {
            maxBottom = Math.max(maxBottom, bounds.y() + bounds.height());
        }
        return maxBottom;
    }

    private int thirdPartyMarginSignature(Screen screen) {
        int signature = 1;
        for (WidgetBounds bounds : thirdPartyMarginWidgetBounds(screen)) {
            signature = 31 * signature + Objects.hash("third-party-widget", bounds);
        }
        for (WidgetBounds bounds : thirdPartyExclusionBounds(screen)) {
            signature = 31 * signature + Objects.hash("third-party-zone", bounds);
        }
        return signature;
    }

    private List<WidgetBounds> thirdPartyMarginWidgetBounds(Screen screen) {
        List<WidgetBounds> bounds = new ArrayList<>();
        for (var listener : screen.children()) {
            if (!(listener instanceof AbstractWidget w)) continue;
            if (!w.visible) continue;
            if (w.getClass().getName().startsWith("com.sanhiruzu.ami")) continue;
            int wh = w.getHeight();
            if (wh <= 0 || wh > MAX_MARGIN_CONTROL_H) continue;
            bounds.add(new WidgetBounds(w.getX(), w.getY(), w.getWidth(), wh));
        }
        return bounds;
    }

    private List<WidgetBounds> thirdPartyExclusionBounds(Screen screen) {
        List<WidgetBounds> bounds = new ArrayList<>();
        FtbLibrarySidebarCompat.sidebarBounds(screen).ifPresent(bounds::add);
        for (var plugin : AmiPluginRegistry.getPlugins()) {
            List<Rect2i> zones;
            try {
                zones = plugin.getExclusionZones(screen);
            } catch (RuntimeException | LinkageError ignored) {
                continue;
            }
            if (zones == null || zones.isEmpty()) continue;
            for (Rect2i zone : zones) {
                if (zone == null || zone.getWidth() <= 0 || zone.getHeight() <= 0) continue;
                bounds.add(new WidgetBounds(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight()));
            }
        }
        return bounds;
    }

    public void computeLayouts(AbstractContainerScreen<?> containerScreen, int screenW, int screenH) {
        ensureWidgets();
        activeSlots.clear();
        hideAllSlots();
        searchBarEmbedded = false;
        leftStripBounds = null;
        rightStripBounds = null;
        lastResultsBounds = null;
        lastRejectedPanelBounds.clear();
        lastScreenH = screenH;

        amiButton.updateBounds(new WidgetBounds(AMI_BTN_X, screenH - AMI_BTN_H - AMI_BTN_MARGIN, AMI_BTN_W, AMI_BTN_H));

        int usableH = screenH - BOTTOM_BAR_H - PANEL_MARGIN_V * 2;
        int panelH = Math.min(usableH, 600);
        int panelY = PANEL_MARGIN_V + (usableH - panelH) / 2;
        int panelBottom = panelY + panelH;

        int containerLeftEdge = containerScreen.getGuiLeft();
        int containerRightEdge = containerScreen.getGuiLeft() + containerScreen.getXSize();

        List<AmiConfig.PanelContent> leftContents = leftContents();
        List<AmiConfig.PanelContent> rightContents = rightContents();
        boolean leftTaken = false;
        boolean rightTaken = false;

        PlacedSlot leftPlacement = placeResponsiveSideSlots(containerScreen, leftContents, leftSlotPool,
                AmiConfig.leftPanelWidth, true, screenW, screenH, containerLeftEdge, containerRightEdge, panelY, panelBottom,
                false, false);
        if (leftPlacement != null) {
            leftTaken = leftPlacement.leftSide();
            rightTaken = !leftPlacement.leftSide();
            claimStrip(leftPlacement.leftSide(), leftPlacement.rect(), screenW, screenH);
            if (containsSearchContent(leftContents)) {
                lastResultsBounds = leftPlacement.rect().toWidgetBounds();
            }
        }

        int configuredRightWidth = AmiConfig.rightPanelWidth > 0
                ? AmiConfig.rightPanelWidth
                : net.minecraft.util.Mth.clamp((int) (screenW * 0.35f), MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
        PlacedSlot rightPlacement = placeResponsiveSideSlots(containerScreen, rightContents, rightSlotPool,
                configuredRightWidth, false, screenW, screenH, containerLeftEdge, containerRightEdge, panelY, panelBottom,
                leftTaken, rightTaken);
        if (rightPlacement != null) {
            claimStrip(rightPlacement.leftSide(), rightPlacement.rect(), screenW, screenH);
            if (containsSearchContent(rightContents)) {
                lastResultsBounds = rightPlacement.rect().toWidgetBounds();
                if (shouldEmbedSearchBar(rightContents, lastResultsBounds)) {
                    searchBar.updateBounds(UniversalResultsPanel.embeddedSearchBounds(lastResultsBounds));
                    searchBarEmbedded = true;
                }
            }
        }

        if (!searchBarEmbedded) {
            int maxBarRight = rightStripBounds != null ? rightStripBounds.x() - PANEL_MARGIN : (screenW - 4);
            int barW = Math.min(AmiConfig.searchBarWidth, screenW - 8);
            int barX = Math.max(4, (screenW - barW) / 2);
            if (barX + barW > maxBarRight) {
                barX = Math.max(4, maxBarRight - barW);
                barW = Math.max(60, Math.min(barW, maxBarRight - barX));
            }
            searchBar.updateBounds(new WidgetBounds(barX, screenH - BOTTOM_BAR_H + 2, barW, SEARCH_H));
        }

        lastThirdPartyMarginSignature = thirdPartyMarginSignature(containerScreen);
        rememberLayout(containerScreen, screenW, screenH);
    }

    private PlacedSlot placeResponsiveSideSlots(Screen screen, List<AmiConfig.PanelContent> contents, List<PanelSlot> pool,
                                                int preferredWidth, boolean preferLeft, int screenW, int screenH,
                                                int containerLeftEdge, int containerRightEdge, int panelY, int panelBottom,
                                                boolean leftTaken, boolean rightTaken) {
        if (contents.isEmpty()) return null;

        boolean[] sides = preferLeft ? new boolean[]{true, false} : new boolean[]{false, true};
        for (boolean leftSide : sides) {
            if (leftSide && leftTaken || !leftSide && rightTaken) continue;
            Rect slot = sideSlot(screen, contents, preferredWidth, leftSide, screenW, screenH,
                    containerLeftEdge, containerRightEdge, panelY, panelBottom);
            if (slot == null) continue;
            placeSideSlots(slot, contents, pool);
            rememberAdaptiveWidth(preferLeft, slot.w());
            return new PlacedSlot(leftSide, slot);
        }
        return null;
    }

    private Rect sideSlot(Screen screen, List<AmiConfig.PanelContent> contents, int preferredWidth, boolean leftSide,
                          int screenW, int screenH, int containerLeftEdge, int containerRightEdge,
                          int panelY, int panelBottom) {
        int stripX1 = leftSide ? 0 : containerRightEdge;
        int stripX2 = leftSide ? containerLeftEdge : screenW;
        int availableW = stripX2 - stripX1 - PANEL_MARGIN * 2;
        int minW = minWidthFor(contents);
        if (availableW < minW) {
            if (!containsSearchContent(contents)) {
                Rect forced = forcedSidebarSlot(screen, preferredWidth, leftSide, stripX1, stripX2, panelY, panelBottom);
                if (forced != null) {
                    return forced;
                }
            }
            rememberRejectedSlot(leftSide ? PANEL_MARGIN : stripX1 + PANEL_MARGIN,
                    panelY, Math.max(0, availableW), Math.max(0, panelBottom - panelY));
            return null;
        }

        int maxW = Math.min(availableW, MAX_PANEL_WIDTH);
        for (int width : widthAttempts(preferredWidth, minW, maxW, leftSide)) {
            Rect below = belowBlockersSlot(screen, width, leftSide, stripX1, stripX2, panelY, panelBottom);
            if (isAcceptableSideSlot(contents, below)) {
                return below;
            }

            Rect beside = besideBlockersSlot(screen, width, leftSide, stripX1, stripX2, panelY, panelBottom);
            if (isAcceptableSideSlot(contents, beside)) {
                return beside;
            }
        }

        if (!containsSearchContent(contents)) {
            Rect forced = forcedSidebarSlot(screen, preferredWidth, leftSide, stripX1, stripX2, panelY, panelBottom);
            if (forced != null) {
                return forced;
            }
        }

        rememberRejectedSlot(leftSide ? PANEL_MARGIN : screenW - maxW - PANEL_MARGIN,
                panelY, maxW, Math.max(0, panelBottom - panelY));
        return null;
    }

    private Rect forcedSidebarSlot(Screen screen, int preferredWidth, boolean leftSide, int stripX1, int stripX2,
                                   int panelY, int panelBottom) {
        int availableW = stripX2 - stripX1 - PANEL_MARGIN * 2;
        int availableH = panelBottom - panelY;
        if (availableW <= 0 || availableH <= 0) return null;

        int width = net.minecraft.util.Mth.clamp(preferredWidth, Math.min(MIN_SIDE_PANEL_WIDTH, availableW),
                Math.min(availableW, MAX_PANEL_WIDTH));
        int x = leftSide ? PANEL_MARGIN : stripX2 - width - PANEL_MARGIN;

        int minH = Math.min(MIN_SIDE_PANEL_HEIGHT, availableH);
        int thirdPartyBottom = thirdPartyWidgetBottomInStrip(screen, stripX1, stripX2, panelY, panelBottom);
        int y = thirdPartyBottom > panelY ? thirdPartyBottom + PANEL_MARGIN : panelY;
        if (panelBottom - y < minH) {
            y = Math.max(panelY, panelBottom - minH);
        }
        int height = Math.max(minH, panelBottom - y);
        return Rect.of(x, y, width, height);
    }

    private List<Integer> widthAttempts(int preferredWidth, int minW, int maxW, boolean leftSide) {
        List<Integer> attempts = new ArrayList<>();
        int start = net.minecraft.util.Mth.clamp(preferredWidth, minW, maxW);
        for (int percent = 100; percent >= WIDTH_SHRINK_STEP_PERCENT; percent -= WIDTH_SHRINK_STEP_PERCENT) {
            int width = net.minecraft.util.Mth.clamp(start * percent / 100, minW, maxW);
            if (!attempts.contains(width)) {
                attempts.add(width);
            }
            if (width == minW) break;
        }
        int cached = leftSide ? lastAdaptiveLeftPanelWidth : lastAdaptiveRightPanelWidth;
        if (cached >= minW && cached <= maxW && !attempts.contains(cached)) {
            attempts.add(cached);
        }
        if (!attempts.contains(minW)) attempts.add(minW);
        return attempts;
    }

    private void rememberAdaptiveWidth(boolean preferredLeftPanel, int width) {
        if (preferredLeftPanel) {
            lastAdaptiveLeftPanelWidth = width;
        } else {
            lastAdaptiveRightPanelWidth = width;
        }
    }

    private Rect belowBlockersSlot(Screen screen, int width, boolean leftSide, int stripX1, int stripX2,
                                  int panelY, int panelBottom) {
        int x = leftSide ? PANEL_MARGIN : stripX2 - width - PANEL_MARGIN;
        int thirdPartyBottom = thirdPartyWidgetBottomInStrip(screen, stripX1, stripX2, panelY, panelBottom);
        int y = thirdPartyBottom > panelY ? thirdPartyBottom + PANEL_MARGIN : panelY;
        int height = panelBottom - y;
        if (height <= 0) {
            rememberRejectedSlot(x, y, width, 0);
            return null;
        }
        return Rect.of(x, y, width, height);
    }

    private Rect besideBlockersSlot(Screen screen, int width, boolean leftSide, int stripX1, int stripX2,
                                   int panelY, int panelBottom) {
        int x;
        if (leftSide) {
            x = Math.max(PANEL_MARGIN, rightEdgeOfTopBlockers(screen, stripX1, stripX2, panelY, panelBottom) + PANEL_MARGIN);
            if (x + width + PANEL_MARGIN > stripX2) return null;
        } else {
            int leftEdge = leftEdgeOfTopBlockers(screen, stripX1, stripX2, panelY, panelBottom);
            x = leftEdge - PANEL_MARGIN - width;
            if (x < stripX1 + PANEL_MARGIN) return null;
        }
        int height = panelBottom - panelY;
        if (height <= 0) return null;
        return Rect.of(x, panelY, width, height);
    }

    private int rightEdgeOfTopBlockers(Screen screen, int stripX1, int stripX2, int panelY, int panelBottom) {
        int edge = stripX1;
        for (WidgetBounds bounds : topSideBlockers(screen, stripX1, stripX2, panelY, panelBottom)) {
            edge = Math.max(edge, bounds.x() + bounds.width());
        }
        return edge;
    }

    private int leftEdgeOfTopBlockers(Screen screen, int stripX1, int stripX2, int panelY, int panelBottom) {
        int edge = stripX2;
        for (WidgetBounds bounds : topSideBlockers(screen, stripX1, stripX2, panelY, panelBottom)) {
            edge = Math.min(edge, bounds.x());
        }
        return edge;
    }

    private List<WidgetBounds> topSideBlockers(Screen screen, int stripX1, int stripX2, int panelY, int panelBottom) {
        List<WidgetBounds> blockers = new ArrayList<>();
        List<WidgetBounds> all = new ArrayList<>();
        all.addAll(thirdPartyMarginWidgetBounds(screen));
        all.addAll(thirdPartyExclusionBounds(screen));
        for (WidgetBounds bounds : all) {
            if (bounds.x() >= stripX2 || bounds.x() + bounds.width() <= stripX1) continue;
            if (bounds.y() >= panelBottom || bounds.y() + bounds.height() <= panelY) continue;
            if (!isTopSideBlocker(bounds, panelY)) continue;
            blockers.add(bounds);
        }
        return blockers;
    }

    private boolean isTopSideBlocker(WidgetBounds bounds, int panelY) {
        return bounds.y() <= panelY + TOP_MARGIN_CONTROL_MAX_Y;
    }

    private void rememberRejectedSlot(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        lastRejectedPanelBounds.add(new WidgetBounds(x, y, width, height));
    }

    private void claimStrip(boolean leftSide, Rect slot, int screenW, int screenH) {
        WidgetBounds actualPanelBounds = slot.toWidgetBounds();
        if (leftSide) {
            leftStripBounds = slot.x() <= PANEL_MARGIN
                    ? new WidgetBounds(0, 0, slot.x() + slot.w(), screenH - BOTTOM_BAR_H)
                    : actualPanelBounds;
        } else {
            rightStripBounds = slot.x() + slot.w() >= screenW - PANEL_MARGIN
                    ? new WidgetBounds(slot.x(), 0, screenW - slot.x(), screenH - BOTTOM_BAR_H)
                    : actualPanelBounds;
        }
    }

    private int minWidthFor(List<AmiConfig.PanelContent> contents) {
        return containsSearchContent(contents) ? MIN_PANEL_WIDTH : MIN_SIDE_PANEL_WIDTH;
    }

    private boolean isAcceptableSideSlot(List<AmiConfig.PanelContent> contents, Rect slot) {
        if (slot == null || slot.w() <= 0 || slot.h() <= 0) return false;
        return !containsSearchContent(contents) || slot.h() >= MIN_SEARCH_PANEL_HEIGHT;
    }

    private boolean containsSearchContent(List<AmiConfig.PanelContent> contents) {
        for (AmiConfig.PanelContent content : contents) {
            if (isSearchContent(content)) return true;
        }
        return false;
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
                Rect leftSlot = Rect.of(PANEL_MARGIN, panelY, leftW, panelH);
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
                panelVisible,
                thirdPartyMarginSignature(screen),
                lastThirdPartyMarginSignature
        );
    }

    private boolean shouldEmbedSearchBar(List<AmiConfig.PanelContent> contents, WidgetBounds panelBounds) {
        return false;
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
        AmiRenderProfiler.beginFrame();
        try {
            try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("overlay.renderAll")) {
                com.sanhiruzu.ami.client.AMITheme.sync();
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
            }
        } catch (Exception e) {
            AMI.LOGGER.error("AMI overlay render failed", e);
        } finally {
            AmiRenderProfiler.endFrame();
        }

        if (pendingEmiReinit) {
            pendingEmiReinit = false;
            var mc = Minecraft.getInstance();
            if (mc.screen != null) mc.screen.init(mc, mc.screen.width, mc.screen.height);
        }
    }

    public void renderPanels(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        if (!panelVisible) return;
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("overlay.renderPanels")) {
            List<PanelSlot> renderingSlots = activeSlotsSnapshot();
            AmiRenderProfiler.add("overlay.panelSlots", renderingSlots.size());
            g.flush();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            g.pose().pushPose();
            g.pose().translate(0, 0, OverlayLayers.PANEL);

            for (PanelSlot slot : renderingSlots) {
                slot.render(g, mx, my, pt);
            }
            for (PanelSlot slot : renderingSlots) {
                slot.renderOverlay(g, mx, my);
            }
            renderCheatDeleteHint(g, mx, my, renderingSlots);

            if (AmiConfig.highlightExclusionAreas) {
                g.pose().pushPose();
                g.pose().translate(0, 0, OverlayLayers.DEBUG);
                renderExclusionHighlights(g, renderingSlots);
                g.pose().popPose();
            }

            g.pose().popPose();
            g.flush();
        }
    }

    public void renderSearchBar(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        if (!panelVisible || searchBar == null) return;
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("overlay.searchBar")) {
            g.flush();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            g.pose().pushPose();
            g.pose().translate(0, 0, OverlayLayers.PANEL + 1);
            searchBar.render(g, mx, my, pt);
            g.pose().popPose();
            g.flush();
        }
    }


    private List<PanelSlot> activeSlotsSnapshot() {
        return new ArrayList<>(activeSlots);
    }

    private void renderCheatDeleteHint(net.minecraft.client.gui.GuiGraphics g, int mx, int my, List<PanelSlot> slots) {
        if (!com.sanhiruzu.ami.client.AMICheatMode.isEnabled()) return;
        if (!com.sanhiruzu.ami.client.AMICheatMode.hasCarriedItem()) return;
        for (PanelSlot slot : slots) {
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

    private void renderExclusionHighlights(net.minecraft.client.gui.GuiGraphics g, List<PanelSlot> slots) {
        // Render panel bounds in blue (matching EMI's debug style)
        for (PanelSlot slot : slots) {
            if (slot.results.visible) {
                WidgetBounds b = slot.results.getBounds();
                g.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), 0x440000ff);
            }
            if (slot.sidebar.visible) {
                WidgetBounds b = slot.sidebar.getBounds();
                g.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), 0x440000ff);
            }
        }
        for (WidgetBounds b : lastRejectedPanelBounds) {
            g.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), 0x55ffaa00);
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


    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (PanelSlot slot : activeSlots) {
            if (slot.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return false;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            bounds.addAll(thirdPartyMarginWidgetBounds(mc.screen));
            bounds.addAll(thirdPartyExclusionBounds(mc.screen));
        }
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

    private record PlacedSlot(boolean leftSide, Rect rect) {
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

        boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            AbstractWidget widget = activeWidget();
            return widget != null && widget.visible && widget.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
