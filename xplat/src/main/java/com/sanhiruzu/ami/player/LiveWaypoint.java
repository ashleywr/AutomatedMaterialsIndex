package com.sanhiruzu.ami.player;

import java.util.Map;

public record LiveWaypoint(
        String providerId,
        String providerLabel,
        String id,
        String name,
        String dimension,
        int x,
        int y,
        int z,
        Map<String, String> metadata
) {
    public LiveWaypoint {
        providerId = providerId == null ? "" : providerId;
        providerLabel = providerLabel == null || providerLabel.isBlank() ? providerId : providerLabel;
        id = id == null ? "" : id;
        name = name == null || name.isBlank() ? "Waypoint" : name;
        dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public String chatLine() {
        return name + " @ " + dimension + " " + x + " " + y + " " + z;
    }
}
