package com.sanhiruzu.ami.player;

import java.util.Map;

public record PlayerWaypointContext(String playerName, String playerUuid, Map<String, String> metadata) {
    public PlayerWaypointContext {
        playerName = playerName == null ? "" : playerName;
        playerUuid = playerUuid == null ? "" : playerUuid;
        metadata = metadata == null ? Map.of() : metadata;
    }
}
