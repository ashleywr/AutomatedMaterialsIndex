package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiPluginRegistry;
import com.sanhiruzu.ami.client.AMICheatMode;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import com.sanhiruzu.ami.client.AmiRenderProfiler;
import com.sanhiruzu.ami.client.AmiRenderPhase;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.UniversalResultsPanel;
import com.sanhiruzu.ami.client.entitydetails.EntityDetailsQuery;
import com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.client.screen.AmiConfigScreen;
import com.sanhiruzu.ami.client.sources.ItemSourceQuery;
import com.sanhiruzu.ami.compat.FtbLibrarySidebarCompat;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

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
    public static final int AMI_BTN_NEXT_X = AMI_BTN_X + AMI_BTN_W + AMI_BTN_MARGIN;
    private static final int MAX_MARGIN_CONTROL_H = 32;
    private static final int TOP_MARGIN_CONTROL_MAX_Y = 96;
    private static final int MIN_SIDE_PANEL_HEIGHT = 24;
    private static final int MIN_SIDE_PANEL_WIDTH = 24;
    private static final int MIN_SEARCH_PANEL_HEIGHT = 80;
    private static final int WIDTH_SHRINK_STEP_PERCENT = 10;
    private static final float LEFT_PANEL_AUTO_WIDTH_RATIO = 0.22f;
    private static final float RIGHT_PANEL_AUTO_WIDTH_RATIO = 0.35f;
    private static final int PANEL_HANDLE_HITBOX = 8;
    private static final int LEFT_PANEL_BAR_H = 20;
    private static final int BAR_EXPAND_BTN_W = 20;
    private static final int BAR_EXPAND_BTN_H = 14;
    private static final int MIN_BAR_W = 40;
    private static final int BAR_ICON_CELL = 18;
    private static final long SEARCH_DEBOUNCE_MS = Math.max(0L, Long.getLong("ami.searchDebounceMs", 120L));
    private final List<PanelSlot> leftSlotPool = new ArrayList<>();
    private final List<PanelSlot> rightSlotPool = new ArrayList<>();
    private final List<PanelSlot> activeSlots = new ArrayList<>();
    private final InventorySearchHighlighter inventorySearchHighlighter = new InventorySearchHighlighter();
    private final List<WidgetBounds> lastRejectedPanelBounds = new ArrayList<>();
    private final java.util.List<PanelDragLayout> panelDragLayout = new java.util.ArrayList<>();
    private SearchBarWidget searchBar;
    private AmiButtonWidget amiButton;
    private boolean widgetsReady = false;
    private boolean panelVisible = false;
    private String lastSyncedQuery = "";
    private String pendingSearchQuery = null;
    private long pendingSearchDeadlineMs = -1L;
    private WidgetBounds lastResultsBounds = null;
    private int lastScreenH = 0;
    private boolean pendingEmiReinit = false;
    private boolean leftAlternateActive = false;
    private boolean rightAlternateActive = false;
    private boolean leftPanelCollapsed = false;
    private WidgetBounds leftPanelBarBounds = null;
    private WidgetBounds leftPanelExpandBtnBounds = null;
    private WidgetBounds leftStripBounds = null;
    private WidgetBounds rightStripBounds = null;
    private boolean searchBarEmbedded = false;
    private int lastLayoutSignature = Integer.MIN_VALUE;
    private boolean layoutDirty = true;
    private int lastThirdPartyMarginSignature = 0;
    private int lastAdaptiveLeftPanelWidth = -1;
    private int lastAdaptiveRightPanelWidth = -1;
    // Layout mode & dragging
    private boolean inLayoutMode = false;
    private String draggedWidgetId = null;
    private double dragStartMouseX, dragStartMouseY;
    private int dragOriginX, dragOriginY, dragOriginW, dragOriginH;
    private PanelDragHandle panelDragHandle = PanelDragHandle.NONE;
    private PinnedWidgetPositions pinnedPositions = new PinnedWidgetPositions();
    private String layoutModePositionSnapshot;
    private boolean panelVisibleBeforeLayoutMode;
    private net.minecraft.client.gui.components.Button layoutDoneButton;
    private net.minecraft.client.gui.components.Button layoutResetButton;
    private int lastLayoutButtonScreenW = -1;
    private int lastLayoutButtonScreenH = -1;

    public OverlayWidgetManager() {
    }

    private void ensureWidgets() {
        if (widgetsReady) return;
        this.searchBar = new SearchBarWidget(new AbstractSearchBarWidget.Listener() {
            @Override
            public void onQueryChanged(String query) {
                triggerSearch(query);
            }

            @Override
            public void onSearchBarDoubleClicked(String query) {
                inventorySearchHighlighter.toggle(query);
                searchBar.setInventoryVisualFilterActive(inventorySearchHighlighter.isActive());
            }

            @Override
            public void onSearchBarCleared() {
                inventorySearchHighlighter.updateQuery("");
                searchBar.setInventoryVisualFilterActive(inventorySearchHighlighter.isActive());
            }
        });
        this.amiButton = new AmiButtonWidget(() -> {
            var mc = Minecraft.getInstance();
            mc.setScreen(new AmiConfigScreen(mc.screen));
        }, InventoryOverlayHandler::toggleAmi, () -> panelVisible);

        AmiFavoritesHandler.getInstance().setOnChange(this::refreshSidebarsAndLayout);

        pinnedPositions = PinnedWidgetPositions.load();
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
        panel.setOnQueryReplace(query -> {
            searchBar.setQuery(query);
            applySearchQuery(query);
        });
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

        int containerLeftEdge = containerScreen.leftPos;
        int containerRightEdge = containerScreen.leftPos + containerScreen.imageWidth;

        int recipeBookW = computeRecipeBookWidth(containerScreen, screenW);
        int adjustedContainerLeftEdge = containerLeftEdge - recipeBookW;

        int leftPanelY = pinnedPanelY("left_panel", panelY, panelBottom);
        int rightPanelY = pinnedPanelY("right_panel", panelY, panelBottom);

        List<AmiConfig.PanelContent> leftContents = leftContents();
        List<AmiConfig.PanelContent> rightContents = rightContents();
        boolean leftTaken = false;
        boolean rightTaken = false;

        leftPanelBarBounds = null;
        leftPanelExpandBtnBounds = null;

        PlacedSlot leftPlacement = null;
        if (!leftPanelCollapsed) {
            int configuredLeftWidth = configuredPanelWidth("left_panel", AmiConfig.leftPanelWidth, screenW);
            leftPlacement = placeResponsiveSideSlots(containerScreen, leftContents, leftSlotPool,
                    "left_panel", configuredLeftWidth, true, screenW, screenH,
                    adjustedContainerLeftEdge, containerRightEdge, leftPanelY, panelBottom,
                    false, false);
        }
        if (leftPlacement != null) {
            leftTaken = leftPlacement.leftSide();
            rightTaken = !leftPlacement.leftSide();
            claimStrip(leftPlacement.leftSide(), leftPlacement.rect(), screenW, screenH);
            if (containsSearchContent(leftContents)) {
                lastResultsBounds = leftPlacement.rect().toWidgetBounds();
            }
        } else if (!leftContents.isEmpty()) {
            int barW = adjustedContainerLeftEdge - PANEL_MARGIN * 2;
            if (barW >= MIN_BAR_W) {
                int barX = PANEL_MARGIN;
                int barY = leftPanelY;
                leftPanelBarBounds = new WidgetBounds(barX, barY, barW, LEFT_PANEL_BAR_H);
                int btnX = barX + barW - BAR_EXPAND_BTN_W - 2;
                int btnY = barY + (LEFT_PANEL_BAR_H - BAR_EXPAND_BTN_H) / 2;
                leftPanelExpandBtnBounds = new WidgetBounds(btnX, btnY, BAR_EXPAND_BTN_W, BAR_EXPAND_BTN_H);
                claimStrip(true, Rect.of(barX, barY, barW, LEFT_PANEL_BAR_H), screenW, screenH);
            }
        }

        int configuredRightWidth = configuredPanelWidth("right_panel", AmiConfig.rightPanelWidth, screenW);
        PlacedSlot rightPlacement = placeResponsiveSideSlots(containerScreen, rightContents, rightSlotPool,
                "right_panel", configuredRightWidth, false, screenW, screenH, containerLeftEdge, containerRightEdge, rightPanelY, panelBottom,
                leftTaken, rightTaken);
        if (rightPlacement != null) {
            claimStrip(rightPlacement.leftSide(), rightPlacement.rect(), screenW, screenH);
            if (containsSearchContent(rightContents)) {
                lastResultsBounds = rightPlacement.rect().toWidgetBounds();
            }
        }

        if (!searchBarEmbedded) {
            int maxBarRight = rightStripBounds != null ? rightStripBounds.x() - PANEL_MARGIN : (screenW - 4);
            int barW = Math.min(AmiConfig.searchBarWidth, screenW - 8);
            int barX = Math.max(4, (screenW - barW) / 2);
            int barY = screenH - BOTTOM_BAR_H + 2;
            if (barX + barW > maxBarRight) {
                barX = Math.max(4, maxBarRight - barW);
                barW = Math.max(60, Math.min(barW, maxBarRight - barX));
            }
            // Apply pinned position if set
            PinnedWidgetPositions.Position pinnedPos = pinnedPositions.get("search_bar", barX, barY);
            barX = pinnedPos.getX(barX);
            barY = pinnedPos.getY(barY);
            searchBar.updateBounds(new WidgetBounds(barX, barY, barW, SEARCH_H));
        }

        lastThirdPartyMarginSignature = thirdPartyMarginSignature(containerScreen);
        rememberLayout(containerScreen, screenW, screenH);
    }

    private PlacedSlot placeResponsiveSideSlots(Screen screen, List<AmiConfig.PanelContent> contents, List<PanelSlot> pool,
                                                String panelId, int preferredWidth, boolean preferLeft, int screenW, int screenH,
                                                int containerLeftEdge, int containerRightEdge, int panelY, int panelBottom,
                                                boolean leftTaken, boolean rightTaken) {
        if (contents.isEmpty()) return null;

        boolean[] sides = preferLeft ? new boolean[]{true, false} : new boolean[]{false, true};
        for (boolean leftSide : sides) {
            if (leftSide && leftTaken || !leftSide && rightTaken) continue;
            Rect slot = sideSlot(screen, contents, preferredWidth, leftSide, screenW, screenH,
                    containerLeftEdge, containerRightEdge, panelY, panelBottom);
            if (slot == null) continue;
            slot = pinnedPanelSlot(panelId, slot, screenW, screenH);
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

    private int configuredPanelWidth(String panelId, int configuredWidth, int screenW) {
        if (configuredWidth > 0) {
            return configuredWidth;
        }
        float ratio = "left_panel".equals(panelId) ? LEFT_PANEL_AUTO_WIDTH_RATIO : RIGHT_PANEL_AUTO_WIDTH_RATIO;
        return net.minecraft.util.Mth.clamp((int) (screenW * ratio), MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
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
        int panelH = Math.max(0, Math.min(usableH, 600));
        int panelY = PANEL_MARGIN_V + (usableH - panelH) / 2;
        int panelBottom = panelY + panelH;
        int centerLeft = Math.max(PANEL_MARGIN, (screenW - Math.max(screenW / 3, 200)) / 2);
        int centerRight = Math.min(screenW - PANEL_MARGIN,
                centerLeft + Math.max(screenW / 3, 200));

        RecipeViewerBridge.RecipeViewerBounds viewerBounds = RecipeViewerBridge.getActiveRecipeViewerBounds();
        boolean hasRecipeViewerBounds = viewerBounds != null && viewerBounds.isValid();
        if (hasRecipeViewerBounds) {
            centerLeft = Math.max(PANEL_MARGIN, viewerBounds.left());
            centerRight = Math.min(screenW - PANEL_MARGIN, viewerBounds.right() + PANEL_MARGIN);
            if (centerRight <= centerLeft) {
                hasRecipeViewerBounds = false;
            }
        }
        if (!hasRecipeViewerBounds) {
            centerLeft = Math.max(PANEL_MARGIN, (screenW - Math.max(screenW / 3, 200)) / 2);
            centerRight = Math.min(screenW - PANEL_MARGIN, centerLeft + Math.max(screenW / 3, 200));
        }

        List<AmiConfig.PanelContent> leftContents = leftContents();
        if (!leftContents.isEmpty()) {
            int configuredLeftWidth = configuredPanelWidth("left_panel", AmiConfig.leftPanelWidth, screenW);
            int leftW = Math.min(configuredLeftWidth, centerLeft - PANEL_MARGIN * 2);
            if (leftW >= 40) {
                int leftStartY = pinnedPanelY("left_panel", panelY, panelBottom);
                Rect leftSlot = Rect.of(PANEL_MARGIN, leftStartY, leftW, panelBottom - leftStartY);
                WidgetBounds leftPinned = pinnedPanelWindowBounds("left_panel",
                        new WidgetBounds(leftSlot.x(), leftSlot.y(), leftSlot.w(), leftSlot.h()), screenW, screenH);
                leftSlot = Rect.of(leftPinned.x(), leftPinned.y(), leftPinned.width(), leftPinned.height());
                placeSideSlots(leftSlot, leftContents, leftSlotPool);
                if (containsSearchContent(leftContents)) {
                    lastResultsBounds = leftSlot.toWidgetBounds();
                }
                leftStripBounds = new WidgetBounds(0, 0, leftSlot.x() + leftSlot.w(), screenH - BOTTOM_BAR_H);
            }
        } else {
            leftStripBounds = null;
        }

        int safeWidth = screenW - centerRight - PANEL_MARGIN * 2;
        int panelStartX = screenW;
        if (safeWidth >= MIN_PANEL_WIDTH) {
            int configuredRightWidth = configuredPanelWidth("right_panel", AmiConfig.rightPanelWidth, screenW);
            int rw = net.minecraft.util.Mth.clamp(configuredRightWidth, MIN_PANEL_WIDTH, Math.min(safeWidth, MAX_PANEL_WIDTH));
            panelStartX = screenW - rw - PANEL_MARGIN;
            int rightStartY = pinnedPanelY("right_panel", panelY, panelBottom);
            int rightH = panelBottom - rightStartY;
            Rect rightSlotFallback = Rect.of(panelStartX, rightStartY, rw, rightH);
            WidgetBounds rightPinned = pinnedPanelWindowBounds("right_panel",
                    new WidgetBounds(rightSlotFallback.x(), rightSlotFallback.y(), rightSlotFallback.w(), rightSlotFallback.h()),
                    screenW, screenH);
            Rect rightSlot = Rect.of(rightPinned.x(), rightPinned.y(), rightPinned.width(), rightPinned.height());

            List<AmiConfig.PanelContent> rightContents = rightContents();
            placeSideSlots(rightSlot, rightContents, rightSlotPool);
            if (containsSearchContent(rightContents)) {
                lastResultsBounds = rightSlot.toWidgetBounds();
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
            int barY = screenH - BOTTOM_BAR_H + 2;
            if (barX + barW > maxBarRight) {
                barX = Math.max(4, maxBarRight - barW);
                barW = Math.max(60, Math.min(barW, maxBarRight - barX));
            }
            // Apply pinned position if set
            PinnedWidgetPositions.Position pinnedPos = pinnedPositions.get("search_bar", barX, barY);
            barX = pinnedPos.getX(barX);
            barY = pinnedPos.getY(barY);
            searchBar.updateBounds(new WidgetBounds(barX, barY, barW, SEARCH_H));
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
        RecipeViewerBridge.RecipeViewerBounds viewerBounds = RecipeViewerBridge.getActiveRecipeViewerBounds();
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            guiLeft = containerScreen.leftPos;
            guiTop = containerScreen.topPos;
            guiWidth = containerScreen.imageWidth;
            guiHeight = containerScreen.imageHeight;
        }
        return Objects.hash(
                System.identityHashCode(screen),
                screenW,
                screenH,
                guiLeft,
                guiTop,
                guiWidth,
                guiHeight,
                viewerBounds.x,
                viewerBounds.y,
                viewerBounds.width,
                viewerBounds.height,
                AmiConfig.leftPanelWidth,
                AmiConfig.rightPanelWidth,
                AmiConfig.leftPanelSlots,
                AmiConfig.leftPanelAlternateSlots,
                AmiConfig.rightPanelSlots,
                AmiConfig.rightPanelAlternateSlots,
                leftAlternateActive,
                rightAlternateActive,
                panelVisible,
                leftPanelCollapsed,
                AmiConfig.pinnedPositionsJson,
                thirdPartyMarginSignature(screen),
                lastThirdPartyMarginSignature
        );
    }

    private boolean shouldEmbedSearchBar(String panelId, List<AmiConfig.PanelContent> contents, WidgetBounds panelBounds) {
        if (panelBounds == null || panelBounds.width() <= 0 || panelBounds.height() <= 0) {
            return false;
        }
        if (!"left_panel".equals(panelId)) {
            return false;
        }
        if (!containsSearchContent(contents)) {
            return false;
        }
        return panelBounds.height() >= MIN_SEARCH_PANEL_HEIGHT
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

    private void toggleLeftPanelCollapsed() {
        leftPanelCollapsed = !leftPanelCollapsed;
        invalidateLayout();
    }

    private int computeRecipeBookWidth(AbstractContainerScreen<?> screen, int screenW) {
        if (AmiConfig.recipeBookAction != AmiConfig.RecipeBookAction.OPEN_VANILLA_BOOK) return 0;
        if (!(screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener rl)) return 0;
        if (!rl.getRecipeBookComponent().isVisible()) return 0;
        int naturalLeft = (screenW - screen.imageWidth) / 2;
        return Math.max(0, screen.leftPos - naturalLeft);
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

    public void tick() {
        if (!panelVisible) return;

        ensureWidgets();
        flushPendingSearch(false);
        if (!AmiConfig.enableAutoIndexing) return;

        var indexer = AmiIndexerService.getInstance();
        if (indexer.isReady()) {
            var service = indexer.getOrBuildSearchService();
            long searchRevision = indexer.searchServiceRevision();
            long runtimeRevision = indexer.runtimeSearchRevision();
            boolean needsRefresh = false;
            for (ResultsPanelWidget panel : getResultPanels()) {
                var inner = panel.getInnerPanel();
                if (inner != null && indexer.indexedItemCount() > 0
                        && inner.setSearchServiceIfChanged(service, searchRevision)) {
                    needsRefresh = true;
                    continue;
                }
                if (inner != null) {
                    inner.setRuntimeSearchRevisionIfChanged(runtimeRevision);
                }
            }
            if (needsRefresh) refreshEntries();
        }

        syncFromRecipeViewer();

        refreshSidebars();
    }

    public void renderAll(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        AmiRenderProfiler.beginFrame();
        try {
            try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("overlay.renderAll")) {
                renderBase(g, mx, my, pt);
                renderTopLayer(g, mx, my);
            }
        } catch (Exception e) {
            AmiCore.LOGGER.error("AMI overlay render failed", e);
        } finally {
            AmiRenderProfiler.endFrame();
        }

        if (pendingEmiReinit) {
            pendingEmiReinit = false;
            var mc = Minecraft.getInstance();
            if (mc.screen != null) mc.screen.init(mc, mc.screen.width, mc.screen.height);
        }
    }

    /**
     * Renders AMI's durable overlay body: buttons, panels, result grids, item icons,
     * search bars, and layout UI. Container screens use the same post-render stack
     * as JEI; AMI-owned tooltips are drawn afterward by renderTopLayer().
     */
    public void renderBase(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        try (AmiRenderPhase.Scope phase = AmiRenderPhase.enter(AmiRenderPhase.Phase.BASE);
             AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("overlay.renderBase")) {
            AMITheme.sync();
            g.flush();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            g.pose().pushPose();
            g.pose().translate(0, 0, OverlayLayers.SCREEN);

            amiButton.render(g, mx, my, pt);

            if (panelVisible) {
                Screen screen = Minecraft.getInstance().screen;
                if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                    inventorySearchHighlighter.render(containerScreen, g);
                }
                renderPanels(g, mx, my, pt);
                renderSearchBar(g, mx, my, pt);
            }

            if (inLayoutMode) {
                g.pose().pushPose();
                g.pose().translate(0, 0, OverlayLayers.LAYOUT_MODE);
                renderLayoutMode(g, mx, my, pt);
                g.pose().popPose();
            }

            g.pose().popPose();
            g.flush();

            // AMI result icons are drawn via g.renderItem, which writes real 3D model depth;
            // block models reach past the vanilla tooltip's z (~400). Clear the base layer's
            // depth residue so the vanilla/status tooltip drawn after the container foreground
            // is never occluded by a result icon. Upholds IconRenderState's "no depth residue"
            // promise at the whole-layer level instead of relying on a 3D icon being visible.
            com.mojang.blaze3d.systems.RenderSystem.clear(
                    org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        }
    }

    /**
     * Renders only AMI-owned transient UI: AMI tooltips, dropdowns, context menus,
     * and hints. This phase must not render result-panel bodies or result icons.
     */
    public void renderTopLayer(net.minecraft.client.gui.GuiGraphics g, int mx, int my) {
        try (AmiRenderPhase.Scope phase = AmiRenderPhase.enter(AmiRenderPhase.Phase.TOP);
             AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("overlay.renderTopLayer")) {
            g.flush();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            g.pose().pushPose();

            amiButton.renderTooltip(g, mx, my);
            if (panelVisible) {
                List<PanelSlot> renderingSlots = activeSlotsSnapshot();
                for (PanelSlot slot : renderingSlots) {
                    slot.renderOverlay(g, mx, my);
                }
                renderCheatDeleteHint(g, mx, my, renderingSlots);
            }

            g.pose().popPose();
            g.flush();
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
            if (leftPanelBarBounds != null) {
                renderLeftPanelBar(g, mx, my);
            }

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
            g.pose().translate(0, 0, OverlayLayers.SEARCH_BAR);
            searchBar.render(g, mx, my, pt);
            g.pose().popPose();
            g.flush();
        }
    }

    private List<PanelSlot> activeSlotsSnapshot() {
        return new ArrayList<>(activeSlots);
    }

    private void renderCheatDeleteHint(net.minecraft.client.gui.GuiGraphics g, int mx, int my, List<PanelSlot> slots) {
        if (!AMICheatMode.isEnabled()) return;
        if (!AMICheatMode.hasCarriedItem()) return;
        for (PanelSlot slot : slots) {
            if (slot.results.visible && slot.results.isMouseOver(mx, my)) {
                var font = net.minecraft.client.Minecraft.getInstance().font;
                var msg = net.minecraft.network.chat.Component.translatable("ami.cheat.drop_to_delete");
                g.renderTooltip(font, List.of(msg), java.util.Optional.empty(), mx, my);
                break;
            }
        }
    }

    private void renderLeftPanelBar(net.minecraft.client.gui.GuiGraphics g, int mx, int my) {
        int bx = leftPanelBarBounds.x(), by = leftPanelBarBounds.y();
        int bw = leftPanelBarBounds.width(), bh = leftPanelBarBounds.height();
        AMITheme.fillPanelChrome(g, bx, by, bw, bh);

        List<AmiConfig.PanelContent> conts = leftContents();
        if (!conts.isEmpty()) {
            List<SearchNode> nodes =
                    AmiSidebarSyncHandler.getNodesForContent(conts.get(0));
            int btnRight = leftPanelExpandBtnBounds != null ? leftPanelExpandBtnBounds.x() : bx + bw;
            int iconAreaW = btnRight - bx - 4;
            int maxIcons = Math.max(0, iconAreaW / BAR_ICON_CELL);
            int count = Math.min(nodes.size(), maxIcons);
            int iconX = bx + 4;
            int iconY = by + (bh - AMITheme.ICON_SIZE) / 2;
            for (int i = 0; i < count; i++) {
                net.minecraft.world.item.ItemStack stack =
                        ItemIconRenderer.resolveStack(nodes.get(i).id());
                if (!stack.isEmpty()) {
                    g.renderItem(stack, iconX + i * BAR_ICON_CELL, iconY);
                }
            }
        }

        if (leftPanelExpandBtnBounds != null) {
            int ebx = leftPanelExpandBtnBounds.x(), eby = leftPanelExpandBtnBounds.y();
            int ebw = leftPanelExpandBtnBounds.width(), ebh = leftPanelExpandBtnBounds.height();
            boolean hov = mx >= ebx && mx < ebx + ebw && my >= eby && my < eby + ebh;
            int bg = hov ? AMITheme.DROPDOWN_BG_ACTIVE
                         : AMITheme.DROPDOWN_BG;
            AMITheme.fillControlChrome(g, ebx, eby, ebw, ebh, bg, false);
            int ic = hov ? AMITheme.TEXT_HEADER
                         : AMITheme.TEXT_SUBTLE;
            AmiGuiIcons.sidebarExpand(g, ebx + ebw / 2, eby + ebh / 2, ic);
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

    public void refreshEntriesForRuntimeIndexUpdate() {
        List<SearchNode> all = new ArrayList<>();
        for (NodeType t : NodeType.atlasValues()) all.addAll(GlobalIndex.getInstance().getNodes(t));
        for (ResultsPanelWidget panel : getResultPanels()) {
            if (panel.getInnerPanel() != null) panel.getInnerPanel().setEntries(all, true);
        }
        refreshSidebars();
    }

    public void refreshSidebars() {
        for (SidebarPanelWidget panel : getSidebarPanels()) {
            panel.refresh();
        }
    }

    private void triggerSearch(String query) {
        inventorySearchHighlighter.updateQuery(query);
        if (searchBar != null) {
            searchBar.setInventoryVisualFilterActive(inventorySearchHighlighter.isActive());
        }
        if (SEARCH_DEBOUNCE_MS <= 0L || query == null || query.isBlank()
                || ItemSourceQuery.isRoute(query)
                || EntityDetailsQuery.isRoute(query)) {
            pendingSearchQuery = null;
            pendingSearchDeadlineMs = -1L;
            applySearchQuery(query == null ? "" : query);
            return;
        }
        pendingSearchQuery = query;
        pendingSearchDeadlineMs = System.currentTimeMillis() + SEARCH_DEBOUNCE_MS;
    }

    private void flushPendingSearch(boolean force) {
        if (pendingSearchQuery == null) {
            return;
        }
        if (!force && System.currentTimeMillis() < pendingSearchDeadlineMs) {
            return;
        }
        String query = pendingSearchQuery;
        pendingSearchQuery = null;
        pendingSearchDeadlineMs = -1L;
        applySearchQuery(query);
    }

    private void applySearchQuery(String query) {
        query = query == null ? "" : query;
        for (ResultsPanelWidget panel : getResultPanels()) {
            if (panel.getInnerPanel() != null) panel.getInnerPanel().getState().setQuery(query);
        }
        if (RecipeViewerBridge.supportsSearchSync()
                && !ItemSourceQuery.isRoute(query)
                && !EntityDetailsQuery.isRoute(query)
                && !query.equals(lastSyncedQuery)) {
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
            pendingSearchQuery = null;
            pendingSearchDeadlineMs = -1L;
            searchBar.setQuery(rvQuery);
            for (ResultsPanelWidget panel : getResultPanels()) {
                if (panel.getInnerPanel() != null) panel.getInnerPanel().getState().setQuery(rvQuery);
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inLayoutMode) return false;
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (leftPanelBarBounds != null && leftPanelExpandBtnBounds != null && button == 0) {
            var btn = leftPanelExpandBtnBounds;
            if (mouseX >= btn.x() && mouseX < btn.x() + btn.width()
                    && mouseY >= btn.y() && mouseY < btn.y() + btn.height()) {
                leftPanelCollapsed = false;
                invalidateLayout();
                return true;
            }
        }
        if (inLayoutMode) {
            Minecraft mc = Minecraft.getInstance();
            ensureLayoutButtons(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            if (layoutDoneButton != null && layoutDoneButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (layoutResetButton != null && layoutResetButton.mouseClicked(mouseX, mouseY, button)) return true;
            return tryStartDrag(mouseX, mouseY, button);
        }
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (inLayoutMode) {
            return updateDrag(mouseX, mouseY);
        }
        if (updateDrag(mouseX, mouseY)) return true;
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            finalizeDrag(screen);
        }
        if (inLayoutMode) {
            return;
        }
        for (PanelSlot slot : activeSlotsSnapshot()) {
            slot.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inLayoutMode && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            cancelLayout();
            return true;
        }
        if (inLayoutMode) return true;
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }

    public boolean hasOpenContextMenu() {
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.hasOpenContextMenu()) return true;
        }
        return false;
    }

    public boolean isMouseOverPanel(double mouseX, double mouseY) {
        if (leftPanelBarBounds != null && leftPanelBarBounds.contains(mouseX, mouseY)) return true;
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.isMouseOver(mouseX, mouseY)) return true;
        }
        return false;
    }

    public SearchNode getHoveredNode() {
        for (PanelSlot slot : activeSlotsSnapshot()) {
            SearchNode node = slot.getHoveredNode();
            if (node != null) return node;
        }
        return null;
    }

    public SidebarPanelWidget getFavoritesPanelAt(double mouseX, double mouseY) {
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.sidebar.visible
                    && slot.sidebar.getContentType() == AmiConfig.PanelContent.FAVORITES
                    && slot.sidebar.isMouseOver(mouseX, mouseY)) {
                return slot.sidebar;
            }
        }
        return null;
    }

    public boolean hasVisibleFavoritesPanel() {
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.sidebar.visible && slot.sidebar.getContentType() == AmiConfig.PanelContent.FAVORITES) {
                return true;
            }
        }
        return false;
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
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.results.visible && slot.results.getInnerPanel() != null) {
                panels.add(slot.results.getInnerPanel());
            }
            if (slot.sidebar.visible && slot.sidebar.getInnerPanel() != null) {
                panels.add(slot.sidebar.getInnerPanel());
            }
        }
        return panels;
    }

    private int pinnedPanelY(String panelId, int defaultY, int panelBottom) {
        PinnedWidgetPositions.Position pos = pinnedPositions.get(panelId, Integer.MIN_VALUE, Integer.MIN_VALUE);
        if (!pos.isPinned()) return defaultY;
        return net.minecraft.util.Mth.clamp(pos.y, defaultY, Math.max(defaultY, panelBottom - 40));
    }

    private WidgetBounds pinnedPanelWindowBounds(String panelId, WidgetBounds fallback, int screenW, int screenH) {
        PinnedWidgetPositions.Position pos = pinnedPositions.get(panelId, fallback.x(), fallback.y(), fallback.width(), fallback.height());
        int minW = Math.max(MIN_PANEL_WIDTH, 1);
        int minH = Math.max(MIN_SIDE_PANEL_HEIGHT, 1);
        int maxW = Math.max(minW, screenW - 4);
        int maxH = Math.max(minH, screenH - 4);
        int width = net.minecraft.util.Mth.clamp(pos.getW(fallback.width()), minW, maxW);
        int height = net.minecraft.util.Mth.clamp(pos.getH(fallback.height()), minH, maxH);
        int x = net.minecraft.util.Mth.clamp(pos.getX(fallback.x()), 0, Math.max(0, screenW - width));
        int y = net.minecraft.util.Mth.clamp(pos.getY(fallback.y()), 0, Math.max(0, screenH - height));
        return new WidgetBounds(x, y, width, height);
    }

    private Rect pinnedPanelSlot(String panelId, Rect fallback, int screenW, int screenH) {
        WidgetBounds pinned = pinnedPanelWindowBounds(panelId,
                new WidgetBounds(fallback.x(), fallback.y(), fallback.w(), fallback.h()), screenW, screenH);
        return Rect.of(pinned.x(), pinned.y(), pinned.width(), pinned.height());
    }

    private List<PanelSlot> panelSlotsFor(String panelId) {
        return "left_panel".equals(panelId) ? leftSlotPool : rightSlotPool;
    }

    private WidgetBounds panelWindowBounds(String panelId) {
        return panelWindowBounds(panelSlotsFor(panelId));
    }

    private WidgetBounds panelWindowBounds(List<PanelSlot> pool) {
        WidgetBounds bounds = null;
        for (PanelSlot slot : pool) {
            AbstractWidget w = slot.visibleWidget();
            if (w == null || !w.visible) continue;
            if (bounds == null) {
                bounds = new WidgetBounds(w.getX(), w.getY(), w.getWidth(), w.getHeight());
            } else {
                int x1 = Math.min(bounds.x(), w.getX());
                int y1 = Math.min(bounds.y(), w.getY());
                int x2 = Math.max(bounds.x() + bounds.width(), w.getX() + w.getWidth());
                int y2 = Math.max(bounds.y() + bounds.height(), w.getY() + w.getHeight());
                bounds = new WidgetBounds(x1, y1, x2 - x1, y2 - y1);
            }
        }
        return bounds;
    }

    private PanelDragHandle resolvePanelDragHandle(WidgetBounds bounds, double mouseX, double mouseY) {
        if (bounds == null || !bounds.contains(mouseX, mouseY)) {
            return PanelDragHandle.NONE;
        }

        int x1 = bounds.x();
        int y1 = bounds.y();
        int x2 = bounds.x() + bounds.width();
        int y2 = bounds.y() + bounds.height();
        int leftEdge = x1 + PANEL_HANDLE_HITBOX;
        int rightEdge = x2 - PANEL_HANDLE_HITBOX;
        int topEdge = y1 + PANEL_HANDLE_HITBOX;
        int bottomEdge = y2 - PANEL_HANDLE_HITBOX;
        boolean onLeft = mouseX >= x1 && mouseX < Math.min(leftEdge, x2);
        boolean onRight = mouseX >= Math.max(rightEdge, x1) && mouseX < x2;
        boolean onTop = mouseY >= y1 && mouseY < Math.min(topEdge, y2);
        boolean onBottom = mouseY >= Math.max(bottomEdge, y1) && mouseY < y2;

        if (onLeft && onTop) return PanelDragHandle.RESIZE_TOP_LEFT;
        if (onRight && onTop) return PanelDragHandle.RESIZE_TOP_RIGHT;
        if (onLeft && onBottom) return PanelDragHandle.RESIZE_BOTTOM_LEFT;
        if (onRight && onBottom) return PanelDragHandle.RESIZE_BOTTOM_RIGHT;
        if (onLeft) return PanelDragHandle.RESIZE_LEFT;
        if (onRight) return PanelDragHandle.RESIZE_RIGHT;
        if (onTop) return PanelDragHandle.RESIZE_TOP;
        if (onBottom) return PanelDragHandle.RESIZE_BOTTOM;
        return PanelDragHandle.MOVE;
    }

    private void capturePanelDragLayout(String panelId, WidgetBounds startBounds) {
        panelDragLayout.clear();
        if (startBounds == null || startBounds.width() <= 0 || startBounds.height() <= 0) return;

        for (PanelSlot slot : panelSlotsFor(panelId)) {
            AbstractWidget w = slot.visibleWidget();
            if (w == null || !w.visible) continue;
            float relX = (w.getX() - (float) startBounds.x()) / startBounds.width();
            float relY = (w.getY() - (float) startBounds.y()) / startBounds.height();
            float relW = (float) w.getWidth() / (float) startBounds.width();
            float relH = (float) w.getHeight() / (float) startBounds.height();
            panelDragLayout.add(new PanelDragLayout(slot, relX, relY, relW, relH));
        }
    }

    private void applyPanelWindowLayout(String panelId, WidgetBounds windowBounds) {
        if (windowBounds == null || windowBounds.width() <= 0 || windowBounds.height() <= 0) return;
        if (panelDragLayout.isEmpty()) {
            capturePanelDragLayout(panelId, panelWindowBounds(panelId));
        }
        if (panelDragLayout.isEmpty()) return;

        for (PanelDragLayout layout : panelDragLayout) {
            int x = windowBounds.x() + Math.round(layout.relX() * windowBounds.width());
            int y = windowBounds.y() + Math.round(layout.relY() * windowBounds.height());
            int w = Math.max(1, Math.round(layout.relW() * windowBounds.width()));
            int h = Math.max(1, Math.round(layout.relH() * windowBounds.height()));
            setPanelSlotBounds(layout.slot(), x, y, w, h);
        }
    }

    private void setPanelSlotBounds(PanelSlot slot, int x, int y, int width, int height) {
        if (slot.results.visible) {
            slot.results.updateBounds(new WidgetBounds(x, y, width, height));
        } else if (slot.sidebar.visible) {
            slot.sidebar.updateLayout(Rect.of(x, y, width, height));
        }
    }

    private boolean beginPanelDrag(String panelId, double mouseX, double mouseY) {
        WidgetBounds bounds = panelWindowBounds(panelId);
        PanelDragHandle handle = resolvePanelDragHandle(bounds, mouseX, mouseY);
        if (handle == PanelDragHandle.NONE) return false;

        draggedWidgetId = panelId;
        panelDragHandle = handle;
        dragStartMouseX = mouseX;
        dragStartMouseY = mouseY;
        dragOriginX = bounds.x();
        dragOriginY = bounds.y();
        dragOriginW = bounds.width();
        dragOriginH = bounds.height();
        capturePanelDragLayout(panelId, bounds);
        return true;
    }

    private void clearPanelDragState() {
        draggedWidgetId = null;
        panelDragHandle = PanelDragHandle.NONE;
        dragOriginX = 0;
        dragOriginY = 0;
        dragOriginW = 0;
        dragOriginH = 0;
        panelDragLayout.clear();
    }

    public void setLayoutMode(boolean enabled) {
        if (enabled && !inLayoutMode) {
            layoutModePositionSnapshot = AmiConfig.pinnedPositionsJson;
            panelVisibleBeforeLayoutMode = panelVisible;
            setPanelVisible(true);
            if (searchBar != null) {
                searchBar.setFocused(false);
            }
        } else if (!enabled && inLayoutMode) {
            setPanelVisible(panelVisibleBeforeLayoutMode);
            layoutDoneButton = null;
            layoutResetButton = null;
            lastLayoutButtonScreenW = -1;
            lastLayoutButtonScreenH = -1;
        }
        inLayoutMode = enabled;
        if (!enabled) {
            clearPanelDragState();
        }
    }

    public boolean isInLayoutMode() {
        return inLayoutMode;
    }

    // ── Layout Mode & Panel Dragging ───────────────────────────────────

    private void finalizeLayout() {
        Screen screen = Minecraft.getInstance().screen;
        if (draggedWidgetId != null && screen != null) finalizeDrag(screen);
        pinnedPositions.save();
        layoutModePositionSnapshot = null;
        setLayoutMode(false);
    }

    public void cancelLayout() {
        if (layoutModePositionSnapshot != null) {
            AmiConfig.pinnedPositionsJson = layoutModePositionSnapshot;
            pinnedPositions = PinnedWidgetPositions.load();
        }
        clearPanelDragState();
        layoutModePositionSnapshot = null;
        setLayoutMode(false);
        layoutDirty = true;
    }

    private void ensureLayoutButtons(int screenW, int screenH) {
        if (layoutDoneButton != null && screenW == lastLayoutButtonScreenW && screenH == lastLayoutButtonScreenH)
            return;
        lastLayoutButtonScreenW = screenW;
        lastLayoutButtonScreenH = screenH;
        int btnW = 100;
        int btnH = 20;
        int btnY = screenH - BOTTOM_BAR_H - btnH - 6;
        layoutDoneButton = net.minecraft.client.gui.components.Button.builder(
                net.minecraft.network.chat.Component.translatable("ami.layout.done"),
                b -> finalizeLayout()
        ).bounds(screenW / 2 - btnW - 4, btnY, btnW, btnH).build();
        layoutResetButton = net.minecraft.client.gui.components.Button.builder(
                net.minecraft.network.chat.Component.translatable("ami.layout.reset"),
                b -> resetLayout()
        ).bounds(screenW / 2 + 4, btnY, btnW, btnH).build();
    }

    public boolean tryStartDrag(double mouseX, double mouseY, int button) {
        if (!inLayoutMode || button != 0) return false;

        if (searchBar != null && isSearchBarDragHandle(mouseX, mouseY)) {
            draggedWidgetId = "search_bar";
            dragStartMouseX = mouseX;
            dragStartMouseY = mouseY;
            dragOriginX = searchBar.getX();
            dragOriginY = searchBar.getY();
            dragOriginW = searchBar.getBounds().width();
            dragOriginH = searchBar.getBounds().height();
            return true;
        }
        if (beginPanelDrag("left_panel", mouseX, mouseY)) return true;
        if (beginPanelDrag("right_panel", mouseX, mouseY)) return true;

        return false;
    }

    private boolean isSearchBarDragHandle(double mouseX, double mouseY) {
        if (searchBar == null || !searchBar.visible) return false;
        WidgetBounds b = searchBar.getBounds();
        return mouseX >= b.x() && mouseX < b.x() + 14 && mouseY >= b.y() && mouseY < b.y() + b.height();
    }

    public boolean updateDrag(double mouseX, double mouseY) {
        if (draggedWidgetId == null) return false;
        int dx = (int) (mouseX - dragStartMouseX);
        int dy = (int) (mouseY - dragStartMouseY);

        if ("search_bar".equals(draggedWidgetId) && searchBar != null) {
            int newX = dragOriginX + dx;
            int newY = dragOriginY + dy;
            searchBar.updateBounds(new WidgetBounds(newX, newY, searchBar.getBounds().width(), searchBar.getBounds().height()));
            pinnedPositions.set("search_bar", newX, newY);
        } else {
            int nextX = dragOriginX;
            int nextY = dragOriginY;
            int nextW = dragOriginW;
            int nextH = dragOriginH;
            switch (panelDragHandle) {
                case MOVE:
                    nextX += dx;
                    nextY += dy;
                    break;
                case RESIZE_LEFT:
                    nextX += dx;
                    nextW -= dx;
                    break;
                case RESIZE_RIGHT:
                    nextW += dx;
                    break;
                case RESIZE_TOP:
                    nextY += dy;
                    nextH -= dy;
                    break;
                case RESIZE_BOTTOM:
                    nextH += dy;
                    break;
                case RESIZE_TOP_LEFT:
                    nextX += dx;
                    nextW -= dx;
                    nextY += dy;
                    nextH -= dy;
                    break;
                case RESIZE_TOP_RIGHT:
                    nextW += dx;
                    nextY += dy;
                    nextH -= dy;
                    break;
                case RESIZE_BOTTOM_LEFT:
                    nextX += dx;
                    nextW -= dx;
                    nextH += dy;
                    break;
                case RESIZE_BOTTOM_RIGHT:
                    nextW += dx;
                    nextH += dy;
                    break;
                case NONE:
                default:
                    break;
            }

            Minecraft mc = Minecraft.getInstance();
            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();
            int minW = Math.max(MIN_PANEL_WIDTH, 1);
            int minH = Math.max(MIN_SIDE_PANEL_HEIGHT, 1);
            int maxW = Math.max(minW, screenW - 4);
            int maxH = Math.max(minH, screenH - 4);

            if (panelDragHandle == PanelDragHandle.RESIZE_LEFT || panelDragHandle == PanelDragHandle.RESIZE_TOP_LEFT
                    || panelDragHandle == PanelDragHandle.RESIZE_BOTTOM_LEFT) {
                nextW = net.minecraft.util.Mth.clamp(nextW, minW, maxW);
                nextX = dragOriginX + dragOriginW - nextW;
            } else {
                nextW = net.minecraft.util.Mth.clamp(nextW, minW, maxW);
            }

            if (panelDragHandle == PanelDragHandle.RESIZE_TOP || panelDragHandle == PanelDragHandle.RESIZE_TOP_LEFT
                    || panelDragHandle == PanelDragHandle.RESIZE_TOP_RIGHT) {
                nextH = net.minecraft.util.Mth.clamp(nextH, minH, maxH);
                nextY = dragOriginY + dragOriginH - nextH;
            } else {
                nextH = net.minecraft.util.Mth.clamp(nextH, minH, maxH);
            }

            nextX = net.minecraft.util.Mth.clamp(nextX, 0, Math.max(0, screenW - nextW));
            nextY = net.minecraft.util.Mth.clamp(nextY, 0, Math.max(0, screenH - nextH));

            WidgetBounds next = new WidgetBounds(nextX, nextY, nextW, nextH);
            applyPanelWindowLayout(draggedWidgetId, next);
            pinnedPositions.set(draggedWidgetId, nextX, nextY, nextW, nextH);
            layoutDirty = true;
        }

        return true;
    }

    public void finalizeDrag(Screen screen) {
        if (draggedWidgetId == null) return;

        int screenW = screen.width;
        int screenH = screen.height;

        if ("search_bar".equals(draggedWidgetId) && searchBar != null) {
            int x = net.minecraft.util.Mth.clamp(searchBar.getX(), 2, screenW - searchBar.getBounds().width() - 2);
            int y = net.minecraft.util.Mth.clamp(searchBar.getY(), 2, screenH - SEARCH_H - 2);
            pinnedPositions.set("search_bar", x, y);
            pinnedPositions.set("search_bar", x, y, searchBar.getBounds().width(), SEARCH_H);
        } else {
            WidgetBounds bounds = panelWindowBounds(draggedWidgetId);
            if (bounds == null) {
                clearPanelDragState();
                return;
            }
            int width = net.minecraft.util.Mth.clamp(bounds.width(), MIN_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, screenW - 4));
            int height = net.minecraft.util.Mth.clamp(bounds.height(), MIN_SIDE_PANEL_HEIGHT, Math.max(MIN_SIDE_PANEL_HEIGHT, screenH - 4));
            int x = net.minecraft.util.Mth.clamp(bounds.x(), 0, Math.max(0, screenW - width));
            int y = net.minecraft.util.Mth.clamp(bounds.y(), 0, Math.max(0, screenH - height));
            WidgetBounds clamped = new WidgetBounds(x, y, width, height);
            applyPanelWindowLayout(draggedWidgetId, clamped);
            pinnedPositions.set(draggedWidgetId, x, y, width, height);
        }

        if (!inLayoutMode) pinnedPositions.save();
        clearPanelDragState();
        layoutDirty = true;
    }

    public void resetLayout() {
        pinnedPositions.clearAll();
        pinnedPositions.save();
        draggedWidgetId = null;
        layoutModePositionSnapshot = null;
        setLayoutMode(false);
        layoutDirty = true;
    }

    private void renderLayoutMode(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        ensureLayoutButtons(sw, sh);

        // Hint text
        var font = mc.font;
        net.minecraft.network.chat.Component hint = net.minecraft.network.chat.Component.translatable("ami.layout.hint");
        int hintW = font.width(hint);
        int hintX = sw / 2 - hintW / 2;
        int hintY = 4;
        g.fill(hintX - 4, hintY - 2, hintX + hintW + 4, hintY + font.lineHeight + 2, 0xCC000000);
        g.drawString(font, hint, hintX, hintY, 0xFFFFFFFF, false);

        renderLayoutDragHandles(g);

        layoutDoneButton.render(g, mx, my, pt);
        layoutResetButton.render(g, mx, my, pt);
    }

    private void renderLayoutDragHandles(net.minecraft.client.gui.GuiGraphics g) {
        int accent = AMITheme.ACCENT_BLUE;
        int accentDim = 0x88_4A90D9;

        // Search bar: grip dots on left side
        if (searchBar != null && searchBar.visible) {
            WidgetBounds b = searchBar.getBounds();
            boolean dragging = "search_bar".equals(draggedWidgetId);
            int gripColor = dragging ? accent : accentDim;
            int cx = b.x() + 4;
            int cy = b.y() + b.height() / 2 - 3;
            for (int row = 0; row < 3; row++) {
                int dy = row * 4;
                g.fill(cx, cy + dy, cx + 2, cy + dy + 2, gripColor);
                g.fill(cx + 4, cy + dy, cx + 6, cy + dy + 2, gripColor);
            }
            if (dragging) g.renderOutline(b.x(), b.y(), b.width(), b.height(), accent);
        }

        // Panels: colored strip across the top
        renderPanelDragStrip(g, leftSlotPool, "left_panel", accent, accentDim);
        renderPanelDragStrip(g, rightSlotPool, "right_panel", accent, accentDim);
    }

    private void renderPanelDragStrip(net.minecraft.client.gui.GuiGraphics g, List<PanelSlot> pool,
                                      String panelId, int accent, int accentDim) {
        WidgetBounds panelBounds = panelWindowBounds(pool);
        if (panelBounds == null) return;

        boolean dragging = panelId.equals(draggedWidgetId);
        int fillColor = dragging ? accent : accentDim;
        g.renderOutline(panelBounds.x(), panelBounds.y(), panelBounds.width(), panelBounds.height(), fillColor);
        int h = PANEL_HANDLE_HITBOX;
        int x1 = panelBounds.x();
        int y1 = panelBounds.y();
        int x2 = panelBounds.x() + panelBounds.width();
        int y2 = panelBounds.y() + panelBounds.height();
        int hx = Math.min(h, panelBounds.width() / 2);
        int hy = Math.min(h, panelBounds.height() / 2);
        g.fill(x1, y1, x1 + panelBounds.width(), y1 + 2, fillColor);
        g.fill(x1, y1, x1 + 2, y1 + panelBounds.height(), fillColor);
        g.fill(x2 - 2, y1, x2, y1 + panelBounds.height(), fillColor);
        g.fill(x1, y2 - 2, x1 + panelBounds.width(), y2, fillColor);
        g.fill(x1, y1, x1 + hx, y1 + hy, fillColor);
        g.fill(x2 - hx, y1, x2, y1 + hy, fillColor);
        g.fill(x1, y2 - hy, x1 + hx, y2, fillColor);
        g.fill(x2 - hx, y2 - hy, x2, y2, fillColor);
        if (dragging) {
            for (PanelSlot slot : pool) {
                AbstractWidget w = slot.visibleWidget();
                if (w == null || !w.visible) continue;
                g.renderOutline(w.getX(), w.getY(), w.getWidth(), w.getHeight(), accent);
            }
        }
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
        for (PanelSlot slot : activeSlotsSnapshot()) {
            if (slot.results.visible) bounds.add(slot.results.getBounds());
            if (slot.sidebar.visible) bounds.add(slot.sidebar.getBounds());
        }
        if (leftPanelBarBounds != null) bounds.add(leftPanelBarBounds);
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

    private enum PanelDragHandle {
        NONE,
        MOVE,
        RESIZE_LEFT,
        RESIZE_RIGHT,
        RESIZE_TOP,
        RESIZE_BOTTOM,
        RESIZE_TOP_LEFT,
        RESIZE_TOP_RIGHT,
        RESIZE_BOTTOM_LEFT,
        RESIZE_BOTTOM_RIGHT
    }

    private record PanelDragLayout(PanelSlot slot, float relX, float relY, float relW, float relH) {
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

        AbstractWidget visibleWidget() {
            return activeWidget();
        }
    }
}
