package com.sanhiruzu.ami.player;

import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.Optional;

final class ManualWaypointProvider implements PlayerWaypointProvider {
    private static final String ID = "manual";
    private static final String LABEL = "Manual Coordinates";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String label() {
        return LABEL;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Optional<PlayerWaypointExport> waypointExport(PlayerWaypointContext context) {
        if (context == null || !hasCoordinates(context)) {
            return Optional.empty();
        }
        String dimension = context.metadata().getOrDefault(SearchNodeKeys.PLAYER_DIMENSION, "minecraft:overworld");
        String x = context.metadata().getOrDefault(SearchNodeKeys.PLAYER_X, "");
        String y = context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Y, "");
        String z = context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Z, "");
        String payload = context.playerName() + " @ " + dimension + " " + x + " " + y + " " + z
                + "\n/tp @s " + x + " " + y + " " + z;
        return Optional.of(new PlayerWaypointExport(ID, LABEL, payload));
    }

    private static boolean hasCoordinates(PlayerWaypointContext context) {
        return !context.metadata().getOrDefault(SearchNodeKeys.PLAYER_X, "").isBlank()
                && !context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Y, "").isBlank()
                && !context.metadata().getOrDefault(SearchNodeKeys.PLAYER_Z, "").isBlank();
    }
}
