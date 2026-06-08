package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.index.SearchNode;

import java.util.Map;
import java.util.List;
import java.util.Optional;

public interface PlayerWaypointProvider {
    String id();

    String label();

    boolean isAvailable();

    default void enrich(PlayerWaypointContext context, Map<String, String> metadata) {
    }

    default Optional<PlayerWaypointExport> waypointExport(PlayerWaypointContext context) {
        return Optional.empty();
    }

    default List<PlayerWaypointAction> waypointActions(PlayerWaypointContext context) {
        return List.of();
    }

    default List<LiveWaypoint> liveWaypoints() {
        return List.of();
    }

    default List<PlayerWaypointAction> liveWaypointActions(LiveWaypointContext context) {
        return List.of();
    }

    default Optional<PlayerWaypointAction> openLiveWaypointAction(LiveWaypointContext context) {
        return Optional.empty();
    }

    /**
     * Get the tooltip label for a waypoint. Called by tooltip builders to customize
     * how the waypoint's provider is displayed. Providers can override this to show
     * custom labels (e.g., "Waystones" instead of "JourneyMap" for synced waypoints).
     *
     * @param node the SearchNode for the waypoint
     * @return the label to show in the tooltip, or null to use the default provider label
     */
    default String getTooltipLabel(SearchNode node) {
        return null; // Use default label
    }
}
