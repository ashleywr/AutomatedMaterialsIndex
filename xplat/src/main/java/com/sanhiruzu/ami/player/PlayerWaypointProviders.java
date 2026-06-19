package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiColors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerWaypointProviders {
    private static final Logger LOGGER = Logger.getLogger(PlayerWaypointProviders.class.getName());
    private static final String WAYPOINT_PRIMARY_PROVIDER = "waypointPrimaryProvider";
    private static final String WAYPOINT_PRIMARY_PROVIDER_LABEL = "waypointPrimaryProviderLabel";
    private static final String WAYPOINT_MERGED_PROVIDERS = "waypointMergedProviders";
    private static final String WAYPOINT_MERGED_PROVIDER_LABELS = "waypointMergedProviderLabels";
    private static final String WAYPOINT_MERGED_PROVIDER_IDS = "waypointMergedProviderIds";
    private static final String WAYPOINT_CANONICAL_ID = "waypointCanonicalId";
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
        List<ResolvedLiveWaypoint> raw = enumerateLiveWaypoints(providers);
        List<MergedWaypoint> merged = AmiConfig.waypointMergeDuplicateProviders
                ? mergeEquivalentWaypoints(raw)
                : raw.stream().map(PlayerWaypointProviders::singletonMergedWaypoint).toList();
        List<SearchNode> nodes = merged.stream()
                .map(PlayerWaypointProviders::toNode)
                .sorted(Comparator
                        .comparing((SearchNode node) -> node.meta(WAYPOINT_PRIMARY_PROVIDER_LABEL, ""), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(SearchNode::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return List.copyOf(nodes);
    }

    public static long liveWaypointRevision() {
        return liveWaypointRevision(providers(), System.currentTimeMillis());
    }

    static long liveWaypointRevisionForTests(List<PlayerWaypointProvider> providers) {
        return liveWaypointRevision(providers, System.currentTimeMillis());
    }

    static void resetLiveWaypointStateForTests() {
        lastLiveWaypointPollMs = Long.MIN_VALUE;
        liveWaypointRevision = 0L;
        lastLiveWaypointSnapshot = "";
    }

    private static long liveWaypointRevision(List<PlayerWaypointProvider> providers, long now) {
        long intervalMs = Math.max(1L, AmiConfig.waypointRefreshIntervalSeconds) * 1000L;
        if (now - lastLiveWaypointPollMs < intervalMs) {
            return liveWaypointRevision;
        }
        lastLiveWaypointPollMs = now;

        String snapshot = liveWaypointSnapshot(providers);
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
        liveWaypointRevision++;
    }

    public static List<PlayerWaypointAction> liveWaypointActions(SearchNode node) {
        List<ResolvedLiveWaypoint> waypoints = resolvedWaypointsFromNode(node);
        if (waypoints.isEmpty()) {
            return List.of();
        }
        List<PlayerWaypointAction> actions = new ArrayList<>();
        for (ResolvedLiveWaypoint resolved : waypoints) {
            PlayerWaypointProvider waypointProvider = resolved.provider();
            if (waypointProvider == null || !available(waypointProvider)) {
                continue;
            }
            try {
                for (PlayerWaypointAction action : waypointProvider.liveWaypointActions(new LiveWaypointContext(node, resolved.waypoint()))) {
                    if (action != null && action.isUsable()) {
                        actions.add(safeAction(waypointProvider, action));
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Player waypoint provider failed while building waypoint actions: " + waypointProvider.id(), e);
            }
        }
        LiveWaypoint primaryWaypoint = waypoints.get(0).waypoint();
        actions.add(new PlayerWaypointAction(
                "ami:copy_waypoint",
                "Copy Waypoint",
                'c',
                () -> com.sanhiruzu.ami.util.AmiClipboardHelper.copyToClipboard(primaryWaypoint.chatLine())
        ));
        return List.copyOf(actions.stream().filter(Objects::nonNull).filter(PlayerWaypointAction::isUsable).toList());
    }

    public static Optional<PlayerWaypointAction> openLiveWaypointAction(SearchNode node) {
        for (ResolvedLiveWaypoint resolved : resolvedWaypointsFromNode(node)) {
            PlayerWaypointProvider waypointProvider = resolved.provider();
            if (waypointProvider == null || !available(waypointProvider)) {
                continue;
            }
            try {
                Optional<PlayerWaypointAction> action = waypointProvider.openLiveWaypointAction(new LiveWaypointContext(node, resolved.waypoint()));
                if (action.isPresent()) {
                    return action.map(found -> safeAction(waypointProvider, found));
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Player waypoint provider failed while opening waypoint: " + waypointProvider.id(), e);
            }
        }
        return Optional.empty();
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

    private static String liveWaypointSnapshot(List<PlayerWaypointProvider> providers) {
        List<String> entries = new ArrayList<>();
        for (SearchNode node : liveWaypointNodes(providers)) {
            entries.add(String.join("|",
                    node.meta(WAYPOINT_PRIMARY_PROVIDER, node.meta(SearchNodeKeys.WAYPOINT_PROVIDER, "")),
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


    private static SearchNode toNode(MergedWaypoint waypoint) {
        Map<String, String> meta = new HashMap<>(waypoint.metadata());
        meta.put(SearchNodeKeys.MOD_ID, waypoint.primaryProviderId());
        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "environment");
        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "waypoints");
        meta.put(SearchNodeKeys.WAYPOINT_PROVIDER, waypoint.primaryProviderId());
        meta.put(SearchNodeKeys.WAYPOINT_PROVIDER_LABEL, waypoint.primaryProviderLabel());
        meta.put(SearchNodeKeys.WAYPOINT_ID, waypoint.primaryWaypointId());
        meta.put(SearchNodeKeys.WAYPOINT_NAME, waypoint.displayName());
        meta.put(SearchNodeKeys.WAYPOINT_DIMENSION, waypoint.dimension());
        meta.put(SearchNodeKeys.WAYPOINT_X, Integer.toString(waypoint.x()));
        meta.put(SearchNodeKeys.WAYPOINT_Y, Integer.toString(waypoint.y()));
        meta.put(SearchNodeKeys.WAYPOINT_Z, Integer.toString(waypoint.z()));
        meta.put(WAYPOINT_PRIMARY_PROVIDER, waypoint.primaryProviderId());
        meta.put(WAYPOINT_PRIMARY_PROVIDER_LABEL, waypoint.primaryProviderLabel());
        meta.put(WAYPOINT_MERGED_PROVIDERS, String.join(",", waypoint.providerIds()));
        meta.put(WAYPOINT_MERGED_PROVIDER_LABELS, String.join(",", waypoint.providerLabels()));
        meta.put(WAYPOINT_MERGED_PROVIDER_IDS, String.join(",", waypoint.providerWaypointIds()));
        meta.put(WAYPOINT_CANONICAL_ID, waypoint.canonicalId());
        meta.put(SearchNodeKeys.SEARCH_TOKENS, String.join(" ",
                waypoint.displayName(),
                waypoint.dimension(),
                Integer.toString(waypoint.x()),
                Integer.toString(waypoint.y()),
                Integer.toString(waypoint.z()),
                String.join(" ", waypoint.providerIds()),
                String.join(" ", waypoint.providerLabels())
        ));
        return new SearchNode(
                Services.PLATFORM.rl("ami", "waypoint/merged/" + safePath(waypoint.canonicalId())),
                NodeType.WAYPOINT,
                waypoint.displayName(),
                AmiColors.PLAYER_NAME_COLOR,
                115,
                meta
        );
    }

    private static List<ResolvedLiveWaypoint> resolvedWaypointsFromNode(SearchNode node) {
        if (node == null || node.type() != NodeType.WAYPOINT) {
            return List.of();
        }
        List<String> providerIds = splitCsv(node.meta(WAYPOINT_MERGED_PROVIDERS, node.meta(SearchNodeKeys.WAYPOINT_PROVIDER, "")));
        List<String> providerLabels = splitCsv(node.meta(WAYPOINT_MERGED_PROVIDER_LABELS, node.meta(SearchNodeKeys.WAYPOINT_PROVIDER_LABEL, "")));
        List<String> waypointIds = splitCsv(node.meta(WAYPOINT_MERGED_PROVIDER_IDS, node.meta(SearchNodeKeys.WAYPOINT_ID, "")));
        List<ResolvedLiveWaypoint> resolved = new ArrayList<>();
        int count = Math.min(providerIds.size(), waypointIds.size());
        for (int i = 0; i < count; i++) {
            String providerId = providerIds.get(i);
            PlayerWaypointProvider waypointProvider = provider(providerId).orElse(null);
            if (waypointProvider == null) {
                continue;
            }
            String label = i < providerLabels.size() ? providerLabels.get(i) : waypointProvider.label();
            resolved.add(new ResolvedLiveWaypoint(
                    waypointProvider,
                    new LiveWaypoint(
                            providerId,
                            label,
                            waypointIds.get(i),
                            node.meta(SearchNodeKeys.WAYPOINT_NAME, node.displayName()),
                            node.meta(SearchNodeKeys.WAYPOINT_DIMENSION, "minecraft:overworld"),
                            parseInt(node.meta(SearchNodeKeys.WAYPOINT_X, "0")),
                            parseInt(node.meta(SearchNodeKeys.WAYPOINT_Y, "0")),
                            parseInt(node.meta(SearchNodeKeys.WAYPOINT_Z, "0")),
                            node.metadata()
                    )
            ));
        }
        return List.copyOf(orderResolvedWaypoints(resolved, node.meta(WAYPOINT_PRIMARY_PROVIDER, "")));
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

    private static List<ResolvedLiveWaypoint> enumerateLiveWaypoints(List<PlayerWaypointProvider> providers) {
        List<ResolvedLiveWaypoint> waypoints = new ArrayList<>();
        for (PlayerWaypointProvider provider : providers) {
            if (!available(provider)) {
                continue;
            }
            try {
                for (LiveWaypoint waypoint : provider.liveWaypoints()) {
                    if (waypoint == null || waypoint.id().isBlank()) {
                        continue;
                    }
                    waypoints.add(new ResolvedLiveWaypoint(provider, waypoint));
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.log(Level.FINE, "AMI: Player waypoint provider failed while enumerating waypoints: " + provider.id(), e);
            }
        }
        return List.copyOf(waypoints);
    }

    private static List<MergedWaypoint> mergeEquivalentWaypoints(List<ResolvedLiveWaypoint> raw) {
        Map<WaypointMergeKey, List<ResolvedLiveWaypoint>> buckets = new LinkedHashMap<>();
        for (ResolvedLiveWaypoint waypoint : raw) {
            WaypointMergeKey key = WaypointMergeKey.from(waypoint.waypoint());
            buckets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(waypoint);
        }
        return buckets.values().stream()
                .map(PlayerWaypointProviders::toMergedWaypoint)
                .toList();
    }

    private static MergedWaypoint singletonMergedWaypoint(ResolvedLiveWaypoint resolved) {
        return toMergedWaypoint(List.of(resolved));
    }

    private static MergedWaypoint toMergedWaypoint(List<ResolvedLiveWaypoint> matches) {
        List<ResolvedLiveWaypoint> ordered = orderResolvedWaypoints(matches, selectPrimaryProvider(matches));
        ResolvedLiveWaypoint primary = ordered.get(0);
        LinkedHashSet<String> providerIds = new LinkedHashSet<>();
        LinkedHashSet<String> providerLabels = new LinkedHashSet<>();
        List<String> providerWaypointIds = new ArrayList<>();
        Map<String, String> mergedMeta = new HashMap<>();
        for (ResolvedLiveWaypoint resolved : ordered) {
            providerIds.add(resolved.provider().id());
            providerLabels.add(resolved.provider().label());
            providerWaypointIds.add(resolved.waypoint().id());
            mergedMeta.putAll(resolved.waypoint().metadata());
        }
        LiveWaypoint waypoint = primary.waypoint();
        return new MergedWaypoint(
                canonicalWaypointId(waypoint),
                waypoint.name(),
                waypoint.dimension(),
                waypoint.x(),
                waypoint.y(),
                waypoint.z(),
                primary.provider().id(),
                primary.provider().label(),
                waypoint.id(),
                List.copyOf(providerIds),
                List.copyOf(providerLabels),
                List.copyOf(providerWaypointIds),
                Map.copyOf(mergedMeta)
        );
    }

    private static List<ResolvedLiveWaypoint> orderResolvedWaypoints(List<ResolvedLiveWaypoint> matches, String primaryProviderId) {
        List<ResolvedLiveWaypoint> ordered = new ArrayList<>(matches);
        ordered.sort(Comparator
                .comparing((ResolvedLiveWaypoint waypoint) -> !waypoint.provider().id().equals(primaryProviderId))
                .thenComparing(waypoint -> waypoint.provider().label(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(waypoint -> waypoint.waypoint().name(), String.CASE_INSENSITIVE_ORDER));
        return ordered;
    }

    private static String selectPrimaryProvider(List<ResolvedLiveWaypoint> providers) {
        List<String> preferred = parseProviderPriority(AmiConfig.waypointOpenProviderPriority);
        String legacyPreferred = normalizeProviderId(AmiConfig.defaultWaypointMapHandler);
        if (!legacyPreferred.isBlank() && !"auto".equals(legacyPreferred)) {
            List<String> reordered = new ArrayList<>();
            reordered.add(legacyPreferred);
            for (String candidate : preferred) {
                if (!candidate.equals(legacyPreferred)) {
                    reordered.add(candidate);
                }
            }
            preferred = reordered;
        }
        for (String id : preferred) {
            for (ResolvedLiveWaypoint candidate : providers) {
                if (candidate.provider().id().equals(id)) {
                    return id;
                }
            }
        }
        return providers.get(0).provider().id();
    }

    private static List<String> parseProviderPriority(String raw) {
        List<String> values = splitCsv(raw).stream()
                .map(PlayerWaypointProviders::normalizeProviderId)
                .filter(value -> !value.isBlank() && !"auto".equals(value))
                .toList();
        return values.isEmpty()
                ? List.of("ftbchunks", "journeymap", "xaero", "waystones", "manual")
                : values;
    }

    private static String normalizeProviderId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isBlank()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static String canonicalWaypointId(LiveWaypoint waypoint) {
        return waypoint.dimension() + ":" + waypoint.x() + ":" + waypoint.y() + ":" + waypoint.z() + ":" + normalizedWaypointName(waypoint.name());
    }

    private static String normalizedWaypointName(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized.replaceAll("[^a-z0-9 _.-]", "");
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

    private record ResolvedLiveWaypoint(PlayerWaypointProvider provider, LiveWaypoint waypoint) {
    }

    private record WaypointMergeKey(String dimension, int x, int y, int z, String normalizedName) {
        private static WaypointMergeKey from(LiveWaypoint waypoint) {
            return new WaypointMergeKey(
                    waypoint.dimension(),
                    waypoint.x(),
                    waypoint.y(),
                    waypoint.z(),
                    normalizedWaypointName(waypoint.name())
            );
        }
    }

    private record MergedWaypoint(
            String canonicalId,
            String displayName,
            String dimension,
            int x,
            int y,
            int z,
            String primaryProviderId,
            String primaryProviderLabel,
            String primaryWaypointId,
            List<String> providerIds,
            List<String> providerLabels,
            List<String> providerWaypointIds,
            Map<String, String> metadata
    ) {
    }
}
