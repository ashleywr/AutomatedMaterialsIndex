package com.sanhiruzu.ami.player;

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
}
