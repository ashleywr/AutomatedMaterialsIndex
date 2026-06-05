package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.AmiRenderProfiler;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class InventorySearchHighlighter {
    private static final int SLOT_OVERLAY_SIZE = 18;
    private static final int DIM_COLOR = 0x77000000;
    private static final int MATCH_BORDER_COLOR = 0xFFEEEE00;
    private static final int MAX_MATCH_BORDERS = 512;
    private static final long QUERY_DEBOUNCE_MS = Math.max(0L, Long.getLong("ami.inventoryHighlighter.debounceMs", 300L));
    private static final ExecutorService MATCH_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "AMI Inventory Search Highlighter");
        thread.setDaemon(true);
        thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
        return thread;
    });
    private static final Object SEARCH_SERVICE_LOCK = new Object();
    private static long cachedSearchServiceRevision = Long.MIN_VALUE;
    private static SearchService cachedSearchService = null;

    private boolean active = false;
    private String requestedQuery = "";
    private long requestedQueryChangedAtMs = 0L;
    private volatile MatchResult completedMatchResult = MatchResult.EMPTY;
    private CompletableFuture<MatchResult> inFlightMatchTask = null;
    private AbstractContainerMenu cachedOverlayMenu = null;
    private long cachedOverlayRevision = Long.MIN_VALUE;
    private String cachedOverlayQuery = "";
    private int cachedOverlaySlotCount = -1;
    private long cachedSlotSignature = Long.MIN_VALUE;
    private OverlayBatch cachedOverlayBatch = OverlayBatch.EMPTY;

    public boolean toggle(String query) {
        String normalized = normalize(query);
        active = !active;
        requestQuery(normalized);
        return active;
    }

    public void updateQuery(String query) {
        requestQuery(normalize(query));
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            requestQuery("");
        }
    }

    public boolean isActive() {
        return active;
    }

    public void render(AbstractContainerScreen<?> screen, GuiGraphics graphics) {
        if (!active || requestedQuery.isBlank()) {
            return;
        }

        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("inventoryHighlighter.render")) {
            MatchResult matches = matchingItemsForRender();
            if (!matches.ready()) {
                AmiRenderProfiler.count("inventoryHighlighter.waitingForQuery");
                return;
            }
            int left = screen.getGuiLeft();
            int top = screen.getGuiTop();
            OverlayBatch overlays = overlayBatch(screen, matches);
            try (AmiRenderProfiler.Section draw = AmiRenderProfiler.section("inventoryHighlighter.draw")) {
                AmiRenderProfiler.add("inventoryHighlighter.dimRuns", overlays.dimRuns().size());
                AmiRenderProfiler.add("inventoryHighlighter.matchBorders", overlays.matches().size());
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, OverlayLayers.PANEL - 1);
                try {
                    for (DimRun run : overlays.dimRuns()) {
                        graphics.fill(left + run.x(), top + run.y(),
                                left + run.x() + run.width(), top + run.y() + SLOT_OVERLAY_SIZE, DIM_COLOR);
                    }
                    for (SlotOverlay match : overlays.matches()) {
                        drawMatchBorder(graphics, left + match.x(), top + match.y());
                    }
                } finally {
                    graphics.pose().popPose();
                }
            }
        }
    }

    private OverlayBatch overlayBatch(AbstractContainerScreen<?> screen, MatchResult matches) {
        AbstractContainerMenu menu = screen.getMenu();
        List<Slot> slots = menu.slots;
        long slotSignature;
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("inventoryHighlighter.slotSignature")) {
            AmiRenderProfiler.add("inventoryHighlighter.slotsScanned", slots.size());
            slotSignature = slotSignature(slots);
        }
        if (menu == cachedOverlayMenu
                && matches.revision() == cachedOverlayRevision
                && matches.query().equals(cachedOverlayQuery)
                && slots.size() == cachedOverlaySlotCount
                && slotSignature == cachedSlotSignature) {
            AmiRenderProfiler.count("inventoryHighlighter.overlayCacheHit");
            return cachedOverlayBatch;
        }

        OverlayBatch batch;
        try (AmiRenderProfiler.Section ignored = AmiRenderProfiler.section("inventoryHighlighter.overlayRebuild")) {
            AmiRenderProfiler.count("inventoryHighlighter.overlayCacheMiss");
            batch = buildOverlayBatch(slots, matches.matches());
        }
        cachedOverlayMenu = menu;
        cachedOverlayRevision = matches.revision();
        cachedOverlayQuery = matches.query();
        cachedOverlaySlotCount = slots.size();
        cachedSlotSignature = slotSignature;
        cachedOverlayBatch = batch;
        return batch;
    }

    private static long slotSignature(List<Slot> slots) {
        long signature = 0xcbf29ce484222325L;
        for (Slot slot : slots) {
            boolean active = slot.isActive();
            signature = mix(signature, active ? 1 : 0);
            signature = mix(signature, slot.x);
            signature = mix(signature, slot.y);
            if (!active) {
                continue;
            }
            ItemStack stack = slot.getItem();
            boolean empty = stack.isEmpty();
            signature = mix(signature, empty ? 0 : 1);
            if (!empty) {
                signature = mix(signature, System.identityHashCode(stack.getItem()));
            }
        }
        return signature;
    }

    private static long mix(long signature, int value) {
        signature ^= value;
        return signature * 0x100000001b3L;
    }

    private static OverlayBatch buildOverlayBatch(List<Slot> slots, Set<ResourceLocation> matchingItems) {
        List<SlotOverlay> dimSlots = new ArrayList<>();
        List<SlotOverlay> matches = new ArrayList<>();
        for (Slot slot : slots) {
            if (!slot.isActive()) {
                continue;
            }
            int x = slot.x - 1;
            int y = slot.y - 1;
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                if (matchingItems.contains(itemId)) {
                    matches.add(new SlotOverlay(x, y));
                    continue;
                }
            }
            dimSlots.add(new SlotOverlay(x, y));
        }
        return new OverlayBatch(mergeDimRuns(dimSlots), visibleMatchBorders(matches));
    }

    private static List<SlotOverlay> visibleMatchBorders(List<SlotOverlay> matches) {
        if (matches.size() > MAX_MATCH_BORDERS) {
            AmiRenderProfiler.count("inventoryHighlighter.matchBordersCapped");
            return List.of();
        }
        return List.copyOf(matches);
    }

    private static List<DimRun> mergeDimRuns(List<SlotOverlay> dimSlots) {
        if (dimSlots.isEmpty()) {
            return List.of();
        }
        dimSlots.sort(Comparator.comparingInt(SlotOverlay::y).thenComparingInt(SlotOverlay::x));
        List<DimRun> runs = new ArrayList<>();
        DimRun current = null;
        for (SlotOverlay slot : dimSlots) {
            if (current != null && slot.y() == current.y() && slot.x() == current.x() + current.width()) {
                current = new DimRun(current.x(), current.y(), current.width() + SLOT_OVERLAY_SIZE);
                runs.set(runs.size() - 1, current);
                continue;
            }
            current = new DimRun(slot.x(), slot.y(), SLOT_OVERLAY_SIZE);
            runs.add(current);
        }
        return List.copyOf(runs);
    }

    private void requestQuery(String query) {
        if (query.equals(requestedQuery)) {
            return;
        }
        requestedQuery = query;
        requestedQueryChangedAtMs = System.currentTimeMillis();
        cachedOverlayQuery = "";
    }

    private MatchResult matchingItemsForRender() {
        GlobalIndex index = GlobalIndex.getInstance();
        long revision = index.revision();
        String query = requestedQuery;
        if (query.isBlank()) {
            return MatchResult.EMPTY;
        }

        acceptCompletedMatchTask(revision, query);

        MatchResult completed = completedMatchResult;
        if (completed.ready() && completed.revision() == revision && completed.query().equals(query)) {
            AmiRenderProfiler.count("inventoryHighlighter.queryCacheHit");
            return completed;
        }
        MatchResult fallback = completed.ready() && completed.revision() == revision ? completed : MatchResult.EMPTY;

        if (System.currentTimeMillis() - requestedQueryChangedAtMs < QUERY_DEBOUNCE_MS) {
            AmiRenderProfiler.count("inventoryHighlighter.queryDebounced");
            return fallback;
        }

        scheduleMatchTask(index, revision, query);
        return fallback;
    }

    private void acceptCompletedMatchTask(long currentRevision, String currentQuery) {
        CompletableFuture<MatchResult> task = inFlightMatchTask;
        if (task == null || !task.isDone()) {
            return;
        }
        MatchResult result = task.join();
        inFlightMatchTask = null;
        if (result.revision() == currentRevision && result.query().equals(currentQuery)) {
            completedMatchResult = result;
            cachedOverlayQuery = "";
            AmiRenderProfiler.count("inventoryHighlighter.queryCompleted");
            AmiRenderProfiler.add("inventoryHighlighter.matchingItemIds", result.matches().size());
        } else {
            AmiRenderProfiler.count("inventoryHighlighter.queryDiscarded");
        }
    }

    private void scheduleMatchTask(GlobalIndex index, long revision, String query) {
        if (inFlightMatchTask != null) {
            AmiRenderProfiler.count("inventoryHighlighter.queryInFlight");
            return;
        }
        AmiRenderProfiler.count("inventoryHighlighter.queryScheduled");
        inFlightMatchTask = CompletableFuture.supplyAsync(() -> new MatchResult(query, revision,
                matchingItems(index, revision, query), true), MATCH_EXECUTOR);
    }

    static Set<ResourceLocation> matchingItems(GlobalIndex index, String query) {
        return matchingItems(index, index.revision(), query);
    }

    private static Set<ResourceLocation> matchingItems(GlobalIndex index, long revision, String query) {
        Set<ResourceLocation> matches = new HashSet<>();
        SearchService service = searchService(index, revision);
        List<SearchNode> nodes = service.query(query).getOrDefault(NodeType.ITEM, List.of());
        for (SearchNode node : nodes) {
            matches.add(node.id());
        }
        return matches;
    }

    private static SearchService searchService(GlobalIndex index, long revision) {
        synchronized (SEARCH_SERVICE_LOCK) {
            if (cachedSearchService != null && cachedSearchServiceRevision == revision) {
                return cachedSearchService;
            }
            cachedSearchService = SearchService.buildFrom(index, false);
            cachedSearchServiceRevision = revision;
            return cachedSearchService;
        }
    }

    private static String normalize(String query) {
        return query == null ? "" : query.trim();
    }

    private static void drawMatchBorder(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_OVERLAY_SIZE, y + 1, MATCH_BORDER_COLOR);
        graphics.fill(x, y + SLOT_OVERLAY_SIZE - 1, x + SLOT_OVERLAY_SIZE, y + SLOT_OVERLAY_SIZE, MATCH_BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + SLOT_OVERLAY_SIZE, MATCH_BORDER_COLOR);
        graphics.fill(x + SLOT_OVERLAY_SIZE - 1, y, x + SLOT_OVERLAY_SIZE, y + SLOT_OVERLAY_SIZE, MATCH_BORDER_COLOR);
    }

    private record OverlayBatch(List<DimRun> dimRuns, List<SlotOverlay> matches) {
        private static final OverlayBatch EMPTY = new OverlayBatch(List.of(), List.of());
    }

    private record SlotOverlay(int x, int y) {
    }

    private record DimRun(int x, int y, int width) {
    }

    private record MatchResult(String query, long revision, Set<ResourceLocation> matches, boolean ready) {
        private static final MatchResult EMPTY = new MatchResult("", Long.MIN_VALUE, Set.of(), false);
    }
}
