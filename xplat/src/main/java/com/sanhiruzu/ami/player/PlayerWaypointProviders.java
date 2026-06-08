package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiColors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerWaypointProviders {
    private static final Logger LOGGER = Logger.getLogger(PlayerWaypointProviders.class.getName());
    private static final long LIVE_WAYPOINT_POLL_INTERVAL_MS = 5_000L;
    private static final CopyOnWriteArrayList<PlayerWaypointProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static long lastLiveWaypointPollMs = Long.MIN_VALUE;
    private static long liveWaypointRevision = 0L;
    private static String lastLiveWaypointSnapshot = "";

    static {
        register(new DetectedMapWaypointProvider("xaero", "Xaero", List.of("xaerominimap", "xaeroworldmap")));
        register(new JourneyMapWaypointProvider());
        register(new FtbChunksWaypointProvider());
        register(new ManualWaypointProvider());
        JourneyMapEventListener.registerIfAvailable();
    }

    private PlayerWaypointProviders() {
    }

    public static void register(PlayerWaypointProvider provider) {
        if (provider == null || provider.id() == null || provider.id().isBlank()) {
            return;
        }
        PROVIDERS.removeIf(existing -> Objects.equals(existing.id(), provider.id()));
        PROVIDERS.add(provider);
    }

    public static List<PlayerWaypointProvider> providers() {
        return List.copyOf(PROVIDERS);
    }

    public static void enrich(PlayerWaypointContext context, Map<String, String> metadata) {
        enrich(providers(), context, metadata);
    }

    public static List<PlayerWaypointExport> waypointExports(PlayerWaypointContext context) {
        return waypointExports(providers(), context);
    }

    public static List<PlayerWaypointAction> waypointActions(PlayerWaypointContext context) {
        return waypointActions(providers(), context);
    }

    public static List<SearchNode> liveWaypointNodes() {
        return liveWaypointNodes(providers());
    }

    static List<SearchNode> liveWaypointNodesForTests(List<PlayerWaypointProvider> providers) {
        return liveWaypointNodes(providers);
    }

    private static List<SearchNode> liveWaypointNodes(List<PlayerWaypointProvider> providers) {
        List<SearchNode> nodes = new ArrayList<>();
        for (PlayerWaypointProvider provider : providers) {
            if (!available(provider)) {
                continue;
            }
            try {
                for (LiveWaypoint waypoint : provider.liveWaypoints()) {
                    if (waypoint != null && !waypoint.id().isBlank()) {
                        nodes.add(toNode(provider, waypoint));
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Player waypoint provider failed while enumerating waypoints: " + provider.id(), e);
            }
        }
        nodes.sort(Comparator
                .comparing((SearchNode node) -> node.meta(SearchNodeKeys.WAYPOINT_PROVIDER_LABEL, ""), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(SearchNode::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(nodes);
    }

    public static long liveWaypointRevision() {
        long now = System.currentTimeMillis();
        if (now - lastLiveWaypointPollMs < LIVE_WAYPOINT_POLL_INTERVAL_MS) {
            return liveWaypointRevision;
        }
        lastLiveWaypointPollMs = now;

        String snapshot = liveWaypointSnapshot();
        if (!snapshot.equals(lastLiveWaypointSnapshot)) {
            lastLiveWaypointSnapshot = snapshot;
            liveWaypointRevision++;
        }
        return liveWaypointRevision;
    }

    /**
     * Invalidate the live waypoint cache immediately. Called when external changes
     * (e.g., edits in JourneyMap directly) are detected via event listeners.
     */
    public static void invalidateLiveWaypointCache() {
        lastLiveWaypointPollMs = Long.MIN_VALUE;
        lastLiveWaypointSnapshot = "";
    }

    public static List<PlayerWaypointAction> liveWaypointActions(SearchNode node) {
        LiveWaypoint waypoint = fromNode(node);
        if (waypoint == null) {
            return List.of();
        }
        List<PlayerWaypointAction> actions = new ArrayList<>();
        PlayerWaypointProvider provider = provider(waypoint.providerId()).orElse(null);
        if (provider != null && available(provider)) {
            try {
                actions.addAll(provider.liveWaypointActions(new LiveWaypointContext(node, waypoint)));
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Player waypoint provider failed while building waypoint actions: " + provider.id(), e);
            }
        }
        actions.add(new PlayerWaypointAction(
                "ami:copy_waypoint",
                "Copy Waypoint",
                'c',
                () -> com.sanhiruzu.ami.util.AmiClipboardHelper.copyToClipboard(waypoint.chatLine())
        ));
        return List.copyOf(actions.stream().filter(Objects::nonNull).filter(PlayerWaypointAction::isUsable).toList());
    }

    public static Optional<PlayerWaypointAction> openLiveWaypointAction(SearchNode node) {
        LiveWaypoint waypoint = fromNode(node);
        if (waypoint == null) {
            return Optional.empty();
        }
        return provider(waypoint.providerId())
                .filter(PlayerWaypointProviders::available)
                .flatMap(provider -> {
                    try {
                        return provider.openLiveWaypointAction(new LiveWaypointContext(node, waypoint));
                    } catch (RuntimeException | LinkageError e) {
                        LOGGER.log(Level.FINE, "AMI: Player waypoint provider failed while opening waypoint: " + provider.id(), e);
                        return Optional.empty();
                    }
                });
    }

    static List<String> availableProviderIdsForTests(List<PlayerWaypointProvider> providers) {
        List<String> ids = new ArrayList<>();
        if (providers == null) {
            return ids;
        }
        for (PlayerWaypointProvider provider : providers) {
            if (available(provider)) {
                ids.add(provider.id());
            }
        }
        return ids;
    }

    static void enrichForTests(List<PlayerWaypointProvider> providers, PlayerWaypointContext context, Map<String, String> metadata) {
        enrich(providers, context, metadata);
    }

    static List<PlayerWaypointExport> waypointExportsForTests(List<PlayerWaypointProvider> providers, PlayerWaypointContext context) {
        return waypointExports(providers, context);
    }

    static List<PlayerWaypointAction> waypointActionsForTests(List<PlayerWaypointProvider> providers, PlayerWaypointContext context) {
        return waypointActions(providers, context);
    }

    private static void enrich(List<PlayerWaypointProvider> providers, PlayerWaypointContext context, Map<String, String> metadata) {
        if (metadata == null) {
            return;
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (PlayerWaypointProvider provider : providers) {
            if (!available(provider)) {
                continue;
            }
            ids.add(provider.id());
            if (provider.label() != null && !provider.label().isBlank()) {
                labels.add(provider.label());
            }
            try {
                provider.enrich(context, metadata);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Player waypoint provider failed during enrichment: " + provider.id(), e);
            }
        }
        if (!ids.isEmpty()) {
            metadata.put(SearchNodeKeys.PLAYER_WAYPOINT_PROVIDERS, String.join(",", ids));
        }
        if (!labels.isEmpty()) {
            metadata.put(SearchNodeKeys.PLAYER_WAYPOINT_PROVIDER_LABELS, String.join(",", labels));
        }
    }

    private static List<PlayerWaypointExport> waypointExports(List<PlayerWaypointProvider> providers, PlayerWaypointContext context) {
        List<PlayerWaypointExport> exports = new ArrayList<>();
        for (PlayerWaypointProvider provider : providers) {
            if (!available(provider)) {
                continue;
            }
            try {
                provider.waypointExport(context)
                        .filter(PlayerWaypointExport::isUsable)
                        .ifPresent(exports::add);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Player waypoint provider failed while building waypoint export: " + provider.id(), e);
            }
        }
        return List.copyOf(exports);
    }

    private static List<PlayerWaypointAction> waypointActions(List<PlayerWaypointProvider> providers, PlayerWaypointContext context) {
        List<PlayerWaypointAction> actions = new ArrayList<>();
        for (PlayerWaypointProvider provider : providers) {
            if (!available(provider)) {
                continue;
            }
            try {
                for (PlayerWaypointAction action : provider.waypointActions(context)) {
                    if (action != null && action.isUsable()) {
                        actions.add(safeAction(provider, action));
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Player waypoint provider failed while building waypoint actions: " + provider.id(), e);
            }
        }
        return List.copyOf(actions);
    }

    private static PlayerWaypointAction safeAction(PlayerWaypointProvider provider, PlayerWaypointAction action) {
        return new PlayerWaypointAction(action.id(), action.label(), action.mnemonic(), () -> {
            try {
                action.action().run();
            } catch (RuntimeException | LinkageError e) {
                String providerId = provider == null ? "<unknown>" : provider.id();
                LOGGER.log(Level.WARNING, "AMI: Player waypoint action failed for provider " + providerId, e);
            }
        });
    }

    private static Optional<PlayerWaypointProvider> provider(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return PROVIDERS.stream().filter(provider -> id.equals(provider.id())).findFirst();
    }

    private static String liveWaypointSnapshot() {
        List<String> entries = new ArrayList<>();
        for (SearchNode node : liveWaypointNodes()) {
            entries.add(String.join("|",
                    node.meta(SearchNodeKeys.WAYPOINT_PROVIDER, ""),
                    node.meta(SearchNodeKeys.WAYPOINT_ID, ""),
                    node.meta(SearchNodeKeys.WAYPOINT_NAME, ""),
                    node.meta(SearchNodeKeys.WAYPOINT_DIMENSION, ""),
                    node.meta(SearchNodeKeys.WAYPOINT_X, ""),
                    node.meta(SearchNodeKeys.WAYPOINT_Y, ""),
                    node.meta(SearchNodeKeys.WAYPOINT_Z, ""),
                    node.meta(SearchNodeKeys.WAYPOINT_VISIBLE, "")
            ));
        }
        entries.sort(String::compareTo);
        return String.join(";", entries);
    }


    private static SearchNode toNode(PlayerWaypointProvider provider, LiveWaypoint waypoint) {
        Map<String, String> meta = new HashMap<>(waypoint.metadata());
        meta.put(SearchNodeKeys.MOD_ID, provider.id());
        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "environment");
        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "waypoints");
        meta.put(SearchNodeKeys.WAYPOINT_PROVIDER, provider.id());
        meta.put(SearchNodeKeys.WAYPOINT_PROVIDER_LABEL, provider.label());
        meta.put(SearchNodeKeys.WAYPOINT_ID, waypoint.id());
        meta.put(SearchNodeKeys.WAYPOINT_NAME, waypoint.name());
        meta.put(SearchNodeKeys.WAYPOINT_DIMENSION, waypoint.dimension());
        meta.put(SearchNodeKeys.WAYPOINT_X, Integer.toString(waypoint.x()));
        meta.put(SearchNodeKeys.WAYPOINT_Y, Integer.toString(waypoint.y()));
        meta.put(SearchNodeKeys.WAYPOINT_Z, Integer.toString(waypoint.z()));
        meta.put(SearchNodeKeys.SEARCH_TOKENS, String.join(" ",
                waypoint.name(),
                waypoint.dimension(),
                Integer.toString(waypoint.x()),
                Integer.toString(waypoint.y()),
                Integer.toString(waypoint.z()),
                provider.id(),
                provider.label()
        ));
        return new SearchNode(
                Services.PLATFORM.rl("ami", "waypoint/" + safePath(provider.id()) + "/" + safePath(waypoint.id())),
                NodeType.WAYPOINT,
                waypoint.name(),
                AmiColors.PLAYER_NAME_COLOR,
                115,
                meta
        );
    }

    private static LiveWaypoint fromNode(SearchNode node) {
        if (node == null || node.type() != NodeType.WAYPOINT) {
            return null;
        }
        String providerId = node.meta(SearchNodeKeys.WAYPOINT_PROVIDER, "");
        String id = node.meta(SearchNodeKeys.WAYPOINT_ID, "");
        if (providerId.isBlank() || id.isBlank()) {
            return null;
        }
        return new LiveWaypoint(
                providerId,
                node.meta(SearchNodeKeys.WAYPOINT_PROVIDER_LABEL, providerId),
                id,
                node.meta(SearchNodeKeys.WAYPOINT_NAME, node.displayName()),
                node.meta(SearchNodeKeys.WAYPOINT_DIMENSION, "minecraft:overworld"),
                parseInt(node.meta(SearchNodeKeys.WAYPOINT_X, "0")),
                parseInt(node.meta(SearchNodeKeys.WAYPOINT_Y, "0")),
                parseInt(node.meta(SearchNodeKeys.WAYPOINT_Z, "0")),
                node.metadata()
        );
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String safePath(String value) {
        return value == null ? "unknown" : value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    private static boolean available(PlayerWaypointProvider provider) {
        if (provider == null) {
            return false;
        }
        try {
            return provider.isAvailable();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Player waypoint provider availability check failed: " + provider.id(), e);
            return false;
        }
    }
}
